# 3. Seguridad implementada en NexoPay

> **Objetivo:** explicar qué protege cada mecanismo, cómo lo hace y en qué archivo se encuentra. La seguridad combina controles del servidor, transporte seguro y protección de los datos del celular.

Rutas de referencia:

```text
B = C:\NexoPayBackend
A = C:\Users\Usuario-PC\AndroidStudioProjects\NexoPay
K = A\app\src\main\java\com\uc1\nexopay
```

## 1. Resumen para memorizar

| Capa | Mecanismo | Para qué sirve |
|---|---|---|
| Identidad | Argon2id + JWT | Verificar credenciales e identificar peticiones autenticadas. |
| Continuidad de sesión | Refresh rotativo | Renovar el acceso y detectar reutilización de un refresh. |
| Tiempo real | Ticket corto de un solo uso | Autorizar una conexión WS sin usar el access token como parámetro de URL. |
| Transporte | HTTPS / WSS | Proteger los datos durante el viaje por internet. |
| Datos Android | SQLCipher + Keystore + AES-GCM | Cifrar historial y proteger secretos guardados. |
| Integridad financiera | Validación, centavos, transacciones e idempotencia | Evitar importes inválidos y ejecuciones parciales o repetidas con la misma clave. |
| Abuso y diagnóstico | Rate limit, bloqueo y auditoría | Limitar intentos y registrar acciones relevantes. |

**Frase de apertura:**

> “No confiamos solo en la pantalla: el servidor verifica identidad y reglas. Además protegemos el transporte y ciframos los datos sensibles guardados en Android.”

## 2. Contraseñas: Argon2id

**Qué es:** una función de hash diseñada para contraseñas. Consume recursos para dificultar probar muchas contraseñas si alguien obtiene la base.

**Dónde:**

- `B\src\security\passwords.js`: `hashPassword()` y `verifyPassword()`.
- `B\src\seed.js`: crea el hash de las contraseñas demo.
- `B\src\routes\auth.js`: verifica el hash al iniciar sesión.
- Tabla: `password_credentials`.

**Cómo se implementó:** biblioteca `hash-wasm`, Argon2id, salt aleatorio de 16 bytes, tres iteraciones, unos 64 MiB de memoria y resultado de 32 bytes. La representación codificada contiene lo necesario para verificar el hash. Se normaliza el texto con NFKC.

No se descifra una contraseña para compararla. Se verifica la contraseña recibida contra el hash almacenado. El salt evita que contraseñas iguales tengan necesariamente el mismo hash.

**Qué mostrar:** las dos funciones, sin abrir `.env` ni copiar secretos.

**Límite:** las cuentas usan una contraseña pública de demostración; el hash no vuelve secreta una contraseña conocida. La versión actual no implementa registro ni recuperación de contraseña.

## 3. JWT de acceso: identificar la cuenta

**Qué es:** un token firmado que el cliente presenta para autenticarse. La firma permite verificar que no fue alterado y que lo emitió quien conoce el secreto.

**Dónde:**

- `B\src\app.js`: registra `@fastify/jwt`.
- `B\src\security\tokens.js`: `signAccessToken()`.
- `B\src\security\authGuard.js`: verifica las peticiones.
- `K\data\remote\AuthInterceptor.kt`: agrega el token a las solicitudes.

Flujo:

```text
Login correcto → access token → Authorization: Bearer TOKEN → authGuard → cuenta autenticada
```

El payload incluye `sub` —ID de cuenta— y `typ: access`, con caducidad. La duración configurada por defecto es 900 segundos: 15 minutos.

`authGuard` verifica firma y vigencia, exige el tipo de token esperado y comprueba que la cuenta exista y esté activa. Luego coloca esa cuenta en `request.account`.

Así, para transferir, el origen sale del token validado. No se permite elegir libremente otra cuenta de origen en el JSON.

**El secreto de firma queda en el servidor.** `config.js` lo exige en producción con al menos 32 caracteres; `render.yaml` solicita que Render lo genere.

**Qué demostrar:** GET `/api/v1/auth/me` con y sin Bearer: 200 frente a 401.

**No confundir:** un JWT firmado no es un mensaje cifrado. Sus datos pueden leerse; su protección de integridad está en la firma. No contiene la contraseña.

## 4. Refresh token y rotación

