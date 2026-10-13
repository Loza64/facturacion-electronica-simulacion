package com.server.app.dte.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.server.app.dte.dto.DteDocumento;
import com.server.app.dte.dto.DteRespuesta;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Simula el flujo de recepción de un DTE: validación, sello de recepción, firma y consulta. El estado vive en memoria
 * (se pierde al reiniciar); no hay persistencia ni comunicación con el Ministerio de Hacienda.
 */
@Service
public class DteSimulacionService {

  private static final int VERSION_RESPUESTA = 2;
  private static final int VERSION_APP = 2;
  private static final DateTimeFormatter FH = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final String ALFANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  /** DTE aceptado: documento original, respuesta con el sello y firma simulada. */
  public record Registrado(JsonNode documento, DteRespuesta respuesta, String firma) {
  }

  /** Archivo DTE entregable al receptor (Manual Técnico 6.5) y URL del código QR (sección 5). */
  public record ArchivoDte(JsonNode archivo, String urlQr) {
  }

  private final ObjectMapper mapper;
  private final DteValidator validator;
  private final byte[] secretoFirma;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  private final Map<String, Registrado> porCodigoGeneracion = new HashMap<>();
  private final Set<String> numerosControl = new HashSet<>();

  @Autowired
  public DteSimulacionService(
      ObjectMapper mapper,
      DteValidator validator,
      @Value("${dte.simulacion.firma-secreto:simulacion-dte-no-usar-en-produccion-0123456789}") String secreto,
      @Value("${dte.simulacion.zona-horaria:America/El_Salvador}") String zona) {
    this(mapper, validator, secreto, Clock.system(ZoneId.of(zona)));
  }

  public DteSimulacionService(ObjectMapper mapper, DteValidator validator, String secreto, Clock clock) {
    this.mapper = mapper;
    this.validator = validator;
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
    String claveControl = doc.emisor().nit() + "|" + doc.identificacion().numeroControl();
    synchronized (this) {
      List<Hallazgo> dup = new ArrayList<>();
      if (porCodigoGeneracion.containsKey(codGen)) {
        dup.add(new Hallazgo(CodigoMensaje.YA_EXISTE, "identificacion.codigoGeneracion", null));
      }
      if (numerosControl.contains(claveControl)) {
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
      porCodigoGeneracion.put(codGen, new Registrado(nodo.deepCopy(), ok, firmar(nodo)));
      numerosControl.add(claveControl);
      return ok;
    }
  }

  public Optional<DteRespuesta> consultar(String codigoGeneracion) {
    synchronized (this) {
      return Optional.ofNullable(porCodigoGeneracion.get(codigoGeneracion)).map(Registrado::respuesta);
    }
  }

  /** Archivo DTE: estructura de datos + firmaElectronica + selloRecibido, y la URL del QR. */
  public Optional<ArchivoDte> archivo(String codigoGeneracion) {
    Registrado reg;
    synchronized (this) {
      reg = porCodigoGeneracion.get(codigoGeneracion);
    }
    if (reg == null) {
      return Optional.empty();
    }
    ObjectNode archivo = reg.documento().deepCopy();
    archivo.put("firmaElectronica", reg.firma());
    archivo.put("selloRecibido", reg.respuesta().selloRecibido());
    String fecEmi = reg.documento().path("identificacion").path("fecEmi").asText();
    String url = "https://admin.factura.gob.sv/consultaPublica?ambiente=" + reg.respuesta().ambiente()
        + "&codGen=" + codigoGeneracion + "&fechaEmi=" + fecEmi;
    return Optional.of(new ArchivoDte(archivo, url));
  }

  // ───────────────────────────── Internos ─────────────────────────────

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
