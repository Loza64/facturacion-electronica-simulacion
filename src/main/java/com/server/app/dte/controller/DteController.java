package com.server.app.dte.controller;

import com.server.app.common.exceptions.NotFoundException;
import com.server.app.dte.dto.DteDocumento;
import com.server.app.dte.dto.DteRespuesta;
import com.server.app.dte.service.DteSimulacionService;
import com.server.app.dte.service.DteSimulacionService.ArchivoDte;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @Operation(
      summary = "Recibir un Documento Tributario Electrónico",
      description = "Valida el DTE recibido y devuelve el resultado de la simulación.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = false,
          description = "Documento DTE en formato JSON.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = DteDocumento.class),
              examples = @ExampleObject(
                  name = "Factura electrónica",
                  summary = "Ejemplo de DTE tipo 01",
                  value = """
                      {
                        "identificacion": {
                          "version": 1,
                          "ambiente": "00",
                          "tipoDte": "01",
                          "numeroControl": "DTE-01-M001P001-000000000000001",
                          "codigoGeneracion": "3F2A9C1E-7B4D-4E8A-9C21-5D6E7F8091AB",
                          "tipoModelo": 1,
                          "tipoOperacion": 1,
                          "tipoContingencia": null,
                          "motivoContin": null,
                          "fecEmi": "2026-10-12",
                          "horEmi": "08:00:00",
                          "tipoMoneda": "USD"
                        },
                        "emisor": {
                          "nit": "06140000000011",
                          "nrc": "123456",
                          "nombre": "Comercial Ejemplo, S.A. de C.V.",
                          "codActividad": "47111",
                          "descActividad": "Venta en supermercados",
                          "nombreComercial": "Super Ejemplo",
                          "tipoEstablecimiento": "01",
                          "direccion": {
                            "departamento": "06",
                            "municipio": "23",
                            "complemento": "Colonia Escalón, calle principal #123"
                          },
                          "telefono": "22223333",
                          "correo": "ventas@ejemplo.com",
                          "codEstable": "M001",
                          "codPuntoVenta": "P001"
                        },
                        "receptor": {
                          "tipoDocumento": "13",
                          "numDocumento": "01234567-8",
                          "nombre": "Juan Pérez",
                          "correo": "juan@correo.com"
                        },
                        "cuerpoDocumento": [
                          {
                            "numItem": 1,
                            "tipoItem": 1,
                            "cantidad": 2,
                            "codigo": "ARR-001",
                            "codTributo": null,
                            "uniMedida": 59,
                            "descripcion": "Arroz blanco 5 lb",
                            "precioUni": 5.65,
                            "montoDescu": 0,
                            "ventaNoSuj": 0,
                            "ventaExenta": 0,
                            "ventaGravada": 11.30,
                            "tributos": null,
                            "psv": 0,
                            "noGravado": 0,
                            "ivaItem": 1.30
                          },
                          {
                            "numItem": 2,
                            "tipoItem": 1,
                            "cantidad": 1,
                            "codigo": "ACE-003",
                            "codTributo": null,
                            "uniMedida": 59,
                            "descripcion": "Aceite vegetal 3 L",
                            "precioUni": 20.00,
                            "montoDescu": 0,
                            "ventaNoSuj": 0,
                            "ventaExenta": 0,
                            "ventaGravada": 20.00,
                            "tributos": null,
                            "psv": 0,
                            "noGravado": 0,
                            "ivaItem": 2.30
                          }
                        ],
                        "resumen": {
                          "totalNoSuj": 0,
                          "totalExenta": 0,
                          "totalGravada": 31.30,
                          "subTotalVentas": 31.30,
                          "descuNoSuj": 0,
                          "descuExenta": 0,
                          "descuGravada": 0,
                          "porcentajeDescuento": 0,
                          "totalDescu": 0,
                          "tributos": null,
                          "subTotal": 31.30,
                          "ivaRete1": 0,
                          "reteRenta": 0,
                          "montoTotalOperacion": 31.30,
                          "totalNoGravado": 0,
                          "totalPagar": 31.30,
                          "totalLetras": "TREINTA Y UN 30/100 DÓLARES",
                          "totalIva": 3.60,
                          "saldoFavor": 0,
                          "condicionOperacion": 1,
                          "pagos": [
                            {
                              "codigo": "01",
                              "montoPago": 31.30,
                              "referencia": null,
                              "plazo": null,
                              "periodo": null
                            }
                          ],
                          "numPagoElectronico": null
                        }
                      }
                      """))))
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
