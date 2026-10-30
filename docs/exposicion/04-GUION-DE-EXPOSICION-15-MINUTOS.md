# 4. Guion de exposición y demostración — máximo 15 minutos

> **Meta:** demostrar que entendemos la aplicación, el recorrido de una operación, REST, WebSocket, persistencia y seguridad. Preparar **14 minutos de contenido y 1 minuto de margen**. No intentar leer los otros tres documentos durante la presentación: son material de estudio y consulta.

## 1. Mensaje central que todos deben dominar

> “NexoPay es una billetera de demostración distribuida: varios clientes Android se comunican con un backend Fastify publicado en Render. El servidor calcula los saldos de forma centralizada en SQLite. REST procesa las operaciones, WebSocket informa cambios en tiempo real y Room con SQLCipher conserva el historial cifrado en cada dispositivo.”

No se está demostrando una base financiera distribuida entre celulares, una conexión con bancos o un servicio comercial de facturación.

## 2. Preparación antes de iniciar el cronómetro

### Dispositivos y herramientas

- Instalar la APK **prodRelease** actual en el celular/emulador; debe usar Render.
- Abrir la app con **Ana: `10020001` / `Nexo#2026`**.
- Usar Postman como segundo cliente, autenticado con **Luis: `10020002` / `Nexo#2026`**.
- Si hay dos celulares, se puede abrir Luis en el segundo, incluso con otra red; no es obligatorio si se usa Postman como segundo cliente.
- Mostrar el celular mediante la proyección disponible. Dejar la ventana lista antes de comenzar.
- Abrir Android Studio, VS Code y Postman con pestañas ya seleccionadas. No esperar a compilar, descargar dependencias o instalar durante los 15 minutos.
- Abrir `/health` y esperar `ok: true`; no redesplegar Render durante la exposición.
- Asegurar saldo suficiente. Usar montos pequeños. No depender de los saldos originales del seed.

### Pestañas de código que deben estar listas

| Herramienta | Archivo | Qué mostrar |
|---|---|---|
| VS Code | `src/routes/operations.js` | Transferencia: saldo real, descuento y abono. |
| VS Code | `src/security/idempotency.js` | Resultado guardado y transacción SQLite. |
| VS Code | `src/routes/realtime.js` y `src/realtime/hub.js` | Autorización del socket y envío por cuenta. |
| VS Code | `src/db.js` | Tablas y configuración SQLite. |
| VS Code | `src/security/passwords.js`, `tokens.js`, `authGuard.js` | Seguridad del servidor. |
| Android Studio | `data/remote/NexoApi.kt` | Contrato Retrofit. |
| Android Studio | `data/repository/WalletRepository.kt` | Llamada REST y guardado local. |
| Android Studio | `ui/dashboard/DashboardViewModel.kt` | Corrutinas, bloqueo y eventos. |
| Android Studio | `data/remote/WalletSocketManager.kt` | Recepción de `transfer.received`. |
| Android Studio | `data/local/DatabaseProvider.kt` y `security/SecureStore.kt` | SQLCipher y Keystore. |

Las rutas Kotlin anteriores parten de `app/src/main/java/com/uc1/nexopay/`. No hace falta proyectar todos los archivos: algunos quedan preparados para preguntas.

### Postman sin improvisación

Preparar solicitudes independientes o una colección específica de defensa, con variables:

```text
baseUrl   = https://nexopay-backend-xnr5.onrender.com
tokenAna  = access token obtenido con el login de Ana
tokenLuis = access token obtenido con el login de Luis
keyDemo   = una clave nueva para la transferencia de demostración
```

Ejemplo de clave: `defensa-transferencia-001`. Debe ser nueva para una operación nueva; cambiar el sufijo al ensayar otra vez. Para demostrar idempotencia, conservar exactamente la misma clave y el mismo cuerpo.

**Atención a la colección de comprobaciones creada en el proyecto:** su login y sus scripts están preparados para Ana. No cambiar ese login a Luis esperando que todas sus validaciones de Ana sigan pasando. Para la defensa, usar solicitudes independientes para Luis o adaptar una copia de la colección con variables separadas.

El access token dura normalmente 15 minutos. Obtenerlo poco antes de iniciar o ejecutar el login de Luis al comenzar la demostración de tiempo real. Si se renueva, actualizar el token correspondiente. No repetir logins innecesariamente: el endpoint tiene un límite de 5 solicitudes por 15 minutos.