**Para qué:** mantener una sesión sin pedir la contraseña cada 15 minutos.

**Dónde:**

- `B\src\security\tokens.js`: generación, hash, rotación y revocación.
- `B\src\routes\auth.js`: `/refresh` y `/logout`.
- `K\data\remote\TokenAuthenticator.kt`: renovación ante 401.
- `K\security\SessionManager.kt`: gestión de sesión y refresh guardado.

El refresh es un valor aleatorio opaco, no otro JWT. El servidor guarda su hash SHA-256, cuenta, familia, vencimiento y estado de revocación. El plazo por defecto es de siete días.

Al renovarlo:

1. Se comprueba su hash y vigencia.
2. Se genera un refresh nuevo y un access token nuevo.
3. El refresh anterior queda revocado y relacionado con su reemplazo.
4. Si se intenta reutilizar el revocado, se rechaza la petición y se revoca la familia. La revocación se realiza fuera del rollback de la transacción fallida para que quede guardada.

Android utiliza un bloqueo `synchronized` para coordinar las renovaciones de su autenticador y una cabecera `X-Refresh-Retry` para evitar un bucle de reintentos. Si otra llamada ya renovó el access token, puede reutilizar el nuevo.

**Qué demostrar:** `/refresh` devuelve otro par de tokens. Guardar siempre los nuevos; no repetir el refresh viejo como si siguiera vigente.

### Cierre de sesión: alcance exacto

`/logout` revoca la familia de refresh tokens y devuelve 204. Android elimina los datos de sesión, desconecta su socket y borra el historial local.

Un access token ya emitido puede seguir siendo válido hasta expirar: no existe una lista de revocación inmediata por JWT. Para demostrar el logout, intentar renovar con el refresh revocado; no prometer que todos los access tokens dejan de funcionar al instante.

## 5. Autorización del WebSocket por ticket

**Dónde:** `B\src\routes\realtime.js`, `B\src\security\tokens.js` y `K\data\remote\WalletSocketManager.kt`.

1. El cliente autenticado pide `POST /api/v1/realtime/ticket`.
2. Recibe un ticket aleatorio de 30 segundos.
3. Abre `wss://.../ws?ticket=...`.
4. El servidor valida su hash, lo marca como usado y vincula el socket con la cuenta.
5. `hub.js` envía cada evento a los sockets asociados a esa cuenta.

El objetivo es no poner el access token de la sesión en la URL del WebSocket. El ticket sigue siendo sensible mientras es válido, pero su tiempo y reutilización están limitados.

Un ticket inválido, usado o vencido causa cierre con código 4401. Para reconectar se solicita uno nuevo.

**Qué demostrar:** conexión correcta con `connection.ready`, evento financiero y, si hay tiempo fuera del guion principal, rechazo al intentar reutilizar el ticket.

**Límite:** la autorización se realiza al abrir el socket; no hay una comprobación del JWT en cada mensaje ni garantía de desconexión inmediata de todos los sockets ajenos al dispositivo que cerró sesión.

## 6. HTTPS y WSS: proteger el transporte

Render publica el dominio con TLS. Android usa HTTPS para REST y WSS para WebSocket en la variante `prod`.

**Dónde:**

- `A\app\build.gradle.kts`: URLs de producción.
- `A\app\src\main\res\xml\network_security_config.xml`: bloquea tráfico HTTP sin cifrar y confía en certificados del sistema.
- `A\app\src\debug\res\xml\network_security_config.xml`: excepciones HTTP limitadas a direcciones locales de desarrollo.
- `A\app\src\main\AndroidManifest.xml`: referencia a esa configuración.

**Explicación sencilla:**

> “Aunque cada usuario tenga un proveedor de internet diferente, las credenciales y tokens viajan dentro de una conexión TLS verificada por Android.”

La app no desactiva la validación del certificado. Tampoco implementa certificate pinning. HTTPS protege el viaje; no sustituye autenticación ni cifrado de archivos.

## 7. SQLCipher: cifrado del historial local

**Qué es:** una implementación de SQLite con cifrado del archivo de base de datos.

**Dónde:** `K\data\local\DatabaseProvider.kt`.

Implementación:

```text
System.loadLibrary("sqlcipher")
   → obtener/generar passphrase de 32 bytes
   → SupportOpenHelperFactory(passphrase)
   → Room.databaseBuilder(...).openHelperFactory(factory)
```

