package com.server.app.dte.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DteAlmacenadoDto(
    String codigoGeneracion,
    String nitEmisor,
    String numeroControl,
    JsonNode documento,
    DteRespuesta respuesta) {
}