### Evidencias de respaldo

Dejar accesibles capturas o un video breve de:

- Cuatro operaciones exitosas.
- Transferencia que actualiza la app sin recargar.
- Historial conservado al reabrir con sesión guardada.
- Historial consultable sin red.
- Respuestas 401, 422, 409 y una operación idempotente.
- Ticket y eventos de una conexión WebSocket.
- Resultados de pruebas automatizadas.

Si se muestra una captura o video, decir que es evidencia de una ejecución previa. No presentarlo como una acción que acaba de realizarse en vivo.

## 3. Distribución exacta del tiempo

| Tiempo | Duración | Bloque | Evidencia principal |
|---|---:|---|---|
| 0:00–1:00 | 1:00 | Problema y objetivo | App abierta y resumen. |
| 1:00–2:30 | 1:30 | Arquitectura distribuida | Diagrama y Render. |
| 2:30–5:00 | 2:30 | Uso de la app | Cuatro operaciones y “Ver todos”. |
| 5:00–7:00 | 2:00 | Tiempo real | Postman Luis → app Ana, sin recargar. |
| 7:00–9:00 | 2:00 | API e integridad | Saldo, repetición idempotente y error controlado. |
| 9:00–11:00 | 2:00 | Recorrido del código | REST → transacción → WS → Room. |
| 11:00–13:00 | 2:00 | Seguridad | JWT, Argon2id, SQLCipher y Keystore. |
| 13:00–14:00 | 1:00 | Resultados y cierre | Evidencias y límites reales. |
| 14:00–15:00 | 1:00 | Margen | Retraso o pregunta breve. |

Si el profesor interrumpe con preguntas, usar el margen y recortar las variantes opcionales. No sumar otro bloque al final.

## 4. Guion de lo que se dice y se muestra

### 0:00–1:00 — Presentación

**Mostrar:** pantalla principal con cuenta identificada y saldo.

**Decir:**

> “Nuestro proyecto es NexoPay, una billetera móvil de demostración. Permite depósitos, retiros, transferencias y pagos de servicios. Además del manejo de operaciones, buscamos que varios clientes puedan consultar el mismo servidor y recibir cambios en tiempo real, conservando un historial local cifrado.”

> “Vamos a demostrar primero su uso y luego el recorrido del código y sus mecanismos de seguridad.”

**Dominio que se demuestra:** identificar qué resuelve el proyecto y qué se va a comprobar.

### 1:00–2:30 — Arquitectura

**Mostrar este diagrama**, preparado en una pestaña:

```text
Android Ana ── HTTPS/REST ──┐
                           ├── Fastify en Render ── SQLite central
Postman/Luis ─ HTTPS/REST ──┘          │
                                     └── WSS → Android recibe eventos

Android: Room + SQLCipher → historial local cifrado
```

**Decir:**

> “Los clientes pueden estar en redes distintas porque se conectan a una dirección pública. No se comunican directamente entre sí. El backend valida la identidad y las reglas; SQLite mantiene el saldo oficial.”

> “REST procesa cada solicitud y devuelve su resultado. WebSocket mantiene un canal para avisar de los cambios. Room contiene el historial del celular y SQLCipher cifra ese archivo.”

> “Es una aplicación distribuida cliente-servidor. No estamos replicando el saldo oficial entre las bases de los celulares.”

**Mostrar brevemente:** `GET /health` con 200 y `ok: true`. Si Render ya está caliente, no dedicar más de diez segundos.

### 2:30–5:00 — Demostración de la aplicación

Usar la app de Ana y registrar su saldo inicial. Los importes siguientes son ejemplos pequeños; el saldo final depende del estado real del servidor.

| Acción | Datos de ejemplo | Qué señalar |
|---|---|---|
| Depositar | `10.00` | Saldo sube y aparece un ingreso verde. |
| Retirar | `2.00` | Saldo baja y aparece un egreso rojo. |
| Pagar servicio | Luz, contrato `001234`, monto `1.00` | Selector, contrato numérico y comprobante del servicio. |
| Transferir | Destino `10020002`, monto `1.00` | Ana envía a Luis; el origen se descuenta. |

Si se ejecutan una vez y sin otros movimientos, Ana termina este bloque con **saldo inicial + 6.00**.

