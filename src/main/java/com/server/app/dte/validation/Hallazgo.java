package com.server.app.dte.validation;

/** Un error u observación detectado durante la validación. */
public record Hallazgo(CodigoMensaje codigo, String campo, String detalle) {

  /** Formato del manual: "[campo] DESCRIPCION", más un detalle opcional. */
  public String mensaje() {
    String base = "[" + campo + "] " + codigo.descripcion();
    return detalle == null || detalle.isBlank() ? base : base + " - " + detalle;
  }
}
