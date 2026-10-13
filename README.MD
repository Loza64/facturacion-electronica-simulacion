## 14. Módulo DTE — Simulación de facturación electrónica

Módulo independiente que simula la recepción de **Documentos Tributarios Electrónicos (DTE)** del Ministerio de Hacienda de El Salvador. Recibe el JSON de un DTE, lo valida con las reglas de los manuales oficiales y responde con el mismo formato del servicio de recepción real (estado, sello de recepción y mensajes).

**Paquete:** `com.server.app.dte` · No agrega dependencias nuevas ni modifica clases existentes.

### 14.1 Alcance

| Incluido | No incluido |
|---|---|
| Factura Electrónica (`01`) y Comprobante de Crédito Fiscal Electrónico (`03`) | Los demás tipos de DTE (04–09, 11, 14, 15). Se rechazan con el código 95 |
| Validación completa de estructura, catálogos y cálculos | Eventos: invalidación, contingencia, retorno y operaciones especiales |
| Sello de recepción y firma simulados | Firma real con certificado, conexión con Hacienda y persistencia en base de datos |

Es una simulación. Los DTE aceptados viven **en memoria** y se pierden al reiniciar la aplicación.

### 14.2 Estructura

```
dte/
  controller/   → DteController (endpoints REST)
  service/      → DteSimulacionService (flujo de recepción, duplicados, sello, firma, consulta)
  validation/   → DteValidator (reglas), CodigoMensaje, Hallazgo, ResultadoValidacion
  catalog/      → CatalogoService (carga y consulta de catálogos)
  dto/          → DteDocumento (estructura del DTE), DteRespuesta (respuesta de recepción)
resources/dte/catalogos.json → CAT-001 a CAT-033 (generado desde "Catálogos – Facturación Electrónica v1.1")
```

### 14.3 Seguridad

Los endpoints de `/api/dte` quedan protegidos por el `SecurityConfig` existente (`anyRequest().authenticated()`): exigen `Authorization: Bearer <token>`. No usan `@PreAuthorize`, por lo que no requieren un permiso específico; basta con un usuario autenticado.

### 14.4 Endpoints

| Método | Ruta | Equivale a | Descripción |
|---|---|---|---|
| POST | `/api/dte` | `/fesv/recepciondte` | Valida el DTE. Responde 200 si `PROCESADO` y 400 si `RECHAZADO` |
| GET | `/api/dte/{codigoGeneracion}` | `consultadte` | Devuelve la respuesta de recepción de un DTE aceptado. 404 si no existe |
| GET | `/api/dte/{codigoGeneracion}/archivo` | Archivo DTE (Manual Técnico 6.5) | Devuelve el JSON original con `firmaElectronica` y `selloRecibido`, más la URL del código QR |

El POST recibe el JSON del DTE tal cual (`Content-Type: application/json`). Si el cuerpo viene vacío o mal formado, responde `RECHAZADO` con los códigos 97 o 98 en lugar de un error genérico.

### 14.5 Validaciones

**Identificación**
- `ambiente` (CAT-001): el simulador solo acepta `00` (pruebas).
- `tipoDte` (CAT-002): soporta `01` y `03`; otros tipos del catálogo devuelven el código 95.
- `numeroControl`: 31 caracteres con formato `DTE-TT-XNNNPNNN-000000000000001`, coherente con `tipoDte` y con `codEstable` + `codPuntoVenta`.
- `codigoGeneracion`: UUID v4 en mayúsculas.
- `tipoModelo`, `tipoOperacion` y `tipoContingencia` (CAT-003, 004, 005). En contingencia el modelo debe ser 2 y `motivoContin` es obligatorio cuando el tipo es 5.
- Fecha y hora: holguras del Manual Técnico 3.1 (un día posterior, salvo el último día del mes, y 30 minutos hacia adelante). Una fecha distinta a la de envío genera una observación, no un rechazo.

**Emisor y receptor**
- NIT (14 dígitos), NRC, actividad económica (CAT-019), tipo de establecimiento (CAT-009), teléfono y correo.
- Departamento y municipio (CAT-012 y CAT-013): el municipio debe pertenecer al departamento, porque los códigos se repiten entre departamentos.
- Factura: el receptor es opcional; si se identifica, `tipoDocumento` (CAT-022), `numDocumento` y `nombre` van juntos.
- Comprobante de Crédito Fiscal: receptor completo (NIT, NRC, nombre, actividad, dirección y correo).

**Cuerpo del documento**
- Correlativo de `numItem`, `tipoItem` (CAT-011) y `uniMedida` (CAT-014).
- `ventaNoSuj + ventaExenta + ventaGravada = cantidad × precioUni − montoDescu`, con tolerancia de ±0.01 y hasta 8 decimales.
- Cargos y abonos que no afectan la base imponible (`noGravado`) y los ítems de tipo 4 con su `codTributo`.
- Tributos del ítem (CAT-015, sección 1). El tributo `20` es obligatorio en ventas gravadas del CCFE y no aplica en Factura. Los tributos de uso informativo (sección 3) devuelven el código 115.

