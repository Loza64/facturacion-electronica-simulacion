package com.server.app.dte.dto;

import java.util.List;

/** Respuesta del servicio de recepción (misma forma que "Respuesta" en el Manual Técnico 4.2.1). */
public record DteRespuesta(
    int version,
    String ambiente,
    int versionApp,
    String estado,
    String codigoGeneracion,
    String selloRecibido,
    String fhProcesamiento,
    String clasificaMsg,
    String codigoMsg,
    String descripcionMsg,
    List<String> observaciones) {

  public static final String PROCESADO = "PROCESADO";
  public static final String RECHAZADO = "RECHAZADO";

  public boolean procesado() {
    return PROCESADO.equals(estado);
  }
}