Room sigue permitiendo trabajar con entidades y DAO; SQLCipher protege cómo quedan los datos en el archivo. La passphrase se guarda mediante `SecureStore` con la clave lógica `db_passphrase_v1`.

**Para qué:** reducir la exposición del historial si alguien obtiene una copia del archivo local sin los secretos necesarios.

**Qué mostrar:** `DatabaseProvider.kt` y la prueba `data/local/DatabaseCipherTest.kt`, ubicada en `app/src/androidTest/java/com/uc1/nexopay/`.

Esa prueba abre la base configurada con SQLCipher, guarda un movimiento y lo consulta. No inspecciona por sí sola los bytes del archivo ni demuestra que pueda resistir cualquier ataque; presentar exactamente lo que verifica.

**Límites:** no cifra el archivo SQLite del backend, no impide que la app autorizada lea el historial y no garantiza protección absoluta frente a un dispositivo comprometido mientras la app se ejecuta.

## 8. Keystore y AES-GCM: proteger secretos guardados

**Dónde:** `K\security\SecureStore.kt` y `SessionManager.kt`.

`SecureStore` crea o utiliza una clave AES de 256 bits en Android Keystore, con el alias `nexopay_master_key_v1`.

Para guardar un valor:

1. Obtiene la clave de Keystore.
2. Cifra el texto con `AES/GCM/NoPadding` y un IV generado para esa operación.
3. Guarda IV y texto cifrado, codificados en Base64, en preferencias privadas.

AES-GCM aporta confidencialidad y verificación de integridad del valor cifrado. **Base64 no cifra**; solo representa los bytes como texto.

La clave maestra permanece administrada por Keystore; no se guarda junto al ciphertext como una cadena legible. No se debe garantizar respaldo por hardware en todos los dispositivos: depende de sus capacidades.

El refresh token se persiste de esta forma. El access token se mantiene en el estado de sesión en memoria; al restaurar la sesión se obtiene uno nuevo.

La configuración no exige biometría para usar esta clave. No afirmar que hay huella digital o PIN implementados en NexoPay.

## 9. Validación y consultas parametrizadas

**Servidor:** `B\src\utils\validate.js`, con esquemas Zod.

- Cuentas de ocho dígitos.
- Montos enteros, positivos y de hasta 10000000 centavos por solicitud.
- Formatos de contrato y de claves de idempotencia.
- Transferencias a la misma cuenta rechazadas en la ruta.
- Saldo suficiente comprobado contra SQLite.

**Frontend:** formularios `AmountSheet`, `TransferSheet`, `ServiceSheet` y `ui/common/ServiceContract.kt`.

La app restringe contratos a 3–12 dígitos y genera el prefijo del servicio. El backend mantiene un esquema más amplio de sufijo alfanumérico. En ambos casos solo se valida formato: no hay consulta de deuda ni existencia real del contrato.

Las consultas del servidor utilizan sentencias preparadas y parámetros `?`, en lugar de concatenar entrada del usuario como instrucciones SQL. El DAO de Room también parametriza sus operaciones.

**Qué demostrar:** importe negativo → 422; sin token → 401; una operación sin saldo suficiente → 400, cuando se cumpla esa condición.

## 10. Integridad financiera e idempotencia

**Dónde:** `B\src\routes\operations.js`, `B\src\security\idempotency.js`, `B\src\db.js` y `K\data\repository\WalletRepository.kt`.

### Centavos enteros

Evitan el error de representación que produce trabajar con dinero en coma flotante. Backend usa enteros y Android `Long`/`BigDecimal` para interpretar y presentar los importes.

### Transacciones

Descuento, abono, comprobantes y registro de idempotencia se confirman en una transacción SQLite. Ante un error se revierten sus cambios.

### Idempotencia

La clave se asocia a la cuenta y se compara un hash de operación y cuerpo:

| Solicitud | Resultado |
|---|---|
| Clave nueva y solicitud válida | Ejecuta y guarda la respuesta. |
| Misma clave y mismo contenido | Devuelve la respuesta original, sin otra operación. |
| Misma clave con otro contenido u operación | 409, conflicto. |

**No es una garantía universal de “exactamente una vez”.** Otra clave representa otra solicitud; la app genera una nueva por invocación. La protección también depende de conservar la tabla de idempotencia, algo limitado por el disco efímero del despliegue actual.

