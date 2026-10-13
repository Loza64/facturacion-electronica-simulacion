package com.server.app.dte.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Acumula los hallazgos. Los errores provocan el rechazo del DTE; las observaciones se informan pero no afectan la
 * recepción (Manual Técnico, nota del servicio de recepción).
 */
public final class ResultadoValidacion {

  private final List<Hallazgo> errores = new ArrayList<>();
  private final List<Hallazgo> observaciones = new ArrayList<>();

  public void error(CodigoMensaje codigo, String campo) {
    error(codigo, campo, null);
  }

  public void error(CodigoMensaje codigo, String campo, String detalle) {
    errores.add(new Hallazgo(codigo, campo, detalle));
  }

  public void observacion(CodigoMensaje codigo, String campo, String detalle) {
    observaciones.add(new Hallazgo(codigo, campo, detalle));
  }

  public boolean valido() {
    return errores.isEmpty();
  }

  public List<Hallazgo> errores() {
    return List.copyOf(errores);
  }

  public List<Hallazgo> observaciones() {
    return List.copyOf(observaciones);
  }
}
