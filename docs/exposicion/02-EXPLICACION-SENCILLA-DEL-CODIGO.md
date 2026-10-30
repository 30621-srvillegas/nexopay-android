# 2. Explicación sencilla del código de NexoPay

> **Resumen:** Android muestra la billetera. Fastify recibe las peticiones y decide las operaciones. SQLite guarda el estado del servidor. WebSocket avisa de cambios. Room guarda el historial en el celular y SQLCipher lo cifra.

### Explicación oral de un minuto

> “Tenemos dos proyectos. En Android, los XML dibujan las pantallas y las Activities atienden al usuario. El ViewModel coordina las acciones y el repositorio usa Retrofit para hablar con el backend. En el backend, Fastify recibe cada endpoint, comprueba la sesión y valida la operación. SQLite guarda el saldo y los movimientos. Cuando hay una transferencia, el servidor descuenta y abona dentro de una transacción; después envía un evento WebSocket. El celular lo recibe, guarda el comprobante en Room y actualiza la pantalla. SQLCipher cifra esa base local.”

Para ubicar rápidamente el código, leer las tablas de las secciones 4 y 6. El resto sirve como consulta.

## 1. Dónde están los dos proyectos

```text
Backend:   C:\NexoPayBackend
Frontend:  C:\Users\Usuario-PC\AndroidStudioProjects\NexoPay
```

Abrir el backend en VS Code y Android en Android Studio. La APK de producción se comunica con Render; no necesita que ambos IDE estén abiertos.

Para las rutas Kotlin de este documento, usar como carpeta base:

```text
app/src/main/java/com/uc1/nexopay/
```

## 2. Diccionario rápido

| Palabra | Significado sencillo |
|---|---|
| Endpoint | Dirección del servidor para realizar una acción. |
| REST | Peticiones HTTP: el cliente pide algo y recibe una respuesta. |
| JSON | Formato de los datos que intercambian app y servidor. |
| DTO | Clase que representa el JSON enviado o recibido. |
| ViewModel | Coordina la pantalla y conserva su estado. |
| Repositorio | Punto que coordina acceso a red y datos locales. |
| DAO | Funciones para consultar o modificar la base local. |
| Flow / StateFlow | Flujo observable: al cambiar un dato, sus observadores pueden actualizarse. |
| WebSocket | Conexión abierta por la que el servidor puede enviar avisos. |
| Transacción SQL | Grupo de cambios que se confirma completo o se revierte si falla. |
| Caché local | Información guardada en el dispositivo para consultar sin volver a descargarla. |

## 3. Estructura del backend

```text
C:\NexoPayBackend
├── package.json              Dependencias y comandos npm
├── render.yaml               Configuración prevista del despliegue
├── .env                      Configuración local; no mostrar secretos
├── data/nexopay.db           Archivo SQLite al ejecutar localmente
├── src/
│   ├── server.js             Arranque del servidor
│   ├── app.js                Fastify, plugins y registro de rutas
│   ├── config.js             Lectura de variables de entorno
│   ├── db.js                 Apertura de SQLite y creación de tablas
│   ├── seed.js               Cuentas demo si la base está vacía
│   ├── routes/
│   │   ├── auth.js           Login, refresh, logout y cuenta
│   │   ├── operations.js     Operaciones, saldo e historial
│   │   └── realtime.js       Ticket y conexión WebSocket
│   ├── realtime/hub.js       Sockets organizados por cuenta
│   ├── security/             Contraseñas, tokens, auth e idempotencia
│   └── utils/                Validación, errores y formato del dinero
└── test/                     Pruebas automatizadas del backend
```

### ¿En qué orden arranca?

```text
npm start
   → src/server.js
   → lee config
   → openDatabase()
   → crea tablas que faltan
   → ensureSeed()
   → buildApp()
   → escucha en el host y puerto configurados
```

`app.js` registra autenticación, límites, WebSocket y rutas. `server.js` inicia la escucha. Separarlos facilita construir una app de prueba sin iniciar siempre un servidor público.

## 4. Dónde están los endpoints

Base de producción:

```text
https://nexopay-backend-xnr5.onrender.com
```