**Resumen**
- Totales por tipo de venta, descuentos globales, `totalDescu`, `subTotal`, `montoTotalOperacion` y `totalPagar`, con hasta 2 decimales y tolerancia de ±0.01.
- Tributos consolidados y su valor: IVA 13 %, turismo 5 %, FOVIAL ($0.20 por galón) y COTRANS ($0.10 por galón).
- IVA retenido: debe ser 0 o el 1 % de la venta gravada neta.
- Condición de la operación y formas de pago (CAT-016, 017 y 018): la suma de pagos debe igualar `totalPagar`; las reglas de plazo dependen de si es contado, crédito u otro.

**Estado**
- No se permite repetir el `codigoGeneracion` ni el número de control del mismo emisor (código 4).

### 14.6 Respuesta

Misma forma que el servicio de recepción del manual:

```json
{
  "version": 2,
  "ambiente": "00",
  "versionApp": 2,
  "estado": "PROCESADO",
  "codigoGeneracion": "3F2A9C1E-7B4D-4E8A-9C21-5D6E7F8091AB",
  "selloRecibido": "2026A86193A827084BC898A2452E93922B921XOG",
  "fhProcesamiento": "12/10/2026 10:40:00",
  "clasificaMsg": "10",
  "codigoMsg": "001",
  "descripcionMsg": "RECIBIDO",
  "observaciones": []
}
```

| Caso | HTTP | `estado` | `clasificaMsg` | `codigoMsg` |
|---|---|---|---|---|
| Aceptado | 200 | `PROCESADO` | `10` | `001` |
| Aceptado con observaciones | 200 | `PROCESADO` | `10` | `002` |
| Rechazado | 400 | `RECHAZADO` | `20` | Código del primer error (por ejemplo `020`) |

En un rechazo, `selloRecibido` es `null` y `observaciones` lista **todos** los errores con el formato `[campo] DESCRIPCIÓN - detalle`, por ejemplo:

```
[resumen.totalPagar] CALCULO INCORRECTO - esperado 31.30, recibido 40.00
```

Los códigos son los de la tabla "Códigos de Respuesta Servicio de Recepción" del Manual Técnico v2.0.

### 14.7 Configuración opcional

Todas las propiedades tienen valor por defecto; no es necesario agregar nada a `application.properties`.

| Propiedad | Por defecto | Descripción |
|---|---|---|
| `dte.simulacion.solo-ambiente-pruebas` | `true` | Rechaza el ambiente `01` (producción) |
| `dte.simulacion.versiones-permitidas` | `1,2,3` | Versiones del JSON del DTE aceptadas |
| `dte.simulacion.nits-registrados` | vacío | Lista de NIT separada por comas; si se llena, otro NIT devuelve el código 9. Vacío acepta cualquier NIT con formato válido |
| `dte.simulacion.zona-horaria` | `America/El_Salvador` | Zona horaria para validar fechas y generar `fhProcesamiento` |
| `dte.simulacion.firma-secreto` | valor de desarrollo | Clave HMAC de la firma simulada. Cámbiala si lo despliegas |

### 14.8 Ejemplo de uso

```powershell
# 1. Login (devuelve el token en el campo "token")
curl -X POST http://localhost:3000/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"TU_USUARIO","password":"TU_PASSWORD"}'

# 2. Enviar el DTE
curl -i -X POST http://localhost:3000/api/dte `
  -H "Authorization: Bearer TU_TOKEN" `
  -H "Content-Type: application/json" `
  --data "@factura-01.json"

# 3. Consultar y obtener el archivo con sello
curl http://localhost:3000/api/dte/3F2A9C1E-7B4D-4E8A-9C21-5D6E7F8091AB -H "Authorization: Bearer TU_TOKEN"
curl http://localhost:3000/api/dte/3F2A9C1E-7B4D-4E8A-9C21-5D6E7F8091AB/archivo -H "Authorization: Bearer TU_TOKEN"
```

Ajusta el puerto según tu variable `SERVER_PORT`. Los JSON de ejemplo (`factura-01.json` y `ccf-03.json`) pasan todas las validaciones.

### 14.9 Limitaciones conocidas

- Los nombres de campo del JSON siguen el vocabulario de los manuales y el esquema público de DTE v1. Deben verificarse contra el Anexo II de la Normativa de Cumplimiento v2.0, que no estaba entre los documentos analizados. Los cambios se concentran en `DteDocumento` y `DteValidator`.
- No se valida el texto de `totalLetras` (solo que exista), el distrito (CAT-008), los documentos relacionados ni la firma real.
- La fórmula de `totalPagar` en Factura es una inferencia; en el CCFE sigue el ejemplo del Manual Funcional.
- El estado es en memoria y no se comparte entre instancias.