En Android un `Mutex` evita operaciones simultáneas desde ese ViewModel. Eso no reemplaza la validación y la transacción del servidor ni coordina todos los celulares.

## 11. Límites de uso y bloqueo de cuenta

**Dónde:** `B\src\app.js` y `B\src\routes\auth.js`.

- Límite general configurado: 120 solicitudes por minuto.
- Login: 5 solicitudes por 15 minutos.
- Refresh, logout y ticket WS tienen límites específicos de 30 solicitudes por minuto.
- Cinco fallos de contraseña pueden bloquear temporalmente la cuenta durante 15 minutos.

El rate limit limita peticiones y, por defecto, suele agruparlas por la IP del cliente; el bloqueo de intentos se guarda por cuenta. Varios usuarios detrás de una misma salida a internet pueden compartir un límite por IP.

El backend utiliza `trustProxy: true` para trabajar detrás de un proxy como Render. Su configuración de proxy debe corresponder al entorno real; no equivale por sí sola a autenticar una IP.

No hacer muchas pruebas de login incorrecto durante la exposición: se podría activar el control y bloquear la propia demostración.

## 12. Controles complementarios

| Control | Ubicación | Alcance |
|---|---|---|
| Helmet y HSTS en producción | `B\src\app.js` | Cabeceras HTTP de protección; no sustituyen TLS ni autorización. |
| CORS | `B\src\app.js`, `config.js` | Restringe orígenes de navegadores. No impide que Postman u otro cliente HTTP llame a la API. |
| Tamaño de cuerpo | `B\src\app.js` | Máximo HTTP de 32 KiB. |
| Tamaño WS | `B\src\app.js` | Máximo configurado de 16 KiB por mensaje. |
| Errores uniformes | `B\src\app.js`, `utils/errors.js` | Respuestas con código, mensaje y requestId; errores internos no devuelven un stack al cliente. |
| Redacción de logs | `B\src\app.js` | Configura ocultación de campos sensibles como Authorization, contraseña y refresh. No afirmar que todo log está automáticamente libre de secretos. |
| Auditoría | `B\src\routes\auth.js`, `routes/operations.js` | Registra acciones, cuenta, hora y datos de contexto. No es un registro inmutable ni externo. |
| Backup deshabilitado | `A\app\src\main\AndroidManifest.xml` | `allowBackup=false` y configuración de extracción de datos. |
| Configuración privada | `.env`, variables de Render y archivos de firma | Evita incrustar el secreto JWT y las contraseñas de firma en la APK o documentación pública. |

La minificación release dificulta una lectura directa de parte del código, pero **no reemplaza** estos mecanismos ni hace segura una clave incrustada.

## 13. Cómo mostrar dominio sin exagerar

**Sí decir:**

> “Tenemos controles de autenticación, integridad de las operaciones, transporte seguro y cifrado local. Los verificamos con respuestas de la API y pruebas específicas.”

**No decir:**

- “El JWT está cifrado”. Está firmado.
- “Toda la base de datos está cifrada”. El cifrado descrito es de la base Android.
- “CORS evita todas las llamadas externas”. No es un mecanismo de autenticación.
- “No se puede duplicar ninguna operación bajo ninguna circunstancia”. La idempotencia tiene un alcance definido.
- “Tenemos una billetera bancaria de producción”. Depósitos y pagos son simulados y el almacenamiento de Render Free es efímero.
- “Cerrar sesión destruye instantáneamente todos los JWT”. Los access tokens tienen vigencia propia.

## 14. Evidencia mínima para la defensa

1. Login correcto y `/auth/me` autenticado.
2. `/auth/me` sin token: 401.
3. Una operación con monto inválido: 422.
4. Repetir depósito con misma clave/cuerpo: `idempotentReplay: true` y sin nuevo abono.
5. Señalar `hashPassword()`, `authGuard()`, `DatabaseProvider` y `SecureStore`.
6. Mostrar ticket y evento WebSocket.

Dejar pruebas de refresh reutilizado, bloqueo y cifrado como evidencias preparadas si el tiempo no alcanza. No abrir `.env`, `keystore.properties`, claves privadas ni tokens completos innecesariamente durante la proyección.

Continuar con [el guion de máximo 15 minutos](04-GUION-DE-EXPOSICION-15-MINUTOS.md).
