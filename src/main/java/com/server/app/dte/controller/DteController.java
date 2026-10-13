package com.server.app.dte.controller;

import com.server.app.common.exceptions.NotFoundException;
import com.server.app.dte.dto.DteRespuesta;
import com.server.app.dte.service.DteSimulacionService;
import com.server.app.dte.service.DteSimulacionService.ArchivoDte;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulación del Sistema de Transmisión de DTE. Requiere JWT como el resto de la API (SecurityConfig).
 *
 * <p>POST /api/dte → equivale a /fesv/recepciondte: 200 si PROCESADO, 400 si RECHAZADO (misma forma de respuesta
 * que el manual). GET /api/dte/{codigoGeneracion} → equivale a consultadte.
 */
@RestController
@RequestMapping("/api/dte")
public class DteController {

  private final DteSimulacionService service;

  public DteController(DteSimulacionService service) {
    this.service = service;
  }

  /** Recibe el JSON del DTE tal cual (se lee como texto para responder con código 97/98 si viene mal formado). */
  @PostMapping
  public ResponseEntity<DteRespuesta> recibir(@RequestBody(required = false) String json) {
    DteRespuesta r = service.procesar(json);
    return ResponseEntity.status(r.procesado() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(r);
  }

  @GetMapping("/{codigoGeneracion}")
  public ResponseEntity<DteRespuesta> consultar(@PathVariable String codigoGeneracion) {
    return ResponseEntity.ok(service.consultar(codigoGeneracion)
        .orElseThrow(() -> new NotFoundException("No existe un DTE con el código de generación " + codigoGeneracion)));
  }

  @GetMapping("/{codigoGeneracion}/archivo")
  public ResponseEntity<ArchivoDte> archivo(@PathVariable String codigoGeneracion) {
    return ResponseEntity.ok(service.archivo(codigoGeneracion)
        .orElseThrow(() -> new NotFoundException("No existe un DTE con el código de generación " + codigoGeneracion)));
  }
}