| Método | Ruta | Autenticación | Archivo del backend |
|---|---|---|---|
| GET | `/health` | No | `src/app.js` |
| POST | `/api/v1/auth/login` | Cuenta y contraseña | `src/routes/auth.js` |
| POST | `/api/v1/auth/refresh` | Refresh token en JSON | `src/routes/auth.js` |
| POST | `/api/v1/auth/logout` | Refresh token en JSON | `src/routes/auth.js` |
| GET | `/api/v1/auth/me` | Bearer | `src/routes/auth.js` |
| POST | `/api/v1/operations/deposit` | Bearer + Idempotency-Key | `src/routes/operations.js` |
| POST | `/api/v1/operations/withdraw` | Bearer + Idempotency-Key | `src/routes/operations.js` |
| POST | `/api/v1/operations/transfer` | Bearer + Idempotency-Key | `src/routes/operations.js` |
| POST | `/api/v1/operations/pay-service` | Bearer + Idempotency-Key | `src/routes/operations.js` |
| GET | `/api/v1/operations/balance` | Bearer | `src/routes/operations.js` |
| GET | `/api/v1/operations/transactions` | Bearer | `src/routes/operations.js` |
| POST | `/api/v1/realtime/ticket` | Bearer | `src/routes/realtime.js` |
| GET con upgrade | `/ws?ticket=...` | Ticket de un solo uso | `src/routes/realtime.js` |

Son **12 endpoints HTTP normales y una ruta WebSocket**. No hay PUT ni DELETE implementados en esta API.

**Cómo leer una definición:**

```javascript
app.post('/api/v1/operations/transfer', { preHandler: app.authGuard }, ...)
```

Significa: “Cuando llegue un POST a esta dirección, primero comprueba la sesión y después procesa la transferencia”.

## 5. Cómo se construye SQLite en el servidor

`src/db.js` abre `new Database(dbPath)` y ejecuta `migrate(db)`. Sus instrucciones `CREATE TABLE IF NOT EXISTS` crean la estructura que falta sin necesitar MySQL ni un servidor de base externo.

| Tabla | Qué guarda |
|---|---|
| `accounts` | ID de cuenta, nombre, moneda, saldo y estado de bloqueo. |
| `password_credentials` | Hash de contraseña asociado a la cuenta. |
| `refresh_tokens` | Hashes de tokens de renovación y su estado. |
| `transactions` | Comprobantes de movimientos por cuenta. |
| `idempotency_keys` | Resultado de solicitudes ya procesadas. |
| `ws_tickets` | Tickets temporales para WebSocket. |
| `audit_log` | Registro de acciones relevantes. |

Hay claves foráneas e índices. El índice del historial ayuda a consultar movimientos de una cuenta por fecha.

Configuración importante:

- `WAL`: permite que haya lectores mientras se realizan escrituras; SQLite sigue teniendo un escritor a la vez.
- `foreign_keys = ON`: activa restricciones de relaciones.
- `busy_timeout = 5000`: permite esperar un bloqueo temporal antes de fallar.
- `db.transaction(...)`: agrupa cambios de una operación.

Los importes se guardan en **centavos enteros**. `2550` significa `25.50`. No se usa `Double` para calcular el saldo.

## 6. Estructura del frontend Android

```text
app/
├── build.gradle.kts                    Dependencias, variantes y URLs
└── src/main/
    ├── AndroidManifest.xml             Permisos y Activities
    ├── java/com/uc1/nexopay/
    │   ├── NexoPayApp.kt               Inicializa el contenedor de la app
    │   ├── di/AppContainer.kt          Construye y comparte dependencias
    │   ├── ui/
    │   │   ├── login/                  LoginActivity y LoginViewModel
    │   │   ├── dashboard/              Inicio, formularios e historial
    │   │   └── common/                 Formato, avisos y validaciones
    │   ├── data/
    │   │   ├── remote/                 Retrofit, tokens y WebSocket
    │   │   ├── remote/dto/             Clases JSON
    │   │   ├── repository/             WalletRepository
    │   │   └── local/                  Room, entidad, DAO y SQLCipher
    │   ├── security/                   Keystore y sesión
    │   └── settings/                   Preferencias de la aplicación
    └── res/
        ├── layout/                     Pantallas y formularios XML
        ├── drawable/                   Iconos y fondos
        └── values/                     Textos, colores, estilos y tema
```