**Decir mientras se opera:**

> “Los importes se envían en centavos enteros. La validación de pantalla orienta al usuario, pero el servidor vuelve a validar y calcula el saldo real.”

> “En servicios generamos un código compatible, por ejemplo LUZ-001234. Este pago es simulado: el monto se ingresa manualmente y no consultamos una factura de una empresa externa.”

**Mostrar:** el movimiento nuevo arriba y el botón **Ver todos**. Abrir la bandeja, desplazarla y volver al inicio antes del bloque WebSocket.

**No dedicar tiempo a:** explicar cada color, volver a escribir datos varias veces o recorrer todo el historial.

Si la red consume este bloque, mostrar una captura de alguna operación ya comprobada y reservar el tiempo para la transferencia en vivo. No iniciar una segunda operación antes de conocer el resultado de la primera.

### 5:00–7:00 — Demostración central del WebSocket

**Preparación visible:** app de Ana abierta en el inicio, cuenta `10020001`, conexión En línea. No pulsar Actualizar durante la recepción.

**Cliente emisor:** Postman autenticado como Luis (`10020002`).

Si hace falta autenticar a Luis en ese momento:

```text
POST {{baseUrl}}/api/v1/auth/login
Authorization: No Auth
Content-Type: application/json
```

```json
{
  "accountId": "10020002",
  "password": "Nexo#2026"
}
```

Guardar el access token en `tokenLuis`. No sobrescribir `tokenAna`.

**Solicitud de transferencia:**

```text
POST {{baseUrl}}/api/v1/operations/transfer
Authorization: Bearer {{tokenLuis}}
Content-Type: application/json
Idempotency-Key: {{keyDemo}}
```

```json
{
  "toAccountId": "10020001",
  "amountCents": 300
}
```

Antes de enviar, señalar el saldo de Ana. Enviar una vez y mostrar:

1. Respuesta 200 con `TRANSFER_SENT` y `referenceId` en Postman.
2. Ana recibe el aviso y su saldo aumenta en 3.00 sin tocar la pantalla.
3. El nuevo movimiento aparece arriba y los anteriores se desplazan.

**Decir:**

> “La petición REST hizo el cambio en SQLite. Después el servidor envió transfer.received al socket de Ana. El cliente recibió el evento, guardó el comprobante y actualizó la interfaz. No hicimos una consulta manual ni una recarga.”

> “Los dos comprobantes comparten una referencia: uno registra la salida de Luis y otro la entrada de Ana.”

Si también hay un celular de Luis, mostrar que su saldo disminuye. Estar en otra red refuerza la demostración; no cambia el mecanismo.

### 7:00–9:00 — Endpoints, idempotencia y errores

**Mostrar el método, URL, JSON, código HTTP y respuesta**, no solo decir que “funciona”.

#### A. Consultar saldo oficial

```text
GET {{baseUrl}}/api/v1/operations/balance
Authorization: Bearer {{tokenLuis}}
```

Explicar que se consulta la cuenta identificada por el token y que el saldo del emisor debe reflejar el descuento.

#### B. Repetir la transferencia sin duplicarla

Reenviar **la solicitud del bloque anterior con la misma clave y el mismo JSON**. No generar otra clave ni cambiar el importe.

Esperado:

```text
200 OK
idempotentReplay: true
```

Comprobar que no se crea otro movimiento ni vuelve a aumentar Ana. Si hace falta, consultar de nuevo `/balance`.

**Decir:**

> “Un reintento de la misma operación puede ocurrir por una respuesta perdida. Con la misma clave y el mismo contenido, el servidor devuelve el resultado original en lugar de volver a mover dinero.”

La respuesta repetida contiene el resultado de la operación original; para conocer el saldo actual después de otras operaciones se usa `/balance`.

#### C. Mostrar dos rechazos rápidos ya preparados

1. `GET /api/v1/auth/me` con **No Auth** → **401**.
2. `POST /api/v1/operations/deposit` con token, una clave nueva y `{"amountCents": -100}` → **422**.

**Decir:**

> “Los controles se ejecutan en el servidor: ni Postman puede saltarse la autenticación o ingresar un importe negativo.”

