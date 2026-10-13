package com.server.app.dte.validation;

import static com.server.app.dte.validation.CodigoMensaje.CALCULO_INCORRECTO;
import static com.server.app.dte.validation.CodigoMensaje.DIFIERE_FECHA_ENVIO;
import static com.server.app.dte.validation.CodigoMensaje.ERROR_CORRELATIVO;
import static com.server.app.dte.validation.CodigoMensaje.EXCEDE_HOLGURA;
import static com.server.app.dte.validation.CodigoMensaje.FECHA_FUERA_PLAZO;
import static com.server.app.dte.validation.CodigoMensaje.FECHA_MENOR_2000;
import static com.server.app.dte.validation.CodigoMensaje.FORMATO_NO_VALIDO;
import static com.server.app.dte.validation.CodigoMensaje.NIT_INVALIDO;
import static com.server.app.dte.validation.CodigoMensaje.NIT_NO_EXISTE;
import static com.server.app.dte.validation.CodigoMensaje.NO_APLICA;
import static com.server.app.dte.validation.CodigoMensaje.NO_COINCIDE;
import static com.server.app.dte.validation.CodigoMensaje.NO_PERMITIDO;
import static com.server.app.dte.validation.CodigoMensaje.REPETIDOS;
import static com.server.app.dte.validation.CodigoMensaje.REQUERIDO;
import static com.server.app.dte.validation.CodigoMensaje.TIPO_NO_AUTORIZADO;
import static com.server.app.dte.validation.CodigoMensaje.TRIBUTO_INFORMATIVO;
import static com.server.app.dte.validation.CodigoMensaje.VALOR_NO_VALIDO;

import com.server.app.dte.catalog.CatalogoService;
import com.server.app.dte.dto.DteDocumento;
import com.server.app.dte.dto.DteDocumento.Direccion;
import com.server.app.dte.dto.DteDocumento.Emisor;
import com.server.app.dte.dto.DteDocumento.Identificacion;
import com.server.app.dte.dto.DteDocumento.Item;
import com.server.app.dte.dto.DteDocumento.Pago;
import com.server.app.dte.dto.DteDocumento.Receptor;
import com.server.app.dte.dto.DteDocumento.Resumen;
import com.server.app.dte.dto.DteDocumento.Tributo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simula las validaciones del servicio de recepción para Factura Electrónica (01) y Comprobante de Crédito Fiscal
 * Electrónico (03), según los Catálogos v1.1, el Manual Técnico v2.0 y el Manual Funcional v2.0.
 *
 * <p>Es una simulación: no verifica firma, ni existencia real de contribuyentes, ni cubre todos los tipos de DTE.
 */
@Component
public class DteValidator {

  public enum TipoDte {
    FE("01"), CCFE("03");

    private final String codigo;

    TipoDte(String codigo) {
      this.codigo = codigo;
    }

    public String codigo() {
      return codigo;
    }

    static TipoDte de(String codigo) {
      for (TipoDte t : values()) {
        if (t.codigo.equals(codigo)) {
          return t;
        }
      }
      return null;
    }
  }

  private static final BigDecimal TOLERANCIA = new BigDecimal("0.01"); // holgura del manual (Redondeos y Holguras)
  private static final BigDecimal IVA = new BigDecimal("0.13");
  private static final BigDecimal IVA_INCLUIDO = new BigDecimal("13").divide(new BigDecimal("113"), 12,
      RoundingMode.HALF_UP);
  private static final BigDecimal UNO_POR_CIENTO = new BigDecimal("0.01");
  private static final BigDecimal TURISMO = new BigDecimal("0.05");
  private static final BigDecimal FOVIAL = new BigDecimal("0.20");
  private static final BigDecimal COTRANS = new BigDecimal("0.10");

  private static final Pattern UUID_V4 =
      Pattern.compile("^[0-9A-F]{8}-[0-9A-F]{4}-4[0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}$");
  /** DTE-TT-(M|B|S|P)NNN P NNN-15 dígitos; en la transición se aceptan 8 caracteres numéricos. */
  private static final Pattern NUMERO_CONTROL =
      Pattern.compile("^DTE-(\\d{2})-([MBSP]\\d{3}P\\d{3}|\\d{8})-(\\d{15})$");
  private static final Pattern NIT = Pattern.compile("^\\d{14}$");
  private static final Pattern NIT_O_DUI9 = Pattern.compile("^(\\d{14}|\\d{9})$");
  private static final Pattern DUI = Pattern.compile("^(\\d{8}-\\d|\\d{9})$");
  private static final Pattern NRC = Pattern.compile("^\\d{1,8}$");
  private static final Pattern TELEFONO = Pattern.compile("^[0-9+\\- ]{8,30}$");
  private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final CatalogoService cat;
  private final Set<String> nitsRegistrados;
  private final Set<Integer> versiones;
  private final boolean soloPruebas;
  private final Clock clock;

  @Autowired
  public DteValidator(
      CatalogoService cat,
      @Value("${dte.simulacion.nits-registrados:}") String nitsRegistrados,
      @Value("${dte.simulacion.versiones-permitidas:1,2,3}") String versiones,
      @Value("${dte.simulacion.solo-ambiente-pruebas:true}") boolean soloPruebas,
      @Value("${dte.simulacion.zona-horaria:America/El_Salvador}") String zona) {
    this(cat, csv(nitsRegistrados),
        csv(versiones).stream().map(Integer::valueOf).collect(Collectors.toSet()),
        soloPruebas, Clock.system(ZoneId.of(zona)));
  }