La interfaz usa **XML Views y Material 3**, no Compose. ViewBinding da acceso a las vistas sin buscar manualmente cada ID.

### Archivos que conviene poder señalar

| Archivo Kotlin | Explicación en una frase |
|---|---|
| `ui/login/LoginActivity.kt` | Atiende el formulario y muestra el estado del login. |
| `ui/login/LoginViewModel.kt` | Ejecuta login y restauración de sesión. |
| `ui/dashboard/DashboardActivity.kt` | Muestra saldo, operaciones, conexión e historial. |
| `ui/dashboard/DashboardViewModel.kt` | Coordina operaciones REST, saldo y eventos del socket. |
| `ui/dashboard/AmountSheet.kt` | Formulario de depósito o retiro. |
| `ui/dashboard/TransferSheet.kt` | Valida cuenta destino y monto. |
| `ui/dashboard/ServiceSheet.kt` | Selección de servicio, contrato numérico y monto. |
| `ui/dashboard/HistoryFragment.kt` | Bandeja “Ver todos”; comparte ViewModel con el inicio. |
| `ui/dashboard/TransactionAdapter.kt` | Dibuja cada movimiento y distingue ingreso/egreso. |
| `data/repository/WalletRepository.kt` | Envía solicitudes y conserva comprobantes en Room. |
| `data/remote/NexoApi.kt` | Declara los endpoints que utiliza Android. |
| `data/remote/ApiFactory.kt` | Construye Retrofit y OkHttp. |
| `data/remote/WalletSocketManager.kt` | Abre el canal, interpreta eventos y reconecta. |
| `data/local/DatabaseProvider.kt` | Construye Room con SQLCipher. |

## 7. Cómo se conecta el frontend al backend

### Paso 1: elegir la dirección

En `app/build.gradle.kts`:

| Variante | Dirección | Uso |
|---|---|---|
| `local` | `http://10.0.2.2:3000/` | Emulador hacia backend de la PC. |
| `device` | `http://127.0.0.1:3000/` | Celular USB con `adb reverse tcp:3000 tcp:3000`. |
| `prod` | `https://nexopay-backend-xnr5.onrender.com/` | APK con backend de Render. |

También se configura la URL WebSocket correspondiente.

### Paso 2: construir el cliente HTTP

`ApiFactory.kt` toma `BuildConfig.API_BASE_URL`, configura tiempos de espera y crea Retrofit con serialización JSON.

- `AuthInterceptor.kt` agrega `Authorization: Bearer ...`.
- `TokenAuthenticator.kt` intenta renovar la sesión cuando corresponde un 401.
- `NexoApi.kt` declara `@POST`, `@GET`, `@Body`, `@Header` y `@Query`.

### Paso 3: llamar desde el repositorio

`WalletRepository.transfer()` genera una clave de idempotencia y llama a `api.transfer(...)` con un DTO. La respuesta exitosa se guarda en Room.

**Frase para explicar:**

> “La pantalla no arma solicitudes HTTP por su cuenta. La acción pasa por el ViewModel y el repositorio; Retrofit convierte nuestros objetos en JSON y llama a la ruta del servidor.”

## 8. Recorrido completo de una transferencia

1. Se toca Transferir y se abre `TransferSheet`.
2. Se valida cuenta de ocho dígitos, cuenta diferente, monto y saldo visible.
3. `DashboardViewModel.transfer()` llama a `runOperation()`.
4. Un `Mutex` evita otra operación local simultánea y la UI desactiva botones.
5. `WalletRepository` llama al endpoint mediante `NexoApi`.
6. Fastify verifica el JWT, valida el cuerpo y verifica idempotencia.
7. SQLite lee el saldo real, descuenta, abona y guarda dos comprobantes atómicamente.
8. La respuesta REST devuelve el saldo del emisor y su comprobante.
9. El backend envía `transfer.received` al receptor y `balance.updated` al emisor.
10. Android guarda los comprobantes y actualiza el saldo con los datos del servidor.
11. Room emite un nuevo Flow; el adaptador actualiza la lista. En el inicio, un movimiento nuevo se revela arriba.

