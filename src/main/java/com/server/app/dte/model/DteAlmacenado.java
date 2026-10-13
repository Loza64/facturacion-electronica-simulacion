package com.server.app.dte.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "dte_almacenado",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_dte_codigo_generacion", columnNames = "codigo_generacion"),
        @UniqueConstraint(
            name = "uk_dte_emisor_numero_control",
            columnNames = {"nit_emisor", "numero_control"})
    })
public class DteAlmacenado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "codigo_generacion", nullable = false, length = 36)
  private String codigoGeneracion;

  @Column(name = "nit_emisor", nullable = false, length = 14)
  private String nitEmisor;

  @Column(name = "numero_control", nullable = false, length = 31)
  private String numeroControl;

  @Column(name = "documento_json", nullable = false, columnDefinition = "text")
  private String documentoJson;

  @Column(name = "respuesta_json", nullable = false, columnDefinition = "text")
  private String respuestaJson;

  @Column(nullable = false, columnDefinition = "text")
  private String firma;

  protected DteAlmacenado() {
  }

  public DteAlmacenado(
      String codigoGeneracion,
      String nitEmisor,
      String numeroControl,
      String documentoJson,
      String respuestaJson,
      String firma) {
    this.codigoGeneracion = codigoGeneracion;
    this.nitEmisor = nitEmisor;
    this.numeroControl = numeroControl;
    this.documentoJson = documentoJson;
    this.respuestaJson = respuestaJson;
    this.firma = firma;
  }

  public String getCodigoGeneracion() {
    return codigoGeneracion;
  }

  public String getDocumentoJson() {
    return documentoJson;
  }

  public String getRespuestaJson() {
    return respuestaJson;
  }

  public String getFirma() {
    return firma;
  }
}
