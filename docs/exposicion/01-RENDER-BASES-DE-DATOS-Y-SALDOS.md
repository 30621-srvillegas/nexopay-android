# 1. Render, bases de datos y comportamiento de los saldos

> **Idea principal:** Render ejecuta nuestro servidor en internet. El servidor decide los saldos usando SQLite; cada celular conserva una copia local del historial. No es necesario que ambos celulares estén en la misma red.

## 1. ¿Qué hace Render en NexoPay?

Render es una plataforma que ejecuta aplicaciones en una computadora de la nube. En este proyecto aloja el **backend Node.js + Fastify**, no la interfaz Android.

El archivo `C:\NexoPayBackend\render.yaml` describe el despliegue:

| Configuración | Significado |
|---|---|
| `type: web` | Es un servicio accesible por internet. |
| `plan: free` | La configuración del repositorio solicita una instancia gratuita. |
| `buildCommand: npm ci` | Instala las dependencias fijadas en el archivo de bloqueo. |
| `startCommand: npm start` | Inicia `src/server.js`. |
| `healthCheckPath: /health` | Render puede comprobar si el servicio responde. |
| `DB_PATH=./data/nexopay.db` | Ubicación del archivo SQLite dentro del servicio. |

La dirección que utiliza la APK de producción es:

```text
https://nexopay-backend-xnr5.onrender.com
```

Render proporciona el acceso HTTPS y su certificado TLS. La conexión WebSocket de producción utiliza `wss://` en el mismo dominio.

**Cómo decirlo en clase:**

> “Render nos permite tener un servidor compartido en internet. La APK no necesita que nuestra computadora esté encendida: se comunica con el backend publicado.”

El archivo describe la configuración prevista. Para confirmar el estado real del despliegue hay que revisar el servicio y sus variables en el panel de Render.

## 2. No confundir base en memoria con almacenamiento efímero

Son conceptos distintos:

| Concepto | Qué significa | Uso en NexoPay |
|---|---|---|
| SQLite en memoria | Se abre como `:memory:` y vive en la RAM del proceso. | Está admitido por `openDatabase()` y se puede usar para pruebas; no es la ruta configurada para Render. |
| SQLite en archivo | Guarda las tablas en un archivo, como `nexopay.db`. | Es la configuración del servidor y de la base local Android. |
| Disco efímero | Los archivos no tienen persistencia garantizada al reemplazar o reiniciar la instancia. | El archivo SQLite del servicio gratuito de Render está en este tipo de almacenamiento. |
| Estructuras en RAM | Objetos del programa que se pierden al terminar el proceso. | El mapa de conexiones WebSocket del servidor. |

Por tanto, la frase correcta es:

> “Usamos SQLite en un archivo del almacenamiento efímero de Render Free; no una base de producción configurada con `:memory:`.”

### ¿Por qué a veces vuelven las cuentas y los saldos iniciales?

1. El servicio pierde el archivo de base de datos al reiniciarse, redesplegarse o suspenderse.
2. `src/server.js` abre la base y ejecuta `ensureSeed(db)`.
3. `src/db.js` crea las tablas que faltan.
4. Si no hay cuentas, `src/seed.js` crea las cuentas demo:

| Cuenta | Nombre | Saldo inicial en centavos | Valor numérico mostrado |
|---|---|---:|---:|
| `10020001` | Ana Torres | 150000 | 1500.00 |
| `10020002` | Luis Rojas | 80000 | 800.00 |

El seed **no reinicia los saldos en cada solicitud ni en cada login**. Solo carga las cuentas si no existe ninguna.

El reinicio también afecta movimientos, refresh tokens, tickets, auditoría y claves de idempotencia almacenados en esa base. No recupera automáticamente las operaciones previas.

### ¿Render siempre responde inmediatamente?

No. Según su documentación, un servicio gratuito puede suspenderse tras 15 minutos sin tráfico entrante; al recibir una nueva solicitud o conexión puede tardar alrededor de un minuto en despertar. No debe presentarse ese tiempo como un fallo del login.

Antes de exponer, abrir:

```text
https://nexopay-backend-xnr5.onrender.com/health
```

Esperar una respuesta JSON con `"ok": true`. Un `/health` correcto prueba disponibilidad, no que se hayan conservado los movimientos anteriores.

## 3. Las dos bases de datos y la memoria del servidor

```text
CELULAR DE ANA                     RENDER                         CELULAR DE LUIS
Kotlin / interfaz           Node.js + Fastify                     Kotlin / interfaz
        │                         │                                      │
        ├──── HTTPS / REST ───────┤──── HTTPS / REST ──────────────────────┤
        ◀──── WSS / eventos ──────┼──── WSS / eventos ────────────────────▶
        │                         │                                      │
Room + SQLCipher            SQLite del servidor                    Room + SQLCipher
Historial local             Cuentas y saldo oficial                Historial local
```

### A. SQLite del backend: la fuente de verdad del saldo

Archivo configurado: `./data/nexopay.db`, relativo a la carpeta desde la que se inicia el backend.