La validación visual mejora la experiencia; la validación del backend es la que impide operar con un saldo real insuficiente.

## 9. Cómo se construye la base del celular

Archivos en `data/local/`:

- `TransactionEntity.kt`: columnas del comprobante local, con `id` como clave primaria.
- `TransactionDao.kt`: consultas, `upsert`, conteo y borrado.
- `AppDatabase.kt`: declara las entidades y el acceso al DAO.
- `DatabaseProvider.kt`: instancia única de Room, carga SQLCipher y configura el cifrado.

Ejemplo conceptual de consulta:

```sql
SELECT * FROM transactions ORDER BY createdAt DESC, id DESC;
```

`observeAll()` entrega un Flow. El repositorio lo expone; el ViewModel lo observa; la pantalla actualiza el RecyclerView. El adaptador usa `DiffUtil` para actualizar los elementos que cambiaron.

Se muestran todos los movimientos guardados localmente. La sincronización REST solicita hasta los últimos 100 del servidor. No hay paginación de todo el historial remoto en esta versión.

## 10. WebSocket explicado sin complicaciones

REST es como hacer una llamada puntual: pedir saldo y recibir respuesta. WebSocket es dejar un canal abierto para que el servidor avise cuando hay un cambio.

```text
Android pide POST /api/v1/realtime/ticket con JWT
   → obtiene ticket de 30 segundos
   → abre wss://.../ws?ticket=...
   → recibe connection.ready
   → recibe balance.updated o transfer.received
```

- Servidor: `src/routes/realtime.js` y `src/realtime/hub.js`.
- Android: `WalletSocketManager.kt` y `DashboardViewModel.kt`.
- Al desconectarse, Android intenta reconectar con espera creciente y una variación aleatoria.
- Al volver a abrir el dashboard se consulta el servidor para sincronizar; no se conserva una cola garantizada de todos los eventos WS perdidos.

La app no necesita consultar el saldo cada segundo. Los eventos avisan de cambios mientras el canal está activo.

## 11. Corrutinas y respuesta de la interfaz

`DashboardViewModel` usa `viewModelScope.launch(Dispatchers.IO)` para los flujos de operaciones y carga. Room y las funciones `suspend` facilitan el acceso asíncrono. La pantalla observa el estado dentro de `repeatOnLifecycle`.

> “La red y la persistencia no deben congelar la interfaz. Coordinamos el trabajo con corrutinas y mostramos el estado mediante StateFlow.”

`runBlocking` aparece en el autenticador de OkHttp para adaptarse a su interfaz síncrona; se ejecuta en el contexto de esa llamada de red. No debe explicarse como una técnica para hacer peticiones desde el hilo de la pantalla.

## 12. Detalles que hay que explicar honestamente

- Es una **aplicación distribuida cliente-servidor**, no un conjunto de microservicios ni una base financiera replicada entre celulares.
- El símbolo `$` es una decisión visual; la API sigue identificando la moneda como `PEN`. No hay conversión.
- Los contratos en la app son numéricos de 3 a 12 dígitos; el backend admite además sufijos alfanuméricos compatibles con su contrato original.
- El servicio se transforma en un prefijo, por ejemplo `AGUA` + `001234` → `AGUA-001234`.
- El importe de un servicio se escribe manualmente; no se consulta una deuda real.
- No hay pagos, depósitos o transferencias financieras pendientes para ejecutar luego sin conexión.

## 13. Recorrido de código de dos minutos

1. `NexoApi.kt`: mostrar la ruta de transferencia.
2. `WalletRepository.kt`: mostrar la llamada y el guardado del comprobante.
3. Backend `routes/operations.js`: mostrar descuento y abono.
4. Backend `security/idempotency.js`: mostrar la transacción.
5. Backend `realtime/hub.js`: mostrar el envío por cuenta.
6. `WalletSocketManager.kt`: mostrar recepción del evento.
7. `TransactionDao.kt`: señalar la consulta observable.

No leer archivos completos en voz alta. Explicar responsabilidades y seguir una sola operación de principio a fin.

Continuar con [la seguridad](03-SEGURIDAD-IMPLEMENTADA.md) o con [el guion de exposición](04-GUION-DE-EXPOSICION-15-MINUTOS.md).