  public DteValidator(CatalogoService cat, Set<String> nitsRegistrados, Set<Integer> versiones,
      boolean soloPruebas, Clock clock) {
    this.cat = cat;
    this.nitsRegistrados = nitsRegistrados;
    this.versiones = versiones;
    this.soloPruebas = soloPruebas;
    this.clock = clock;
  }

  private static Set<String> csv(String s) {
    if (s == null || s.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  // ───────────────────────────── API ─────────────────────────────

  public ResultadoValidacion validar(DteDocumento d) {
    ResultadoValidacion r = new ResultadoValidacion();
    if (d == null || d.identificacion() == null) {
      r.error(REQUERIDO, "identificacion");
      return r;
    }
    TipoDte tipo = validarIdentificacion(d.identificacion(), r);
    if (tipo == null) {
      return r; // sin tipo soportado no se puede seguir
    }
    validarEmisor(d.emisor(), d.identificacion(), r);
    validarReceptor(d.receptor(), tipo, r);
    Acum acum = validarCuerpo(d.cuerpoDocumento(), tipo, r);
    validarResumen(d.resumen(), tipo, acum, r);
    return r;
  }

  // ───────────────────────── Identificación ─────────────────────────

  private TipoDte validarIdentificacion(Identificacion id, ResultadoValidacion r) {
    String p = "identificacion.";

    if (id.version() == null) {
      r.error(REQUERIDO, p + "version");
    } else if (!versiones.contains(id.version())) {
      r.error(VALOR_NO_VALIDO, p + "version", "versiones permitidas " + versiones);
    }

    if (vacio(id.ambiente())) {
      r.error(REQUERIDO, p + "ambiente");
    } else if (!cat.existe("CAT-001", id.ambiente())) {
      r.error(VALOR_NO_VALIDO, p + "ambiente", "CAT-001: 00 prueba, 01 producción");
    } else if (soloPruebas && !"00".equals(id.ambiente())) {
      r.error(NO_PERMITIDO, p + "ambiente",
          "el simulador sólo acepta ambiente 00 (nunca enviar DTE de prueba a producción)");
    }

    TipoDte tipo = null;
    if (vacio(id.tipoDte())) {
      r.error(REQUERIDO, p + "tipoDte");
    } else if (!cat.existe("CAT-002", id.tipoDte())) {
      r.error(VALOR_NO_VALIDO, p + "tipoDte", "CAT-002");
    } else {
      tipo = TipoDte.de(id.tipoDte());
      if (tipo == null) {
        r.error(TIPO_NO_AUTORIZADO, p + "tipoDte", "el simulador sólo soporta 01 (FE) y 03 (CCFE)");
      }
    }

    if (vacio(id.numeroControl())) {
      r.error(REQUERIDO, p + "numeroControl");
    } else {
      var m = NUMERO_CONTROL.matcher(id.numeroControl());
      if (!m.matches()) {
        r.error(FORMATO_NO_VALIDO, p + "numeroControl", "formato DTE-TT-XNNNPNNN-000000000000001 (31 caracteres)");
      } else if (id.tipoDte() != null && !m.group(1).equals(id.tipoDte())) {
        r.error(NO_COINCIDE, p + "numeroControl", "el tipo del número de control no coincide con tipoDte");
      }
    }

    if (vacio(id.codigoGeneracion())) {
      r.error(REQUERIDO, p + "codigoGeneracion");
    } else if (!UUID_V4.matcher(id.codigoGeneracion()).matches()) {
      r.error(FORMATO_NO_VALIDO, p + "codigoGeneracion", "UUID versión 4 en MAYÚSCULAS");
    }

    if (id.tipoModelo() == null) {
      r.error(REQUERIDO, p + "tipoModelo");
    } else if (!cat.existe("CAT-003", String.valueOf(id.tipoModelo()))) {
      r.error(VALOR_NO_VALIDO, p + "tipoModelo", "CAT-003");
    }

    if (id.tipoOperacion() == null) {
      r.error(REQUERIDO, p + "tipoOperacion");
    } else if (!cat.existe("CAT-004", String.valueOf(id.tipoOperacion()))) {
      r.error(VALOR_NO_VALIDO, p + "tipoOperacion", "CAT-004");
    } else if (id.tipoOperacion() == 2) {
      // Contingencia: modelo diferido y causa obligatoria (CAT-005).
      if (id.tipoModelo() != null && id.tipoModelo() != 2) {
        r.error(NO_PERMITIDO, p + "tipoModelo", "en contingencia el modelo debe ser 2 (diferido)");
      }
      if (id.tipoContingencia() == null) {
        r.error(REQUERIDO, p + "tipoContingencia");
      } else if (!cat.existe("CAT-005", String.valueOf(id.tipoContingencia()))) {
        r.error(VALOR_NO_VALIDO, p + "tipoContingencia", "CAT-005");
      } else if (id.tipoContingencia() == 5 && vacio(id.motivoContin())) {
        r.error(REQUERIDO, p + "motivoContin", "obligatorio cuando tipoContingencia = 5 (máx. 500 caracteres)");
      }
      if (id.motivoContin() != null && id.motivoContin().length() > 500) {
        r.error(FORMATO_NO_VALIDO, p + "motivoContin", "máximo 500 caracteres");
      }
    } else {
      if (id.tipoContingencia() != null) {
        r.error(NO_APLICA, p + "tipoContingencia", "sólo aplica en transmisión por contingencia");
      }
      if (!vacio(id.motivoContin())) {
        r.error(NO_APLICA, p + "motivoContin", "sólo aplica en transmisión por contingencia");
      }
    }

    if (!"USD".equals(id.tipoMoneda())) {
      r.error(VALOR_NO_VALIDO, p + "tipoMoneda", "debe ser USD");
    }

    validarFechaHora(id, r);
    return tipo;
  }

  /** Holguras del Manual Técnico 3.1 y códigos 17/18/32/34/116 del servicio de recepción. */
  private void validarFechaHora(Identificacion id, ResultadoValidacion r) {
    LocalDate fec = null;
    LocalTime hor = null;
    if (vacio(id.fecEmi())) {
      r.error(REQUERIDO, "identificacion.fecEmi");
    } else {
      try {
        fec = LocalDate.parse(id.fecEmi());
      } catch (DateTimeParseException e) {
        r.error(FORMATO_NO_VALIDO, "identificacion.fecEmi", "formato yyyy-MM-dd");
      }
    }
    if (vacio(id.horEmi())) {
      r.error(REQUERIDO, "identificacion.horEmi");
    } else {
      try {
        hor = LocalTime.parse(id.horEmi());
      } catch (DateTimeParseException e) {
        r.error(FORMATO_NO_VALIDO, "identificacion.horEmi", "formato HH:mm:ss");
      }
    }
    if (fec == null || hor == null) {
      return;
    }
    if (fec.isBefore(LocalDate.of(2000, 1, 1))) {
      r.error(FECHA_MENOR_2000, "identificacion.fecEmi");
      return;
    }
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDate hoy = ahora.toLocalDate();
    LocalDateTime emision = LocalDateTime.of(fec, hor);
    if (emision.isAfter(ahora.plusMinutes(30))) {
      boolean ultimoDiaDelMes = hoy.equals(hoy.withDayOfMonth(hoy.lengthOfMonth()));
      if (fec.isAfter(hoy.plusDays(1))) {
        r.error(FECHA_FUERA_PLAZO, "identificacion.fecEmi", "sólo se admite un día posterior a la transmisión");
      } else if (fec.equals(hoy.plusDays(1)) && !ultimoDiaDelMes) {
        // permitido: un día posterior, excepto el último día del período tributario
      } else {
        r.error(EXCEDE_HOLGURA, "identificacion.horEmi");
      }
    }
    if (id.tipoOperacion() != null && id.tipoOperacion() == 1 && !fec.equals(hoy)) {
      r.observacion(DIFIERE_FECHA_ENVIO, "identificacion.fecEmi", null);
    }
  }

  // ─────────────────────────── Emisor / Receptor ───────────────────────────

  private void validarEmisor(Emisor e, Identificacion id, ResultadoValidacion r) {
    if (e == null) {
      r.error(REQUERIDO, "emisor");
      return;
    }
    String p = "emisor.";
    if (vacio(e.nit())) {
      r.error(REQUERIDO, p + "nit");
    } else if (!NIT.matcher(e.nit()).matches()) {
      r.error(NIT_INVALIDO, p + "nit", "14 dígitos, sin guiones");
    } else if (!nitsRegistrados.isEmpty() && !nitsRegistrados.contains(e.nit())) {
      r.error(NIT_NO_EXISTE, p + "nit");
    }
    if (vacio(e.nrc())) {
      r.error(REQUERIDO, p + "nrc");
    } else if (!NRC.matcher(e.nrc()).matches()) {
      r.error(FORMATO_NO_VALIDO, p + "nrc", "de 1 a 8 dígitos");
    }
    requerido(r, p + "nombre", e.nombre());
    actividad(r, p, e.codActividad(), e.descActividad(), true);
    if (vacio(e.tipoEstablecimiento())) {
      r.error(REQUERIDO, p + "tipoEstablecimiento");
    } else if (!cat.existe("CAT-009", e.tipoEstablecimiento())) {
      r.error(VALOR_NO_VALIDO, p + "tipoEstablecimiento", "CAT-009");
    }
    direccion(r, p + "direccion", e.direccion(), true, false);
    if (vacio(e.telefono())) {
      r.error(REQUERIDO, p + "telefono");
    } else if (!TELEFONO.matcher(e.telefono()).matches()) {
      r.error(FORMATO_NO_VALIDO, p + "telefono");
    }
    if (vacio(e.correo())) {
      r.error(REQUERIDO, p + "correo");
    } else if (!CORREO.matcher(e.correo()).matches()) {
      r.error(FORMATO_NO_VALIDO, p + "correo");
    }
    // Sección 3 del número de control = código de establecimiento + punto de venta.
    if (!vacio(e.codEstable()) && !vacio(e.codPuntoVenta()) && id.numeroControl() != null) {
      var m = NUMERO_CONTROL.matcher(id.numeroControl());
      if (m.matches() && !m.group(2).equals(e.codEstable() + e.codPuntoVenta())) {
        r.error(NO_COINCIDE, p + "codEstable", "no coincide con la sección 2 del número de control");
      }
    }
  }

  private void validarReceptor(Receptor rc, TipoDte tipo, ResultadoValidacion r) {
    String p = "receptor.";
    if (tipo == TipoDte.CCFE) {
      if (rc == null) {
        r.error(REQUERIDO, "receptor");
        return;
      }
      if (vacio(rc.nit())) {
        r.error(REQUERIDO, p + "nit");
      } else if (!NIT_O_DUI9.matcher(rc.nit()).matches()) {
        r.error(NIT_INVALIDO, p + "nit", "14 dígitos (o 9 si es DUI), sin guiones");
      }
      if (vacio(rc.nrc())) {
        r.error(REQUERIDO, p + "nrc");
      } else if (!NRC.matcher(rc.nrc()).matches()) {
        r.error(FORMATO_NO_VALIDO, p + "nrc", "de 1 a 8 dígitos");
      }
      requerido(r, p + "nombre", rc.nombre());
      actividad(r, p, rc.codActividad(), rc.descActividad(), true);
      direccion(r, p + "direccion", rc.direccion(), true, false);
      if (vacio(rc.correo())) {
        r.error(REQUERIDO, p + "correo");
      }
    } else {
      // FE: el receptor puede omitirse; si se identifica, tipoDocumento, numDocumento y nombre van juntos.
      if (rc == null) {
        return;
      }
      boolean alguno = !vacio(rc.tipoDocumento()) || !vacio(rc.numDocumento()) || !vacio(rc.nombre());
      if (alguno) {
        if (vacio(rc.tipoDocumento())) {
          r.error(REQUERIDO, p + "tipoDocumento");
        } else if (!cat.existe("CAT-022", rc.tipoDocumento())) {
          r.error(VALOR_NO_VALIDO, p + "tipoDocumento", "CAT-022");
        }
        if (vacio(rc.numDocumento())) {
          r.error(REQUERIDO, p + "numDocumento");
        } else if ("36".equals(rc.tipoDocumento()) && !NIT.matcher(rc.numDocumento()).matches()) {
          r.error(FORMATO_NO_VALIDO, p + "numDocumento", "NIT: 14 dígitos sin guiones");
        } else if ("13".equals(rc.tipoDocumento()) && !DUI.matcher(rc.numDocumento()).matches()) {
          r.error(FORMATO_NO_VALIDO, p + "numDocumento", "DUI: 00000000-0");
        }
        requerido(r, p + "nombre", rc.nombre());
      }
      if (!vacio(rc.codActividad())) {
        actividad(r, p, rc.codActividad(), rc.descActividad(), false);
      }
      if (rc.direccion() != null) {
        direccion(r, p + "direccion", rc.direccion(), false, true);
      }
      if (!vacio(rc.correo()) && !CORREO.matcher(rc.correo()).matches()) {
        r.error(FORMATO_NO_VALIDO, p + "correo");
      }
    }
  }

  private void actividad(ResultadoValidacion r, String p, String cod, String desc, boolean obligatoria) {
    if (vacio(cod)) {
      if (obligatoria) {
        r.error(REQUERIDO, p + "codActividad");
      }
    } else if (!cat.existe("CAT-019", cod)) {
      r.error(VALOR_NO_VALIDO, p + "codActividad", "CAT-019");
    }
    if (obligatoria && vacio(desc)) {
      r.error(REQUERIDO, p + "descActividad");
    }
  }

  private void direccion(ResultadoValidacion r, String p, Direccion d, boolean obligatoria, boolean permiteExtranjero) {
    if (d == null) {
      if (obligatoria) {
        r.error(REQUERIDO, p);
      }
      return;
    }
    boolean depOk = false;
    if (vacio(d.departamento())) {
      r.error(REQUERIDO, p + ".departamento");
    } else if (!cat.existe("CAT-012", d.departamento())) {
      r.error(VALOR_NO_VALIDO, p + ".departamento", "CAT-012");
    } else if ("00".equals(d.departamento()) && !permiteExtranjero) {
      r.error(NO_PERMITIDO, p + ".departamento", "00 (extranjero) no aplica");
    } else {
      depOk = true;
    }
    if (vacio(d.municipio())) {
      r.error(REQUERIDO, p + ".municipio");
    } else if (!cat.existe("CAT-013", d.municipio())) {
      r.error(VALOR_NO_VALIDO, p + ".municipio", "CAT-013");
    } else if (depOk && !cat.municipioDeDepartamento(d.departamento(), d.municipio())) {
      r.error(NO_COINCIDE, p + ".municipio", "el municipio no pertenece al departamento " + d.departamento());
    }
    if (obligatoria) {
      requerido(r, p + ".complemento", d.complemento());
    }
  }

  // ───────────────────────────── Cuerpo ─────────────────────────────

  /** Totales calculados a partir de los ítems, contra los que se contrasta el resumen. */
  private static final class Acum {
    BigDecimal noSuj = BigDecimal.ZERO;
    BigDecimal exenta = BigDecimal.ZERO;
    BigDecimal gravada = BigDecimal.ZERO;
    BigDecimal descu = BigDecimal.ZERO;
    BigDecimal noGravado = BigDecimal.ZERO;
    BigDecimal ivaItems = BigDecimal.ZERO;
    boolean hayIvaItem = false;
    BigDecimal gravada59 = BigDecimal.ZERO;
    BigDecimal galonesD1 = BigDecimal.ZERO;
    BigDecimal galonesC8 = BigDecimal.ZERO;
    final Set<String> tributos = new LinkedHashSet<>();
  }

  private Acum validarCuerpo(List<Item> items, TipoDte tipo, ResultadoValidacion r) {
    Acum a = new Acum();
    if (items == null || items.isEmpty()) {
      r.error(REQUERIDO, "cuerpoDocumento", "debe incluir al menos un ítem");
      return a;
    }
    boolean hayVenta = false;
    boolean hayCargo = false;

    for (int i = 0; i < items.size(); i++) {
      Item it = items.get(i);
      String p = "cuerpoDocumento[" + i + "].";
      if (it == null) {
        r.error(REQUERIDO, "cuerpoDocumento[" + i + "]");
        continue;
      }
      if (it.numItem() == null) {
        r.error(REQUERIDO, p + "numItem");
      } else if (it.numItem() != i + 1) {
        r.error(ERROR_CORRELATIVO, p + "numItem", "esperado " + (i + 1));
      }
      if (it.tipoItem() == null) {
        r.error(REQUERIDO, p + "tipoItem");
      } else if (!cat.existe("CAT-011", String.valueOf(it.tipoItem()))) {
        r.error(VALOR_NO_VALIDO, p + "tipoItem", "CAT-011");
      }
      if (it.uniMedida() == null) {
        r.error(REQUERIDO, p + "uniMedida");
      } else if (!cat.existe("CAT-014", String.valueOf(it.uniMedida()))) {
        r.error(VALOR_NO_VALIDO, p + "uniMedida", "CAT-014");
      }
      requerido(r, p + "descripcion", it.descripcion());

      BigDecimal cant = monto(r, p + "cantidad", it.cantidad(), 8, false);
      if (cant != null && cant.signum() == 0) {
        r.error(VALOR_NO_VALIDO, p + "cantidad", "debe ser mayor a 0");
      }
      BigDecimal precio = monto(r, p + "precioUni", it.precioUni(), 8, false);
      BigDecimal descu = monto(r, p + "montoDescu", it.montoDescu(), 8, false);
      BigDecimal vNoSuj = monto(r, p + "ventaNoSuj", it.ventaNoSuj(), 8, false);
      BigDecimal vExenta = monto(r, p + "ventaExenta", it.ventaExenta(), 8, false);
      BigDecimal vGravada = monto(r, p + "ventaGravada", it.ventaGravada(), 8, false);
      BigDecimal noGrav = it.noGravado() == null ? BigDecimal.ZERO
          : monto(r, p + "noGravado", it.noGravado(), 8, true);
      if (it.psv() != null) {
        monto(r, p + "psv", it.psv(), 8, false);
      }

      a.noSuj = a.noSuj.add(nz(vNoSuj));
      a.exenta = a.exenta.add(nz(vExenta));
      a.gravada = a.gravada.add(nz(vGravada));
      a.descu = a.descu.add(nz(descu));
      a.noGravado = a.noGravado.add(noGrav);
      if (nz(vNoSuj).add(nz(vExenta)).add(nz(vGravada)).signum() > 0) {
        hayVenta = true;
      }

      // Cargos/abonos que no afectan la base imponible (Manual Funcional): ítem sin precio ni ventas.
      if (noGrav.signum() != 0) {
        hayCargo = true;
        if (precio != null && precio.signum() != 0) {
          r.error(NO_APLICA, p + "precioUni", "debe ser 0 cuando se usa noGravado");
        }
        if (nz(vNoSuj).add(nz(vExenta)).add(nz(vGravada)).signum() != 0) {
          r.error(NO_APLICA, p + "ventaGravada", "las ventas deben ser 0 cuando se usa noGravado");
        }
        if (cant != null && cant.compareTo(BigDecimal.ONE) != 0) {
          r.error(NO_PERMITIDO, p + "cantidad", "debe ser 1 para cargos/abonos no gravados");
        }
        if (it.uniMedida() != null && it.uniMedida() != 99) {
          r.error(NO_PERMITIDO, p + "uniMedida", "debe ser 99 para cargos/abonos no gravados");
        }
      } else if (cant != null && precio != null && descu != null
          && vNoSuj != null && vExenta != null && vGravada != null) {
        BigDecimal bruto = cant.multiply(precio);
        if (descu.compareTo(bruto) > 0) {
          r.error(CALCULO_INCORRECTO, p + "montoDescu", "el descuento supera cantidad × precio");
        }
        BigDecimal esperado = bruto.subtract(descu);
        BigDecimal suma = vNoSuj.add(vExenta).add(vGravada);
        if (!cerca(suma, esperado)) {
          r.error(CALCULO_INCORRECTO, p + "ventas",
              "esperado " + f2(esperado) + " (cantidad × precio − descuento), recibido " + f2(suma));
        }
      }

      // Tipo 4 = "otros tributos por ítem": el código de la sección 2 de CAT-015 va en codTributo.
      if (it.tipoItem() != null && it.tipoItem() == 4) {
        if (vacio(it.codTributo())) {
          r.error(REQUERIDO, p + "codTributo");
        } else if (!cat.tributoEnSeccion(it.codTributo(), 2)) {
          r.error(VALOR_NO_VALIDO, p + "codTributo", "CAT-015 sección 2");
        }
        if (cant != null && cant.compareTo(BigDecimal.ONE) != 0) {
          r.error(NO_PERMITIDO, p + "cantidad", "debe ser 1 en ítems de tipo 4");
        }
        if (it.uniMedida() != null && it.uniMedida() != 99) {
          r.error(NO_PERMITIDO, p + "uniMedida", "debe ser 99 en ítems de tipo 4");
        }
      } else if (!vacio(it.codTributo())) {
        r.error(NO_APLICA, p + "codTributo", "sólo aplica en ítems tipo 4");
      }

      // Tributos del ítem (CAT-015 sección 1).
      Set<String> vistos = new HashSet<>();
      List<String> tr = it.tributos() == null ? List.of() : it.tributos();
      for (int j = 0; j < tr.size(); j++) {
        String c = tr.get(j);
        String pt = p + "tributos[" + j + "]";
        if (vacio(c)) {
          r.error(REQUERIDO, pt);
        } else if (!vistos.add(c)) {
          r.error(REPETIDOS, pt);
        } else if (!cat.existe("CAT-015", c)) {
          r.error(VALOR_NO_VALIDO, pt, "CAT-015");
        } else if (cat.tributoEnSeccion(c, 3)) {
          r.error(TRIBUTO_INFORMATIVO, pt);
        } else if (!cat.tributoEnSeccion(c, 1) || "D4".equals(c) || "C3".equals(c)) {
          r.error(NO_PERMITIDO, pt, "no aplica a FE/CCFE o es de uso interno del MH");
        } else if ("20".equals(c) && tipo == TipoDte.FE) {
          r.error(NO_PERMITIDO, pt, "en FE el IVA va incluido en el precio y no se detalla como tributo");
        } else if (("D1".equals(c) || "C8".equals(c)) && it.uniMedida() != null && it.uniMedida() != 22) {
          r.error(NO_PERMITIDO, pt, "FOVIAL/COTRANS sólo aplican por galón (uniMedida 22)");
        } else {
          a.tributos.add(c);
          if ("59".equals(c)) {
            a.gravada59 = a.gravada59.add(nz(vGravada));
          } else if ("D1".equals(c)) {
            a.galonesD1 = a.galonesD1.add(nz(cant));
          } else if ("C8".equals(c)) {
            a.galonesC8 = a.galonesC8.add(nz(cant));
          }
        }
      }
      if (tipo == TipoDte.CCFE) {
        if (nz(vGravada).signum() > 0 && !vistos.contains("20")) {
          r.error(REQUERIDO, p + "tributos", "ventaGravada > 0 requiere el tributo 20 (IVA 13%)");
        } else if (nz(vGravada).signum() == 0 && vistos.contains("20")) {
          r.error(NO_APLICA, p + "tributos", "el tributo 20 sólo aplica a ventas gravadas");
        }
      }
      if (tipo == TipoDte.FE && it.ivaItem() != null) {
        BigDecimal iva = monto(r, p + "ivaItem", it.ivaItem(), 2, false);
        BigDecimal esp = r2(nz(vGravada).multiply(IVA_INCLUIDO));
        if (iva != null && !cerca(iva, esp)) {
          r.error(CALCULO_INCORRECTO, p + "ivaItem", "esperado " + f2(esp) + " (gravada × 13/113), recibido " + f2(iva));
        }
        a.hayIvaItem = true;
        a.ivaItems = a.ivaItems.add(nz(iva));
      }
    }
    if (hayCargo && !hayVenta) {
      r.error(NO_PERMITIDO, "cuerpoDocumento",
          "para usar noGravado debe existir al menos un ítem con venta gravada, exenta o no sujeta");
    }
    return a;
  }

  // ───────────────────────────── Resumen ─────────────────────────────

  private void validarResumen(Resumen s, TipoDte tipo, Acum a, ResultadoValidacion r) {
    if (s == null) {
      r.error(REQUERIDO, "resumen");
      return;
    }
    String p = "resumen.";
    BigDecimal totNoSuj = monto(r, p + "totalNoSuj", s.totalNoSuj(), 2, false);
    BigDecimal totExenta = monto(r, p + "totalExenta", s.totalExenta(), 2, false);
    BigDecimal totGravada = monto(r, p + "totalGravada", s.totalGravada(), 2, false);
    BigDecimal subVentas = monto(r, p + "subTotalVentas", s.subTotalVentas(), 2, false);
    BigDecimal dNoSuj = monto(r, p + "descuNoSuj", s.descuNoSuj(), 2, false);
    BigDecimal dExenta = monto(r, p + "descuExenta", s.descuExenta(), 2, false);
    BigDecimal dGravada = monto(r, p + "descuGravada", s.descuGravada(), 2, false);
    BigDecimal totDescu = monto(r, p + "totalDescu", s.totalDescu(), 2, false);
    BigDecimal subTotal = monto(r, p + "subTotal", s.subTotal(), 2, false);
    BigDecimal montoTotal = monto(r, p + "montoTotalOperacion", s.montoTotalOperacion(), 2, false);
    BigDecimal totNoGrav = monto(r, p + "totalNoGravado", s.totalNoGravado(), 2, true);
    BigDecimal totPagar = monto(r, p + "totalPagar", s.totalPagar(), 2, false);
    BigDecimal ivaRete = s.ivaRete1() == null ? BigDecimal.ZERO : monto(r, p + "ivaRete1", s.ivaRete1(), 2, false);
    BigDecimal reteRenta = s.reteRenta() == null ? BigDecimal.ZERO : monto(r, p + "reteRenta", s.reteRenta(), 2, false);
    BigDecimal ivaPerci = s.ivaPerci1() == null ? BigDecimal.ZERO : monto(r, p + "ivaPerci1", s.ivaPerci1(), 2, false);
    requerido(r, p + "totalLetras", s.totalLetras());
    if (s.porcentajeDescuento() != null
        && (s.porcentajeDescuento().signum() < 0 || s.porcentajeDescuento().compareTo(new BigDecimal("100")) > 0)) {
      r.error(VALOR_NO_VALIDO, p + "porcentajeDescuento", "debe estar entre 0 y 100");
    }

    // Totales por tipo de venta = suma de los ítems.
    comparar(r, p + "totalNoSuj", totNoSuj, a.noSuj);
    comparar(r, p + "totalExenta", totExenta, a.exenta);
    comparar(r, p + "totalGravada", totGravada, a.gravada);
    comparar(r, p + "subTotalVentas", subVentas, nz(totNoSuj).add(nz(totExenta)).add(nz(totGravada)));

    // Descuentos globales: no pueden exceder la venta de su tipo.
    if (dNoSuj != null && totNoSuj != null && dNoSuj.compareTo(totNoSuj) > 0) {
      r.error(CALCULO_INCORRECTO, p + "descuNoSuj", "supera el total de ventas no sujetas");
    }
    if (dExenta != null && totExenta != null && dExenta.compareTo(totExenta) > 0) {
      r.error(CALCULO_INCORRECTO, p + "descuExenta", "supera el total de ventas exentas");
    }
    if (dGravada != null && totGravada != null && dGravada.compareTo(totGravada) > 0) {
      r.error(CALCULO_INCORRECTO, p + "descuGravada", "supera el total de ventas gravadas");
    }
    BigDecimal descuGlobal = nz(dNoSuj).add(nz(dExenta)).add(nz(dGravada));
    // Total de descuento = descuentos por ítem + descuentos globales (Manual Funcional XVIII.4).
    comparar(r, p + "totalDescu", totDescu, a.descu.add(descuGlobal));
    comparar(r, p + "subTotal", subTotal, nz(subVentas).subtract(descuGlobal));
    comparar(r, p + "totalNoGravado", totNoGrav, a.noGravado);

    BigDecimal baseGravada = nz(totGravada).subtract(nz(dGravada));
    BigDecimal sumaTributos = validarTributosResumen(s, tipo, a, baseGravada, r);

    // IVA retenido 1% (CAT-006 código 22): 0 o el 1% de la venta gravada neta.
    if (ivaRete.signum() > 0 && !cerca(ivaRete, r2(baseGravada.multiply(UNO_POR_CIENTO)))) {
      r.error(CALCULO_INCORRECTO, p + "ivaRete1",
          "esperado " + f2(r2(baseGravada.multiply(UNO_POR_CIENTO))) + " (1% de la venta gravada neta), recibido "
              + f2(ivaRete));
    }

    BigDecimal montoEsperado;
    BigDecimal pagarEsperado;
    if (tipo == TipoDte.CCFE) {
      montoEsperado = nz(subTotal).add(sumaTributos).add(ivaPerci).subtract(ivaRete).subtract(reteRenta);
      pagarEsperado = nz(montoTotal).add(nz(totNoGrav));
    } else {
      montoEsperado = nz(subTotal).add(sumaTributos);
      pagarEsperado = nz(montoTotal).add(nz(totNoGrav)).subtract(ivaRete).subtract(reteRenta);
      if (s.totalIva() != null) {
        BigDecimal ivaEsperado = r2(baseGravada.multiply(IVA_INCLUIDO));
        comparar(r, p + "totalIva", s.totalIva(), ivaEsperado);
        if (a.hayIvaItem) {
          comparar(r, p + "totalIva", s.totalIva(), a.ivaItems);
        }
      }
    }
    comparar(r, p + "montoTotalOperacion", montoTotal, montoEsperado);
    comparar(r, p + "totalPagar", totPagar, pagarEsperado);

    validarPagos(s, totPagar, r);
  }

  /** Valida resumen.tributos y devuelve la suma de sus valores. */
  private BigDecimal validarTributosResumen(Resumen s, TipoDte tipo, Acum a, BigDecimal baseGravada,
      ResultadoValidacion r) {
    List<Tributo> lista = s.tributos() == null ? List.of() : s.tributos();
    Map<String, Tributo> porCodigo = new HashMap<>();
    BigDecimal suma = BigDecimal.ZERO;
    for (int i = 0; i < lista.size(); i++) {
      Tributo t = lista.get(i);
      String p = "resumen.tributos[" + i + "].";
      if (t == null || vacio(t.codigo())) {
        r.error(REQUERIDO, p + "codigo");
        continue;
      }
      if (porCodigo.putIfAbsent(t.codigo(), t) != null) {
        r.error(REPETIDOS, p + "codigo", "cada tributo debe consolidarse en una sola línea");
        continue;
      }
      if (!cat.existe("CAT-015", t.codigo())) {
        r.error(VALOR_NO_VALIDO, p + "codigo", "CAT-015");
        continue;
      }
      if (cat.tributoEnSeccion(t.codigo(), 3)) {
        r.error(TRIBUTO_INFORMATIVO, p + "codigo");
        continue;
      }
      if (!cat.tributoEnSeccion(t.codigo(), 1) || "D4".equals(t.codigo()) || "C3".equals(t.codigo())
          || ("20".equals(t.codigo()) && tipo == TipoDte.FE)) {
        r.error(NO_PERMITIDO, p + "codigo");
        continue;
      }
      if (vacio(t.descripcion())) {
        r.error(REQUERIDO, p + "descripcion");
      } else {
        String oficial = cat.valor("CAT-015", t.codigo()).orElse("");
        if (!oficial.equalsIgnoreCase(t.descripcion().trim())) {
          r.observacion(NO_COINCIDE, p + "descripcion", "el catálogo indica: " + oficial);
        }
      }
      BigDecimal valor = monto(r, p + "valor", t.valor(), 2, false);
      suma = suma.add(nz(valor));

      BigDecimal esperado = switch (t.codigo()) {
        case "20" -> r2(baseGravada.multiply(IVA));
        case "59" -> r2(a.gravada59.multiply(TURISMO));
        case "D1" -> r2(a.galonesD1.multiply(FOVIAL));
        case "C8" -> r2(a.galonesC8.multiply(COTRANS));
        default -> null;
      };
      if (valor != null && esperado != null && !cerca(valor, esperado)) {
        r.error(CALCULO_INCORRECTO, p + "valor", "esperado " + f2(esperado) + ", recibido " + f2(valor));
      }
    }
    if (tipo == TipoDte.CCFE && baseGravada.signum() > 0 && !porCodigo.containsKey("20")) {
      r.error(REQUERIDO, "resumen.tributos", "el CCFE con ventas gravadas debe incluir el tributo 20 (IVA 13%)");
    }
    // Todo tributo usado en los ítems debe consolidarse en el resumen, y viceversa.
    for (String c : a.tributos) {
      if (!porCodigo.containsKey(c)) {
        r.error(NO_COINCIDE, "resumen.tributos", "falta consolidar el tributo " + c + " usado en el cuerpo");
      }
    }
    for (String c : porCodigo.keySet()) {
      if (!a.tributos.contains(c) && !("20".equals(c) && tipo == TipoDte.CCFE && baseGravada.signum() > 0)) {
        r.error(NO_COINCIDE, "resumen.tributos", "el tributo " + c + " no se usa en ningún ítem");
      }
    }
    return suma;
  }

  /** Condición de la operación y formas de pago (Manual Funcional XIX). */
  private void validarPagos(Resumen s, BigDecimal totalPagar, ResultadoValidacion r) {
    Integer cond = s.condicionOperacion();
    if (cond == null) {
      r.error(REQUERIDO, "resumen.condicionOperacion");
    } else if (!cat.existe("CAT-016", String.valueOf(cond))) {
      r.error(VALOR_NO_VALIDO, "resumen.condicionOperacion", "CAT-016");
    }
    List<Pago> pagos = s.pagos() == null ? List.of() : s.pagos();
    if (pagos.isEmpty()) {
      if (totalPagar != null && totalPagar.signum() > 0) {
        r.error(REQUERIDO, "resumen.pagos", "debe detallar al menos una forma de pago");
      }
      return;
    }
    int conPlazo = 0;
    int sinPlazo = 0;
    BigDecimal suma = BigDecimal.ZERO;
    for (int i = 0; i < pagos.size(); i++) {
      Pago g = pagos.get(i);
      String p = "resumen.pagos[" + i + "].";
      if (g == null) {
        r.error(REQUERIDO, "resumen.pagos[" + i + "]");
        continue;
      }
      if (vacio(g.codigo())) {
        r.error(REQUERIDO, p + "codigo");
      } else if (!cat.existe("CAT-017", g.codigo())) {
        r.error(VALOR_NO_VALIDO, p + "codigo", "CAT-017");
      } else if ("01".equals(g.codigo()) && !vacio(g.referencia())) {
        r.error(NO_APLICA, p + "referencia", "billetes y monedas no lleva referencia");
      }
      BigDecimal m = monto(r, p + "montoPago", g.montoPago(), 2, false);
      if (m != null && m.signum() == 0) {
        r.error(VALOR_NO_VALIDO, p + "montoPago", "debe ser mayor a 0");
      }
      suma = suma.add(nz(m));
      if (vacio(g.plazo())) {
        sinPlazo++;
        if (g.periodo() != null) {
          r.error(NO_APLICA, p + "periodo", "sin plazo no se informa periodo");
        }
      } else {
        conPlazo++;
        if (!cat.existe("CAT-018", g.plazo())) {
          r.error(VALOR_NO_VALIDO, p + "plazo", "CAT-018");
        }
        if (g.periodo() == null || g.periodo() <= 0) {
          r.error(REQUERIDO, p + "periodo", "obligatorio cuando se informa plazo");
        }
      }
    }
    if (cond != null) {
      switch (cond) {
        case 1 -> {
          if (conPlazo > 0) {
            r.error(NO_PERMITIDO, "resumen.pagos", "condición contado no admite plazo");
          }
        }
        case 2 -> {
          if (sinPlazo > 0) {
            r.error(REQUERIDO, "resumen.pagos", "condición a crédito requiere plazo y periodo en cada pago");
          }
        }
        case 3 -> {
          if (conPlazo == 0 || sinPlazo == 0) {
            r.error(NO_PERMITIDO, "resumen.pagos",
                "condición 'otro' requiere una parte de contado y otra a crédito");
          }
        }
        default -> { }
      }
    }
    if (totalPagar != null && !cerca(suma, totalPagar)) {
      r.error(CALCULO_INCORRECTO, "resumen.pagos",
          "la suma de pagos (" + f2(suma) + ") debe igualar totalPagar (" + f2(totalPagar) + ")");
    }
  }

  // ───────────────────────────── Utilidades ─────────────────────────────

  private static boolean vacio(String s) {
    return s == null || s.isBlank();
  }

  private static void requerido(ResultadoValidacion r, String campo, String valor) {
    if (vacio(valor)) {
      r.error(REQUERIDO, campo);
    }
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private static boolean cerca(BigDecimal a, BigDecimal b) {
    return a.subtract(b).abs().compareTo(TOLERANCIA) <= 0;
  }

  private static BigDecimal r2(BigDecimal v) {
    return v.setScale(2, RoundingMode.HALF_UP);
  }

  private static String f2(BigDecimal v) {
    return r2(v).toPlainString();
  }

  /** Valida presencia, signo y máximo de decimales (8 en cuerpo, 2 en resumen). Devuelve null si falta. */
  private static BigDecimal monto(ResultadoValidacion r, String campo, BigDecimal v, int decimales,
      boolean permiteNegativo) {
    if (v == null) {
      r.error(REQUERIDO, campo);
      return null;
    }
    if (!permiteNegativo && v.signum() < 0) {
      r.error(VALOR_NO_VALIDO, campo, "no puede ser negativo");
    }
    if (v.stripTrailingZeros().scale() > decimales) {
      r.error(FORMATO_NO_VALIDO, campo, "máximo " + decimales + " decimales");
    }
    return v;
  }

  private static void comparar(ResultadoValidacion r, String campo, BigDecimal declarado, BigDecimal esperado) {
    if (declarado != null && !cerca(declarado, esperado)) {
      r.error(CALCULO_INCORRECTO, campo, "esperado " + f2(esperado) + ", recibido " + f2(declarado));
    }
  }
}