Se construye en `src/db.js` con `better-sqlite3`. Contiene cuentas, credenciales, movimientos y controles de seguridad.

**El servidor autoriza o rechaza el movimiento y calcula el saldo.** No confía en que el celular le envíe su saldo actual.

### B. Room + SQLCipher: historial privado de cada celular

Se crea en `DatabaseProvider.kt` y contiene `TransactionEntity`.

- Room facilita consultas y observación del historial.
- SQLCipher cifra el archivo de la base local.
- Recibir el mismo comprobante por REST y WebSocket usa un `upsert` con su ID: reemplaza el registro existente en lugar de crear otra fila con ese mismo ID.
- Permite consultar movimientos guardados cuando falla la red, con una sesión local disponible.
- No permite ejecutar transferencias sin servidor ni convierte al celular en autoridad del saldo.

El historial puede sobrevivir a cerrar y reabrir la app. **Cerrar sesión borra el historial local**, y al iniciar otra sesión se vuelve a sincronizar.

### C. Mapa WebSocket: conexiones en memoria

`src/realtime/hub.js` mantiene un `Map` de cuenta → conjunto de sockets. Eso permite dirigir un evento a las conexiones de una cuenta concreta.

El mapa no es una base financiera. Si se reinicia el proceso, los sockets se pierden y los clientes deben reconectarse.

### Una consecuencia importante

Si Render pierde su SQLite pero el celular conserva Room, el celular puede tener comprobantes antiguos que ya no están en el servidor recién inicializado. `syncTransactions()` agrega o reemplaza los últimos movimientos recibidos; no reconstruye el dinero del servidor desde Room.

Esto explica una posible **diferencia entre historial local y estado actual del servidor**, no demuestra por sí solo dinero duplicado.

## 4. ¿Por qué funciona desde redes diferentes?

Un celular puede usar Wi-Fi y otro datos móviles. Ambos envían solicitudes salientes al mismo dominio público de Render.

No se conectan directamente entre sí. Las cuentas se identifican por la autenticación, no por compartir una IP o una red.

```text
Ana en Wi-Fi ──────┐
                  ├── Internet ── mismo backend ── misma SQLite
Luis en 4G/5G ─────┘
```

La latencia puede variar entre redes. Eso afecta cuándo se ve una respuesta; **no debe cambiar la regla de descontar al emisor y abonar al receptor**.

## 5. Depósito y transferencia no son lo mismo

| Operación | Endpoint | Cuenta afectada |
|---|---|---|
| Depósito demo | `POST /api/v1/operations/deposit` | Aumenta la cuenta autenticada. No pide una cuenta destino. |
| Transferencia | `POST /api/v1/operations/transfer` | Descuenta de la cuenta autenticada y abona a `toAccountId`. |

Si se quiere enviar dinero de Ana a Luis, se usa **transferencia**. El depósito del proyecto simula una entrada de dinero; no está conectado a un banco ni prueba que se hayan recibido fondos externos.

## 6. ¿Qué debería pasar con una transferencia?

Ejemplo sin otras operaciones concurrentes:

```text
Antes:    Ana = 1500.00    Luis = 800.00     Total = 2300.00
Envío:    Ana transfiere 25.00 a Luis
Después:  Ana = 1475.00    Luis = 825.00     Total = 2300.00
```

En `src/routes/operations.js` se calcula:

```javascript
const fromBalance = fromAccount.balance_cents - body.amountCents;
const toBalance = toAccount.balance_cents + body.amountCents;
```

Las dos actualizaciones y los dos comprobantes se ejecutan dentro de la transacción SQLite creada por `runIdempotent()` en `src/security/idempotency.js`.

**Atomicidad:** si la transacción falla, no debería quedar confirmado solamente un lado de esa operación. Esto es una transacción de una base SQLite central, no una transacción entre dos bases financieras de los celulares.

Se generan dos movimientos:

- `TRANSFER_SENT` para el emisor.
- `TRANSFER_RECEIVED` para el receptor.

Tienen IDs diferentes y el mismo `referenceId`. **Dos comprobantes no significan dos transferencias.** El campo `amountCents` es positivo en ambos; el tipo indica ingreso o egreso. Sumarlos sin interpretar el tipo lleva a una conclusión equivocada.

## 7. Sobre la aparente duplicación reportada

Se ha descrito este síntoma: llega dinero al destino, pero también parece aumentar el saldo de quien envió.

**No se ha reproducido ni confirmado aquí su causa.** El código revisado de transferencia resta al emisor; el frontend asigna los saldos recibidos, no suma el importe de la respuesta REST y después vuelve a sumarlo por WebSocket. No sería correcto afirmar que “Render duplica el dinero” o que “ocurre por estar en redes diferentes”.

### Posibilidades que deben distinguirse

