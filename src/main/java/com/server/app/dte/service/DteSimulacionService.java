package com.server.app.dte.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.server.app.dte.dto.DteDocumento;
import com.server.app.dte.dto.DteRespuesta;
import com.server.app.dte.model.DteAlmacenado;
import com.server.app.dte.repository.DteAlmacenadoRepository;
import com.server.app.dte.validation.CodigoMensaje;
import com.server.app.dte.validation.DteValidator;
import com.server.app.dte.validation.Hallazgo;
import com.server.app.dte.validation.ResultadoValidacion;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Simula el flujo de recepción de un DTE: validación, sello de recepción, firma y consulta. Los documentos aceptados
 * se almacenan en la base de datos; no hay comunicación con el Ministerio de Hacienda.
 */
@Service
public class DteSimulacionService {

  private static final int VERSION_RESPUESTA = 2;
  private static final int VERSION_APP = 2;
  private static final DateTimeFormatter FH = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final String ALFANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  /** Archivo DTE entregable al receptor (Manual Técnico 6.5) y URL del código QR (sección 5). */
  public record ArchivoDte(JsonNode archivo, String urlQr) {
  }

  private final ObjectMapper mapper;
  private final DteValidator validator;
  private final DteAlmacenadoRepository repository;
  private final byte[] secretoFirma;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  @Autowired
  public DteSimulacionService(
      ObjectMapper mapper,
      DteValidator validator,
      DteAlmacenadoRepository repository,
      @Value("${dte.simulacion.firma-secreto:simulacion-dte-no-usar-en-produccion-0123456789}") String secreto,
      @Value("${dte.simulacion.zona-horaria:America/El_Salvador}") String zona) {
    this(mapper, validator, repository, secreto, Clock.system(ZoneId.of(zona)));
  }

  DteSimulacionService(
      ObjectMapper mapper,
      DteValidator validator,
      DteAlmacenadoRepository repository,
      String secreto,
      Clock clock) {
    this.mapper = mapper;
    this.validator = validator;
    this.repository = repository;
    this.secretoFirma = secreto.getBytes(StandardCharsets.UTF_8);
    this.clock = clock;
  }

  /** Procesa el JSON crudo del DTE y devuelve la respuesta de recepción simulada. */
  public DteRespuesta procesar(String json) {
    if (json == null || json.isBlank()) {
      return rechazo(null, "00", List.of(new Hallazgo(CodigoMensaje.SIN_DATOS, "body", null)));
    }
    JsonNode nodo;
    try {
      nodo = mapper.readTree(json);
    } catch (JsonProcessingException e) {
      return rechazo(null, "00", List.of(new Hallazgo(CodigoMensaje.JSON_INVALIDO, "body", e.getOriginalMessage())));
    }
    if (nodo == null || !nodo.isObject() || nodo.size() == 0) {
      return rechazo(null, "00", List.of(new Hallazgo(CodigoMensaje.SIN_DATOS, "body", "se esperaba un objeto JSON")));
    }
    DteDocumento doc;
    try {
      doc = mapper.treeToValue(nodo, DteDocumento.class);
    } catch (JsonProcessingException e) {
      return rechazo(null, "00", List.of(new Hallazgo(CodigoMensaje.JSON_INVALIDO, "body", e.getOriginalMessage())));
    }

    ResultadoValidacion res = validator.validar(doc);
    String codGen = doc.identificacion() == null ? null : doc.identificacion().codigoGeneracion();
    String ambiente = ambiente(doc);
    if (!res.valido()) {
      return rechazo(codGen, ambiente, res.errores());
    }

    // Duplicados (código 4): el código de generación y el número de control no pueden repetirse.
    synchronized (this) {
      List<Hallazgo> dup = new ArrayList<>();
      if (repository.existsByCodigoGeneracion(codGen)) {
        dup.add(new Hallazgo(CodigoMensaje.YA_EXISTE, "identificacion.codigoGeneracion", null));
      }
      if (repository.existsByNitEmisorAndNumeroControl(doc.emisor().nit(), doc.identificacion().numeroControl())) {
        dup.add(new Hallazgo(CodigoMensaje.YA_EXISTE, "identificacion.numeroControl", null));
      }
      if (!dup.isEmpty()) {
        return rechazo(codGen, ambiente, dup);
      }
      String sello = generarSello();
      List<String> obs = res.observaciones().stream().map(Hallazgo::mensaje).toList();
      boolean conObs = !obs.isEmpty();
      DteRespuesta ok = new DteRespuesta(VERSION_RESPUESTA, ambiente, VERSION_APP, DteRespuesta.PROCESADO, codGen,
          sello, ahora(), "10",
          (conObs ? CodigoMensaje.RECIBIDO_CON_OBSERVACIONES : CodigoMensaje.RECIBIDO).codigoMsg(),
          (conObs ? CodigoMensaje.RECIBIDO_CON_OBSERVACIONES : CodigoMensaje.RECIBIDO).descripcion(), obs);
      String respuestaJson;
      try {
        respuestaJson = mapper.writeValueAsString(ok);
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("No se pudo serializar la respuesta DTE para almacenarla.", e);
      }
      DteAlmacenado almacenado = new DteAlmacenado(
          codGen,
          doc.emisor().nit(),
          doc.identificacion().numeroControl(),
          nodo.toString(),
          respuestaJson,
          firmar(nodo));
      try {
        repository.saveAndFlush(almacenado);
      } catch (DataIntegrityViolationException e) {
        List<Hallazgo> duplicadosConcurrentes = new ArrayList<>();
        if (repository.existsByCodigoGeneracion(codGen)) {
          duplicadosConcurrentes.add(new Hallazgo(CodigoMensaje.YA_EXISTE, "identificacion.codigoGeneracion", null));
        }
        if (repository.existsByNitEmisorAndNumeroControl(
            doc.emisor().nit(), doc.identificacion().numeroControl())) {
          duplicadosConcurrentes.add(new Hallazgo(CodigoMensaje.YA_EXISTE, "identificacion.numeroControl", null));
        }
        if (duplicadosConcurrentes.isEmpty()) {
          throw e;
        }
        return rechazo(codGen, ambiente, duplicadosConcurrentes);
      }
      return ok;
    }
  }

