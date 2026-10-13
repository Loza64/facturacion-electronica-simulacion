package com.server.app.dte.validation;

/**
 * Códigos de respuesta del servicio de recepción (Manual Técnico v2.0, "Códigos de Respuesta Servicio de
 * Recepción"). Sólo se incluyen los que el simulador puede producir.
 */
public enum CodigoMensaje {
  RECIBIDO(1, "RECIBIDO"),
  RECIBIDO_CON_OBSERVACIONES(2, "RECIBIDO CON OBSERVACIONES"),
  VALOR_NO_VALIDO(3, "VALOR NO VALIDO"),
  YA_EXISTE(4, "YA EXISTE UN REGISTRO CON ESE VALOR"),
  REQUERIDO(5, "DEBE PROPORCIONAR UN VALOR"),
  FORMATO_NO_VALIDO(6, "FORMATO NO VALIDO"),
  NO_PERMITIDO(7, "VALOR NO ES PERMITIDO"),
  NIT_NO_EXISTE(9, "NIT CONTRIBUYENTE NO EXISTE"),
  FECHA_INCORRECTA(17, "FECHA NO ES CORRECTA"),
  FECHA_FUERA_PLAZO(18, "FECHA FUERA DE PLAZO PERMITIDO"),
  ERROR_CORRELATIVO(19, "ERROR EN CORRELATIVO"),
  CALCULO_INCORRECTO(20, "CALCULO INCORRECTO"),
  NO_APLICA(24, "VALOR EN CAMPO NO APLICA"),
  REPETIDOS(25, "VALORES REPETIDOS"),
  NO_COINCIDE(27, "DATO NO COINCIDE CON DTE"),
  NIT_INVALIDO(29, "NIT DE CONTRIBUYENTE NO ES VALIDO"),
  DIFIERE_FECHA_ENVIO(32, "DIFIERE DE LA FECHA DE ENVIO"),
  EXCEDE_HOLGURA(34, "EXCEDE HOLGURA DE 30 MINUTOS"),
  TIPO_NO_AUTORIZADO(95, "CONTRIBUYENTE NO AUTORIZADO PARA EMITIR TIPO DE DTE"),
  NO_CUMPLE_NORMATIVA(96, "DOCUMENTO NO CUMPLE CON NORMATIVA DE CUMPLIMIENTO"),
  SIN_DATOS(97, "NO SE HA PROPORCIONADO DATOS PARA VALIDAR"),
  JSON_INVALIDO(98, "DATOS NO CUMPLEN ESTANDAR JSON"),
  TRIBUTO_INFORMATIVO(115, "ERROR NO SE PERMITEN TRIBUTOS DE USO INFORMATIVO"),
  FECHA_MENOR_2000(116, "FECHA MENOR A 01-01-2000");

  private final int codigo;
  private final String descripcion;

  CodigoMensaje(int codigo, String descripcion) {
    this.codigo = codigo;
    this.descripcion = descripcion;
  }

  public int codigo() {
    return codigo;
  }

  public String descripcion() {
    return descripcion;
  }

  /** Formato de tres dígitos usado en el campo codigoMsg ("001", "003"...). */
  public String codigoMsg() {
    return String.format("%03d", codigo);
  }
}
