package com.server.app.dte.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estructura (simplificada) de la Factura Electrónica (01) y del Comprobante de Crédito Fiscal Electrónico (03).
 *
 * <p>Los nombres de campo siguen el vocabulario de los manuales y el JSON Schema público de DTE v1. Si el Anexo II de
 * la Normativa v2.0 renombra algún campo, se ajusta aquí y en {@code DteValidator}. Se ignoran las propiedades
 * desconocidas (firmaElectronica, selloRecibido, apendice, etc.).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DteDocumento(
    Identificacion identificacion,
    Emisor emisor,
    Receptor receptor,
    List<Item> cuerpoDocumento,
    Resumen resumen) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Identificacion(
      Integer version,
      String ambiente,
      String tipoDte,
      String numeroControl,
      String codigoGeneracion,
      Integer tipoModelo,
      Integer tipoOperacion,
      Integer tipoContingencia,
      String motivoContin,
      String fecEmi,
      String horEmi,
      String tipoMoneda) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Direccion(String departamento, String municipio, String complemento) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Emisor(
      String nit,
      String nrc,
      String nombre,
      String codActividad,
      String descActividad,
      String nombreComercial,
      String tipoEstablecimiento,
      Direccion direccion,
      String telefono,
      String correo,
      String codEstable,
      String codPuntoVenta) {
  }

  /** Para FE usa tipoDocumento/numDocumento; para CCFE usa nit/nrc. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Receptor(
      String tipoDocumento,
      String numDocumento,
      String nit,
      String nrc,
      String nombre,
      String codActividad,
      String descActividad,
      Direccion direccion,
      String telefono,
      String correo) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Item(
      Integer numItem,
      Integer tipoItem,
      String numeroDocumento,
      BigDecimal cantidad,
      String codigo,
      String codTributo,
      Integer uniMedida,
      String descripcion,
      BigDecimal precioUni,
      BigDecimal montoDescu,
      BigDecimal ventaNoSuj,
      BigDecimal ventaExenta,
      BigDecimal ventaGravada,
      List<String> tributos,
      BigDecimal psv,
      BigDecimal noGravado,
      BigDecimal ivaItem) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Tributo(String codigo, String descripcion, BigDecimal valor) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Pago(String codigo, BigDecimal montoPago, String referencia, String plazo, Integer periodo) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Resumen(
      BigDecimal totalNoSuj,
      BigDecimal totalExenta,
      BigDecimal totalGravada,
      BigDecimal subTotalVentas,
      BigDecimal descuNoSuj,
      BigDecimal descuExenta,
      BigDecimal descuGravada,
      BigDecimal porcentajeDescuento,
      BigDecimal totalDescu,
      List<Tributo> tributos,
      BigDecimal subTotal,
      BigDecimal ivaPerci1,
      BigDecimal ivaRete1,
      BigDecimal reteRenta,
      BigDecimal montoTotalOperacion,
      BigDecimal totalNoGravado,
      BigDecimal totalPagar,
      String totalLetras,
      BigDecimal totalIva,
      BigDecimal saldoFavor,
      Integer condicionOperacion,
      List<Pago> pagos,
      String numPagoElectronico) {
  }
}