Para ahorrar tiempo, el 422 o el conflicto 409 pueden mostrarse como captura preparada. No intentar ejecutar los 12 endpoints uno por uno en vivo: la colección y sus capturas respaldan la cobertura completa.

### 9:00–11:00 — Recorrido del código

Seguir una sola transferencia. Abrir los bloques ya ubicados, sin búsquedas largas.

| Tiempo aproximado | Archivo | Frase concreta |
|---|---|---|
| 15 s | Android `NexoApi.kt` | “Aquí está declarada la misma ruta que acabamos de usar en Postman.” |
| 15 s | Android `WalletRepository.kt` | “Aquí enviamos el DTO, la clave de idempotencia y conservamos el comprobante.” |
| 25 s | Backend `routes/operations.js` | “Leemos el saldo real, restamos al emisor y sumamos al receptor.” |
| 15 s | Backend `security/idempotency.js` | “Estos cambios se confirman dentro de una transacción.” |
| 15 s | Backend `realtime/hub.js` y Android `WalletSocketManager.kt` | “Emitimos por cuenta y recibimos el evento en Android.” |
| 15 s | Android `TransactionDao.kt` y `DashboardViewModel.kt` | “Room emite el historial y el estado observable actualiza la pantalla.” |
| 20 s | Persistencia / evidencia offline | “El historial guardado puede consultarse sin conexión; las operaciones requieren servidor.” |

**Añadir durante el recorrido:**

> “Usamos corrutinas para coordinar red y persistencia sin bloquear la interfaz. Un Mutex evita que esta pantalla lance dos operaciones simultáneas, pero el control final lo conserva el backend.”

Para la persistencia, usar una captura o video breve de reabrir la app y ver el historial. Si se demuestra modo avión en vivo, hacerlo brevemente sin cerrar sesión y restaurar la conexión. **No prometer que el saldo offline es una lectura fresca del servidor.**

Si se pregunta por la creación de tablas, mostrar `src/db.js` y señalar `CREATE TABLE IF NOT EXISTS`. No leer todas las columnas.

### 11:00–13:00 — Seguridad

Presentar cuatro mecanismos y su relación, con código ya abierto:

#### A. Contraseñas — 25 segundos

**Mostrar:** backend `security/passwords.js`.

> “Guardamos un hash Argon2id con salt aleatorio. En el login verificamos la contraseña contra ese hash; no guardamos la contraseña de cada cuenta como texto en su tabla de credenciales.”

La clave demo es pública para la evaluación; no se presenta como una contraseña de producción.

#### B. Sesión — 35 segundos

**Mostrar:** `tokens.js` y `authGuard.js`.

> “El access token JWT identifica la cuenta y tiene duración corta. El servidor verifica su firma y vencimiento. La sesión se renueva con un refresh rotativo; su reutilización se rechaza. El JWT está firmado, no cifrado.”

Relacionarlo con el 401 ya demostrado. El secreto del servidor no se abre ni se incrusta en la APK.

#### C. Datos locales — 35 segundos

**Mostrar:** Android `DatabaseProvider.kt` y `SecureStore.kt`.

> “Room administra el historial y SQLCipher cifra su archivo. La passphrase y el refresh token se guardan mediante SecureStore, que usa AES-GCM y una clave administrada por Android Keystore.”

> “Esto protege almacenamiento local; la base financiera del backend no está cifrada con SQLCipher.”

#### D. Transporte y WebSocket — 25 segundos

**Mostrar:** URL HTTPS/WSS o respuesta de ticket previamente capturada.

> “TLS protege el transporte. Para abrir el socket pedimos un ticket de 30 segundos y un solo uso. Así no colocamos el access token de la sesión en la URL del WebSocket.”

Mencionar brevemente que también existen validación, rate limit, bloqueo por fallos y auditoría. Dejar el detalle para preguntas.

### 13:00–14:00 — Resultados, límites y cierre

**Mostrar:** resumen de evidencias o resultados de pruebas ya ejecutadas.

> “Demostramos operaciones REST, una transferencia entre clientes con actualización en tiempo real, historial local y controles de autenticación e integridad.”

> “La versión es académica: los pagos y depósitos son simulados, el símbolo dólar es visual y la API mantiene PEN. Render Free usa almacenamiento efímero; para uso duradero necesitaríamos persistencia adecuada. Las operaciones requieren conexión.”