| Posibilidad | Qué puede explicar | Cómo comprobarla |
|---|---|---|
| Respuestas o eventos con saldos antiguos | Un saldo vuelve a un valor anterior y parece subir. | Comparar lo mostrado con un nuevo GET `/balance`. |
| REST, sincronización y WebSocket se solapan | El último mensaje procesado puede reemplazar un saldo más reciente. | Registrar orden, hora y contenido de respuestas y eventos. |
| Reinicio y seed de Render | Saldos vuelven a los valores iniciales y no coinciden con Room. | Revisar reinicios/despliegues y movimientos del servidor. |
| Dos dispositivos usan la misma cuenta | Acciones de otra sesión cambian el mismo saldo. | Verificar `/auth/me` de cada token y el número de cuenta en cada app. |
| Se enviaron varias operaciones | Hay múltiples movimientos reales. | Contar IDs, referencias y claves de idempotencia. |
| Se compara local con Render, o versiones distintas | Las pantallas no consultan la misma base o no ejecutan el mismo código. | Comprobar variante, URL y versión desplegada. |

El frontend actual no compara una versión monotónica del saldo antes de aplicar cada respuesta/evento. Por ejemplo, un GET iniciado antes de una transferencia podría terminar después y mostrar su saldo anterior. Eso sería un problema de orden de actualizaciones visibles, **no una prueba de que SQLite creó dinero**.

También habría que revisar callbacks pendientes de sockets al cambiar de cuenta si la incidencia ocurre justo después de cerrar e iniciar sesión. Es una hipótesis a investigar, no una causa demostrada.

### Alcance real de la idempotencia

Reintentar la misma solicitud con **la misma clave y el mismo cuerpo** devuelve la respuesta guardada sin ejecutar otra operación. Usar esa clave con otro cuerpo u operación devuelve `409`.

Sin embargo, `WalletRepository.kt` genera un UUID nuevo para cada invocación nueva. Dos pulsaciones que finalmente originan llamadas distintas, dos celulares o un reenvío manual con otra clave **no representan el mismo reintento para el servidor**. El `Mutex` de la app evita simultaneidad local durante una operación; no es un bloqueo global entre todos los dispositivos.

Repetir una transferencia válida debería volver a descontar al emisor. **Por sí sola, esta repetición no explica que ambos saldos aumenten.**

## 8. Procedimiento para diagnosticar la incidencia

1. Confirmar que ambos dispositivos usan Render y cuentas distintas.
2. Con el token de cada cuenta, consultar `GET /api/v1/auth/me` y `GET /api/v1/operations/balance`.
3. Anotar los saldos iniciales del servidor, no solo los de pantalla.
4. Detener otras operaciones y hacer una sola transferencia pequeña con una clave nueva.
5. Guardar el JSON de respuesta, su `referenceId`, `transaction.id` y la hora.
6. Consultar otra vez `/balance` para ambas cuentas.
7. Consultar `/transactions?limit=100` con cada token y buscar la referencia.
8. Comparar con las pantallas y pulsar Actualizar.
9. Revisar en Render si hubo reinicio o redespliegue entre las lecturas.

Interpretación:

- **Servidor correcto y pantalla incorrecta:** investigar sincronización y orden de mensajes del cliente.
- **Servidor incorrecto incluso con una nueva consulta:** investigar logs, movimientos y versión desplegada; no descartarlo como “solo visual”.
- **Varias referencias:** hubo varias operaciones; revisar reenvíos e idempotencia.
- **Misma referencia, un comprobante por cuenta:** estructura esperada de una transferencia.
- **Regreso al seed y desaparición de movimientos:** investigar pérdida del archivo SQLite.

Si solo hay una transferencia y ningún depósito/retiro/pago concurrente, verificar que la suma de ambos saldos se conserva.

## 9. Respuestas cortas para el profesor

**¿Es una base distribuida?** No. Es una aplicación distribuida cliente-servidor, con una base financiera central y cachés locales en los clientes.

**¿Hace falta la misma red?** No. Ambos clientes llegan al mismo servicio público mediante internet.

**¿SQLite es necesariamente en memoria?** No. Aquí se usa un archivo; el problema de persistencia de Render Free es su almacenamiento efímero.

**¿Un WebSocket acredita dinero?** No. La transacción REST modifica SQLite y después el servidor emite el evento para informar el resultado.

**¿Cómo se conservarían los datos de forma más duradera?** Con almacenamiento persistente adecuado para SQLite o migrando a una base administrada. No está implementado en esta versión.

**¿La app maneja dólares reales?** El símbolo `$` es visual; el servidor sigue declarando `PEN`. No hay conversión de moneda ni integración bancaria.

## 10. Referencias de implementación

- Backend: `render.yaml`, `src/server.js`, `src/db.js`, `src/seed.js`, `src/routes/operations.js`, `src/security/idempotency.js`, `src/realtime/hub.js`.
- Android, bajo `app/src/main/java/com/uc1/nexopay/`: `data/local/DatabaseProvider.kt`, `data/repository/WalletRepository.kt`, `data/remote/WalletSocketManager.kt`, `ui/dashboard/DashboardViewModel.kt`.
- Documentación oficial de Render: <https://render.com/docs/free> y <https://render.com/docs/disks>.

Continuar con [la explicación del código](02-EXPLICACION-SENCILLA-DEL-CODIGO.md).