  public Optional<DteRespuesta> consultar(String codigoGeneracion) {
    return repository.findByCodigoGeneracion(codigoGeneracion).map(this::respuestaDe);
  }

  /** Archivo DTE: estructura de datos + firmaElectronica + selloRecibido, y la URL del QR. */
  public Optional<ArchivoDte> archivo(String codigoGeneracion) {
    return repository.findByCodigoGeneracion(codigoGeneracion).map(reg -> {
      JsonNode documento;
      try {
        documento = mapper.readTree(reg.getDocumentoJson());
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("No se pudo leer el documento DTE almacenado.", e);
      }
      DteRespuesta respuesta = respuestaDe(reg);
      ObjectNode archivo = ((ObjectNode) documento).deepCopy();
      archivo.put("firmaElectronica", reg.getFirma());
      archivo.put("selloRecibido", respuesta.selloRecibido());
      String fecEmi = documento.path("identificacion").path("fecEmi").asText();
      String url = "https://admin.factura.gob.sv/consultaPublica?ambiente=" + respuesta.ambiente()
        + "&codGen=" + codigoGeneracion + "&fechaEmi=" + fecEmi;
      return new ArchivoDte(archivo, url);
    });
  }

  // ───────────────────────────── Internos ─────────────────────────────

  private DteRespuesta respuestaDe(DteAlmacenado registrado) {
    try {
      return mapper.readValue(registrado.getRespuestaJson(), DteRespuesta.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo leer la respuesta DTE almacenada.", e);
    }
  }

  private DteRespuesta rechazo(String codGen, String ambiente, List<Hallazgo> errores) {
    Hallazgo primero = errores.get(0);
    List<String> mensajes = errores.stream().map(Hallazgo::mensaje).toList();
    return new DteRespuesta(VERSION_RESPUESTA, ambiente, VERSION_APP, DteRespuesta.RECHAZADO, codGen, null, ahora(),
        "20", primero.codigo().codigoMsg(), primero.codigo().descripcion(), mensajes);
  }

  private static String ambiente(DteDocumento doc) {
    String a = doc.identificacion() == null ? null : doc.identificacion().ambiente();
    return "00".equals(a) || "01".equals(a) ? a : "00";
  }

  private String ahora() {
    return LocalDateTime.now(clock).format(FH);
  }

  /** Sello simulado: año + 36 caracteres alfanuméricos en mayúscula (40 en total, como en los ejemplos del manual). */
  private String generarSello() {
    StringBuilder sb = new StringBuilder(String.valueOf(LocalDateTime.now(clock).getYear()));
    sb.append(UUID.randomUUID().toString().replace("-", "").toUpperCase());
    for (int i = 0; i < 4; i++) {
      sb.append(ALFANUM.charAt(random.nextInt(ALFANUM.length())));
    }
    return sb.toString();
  }

  /** JWS compacto HS256. La firma real usa el certificado del contribuyente mediante el servicio firmador. */
  private String firmar(JsonNode documento) {
    try {
      Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
      String cabecera = b64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
      String carga = b64.encodeToString(mapper.writeValueAsBytes(documento));
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretoFirma, "HmacSHA256"));
      byte[] firma = mac.doFinal((cabecera + "." + carga).getBytes(StandardCharsets.UTF_8));
      return cabecera + "." + carga + "." + b64.encodeToString(firma);
    } catch (GeneralSecurityException | JsonProcessingException e) {
      throw new IllegalStateException("No se pudo firmar el DTE simulado", e);
    }
  }
}