> “Nuestra decisión principal fue mantener el saldo oficial en el servidor y utilizar el almacenamiento local para el historial. Esto separa las responsabilidades y permite que distintos clientes trabajen sobre un mismo estado.”

**Terminar aquí.** El minuto siguiente es margen, no otro discurso.

## 5. Pruebas y evidencias que respaldan lo presentado

No ejecutar pruebas largas contra Render durante la defensa. Abrir reportes o terminales con resultados de una ejecución previa e identificar qué cubren.

| Grupo | Qué respalda | Ubicación |
|---|---|---|
| Backend | Autenticación, operaciones, idempotencia y tiempo real | `C:\NexoPayBackend\test\` |
| Unitarias Android | Importes, errores y contratos numéricos | `app/src/test/java/com/uc1/nexopay/ui/common/` |
| Instrumentadas Android | Room/SQLCipher, seguridad, red y flujos | `app/src/androidTest/java/com/uc1/nexopay/` |
| Regresión visual/estado | Espacio de teclado simulado, indicador centrado y detección de entradas nuevas | `ui/FrontendRegressionTest.kt`, dentro de androidTest |
| Postman | Contrato HTTP y respuestas visibles | Colección y capturas de endpoints |

Comandos de referencia para preparar evidencias:

```powershell
# En C:\NexoPayBackend
npm test
```

```powershell
# En la raíz del proyecto Android
.\gradlew.bat testProdDebugUnitTest
.\gradlew.bat lintProdDebug
.\gradlew.bat connectedProdDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.uc1.nexopay.ui.FrontendRegressionTest"
.\gradlew.bat assembleProdRelease
```

La prueba instrumentada anterior necesita un dispositivo/emulador y omite la comprobación de login si encuentra una sesión guardada; revisar el reporte antes de decir que se ejecutó. Usa insets simulados para probar el espacio del teclado, no certifica todos los teclados físicos y versiones Android.

Las otras pruebas instrumentadas de red tienen sus propias condiciones y algunas modifican saldos o esperan un seed concreto. No ejecutarlas indiscriminadamente sobre las cuentas de la demostración.

La última verificación de los ajustes frontend tuvo 9 pruebas unitarias y 2 pruebas instrumentadas de regresión sin fallos ni omisiones, además de compilación release y lint sin errores. Esto no sustituye una ejecución de todas las pruebas del backend ni confirma que todo fallo posible esté descartado.

## 6. Reparto si exponen varias personas

Si son cuatro, una distribución práctica es:

| Persona | Responsabilidad | Tiempo total |
|---|---|---:|
| 1 | Introducción y arquitectura | 2:30 |
| 2 | App y demostración WS | 4:30 |
| 3 | API y recorrido del código | 4:00 |
| 4 | Seguridad y conclusión | 3:00 |

Total de contenido: 14 minutos. Si son menos, combinar bloques. Todos deben poder explicar la transferencia completa y las dos bases; no dividir el conocimiento en partes aisladas.

Conviene que una persona maneje las ventanas y otra hable durante los cambios. Ensayar las transiciones evita perder tiempo buscando archivos.

## 7. Preguntas probables y respuestas breves

**¿Dónde está el saldo verdadero?** En `accounts.balance_cents` de SQLite del backend. La pantalla muestra una representación obtenida del servidor.

**¿Por qué dos SQLite?** La del servidor registra el estado financiero; la del celular conserva historial local. No son dos autoridades del saldo.

**¿Por qué es distribuida si solo hay un backend?** Porque varios procesos en dispositivos distintos coordinan acciones por una red, con latencia, fallos de comunicación y sincronización de estado.

**¿Qué cambia cuando los usuarios están en redes diferentes?** La ruta y latencia de internet; ambos siguen usando el mismo backend y las mismas reglas.

**¿Qué diferencia hay entre REST y WebSocket?** REST inicia una operación y recibe su resultado. WebSocket permite recibir avisos sin consultar manualmente cada cambio.

**¿El WebSocket modifica el saldo?** El endpoint modifica SQLite. El evento informa el resultado a los clientes conectados.

**¿Qué sucede si se pierde un evento?** La app intenta reconectar y puede sincronizar mediante REST. No implementa un registro durable con confirmación de entrega de todos los eventos WS.

**¿Cómo evitan dobles operaciones?** Con idempotencia para la misma clave/cuerpo en el servidor, una transacción SQL y bloqueo de operación local. Una clave distinta sigue siendo otra solicitud.

**¿El bloqueo de Android evita que dos celulares operen a la vez?** No. El Mutex es local. Las reglas y las transacciones del backend protegen el saldo compartido.

**¿Funciona sin internet?** Permite consultar historial guardado con sesión local disponible. No procesa pagos ni transferencias offline.

**¿Por qué centavos enteros?** Para evitar errores de precisión en cálculos monetarios con coma flotante.

**¿Qué se cifra?** El transporte con TLS, la base Android con SQLCipher y los valores sensibles guardados con AES-GCM/Keystore. Las contraseñas se hashean; no se descifran.

**¿Dónde se guardan los tokens?** El access token está en el estado de sesión en memoria; el refresh se persiste cifrado mediante SecureStore.

**¿JWT y SQLCipher resuelven lo mismo?** No. JWT identifica y autoriza solicitudes; SQLCipher protege el archivo local.

**¿El backend sabe si existe el contrato de Luz?** No. Valida formato e importe. La integración con proveedores y facturas no está implementada.

**¿Por qué vuelve el saldo inicial?** Puede haberse perdido el archivo del servicio efímero y ejecutado el seed. Se comprueba con movimientos y registros del despliegue, no se asume solo por la pantalla.

**¿La aplicación duplica dinero al cambiar de red?** No es el comportamiento esperado ni una consecuencia normal de redes diferentes. Se comparan ambos saldos vía REST, referencias y logs para distinguir estado visual atrasado de un fallo persistido. No tenemos una causa confirmada de la incidencia reportada.

**¿Qué falta para producción?** Persistencia y respaldo adecuados, integración real de fondos y facturas, moneda consistente en todo el sistema y mayor robustez de sincronización/observabilidad. Son ampliaciones, no funciones ya implementadas.

## 8. Si algo falla durante la exposición

### Render tarda en responder

> “El servicio gratuito puede estar despertando. Mostramos mientras tanto el recorrido del código y retomamos la petición cuando /health responda.”

No esperar en silencio ni superar el minuto de margen. Usar evidencia previa identificada como tal si no se recupera a tiempo.

### La app está Desconectada

Comprobar red y que el cliente sea prod. Intentar restablecer la conexión antes de presentar la transferencia. Si se pulsa Actualizar para recuperar datos, decirlo: ya no es prueba de recepción automática de esa operación.

### Aparece un saldo extraño

No hacer más operaciones para “arreglarlo”. Consultar `/balance` de ambas cuentas, guardar la referencia y explicar la distinción entre pantalla y servidor. El documento 1 contiene el diagnóstico completo.

### Token vencido o rate limit

Renovar el token correcto si corresponde; ante 429, no insistir con muchos logins. Mostrar las evidencias preparadas y explicar el control activado.

### Poco tiempo restante

Priorizar: transferencia WS en vivo, una respuesta REST, transacción del backend y seguridad local. Mostrar el resto como evidencias preparadas. Cerrar en el minuto 14.

## 9. Lista final de ensayo

- [ ] Podemos explicar el proyecto en 30 segundos sin leer.
- [ ] Sabemos qué cuenta usa cada cliente y qué servidor consulta.
- [ ] Render responde antes de comenzar y no se redespliega durante la prueba.
- [ ] Las solicitudes y archivos están abiertos en el orden del guion.
- [ ] La transferencia cambia ambas cuentas correctamente y la recepción se muestra sin recargar.
- [ ] La repetición idempotente conserva clave y cuerpo.
- [ ] Podemos señalar dónde se valida identidad, monto y saldo real.
- [ ] Distinguimos datos del servidor, historial local y conexiones en memoria.
- [ ] Explicamos cada protección sin afirmar funciones no implementadas.
- [ ] Tenemos respaldo visible y sabemos identificarlo como evidencia previa.
- [ ] Terminamos el ensayo de contenido en 14 minutos o menos.

## Material de estudio relacionado

- [Render, bases y diagnóstico de saldos](01-RENDER-BASES-DE-DATOS-Y-SALDOS.md).
- [Mapa y explicación del código](02-EXPLICACION-SENCILLA-DEL-CODIGO.md).
- [Seguridad implementada](03-SEGURIDAD-IMPLEMENTADA.md).
