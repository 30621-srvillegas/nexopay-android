# Evidencias de NexoPay con Postman y Render

## Importar

1. Abre Postman Desktop y pulsa **Import > Files**.
2. Selecciona `NexoPay-Render.postman_collection.json`, en esta carpeta.
3. Abre la colección **NexoPay — Render — Evidencias de endpoints**.
4. Todos los nombres incluyen el método, endpoint y código HTTP esperado entre corchetes.

La colección incluye los 12 endpoints HTTP del backend, comprobaciones adicionales y las instrucciones para el endpoint WebSocket. No necesitas copiar tokens: los scripts del login y refresh actualizan las variables de la colección. La autorización Bearer se hereda de la colección, excepto en las peticiones que requieren No Auth.

No hay que levantar el backend local. El servidor usado es `https://nexopay-backend-xnr5.onrender.com`.

## Ejecutar y tomar capturas

Ejecuta las solicitudes individualmente, en orden numérico, pulsando **Send**. Para las capturas utiliza **Windows + Shift + S**. Muestra el nombre, método, URL, JSON enviado si corresponde, estado HTTP y respuesta en formato Pretty/JSON. La pestaña **Test Results** (o **Scripts > resultados**, según versión) muestra las comprobaciones automáticas.

Los estados 401, 409 y 422 son resultados correctos en las solicitudes que los esperan. En el logout, 204 sin contenido es correcto.

| Request | Archivo de captura sugerido | Evidencia |
|---|---|---|
| 01 | `01-health.png` | Backend activo, `ok: true` |
| 02 | `02-login.png` | Inicio de sesión y datos de Ana |
| 03 | `03-cuenta.png` | Cuenta autenticada |
| 04 | `04-saldo-inicial.png` | Saldo antes de operar |
| 05 | `05-deposito.png` | Abono S/ 100, DEPOSIT |
| 06 | `06-idempotencia.png` | `idempotentReplay: true`, sin doble abono |
| 07 | `07-retiro.png` | Descuento S/ 20, WITHDRAWAL |
| 08 | `08-transferencia.png` | Envío S/ 25 a Luis y referenceId |
| 09 | `09-servicio.png` | Pago S/ 15, Luz, LUZ-001 |
| 10 | `10-saldo-final.png` | Inicial + S/ 40 |
| 11 | `11-historial.png` | Movimientos guardados |
| 12 | `12-ticket.png` | Ticket y vigencia de 30 segundos |
| WebSocket | `13-websocket.png` | Conexión abierta y connection.ready/pong |
| 13 | `14-refresh.png` | Renovación de tokens |
| 14 | `15-token-renovado.png` | Consulta con el nuevo access token |
| 15 | `16-sin-token.png` | 401 por falta de token |
| 16 | `17-importe-invalido.png` | 422 por importe negativo |
| 17 | `18-sin-idempotencia.png` | 422 por falta de clave |
| 18 | `19-conflicto.png` | 409 por clave repetida con otro cuerpo |
| 19 | `20-misma-cuenta.png` | 422 al transferir a sí mismo |
| 20 | `21-logout.png` | 204 sin contenido |
| 21 | `22-sesion-revocada.png` | 401 al renovar después del logout |

Haz 05 y 06 consecutivamente. Las validaciones de saldo suponen que nadie más opera sobre Ana durante la prueba y que cada solicitud nueva se envía una vez. Los valores dinámicos `{{$guid}}` generan una clave nueva: repetir Send en una operación nueva sí vuelve a mover dinero. El request 06 reutiliza la clave guardada por 05.

El login tiene un límite de 5 solicitudes por 15 minutos. No hace falta repetirlo para cada endpoint. El access token dura normalmente 15 minutos; si vence antes de terminar, ejecuta 13 para renovarlo y continúa. Evita otras sesiones de prueba simultáneas sobre Ana.

## WebSocket: GET /ws con cambio de protocolo

Las colecciones HTTP v2.1 no representan una solicitud WebSocket ejecutable como las peticiones REST. Por eso la conexión se crea desde **New > WebSocket**.

1. Prepara una pestaña **New > WebSocket**.
2. Ejecuta el request 12. Se guarda `wsTicket` en las variables de la colección.
3. Copia el valor `ticket` de la respuesta y reemplaza `PEGA_EL_TICKET` en:

   ```text
   wss://nexopay-backend-xnr5.onrender.com/ws?ticket=PEGA_EL_TICKET
   ```

4. Pulsa **Connect** antes de 30 segundos. El ticket es de un solo uso.
5. Verifica el evento `connection.ready`, con cuenta y saldo. El handshake correcto es 101 Switching Protocols.
6. En mensajes selecciona texto/JSON, pega y envía:

   ```json
   {
     "type": "ping"
   }
   ```

7. Verifica el evento `pong` y captura la conexión y los mensajes.
8. Desconecta al terminar. Si venció el ticket o ya fue consumido, ejecuta 12 para obtener otro.

La pestaña WebSocket puede no compartir el ámbito de variables de la colección HTTP; copiar el ticket explícitamente evita ese problema.

### Evidencia de evento financiero en tiempo real

Para capturar `balance.updated` sin alterar la comparación del saldo final, puedes abrir el socket de Ana antes del depósito 05: ejecuta 12 anticipadamente, conecta y luego continúa con 05. En la pestaña del socket aparecerá `balance.updated`.

Para mostrar una transferencia entrante en Android, abre la app con **Luis, 10020002 / Nexo#2026**, conectada a **Render**, antes de ejecutar 08. El token HTTP de la colección sigue siendo el de Ana. Captura el cambio de saldo/historial de Luis sin recargar.

## Evidencia adicional: fondos insuficientes

Hazla después de terminar la comparación de saldo y antes del logout:

1. Consulta el saldo con GET `/api/v1/operations/balance`.
2. Crea una solicitud HTTP fuera de esta colección, con método POST y nombre `POST /api/v1/operations/withdraw — Fondos insuficientes`, para hacer esta comprobación manual sin los scripts de la colección.
3. Usa la URL `https://nexopay-backend-xnr5.onrender.com/api/v1/operations/withdraw` y Authorization Bearer con el access token actual.
4. Agrega `Content-Type: application/json` e `Idempotency-Key: error-fondos-001`.
5. Envía un importe mayor al saldo actual, pero no mayor a 10000000 centavos. Por ejemplo, si el saldo es menor a S/ 100000:

   ```json
   {
     "amountCents": 10000000
   }
   ```

6. Esperado: **400 Bad Request**, `error.code: insufficient_funds`. Si el saldo ya es igual o superior a ese importe, este ejemplo no sirve para demostrar fondos insuficientes; no lo envíes como prueba negativa.

## Ejecución completa con Runner

Para un resumen automático, selecciona **Run collection**, una iteración, orden original y ejecución secuencial. Esto hace operaciones reales. Primero despierta Render con 01. El Runner HTTP no abre el WebSocket ni sustituye sus capturas. Para evidencias individuales es más práctico usar Send y capturar cada respuesta.

## Alcance de la verificación

El archivo fue preparado a partir del código local del backend. Los resultados reales de Render se comprueban al enviar las solicitudes. Si el despliegue usa otra versión o Render se reinicia durante la prueba, las respuestas pueden variar. No se han enviado operaciones a Render al generar estos archivos.
