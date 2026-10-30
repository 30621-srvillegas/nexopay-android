# GUÍA ULTRA-DETALLADA V2 — NexoPay: Billetera Móvil Distribuida con Fastify, SQLite, Room y WebSockets

> **Proyecto:** Billetera móvil distribuida UC1 — Sistemas Distribuidos.
> **Objetivo:** cumplir al 100 % el Word y apuntar a **20/20** en la rúbrica, con un capítulo de seguridad sólido para el informe y la defensa del **29-10-2026**.
> **Stack V2:** Android Kotlin + **XML Views + Material 3** + ViewBinding + RecyclerView + Room + SQLCipher + Retrofit/OkHttp + kotlinx.serialization + Corrutinas/Flow. Backend **Node.js 22 + Fastify 5 + SQLite** + WebSocket + JWT de acceso corto + refresh rotativo.
> **Diferencia clave con la guía original:** no se usa Compose, no se usa Express, no se usa Gson, no se guarda el JWT en query del WebSocket y no se trabaja con `Double` para dinero. La app es la misma idea distribuida, pero la implementación es distinta y con más controles de seguridad.

---

## ÍNDICE

1. [Cómo usar esta guía y qué entregarás](#1-cómo-usar-esta-guía-y-qué-entregarás)
2. [Matriz de trazabilidad Word → guía → evidencia](#2-matriz-de-trazabilidad-word--guía--evidencia)
3. [Timeline de 14 días](#3-timeline-de-14-días)
4. [Arquitectura distribuida y decisiones técnicas](#4-arquitectura-distribuida-y-decisiones-técnicas)
5. [FASE A — Backend Fastify + SQLite + seguridad](#5-fase-a--backend-fastify--sqlite--seguridad)
6. [FASE B — Proyecto Android base, XML y Material 3](#6-fase-b--proyecto-android-base-xml-y-material-3)
7. [FASE C — Room + SQLCipher](#7-fase-c--room--sqlcipher)
8. [FASE D — Red: Retrofit, OkHttp, refresh automático](#8-fase-d--red-retrofit-okhttp-refresh-automático)
9. [FASE E — WebSocket en tiempo real](#9-fase-e--websocket-en-tiempo-real)
10. [FASE F — Concurrencia, corrutinas y ViewModel](#10-fase-f--concurrencia-corrutinas-y-viewmodel)
11. [FASE G — UI XML: dashboard, bottom sheets e historial](#11-fase-g--ui-xml-dashboard-bottom-sheets-e-historial)
12. [FASE H — Seguridad](#12-fase-h--seguridad)
13. [FASE I — Pruebas y evidencias](#13-fase-i--pruebas-y-evidencias)
14. [FASE J — APK, firma, GitHub y CI](#14-fase-j--apk-firma-github-y-ci)
15. [FASE K — Informe APA 7](#15-fase-k--informe-apa-7)
16. [FASE L — Guion de defensa (29-10-2026)](#16-fase-l--guion-de-defensa-29-10-2026)
17. [Checklists y errores que bajan nota](#17-checklists-y-errores-que-bajan-nota)
18. [Anexos: Postman, curl, SQL, FAQ y solución de problemas](#18-anexos-postman-curl-sql-faq-y-solución-de-problemas)

---

## 1. CÓMO USAR ESTA GUÍA Y QUÉ ENTREGARÁS

### 1.1. Paquete de entrega

1. **Código fuente Android** en GitHub/GitLab, con Room, Retrofit/OkHttp y corrutinas visibles.
2. **Backend** en el mismo repositorio o en otro repositorio enlazado, con instrucciones de ejecución.
3. **APK** compilado e instalable, probado en emulador y en un celular real.
4. **Colección Postman** con login, refresh, las 4 operaciones, balance y ticket WebSocket.
5. **Informe técnico APA 7** con arquitectura, seguridad, pruebas y conclusiones.
6. **Defensa por Zoom del 29-10-2026**: celular/emulador a un lado, Postman o terminal al otro. Debes demostrar que una transferencia enviada desde Postman actualiza el saldo de la app en tiempo real por WebSocket, sin recargar.

### 1.2. Convenciones

- `ruta:línea` indica dónde va el archivo o el bloque.
- `✅ Verificación` significa que no debes avanzar si no se cumple.
- `⚠️ Error típico` es una causa real de pérdida de puntos.
- Todo el dinero se maneja en **centavos enteros** (`Long` en Android, `INTEGER` en SQLite). Ejemplo: `S/ 25.50` → `2550`. Esto evita errores de precisión de `Double`.
- Los bloques de código son copiar-pegar, salvo que se indique “reemplaza”.

### 1.3. Requisitos de tu PC

- Windows 10/11, 8 GB de RAM mínimo (16 GB recomendado), 25 GB libres.
- **Android Studio Ladybug o superior**.
- **JDK 17** (Android Studio lo incluye; si no, instálalo).
- **Node.js 22 LTS** (`node -v` debe mostrar `v22.x`).
- **Git**, **Postman Desktop** y un navegador.
- Android SDK **API 35** y **Platform-Tools**; emulador Pixel 6 con **API 34**.
- Cuenta GitHub.

> Si tu PC usa `D:`, cambia las rutas de ejemplo. La guía usa `C:\NexoPayBackend` y `C:\Users\Usuario-PC\AndroidStudioProjects\NexoPay`.

### 1.4. Versiones fijadas (no las cambies sin leer el anexo)

| Componente | Versión |
|---|---|
| Node.js | 22 LTS |
| Fastify | 5.12.x |
| `@fastify/websocket` | 11.x |
| `@fastify/jwt` | 9.x |
| `@fastify/rate-limit` | 10.x |
| `@fastify/helmet` | 13.x |
| `@fastify/cors` | 11.x |
| `better-sqlite3` | 13.x |
| `hash-wasm` (Argon2id) | 4.x |
| AGP | 8.7.2 |
| Gradle | 8.9 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| compileSdk / targetSdk / minSdk | 35 / 34 / 26 |
| Room | 2.7.0 |
| SQLCipher Android | 4.19.1 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| kotlinx.serialization | 1.7.3 |

---

## 2. MATRIZ DE TRAZABILIDAD WORD → GUÍA → EVIDENCIA

| Requisito del Word | Dónde se implementa | Evidencia para el informe/defensa |
|---|---|---|
| Cliente móvil Android/Kotlin | Fase B, G | Proyecto Android Studio, APK, capturas |
| Room/SQLite con entidades y DAOs | Fase C | `TransactionEntity`, `TransactionDao`, `AppDatabase`, prueba de reinicio |
| Historial sobrevive reinicio | Fase C + Fase I | Video: operar, force-stop, reabrir, historial intacto |
| Historial visible sin internet | Fase C + Fase D | Modo avión y reabrir: Room muestra la caché; un fallo de red no borra la sesión ni el historial |
| Depósito REST | Fase A + D | Postman y app; saldo suma |
| Retiro REST con fondos suficientes | Fase A + D | Retiro válido y retiro con saldo insuficiente sin crash |
| Transferencia P2P REST + WebSocket | Fase A + D + E | Postman envía a la app y el saldo sube sin recargar |
| Pago de servicios con código de contrato | Fase A + D + G | Pago `LUZ-001`, fila “Servicio: Luz” con icono distinto |
| Corrutinas: red/BD en IO, UI en Main | Fase F | `viewModelScope.launch(Dispatchers.IO)`, `StateFlow` recolectado con `repeatOnLifecycle`, StrictMode |
| Cero ANR | Fase F + I | Scroll mientras se opera; Logcat sin ANR |
| Dashboard con saldo grande | Fase G | `activity_dashboard.xml`, tarjeta superior |
| Indicador En línea/Desconectado | Fase E + G | Chip verde/rojo conectado al `StateFlow` del socket |
| 4 botones con iconos y diálogos | Fase G | Grid de 4 `MaterialButton` + bottom sheets |
| Historial reactivo | Fase C + G | `RecyclerView` + `Flow` de Room + `ListAdapter` |
| Verde ingresos / rojo egresos | Fase G | `TransactionAdapter` con colores condicionales |
| APK instalable | Fase J | `app-prod-debug.apk` o release firmado |
| GitHub/GitLab | Fase J | Repositorio público + README + release |
| Defensa con Postman | Fase L | Guion y colección Postman |
| Informe APA 7 | Fase K | Documento con portada, tablas y referencias |
| Seguridad adicional | Fase H | JWT corto + refresh rotativo, Argon2id, ticket WS, idempotencia, SQLCipher, Keystore, TLS/WSS |

> **Nota importante:** el Word permite **“Jetpack Compose o XML Layouts”**. Esta guía usa XML Layouts; no estás incumpliendo nada. La arquitectura sigue siendo Android + REST + WebSocket + Room + corrutinas.

---

## 3. TIMELINE DE 14 DÍAS

| Día | Meta | Verificación |
|---|---|---|
| 1 | Backend: proyecto, SQLite, esquema, seed | `npm start` y `/health` responde |
| 2 | Auth: login, refresh, logout, bloqueo | Postman login OK y 401 con clave mala |
| 3 | 4 operaciones + idempotencia + ticket WS | Postman: depósito, retiro, transferencia, servicio |
| 4 | WebSocket + pruebas backend + Render | Postman WS recibe `transfer.received` |
| 5 | Android: proyecto, Gradle, tema, layouts | App abre login sin errores |
| 6 | Room + SQLCipher | Insertar y leer; reiniciar y persiste |
| 7 | Retrofit + refresh automático + repositorio | Login y depósito desde la app |
| 8 | WebSocket + reconexión | Chip verde; transferencia desde Postman actualiza saldo |
| 9 | ViewModel + corrutinas + estados | Operaciones sin ANR; errores controlados |
| 10 | UI final: bottom sheets, adapter, colores | Las 4 operaciones desde la app |
| 11 | Seguridad + pruebas + capturas | 401, 422, 429, idempotencia, SQLCipher |
| 12 | APK release + celular real + GitHub | Instalar APK y operar contra Render |
| 13 | Informe APA + ensayo de defensa | Documento completo y guion cronometrado |
| 14 | Defensa | Demo en vivo sin recargar la app |

> Si vas atrasado, recorta adornos visuales. **Nunca recortes** Room, REST, WebSocket, corrutinas, manejo de errores ni APK.

---

## 4. ARQUITECTURA DISTRIBUIDA Y DECISIONES TÉCNICAS

### 4.1. Diagrama

```text
┌──────────────────────────────┐        HTTPS / REST + JSON        ┌──────────────────────────────┐
│  APP ANDROID (Kotlin + XML)  │ ────────────────────────────────▶ │  BACKEND FASTIFY :3000       │
│                              │                                   │                              │
│  Main thread: Activities,    │ ◀──────────────────────────────── │  /api/v1/auth/*              │
│  RecyclerView, Material 3    │        WSS / eventos push         │  /api/v1/operations/*        │
│                              │                                   │  /api/v1/realtime/ticket     │
│  IO: Retrofit, Room,         │                                   │  /ws?ticket=...              │
│  SQLCipher, WebSocket        │                                   │                              │
│                              │                                   │  SQLite autoritativo         │
│  Room local: historial       │                                   │  WAL + transacciones         │
└──────────────────────────────┘                                   └──────────────────────────────┘
```

### 4.2. Decisiones y por qué

| Decisión | Motivo |
|---|---|
| XML + Material 3 en vez de Compose | El Word lo permite; demuestra ViewBinding, RecyclerView y ciclo de vida. Es un frontend claramente distinto. |
| Fastify en vez de Express | Mejor rendimiento, plugins oficiales, validación y logs estructurados. |
| SQLite autoritativo en backend | El saldo se calcula en servidor con transacciones; Room es historial local. |
| Dinero en centavos | Evita `0.1 + 0.2 != 0.3`; el servidor nunca usa `Double` para saldos. |
| Access token 15 min + refresh rotativo | Reduce el impacto de un token robado; el refresh se detecta si se reutiliza. |
| Ticket WS de un solo uso | Evita poner el JWT en la URL y en logs. |
| Idempotency-Key | Evita doble depósito si el usuario reintenta por mala red. |
| SQLCipher + Keystore | El historial local queda cifrado; la clave no se guarda en texto plano. |
| `adb reverse` y flavors | Permite probar en celular real sin depender de la IP de la PC. |

### 4.3. Cuentas demo

| Cuenta | Dueño | Clave | Uso |
|---|---|---|---|
| `10020001` | Ana Torres | `Nexo#2026` | App Android |
| `10020002` | Luis Rojas | `Nexo#2026` | Postman / otro usuario |

### 4.4. Flujo de una transferencia P2P

1. La app de Ana envía `POST /api/v1/operations/transfer` con su access token, `toAccountId=10020002` y `amountCents`.
2. Fastify valida, abre una transacción SQLite, descuenta a Ana y abona a Luis.
3. Fastify inserta dos filas en `transactions` (enviada y recibida) con el mismo `reference_id`.
4. Fastify responde a Ana por REST.
5. Fastify busca los sockets de Luis y les envía `transfer.received`.
6. La app de Luis (si está abierta) recibe el evento, guarda el comprobante en Room y actualiza el saldo en pantalla sin recargar.

### 4.5. Contrato de la API

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/login` | No | Login, devuelve access + refresh |
| POST | `/api/v1/auth/refresh` | No | Rota refresh y devuelve nuevo par |
| POST | `/api/v1/auth/logout` | No | Revoca la familia de refresh |
| GET | `/api/v1/auth/me` | Bearer | Datos de la cuenta |
| POST | `/api/v1/operations/deposit` | Bearer + Idempotency-Key | Depósito |
| POST | `/api/v1/operations/withdraw` | Bearer + Idempotency-Key | Retiro |
| POST | `/api/v1/operations/transfer` | Bearer + Idempotency-Key | Transferencia P2P |
| POST | `/api/v1/operations/pay-service` | Bearer + Idempotency-Key | Pago de servicio |
| GET | `/api/v1/operations/balance` | Bearer | Saldo actual |
| GET | `/api/v1/operations/transactions` | Bearer | Últimos movimientos (para sincronizar Room) |
| POST | `/api/v1/realtime/ticket` | Bearer | Ticket WS de 30 s |
| GET | `/ws?ticket=...` | Ticket | WebSocket de eventos |
| GET | `/health` | No | Estado del servicio |

### 4.6. Formato de error uniforme

```json
{
  "error": {
    "code": "insufficient_funds",
    "message": "Saldo insuficiente",
    "details": { "balanceCents": 2500 },
    "requestId": "req-123"
  }
}
```

Códigos que la app debe mapear: `invalid_credentials`, `account_locked`, `insufficient_funds`, `validation_error`, `rate_limited`, `idempotency_conflict`, `invalid_access_token`, `destination_not_found`.

---

## 5. FASE A — BACKEND FASTIFY + SQLITE + SEGURIDAD

> Meta: en esta fase tendrás un backend real, con saldo autoritativo, autenticación con refresh rotativo, las 4 operaciones, WebSocket con ticket de un solo uso, rate limit e idempotencia. Todo se prueba con Postman antes de tocar Android.

### 5.1. Crear el proyecto

Abre PowerShell:

```powershell
Test-Path "C:\NexoPayBackend"
New-Item -ItemType Directory -Path "C:\NexoPayBackend"
Set-Location "C:\NexoPayBackend"
node -v
npm -v
npm init -y
npm install fastify @fastify/cors @fastify/helmet @fastify/jwt @fastify/rate-limit @fastify/websocket better-sqlite3 dotenv hash-wasm zod ws
npm install --save-dev pino-pretty
```

✅ `node -v` muestra `v22.x` y se crea `package.json`.

Crea esta estructura:

```text
C:\NexoPayBackend
├─ package.json
├─ .env
├─ .env.example
├─ .gitignore
├─ data/                  (se crea sola al arrancar)
├─ src/
│  ├─ app.js
│  ├─ server.js
│  ├─ config.js
│  ├─ db.js
│  ├─ seed.js
│  ├─ security/
│  │  ├─ passwords.js
│  │  ├─ tokens.js
│  │  ├─ idempotency.js
│  │  └─ authGuard.js
│  ├─ routes/
│  │  ├─ auth.js
│  │  ├─ operations.js
│  │  └─ realtime.js
│  ├─ realtime/
│  │  └─ hub.js
│  └─ utils/
│     ├─ errors.js
│     ├─ validate.js
│     └─ money.js
└─ test/
   ├─ helpers.js
   ├─ auth.test.js
   ├─ operations.test.js
   └─ realtime.test.js
```

### 5.2. `package.json`

Ruta: `C:\NexoPayBackend\package.json`

```json
{
  "name": "nexopay-backend",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "description": "Backend distribuido de NexoPay UC1",
  "engines": {
    "node": ">=22"
  },
  "scripts": {
    "start": "node src/server.js",
    "dev": "node --watch src/server.js",
    "seed": "node src/seed.js",
    "test": "node --test --test-concurrency=1"
  },
  "dependencies": {
    "@fastify/cors": "^11.0.0",
    "@fastify/helmet": "^13.0.0",
    "@fastify/jwt": "^9.0.0",
    "@fastify/rate-limit": "^10.0.0",
    "@fastify/websocket": "^11.3.4",
    "better-sqlite3": "^13.0.0",
    "dotenv": "^16.6.0",
    "fastify": "^5.6.0",
    "hash-wasm": "^4.12.0",
    "ws": "^8.18.0",
    "zod": "^3.25.0"
  },
  "devDependencies": {
    "pino-pretty": "^13.0.0"
  }
}
```

> `ws` se declara porque las pruebas de WebSocket lo usan directamente. `@fastify/websocket` también lo usa internamente.

### 5.3. Variables de entorno

Ruta: `C:\NexoPayBackend\.env.example`

```env
PORT=3000
HOST=0.0.0.0
NODE_ENV=development
JWT_ACCESS_SECRET=CAMBIA_ESTE_SECRETO_POR_UNO_DE_64_CARACTERES_MINIMO_2026
ACCESS_TTL_SECONDS=900
REFRESH_TTL_DAYS=7
DB_PATH=./data/nexopay.db
CORS_ORIGINS=http://localhost:3000
```

Copia ese archivo a `.env` y cambia el secreto:

```powershell
Copy-Item ".env.example" ".env"
```

Ruta: `C:\NexoPayBackend\.gitignore`

```gitignore
node_modules/
.env
data/
*.db
*.db-shm
*.db-wal
coverage/
.DS_Store
```

⚠️ **Nunca subas `.env` a GitHub.** El secreto de producción se configura en Render como variable de entorno.

### 5.4. `src/config.js`

```javascript
import 'dotenv/config';

const env = process.env.NODE_ENV ?? 'development';
const isProd = env === 'production';

function readSecret() {
  const value = process.env.JWT_ACCESS_SECRET;
  if (value && value.trim().length >= 32) return value.trim();
  if (isProd) throw new Error('JWT_ACCESS_SECRET es obligatorio en producción (mínimo 32 caracteres)');
  return 'dev_only_secret_no_usar_en_produccion_2026_nexopay';
}

export const config = {
  env,
  isProd,
  port: Number(process.env.PORT ?? 3000),
  host: process.env.HOST ?? '0.0.0.0',
  dbPath: process.env.DB_PATH ?? './data/nexopay.db',
  accessSecret: readSecret(),
  accessTtlSeconds: Number(process.env.ACCESS_TTL_SECONDS ?? 900),
  refreshTtlDays: Number(process.env.REFRESH_TTL_DAYS ?? 7),
  corsOrigins: (process.env.CORS_ORIGINS ?? 'http://localhost:3000')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean),
};
```

✅ Si ejecutas `node -e "import('./src/config.js').then(m=>console.log(m.config))"` no debe lanzar error en desarrollo.

### 5.5. `src/db.js` — SQLite autoritativo

```javascript
import Database from 'better-sqlite3';
import fs from 'node:fs';
import path from 'node:path';

export function openDatabase(dbPath = './data/nexopay.db') {
  if (dbPath !== ':memory:') {
    fs.mkdirSync(path.dirname(path.resolve(dbPath)), { recursive: true });
  }

  const db = new Database(dbPath);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');
  db.pragma('busy_timeout = 5000');
  db.pragma('synchronous = NORMAL');
  migrate(db);
  return db;
}

export function migrate(db) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS accounts (
      id TEXT PRIMARY KEY,
      owner TEXT NOT NULL,
      currency TEXT NOT NULL DEFAULT 'PEN',
      balance_cents INTEGER NOT NULL CHECK (balance_cents >= 0),
      status TEXT NOT NULL DEFAULT 'ACTIVE',
      failed_attempts INTEGER NOT NULL DEFAULT 0,
      locked_until INTEGER,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS password_credentials (
      account_id TEXT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
      password_hash TEXT NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS refresh_tokens (
      id TEXT PRIMARY KEY,
      account_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
      token_hash TEXT NOT NULL UNIQUE,
      family_id TEXT NOT NULL,
      expires_at INTEGER NOT NULL,
      revoked_at INTEGER,
      replaced_by TEXT,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS transactions (
      id TEXT PRIMARY KEY,
      account_id TEXT NOT NULL REFERENCES accounts(id),
      type TEXT NOT NULL,
      amount_cents INTEGER NOT NULL,
      balance_after_cents INTEGER NOT NULL,
      counterparty_account_id TEXT,
      contract_code TEXT,
      service_name TEXT,
      description TEXT,
      reference_id TEXT,
      created_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_transactions_account_created
      ON transactions(account_id, created_at DESC);

    CREATE TABLE IF NOT EXISTS idempotency_keys (
      account_id TEXT NOT NULL,
      idempotency_key TEXT NOT NULL,
      request_hash TEXT NOT NULL,
      status_code INTEGER NOT NULL,
      response_json TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      PRIMARY KEY (account_id, idempotency_key)
    );

    CREATE TABLE IF NOT EXISTS ws_tickets (
      ticket_hash TEXT PRIMARY KEY,
      account_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
      expires_at INTEGER NOT NULL,
      used_at INTEGER,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS audit_log (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      account_id TEXT,
      action TEXT NOT NULL,
      detail TEXT,
      ip TEXT,
      created_at INTEGER NOT NULL
    );
  `);
}
```

> `journal_mode = WAL` permite lecturas mientras se escribe. `busy_timeout` evita errores inmediatos si dos operaciones coinciden. Las operaciones de dinero se ejecutan dentro de `db.transaction()`.

### 5.6. `src/security/passwords.js` — Argon2id sin compilador nativo

```javascript
import { argon2id, argon2Verify } from 'hash-wasm';
import { randomBytes } from 'node:crypto';

const ARGON2_PARAMS = {
  parallelism: 1,
  iterations: 3,
  memorySize: 64 * 1024, // 64 MB
  hashLength: 32,
};

export async function hashPassword(password) {
  const salt = randomBytes(16);
  return argon2id({
    password: password.normalize('NFKC'),
    salt,
    ...ARGON2_PARAMS,
    outputType: 'encoded',
  });
}

export async function verifyPassword(password, encodedHash) {
  if (!encodedHash) return false;
  return argon2Verify({
    password: password.normalize('NFKC'),
    hash: encodedHash,
  });
}
```

> `hash-wasm` es WebAssembly puro: funciona en Windows sin Visual Studio Build Tools. No cambies `memorySize` a un valor absurdamente alto o el login se vuelve lento en una PC de 8 GB.

### 5.7. `src/utils/errors.js`

```javascript
export class AppError extends Error {
  constructor(statusCode, code, message, details = undefined) {
    super(message);
    this.name = 'AppError';
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
  }
}
```

### 5.8. `src/utils/validate.js`

```javascript
import { z } from 'zod';
import { AppError } from './errors.js';

export const accountIdSchema = z
  .string()
  .regex(/^\d{8}$/, 'La cuenta debe tener exactamente 8 dígitos');

export const loginSchema = z.object({
  accountId: accountIdSchema,
  password: z.string().min(8).max(72),
});

export const refreshSchema = z.object({
  refreshToken: z.string().min(20).max(300),
});

export const amountSchema = z.object({
  amountCents: z.number().int().positive().max(10_000_000),
});

export const transferSchema = z.object({
  toAccountId: accountIdSchema,
  amountCents: z.number().int().positive().max(10_000_000),
});

export const payServiceSchema = z.object({
  contractCode: z
    .string()
    .regex(/^(LUZ|AGUA|NET|GAS)-[A-Z0-9]{3,12}$/, 'Código de contrato inválido'),
  amountCents: z.number().int().positive().max(10_000_000),
});

export const idempotencyKeySchema = z
  .string()
  .min(8)
  .max(64)
  .regex(/^[A-Za-z0-9_-]+$/, 'Idempotency-Key inválida');

export function parse(schema, data) {
  const result = schema.safeParse(data);
  if (!result.success) {
    throw new AppError(422, 'validation_error', 'Datos inválidos', result.error.flatten());
  }
  return result.data;
}

export function parseIdempotencyKey(value) {
  const result = idempotencyKeySchema.safeParse(value);
  if (!result.success) {
    throw new AppError(422, 'idempotency_key_invalid', 'Falta o es inválida la cabecera Idempotency-Key');
  }
  return result.data;
}
```

> `amountCents` máximo `10_000_000` = `S/ 100,000.00`. El mínimo positivo es 1 centavo.

### 5.9. `src/utils/money.js`

```javascript
export function formatCents(cents) {
  return (cents / 100).toFixed(2);
}

export function moneyView(cents) {
  return {
    amountCents: cents,
    amount: formatCents(cents),
    currency: 'PEN',
  };
}

export function balanceView(cents) {
  return {
    balanceCents: cents,
    balance: formatCents(cents),
  };
}
```

### 5.10. `src/security/idempotency.js`

```javascript
import crypto from 'node:crypto';
import { AppError } from '../utils/errors.js';

function stableStringify(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
  return `{${Object.keys(value)
    .sort()
    .map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`)
    .join(',')}}`;
}

export function requestHash(operation, body) {
  return crypto
    .createHash('sha256')
    .update(stableStringify({ operation, body }))
    .digest('hex');
}

/**
 * Ejecuta una operación de dinero una sola vez por (cuenta, operación, Idempotency-Key).
 * Si llega un reintento con el mismo cuerpo, devuelve la respuesta original.
 * Si llega la misma clave con otra operación o cuerpo, responde 409.
 */
export function runIdempotent(db, { accountId, key, operation, body }, execute) {
  if (!key) {
    throw new AppError(422, 'idempotency_key_required', 'Falta la cabecera Idempotency-Key');
  }

  const hash = requestHash(operation, body);
  const existing = db
    .prepare(
      'SELECT request_hash, status_code, response_json FROM idempotency_keys WHERE account_id = ? AND idempotency_key = ?',
    )
    .get(accountId, key);

  if (existing) {
    if (existing.request_hash !== hash) {
      throw new AppError(409, 'idempotency_conflict', 'La clave ya se usó con otra operación o cuerpo');
    }
    return {
      statusCode: existing.status_code,
      body: JSON.parse(existing.response_json),
      replayed: true,
    };
  }

  const transaction = db.transaction(() => {
    const result = execute();
    db.prepare(
      `INSERT INTO idempotency_keys
       (account_id, idempotency_key, request_hash, status_code, response_json, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`,
    ).run(accountId, key, hash, result.statusCode, JSON.stringify(result.body), Date.now());
    return { ...result, replayed: false };
  });

  try {
    return transaction();
  } catch (error) {
    if (
      error?.code === 'SQLITE_CONSTRAINT_PRIMARYKEY' ||
      error?.code === 'SQLITE_CONSTRAINT_UNIQUE'
    ) {
      const row = db
        .prepare(
          'SELECT request_hash, status_code, response_json FROM idempotency_keys WHERE account_id = ? AND idempotency_key = ?',
        )
        .get(accountId, key);
      if (row && row.request_hash === hash) {
        return { statusCode: row.status_code, body: JSON.parse(row.response_json), replayed: true };
      }
      throw new AppError(409, 'idempotency_conflict', 'Operación duplicada en conflicto');
    }
    throw error;
  }
}
```

> Si la operación lanza `AppError` (por ejemplo, saldo insuficiente), la transacción hace rollback y **no** se guarda la clave. Así el usuario puede reintentar cuando tenga saldo.

### 5.11. `src/security/tokens.js`

```javascript
import crypto from 'node:crypto';
import { config } from '../config.js';
import { AppError } from '../utils/errors.js';

export function randomToken(bytes = 32) {
  return crypto.randomBytes(bytes).toString('base64url');
}

export function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

export function signAccessToken(app, accountId) {
  return app.jwt.sign(
    { sub: accountId, typ: 'access' },
    { expiresIn: config.accessTtlSeconds },
  );
}

export function issueRefreshToken(db, accountId, familyId = crypto.randomUUID()) {
  const raw = randomToken(48);
  const id = crypto.randomUUID();
  const now = Date.now();
  const expiresAt = now + config.refreshTtlDays * 24 * 60 * 60 * 1000;

  db.prepare(
    `INSERT INTO refresh_tokens
     (id, account_id, token_hash, family_id, expires_at, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`,
  ).run(id, accountId, sha256(raw), familyId, expiresAt, now);

  return { raw, id, familyId, expiresAt };
}

export function revokeFamily(db, familyId) {
  db.prepare(
    'UPDATE refresh_tokens SET revoked_at = ? WHERE family_id = ? AND revoked_at IS NULL',
  ).run(Date.now(), familyId);
}

export function revokeByRaw(db, raw) {
  const row = db
    .prepare('SELECT family_id FROM refresh_tokens WHERE token_hash = ?')
    .get(sha256(raw));
  if (row) revokeFamily(db, row.family_id);
}

export function rotateRefreshToken(db, raw) {
  const hash = sha256(raw);
  const now = Date.now();

  return db.transaction(() => {
    const row = db.prepare('SELECT * FROM refresh_tokens WHERE token_hash = ?').get(hash);
    if (!row) throw new AppError(401, 'invalid_refresh', 'Refresh token inválido');

    if (row.revoked_at) {
      revokeFamily(db, row.family_id);
      throw new AppError(401, 'refresh_reused', 'Refresh token reutilizado; sesión revocada');
    }

    if (row.expires_at <= now) {
      throw new AppError(401, 'refresh_expired', 'Refresh token expirado');
    }

    const next = issueRefreshToken(db, row.account_id, row.family_id);
    db.prepare('UPDATE refresh_tokens SET revoked_at = ?, replaced_by = ? WHERE id = ?').run(
      now,
      next.id,
      row.id,
    );

    return { accountId: row.account_id, ...next };
  })();
}

export function issueWsTicket(db, accountId) {
  const raw = randomToken(32);
  const hash = sha256(raw);
  const now = Date.now();

  db.prepare(
    `INSERT INTO ws_tickets (ticket_hash, account_id, expires_at, created_at)
     VALUES (?, ?, ?, ?)`,
  ).run(hash, accountId, now + 30_000, now);

  return { ticket: raw, expiresInSeconds: 30 };
}

export function consumeWsTicket(db, raw) {
  const hash = sha256(raw);
  const now = Date.now();
  const row = db.prepare('SELECT * FROM ws_tickets WHERE ticket_hash = ?').get(hash);

  if (!row || row.used_at || row.expires_at <= now) return null;

  const result = db
    .prepare('UPDATE ws_tickets SET used_at = ? WHERE ticket_hash = ? AND used_at IS NULL')
    .run(now, hash);

  return result.changes === 1 ? row.account_id : null;
}
```

### 5.12. `src/security/authGuard.js`

```javascript
import { AppError } from '../utils/errors.js';

export async function authGuard(request) {
  try {
    await request.jwtVerify();
  } catch {
    throw new AppError(401, 'invalid_access_token', 'Access token inválido o expirado');
  }

  if (request.user?.typ !== 'access' || !request.user?.sub) {
    throw new AppError(401, 'invalid_access_token', 'El token no es un access token');
  }

  const account = request.server.db
    .prepare('SELECT * FROM accounts WHERE id = ?')
    .get(request.user.sub);

  if (!account || account.status !== 'ACTIVE') {
    throw new AppError(401, 'account_unavailable', 'Cuenta no disponible');
  }

  request.account = account;
}
```

### 5.13. `src/realtime/hub.js`

```javascript
export class RealtimeHub {
  constructor() {
    this.byAccount = new Map();
  }

  add(accountId, socket) {
    if (!this.byAccount.has(accountId)) this.byAccount.set(accountId, new Set());
    this.byAccount.get(accountId).add(socket);
  }

  remove(accountId, socket) {
    const set = this.byAccount.get(accountId);
    if (!set) return;
    set.delete(socket);
    if (set.size === 0) this.byAccount.delete(accountId);
  }

  sendToSocket(socket, event, data) {
    if (socket.readyState !== 1) return;
    try {
      socket.send(
        JSON.stringify({
          v: 1,
          event,
          data,
          ts: new Date().toISOString(),
        }),
      );
    } catch {
      // El socket se cerró entre la comprobación y el envío.
    }
  }

  send(accountId, event, data) {
    const set = this.byAccount.get(accountId);
    if (!set) return;
    for (const socket of set) this.sendToSocket(socket, event, data);
  }

  allSockets() {
    const all = [];
    for (const set of this.byAccount.values()) all.push(...set);
    return all;
  }
}
```

---

### 5.14. `src/routes/auth.js`

```javascript
import { config } from '../config.js';
import { AppError } from '../utils/errors.js';
import { parse, loginSchema, refreshSchema } from '../utils/validate.js';
import { verifyPassword } from '../security/passwords.js';
import {
  signAccessToken,
  issueRefreshToken,
  rotateRefreshToken,
  revokeByRaw,
} from '../security/tokens.js';

function accountView(account) {
  return {
    id: account.id,
    owner: account.owner,
    currency: account.currency,
    balanceCents: account.balance_cents,
    balance: (account.balance_cents / 100).toFixed(2),
  };
}

function audit(db, accountId, action, detail, ip) {
  db.prepare(
    'INSERT INTO audit_log (account_id, action, detail, ip, created_at) VALUES (?, ?, ?, ?, ?)',
  ).run(accountId, action, detail, ip, Date.now());
}

export async function authRoutes(app) {
  const { db } = app;

  app.post(
    '/api/v1/auth/login',
    { config: { rateLimit: { max: 5, timeWindow: '15 minutes' } } },
    async (request, reply) => {
      const body = parse(loginSchema, request.body);
      const row = db
        .prepare(
          `SELECT a.*, p.password_hash
           FROM accounts a
           JOIN password_credentials p ON p.account_id = a.id
           WHERE a.id = ?`,
        )
        .get(body.accountId);

      const generic = new AppError(401, 'invalid_credentials', 'Credenciales inválidas');
      if (!row) throw generic;
      if (row.status !== 'ACTIVE') {
        throw new AppError(423, 'account_locked', 'Cuenta bloqueada');
      }
      if (row.locked_until && row.locked_until > Date.now()) {
        throw new AppError(423, 'account_locked', 'Cuenta bloqueada temporalmente');
      }

      const ok = await verifyPassword(body.password, row.password_hash);
      if (!ok) {
        const attempts = row.failed_attempts + 1;
        const lockedUntil = attempts >= 5 ? Date.now() + 15 * 60 * 1000 : null;
        db.prepare(
          'UPDATE accounts SET failed_attempts = ?, locked_until = ? WHERE id = ?',
        ).run(attempts, lockedUntil, row.id);
        audit(db, row.id, 'LOGIN_FAILED', `attempts=${attempts}`, request.ip);
        throw generic;
      }

      db.prepare(
        'UPDATE accounts SET failed_attempts = 0, locked_until = NULL WHERE id = ?',
      ).run(row.id);

      const accessToken = signAccessToken(app, row.id);
      const refresh = issueRefreshToken(db, row.id);
      audit(db, row.id, 'LOGIN_OK', null, request.ip);

      return reply.send({
        accessToken,
        refreshToken: refresh.raw,
        tokenType: 'Bearer',
        expiresInSeconds: config.accessTtlSeconds,
        account: accountView(row),
      });
    },
  );

  app.post(
    '/api/v1/auth/refresh',
    { config: { rateLimit: { max: 30, timeWindow: '1 minute' } } },
    async (request, reply) => {
      const body = parse(refreshSchema, request.body);
      const rotated = rotateRefreshToken(db, body.refreshToken);
      const account = db
        .prepare('SELECT * FROM accounts WHERE id = ?')
        .get(rotated.accountId);

      if (!account || account.status !== 'ACTIVE') {
        throw new AppError(401, 'account_unavailable', 'Cuenta no disponible');
      }

      const accessToken = signAccessToken(app, account.id);
      audit(db, account.id, 'TOKEN_REFRESHED', null, request.ip);

      return reply.send({
        accessToken,
        refreshToken: rotated.raw,
        tokenType: 'Bearer',
        expiresInSeconds: config.accessTtlSeconds,
        account: accountView(account),
      });
    },
  );

  app.post(
    '/api/v1/auth/logout',
    { config: { rateLimit: { max: 30, timeWindow: '1 minute' } } },
    async (request, reply) => {
      const body = parse(refreshSchema, request.body);
      revokeByRaw(db, body.refreshToken);
      return reply.code(204).send();
    },
  );

  app.get('/api/v1/auth/me', { preHandler: app.authGuard }, async (request, reply) => {
    return reply.send(accountView(request.account));
  });
}
```

### 5.15. `src/routes/operations.js`

```javascript
import crypto from 'node:crypto';
import { AppError } from '../utils/errors.js';
import {
  parse,
  amountSchema,
  transferSchema,
  payServiceSchema,
  parseIdempotencyKey,
} from '../utils/validate.js';
import { runIdempotent } from '../security/idempotency.js';
import { balanceView } from '../utils/money.js';

const SERVICE_MAP = {
  LUZ: 'Luz',
  AGUA: 'Agua',
  NET: 'Internet',
  GAS: 'Gas',
};

function txView(row) {
  return {
    id: row.id,
    type: row.type,
    amountCents: row.amount_cents,
    amount: (row.amount_cents / 100).toFixed(2),
    balanceCents: row.balance_after_cents,
    balance: (row.balance_after_cents / 100).toFixed(2),
    counterpartyAccountId: row.counterparty_account_id ?? null,
    contractCode: row.contract_code ?? null,
    serviceName: row.service_name ?? null,
    description: row.description ?? null,
    referenceId: row.reference_id ?? null,
    createdAt: new Date(row.created_at).toISOString(),
  };
}

function insertTransaction(db, accountId, tx) {
  db.prepare(
    `INSERT INTO transactions
     (id, account_id, type, amount_cents, balance_after_cents, counterparty_account_id,
      contract_code, service_name, description, reference_id, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
  ).run(
    tx.id,
    accountId,
    tx.type,
    tx.amount_cents,
    tx.balance_after_cents,
    tx.counterparty_account_id,
    tx.contract_code,
    tx.service_name,
    tx.description,
    tx.reference_id,
    tx.created_at,
  );
}

function audit(db, accountId, action, detail, ip, now = Date.now()) {
  db.prepare(
    'INSERT INTO audit_log (account_id, action, detail, ip, created_at) VALUES (?, ?, ?, ?, ?)',
  ).run(accountId, action, detail, ip, now);
}

export async function operationRoutes(app) {
  const { db, hub } = app;

  app.post(
    '/api/v1/operations/deposit',
    { preHandler: app.authGuard },
    async (request, reply) => {
      const body = parse(amountSchema, request.body);
      const key = parseIdempotencyKey(request.headers['idempotency-key']);
      const accountId = request.account.id;

      const result = runIdempotent(db, { accountId, key, operation: 'deposit', body }, () => {
        const now = Date.now();
        const account = db.prepare('SELECT * FROM accounts WHERE id = ?').get(accountId);
        const newBalance = account.balance_cents + body.amountCents;

        db.prepare('UPDATE accounts SET balance_cents = ? WHERE id = ?').run(
          newBalance,
          accountId,
        );

        const tx = {
          id: crypto.randomUUID(),
          type: 'DEPOSIT',
          amount_cents: body.amountCents,
          balance_after_cents: newBalance,
          counterparty_account_id: null,
          contract_code: null,
          service_name: null,
          description: 'Depósito',
          reference_id: null,
          created_at: now,
        };
        insertTransaction(db, accountId, tx);
        audit(db, accountId, 'DEPOSIT', `amountCents=${body.amountCents}`, request.ip, now);

        return {
          statusCode: 200,
          body: { transaction: txView(tx), ...balanceView(newBalance) },
        };
      });

      if (!result.replayed) {
        hub.send(accountId, 'balance.updated', {
          ...balanceView(result.body.balanceCents),
          reason: 'deposit',
          transaction: result.body.transaction,
        });
      }

      return reply
        .code(result.statusCode)
        .send({ ...result.body, idempotentReplay: result.replayed });
    },
  );

  app.post(
    '/api/v1/operations/withdraw',
    { preHandler: app.authGuard },
    async (request, reply) => {
      const body = parse(amountSchema, request.body);
      const key = parseIdempotencyKey(request.headers['idempotency-key']);
      const accountId = request.account.id;

      const result = runIdempotent(db, { accountId, key, operation: 'withdraw', body }, () => {
        const now = Date.now();
        const account = db.prepare('SELECT * FROM accounts WHERE id = ?').get(accountId);

        if (account.balance_cents < body.amountCents) {
          throw new AppError(400, 'insufficient_funds', 'Saldo insuficiente', {
            ...balanceView(account.balance_cents),
          });
        }

        const newBalance = account.balance_cents - body.amountCents;
        db.prepare('UPDATE accounts SET balance_cents = ? WHERE id = ?').run(
          newBalance,
          accountId,
        );

        const tx = {
          id: crypto.randomUUID(),
          type: 'WITHDRAWAL',
          amount_cents: body.amountCents,
          balance_after_cents: newBalance,
          counterparty_account_id: null,
          contract_code: null,
          service_name: null,
          description: 'Retiro',
          reference_id: null,
          created_at: now,
        };
        insertTransaction(db, accountId, tx);
        audit(db, accountId, 'WITHDRAWAL', `amountCents=${body.amountCents}`, request.ip, now);

        return {
          statusCode: 200,
          body: { transaction: txView(tx), ...balanceView(newBalance) },
        };
      });

      if (!result.replayed) {
        hub.send(accountId, 'balance.updated', {
          ...balanceView(result.body.balanceCents),
          reason: 'withdrawal',
          transaction: result.body.transaction,
        });
      }

      return reply
        .code(result.statusCode)
        .send({ ...result.body, idempotentReplay: result.replayed });
    },
  );

  app.post(
    '/api/v1/operations/transfer',
    { preHandler: app.authGuard },
    async (request, reply) => {
      const body = parse(transferSchema, request.body);
      const key = parseIdempotencyKey(request.headers['idempotency-key']);
      const from = request.account;

      if (from.id === body.toAccountId) {
        throw new AppError(422, 'same_account', 'No puedes transferirte a ti mismo');
      }
      const destination = db
        .prepare('SELECT * FROM accounts WHERE id = ?')
        .get(body.toAccountId);
      if (!destination) {
        throw new AppError(404, 'destination_not_found', 'Cuenta destino no existe');
      }

      const result = runIdempotent(db, { accountId: from.id, key, operation: 'transfer', body }, () => {
        const now = Date.now();
        const fromAccount = db.prepare('SELECT * FROM accounts WHERE id = ?').get(from.id);
        const toAccount = db
          .prepare('SELECT * FROM accounts WHERE id = ?')
          .get(body.toAccountId);

        if (fromAccount.balance_cents < body.amountCents) {
          throw new AppError(400, 'insufficient_funds', 'Saldo insuficiente', {
            ...balanceView(fromAccount.balance_cents),
          });
        }

        const fromBalance = fromAccount.balance_cents - body.amountCents;
        const toBalance = toAccount.balance_cents + body.amountCents;

        db.prepare('UPDATE accounts SET balance_cents = ? WHERE id = ?').run(
          fromBalance,
          from.id,
        );
        db.prepare('UPDATE accounts SET balance_cents = ? WHERE id = ?').run(
          toBalance,
          to.id,
        );

        const referenceId = crypto.randomUUID();
        const sent = {
          id: crypto.randomUUID(),
          type: 'TRANSFER_SENT',
          amount_cents: body.amountCents,
          balance_after_cents: fromBalance,
          counterparty_account_id: to.id,
          contract_code: null,
          service_name: null,
          description: `A ${to.id}`,
          reference_id: referenceId,
          created_at: now,
        };
        const received = {
          id: crypto.randomUUID(),
          type: 'TRANSFER_RECEIVED',
          amount_cents: body.amountCents,
          balance_after_cents: toBalance,
          counterparty_account_id: from.id,
          contract_code: null,
          service_name: null,
          description: `De ${from.id}`,
          reference_id: referenceId,
          created_at: now,
        };

        insertTransaction(db, from.id, sent);
        insertTransaction(db, to.id, received);
        audit(db, from.id, 'TRANSFER_SENT', `to=${to.id};amountCents=${body.amountCents}`, request.ip, now);
        audit(db, to.id, 'TRANSFER_RECEIVED', `from=${from.id};amountCents=${body.amountCents}`, request.ip, now);

        return {
          statusCode: 200,
          body: {
            transaction: txView(sent),
            ...balanceView(fromBalance),
            toAccountId: to.id,
            referenceId,
          },
        };
      });

      if (!result.replayed) {
        const receivedRow = db
          .prepare(
            'SELECT * FROM transactions WHERE reference_id = ? AND account_id = ?',
          )
          .get(result.body.referenceId, to.id);

        if (receivedRow) {
          hub.send(to.id, 'transfer.received', {
            transaction: txView(receivedRow),
            fromAccountId: from.id,
            ...balanceView(receivedRow.balance_after_cents),
          });
        }

        hub.send(from.id, 'balance.updated', {
          ...balanceView(result.body.balanceCents),
          reason: 'transfer_sent',
          transaction: result.body.transaction,
        });
      }

      return reply
        .code(result.statusCode)
        .send({ ...result.body, idempotentReplay: result.replayed });
    },
  );

  app.post(
    '/api/v1/operations/pay-service',
    { preHandler: app.authGuard },
    async (request, reply) => {
      const body = parse(payServiceSchema, request.body);
      const key = parseIdempotencyKey(request.headers['idempotency-key']);
      const accountId = request.account.id;
      const prefix = body.contractCode.split('-')[0];
      const serviceName = SERVICE_MAP[prefix] ?? 'Servicio Básico';

      const result = runIdempotent(db, { accountId, key, operation: 'pay-service', body }, () => {
        const now = Date.now();
        const account = db.prepare('SELECT * FROM accounts WHERE id = ?').get(accountId);

        if (account.balance_cents < body.amountCents) {
          throw new AppError(400, 'insufficient_funds', 'Saldo insuficiente', {
            ...balanceView(account.balance_cents),
          });
        }

        const newBalance = account.balance_cents - body.amountCents;
        db.prepare('UPDATE accounts SET balance_cents = ? WHERE id = ?').run(
          newBalance,
          accountId,
        );

        const tx = {
          id: crypto.randomUUID(),
          type: 'SERVICE_PAYMENT',
          amount_cents: body.amountCents,
          balance_after_cents: newBalance,
          counterparty_account_id: null,
          contract_code: body.contractCode,
          service_name: serviceName,
          description: `Pago ${serviceName} · ${body.contractCode}`,
          reference_id: null,
          created_at: now,
        };
        insertTransaction(db, accountId, tx);
        audit(
          db,
          accountId,
          'SERVICE_PAYMENT',
          `contract=${body.contractCode};amountCents=${body.amountCents}`,
          request.ip,
          now,
        );

        return {
          statusCode: 200,
          body: { transaction: txView(tx), ...balanceView(newBalance) },
        };
      });

      if (!result.replayed) {
        hub.send(accountId, 'balance.updated', {
          ...balanceView(result.body.balanceCents),
          reason: 'service_payment',
          transaction: result.body.transaction,
        });
      }

      return reply
        .code(result.statusCode)
        .send({ ...result.body, idempotentReplay: result.replayed });
    },
  );

  app.get(
    '/api/v1/operations/balance',
    { preHandler: app.authGuard },
    async (request, reply) => {
      const account = db
        .prepare('SELECT * FROM accounts WHERE id = ?')
        .get(request.account.id);
      return reply.send({
        accountId: account.id,
        ...balanceView(account.balance_cents),
        currency: account.currency,
      });
    },
  );

  app.get(
    '/api/v1/operations/transactions',
    { preHandler: app.authGuard },
    async (request, reply) => {
      const rawLimit = Number(request.query?.limit ?? 50);
      const limit = Number.isFinite(rawLimit)
        ? Math.min(Math.max(Math.trunc(rawLimit), 1), 100)
        : 50;

      const rows = db
        .prepare(
          `SELECT * FROM transactions
           WHERE account_id = ?
           ORDER BY created_at DESC, id DESC
           LIMIT ?`,
        )
        .all(request.account.id, limit);

      return reply.send({ transactions: rows.map(txView) });
    },
  );
}
```

### 5.16. `src/routes/realtime.js`

```javascript
import { issueWsTicket, consumeWsTicket } from '../security/tokens.js';
import { balanceView } from '../utils/money.js';

export async function realtimeRoutes(app) {
  const { db, hub } = app;

  app.post(
    '/api/v1/realtime/ticket',
    {
      preHandler: app.authGuard,
      config: { rateLimit: { max: 30, timeWindow: '1 minute' } },
    },
    async (request, reply) => {
      return reply.send(issueWsTicket(db, request.account.id));
    },
  );

  app.get('/ws', { websocket: true }, (socket, request) => {
    const url = new URL(request.url, 'http://localhost');
    const ticket = url.searchParams.get('ticket');
    const accountId = ticket ? consumeWsTicket(db, ticket) : null;

    if (!accountId) {
      socket.close(4401, 'invalid_ticket');
      return;
    }

    const account = db.prepare('SELECT * FROM accounts WHERE id = ?').get(accountId);
    if (!account) {
      socket.close(4401, 'account_not_found');
      return;
    }

    hub.add(accountId, socket);
    hub.sendToSocket(socket, 'connection.ready', {
      accountId,
      ...balanceView(account.balance_cents),
    });

    socket.isAlive = true;
    socket.on('pong', () => {
      socket.isAlive = true;
    });
    socket.on('close', () => hub.remove(accountId, socket));
    socket.on('error', () => hub.remove(accountId, socket));
    socket.on('message', (raw) => {
      try {
        const message = JSON.parse(raw.toString());
        if (message?.type === 'ping') {
          hub.sendToSocket(socket, 'pong', { ts: Date.now() });
        }
      } catch {
        // El protocolo del cliente es opcional; los eventos van del servidor al cliente.
      }
    });
  });

  const pingInterval = setInterval(() => {
    for (const socket of hub.allSockets()) {
      if (socket.isAlive === false) {
        socket.terminate();
        continue;
      }
      socket.isAlive = false;
      try {
        socket.ping();
      } catch {
        // El socket ya se cerró.
      }
    }
  }, 30_000);

  pingInterval.unref();

  app.addHook('onClose', async () => {
    clearInterval(pingInterval);
  });
}
```

### 5.17. `src/app.js`

```javascript
import Fastify from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import jwt from '@fastify/jwt';
import rateLimit from '@fastify/rate-limit';
import websocket from '@fastify/websocket';
import { config } from './config.js';
import { authRoutes } from './routes/auth.js';
import { operationRoutes } from './routes/operations.js';
import { realtimeRoutes } from './routes/realtime.js';
import { authGuard } from './security/authGuard.js';
import { RealtimeHub } from './realtime/hub.js';
import { AppError } from './utils/errors.js';

function buildLogger(logger) {
  if (logger === false) return false;
  if (logger !== true) return logger;

  return {
    level: config.isProd ? 'info' : 'debug',
    redact: {
      paths: [
        'req.headers.authorization',
        'req.headers["idempotency-key"]',
        'req.body.password',
        'req.body.refreshToken',
      ],
      censor: '[REDACTED]',
    },
    ...(config.isProd
      ? {}
      : {
          transport: {
            target: 'pino-pretty',
            options: { translateTime: 'SYS:standard', ignore: 'pid,hostname' },
          },
        }),
  };
}

export async function buildApp({ db, logger = true }) {
  const app = Fastify({
    logger: buildLogger(logger),
    trustProxy: true,
    bodyLimit: 32 * 1024,
  });

  app.decorate('db', db);
  app.decorate('hub', new RealtimeHub());
  app.decorate('authGuard', authGuard);
  app.decorateRequest('account', null);

  await app.register(helmet, {
    contentSecurityPolicy: false,
    crossOriginResourcePolicy: { policy: 'same-site' },
    hsts: config.isProd
      ? { maxAge: 15_552_000, includeSubDomains: true }
      : false,
  });

  await app.register(cors, {
    origin: (origin, callback) => {
      if (!origin || config.corsOrigins.includes(origin)) {
        callback(null, true);
        return;
      }
      callback(null, false);
    },
  });

  await app.register(rateLimit, {
    max: 120,
    timeWindow: '1 minute',
    hook: 'onRequest',
  });

  await app.register(jwt, {
    secret: config.accessSecret,
  });

  await app.register(websocket, {
    options: { maxPayload: 16 * 1024 },
  });

  app.setErrorHandler((error, request, reply) => {
    if (error instanceof AppError) {
      return reply.code(error.statusCode).send({
        error: {
          code: error.code,
          message: error.message,
          details: error.details ?? null,
          requestId: request.id,
        },
      });
    }

    if (error.statusCode === 429) {
      return reply.code(429).send({
        error: {
          code: 'rate_limited',
          message: 'Demasiadas solicitudes. Espera un momento.',
          requestId: request.id,
        },
      });
    }

    request.log.error({ err: error }, 'Error no controlado');
    return reply.code(500).send({
      error: {
        code: 'internal_error',
        message: 'Error interno del servidor',
        requestId: request.id,
      },
    });
  });

  app.setNotFoundHandler((request, reply) => {
    return reply.code(404).send({
      error: {
        code: 'not_found',
        message: 'Ruta no encontrada',
        requestId: request.id,
      },
    });
  });

  app.get('/health', async () => ({
    ok: true,
    service: 'nexopay-backend',
    env: config.env,
    ts: new Date().toISOString(),
  }));

  await app.register(authRoutes);
  await app.register(operationRoutes);
  await app.register(realtimeRoutes);

  return app;
}
```

### 5.18. `src/server.js`

```javascript
import { config } from './config.js';
import { openDatabase } from './db.js';
import { ensureSeed } from './seed.js';
import { buildApp } from './app.js';

const db = openDatabase(config.dbPath);
await ensureSeed(db);

const app = await buildApp({ db });

async function shutdown(signal) {
  app.log.info(`Cerrando por ${signal}`);
  try {
    await app.close();
  } finally {
    db.close();
    process.exit(0);
  }
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

try {
  await app.listen({ port: config.port, host: config.host });
  app.log.info(`NexoPay backend en http://localhost:${config.port}`);
} catch (error) {
  app.log.error(error);
  process.exit(1);
}
```

### 5.19. `src/seed.js`

```javascript
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { config } from './config.js';
import { openDatabase } from './db.js';
import { hashPassword } from './security/passwords.js';

export async function ensureSeed(db) {
  const existing = db.prepare('SELECT COUNT(*) AS c FROM accounts').get().c;
  if (existing > 0) return;

  const passwordHash = await hashPassword('Nexo#2026');
  const now = Date.now();

  const insertAccount = db.prepare(
    `INSERT INTO accounts
     (id, owner, currency, balance_cents, status, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`,
  );
  const insertCredential = db.prepare(
    `INSERT INTO password_credentials (account_id, password_hash, updated_at)
     VALUES (?, ?, ?)`,
  );

  const transaction = db.transaction(() => {
    insertAccount.run('10020001', 'Ana Torres', 'PEN', 150000, 'ACTIVE', now);
    insertCredential.run('10020001', passwordHash, now);

    insertAccount.run('10020002', 'Luis Rojas', 'PEN', 80000, 'ACTIVE', now);
    insertCredential.run('10020002', passwordHash, now);
  });

  transaction();
  console.log('Seed listo: 10020001 (Ana) y 10020002 (Luis), clave Nexo#2026');
}

const isMain =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isMain) {
  const db = openDatabase(config.dbPath);
  await ensureSeed(db);
  db.close();
}
```

### 5.20. Arrancar y probar el backend

```powershell
Set-Location C:\NexoPayBackend
npm start
```

Debe mostrar algo como:

```text
Server listening at http://0.0.0.0:3000
NexoPay backend en http://localhost:3000
```

✅ Abre `http://localhost:3000/health` y debe responder `{"ok":true,...}`.

✅ Abre `http://localhost:3000/api/v1/auth/me` y debe dar **401**. Eso confirma que la seguridad está activa.

### 5.21. Prueba rápida con PowerShell

```powershell
$base = "http://localhost:3000"

$login = Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body '{"accountId":"10020001","password":"Nexo#2026"}'

$login.accessToken
$login.refreshToken
$login.account

$headers = @{
  Authorization = "Bearer $($login.accessToken)"
  "Idempotency-Key" = [guid]::NewGuid().ToString()
}

Invoke-RestMethod -Method Post -Uri "$base/api/v1/operations/deposit" `
  -Headers $headers -ContentType "application/json" `
  -Body '{"amountCents":10000}'
```

✅ El depósito debe devolver `balanceCents: 160000` (S/ 1,600.00).

### 5.22. Flujo Postman para la defensa

1. `POST http://localhost:3000/api/v1/auth/login` con `{"accountId":"10020001","password":"Nexo#2026"}`. Guarda `accessToken` y `refreshToken` como variables.
2. Repite el login con `10020002` y guarda `accessTokenLuis`.
3. `POST /api/v1/operations/deposit` con Bearer de Ana, header `Idempotency-Key` y body `{"amountCents":20000}`.
4. `POST /api/v1/operations/transfer` con Bearer de Luis, `Idempotency-Key` y body `{"toAccountId":"10020001","amountCents":5000}`.
5. `GET /api/v1/operations/balance` con Bearer de Ana: debe haber subido.
6. `POST /api/v1/realtime/ticket` con Bearer de Ana: copia `ticket`.
7. En Postman: **New → WebSocket** → `ws://localhost:3000/ws?ticket=TU_TICKET` → **Connect**. Debes recibir `connection.ready`.
8. Deja el WS abierto. Desde otra pestaña, haz la transferencia de Luis a Ana. Debes ver `transfer.received` en vivo. **Esa es la demo del 29-10.**

### 5.23. Pruebas automatizadas del backend

`test/helpers.js`

```javascript
import { openDatabase } from '../src/db.js';
import { ensureSeed } from '../src/seed.js';
import { buildApp } from '../src/app.js';

export async function createTestContext() {
  const db = openDatabase(':memory:');
  await ensureSeed(db);
  const app = await buildApp({ db, logger: false });
  await app.ready();

  return {
    db,
    app,
    async close() {
      await app.close();
      db.close();
    },
  };
}

export async function login(
  app,
  accountId = '10020001',
  password = 'Nexo#2026',
  remoteAddress = '10.0.0.1',
) {
  return app.inject({
    method: 'POST',
    url: '/api/v1/auth/login',
    payload: { accountId, password },
    remoteAddress,
  });
}

export async function loginJson(app, accountId = '10020001') {
  return (await login(app, accountId)).json();
}
```

`test/auth.test.js`

```javascript
import test from 'node:test';
import assert from 'node:assert/strict';
import { createTestContext, login } from './helpers.js';

test('login correcto devuelve tokens y cuenta', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  const response = await login(ctx.app);
  assert.equal(response.statusCode, 200);

  const body = response.json();
  assert.ok(body.accessToken);
  assert.ok(body.refreshToken);
  assert.equal(body.account.id, '10020001');
  assert.equal(body.account.balanceCents, 150000);
});

test('login con clave incorrecta devuelve 401 genérico', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  const response = await login(ctx.app, '10020001', 'ClaveMala#1');
  assert.equal(response.statusCode, 401);
  assert.equal(response.json().error.code, 'invalid_credentials');
});

test('bloqueo tras 5 intentos fallidos', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  for (let i = 0; i < 5; i += 1) {
    const response = await login(
      ctx.app,
      '10020001',
      'ClaveMala#1',
      `10.0.1.${i + 1}`,
    );
    assert.equal(response.statusCode, 401);
  }

  const locked = await login(ctx.app, '10020001', 'Nexo#2026', '10.0.1.99');
  assert.equal(locked.statusCode, 423);
  assert.equal(locked.json().error.code, 'account_locked');
});

test('refresh rota y detecta reuso', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  const first = (await login(ctx.app)).json();

  const refresh1 = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/auth/refresh',
    payload: { refreshToken: first.refreshToken },
  });
  assert.equal(refresh1.statusCode, 200);
  const second = refresh1.json();

  const reuse = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/auth/refresh',
    payload: { refreshToken: first.refreshToken },
  });
  assert.equal(reuse.statusCode, 401);

  const afterReuse = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/auth/refresh',
    payload: { refreshToken: second.refreshToken },
  });
  assert.equal(afterReuse.statusCode, 401);
});
```

`test/operations.test.js`

```javascript
import test from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import { createTestContext, loginJson } from './helpers.js';

function auth(token, key = crypto.randomUUID()) {
  return {
    authorization: `Bearer ${token}`,
    'idempotency-key': key,
  };
}

test('depósito suma saldo y la idempotencia evita duplicados', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  const session = await loginJson(ctx.app);
  const key = crypto.randomUUID();

  const first = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/operations/deposit',
    headers: auth(session.accessToken, key),
    payload: { amountCents: 10000 },
  });
  assert.equal(first.statusCode, 200);
  assert.equal(first.json().balanceCents, 160000);

  const second = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/operations/deposit',
    headers: auth(session.accessToken, key),
    payload: { amountCents: 10000 },
  });
  assert.equal(second.statusCode, 200);
  assert.equal(second.json().idempotentReplay, true);
  assert.equal(second.json().balanceCents, 160000);

  const count = ctx.db
    .prepare('SELECT COUNT(*) AS c FROM transactions WHERE account_id = ?')
    .get('10020001').c;
  assert.equal(count, 1);
});

test('retiro con saldo insuficiente devuelve 400 sin insertar', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  const session = await loginJson(ctx.app);
  const response = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/operations/withdraw',
    headers: auth(session.accessToken),
    payload: { amountCents: 200000 },
  });

  assert.equal(response.statusCode, 400);
  assert.equal(response.json().error.code, 'insufficient_funds');

  const count = ctx.db
    .prepare('SELECT COUNT(*) AS c FROM transactions WHERE account_id = ?')
    .get('10020001').c;
  assert.equal(count, 0);
});

test('transferencia mueve saldo e inserta dos filas', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  const ana = await loginJson(ctx.app, '10020001');
  const before = ctx.db
    .prepare('SELECT balance_cents FROM accounts WHERE id = ?')
    .get('10020002').balance_cents;

  const response = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/operations/transfer',
    headers: auth(ana.accessToken),
    payload: { toAccountId: '10020002', amountCents: 5000 },
  });

  assert.equal(response.statusCode, 200);
  assert.equal(response.json().balanceCents, 145000);

  const after = ctx.db
    .prepare('SELECT balance_cents FROM accounts WHERE id = ?')
    .get('10020002').balance_cents;
  assert.equal(after, before + 5000);

  const rows = ctx.db
    .prepare('SELECT COUNT(*) AS c FROM transactions WHERE reference_id = ?')
    .get(response.json().referenceId).c;
  assert.equal(rows, 2);
});

test('pago de servicio se categoriza como Luz', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  const session = await loginJson(ctx.app);
  const response = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/operations/pay-service',
    headers: auth(session.accessToken),
    payload: { contractCode: 'LUZ-001', amountCents: 3000 },
  });

  assert.equal(response.statusCode, 200);
  assert.equal(response.json().transaction.serviceName, 'Luz');
  assert.equal(response.json().transaction.contractCode, 'LUZ-001');
});
```

`test/realtime.test.js`

```javascript
import test from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import { once } from 'node:events';
import WebSocket from 'ws';
import { createTestContext, loginJson } from './helpers.js';

test('ticket WS es de un solo uso y llega transfer.received', async (t) => {
  const ctx = await createTestContext();
  t.after(() => ctx.close());

  await ctx.app.listen({ port: 0, host: '127.0.0.1' });
  const port = ctx.app.server.address().port;

  const ana = await loginJson(ctx.app, '10020001');
  const luis = await loginJson(ctx.app, '10020002');

  const ticketResponse = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/realtime/ticket',
    headers: { authorization: `Bearer ${ana.accessToken}` },
  });
  assert.equal(ticketResponse.statusCode, 200);
  const ticket = ticketResponse.json().ticket;

  const ws = new WebSocket(`ws://127.0.0.1:${port}/ws?ticket=${ticket}`);
  await once(ws, 'open');

  const [readyRaw] = await once(ws, 'message');
  const ready = JSON.parse(readyRaw.toString());
  assert.equal(ready.event, 'connection.ready');

  const reuse = new WebSocket(`ws://127.0.0.1:${port}/ws?ticket=${ticket}`);
  reuse.on('error', () => {});
  const [closeCode] = await once(reuse, 'close');
  assert.equal(closeCode, 4401);

  const receivedPromise = once(ws, 'message');

  const transfer = await ctx.app.inject({
    method: 'POST',
    url: '/api/v1/operations/transfer',
    headers: {
      authorization: `Bearer ${luis.accessToken}`,
      'idempotency-key': crypto.randomUUID(),
    },
    payload: { toAccountId: '10020001', amountCents: 2500 },
  });
  assert.equal(transfer.statusCode, 200);

  const [receivedRaw] = await receivedPromise;
  const envelope = JSON.parse(receivedRaw.toString());
  assert.equal(envelope.event, 'transfer.received');
  assert.equal(envelope.data.transaction.amountCents, 2500);
  assert.equal(envelope.data.fromAccountId, '10020002');

  ws.close();
});
```

Ejecuta:

```powershell
Set-Location C:\NexoPayBackend
npm test
```

✅ Deben pasar todas las pruebas. Si falla el test de WebSocket, revisa que `@fastify/websocket` esté registrado antes de las rutas y que el puerto `0` esté libre.

### 5.24. Desplegar en Render (HTTPS/WSS gratis)

Crea `render.yaml` en la raíz del backend:

```yaml
services:
  - type: web
    name: nexopay-backend
    env: node
    plan: free
    buildCommand: npm ci
    startCommand: npm start
    healthCheckPath: /health
    envVars:
      - key: NODE_ENV
        value: production
      - key: JWT_ACCESS_SECRET
        generateValue: true
      - key: ACCESS_TTL_SECONDS
        value: "900"
      - key: REFRESH_TTL_DAYS
        value: "7"
      - key: DB_PATH
        value: ./data/nexopay.db
      - key: CORS_ORIGINS
        value: https://nexopay.local
```

Pasos:

1. Sube el backend a GitHub.
2. En Render: **New → Web Service** → conecta el repo.
3. Build: `npm ci`; Start: `npm start`.
4. Configura las variables de entorno (si no usas `render.yaml`).
5. Copia la URL `https://nexopay-backend-xxxx.onrender.com`.
6. Prueba `https://.../health`.
7. El WebSocket será `wss://.../ws?ticket=...`.

⚠️ **Render free tiene disco efímero.** La base SQLite se reinicia si el servicio se redespliega o se reinicia. Para la demo es aceptable porque el seed vuelve a crear las cuentas. Si quieres persistencia real, agrega un disco persistente (plan pago) y configura `DB_PATH=/var/data/nexopay.db`.

⚠️ **Render free se duerme** tras un periodo sin tráfico. Antes de la defensa, abre `/health` y espera a que despierte. Ten un plan B local.

---

## 6. FASE B — PROYECTO ANDROID BASE, XML Y MATERIAL 3

En esta fase dejas el proyecto Android **compilable hasta el login visual**, con tema Material 3, recursos, seguridad local y sesión. Las pantallas se completan en la Fase G.

### 6.1. Crear el proyecto en Android Studio

1. Abre Android Studio → **New Project** → **Empty Views Activity**.
2. Configura:

| Campo | Valor |
|---|---|
| Name | `NexoPay` |
| Package name | `com.uc1.nexopay` |
| Save location | `C:\Users\Usuario-PC\AndroidStudioProjects\NexoPay` |
| Language | Kotlin |
| Minimum SDK | API 26 ("Android 8.0") |
| Build configuration language | Kotlin DSL |

3. Espera la sincronización inicial.
4. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, selecciona el JDK 17 embebido por Android Studio.
5. Cierra el proyecto y reemplaza los archivos Gradle por los de esta guía.

> **¿Por qué minSdk 26?** SQLCipher, Keystore y las APIs de red que usamos funcionan sin ramas de compatibilidad. Además, Android 8+ cubre prácticamente todos los celulares de la demo.

✅ **Verificación:** antes de continuar, el proyecto vacío debe compilar y abrir una pantalla en blanco.

---

### 6.2. Gradle completo

#### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NexoPay"
include(":app")
```

#### `build.gradle.kts` (raíz)

```kotlin
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

#### `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

#### `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.uc1.nexopay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uc1.nexopay"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += "environment"

    productFlavors {
        create("local") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/\"")
            buildConfigField("String", "WS_URL", "\"ws://10.0.2.2:3000/ws\"")
        }
        create("device") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:3000/\"")
            buildConfigField("String", "WS_URL", "\"ws://127.0.0.1:3000/ws\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://TU-BACKEND.onrender.com/\"")
            buildConfigField("String", "WS_URL", "\"wss://TU-BACKEND.onrender.com/ws\"")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES"
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    implementation("net.zetetic:sqlcipher-android:4.19.1@aar")
    implementation("androidx.sqlite:sqlite:2.7.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("androidx.room:room-testing:2.7.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
```

> **Importante:** `prod` contiene un marcador. Antes de generar el APK final, reemplaza `TU-BACKEND` por el subdominio real de Render. Si lo olvidas, el APK de producción no conecta.

#### `app/proguard-rules.pro`

```proguard
-keepattributes Signature, *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.uc1.nexopay.data.remote.dto.** { *; }

-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
```

> **Por qué:** R8 ofusca clases y puede romper `kotlinx.serialization` o los DTO. Estas reglas conservan serializadores y nombres de campos necesarios.

✅ **Verificación:** ejecuta **Sync Now**. No debe haber errores de resolución. Si aparece `Unsupported class file major version`, estás usando JDK equivocado.

---

### 6.3. Estructura de paquetes

Crea esta estructura bajo `app/src/main/java/com/uc1/nexopay/`:

```text
com.uc1.nexopay
├── NexoPayApp.kt
├── di
│   └── AppContainer.kt
├── data
│   ├── local
│   │   ├── AppDatabase.kt
│   │   ├── DatabaseProvider.kt
│   │   ├── TransactionDao.kt
│   │   └── TransactionEntity.kt
│   ├── remote
│   │   ├── ApiFactory.kt
│   │   ├── AuthInterceptor.kt
│   │   ├── NexoApi.kt
│   │   ├── TokenAuthenticator.kt
│   │   ├── WalletSocketManager.kt
│   │   └── dto
│   │       └── ApiDtos.kt
│   └── repository
│       └── WalletRepository.kt
├── security
│   ├── SecureStore.kt
│   └── SessionManager.kt
├── settings
│   └── SettingsStore.kt
└── ui
    ├── common
    │   ├── ErrorMapper.kt
    │   ├── NexoViewModelFactory.kt
    │   └── UiUtils.kt
    ├── dashboard
    │   ├── AmountSheet.kt
    │   ├── DashboardActivity.kt
    │   ├── ServiceSheet.kt
    │   ├── TransactionAdapter.kt
    │   └── TransferSheet.kt
    └── login
        └── LoginActivity.kt
```

Recursos:

```text
app/src/main/res/
├── drawable/
│   ├── bg_balance_card.xml
│   ├── bg_chip_offline.xml
│   ├── bg_chip_online.xml
│   ├── ic_deposit.xml
│   ├── ic_history.xml
│   ├── ic_logout.xml
│   ├── ic_service.xml
│   ├── ic_transfer.xml
│   ├── ic_wallet.xml
│   └── ic_withdraw.xml
├── layout/
│   ├── activity_dashboard.xml
│   ├── activity_login.xml
│   ├── item_transaction.xml
│   ├── sheet_amount.xml
│   ├── sheet_service.xml
│   └── sheet_transfer.xml
├── mipmap-anydpi-v26/          (se crea en la Fase J)
├── values/
│   ├── colors.xml
│   ├── strings.xml
│   ├── styles.xml
│   └── themes.xml
└── xml/
    ├── data_extraction_rules.xml
    └── network_security_config.xml
```

> **Nota:** los archivos se crean a medida que avanzas. No te preocupes si al terminar esta fase faltan layouts o clases: se completan en las fases C–G.

---

### 6.4. `AndroidManifest.xml`

Reemplaza `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".NexoPayApp"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="false"
        android:icon="@drawable/ic_wallet"
        android:label="@string/app_name"
        android:networkSecurityConfig="@xml/network_security_config"
        android:roundIcon="@drawable/ic_wallet"
        android:supportsRtl="true"
        android:theme="@style/Theme.NexoPay"
        android:usesCleartextTraffic="false">

        <activity
            android:name=".ui.dashboard.DashboardActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize" />

        <activity
            android:name=".ui.login.LoginActivity"
            android:exported="true"
            android:theme="@style/Theme.NexoPay.Splash"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

> **Seguridad:** `allowBackup=false` evita que adb/cloud copie la base cifrada y los tokens. `usesCleartextTraffic=false` bloquea HTTP en producción. El HTTP local se habilita solo en builds `debug` mediante el archivo de la sección 6.6.

#### `res/xml/data_extraction_rules.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
    </device-transfer>
</data-extraction-rules>
```

---

### 6.5. Recursos: colores, tema, strings y drawables

#### `res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="nexo_primary">#006B5B</color>
    <color name="nexo_on_primary">#FFFFFF</color>
    <color name="nexo_primary_container">#9CF2E2</color>
    <color name="nexo_on_primary_container">#00201A</color>
    <color name="nexo_primary_dark">#00453A</color>

    <color name="nexo_secondary">#4B635D</color>
    <color name="nexo_on_secondary">#FFFFFF</color>
    <color name="nexo_secondary_container">#CDE8E1</color>
    <color name="nexo_on_secondary_container">#08201B</color>

    <color name="nexo_background">#F7FAF8</color>
    <color name="nexo_surface">#F7FAF8</color>
    <color name="nexo_on_surface">#171D1B</color>
    <color name="nexo_surface_variant">#DBE5E1</color>
    <color name="nexo_on_surface_variant">#3F4946</color>
    <color name="nexo_outline">#6F7976</color>
    <color name="nexo_card">#FFFFFF</color>

    <color name="nexo_success">#0F7B4F</color>
    <color name="nexo_success_container">#D7F5E3</color>
    <color name="nexo_on_success_container">#00210F</color>
    <color name="nexo_danger">#B3261E</color>
    <color name="nexo_danger_container">#FFDAD6</color>
    <color name="nexo_on_danger_container">#410002</color>
    <color name="nexo_warning">#7A5900</color>
    <color name="nexo_warning_container">#FFDF9B</color>

    <color name="nexo_scrim">#66000000</color>
    <color name="nexo_chip_offline_bg">#FFE2DE</color>
    <color name="nexo_chip_offline_text">#8C1D18</color>
</resources>
```

#### `res/values/themes.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">

    <style name="Theme.NexoPay" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/nexo_primary</item>
        <item name="colorOnPrimary">@color/nexo_on_primary</item>
        <item name="colorPrimaryContainer">@color/nexo_primary_container</item>
        <item name="colorOnPrimaryContainer">@color/nexo_on_primary_container</item>
        <item name="colorSecondary">@color/nexo_secondary</item>
        <item name="colorOnSecondary">@color/nexo_on_secondary</item>
        <item name="colorSecondaryContainer">@color/nexo_secondary_container</item>
        <item name="colorOnSecondaryContainer">@color/nexo_on_secondary_container</item>
        <item name="colorSurface">@color/nexo_surface</item>
        <item name="colorOnSurface">@color/nexo_on_surface</item>
        <item name="colorSurfaceVariant">@color/nexo_surface_variant</item>
        <item name="colorOnSurfaceVariant">@color/nexo_on_surface_variant</item>
        <item name="colorOutline">@color/nexo_outline</item>
        <item name="colorError">@color/nexo_danger</item>
        <item name="colorOnError">@color/nexo_on_primary</item>
        <item name="android:colorBackground">@color/nexo_background</item>
        <item name="android:windowBackground">@color/nexo_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@color/nexo_background</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:forceDarkAllowed" tools:targetApi="q">false</item>
    </style>

    <style name="Theme.NexoPay.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/nexo_primary</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_wallet</item>
        <item name="windowSplashScreenAnimationDuration">300</item>
        <item name="postSplashScreenTheme">@style/Theme.NexoPay</item>
    </style>

</resources>
```

#### `res/values/styles.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="Widget.NexoPay.PrimaryButton" parent="Widget.Material3.Button">
        <item name="android:minHeight">56dp</item>
        <item name="cornerRadius">16dp</item>
        <item name="android:textAllCaps">false</item>
        <item name="android:textSize">16sp</item>
        <item name="android:textStyle">bold</item>
    </style>

    <style name="Widget.NexoPay.ActionButton" parent="Widget.Material3.Button.TonalButton">
        <item name="android:minHeight">88dp</item>
        <item name="cornerRadius">24dp</item>
        <item name="android:textAllCaps">false</item>
        <item name="android:textSize">13sp</item>
        <item name="android:textStyle">bold</item>
        <item name="iconGravity">top</item>
        <item name="iconPadding">10dp</item>
        <item name="iconSize">28dp</item>
    </style>

    <style name="TextAppearance.NexoPay.Balance" parent="TextAppearance.Material3.DisplaySmall">
        <item name="android:textSize">40sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:letterSpacing">-0.02</item>
    </style>

</resources>
```

#### `res/values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">NexoPay</string>

    <string name="login_title">Bienvenido a NexoPay</string>
    <string name="login_subtitle">Tu billetera digital, rápida y segura.</string>
    <string name="login_account">Número de cuenta</string>
    <string name="login_password">Contraseña</string>
    <string name="login_button">Ingresar</string>
    <string name="login_loading">Validando credenciales…</string>
    <string name="login_demo_title">Cuentas de demostración</string>
    <string name="login_demo_body">Ana Torres: 10020001\nLuis Rojas: 10020002\nContraseña: Nexo#2026</string>
    <string name="login_show_password">Mostrar contraseña</string>
    <string name="login_hide_password">Ocultar contraseña</string>
    <string name="login_account_required">Ingresa tu número de cuenta.</string>
    <string name="login_password_required">Ingresa tu contraseña.</string>
    <string name="login_generic_error">No se pudo iniciar sesión. Inténtalo de nuevo.</string>

    <string name="dashboard_greeting">Hola, %1$s</string>
    <string name="dashboard_subtitle">Esto es lo que tienes disponible hoy.</string>
    <string name="dashboard_balance_label">Saldo disponible</string>
    <string name="dashboard_balance_loading">S/ —</string>
    <string name="dashboard_account">Cuenta %1$s</string>
    <string name="dashboard_status_online">En línea</string>
    <string name="dashboard_status_offline">Desconectado</string>
    <string name="dashboard_status_connecting">Conectando…</string>
    <string name="dashboard_action_deposit">Depositar</string>
    <string name="dashboard_action_withdraw">Retirar</string>
    <string name="dashboard_action_transfer">Transferir</string>
    <string name="dashboard_action_service">Servicios</string>
    <string name="dashboard_history">Movimientos</string>
    <string name="dashboard_history_empty">Aún no tienes movimientos.\nHaz tu primera operación.</string>
    <string name="dashboard_refresh">Actualizar</string>
    <string name="dashboard_logout">Cerrar sesión</string>
    <string name="dashboard_logout_confirm">¿Cerrar sesión en NexoPay?</string>
    <string name="dashboard_logout_body">Se borrarán los tokens y el historial local de este dispositivo. La cuenta y el saldo permanecen en el servidor.</string>
    <string name="dashboard_balance_error">No se pudo actualizar el saldo.</string>

    <string name="amount_sheet_deposit_title">Depositar dinero</string>
    <string name="amount_sheet_withdraw_title">Retirar dinero</string>
    <string name="amount_sheet_amount">Monto</string>
    <string name="amount_sheet_currency">S/</string>
    <string name="amount_sheet_available">Disponible: %1$s</string>
    <string name="amount_sheet_confirm_deposit">Depositar</string>
    <string name="amount_sheet_confirm_withdraw">Retirar</string>
    <string name="amount_sheet_invalid">Ingresa un monto mayor a cero.</string>
    <string name="amount_sheet_insufficient">Saldo insuficiente.</string>
    <string name="amount_sheet_processing">Procesando…</string>

    <string name="transfer_sheet_title">Transferir a otra cuenta</string>
    <string name="transfer_sheet_destination">Cuenta destino</string>
    <string name="transfer_sheet_amount">Monto</string>
    <string name="transfer_sheet_confirm">Transferir</string>
    <string name="transfer_sheet_same_account">No puedes transferirte a la misma cuenta.</string>
    <string name="transfer_sheet_success">Transferencia enviada a %1$s.</string>

    <string name="service_sheet_title">Pagar servicio</string>
    <string name="service_sheet_code">Código de contrato</string>
    <string name="service_sheet_amount">Monto a pagar</string>
    <string name="service_sheet_confirm">Pagar servicio</string>
    <string name="service_sheet_hint">Ejemplo: LUZ-001, AGUA-002, INTERNET-003</string>
    <string name="service_sheet_success">Pago de servicio registrado.</string>

    <string name="common_cancel">Cancelar</string>
    <string name="common_accept">Aceptar</string>
    <string name="common_retry">Reintentar</string>
    <string name="common_loading">Cargando…</string>
    <string name="common_offline">Sin conexión. Revisa tu red.</string>
    <string name="common_unexpected_error">Ocurrió un error inesperado.</string>
    <string name="common_session_expired">Tu sesión expiró. Inicia sesión otra vez.</string>

    <string name="tx_deposit">Depósito</string>
    <string name="tx_withdraw">Retiro</string>
    <string name="tx_transfer_in">Transferencia recibida</string>
    <string name="tx_transfer_out">Transferencia enviada</string>
    <string name="tx_service">Servicio básico</string>
    <string name="tx_unknown">Movimiento</string>
    <string name="tx_income">+ %1$s</string>
    <string name="tx_expense">− %1$s</string>

    <string name="cd_wallet">Icono de billetera</string>
    <string name="cd_status">Estado de conexión</string>
    <string name="cd_history">Historial de movimientos</string>
    <string name="cd_logout">Cerrar sesión</string>
</resources>
```

#### Drawables

`res/drawable/ic_wallet.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M21,7H3V5h18V7zM21,9H3v10h18V9zM16,12.5a1.5,1.5 0,1 0,0 3a1.5,1.5 0,1 0,0 -3z" />
</vector>
```

`res/drawable/ic_deposit.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z" />
</vector>
```

`res/drawable/ic_withdraw.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M5,15h4v6h6v-6h4l-7,-7 -7,7zM19,3H5v2h14V3z" />
</vector>
```

`res/drawable/ic_transfer.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z" />
</vector>
```

`res/drawable/ic_service.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM19,19H5V5h14v14zM7,7h10v2H7V7zM7,11h10v2H7v-2zM7,15h7v2H7v-2z" />
</vector>
```

`res/drawable/ic_history.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M13,3a9,9 0,0 0,-9 9H1l4,4 4,-4H6a7,7 0,1 1,7 7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42A8.95,8.95 0,0 0,13 21a9,9 0,0 0,0 -18zM12,8v5l4.25,2.52 0.77,-1.28 -3.52,-2.09V8H12z" />
</vector>
```

`res/drawable/ic_logout.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M10,17l5,-5 -5,-5v3H3v4h7v3zM21,3h-8v2h8v14h-8v2h8c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2z" />
</vector>
```

`res/drawable/bg_balance_card.xml`:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="315"
        android:startColor="@color/nexo_primary_dark"
        android:endColor="@color/nexo_primary" />
    <corners android:radius="28dp" />
</shape>
```

`res/drawable/bg_chip_online.xml`:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/nexo_success_container" />
    <stroke
        android:width="1dp"
        android:color="@color/nexo_success" />
    <corners android:radius="999dp" />
</shape>
```

`res/drawable/bg_chip_offline.xml`:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/nexo_chip_offline_bg" />
    <stroke
        android:width="1dp"
        android:color="@color/nexo_danger" />
    <corners android:radius="999dp" />
</shape>
```

> **Detalle:** `ic_wallet` usa relleno blanco porque se muestra sobre el fondo verde del splash y del encabezado. Los iconos de acciones usan negro y Material 3 los tiñe con `app:iconTint`.

✅ **Verificación:** `Build → Clean Project` y `Build → Rebuild Project`. No debe haber errores de recursos.

---

### 6.6. Network Security Config: HTTP solo en debug

#### `app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

#### `app/src/debug/res/xml/network_security_config.xml`

Crea el archivo con la misma ruta dentro de `src/debug`. Android Studio lo superpone al de `main` solo en debug.

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
</network-security-config>
```

> **Por qué no pones `usesCleartextTraffic=true`:** eso abriría HTTP a cualquier dominio. Aquí solo el emulador (`10.0.2.2`) y el loopback del celular (`127.0.0.1` con `adb reverse`) pueden usar HTTP, y solo en debug. La build `prod` release usa HTTPS/WSS obligatoriamente.

⚠️ **Error típico:** probar el APK `prod` contra Render con `http://`. La Network Security Config lo bloquea con `CLEARTEXT communication not permitted`. Usa `https://` y `wss://`.

---

### 6.7. `security/SecureStore.kt` — Keystore + AES-GCM

No usamos `EncryptedSharedPreferences` porque AndroidX lo marcó como obsoleto. Implementamos el patrón recomendado: una clave AES de 256 bits en **Android Keystore** que nunca sale del hardware/TEE, y cifrado **AES/GCM** de cada valor.

```kotlin
package com.uc1.nexopay.security

import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStore(context: Context) {

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    fun put(key: String, value: String) = synchronized(this) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = "${b64(cipher.iv)}:${b64(ciphertext)}"
        prefs.edit().putString(key, payload).apply()
    }

    fun get(key: String): String? = synchronized(this) {
        val payload = prefs.getString(key, null) ?: return null
        val parts = payload.split(":")
        if (parts.size != 2) return null

        runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrElse {
            prefs.edit().remove(key).apply()
            null
        }
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun getOrCreateBytes(key: String, size: Int): ByteArray {
        get(key)?.let { return Base64.decode(it, Base64.NO_WRAP) }

        val bytes = ByteArray(size).also { SecureRandom().nextBytes(it) }
        put(key, Base64.encodeToString(bytes, Base64.NO_WRAP))
        return bytes
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)
            ?.secretKey
            ?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private companion object {
        const val PREFS_NAME = "nexo_secure"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "nexopay_master_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
```

> **Qué defender:** la clave se genera dentro de Android Keystore y no se puede exportar. El archivo de preferencias solo contiene IV + ciphertext en Base64. Si un atacante copia el archivo, no puede descifrarlo sin la clave del dispositivo. GCM además detecta manipulación.

⚠️ **Error típico:** usar `AES/ECB`. GCM es obligatorio aquí porque aporta confidencialidad e integridad. Tampoco reutilices el IV: `Cipher` genera uno nuevo en cada `init(ENCRYPT_MODE, ...)`.

---

### 6.8. `settings/SettingsStore.kt` — DataStore para datos no sensibles

```kotlin
package com.uc1.nexopay.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nexo_settings"
)

class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val lastAccount: Flow<String?> = store.data.map { it[LAST_ACCOUNT] }
    val notificationsEnabled: Flow<Boolean> = store.data.map {
        it[NOTIFICATIONS] ?: true
    }

    suspend fun setLastAccount(accountId: String) {
        store.edit { it[LAST_ACCOUNT] = accountId }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.edit { it[NOTIFICATIONS] = enabled }
    }

    suspend fun clearLastAccount() {
        store.edit { it.remove(LAST_ACCOUNT) }
    }

    private companion object {
        val LAST_ACCOUNT = stringPreferencesKey("last_account")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }
}
```

> **Por qué DataStore y no SharedPreferences:** DataStore es asíncrono, transaccional y expone `Flow`, así que no bloquea el hilo principal. Aquí solo guardamos el último número de cuenta (para comodidad) y preferencias; **nunca** tokens ni contraseñas.

---

### 6.9. `security/SessionManager.kt` — tokens y sesión en memoria

```kotlin
package com.uc1.nexopay.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Session(
    val accountId: String,
    val name: String,
    val accessToken: String,
    val refreshToken: String
)

class SessionManager(private val secureStore: SecureStore) {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    val accessToken: String?
        get() = _session.value?.accessToken

    val refreshToken: String?
        get() = secureStore.get(KEY_REFRESH)

    val accountId: String?
        get() = _session.value?.accountId

    fun restore(): Boolean {
        val refresh = secureStore.get(KEY_REFRESH) ?: return false
        val accountId = secureStore.get(KEY_ACCOUNT_ID) ?: return false
        val name = secureStore.get(KEY_NAME) ?: "Usuario"

        _session.value = Session(
            accountId = accountId,
            name = name,
            accessToken = "",
            refreshToken = refresh
        )
        return true
    }

    fun save(
        accountId: String,
        name: String,
        accessToken: String,
        refreshToken: String
    ) {
        secureStore.put(KEY_REFRESH, refreshToken)
        secureStore.put(KEY_ACCOUNT_ID, accountId)
        secureStore.put(KEY_NAME, name)
        _session.value = Session(accountId, name, accessToken, refreshToken)
    }

    fun updateAccessToken(accessToken: String) {
        _session.update { current ->
            current?.copy(accessToken = accessToken)
        }
    }

    fun updateRefreshToken(refreshToken: String) {
        secureStore.put(KEY_REFRESH, refreshToken)
        _session.update { current ->
            current?.copy(refreshToken = refreshToken)
        }
    }

    fun clear() {
        _session.value = null
        secureStore.remove(KEY_REFRESH)
        secureStore.remove(KEY_ACCOUNT_ID)
        secureStore.remove(KEY_NAME)
    }

    private companion object {
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_NAME = "account_name"
    }
}
```

> **Decisión de seguridad:** el **access token vive solo en memoria**. Si alguien extrae el archivo de preferencias, no obtiene un JWT utilizable. El refresh token sí se persiste cifrado para permitir reabrir la app sin volver a escribir la contraseña. Al cerrar sesión se borra.

⚠️ **Error típico:** guardar el JWT en `SharedPreferences` plano o dentro de la URL del WebSocket. Aquí el access token se envía en el header `Authorization` y el WebSocket usa un ticket efímero.

---

### 6.10. `NexoPayApp.kt` y `di/AppContainer.kt`

#### `di/AppContainer.kt` (versión base)

```kotlin
package com.uc1.nexopay.di

import android.content.Context
import com.uc1.nexopay.security.SecureStore
import com.uc1.nexopay.security.SessionManager
import com.uc1.nexopay.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val secureStore: SecureStore = SecureStore(appContext)
    val sessionManager: SessionManager = SessionManager(secureStore)
    val settingsStore: SettingsStore = SettingsStore(appContext)

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
```

> En la Fase E ampliamos este contenedor con `AppDatabase`, `NexoApi`, `WalletRepository` y `WalletSocketManager`. Mantener un solo contenedor evita singletons globales y facilita las pruebas.

#### `NexoPayApp.kt`

```kotlin
package com.uc1.nexopay

import android.app.Application
import android.os.StrictMode
import com.uc1.nexopay.di.AppContainer

class NexoPayApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        container = AppContainer(this)
    }
}
```

> **StrictMode en debug** te avisa en Logcat si haces red o disco en el hilo principal. Si ves `StrictMode policy violation` durante una operación, estás violando el requisito de corrutinas. No lo desactives: corrígelo.

✅ **Verificación de la Fase B:**

1. `Build → Rebuild Project` sin errores.
2. La app abre el splash y luego `LoginActivity` (aún vacía o con el layout de la Fase G).
3. En Logcat no aparecen crashes por Keystore.
4. El manifest fusionado muestra `usesCleartextTraffic="false"` en release y la NSC de debug en debug.

⚠️ **Errores típicos de esta fase**

| Síntoma | Causa | Solución |
|---|---|---|
| `Keystore operation failed` | Clave invalidada por cambio de bloqueo | `SessionManager.clear()` y volver a iniciar sesión |
| `CLEARTEXT communication not permitted` | Build release/local contra HTTP | Usa `local` debug en emulador o HTTPS en prod |
| `Unresolved reference: BuildConfig` | `buildConfig` desactivado | Activa `buildFeatures.buildConfig = true` |
| `Room cannot verify data integrity` | Passphrase distinta a la que creó la BD | Borra datos de la app o usa la misma clave de Keystore |
| `Duplicate class` | Dependencias mezcladas | `./gradlew clean` y revisa el árbol de dependencias |

---

## 7. FASE C — ROOM + SQLCIPHER

**Requisito del Word:** persistencia local con Room y demostrar que el historial **sobrevive al reinicio**. Aquí Room guarda el historial; SQLCipher cifra el archivo completo. El backend sigue siendo la fuente de verdad del saldo y de las operaciones.

### 7.1. `data/local/TransactionEntity.kt`

```kotlin
package com.uc1.nexopay.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["createdAt"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val balanceAfterCents: Long,
    val counterpartyAccountId: String?,
    val contractCode: String?,
    val serviceName: String?,
    val description: String?,
    val referenceId: String?,
    val createdAt: Long
)

object TransactionType {
    const val DEPOSIT = "DEPOSIT"
    const val WITHDRAWAL = "WITHDRAWAL"
    const val TRANSFER_SENT = "TRANSFER_SENT"
    const val TRANSFER_RECEIVED = "TRANSFER_RECEIVED"
    const val SERVICE_PAYMENT = "SERVICE_PAYMENT"

    fun isIncome(type: String): Boolean =
        type == DEPOSIT || type == TRANSFER_RECEIVED
}
```

> **Por qué `createdAt` es `Long`:** guardamos milisegundos UTC. Evita ambigüedades de zona horaria y permite ordenar con SQL. La conversión desde el ISO-8601 del backend se hace una sola vez, en el mapeo.

> **Por qué el `id` es la PK:** el backend genera un UUID por movimiento. Si el mismo movimiento llega por REST y por WebSocket, `REPLACE` evita duplicados. Es idempotencia también en el cliente.

### 7.2. `data/local/TransactionDao.kt`

```kotlin
package com.uc1.nexopay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT * FROM transactions
        ORDER BY createdAt DESC, id DESC
        LIMIT 200
        """
    )
    fun observeRecent(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        ORDER BY createdAt DESC, id DESC
        LIMIT 200
        """
    )
    suspend fun getRecent(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}
```

> `observeRecent()` devuelve un `Flow`: cualquier inserción o reemplazo hace que el `RecyclerView` se actualice solo. Es la base del requisito “historial reactivo”.

### 7.3. `data/local/AppDatabase.kt`

```kotlin
package com.uc1.nexopay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
```

> `exportSchema = true` genera `app/schemas/.../1.json`. Inclúyelo en GitHub: demuestra que trabajaste con migraciones y versionado. Para este proyecto la versión 1 no necesita migración.

### 7.4. `data/local/DatabaseProvider.kt` — SQLCipher

```kotlin
package com.uc1.nexopay.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.uc1.nexopay.security.SecureStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseProvider {

    private const val DB_NAME = "nexopay.db"
    private const val PASSPHRASE_KEY = "db_passphrase_v1"
    private const val PASSPHRASE_BYTES = 32

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context, secureStore: SecureStore): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: build(context, secureStore).also { instance = it }
        }
    }

    private fun build(context: Context, secureStore: SecureStore): AppDatabase {
        System.loadLibrary("sqlcipher")

        val passphrase = secureStore.getOrCreateBytes(
            key = PASSPHRASE_KEY,
            size = PASSPHRASE_BYTES
        )
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration()
            .build()
    }
}
```

> **Qué ocurre la primera vez:** `SecureStore` genera 32 bytes aleatorios con `SecureRandom`, los cifra con AES-GCM y los guarda. Esos bytes son la passphrase de SQLCipher. En arranques posteriores se descifran y se reutilizan. Si se pierde la clave de Keystore, la BD no se puede abrir: la app la recrea vacía y vuelve a sincronizar desde el backend.

> **Por qué no una passphrase hardcodeada:** cualquiera que decompile el APK la vería y podría abrir la base extraída. Aquí la clave está protegida por el Keystore del dispositivo.

⚠️ **Error típico:** llamar `System.loadLibrary("sqlcipher")` después de construir Room. Debe ser **antes** de crear la factory. Si ves `UnsatisfiedLinkError`, revisa que el AAR de SQLCipher esté en `dependencies` y que el APK incluya las ABI de tu dispositivo.

### 7.5. Verificar que la base está realmente cifrada

1. Ejecuta la app, inicia sesión y provoca al menos una operación para que Room inserte filas.
2. Conecta el dispositivo/emulador y ejecuta:

```powershell
adb shell run-as com.uc1.nexopay.debug ls -l databases
adb shell run-as com.uc1.nexopay.debug od -An -tx1 -N16 databases/nexopay.db
```

3. La primera fila debe mostrar un tamaño y la segunda **no** debe empezar con:

```text
53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33
```

Eso es `SQLite format 3` en hexadecimal. Si aparece, la base **no** está cifrada. Si aparece ruido hexadecimal, SQLCipher está activo.

4. Inspecciona también `nexopay.db-wal` y `nexopay.db-shm`: SQLCipher cifra el contenido de la base y el WAL. No publiques capturas con rutas ni datos reales.

### 7.6. Prueba de persistencia (evidencia para el informe)

1. Inicia sesión como Ana.
2. Haz un depósito de `S/ 10.00`.
3. Verifica que aparece en “Movimientos”.
4. Cierra la app por completo:

```powershell
adb shell am force-stop com.uc1.nexopay.debug
```

5. Vuelve a abrirla. El historial debe seguir ahí **antes** de que termine la sincronización de red.
6. Activa modo avión y reabre: el historial local sigue visible y la app no cierra la sesión. El saldo se sincroniza cuando vuelve la red; mientras tanto, las operaciones nuevas fallan con un mensaje controlado.

> **Para la defensa:** explica que Room es la caché persistente y el backend la autoridad. Si el profesor pregunta “¿qué pasa si borro la app?”, responde: se borran BD, clave de Keystore y tokens; al reinstalar, el historial se vuelve a descargar del backend porque la cuenta vive en el servidor.

✅ **Verificación:** `TransactionDao` aparece en el código generado por KSP (`build/generated/ksp/.../TransactionDao_Impl.kt`) y el archivo `schemas/com.uc1.nexopay.data.local.AppDatabase/1.json` existe.

---

## 8. FASE D — RED: RETROFIT, OKHTTP Y REFRESH AUTOMÁTICO

**Requisito del Word:** consumir REST para depósito, retiro, transferencia y pago de servicios, sin crashes y sin bloquear la UI. Aquí añadimos algo que la guía original no tenía: **refresh token rotativo automático** y **un solo reintento** ante `401`.

### 8.1. `data/remote/dto/ApiDtos.kt`

```kotlin
package com.uc1.nexopay.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val accountId: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Int = 0,
    val account: AccountDto
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Int = 0
)

@Serializable
data class AccountDto(
    val id: String,
    val owner: String,
    val currency: String = "PEN",
    val balanceCents: Long,
    val balance: String
)

@Serializable
data class BalanceResponse(
    val balanceCents: Long,
    val balance: String
)

@Serializable
data class TransactionDto(
    val id: String,
    val type: String,
    val amountCents: Long,
    val amount: String,
    val balanceCents: Long,
    val balance: String,
    val counterpartyAccountId: String? = null,
    val contractCode: String? = null,
    val serviceName: String? = null,
    val description: String? = null,
    val referenceId: String? = null,
    val createdAt: String
)

@Serializable
data class TransactionsResponse(
    val transactions: List<TransactionDto>
)

@Serializable
data class DepositRequest(val amountCents: Long)

@Serializable
data class WithdrawRequest(val amountCents: Long)

@Serializable
data class TransferRequest(
    val toAccountId: String,
    val amountCents: Long
)

@Serializable
data class PayServiceRequest(
    val contractCode: String,
    val amountCents: Long
)

@Serializable
data class OperationResponse(
    val transaction: TransactionDto,
    val balanceCents: Long,
    val balance: String,
    val toAccountId: String? = null,
    val referenceId: String? = null,
    val idempotentReplay: Boolean = false
)

@Serializable
data class TicketResponse(
    val ticket: String,
    val expiresInSeconds: Int = 30
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: ApiErrorDetails? = null,
    val requestId: String? = null
)

@Serializable
data class ApiErrorDetails(
    val balanceCents: Long? = null,
    val field: String? = null,
    val retryAfterSeconds: Int? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String? = null
)
```

> **`@SerialName`:** no lo necesitas si los nombres del backend son idénticos a los de Kotlin. Si algún día usas `snake_case`, agrega `@SerialName("balance_cents")`.

### 8.2. `data/remote/NexoApi.kt`

```kotlin
package com.uc1.nexopay.data.remote

import com.uc1.nexopay.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NexoApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<RefreshResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<Unit>

    @GET("api/v1/auth/me")
    suspend fun me(): Response<AccountDto>

    @GET("api/v1/operations/balance")
    suspend fun balance(): Response<BalanceResponse>

    @GET("api/v1/operations/transactions")
    suspend fun transactions(
        @Query("limit") limit: Int = 100
    ): Response<TransactionsResponse>

    @POST("api/v1/operations/deposit")
    suspend fun deposit(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DepositRequest
    ): Response<OperationResponse>

    @POST("api/v1/operations/withdraw")
    suspend fun withdraw(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: WithdrawRequest
    ): Response<OperationResponse>

    @POST("api/v1/operations/transfer")
    suspend fun transfer(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: TransferRequest
    ): Response<OperationResponse>

    @POST("api/v1/operations/pay-service")
    suspend fun payService(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PayServiceRequest
    ): Response<OperationResponse>

    @POST("api/v1/realtime/ticket")
    suspend fun realtimeTicket(): Response<TicketResponse>
}
```

> **Sin `/` inicial:** con `baseUrl = "http://10.0.2.2:3000/"` y `@POST("api/v1/...")`, Retrofit construye correctamente la URL. Si pones `/api/...`, elimina el path del `baseUrl`; es una fuente clásica de `404`.

### 8.3. `data/remote/AuthInterceptor.kt`

```kotlin
package com.uc1.nexopay.data.remote

import com.uc1.nexopay.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        val isPublic = path.endsWith("/auth/login") ||
            path.endsWith("/auth/refresh")

        if (isPublic) return chain.proceed(original)

        val token = sessionManager.accessToken
        if (token.isNullOrBlank()) return chain.proceed(original)

        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(request)
    }
}
```

> **Por qué excluir login y refresh:** el login no llevará un token viejo y el refresh usa el refresh token en el body. Si enviaras un access token expirado al refresh, algunos backends lo rechazan antes de leer el body.

### 8.4. `data/remote/TokenAuthenticator.kt` — refresh con un solo reintento

```kotlin
package com.uc1.nexopay.data.remote

import com.uc1.nexopay.data.remote.dto.RefreshRequest
import com.uc1.nexopay.security.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val refreshApi: NexoApi,
    private val onSessionExpired: () -> Unit
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("X-Refresh-Retry") != null) return null

        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) return null

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(refreshLock) {
            val current = sessionManager.accessToken
            if (!current.isNullOrBlank() && current != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .header("X-Refresh-Retry", "1")
                    .build()
            }

            val refreshToken = sessionManager.refreshToken ?: run {
                onSessionExpired()
                return null
            }

            val result = runBlocking {
                runCatching {
                    refreshApi.refresh(RefreshRequest(refreshToken))
                }.getOrNull()
            }

            val body = result?.body()
            if (result?.isSuccessful != true || body == null) {
                if (result?.code() == 401 || result?.code() == 403) {
                    sessionManager.clear()
                    onSessionExpired()
                }
                return null
            }

            sessionManager.updateAccessToken(body.accessToken)
            sessionManager.updateRefreshToken(body.refreshToken)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${body.accessToken}")
                .header("X-Refresh-Retry", "1")
                .build()
        }
    }
}
```

> **Por qué `runBlocking`:** `Authenticator.authenticate` es una API síncrona de OkHttp y se ejecuta en un hilo de red, no en el main. Es el lugar correcto para bloquear. La llamada de refresh usa la misma instancia de Retrofit **sin** este autenticador (ver 8.5), evitando bucles.

> **Rotación:** cada refresh devuelve un refresh token nuevo. Se guarda cifrado y se invalida el anterior en el servidor. Si alguien reutiliza el viejo, el backend revoca la familia completa.

> **Sin conexión:** solo un `401/403` del refresh invalida la sesión. Un `IOException`, timeout o `5xx` no borra los tokens: la app conserva la sesión y muestra el historial local en Room mientras no haya red.

⚠️ **Error típico:** crear un `OkHttpClient` nuevo dentro de `authenticate` y volver a instalar el autenticador. Eso produce bucles de refresh y `429`. Usa el cliente “limpio” de 8.5.

### 8.5. `data/remote/ApiFactory.kt`

```kotlin
package com.uc1.nexopay.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.uc1.nexopay.BuildConfig
import com.uc1.nexopay.security.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val contentType = "application/json".toMediaType()

    fun create(
        sessionManager: SessionManager,
        onSessionExpired: () -> Unit
    ): NexoApi {
        val baseClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()

        val refreshRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(baseClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        val refreshApi = refreshRetrofit.create(NexoApi::class.java)

        val authenticatedClient = baseClient.newBuilder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(
                TokenAuthenticator(
                    sessionManager = sessionManager,
                    refreshApi = refreshApi,
                    onSessionExpired = onSessionExpired
                )
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(NexoApi::class.java)
    }
}
```

> **Logging solo `BASIC`:** imprime método, URL, código y tiempo, pero **no** headers ni bodies. `BODY` filtraría contraseñas, tokens y montos en Logcat. Nunca lo actives en release.

> **`ignoreUnknownKeys = true`:** el backend puede añadir campos sin romper la app. `coerceInputValues = true` convierte `null` en valores por defecto cuando el campo no es nullable.

### 8.6. `data/repository/WalletRepository.kt`

```kotlin
package com.uc1.nexopay.data.repository

import com.uc1.nexopay.data.local.AppDatabase
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.local.TransactionType
import com.uc1.nexopay.data.remote.NexoApi
import com.uc1.nexopay.data.remote.dto.*
import com.uc1.nexopay.security.SessionManager
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

class WalletRepository(
    private val api: NexoApi,
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {

    private val dao = database.transactionDao()

    val transactions: Flow<List<TransactionEntity>> = dao.observeRecent()

    suspend fun login(accountId: String, password: String): Result<LoginResponse> =
        call { api.login(LoginRequest(accountId.trim(), password)) }
            .onSuccess { response ->
                database.transactionDao().deleteAll()
                sessionManager.save(
                    accountId = response.account.id,
                    name = response.account.owner,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken
                )
            }

    suspend fun restoreSession(): Result<Unit> = runCatching {
        if (!sessionManager.restore()) error("No hay sesión guardada")
    }

    suspend fun refreshAccessToken(): Result<Unit> = runCatching {
        val refreshToken = sessionManager.refreshToken
            ?: error("No hay refresh token")
        val response = api.refresh(RefreshRequest(refreshToken))

        if (!response.isSuccessful) {
            if (response.code() == 401 || response.code() == 403) {
                sessionManager.clear()
            }
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        val body = response.body() ?: error("Respuesta de refresh vacía")
        sessionManager.updateAccessToken(body.accessToken)
        sessionManager.updateRefreshToken(body.refreshToken)
    }

    suspend fun logout() {
        val refreshToken = sessionManager.refreshToken
        if (refreshToken != null) {
            runCatching { api.logout(RefreshRequest(refreshToken)) }
        }
        sessionManager.clear()
        database.transactionDao().deleteAll()
    }

    suspend fun balance(): Result<BalanceResponse> =
        call { api.balance() }

    suspend fun syncTransactions(): Result<List<TransactionEntity>> =
        call { api.transactions(limit = 100) }
            .map { response ->
                val entities = response.transactions.map { it.toEntity() }
                database.withTransaction {
                    if (entities.isNotEmpty()) dao.upsertAll(entities)
                }
                entities
            }

    suspend fun deposit(amountCents: Long): Result<OperationResponse> =
        call { api.deposit(uuid(), DepositRequest(amountCents)) }
            .onSuccess { persist(it) }

    suspend fun withdraw(amountCents: Long): Result<OperationResponse> =
        call { api.withdraw(uuid(), WithdrawRequest(amountCents)) }
            .onSuccess { persist(it) }

    suspend fun transfer(
        toAccountId: String,
        amountCents: Long
    ): Result<OperationResponse> =
        call {
            api.transfer(
                uuid(),
                TransferRequest(toAccountId.trim(), amountCents)
            )
        }.onSuccess { persist(it) }

    suspend fun payService(
        contractCode: String,
        amountCents: Long
    ): Result<OperationResponse> =
        call {
            api.payService(
                uuid(),
                PayServiceRequest(contractCode.trim().uppercase(), amountCents)
            )
        }.onSuccess { persist(it) }

    suspend fun realtimeTicket(): Result<TicketResponse> =
        call { api.realtimeTicket() }

    suspend fun saveIncoming(transaction: TransactionDto): TransactionEntity {
        val entity = transaction.toEntity()
        dao.upsert(entity)
        return entity
    }

    private suspend fun persist(response: OperationResponse) {
        dao.upsert(response.transaction.toEntity())
    }

    private suspend fun <T> call(block: suspend () -> Response<T>): Result<T> =
        runCatching {
            val response = block()
            if (!response.isSuccessful) {
                throw ApiException(response.code(), response.errorBody()?.string())
            }
            response.body() ?: error("Respuesta vacía del servidor")
        }

    private fun uuid(): String = UUID.randomUUID().toString()

    private fun TransactionDto.toEntity(): TransactionEntity =
        TransactionEntity(
            id = id,
            type = type,
            amountCents = amountCents,
            balanceAfterCents = balanceCents,
            counterpartyAccountId = counterpartyAccountId,
            contractCode = contractCode,
            serviceName = serviceName,
            description = description,
            referenceId = referenceId,
            createdAt = parseInstant(createdAt)
        )

    private fun parseInstant(value: String): Long =
        try {
            Instant.parse(value).toEpochMilli()
        } catch (_: DateTimeParseException) {
            System.currentTimeMillis()
        }

    companion object {
        fun displayType(type: String): String = when (type) {
            TransactionType.DEPOSIT -> "Depósito"
            TransactionType.WITHDRAWAL -> "Retiro"
            TransactionType.TRANSFER_RECEIVED -> "Transferencia recibida"
            TransactionType.TRANSFER_SENT -> "Transferencia enviada"
            TransactionType.SERVICE_PAYMENT -> "Servicio básico"
            else -> "Movimiento"
        }

        fun isIncome(type: String): Boolean = TransactionType.isIncome(type)
    }
}

class ApiException(
    val statusCode: Int,
    val rawBody: String?
) : Exception("HTTP $statusCode")
```

> **`database.withTransaction`:** es una extensión de `androidx.room:room-ktx`. Importa `androidx.room.withTransaction`. Si olvidas el import, el compilador te pedirá `withTransaction`.

> **`saveIncoming` es `suspend`:** el listener del WebSocket no puede llamar funciones suspend directamente, así que `WalletSocketManager` lo invocará dentro de un `CoroutineScope(Dispatchers.IO)`. Nunca ejecutes `dao.upsert` en el hilo principal.

> **Idempotencia en el cliente:** cada operación crea un `UUID` distinto. Si el usuario toca dos veces “Depositar”, se envían dos keys distintas y se hacen dos depósitos. Para impedir el doble toque, la UI deshabilita el botón mientras `isLoading` es true (Fase F/G). No reutilices la key entre operaciones distintas; si añades un reintento automático, conserva la misma key para el mismo intento.

> **Aislamiento entre cuentas:** al iniciar sesión se borra el historial local anterior antes de guardar la nueva sesión, y al cerrar sesión se borran tokens e historial. Así Luis no ve movimientos de Ana en un dispositivo compartido. La persistencia exigida por el Word se demuestra con `force-stop` y reapertura, no con logout.

⚠️ **Error típico:** mapear `balanceCents` del `OperationResponse` al saldo y luego ignorarlo. En esta app el saldo se actualiza por REST + WS; guarda el historial y actualiza el `StateFlow` del ViewModel con `response.balanceCents`.

### 8.7. `ui/common/ErrorMapper.kt`

```kotlin
package com.uc1.nexopay.ui.common

import com.uc1.nexopay.data.repository.ApiException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun humanMessage(error: Throwable): String = when (error) {
        is ApiException -> fromApi(error)
        is UnknownHostException -> "No se pudo conectar. Revisa Internet o la URL del backend."
        is SocketTimeoutException -> "El servidor tardó demasiado. Inténtalo otra vez."
        is IOException -> "Sin conexión. Revisa tu red."
        is HttpException -> "Error del servidor (${error.code()})."
        else -> error.message ?: "Ocurrió un error inesperado."
    }

    private fun fromApi(error: ApiException): String {
        val body = error.rawBody.orEmpty()
        val code = runCatching {
            json.parseToJsonElement(body)
                .jsonObject["error"]
                ?.jsonObject
                ?.get("code")
                ?.jsonPrimitive
                ?.content
        }.getOrNull()

        return when (code) {
            "invalid_credentials" -> "Cuenta o contraseña incorrectos."
            "account_locked" -> "Cuenta bloqueada por intentos fallidos. Espera 15 minutos."
            "insufficient_funds" -> "Saldo insuficiente para esta operación."
            "invalid_amount" -> "El monto no es válido."
            "invalid_contract" -> "El código de contrato no es válido."
            "idempotency_conflict" -> "La operación ya fue procesada con otros datos."
            "rate_limited" -> "Demasiadas solicitudes. Espera unos segundos."
            "unauthorized" -> "Tu sesión expiró. Inicia sesión otra vez."
            else -> when (error.statusCode) {
                400, 422 -> "Revisa los datos e inténtalo de nuevo."
                401 -> "Tu sesión expiró. Inicia sesión otra vez."
                403 -> "No tienes permiso para esta operación."
                404 -> "No encontramos la cuenta o el recurso."
                409 -> "La operación entra en conflicto con otra. Reintenta."
                429 -> "Demasiadas solicitudes. Espera unos segundos."
                in 500..599 -> "El servidor falló. Inténtalo más tarde."
                else -> "No se pudo completar la operación."
            }
        }
    }
}
```

> **Importante:** el backend devuelve mensajes en español, pero la app **no** muestra `error.message` crudo por defecto. Mapear códigos evita filtrar detalles internos y permite traducir/mejorar el texto.

✅ **Verificación de la Fase D:**

1. `NexoApi` compila y Retrofit se crea sin `IllegalArgumentException`.
2. Login desde un test o desde la app devuelve tokens.
3. Con un access token vencido (puedes esperar 15 min o poner `ACCESS_TTL_SECONDS=10` en `.env`), la siguiente llamada se reintenta una vez y funciona.
4. Un `401` de refresh limpia la sesión y lleva al login, sin bucle.

⚠️ **Errores típicos**

| Síntoma | Causa | Solución |
|---|---|---|
| `Unable to create converter` | Falta converter o plugin serialization | Revisa dependencias y `@Serializable` |
| `401` en bucle | Autenticador recursivo | Usa `refreshApi` con el cliente base |
| `CLEARTEXT...` | HTTP en release | Usa `local` debug o HTTPS |
| `SocketTimeoutException` | Render dormido | Abre `/health` y espera |
| `JsonDecodingException` | DTO no coincide | Revisa el JSON real en Postman |

---

## 9. FASE E — WEBSOCKET EN TIEMPO REAL

**Requisito del Word:** el saldo debe actualizarse sin recargar cuando otra cuenta transfiere. La app abre un WebSocket autenticado con un **ticket de un solo uso** y escucha `transfer.received` y `balance.updated`.

### 9.1. Protocolo de eventos

| Evento | Quién lo recibe | Datos principales | Uso en la app |
|---|---|---|---|
| `connection.ready` | Al conectar | `accountId`, `balanceCents` | Confirmar conexión y saldo inicial |
| `balance.updated` | Dueño de la operación | `balanceCents`, `reason`, `transaction?` | Actualizar tarjeta de saldo |
| `transfer.received` | Cuenta destino | `transaction`, `fromAccountId`, `balanceCents` | Insertar en Room + actualizar saldo |

Envelope real del backend:

```json
{
  "v": 1,
  "event": "transfer.received",
  "data": {
    "transaction": {
      "id": "8f4a...",
      "type": "TRANSFER_RECEIVED",
      "amountCents": 5000,
      "amount": "50.00",
      "balanceCents": 155000,
      "balance": "1550.00",
      "counterpartyAccountId": "10020002",
      "createdAt": "2026-10-20T18:45:00.000Z"
    },
    "fromAccountId": "10020002",
    "balanceCents": 155000,
    "balance": "1550.00"
  },
  "ts": "2026-10-20T18:45:00.010Z"
}
```

> **Ticket, no JWT en la URL:** la URL del WebSocket puede quedar en logs de proxy. Un ticket de 30 s y un solo uso no sirve después. El servidor guarda solo su hash.

### 9.2. `data/remote/dto/WsDtos.kt` — DTOs de WebSocket

Crea un archivo nuevo para no mezclar imports:

```kotlin
package com.uc1.nexopay.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class WsEnvelope(
    val v: Int = 1,
    val event: String,
    val data: JsonObject,
    val ts: String? = null
)

@Serializable
data class WsReadyData(
    val accountId: String,
    val balanceCents: Long,
    val balance: String? = null
)

@Serializable
data class WsBalanceData(
    val balanceCents: Long,
    val balance: String? = null,
    val reason: String? = null,
    val transaction: TransactionDto? = null
)

@Serializable
data class WsTransferData(
    val transaction: TransactionDto,
    val fromAccountId: String,
    val balanceCents: Long,
    val balance: String? = null
)
```

> **No dupliques** `TransactionDto`: se reutiliza el de `ApiDtos.kt`. `WsEnvelope.data` es un `JsonObject` porque cada evento tiene una forma distinta; se decodifica al tipo concreto después de leer `event`.

### 9.3. `data/remote/WalletSocketManager.kt`

```kotlin
package com.uc1.nexopay.data.remote

import android.util.Log
import com.uc1.nexopay.BuildConfig
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.remote.dto.WsBalanceData
import com.uc1.nexopay.data.remote.dto.WsEnvelope
import com.uc1.nexopay.data.remote.dto.WsReadyData
import com.uc1.nexopay.data.remote.dto.WsTransferData
import com.uc1.nexopay.data.repository.WalletRepository
import com.uc1.nexopay.security.SessionManager
import com.uc1.nexopay.ui.common.ErrorMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class SocketStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

sealed interface WalletEvent {
    data class Ready(val balanceCents: Long) : WalletEvent
    data class BalanceChanged(
        val balanceCents: Long,
        val reason: String?,
        val transaction: TransactionEntity?
    ) : WalletEvent
    data class TransferReceived(
        val transaction: TransactionEntity,
        val fromAccountId: String,
        val balanceCents: Long
    ) : WalletEvent
    data class Failed(val message: String) : WalletEvent
}

class WalletSocketManager(
    private val repository: WalletRepository,
    private val sessionManager: SessionManager,
    private val scope: CoroutineScope
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _status = MutableStateFlow(SocketStatus.DISCONNECTED)
    val status: StateFlow<SocketStatus> = _status.asStateFlow()

    private val _events = MutableSharedFlow<WalletEvent>(
        extraBufferCapacity = 16
    )
    val events: SharedFlow<WalletEvent> = _events.asSharedFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var opening = false

    @Volatile
    private var intentionalClose = false

    private var reconnectJob: Job? = null
    private var retryAttempt = 0

    fun connect() {
        if (sessionManager.session.value == null) return
        if (webSocket != null) return
        if (opening) return

        intentionalClose = false
        openSocket()
    }

    fun disconnect() {
        intentionalClose = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "client_close")
        webSocket = null
        _status.value = SocketStatus.DISCONNECTED
    }

    private fun openSocket() {
        if (intentionalClose) return
        if (webSocket != null) return
        if (opening) return

        opening = true

        scope.launch {
            try {
                _status.value = SocketStatus.CONNECTING

                val ticket = repository.realtimeTicket().getOrElse { error ->
                    _status.value = SocketStatus.DISCONNECTED
                    _events.emit(WalletEvent.Failed(ErrorMapper.humanMessage(error)))
                    scheduleReconnect()
                    return@launch
                }

                if (intentionalClose) return@launch

                val encoded = URLEncoder.encode(ticket.ticket, "UTF-8")
                val request = Request.Builder()
                    .url("${BuildConfig.WS_URL}?ticket=$encoded")
                    .build()

                webSocket = client.newWebSocket(request, listener)
            } catch (error: Exception) {
                _status.value = SocketStatus.DISCONNECTED
                _events.emit(WalletEvent.Failed(ErrorMapper.humanMessage(error)))
                scheduleReconnect()
            } finally {
                opening = false
            }
        }
    }

    private val listener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            retryAttempt = 0
            _status.value = SocketStatus.CONNECTED
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                handleMessage(text)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@WalletSocketManager.webSocket = null
            _status.value = SocketStatus.DISCONNECTED
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            this@WalletSocketManager.webSocket = null
            _status.value = SocketStatus.DISCONNECTED

            scope.launch {
                _events.emit(WalletEvent.Failed(ErrorMapper.humanMessage(t)))
            }
            scheduleReconnect()
        }
    }

    private suspend fun handleMessage(text: String) {
        try {
            val envelope = json.decodeFromString<WsEnvelope>(text)

            when (envelope.event) {
                "connection.ready" -> {
                    val data = json.decodeFromJsonElement<WsReadyData>(envelope.data)
                    _events.emit(WalletEvent.Ready(data.balanceCents))
                }

                "balance.updated" -> {
                    val data = json.decodeFromJsonElement<WsBalanceData>(envelope.data)
                    val entity = data.transaction?.let {
                        repository.saveIncoming(it)
                    }
                    _events.emit(
                        WalletEvent.BalanceChanged(
                            balanceCents = data.balanceCents,
                            reason = data.reason,
                            transaction = entity
                        )
                    )
                }

                "transfer.received" -> {
                    val data = json.decodeFromJsonElement<WsTransferData>(envelope.data)
                    val entity = repository.saveIncoming(data.transaction)
                    _events.emit(
                        WalletEvent.TransferReceived(
                            transaction = entity,
                            fromAccountId = data.fromAccountId,
                            balanceCents = data.balanceCents
                        )
                    )
                }
            }
        } catch (error: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w("NexoPaySocket", "Evento WS inválido: ${error.message}")
            }
        }
    }

    private fun scheduleReconnect() {
        if (intentionalClose) return
        if (sessionManager.session.value == null) return
        if (reconnectJob?.isActive == true) return

        val attempt = retryAttempt.coerceAtMost(5)
        retryAttempt++

        reconnectJob = scope.launch {
            val backoff = (1_000L shl attempt) + Random.nextLong(0, 600)
            delay(backoff)
            if (!intentionalClose && webSocket == null) {
                openSocket()
            }
        }
    }
}
```

> **Reconexión exponencial:** 1 s, 2 s, 4 s, 8 s, 16 s, 32 s (tope), con jitter. Evita martillar Render si está dormido. Al volver a conectar se pide un **ticket nuevo**, porque el anterior ya se consumió.

> **`pingInterval(20s)`:** detecta conexiones muertas que el sistema operativo aún no cerró. Sin ping, el chip podría decir “En línea” con una conexión zombi.

> **`onClosing`:** responde el close handshake. Si no lo haces, algunos proxies dejan el socket colgado.

⚠️ **Error típico:** conectar en `Application.onCreate()` antes de tener sesión. Aquí `connect()` sale si no hay sesión; la actividad llama después del login o al restaurar.

⚠️ **Error típico:** reconectar con el mismo ticket. El backend responde `4401 invalid_ticket`. La reconexión debe pedir otro ticket por REST.

### 9.4. Cableado final de `di/AppContainer.kt`

Reemplaza la versión base por esta:

```kotlin
package com.uc1.nexopay.di

import android.content.Context
import com.uc1.nexopay.data.local.AppDatabase
import com.uc1.nexopay.data.local.DatabaseProvider
import com.uc1.nexopay.data.remote.ApiFactory
import com.uc1.nexopay.data.remote.NexoApi
import com.uc1.nexopay.data.remote.WalletSocketManager
import com.uc1.nexopay.data.repository.WalletRepository
import com.uc1.nexopay.security.SecureStore
import com.uc1.nexopay.security.SessionManager
import com.uc1.nexopay.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val secureStore: SecureStore = SecureStore(appContext)
    val sessionManager: SessionManager = SessionManager(secureStore)
    val settingsStore: SettingsStore = SettingsStore(appContext)

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        DatabaseProvider.get(appContext, secureStore)
    }

    val api: NexoApi by lazy {
        ApiFactory.create(sessionManager) {
            sessionManager.clear()
        }
    }

    val repository: WalletRepository by lazy {
        WalletRepository(api, database, sessionManager)
    }

    val socket: WalletSocketManager by lazy {
        WalletSocketManager(repository, sessionManager, applicationScope)
    }

    fun close() {
        socket.disconnect()
        applicationScope.coroutineContext[Job]?.cancel()
    }
}
```

> **`by lazy`:** la base y Retrofit se crean la primera vez que se usan. Si se crean en `Application.onCreate()`, SQLCipher y la lectura de Keystore pueden alargar el arranque. La primera llamada ocurre dentro de `Dispatchers.IO` en el ViewModel.

> **Cierre:** `close()` es opcional; Android mata el proceso. Úsalo en pruebas instrumentadas para liberar SQLCipher.

### 9.5. Prueba en tiempo real con Postman (ensayo de la defensa)

1. Backend local: `npm start`.
2. App Android: login como **Ana** (`10020001`).
3. En Postman, login como **Luis** (`10020002`) y guarda su `accessToken`.
4. En la app, verifica el chip **En línea**.
5. Desde Postman:

```http
POST http://localhost:3000/api/v1/operations/transfer
Authorization: Bearer <ACCESS_TOKEN_LUIS>
Idempotency-Key: <UUID_NUEVO>
Content-Type: application/json

{
  "toAccountId": "10020001",
  "amountCents": 5000
}
```

6. La app de Ana debe:
   - Cambiar el saldo de `S/ 1,500.00` a `S/ 1,550.00`.
   - Insertar “Transferencia recibida” en la lista.
   - No recargar ni reiniciar.
7. Repite con el emulador en modo avión: el chip debe pasar a **Desconectado** y reconectar al volver la red.

> **Evidencia:** graba la pantalla con la app y Postman lado a lado. Muestra la hora y el saldo antes/después. Esa grabación vale más que diez capturas estáticas.

### 9.6. Prueba del ticket de un solo uso

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:3000/api/v1/auth/login `
  -ContentType 'application/json' `
  -Body '{"accountId":"10020001","password":"Nexo#2026"}'

$ticket = Invoke-RestMethod -Method Post -Uri http://localhost:3000/api/v1/realtime/ticket `
  -Headers @{ Authorization = "Bearer $($login.accessToken)" }

$ticket.ticket
```

Abre `ws://localhost:3000/ws?ticket=...` en Postman y conéctate. Cierra y vuelve a intentar con el mismo ticket: debe cerrar con código `4401`. Eso demuestra que no es reutilizable.

✅ **Verificación de la Fase E:**

1. La app recibe `connection.ready` y el chip se pone verde.
2. Una transferencia desde Postman actualiza saldo e historial sin recargar.
3. Al matar el backend, el chip se pone rojo y la app reintenta.
4. Al volver el backend, se reconecta sola y pide ticket nuevo.
5. El ticket viejo no se puede reutilizar.

⚠️ **Errores típicos**

| Síntoma | Causa | Solución |
|---|---|---|
| `4401 invalid_ticket` | Ticket vencido o reutilizado | Pide otro ticket antes de reconectar |
| Chip verde pero no llegan eventos | Estás conectado con otra cuenta | Verifica `accountId` del login |
| `onFailure` inmediato en emulador | URL `localhost` en vez de `10.0.2.2` | Usa flavor `local` |
| `onFailure` en celular real | Falta `adb reverse` | Ejecuta `adb reverse tcp:3000 tcp:3000` |
| Bucle de reconexión | Token inválido | Revisa refresh; borra datos y vuelve a loguear |

---

## 10. FASE F — CONCURRENCIA, CORRUTINAS Y VIEWMODEL

**Requisito del Word:** red y base de datos en `Dispatchers.IO`, UI en `Dispatchers.Main`, cero ANR. Aquí están las reglas y el código que lo garantiza.

### 10.1. Reglas de oro (memorízalas para la defensa)

1. **Nunca** llames Retrofit, Room, SQLCipher ni OkHttp desde el hilo principal.
2. `viewModelScope.launch(Dispatchers.IO)` para operaciones de red/BD.
3. Los `StateFlow` se actualizan desde cualquier hilo, pero se **recolectan** en el lifecycle de la Activity (Main).
4. Las funciones `suspend` de Retrofit y Room ya cambian de hilo; aun así, el bloque completo va en IO para que el mapeo y la lógica no bloqueen.
5. `withContext(Dispatchers.Main)` solo si necesitas tocar una vista directamente desde un bloque IO. En MVVM no debería hacer falta: la vista observa estado.
6. Un `Mutex` o un flag atómico evita dobles operaciones por doble toque.
7. `Flow` de Room se cancela solo cuando se cancela el scope del ViewModel. No hagas `collect` bloqueante en la Activity.

> **Frase para la defensa:** “La UI es una función del estado. El ViewModel recibe eventos, ejecuta IO en corrutinas y publica un `StateFlow`; la Activity solo renderiza y reacciona. Por eso no hay ANR.”

### 10.2. `ui/common/UiUtils.kt`

```kotlin
package com.uc1.nexopay.ui.common

import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.local.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UiUtils {

    private val locale = Locale("es", "PE")
    private val moneyFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val dateFormat = DateTimeFormatter
        .ofPattern("dd MMM yyyy · HH:mm", locale)
        .withZone(ZoneId.systemDefault())

    fun formatCents(cents: Long): String {
        val value = BigDecimal.valueOf(cents, 2)
        return "S/ ${moneyFormat.format(value)}"
    }

    fun parseAmountToCents(raw: String): Long? {
        val cleaned = raw
            .trim()
            .replace("S/", "", ignoreCase = true)
            .replace(" ", "")
        if (cleaned.isEmpty()) return null

        val normalized = when {
            cleaned.contains(',') && cleaned.contains('.') ->
                cleaned.replace(",", "")
            cleaned.contains(',') ->
                cleaned.replace(',', '.')
            else -> cleaned
        }

        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO) return null

        return runCatching {
            value.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0 }
    }

    fun formatDate(epochMillis: Long): String =
        dateFormat.format(Instant.ofEpochMilli(epochMillis))

    fun transactionTitle(transaction: TransactionEntity): String = when (transaction.type) {
        TransactionType.DEPOSIT -> "Depósito"
        TransactionType.WITHDRAWAL -> "Retiro"
        TransactionType.TRANSFER_RECEIVED -> "Transferencia recibida"
        TransactionType.TRANSFER_SENT -> "Transferencia enviada"
        TransactionType.SERVICE_PAYMENT ->
            transaction.serviceName?.let { "Servicio: $it" } ?: "Servicio básico"
        else -> transaction.description ?: "Movimiento"
    }

    fun transactionSubtitle(transaction: TransactionEntity): String {
        val detail = when {
            transaction.counterpartyAccountId != null ->
                "Cuenta ${transaction.counterpartyAccountId}"
            transaction.contractCode != null ->
                "Contrato ${transaction.contractCode}"
            else -> transaction.description ?: "NexoPay"
        }
        return "${formatDate(transaction.createdAt)} · $detail"
    }

    fun signedAmount(transaction: TransactionEntity): String {
        val prefix = if (TransactionType.isIncome(transaction.type)) "+" else "−"
        return "$prefix ${formatCents(transaction.amountCents)}"
    }
}
```

> **Dinero:** `BigDecimal.valueOf(cents, 2)` construye el valor exacto desde enteros. No se usa `Double` para calcular saldos. El formateo es solo presentación.

> **Fechas:** `java.time` está disponible desde API 26, que es nuestro `minSdk`. No necesitas desugaring.

### 10.3. `ui/login/LoginViewModel.kt`

```kotlin
package com.uc1.nexopay.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uc1.nexopay.di.AppContainer
import com.uc1.nexopay.ui.common.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isRestoring: Boolean = true,
    val lastAccount: String = "",
    val error: String? = null
)

sealed interface LoginEvent {
    data object Success : LoginEvent
    data class Message(val text: String) : LoginEvent
}

class LoginViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    private var restoreAttempted = false

    init {
        viewModelScope.launch {
            container.settingsStore.lastAccount.collect { account ->
                _state.update { it.copy(lastAccount = account.orEmpty()) }
            }
        }
    }

    fun restoreSession() {
        if (restoreAttempted) return
        restoreAttempted = true

        viewModelScope.launch(Dispatchers.IO) {
            val restored = container.repository.restoreSession().isSuccess
            if (!restored) {
                _state.update { it.copy(isRestoring = false) }
                return@launch
            }

            val refreshed = container.repository.refreshAccessToken()
            if (refreshed.isSuccess || container.sessionManager.session.value != null) {
                _state.update { it.copy(isRestoring = false) }
                _events.emit(LoginEvent.Success)
            } else {
                container.repository.logout()
                _state.update { it.copy(isRestoring = false) }
            }
        }
    }

    fun login(accountId: String, password: String) {
        if (_state.value.isLoading) return

        if (accountId.isBlank()) {
            _state.update { it.copy(error = "Ingresa tu número de cuenta.") }
            return
        }
        if (password.isBlank()) {
            _state.update { it.copy(error = "Ingresa tu contraseña.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = container.repository.login(accountId, password)

            result.onSuccess { response ->
                container.settingsStore.setLastAccount(response.account.id)
                _state.update { it.copy(isLoading = false, isRestoring = false) }
                _events.emit(LoginEvent.Success)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRestoring = false,
                        error = ErrorMapper.humanMessage(error)
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
```

> **`isRestoring`:** mientras se intenta restaurar la sesión, la Activity muestra un indicador. Si no hay refresh token, cae al formulario sin mostrar error.

> **`container.repository.login` guarda la sesión:** el ViewModel no manipula tokens. Esa separación evita fugas y facilita las pruebas. El contenedor se accede dentro de `Dispatchers.IO`, así SQLCipher y Keystore no se inicializan en el hilo principal.

### 10.4. `ui/dashboard/DashboardViewModel.kt`

```kotlin
package com.uc1.nexopay.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.remote.SocketStatus
import com.uc1.nexopay.data.remote.WalletEvent
import com.uc1.nexopay.data.remote.WalletSocketManager
import com.uc1.nexopay.data.repository.WalletRepository
import com.uc1.nexopay.security.SessionManager
import com.uc1.nexopay.ui.common.ErrorMapper
import com.uc1.nexopay.ui.common.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DashboardUiState(
    val owner: String = "",
    val accountId: String = "",
    val balanceCents: Long = 0L,
    val transactions: List<TransactionEntity> = emptyList(),
    val socketStatus: SocketStatus = SocketStatus.DISCONNECTED,
    val isLoading: Boolean = false,
    val isOperating: Boolean = false,
    val error: String? = null
)

sealed interface DashboardEvent {
    data class Message(val text: String) : DashboardEvent
    data object SessionExpired : DashboardEvent
}

class DashboardViewModel(
    private val repository: WalletRepository,
    private val sessionManager: SessionManager,
    private val socket: WalletSocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    private val operationLock = Mutex()
    private var started = false

    init {
        val session = sessionManager.session.value
        _state.update {
            it.copy(
                owner = session?.name.orEmpty(),
                accountId = session?.accountId.orEmpty()
            )
        }

        repository.transactions
            .onEach { list -> _state.update { it.copy(transactions = list) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            socket.status.collect { status ->
                _state.update { it.copy(socketStatus = status) }
            }
        }

        viewModelScope.launch {
            socket.events.collect { event -> handleSocketEvent(event) }
        }
    }

    fun onStart() {
        if (!started) {
            started = true
            socket.connect()
            viewModelScope.launch(Dispatchers.IO) { loadInitialData() }
        }
    }

    fun onStop() {
        started = false
        socket.disconnect()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { loadInitialData() }
    }

    fun deposit(amountCents: Long) = runOperation(
        successMessage = "Depósito de ${UiUtils.formatCents(amountCents)} realizado."
    ) {
        repository.deposit(amountCents)
    }

    fun withdraw(amountCents: Long) = runOperation(
        successMessage = "Retiro de ${UiUtils.formatCents(amountCents)} realizado."
    ) {
        repository.withdraw(amountCents)
    }

    fun transfer(toAccountId: String, amountCents: Long) = runOperation(
        successMessage = "Transferencia de ${UiUtils.formatCents(amountCents)} enviada."
    ) {
        repository.transfer(toAccountId, amountCents)
    }

    fun payService(contractCode: String, amountCents: Long) = runOperation(
        successMessage = "Pago de servicio registrado."
    ) {
        repository.payService(contractCode, amountCents)
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logout()
            socket.disconnect()
            _events.emit(DashboardEvent.SessionExpired)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun loadInitialData() {
        _state.update { it.copy(isLoading = true, error = null) }

        if (sessionManager.accessToken.isNullOrBlank()) {
            val refreshed = repository.refreshAccessToken()
            if (refreshed.isFailure) {
                if (sessionManager.session.value == null) {
                    handleSessionFailure()
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = refreshed.exceptionOrNull()
                                ?.let(ErrorMapper::humanMessage)
                                ?: "Sin conexión. Se muestra el historial local."
                        )
                    }
                }
                return
            }
        }

        repository.balance().onSuccess { response ->
            _state.update { it.copy(balanceCents = response.balanceCents) }
        }.onFailure { error ->
            if (sessionManager.session.value == null) {
                handleSessionFailure()
            } else {
                _state.update { it.copy(error = ErrorMapper.humanMessage(error)) }
            }
        }

        repository.syncTransactions().onFailure { error ->
            if (sessionManager.session.value == null) {
                handleSessionFailure()
            }
        }

        _state.update { it.copy(isLoading = false) }
    }

    private fun runOperation(
        successMessage: String,
        block: suspend () -> Result<com.uc1.nexopay.data.remote.dto.OperationResponse>
    ) {
        if (!operationLock.tryLock()) return

        _state.update { it.copy(isOperating = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = block()
                result.onSuccess { response ->
                    _state.update {
                        it.copy(
                            balanceCents = response.balanceCents,
                            isOperating = false
                        )
                    }
                    _events.emit(DashboardEvent.Message(successMessage))
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isOperating = false,
                            error = ErrorMapper.humanMessage(error)
                        )
                    }
                }
            } finally {
                operationLock.unlock()
                _state.update { it.copy(isOperating = false) }
            }
        }
    }

    private suspend fun handleSocketEvent(event: WalletEvent) {
        when (event) {
            is WalletEvent.Ready -> {
                _state.update { it.copy(balanceCents = event.balanceCents) }
            }

            is WalletEvent.BalanceChanged -> {
                _state.update { it.copy(balanceCents = event.balanceCents) }
            }

            is WalletEvent.TransferReceived -> {
                _state.update { it.copy(balanceCents = event.balanceCents) }
                _events.emit(
                    DashboardEvent.Message(
                        "Recibiste ${UiUtils.formatCents(event.transaction.amountCents)} " +
                            "de la cuenta ${event.fromAccountId}."
                    )
                )
            }

            is WalletEvent.Failed -> {
                if (sessionManager.session.value == null) {
                    _events.emit(DashboardEvent.SessionExpired)
                }
            }
        }
    }

    private suspend fun handleSessionFailure() {
        sessionManager.clear()
        socket.disconnect()
        _events.emit(DashboardEvent.SessionExpired)
    }
}
```

> **`Mutex.tryLock()`:** si el usuario toca dos veces, la segunda llamada sale inmediatamente. No se encola una segunda operación financiera. El `finally` libera el lock incluso si hay excepción.

> **`finally` actualiza `isOperating=false` dos veces:** es intencional. El `onSuccess/onFailure` lo apaga para que la UI reaccione antes; el `finally` garantiza el apagado ante una excepción no controlada. Actualizar un `StateFlow` dos veces es idempotente.

> **`loadInitialData` en IO:** incluye refresh, balance y sincronización. La UI no se bloquea. El `StateFlow` se renderiza en Main.

> **Sin red:** si el refresh falla por `IOException`/timeout/`5xx`, la sesión no se borra; se muestra el error y el historial de Room sigue visible. Solo un `401/403` cierra la sesión.

⚠️ **Error típico:** usar `runBlocking` en el ViewModel. Eso bloquea el hilo que lo invoca y puede producir ANR. Aquí no se usa.

⚠️ **Error típico:** lanzar la operación desde el click sin `launch`. Una función `suspend` no se puede llamar desde un listener normal; el `launch` es obligatorio.

### 10.5. `ui/common/NexoViewModelFactory.kt`

```kotlin
package com.uc1.nexopay.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uc1.nexopay.di.AppContainer
import com.uc1.nexopay.ui.dashboard.DashboardViewModel
import com.uc1.nexopay.ui.login.LoginViewModel

class NexoViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(container)

            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    repository = container.repository,
                    sessionManager = container.sessionManager,
                    socket = container.socket
                )

            else -> error("ViewModel no soportado: ${modelClass.name}")
        }
        return viewModel as T
    }
}
```

> **Sin frameworks de DI:** para el alcance del curso, un contenedor manual es suficiente y transparente. Hilt añadiría kapt/KSP y configuración que no aporta puntos aquí.

### 10.6. Cómo demostrar concurrencia y ausencia de ANR

1. Abre **Logcat** y filtra por `StrictMode`.
2. Haz un depósito y desplázate por el historial al mismo tiempo.
3. No debe aparecer `StrictMode policy violation` ni `ANR in com.uc1.nexopay`.
4. En **Android Studio → Profiler → CPU**, verifica que el hilo main está mayormente en `Running`/`Native` y no bloqueado en `Network` o `SQLite`.
5. Explica en el informe: “Las llamadas de red usan las corrutinas de Retrofit; Room usa su executor; el ViewModel lanza en IO y publica estado; la Activity recolecta en `repeatOnLifecycle(STARTED)`”.

✅ **Verificación de la Fase F:**

- El saldo y el historial se actualizan sin tocar la UI directamente.
- El botón de operación se deshabilita visualmente mientras `isOperating`.
- Un doble toque no duplica el depósito.
- El logout limpia tokens y vuelve al login.
- Al vencer el access token, el refresh ocurre sin sacar al usuario.

---

## 11. FASE G — UI XML: LOGIN, DASHBOARD, BOTTOM SHEETS E HISTORIAL

**Requisito del Word:** dashboard con saldo grande, indicador En línea/Desconectado, 4 botones con iconos y diálogos, historial reactivo con ingresos en verde y egresos en rojo. Todo con XML + Material 3 y ViewBinding.

### 11.1. `res/layout/activity_login.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/nexo_background">

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_horizontal"
            android:orientation="vertical"
            android:paddingStart="24dp"
            android:paddingTop="40dp"
            android:paddingEnd="24dp"
            android:paddingBottom="32dp">

            <com.google.android.material.card.MaterialCardView
                android:layout_width="88dp"
                android:layout_height="88dp"
                app:cardBackgroundColor="@color/nexo_primary_container"
                app:cardCornerRadius="28dp"
                app:cardElevation="0dp">

                <androidx.appcompat.widget.AppCompatImageView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:layout_gravity="center"
                    android:contentDescription="@string/cd_wallet"
                    android:src="@drawable/ic_wallet"
                    app:tint="@color/nexo_primary" />
            </com.google.android.material.card.MaterialCardView>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="24dp"
                android:text="@string/login_title"
                android:textColor="@color/nexo_on_surface"
                android:textSize="28sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:gravity="center"
                android:text="@string/login_subtitle"
                android:textColor="@color/nexo_on_surface_variant"
                android:textSize="14sp" />

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/inputLayoutAccount"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="32dp"
                android:hint="@string/login_account">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/inputAccount"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:autofillHints="username"
                    android:imeOptions="actionNext"
                    android:inputType="number"
                    android:maxLength="8"
                    android:singleLine="true" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/inputLayoutPassword"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:hint="@string/login_password"
                app:endIconMode="password_toggle">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/inputPassword"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:autofillHints="password"
                    android:imeOptions="actionDone"
                    android:inputType="textPassword"
                    android:maxLength="72"
                    android:singleLine="true" />
            </com.google.android.material.textfield.TextInputLayout>

            <TextView
                android:id="@+id/textError"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:textColor="@color/nexo_danger"
                android:textSize="13sp"
                android:visibility="gone" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/buttonLogin"
                style="@style/Widget.NexoPay.PrimaryButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="@string/login_button" />

            <com.google.android.material.progressindicator.CircularProgressIndicator
                android:id="@+id/progressLogin"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:indeterminateTint="@color/nexo_primary"
                android:visibility="gone" />

            <com.google.android.material.card.MaterialCardView
                android:id="@+id/cardDemo"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="28dp"
                android:clickable="true"
                android:focusable="true"
                android:foreground="?attr/selectableItemBackground"
                app:cardBackgroundColor="@color/nexo_secondary_container"
                app:cardCornerRadius="20dp"
                app:cardElevation="0dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/login_demo_title"
                        android:textColor="@color/nexo_on_secondary_container"
                        android:textSize="14sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:text="@string/login_demo_body"
                        android:textColor="@color/nexo_on_secondary_container"
                        android:textSize="13sp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="20dp"
                android:text="Toca la tarjeta para autocompletar la cuenta de Ana."
                android:textColor="@color/nexo_on_surface_variant"
                android:textSize="11sp" />
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</FrameLayout>
```

> **Accesibilidad:** los campos tienen `autofillHints`; el botón de mostrar contraseña lo aporta `TextInputLayout`; los textos usan colores con contraste. No uses `inputType="textVisiblePassword"` para la contraseña.

### 11.2. `ui/login/LoginActivity.kt`

```kotlin
package com.uc1.nexopay.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.uc1.nexopay.NexoPayApp
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.ActivityLoginBinding
import com.uc1.nexopay.ui.common.NexoViewModelFactory
import com.uc1.nexopay.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var prefilled = false

    private val viewModel: LoginViewModel by viewModels {
        NexoViewModelFactory((application as NexoPayApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.buttonLogin.setOnClickListener { submit() }
        binding.cardDemo.setOnClickListener { fillDemo() }

        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        observe()
        viewModel.restoreSession()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: LoginUiState) {
        val busy = state.isLoading || state.isRestoring

        binding.progressLogin.isVisible = busy
        binding.buttonLogin.isEnabled = !busy
        binding.inputAccount.isEnabled = !busy
        binding.inputPassword.isEnabled = !busy

        if (!prefilled && binding.inputAccount.text.isNullOrBlank() && state.lastAccount.isNotBlank()) {
            binding.inputAccount.setText(state.lastAccount)
            prefilled = true
        }

        binding.textError.isVisible = state.error != null
        binding.textError.text = state.error.orEmpty()
    }

    private fun handleEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.Success -> openDashboard()
            is LoginEvent.Message -> {
                Snackbar.make(binding.root, event.text, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun submit() {
        hideKeyboard()
        viewModel.login(
            accountId = binding.inputAccount.text?.toString().orEmpty(),
            password = binding.inputPassword.text?.toString().orEmpty()
        )
    }

    private fun fillDemo() {
        binding.inputAccount.setText("10020001")
        binding.inputPassword.setText("Nexo#2026")
        binding.textError.isVisible = false
    }

    private fun openDashboard() {
        if (isFinishing || isDestroyed) return
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}
```

> **`restoreSession()`** intenta refresh silencioso al abrir. Si no hay refresh token o falla, muestra el formulario. El usuario no ve un error por una sesión vieja.

### 11.3. `res/layout/activity_dashboard.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/nexo_background"
    android:paddingStart="20dp"
    android:paddingEnd="20dp">

    <LinearLayout
        android:id="@+id/header"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/textGreeting"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@color/nexo_on_surface"
                android:textSize="22sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/textSubtitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:text="@string/dashboard_subtitle"
                android:textColor="@color/nexo_on_surface_variant"
                android:textSize="12sp" />
        </LinearLayout>

        <TextView
            android:id="@+id/textStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_chip_offline"
            android:contentDescription="@string/cd_status"
            android:paddingStart="10dp"
            android:paddingTop="6dp"
            android:paddingEnd="10dp"
            android:paddingBottom="6dp"
            android:text="@string/dashboard_status_offline"
            android:textColor="@color/nexo_chip_offline_text"
            android:textSize="11sp"
            android:textStyle="bold" />

        <ImageButton
            android:id="@+id/buttonLogout"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:layout_marginStart="6dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/cd_logout"
            android:padding="10dp"
            android:src="@drawable/ic_logout"
            android:tint="@color/nexo_on_surface_variant" />
    </LinearLayout>

    <FrameLayout
        android:id="@+id/cardBalance"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:background="@drawable/bg_balance_card"
        android:padding="24dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/header">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:alpha="0.85"
                android:text="@string/dashboard_balance_label"
                android:textColor="@color/nexo_on_primary"
                android:textSize="13sp" />

            <TextView
                android:id="@+id/textBalance"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="@string/dashboard_balance_loading"
                android:textAppearance="@style/TextAppearance.NexoPay.Balance"
                android:textColor="@color/nexo_on_primary" />

            <TextView
                android:id="@+id/textAccount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:alpha="0.8"
                android:textColor="@color/nexo_on_primary"
                android:textSize="13sp" />
        </LinearLayout>
    </FrameLayout>

    <LinearLayout
        android:id="@+id/actions"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="18dp"
        android:orientation="vertical"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/cardBalance">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/buttonDeposit"
                style="@style/Widget.NexoPay.ActionButton"
                android:layout_width="0dp"
                android:layout_height="88dp"
                android:layout_weight="1"
                android:text="@string/dashboard_action_deposit"
                app:icon="@drawable/ic_deposit"
                app:iconTint="@color/nexo_primary" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/buttonWithdraw"
                style="@style/Widget.NexoPay.ActionButton"
                android:layout_width="0dp"
                android:layout_height="88dp"
                android:layout_marginStart="12dp"
                android:layout_weight="1"
                android:text="@string/dashboard_action_withdraw"
                app:icon="@drawable/ic_withdraw"
                app:iconTint="@color/nexo_primary" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:orientation="horizontal">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/buttonTransfer"
                style="@style/Widget.NexoPay.ActionButton"
                android:layout_width="0dp"
                android:layout_height="88dp"
                android:layout_weight="1"
                android:text="@string/dashboard_action_transfer"
                app:icon="@drawable/ic_transfer"
                app:iconTint="@color/nexo_primary" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/buttonService"
                style="@style/Widget.NexoPay.ActionButton"
                android:layout_width="0dp"
                android:layout_height="88dp"
                android:layout_marginStart="12dp"
                android:layout_weight="1"
                android:text="@string/dashboard_action_service"
                app:icon="@drawable/ic_service"
                app:iconTint="@color/nexo_primary" />
        </LinearLayout>
    </LinearLayout>

    <LinearLayout
        android:id="@+id/historyHeader"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="18dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/actions">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/dashboard_history"
            android:textColor="@color/nexo_on_surface"
            android:textSize="18sp"
            android:textStyle="bold" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/buttonRefresh"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/dashboard_refresh" />
    </LinearLayout>

    <FrameLayout
        android:id="@+id/historyContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="6dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/historyHeader">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerTransactions"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipToPadding="false"
            android:paddingBottom="20dp" />

        <LinearLayout
            android:id="@+id/layoutEmpty"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:orientation="vertical"
            android:padding="24dp">

            <ImageView
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:alpha="0.4"
                android:contentDescription="@string/cd_history"
                android:src="@drawable/ic_history"
                android:tint="@color/nexo_on_surface_variant" />

            <TextView
                android:id="@+id/textEmpty"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:gravity="center"
                android:text="@string/dashboard_history_empty"
                android:textColor="@color/nexo_on_surface_variant"
                android:textSize="13sp" />
        </LinearLayout>

        <com.google.android.material.progressindicator.CircularProgressIndicator
            android:id="@+id/progressHistory"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:indeterminateTint="@color/nexo_primary"
            android:visibility="gone" />
    </FrameLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

> **Sin `ScrollView` envolviendo el RecyclerView:** se usa ConstraintLayout y el RecyclerView ocupa el espacio restante. Así el scroll del historial es nativo y no se inflan 200 filas de golpe.

### 11.4. `res/layout/item_transaction.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="10dp"
    app:cardBackgroundColor="@color/nexo_card"
    app:cardCornerRadius="20dp"
    app:cardElevation="0dp"
    app:strokeColor="@color/nexo_surface_variant"
    app:strokeWidth="1dp">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="14dp">

        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardIcon"
            android:layout_width="44dp"
            android:layout_height="44dp"
            app:cardBackgroundColor="@color/nexo_primary_container"
            app:cardCornerRadius="22dp"
            app:cardElevation="0dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent">

            <ImageView
                android:id="@+id/imageIcon"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="center"
                android:contentDescription="@null"
                android:src="@drawable/ic_deposit" />
        </com.google.android.material.card.MaterialCardView>

        <TextView
            android:id="@+id/textTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:layout_marginEnd="8dp"
            android:ellipsize="end"
            android:maxLines="1"
            android:textColor="@color/nexo_on_surface"
            android:textSize="15sp"
            android:textStyle="bold"
            app:layout_constraintEnd_toStartOf="@id/textAmount"
            app:layout_constraintStart_toEndOf="@id/cardIcon"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/textSubtitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="3dp"
            android:ellipsize="end"
            android:maxLines="1"
            android:textColor="@color/nexo_on_surface_variant"
            android:textSize="12sp"
            app:layout_constraintEnd_toStartOf="@id/textAmount"
            app:layout_constraintStart_toStartOf="@id/textTitle"
            app:layout_constraintTop_toBottomOf="@id/textTitle" />

        <TextView
            android:id="@+id/textAmount"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="15sp"
            android:textStyle="bold"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent" />
    </androidx.constraintlayout.widget.ConstraintLayout>
</com.google.android.material.card.MaterialCardView>
```

### 11.5. `ui/dashboard/TransactionAdapter.kt`

```kotlin
package com.uc1.nexopay.ui.dashboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uc1.nexopay.R
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.local.TransactionType
import com.uc1.nexopay.databinding.ItemTransactionBinding
import com.uc1.nexopay.ui.common.UiUtils

class TransactionAdapter :
    ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(DIFF) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionEntity) {
            val context = binding.root.context
            val income = TransactionType.isIncome(transaction.type)

            val accent = ContextCompat.getColor(
                context,
                if (income) R.color.nexo_success else R.color.nexo_danger
            )
            val container = ContextCompat.getColor(
                context,
                if (income) R.color.nexo_success_container else R.color.nexo_danger_container
            )

            binding.textTitle.text = UiUtils.transactionTitle(transaction)
            binding.textSubtitle.text = UiUtils.transactionSubtitle(transaction)
            binding.textAmount.text = UiUtils.signedAmount(transaction)
            binding.textAmount.setTextColor(accent)

            binding.cardIcon.setCardBackgroundColor(container)
            binding.imageIcon.setImageResource(iconFor(transaction.type))
            binding.imageIcon.imageTintList = ColorStateList.valueOf(accent)
        }

        private fun iconFor(type: String): Int = when (type) {
            TransactionType.DEPOSIT -> R.drawable.ic_deposit
            TransactionType.WITHDRAWAL -> R.drawable.ic_withdraw
            TransactionType.TRANSFER_SENT,
            TransactionType.TRANSFER_RECEIVED -> R.drawable.ic_transfer
            TransactionType.SERVICE_PAYMENT -> R.drawable.ic_service
            else -> R.drawable.ic_history
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TransactionEntity>() {
            override fun areItemsTheSame(
                oldItem: TransactionEntity,
                newItem: TransactionEntity
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: TransactionEntity,
                newItem: TransactionEntity
            ): Boolean = oldItem == newItem
        }
    }
}
```

> **Verde/rojo:** se usa `nexo_success` y `nexo_danger`, no colores hardcodeados en el adapter. El contenedor del icono también cambia para reforzar la lectura.

### 11.6. `res/layout/sheet_amount.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="24dp"
    android:paddingTop="20dp"
    android:paddingEnd="24dp"
    android:paddingBottom="28dp">

    <TextView
        android:id="@+id/textTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@color/nexo_on_surface"
        android:textSize="22sp"
        android:textStyle="bold" />

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/inputLayoutAmount"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:hint="@string/amount_sheet_amount"
        app:prefixText="S/ ">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/inputAmount"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:imeOptions="actionDone"
            android:inputType="numberDecimal"
            android:maxLength="12"
            android:singleLine="true" />
    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:id="@+id/textAvailable"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@color/nexo_on_surface_variant"
        android:textSize="13sp" />

    <TextView
        android:id="@+id/textError"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@color/nexo_danger"
        android:textSize="13sp"
        android:visibility="gone" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:orientation="horizontal">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/buttonCancel"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/common_cancel" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/buttonConfirm"
            style="@style/Widget.NexoPay.PrimaryButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:layout_weight="1"
            android:text="@string/amount_sheet_confirm_deposit" />
    </LinearLayout>
</LinearLayout>
```

### 11.7. `ui/dashboard/AmountSheet.kt`

```kotlin
package com.uc1.nexopay.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.SheetAmountBinding
import com.uc1.nexopay.ui.common.UiUtils

class AmountSheet : BottomSheetDialogFragment() {

    enum class Mode { DEPOSIT, WITHDRAW }

    private var _binding: SheetAmountBinding? = null
    private val binding get() = _binding!!

    var onConfirm: ((Long) -> Unit)? = null

    private val mode: Mode
        get() = Mode.valueOf(requireArguments().getString(ARG_MODE)!!)

    private val balanceCents: Long
        get() = requireArguments().getLong(ARG_BALANCE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetAmountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textTitle.setText(
            if (mode == Mode.DEPOSIT) {
                R.string.amount_sheet_deposit_title
            } else {
                R.string.amount_sheet_withdraw_title
            }
        )
        binding.buttonConfirm.setText(
            if (mode == Mode.DEPOSIT) {
                R.string.amount_sheet_confirm_deposit
            } else {
                R.string.amount_sheet_confirm_withdraw
            }
        )

        binding.textAvailable.isVisible = mode == Mode.WITHDRAW
        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        binding.inputAmount.doAfterTextChanged {
            binding.textError.isVisible = false
        }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener { submit() }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun submit() {
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        val error = when {
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> "El monto máximo es S/ 100,000.00."
            mode == Mode.WITHDRAW && cents > balanceCents ->
                getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        if (error != null) {
            binding.textError.text = error
            binding.textError.isVisible = true
            return
        }

        onConfirm?.invoke(cents!!)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MODE = "mode"
        private const val ARG_BALANCE = "balance"
        private const val MAX_AMOUNT_CENTS = 10_000_000L

        fun newInstance(mode: Mode, balanceCents: Long): AmountSheet =
            AmountSheet().apply {
                arguments = bundleOf(
                    ARG_MODE to mode.name,
                    ARG_BALANCE to balanceCents
                )
            }
    }
}
```

### 11.8. `res/layout/sheet_transfer.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="24dp"
    android:paddingTop="20dp"
    android:paddingEnd="24dp"
    android:paddingBottom="28dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/transfer_sheet_title"
        android:textColor="@color/nexo_on_surface"
        android:textSize="22sp"
        android:textStyle="bold" />

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/inputLayoutDestination"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:hint="@string/transfer_sheet_destination">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/inputDestination"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:imeOptions="actionNext"
            android:inputType="number"
            android:maxLength="8"
            android:singleLine="true" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/inputLayoutAmount"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:hint="@string/transfer_sheet_amount"
        app:prefixText="S/ ">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/inputAmount"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:imeOptions="actionDone"
            android:inputType="numberDecimal"
            android:maxLength="12"
            android:singleLine="true" />
    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:id="@+id/textAvailable"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@color/nexo_on_surface_variant"
        android:textSize="13sp" />

    <TextView
        android:id="@+id/textError"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@color/nexo_danger"
        android:textSize="13sp"
        android:visibility="gone" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:orientation="horizontal">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/buttonCancel"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/common_cancel" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/buttonConfirm"
            style="@style/Widget.NexoPay.PrimaryButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:layout_weight="1"
            android:text="@string/transfer_sheet_confirm" />
    </LinearLayout>
</LinearLayout>
```

### 11.9. `ui/dashboard/TransferSheet.kt`

```kotlin
package com.uc1.nexopay.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.SheetTransferBinding
import com.uc1.nexopay.ui.common.UiUtils

class TransferSheet : BottomSheetDialogFragment() {

    private var _binding: SheetTransferBinding? = null
    private val binding get() = _binding!!

    var onConfirm: ((String, Long) -> Unit)? = null

    private val balanceCents: Long
        get() = requireArguments().getLong(ARG_BALANCE)

    private val ownAccountId: String
        get() = requireArguments().getString(ARG_OWN_ACCOUNT).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        binding.inputDestination.doAfterTextChanged {
            binding.textError.isVisible = false
        }
        binding.inputAmount.doAfterTextChanged {
            binding.textError.isVisible = false
        }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener { submit() }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun submit() {
        val destination = binding.inputDestination.text?.toString().orEmpty().trim()
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        val error = when {
            !destination.matches(Regex("^\\d{8}$")) ->
                "La cuenta destino debe tener exactamente 8 dígitos."
            destination == ownAccountId ->
                getString(R.string.transfer_sheet_same_account)
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> "El monto máximo es S/ 100,000.00."
            cents > balanceCents -> getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        if (error != null) {
            binding.textError.text = error
            binding.textError.isVisible = true
            return
        }

        onConfirm?.invoke(destination, cents!!)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BALANCE = "balance"
        private const val ARG_OWN_ACCOUNT = "own_account"
        private const val MAX_AMOUNT_CENTS = 10_000_000L

        fun newInstance(balanceCents: Long, ownAccountId: String): TransferSheet =
            TransferSheet().apply {
                arguments = bundleOf(
                    ARG_BALANCE to balanceCents,
                    ARG_OWN_ACCOUNT to ownAccountId
                )
            }
    }
}
```

### 11.10. `res/layout/sheet_service.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="24dp"
    android:paddingTop="20dp"
    android:paddingEnd="24dp"
    android:paddingBottom="28dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/service_sheet_title"
        android:textColor="@color/nexo_on_surface"
        android:textSize="22sp"
        android:textStyle="bold" />

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/inputLayoutContract"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:hint="@string/service_sheet_code"
        app:helperText="@string/service_sheet_hint">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/inputContract"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:digits="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-"
            android:imeOptions="actionNext"
            android:inputType="textCapCharacters"
            android:maxLength="17"
            android:singleLine="true" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/inputLayoutAmount"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:hint="@string/service_sheet_amount"
        app:prefixText="S/ ">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/inputAmount"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:imeOptions="actionDone"
            android:inputType="numberDecimal"
            android:maxLength="12"
            android:singleLine="true" />
    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:id="@+id/textAvailable"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@color/nexo_on_surface_variant"
        android:textSize="13sp" />

    <TextView
        android:id="@+id/textError"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@color/nexo_danger"
        android:textSize="13sp"
        android:visibility="gone" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:orientation="horizontal">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/buttonCancel"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/common_cancel" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/buttonConfirm"
            style="@style/Widget.NexoPay.PrimaryButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:layout_weight="1"
            android:text="@string/service_sheet_confirm" />
    </LinearLayout>
</LinearLayout>
```

### 11.11. `ui/dashboard/ServiceSheet.kt`

```kotlin
package com.uc1.nexopay.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.SheetServiceBinding
import com.uc1.nexopay.ui.common.UiUtils

class ServiceSheet : BottomSheetDialogFragment() {

    private var _binding: SheetServiceBinding? = null
    private val binding get() = _binding!!

    var onConfirm: ((String, Long) -> Unit)? = null

    private val balanceCents: Long
        get() = requireArguments().getLong(ARG_BALANCE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        binding.inputContract.doAfterTextChanged {
            binding.textError.isVisible = false
        }
        binding.inputAmount.doAfterTextChanged {
            binding.textError.isVisible = false
        }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener { submit() }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun submit() {
        val contract = binding.inputContract.text
            ?.toString()
            .orEmpty()
            .trim()
            .uppercase()
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        val error = when {
            !contract.matches(Regex("^(LUZ|AGUA|NET|GAS)-[A-Z0-9]{3,12}$")) ->
                "Usa un código como LUZ-001, AGUA-002, NET-003 o GAS-004."
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> "El monto máximo es S/ 100,000.00."
            cents > balanceCents -> getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        if (error != null) {
            binding.textError.text = error
            binding.textError.isVisible = true
            return
        }

        onConfirm?.invoke(contract, cents!!)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BALANCE = "balance"
        private const val MAX_AMOUNT_CENTS = 10_000_000L

        fun newInstance(balanceCents: Long): ServiceSheet =
            ServiceSheet().apply {
                arguments = bundleOf(ARG_BALANCE to balanceCents)
            }
    }
}
```

### 11.12. `ui/dashboard/DashboardActivity.kt`

```kotlin
package com.uc1.nexopay.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.uc1.nexopay.NexoPayApp
import com.uc1.nexopay.R
import com.uc1.nexopay.data.remote.SocketStatus
import com.uc1.nexopay.databinding.ActivityDashboardBinding
import com.uc1.nexopay.ui.common.NexoViewModelFactory
import com.uc1.nexopay.ui.common.UiUtils
import com.uc1.nexopay.ui.login.LoginActivity
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val adapter = TransactionAdapter()
    private var lastError: String? = null

    private val viewModel: DashboardViewModel by viewModels {
        NexoViewModelFactory((application as NexoPayApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                bars.left + dp(20),
                bars.top,
                bars.right + dp(20),
                bars.bottom
            )
            insets
        }

        binding.recyclerTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerTransactions.adapter = adapter

        binding.buttonDeposit.setOnClickListener {
            showAmountSheet(AmountSheet.Mode.DEPOSIT)
        }
        binding.buttonWithdraw.setOnClickListener {
            showAmountSheet(AmountSheet.Mode.WITHDRAW)
        }
        binding.buttonTransfer.setOnClickListener { showTransferSheet() }
        binding.buttonService.setOnClickListener { showServiceSheet() }
        binding.buttonRefresh.setOnClickListener { viewModel.refresh() }
        binding.buttonLogout.setOnClickListener { confirmLogout() }

        observe()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        binding.textGreeting.text = getString(
            R.string.dashboard_greeting,
            state.owner.ifBlank { "Usuario" }
        )
        binding.textAccount.text = getString(
            R.string.dashboard_account,
            state.accountId
        )

        binding.textBalance.text =
            if (state.isLoading && state.balanceCents == 0L) {
                getString(R.string.dashboard_balance_loading)
            } else {
                UiUtils.formatCents(state.balanceCents)
            }

        adapter.submitList(state.transactions)

        binding.progressHistory.isVisible =
            state.isLoading && state.transactions.isEmpty()
        binding.layoutEmpty.isVisible =
            !state.isLoading && state.transactions.isEmpty()
        binding.recyclerTransactions.isVisible = state.transactions.isNotEmpty()

        val enabled = !state.isOperating
        binding.buttonDeposit.isEnabled = enabled
        binding.buttonWithdraw.isEnabled = enabled
        binding.buttonTransfer.isEnabled = enabled
        binding.buttonService.isEnabled = enabled
        binding.buttonRefresh.isEnabled = enabled

        renderStatus(state.socketStatus)

        state.error?.let { message ->
            if (message != lastError) {
                lastError = message
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        } ?: run {
            lastError = null
        }
    }

    private fun renderStatus(status: SocketStatus) {
        val textRes: Int
        val backgroundRes: Int
        val colorRes: Int

        when (status) {
            SocketStatus.CONNECTED -> {
                textRes = R.string.dashboard_status_online
                backgroundRes = R.drawable.bg_chip_online
                colorRes = R.color.nexo_success
            }
            SocketStatus.CONNECTING -> {
                textRes = R.string.dashboard_status_connecting
                backgroundRes = R.drawable.bg_chip_offline
                colorRes = R.color.nexo_warning
            }
            SocketStatus.DISCONNECTED -> {
                textRes = R.string.dashboard_status_offline
                backgroundRes = R.drawable.bg_chip_offline
                colorRes = R.color.nexo_chip_offline_text
            }
        }

        binding.textStatus.setBackgroundResource(backgroundRes)
        binding.textStatus.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.textStatus.text = "● ${getString(textRes)}"
    }

    private fun handleEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Message -> {
                Snackbar.make(binding.root, event.text, Snackbar.LENGTH_LONG).show()
            }
            DashboardEvent.SessionExpired -> navigateToLogin()
        }
    }

    private fun showAmountSheet(mode: AmountSheet.Mode) {
        val sheet = AmountSheet.newInstance(mode, viewModel.state.value.balanceCents)
        sheet.onConfirm = { amountCents ->
            if (mode == AmountSheet.Mode.DEPOSIT) {
                viewModel.deposit(amountCents)
            } else {
                viewModel.withdraw(amountCents)
            }
        }
        sheet.show(supportFragmentManager, "amount_sheet")
    }

    private fun showTransferSheet() {
        val state = viewModel.state.value
        val sheet = TransferSheet.newInstance(
            balanceCents = state.balanceCents,
            ownAccountId = state.accountId
        )
        sheet.onConfirm = { destination, amountCents ->
            viewModel.transfer(destination, amountCents)
        }
        sheet.show(supportFragmentManager, "transfer_sheet")
    }

    private fun showServiceSheet() {
        val sheet = ServiceSheet.newInstance(viewModel.state.value.balanceCents)
        sheet.onConfirm = { contract, amountCents ->
            viewModel.payService(contract, amountCents)
        }
        sheet.show(supportFragmentManager, "service_sheet")
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dashboard_logout_confirm)
            .setMessage(R.string.dashboard_logout_body)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.dashboard_logout) { _, _ ->
                viewModel.logout()
            }
            .show()
    }

    private fun navigateToLogin() {
        if (isFinishing || isDestroyed) return
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
```

> **Insets:** el root recibe padding por las barras del sistema. No hay contenido debajo de la status bar ni de la barra de gestos. Al añadir el padding horizontal de 20dp en código, evita duplicarlo en el XML.

> **`repeatOnLifecycle(STARTED)`:** las colecciones se cancelan al pasar a segundo plano y se reanudan al volver. Evita que un `Snackbar` o una lista se actualice cuando la Activity no es visible.

✅ **Verificación de la Fase G:**

1. Login con la tarjeta demo o escribiendo `10020001` / `Nexo#2026`.
2. El dashboard muestra saldo grande, cuenta, chip y 4 botones con iconos.
3. Los 4 bottom sheets validan monto, cuenta y contrato.
4. Un depósito aparece de inmediato en el historial (Flow de Room).
5. Ingresos en verde y egresos en rojo.
6. El historial se desplaza sin trabar la UI.
7. Al girar el teléfono, el estado sobrevive (ViewModel + `savedInstanceState` del formulario).
8. Logout pide confirmación y vuelve al login.

⚠️ **Errores típicos**

| Síntoma | Causa | Solución |
|---|---|---|
| `NullPointerException` en `binding` | Usar binding después de `onDestroyView` | Usa `_binding` nullable y `get()` solo en `onViewCreated` |
| El sheet no se expande | Falta `onStart` con `STATE_EXPANDED` | Copia el bloque de 11.7 |
| Saldo no cambia | No actualizas `balanceCents` con la respuesta | Revisa `runOperation` |
| Botones duplican operaciones | No se usa `Mutex` | Revisa `tryLock` |
| Historial no se actualiza | `ListAdapter` sin `submitList` | Llama `adapter.submitList(state.transactions)` |
| Texto blanco sobre fondo blanco | Tema o color mal aplicado | Usa `nexo_on_primary` dentro de la tarjeta |

---

## 12. FASE H — SEGURIDAD

**Requisito del Word:** capítulo de seguridad con protocolos y buenas prácticas. Esta fase consolida lo que ya implementaste y te da material listo para el informe y la defensa.

### 12.1. Modelo de amenazas (tabla para el informe)

| Amenaza | Vector | Control implementado | Dónde se evidencia |
|---|---|---|---|
| Robo de contraseña | Fuerza bruta / credenciales filtradas | Argon2id + bloqueo 5 intentos/15 min + rate limit | `passwords.js`, `auth.js` |
| Robo de JWT | Logs, URL, backup, malware | Access 15 min en memoria; refresh cifrado; ticket WS | `SessionManager`, `tokens.js` |
| Reutilización de refresh | Copia del dispositivo | Rotación + detección de reuso + revocación de familia | `rotateRefreshToken` |
| Doble cobro | Reintento por mala red | `Idempotency-Key` + tabla de idempotencia | `idempotency.js` |
| Manipulación de saldo | Cliente modificado | Saldo autoritativo en servidor; transacción SQLite | `operations.js`, `db.js` |
| Inyección SQL | Inputs maliciosos | Prepared statements + validación Zod | `db.js`, `validate.js` |
| Fuga de base local | Root / backup / extracción | SQLCipher + clave en Keystore + backup deshabilitado | `DatabaseProvider`, manifest |
| Espionaje de red | Wi-Fi pública / proxy | HTTPS/WSS + NSC sin cleartext en release | `network_security_config.xml` |
| Secuestro de WebSocket | URL con JWT filtrado | Ticket de un solo uso, 30 s, hash en servidor | `tokens.js`, `realtime.js` |
| Fuerza bruta distribuida | Muchas IPs | Rate limit por IP + bloqueo por cuenta | `app.js`, `auth.js` |
| Fuga por logs | `Log.d` con tokens | Logging BASIC sin headers/body; backend censura | `ApiFactory`, `app.js` |
| APK alterado | Repackaging | Firma release + R8 + verificación de firma | Fase J |

> **Frase para la defensa:** “El cliente no es confiable. El servidor valida todo, ejecuta las operaciones dentro de una transacción SQLite y decide el saldo. La app solo representa el estado que el servidor autoriza.”

### 12.2. Protocolos y estándares usados

| Capa | Protocolo / estándar | Uso |
|---|---|---|
| Transporte | TLS 1.2/1.3 (HTTPS/WSS) | Cifrado en tránsito en producción |
| Autenticación | OAuth 2.0 Bearer (JWT) | Access token en `Authorization` |
| Refresh | Rotación de tokens opacos | Renovación sin reenviar contraseña |
| Hash de contraseña | Argon2id | Almacenamiento irreversible |
| Hash de tokens | SHA-256 | El servidor no guarda refresh/ticket en claro |
| Cifrado local | AES-256-GCM + SQLCipher | Base e historial cifrados |
| Integridad | GCM tag de 128 bits | Detecta manipulación |
| API | REST/JSON + WebSocket | Operaciones y push |
| Validación | JSON Schema vía Zod | Tipos, rangos y formatos |
| Cabeceras | Helmet + HSTS | Endurecimiento HTTP |
| Errores | Envelope uniforme con `requestId` | No filtra stack traces |

> **No confundas:** JWT no cifra, solo firma. Por eso el access token va por TLS y nunca se guarda en texto plano. La contraseña nunca se mete en el JWT.

### 12.3. Network Security Config de producción (sin cleartext)

El archivo de `src/main` ya bloquea HTTP:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**Regla:** el APK que entregas para la demo debe ser `prod` y apuntar a `https://`/`wss://`. Los flavors `local` y `device` son solo para desarrollo.

#### Pinning opcional (no lo actives sin entenderlo)

Si el profesor pregunta por “certificate pinning”, puedes mostrarlo como mejora. **No lo dejes activo en la entrega** salvo que tengas un pin de respaldo y una fecha de expiración, porque Render rota certificados y puedes romper la app.

```xml
<!-- Ejemplo SOLO para producción y con un pin de respaldo real. -->
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">TU-BACKEND.onrender.com</domain>
    <pin-set expiration="2027-12-31">
        <pin digest="SHA-256">PIN_PRINCIPAL_BASE64=</pin>
        <pin digest="SHA-256">PIN_RESPALDO_BASE64=</pin>
    </pin-set>
</domain-config>
```

Para obtener el SPKI del servidor (Git Bash o WSL):

```bash
openssl s_client -servername TU-BACKEND.onrender.com \
  -connect TU-BACKEND.onrender.com:443 </dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

> **Riesgo:** si pinnas solo la hoja y Render cambia el certificado, la app deja de conectar. Un pin de respaldo (otra CA o clave) es obligatorio. Para este proyecto, la NSC sin cleartext + TLS ya es suficiente; el pinning es un extra que debes poder explicar.

### 12.4. Almacenamiento seguro en Android

| Dato | Dónde vive | Cifrado | Se borra al logout |
|---|---|---|---|
| Access token | Memoria (`SessionManager`) | N/A | Sí (proceso) |
| Refresh token | SharedPreferences privado | AES-256-GCM con clave Keystore | Sí |
| Passphrase SQLCipher | SharedPreferences privado | AES-256-GCM con clave Keystore | No (la BD sigue) |
| Historial | SQLCipher | Página completa | No (caché local) |
| Último número de cuenta | DataStore | No sensible | Sí (opcional) |
| Contraseña | Nunca se guarda | N/A | N/A |

> **¿Por qué el refresh sí y el access no?** El refresh permite reabrir la app sin escribir la contraseña; el access dura 15 minutos y no necesita persistir. Guardar menos secretos reduce el impacto de una extracción.

#### Verificar backup deshabilitado

```powershell
adb backup -f nexo.ab com.uc1.nexopay.debug
```

El archivo debe ser vacío o el sistema debe rechazar el backup. Si contiene la base o preferencias, revisa `allowBackup` y `dataExtractionRules`.

#### Verificar que no hay tokens en Logcat

```powershell
adb logcat -c
# usa la app: login, depósito, transferencia
adb logcat -d | Select-String -Pattern "Bearer|refreshToken|accessToken|Nexo#2026"
```

No debe haber coincidencias. Si aparecen, tienes un `Log.d` o el logger de OkHttp en `BODY`.

### 12.5. Seguridad del backend (resumen para el capítulo)

1. **Argon2id** con parámetros moderados y salt aleatorio; verificación en tiempo constante.
2. **JWT de acceso** firmado con secreto de 32+ bytes, `typ=access`, `sub=accountId`, expiración 15 min.
3. **Refresh opaco** de 48 bytes, guardado solo como SHA-256, rotación atómica, familia revocada ante reuso.
4. **Bloqueo de cuenta** tras 5 fallos en 15 min; el mensaje de error es genérico para no enumerar cuentas.
5. **Rate limit** por IP en login, operaciones y ticket; `429` con `Retry-After`.
6. **Idempotencia** por cuenta + operación + key; se guarda el hash del request y la respuesta; otra operación o body con la misma key devuelve conflicto.
7. **Transacciones SQLite** con `BEGIN IMMEDIATE`/`db.transaction()` para evitar doble gasto por concurrencia.
8. **Prepared statements** en todas las consultas; nunca concatenes SQL.
9. **Zod** valida longitudes, regex, enteros positivos y montos máximos antes de tocar la BD.
10. **Helmet** añade CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`; HSTS se activa detrás de HTTPS.
11. **CORS** restringido por lista blanca; una app Android no necesita CORS, pero el backend no queda abierto a cualquier origen.
12. **Errores uniformes** con `requestId`; en producción no se devuelve stack trace.
13. **Auditoría** de login, refresh, operaciones y bloqueos en `audit_log`.
14. **Secretos** en `.env`/Render, nunca en Git. `JWT_ACCESS_SECRET` se genera en Render.

> **No digas “usamos JWT, entonces es seguro”.** Explica la cadena completa: TLS + token corto + refresh rotativo + validación servidor + almacenamiento cifrado + auditoría.

### 12.6. Controles OWASP Mobile Top 10 (2024)

| Riesgo | Cómo lo cubres |
|---|---|
| M1 Credenciales inseguras | Keystore, sin secretos hardcodeados, refresh cifrado |
| M2 Seguridad de la cadena de suministro | Versiones fijadas, `npm audit`, dependencias oficiales |
| M3 Autenticación/Autorización | JWT corto, refresh rotativo, guards por cuenta |
| M4 Validación de entrada/salida | Zod en backend, validación en sheets, DTOs estrictos |
| M5 Comunicación | HTTPS/WSS, NSC, sin cleartext en release |
| M6 Privacidad | Minimización de datos, sin PII en logs, backup off |
| M7 Binarios | R8, firma release, ProGuard, sin debug en release |
| M8 Configuración | `allowBackup=false`, activities no exportadas, NSC |
| M9 Almacenamiento | SQLCipher + AES-GCM + Keystore |
| M10 Criptografía | AES-256-GCM, Argon2id, SHA-256, sin ECB/MD5 |

### 12.7. Pruebas de seguridad que debes ejecutar y capturar

| Prueba | Comando / acción | Resultado esperado |
|---|---|---|
| Login incorrecto | Postman con clave mala | `401 invalid_credentials` genérico |
| Bloqueo | 5 intentos fallidos | `423 account_locked` o `429` |
| Access vencido | `ACCESS_TTL_SECONDS=10` | Refresh automático y reintento único |
| Reuso de refresh | Repetir refresh viejo | `401` y familia revocada |
| Ticket reutilizado | Conectar dos veces | Cierre `4401` |
| Idempotencia | Misma operación, key y body | Misma respuesta, un solo movimiento |
| Conflicto de idempotencia | Misma key, otra operación o body | `409 idempotency_conflict` |
| Saldo insuficiente | Retiro > saldo | `422 insufficient_funds`, sin crash |
| Rate limit | Ráfaga de login | `429` |
| SQLCipher | `od` sobre `nexopay.db` | Sin `SQLite format 3` |
| Backup | `adb backup` | Vacío/rechazado |
| Cleartext | Compilar `prod` release contra HTTP | Bloqueado por NSC |
| Logs | `adb logcat` | Sin tokens ni contraseñas |

> **Evidencia:** guarda capturas de Postman con el código HTTP y del Logcat sin secretos. En el informe, una tabla con “prueba, entrada, salida esperada, salida obtenida” vale mucho.

### 12.8. Hardening del APK release

1. `isMinifyEnabled = true` y `isShrinkResources = true` ya están en `app/build.gradle.kts`.
2. Firma el APK con una keystore **fuera del repositorio** (Fase J).
3. No publiques `keystore.properties` ni la keystore. Añádelos a `.gitignore`.
4. Verifica que el APK release no sea debuggable:

```powershell
aapt dump badging app-prod-release.apk | Select-String "debuggable"
```

No debe mostrar `debuggable=true`.

5. Verifica la firma:

```powershell
apksigner verify --print-certs app-prod-release.apk
```

Debe mostrar tu certificado y `Verified using v2/v3 scheme`.

6. Instala el release en un celular real y repite el flujo completo contra Render.

### 12.9. Qué decir si te preguntan “¿qué falta?”

Respuesta honesta y técnica:

- **MFA/TOTP** para operaciones de alto valor.
- **Biometría** con `BiometricPrompt` para desbloquear el refresh token (no para reemplazar la contraseña).
- **Play Integrity API** para detectar dispositivos rooteados o apps alteradas.
- **Certificate pinning con rotación** y pins de respaldo.
- **Cifrado de la base con clave derivada de biometría** y re-cifrado al cambiar credencial.
- **Observabilidad** con métricas y alertas de intentos fallidos.
- **Rate limit distribuido** (Redis) si hay varias instancias.
- **Base de datos gestionada** (PostgreSQL) para producción real, en vez de SQLite efímero.
- **Rotación de secretos** y gestión con KMS/Vault.

> Decir “esto falta para producción, pero para el alcance académico está cubierto con X, Y, Z” transmite madurez. No improvises que ya tienes MFA si no lo implementaste.

✅ **Verificación de la Fase H:**

- Release bloquea HTTP.
- El backup no extrae datos.
- Logcat no muestra secretos.
- SQLCipher impide leer la BD con `sqlite3`.
- Refresh y ticket no se pueden reutilizar.
- El APK release está firmado y no es debuggable.

---

## 13. FASE I — PRUEBAS Y EVIDENCIAS

**Requisito del Word:** demostrar funcionamiento, persistencia, concurrencia y ausencia de ANR. Aquí tienes pruebas automatizadas y una matriz manual con evidencia.

### 13.1. Pruebas unitarias Android

#### `app/src/test/java/com/uc1/nexopay/ui/common/UiUtilsTest.kt`

```kotlin
package com.uc1.nexopay.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiUtilsTest {

    @Test
    fun `formatea centavos como soles`() {
        assertEquals("S/ 1,500.00", UiUtils.formatCents(150_000))
        assertEquals("S/ 25.50", UiUtils.formatCents(2_550))
        assertEquals("S/ 0.01", UiUtils.formatCents(1))
    }

    @Test
    fun `parsea punto decimal`() {
        assertEquals(2_550L, UiUtils.parseAmountToCents("25.50"))
        assertEquals(2_550L, UiUtils.parseAmountToCents("25,50"))
    }

    @Test
    fun `parsea separador de miles`() {
        assertEquals(125_050L, UiUtils.parseAmountToCents("1,250.50"))
    }

    @Test
    fun `rechaza montos invalidos`() {
        assertNull(UiUtils.parseAmountToCents(""))
        assertNull(UiUtils.parseAmountToCents("0"))
        assertNull(UiUtils.parseAmountToCents("-5"))
        assertNull(UiUtils.parseAmountToCents("abc"))
    }
}
```

> **Cuidado con la coma:** la app asume que una coma sola es decimal (`25,50`) y que coma + punto es separador de miles (`1,250.50`). Documenta esa decisión en el informe para que no parezca un bug.

#### `app/src/test/java/com/uc1/nexopay/ui/common/ErrorMapperTest.kt`

```kotlin
package com.uc1.nexopay.ui.common

import com.uc1.nexopay.data.repository.ApiException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    @Test
    fun `mapea saldo insuficiente`() {
        val error = ApiException(
            422,
            """{"error":{"code":"insufficient_funds","message":"Saldo insuficiente"}}"""
        )
        assertEquals(
            "Saldo insuficiente para esta operación.",
            ErrorMapper.humanMessage(error)
        )
    }

    @Test
    fun `mapea cuenta bloqueada`() {
        val error = ApiException(
            423,
            """{"error":{"code":"account_locked","message":"Cuenta bloqueada"}}"""
        )
        assertEquals(
            "Cuenta bloqueada por intentos fallidos. Espera 15 minutos.",
            ErrorMapper.humanMessage(error)
        )
    }

    @Test
    fun `mapea error de red`() {
        assertEquals(
            "Sin conexión. Revisa tu red.",
            ErrorMapper.humanMessage(IOException("timeout"))
        )
    }
}
```

Ejecuta:

```powershell
.\gradlew.bat testLocalDebugUnitTest
```

> El nombre de la tarea incluye el flavor (`local`). Si usas otro flavor, ejecuta `testDeviceDebugUnitTest` o `testProdDebugUnitTest`.

### 13.2. Prueba instrumentada de Room

#### `app/src/androidTest/java/com/uc1/nexopay/data/local/TransactionDaoTest.kt`

```kotlin
package com.uc1.nexopay.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertaReemplazaYOrdena() = runBlocking {
        val older = transaction(createdAt = 1_000L, amountCents = 1_000L)
        val newer = transaction(createdAt = 2_000L, amountCents = 2_000L)

        dao.upsertAll(listOf(older, newer))
        assertEquals(2, dao.count())

        val recent = dao.observeRecent().first()
        assertEquals(newer.id, recent.first().id)

        dao.upsert(older.copy(amountCents = 9_999L))
        assertEquals(2, dao.count())
        assertEquals(9_999L, dao.getRecent().last().amountCents)
    }

    @Test
    fun borraTodo() = runBlocking {
        dao.upsert(transaction(createdAt = 1_000L, amountCents = 1_000L))
        dao.deleteAll()
        assertEquals(0, dao.count())
    }

    private fun transaction(createdAt: Long, amountCents: Long) =
        TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = TransactionType.DEPOSIT,
            amountCents = amountCents,
            balanceAfterCents = amountCents,
            counterpartyAccountId = null,
            contractCode = null,
            serviceName = null,
            description = "Prueba",
            referenceId = null,
            createdAt = createdAt
        )
}
```

> **Por qué `inMemoryDatabaseBuilder`:** aísla la prueba y no toca la base SQLCipher real. La prueba de cifrado se hace manualmente con `od` (Fase C).

### 13.3. Prueba instrumentada de SecureStore

#### `app/src/androidTest/java/com/uc1/nexopay/security/SecureStoreTest.kt`

```kotlin
package com.uc1.nexopay.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SecureStoreTest {

    @Test
    fun cifraYDescifra() {
        val store = SecureStore(ApplicationProvider.getApplicationContext())
        val key = "test_${UUID.randomUUID()}"

        store.put(key, "Nexo#2026")
        assertEquals("Nexo#2026", store.get(key))

        store.remove(key)
        assertNull(store.get(key))
    }

    @Test
    fun generaBytesEstables() {
        val store = SecureStore(ApplicationProvider.getApplicationContext())
        val key = "bytes_${UUID.randomUUID()}"

        val first = store.getOrCreateBytes(key, 32)
        val second = store.getOrCreateBytes(key, 32)

        assertEquals(32, first.size)
        assertEquals(first.toList(), second.toList())
    }
}
```

Ejecuta:

```powershell
.\gradlew.bat connectedLocalDebugAndroidTest
```

> Necesitas un emulador o dispositivo conectado. La prueba de Keystore falla en un JVM puro porque no existe `AndroidKeyStore`; por eso es instrumentada.

### 13.4. Pruebas del backend

Ya están en la Fase A. Ejecuta:

```powershell
Set-Location C:\NexoPayBackend
npm test
```

Deben pasar autenticación, operaciones, idempotencia, saldo insuficiente, ticket de un solo uso y WebSocket.

### 13.5. Matriz manual de aceptación (copia al informe)

| # | Caso | Pasos | Resultado esperado | Evidencia |
|---|---|---|---|---|
| 1 | Login válido | Cuenta `10020001`, clave `Nexo#2026` | Dashboard con saldo `S/ 1,500.00` | `01-login.png` |
| 2 | Login inválido | Clave incorrecta | Mensaje “Cuenta o contraseña incorrectos”, sin crash | `02-login-error.png` |
| 3 | Depósito | Depositar `S/ 100.00` | Saldo sube a `S/ 1,600.00`; fila verde | `03-deposito.png` |
| 4 | Retiro válido | Retirar `S/ 50.00` | Saldo baja; fila roja | `04-retiro.png` |
| 5 | Retiro sin fondos | Retirar `S/ 9,999.00` | Snackbar “Saldo insuficiente”, sin crash | `05-sin-fondos.png` |
| 6 | Transferencia P2P | Ana transfiere `S/ 25.00` a Luis | Saldo de Ana baja; Luis recibe en vivo | `06-transferencia.gif` |
| 7 | Transferencia desde Postman | Luis transfiere a Ana mientras la app está abierta | App de Ana actualiza saldo sin recargar | `07-postman-ws.mp4` |
| 8 | Pago de servicio | Pagar `LUZ-001` por `S/ 30.00` | Fila “Servicio: Luz” con icono distinto | `08-servicio.png` |
| 9 | Historial persistente | Operar, `force-stop`, reabrir | Historial sigue visible | `09-persistencia.mp4` |
| 10 | Modo avión | Activar avión, intentar depósito | Chip “Desconectado”; error de red controlado | `10-offline.png` |
| 11 | Reconexión | Desactivar avión | Chip vuelve a “En línea” | `11-reconexion.mp4` |
| 12 | Doble toque | Tocar dos veces “Depositar” | Solo una operación | `12-idempotencia.mp4` |
| 13 | SQLCipher | `od` sobre la BD | Sin cabecera `SQLite format 3` | `13-sqlcipher.png` |
| 14 | Backup | `adb backup` | Vacío o rechazado | `14-backup.png` |
| 15 | Release | Instalar `prod-release` y operar contra Render | Todo funciona por HTTPS/WSS | `15-release.mp4` |
| 16 | Rotación | Girar el teléfono en dashboard | Estado y saldo se conservan | `16-rotacion.mp4` |
| 17 | Logout | Cerrar sesión y reabrir | Vuelve al login; no hay tokens | `17-logout.mp4` |
| 18 | Token vencido | `ACCESS_TTL_SECONDS=10`, esperar y operar | Refresh transparente | `18-refresh.mp4` |

> **Nombres de archivo:** úsalos también en el informe. Que el evaluador pueda relacionar la tabla con las capturas sin adivinar.

### 13.6. Cómo grabar evidencia sin arruinar la calidad

```powershell
# Video de la pantalla (máximo 3 minutos)
adb shell screenrecord --time-limit 180 /sdcard/nexopay-demo.mp4
adb pull /sdcard/nexopay-demo.mp4 .

# Captura PNG (usa Android Studio o scrcpy; evita redirección de PowerShell)
adb exec-out screencap -p > captura.png
```

> En PowerShell, `>` puede guardar UTF-16 y corromper el PNG. Usa **cmd.exe** para ese comando, o toma la captura desde Android Studio (**Logcat → Camera**), o usa `scrcpy` con `Ctrl+S`.

Para Logcat:

```powershell
adb logcat -c
adb logcat -d | Out-File -Encoding utf8 logcat-nexopay.txt
```

### 13.7. Prueba de ANR y concurrencia

1. Abre el dashboard con 50+ movimientos.
2. Mientras haces scroll, ejecuta un depósito desde Postman.
3. La lista debe actualizarse sin congelarse.
4. Revisa:

```powershell
adb logcat -d | Select-String -Pattern "ANR in|StrictMode policy violation"
```

No debe haber resultados. Si hay `StrictMode`, revisa que no estés llamando Room/Retrofit en Main.

5. En Android Studio, abre **Profiler → CPU** y graba 20 segundos mientras operas. El hilo `main` no debe quedarse bloqueado en `SocketInputStream` ni en `SQLiteConnection`.

> **Para la defensa:** muestra el Profiler o el Logcat limpio. No basta con decir “no hay ANR”; demuéstralo.

### 13.8. Checklist de pruebas antes de entregar

- [ ] `npm test` pasa en el backend.
- [ ] `.\gradlew.bat testLocalDebugUnitTest` pasa.
- [ ] `.\gradlew.bat connectedLocalDebugAndroidTest` pasa.
- [ ] Las 18 filas de la matriz tienen evidencia.
- [ ] El video de la defensa muestra Postman y la app al mismo tiempo.
- [ ] SQLCipher verificado con `od`.
- [ ] Backup deshabilitado verificado.
- [ ] Logcat sin tokens.
- [ ] Release firmado probado en celular real.

---

## 14. FASE J — APK, FIRMA, GITHUB Y CI

**Requisito del Word:** APK instalable y código en GitHub/GitLab. Aquí generas un debug para pruebas, un release firmado para la demo y un repositorio presentable.

### 14.1. Generar la keystore de release

Ejecuta en PowerShell (fuera de la carpeta del proyecto):

```powershell
New-Item -ItemType Directory -Force -Path "C:\Users\Usuario-PC\keystores"
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" `
  -genkeypair -v `
  -keystore "C:\Users\Usuario-PC\keystores\nexopay-release.jks" `
  -alias nexopay `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass "CAMBIA_ESTA_CLAVE" `
  -keypass "CAMBIA_ESTA_CLAVE" `
  -dname "CN=NexoPay, OU=UC1, O=Universidad, L=Lima, ST=Lima, C=PE"
```

> **No subas la keystore a GitHub.** Si la pierdes, no podrás actualizar el APK con la misma identidad. Guarda una copia en un gestor de contraseñas o unidad cifrada.

Crea `keystore.properties` en la **raíz del proyecto Android**:

```properties
storeFile=C:/Users/Usuario-PC/keystores/nexopay-release.jks
storePassword=CAMBIA_ESTA_CLAVE
keyAlias=nexopay
keyPassword=CAMBIA_ESTA_CLAVE
```

Añade a `.gitignore`:

```gitignore
keystore.properties
*.jks
*.keystore
```

### 14.2. Configurar la firma en `app/build.gradle.kts`

Añade los imports al inicio del archivo:

```kotlin
import java.io.FileInputStream
import java.util.Properties
```

Antes del bloque `android { ... }`:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
```

Dentro de `android { ... }`, antes de `buildTypes`:

```kotlin
signingConfigs {
    if (keystorePropertiesFile.exists()) {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}
```

Dentro de `buildTypes { release { ... } }`, agrega:

```kotlin
if (keystorePropertiesFile.exists()) {
    signingConfig = signingConfigs.getByName("release")
}
```

> **Si `keystore.properties` no existe**, el proyecto sigue compilando en debug; el release quedará sin firmar y Android Studio te pedirá firmarlo al generar el APK. Es intencional para no romper el build de un compañero.

### 14.3. Compilar los APK

```powershell
Set-Location C:\Users\Usuario-PC\AndroidStudioProjects\NexoPay

# Debug para emulador (backend local en 10.0.2.2)
.\gradlew.bat assembleLocalDebug

# Debug para celular real con adb reverse
.\gradlew.bat assembleDeviceDebug

# Release para Render (reemplaza TU-BACKEND antes)
.\gradlew.bat assembleProdRelease
```

Rutas:

```text
app/build/outputs/apk/local/debug/app-local-debug.apk
app/build/outputs/apk/device/debug/app-device-debug.apk
app/build/outputs/apk/prod/release/app-prod-release.apk
```

> **Antes del release:** abre `app/build.gradle.kts` y reemplaza `TU-BACKEND.onrender.com` por tu URL real en `prod`. Si no, el APK no conecta. Verifica también que el backend esté despierto.

### 14.4. Instalar y probar el APK

```powershell
adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk
```

Si falla por firma distinta:

```powershell
adb uninstall com.uc1.nexopay
adb install app\build\outputs\apk\prod\release\app-prod-release.apk
```

> **Paquete de debug:** `com.uc1.nexopay.debug` por `applicationIdSuffix`. El release usa `com.uc1.nexopay`. Puedes tener ambos instalados, pero no compartes datos.

Verifica el APK:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt.exe" dump badging `
  app\build\outputs\apk\prod\release\app-prod-release.apk | Select-String "package|debuggable"

& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs `
  app\build\outputs\apk\prod\release\app-prod-release.apk
```

Debe mostrar `package: name='com.uc1.nexopay'`, **sin** `debuggable=true`, y `Verified using v2/v3 scheme`.

### 14.5. Estructura de repositorios recomendada

Opción A (recomendada para el curso): **dos repositorios enlazados**.

```text
https://github.com/TU-USUARIO/nexopay-android
https://github.com/TU-USUARIO/nexopay-backend
```

Opción B: **monorepo**.

```text
nexopay/
├── android/        (proyecto Android)
├── backend/        (Fastify)
├── docs/
│   ├── informe.pdf
│   └── evidencias/
└── README.md
```

> El Word permite cualquiera. Si usas dos repos, el README de Android debe enlazar el backend y viceversa. Si usas monorepo, no mezcles `node_modules` ni `.gradle` en Git.

### 14.6. `.gitignore` del proyecto Android

```gitignore
# Android Studio
.idea/
*.iml
.gradle/
build/
local.properties
captures/
.externalNativeBuild/
.cxx/

# Secretos
keystore.properties
*.jks
*.keystore

# APK generados (sube solo el release final a Releases)
*.apk
*.aab

# SO
.DS_Store
Thumbs.db
```

> **Excepción:** si el profesor pide el APK en el repo, súbelo a **GitHub Releases**, no lo commitees. El release tiene un enlace estable y no infla el historial.

### 14.7. `.gitignore` del backend

```gitignore
node_modules/
.env
.env.*
!.env.example
data/
*.db
*.db-shm
*.db-wal
*.log
coverage/
.DS_Store
```

> **Nunca** subas `.env` ni `data/nexopay.db`. El seed crea las cuentas y el evaluador puede reproducir el entorno.

### 14.8. README del repositorio (plantilla)

````markdown
# NexoPay — Billetera móvil distribuida UC1

Aplicación Android (Kotlin, XML, Material 3, Room/SQLCipher, Retrofit, WebSocket)
y backend (Node.js 22, Fastify 5, SQLite) para operaciones de billetera en tiempo real.

## Demo
- Video: [enlace]
- APK release: [GitHub Releases]
- Backend: [Render URL]

## Cuentas de prueba
| Cuenta | Dueño | Clave |
|---|---|---|
| 10020001 | Ana Torres | Nexo#2026 |
| 10020002 | Luis Rojas | Nexo#2026 |

## Arquitectura
Android → HTTPS/REST → Fastify → SQLite
Android ← WSS (ticket de un solo uso) ← Fastify

## Funcionalidades
- Login con JWT corto + refresh rotativo
- Depósito, retiro, transferencia P2P y pago de servicios
- Historial local cifrado con SQLCipher y reactivo con Room/Flow
- Actualización de saldo en tiempo real por WebSocket
- Seguridad: Argon2id, idempotencia, rate limit, tickets WS, Keystore

## Backend
```bash
cd backend
npm ci
cp .env.example .env
npm run seed
npm start
```

## Android
1. Abre `android/` en Android Studio.
2. Configura `prod` con tu URL de Render.
3. Ejecuta `assembleProdRelease` o instala el APK de Releases.

## Pruebas
```bash
npm test
./gradlew testLocalDebugUnitTest
```

## Seguridad
Ver `docs/seguridad.md` y el capítulo 6 del informe.

## Autor
Nombre — Curso — Fecha
````

> **README evaluable:** debe permitir que otra persona ejecute el proyecto en menos de 10 minutos. Si tu README no tiene URL del backend y credenciales, pierdes presentación.

### 14.9. Publicar en GitHub

```powershell
Set-Location C:\Users\Usuario-PC\AndroidStudioProjects\NexoPay

git init
git add .
git status
git commit -m "feat: NexoPay Android con Room, REST y WebSocket"
git branch -M main
git remote add origin https://github.com/TU-USUARIO/nexopay-android.git
git push -u origin main
```

Backend:

```powershell
Set-Location C:\NexoPayBackend

git init
git add .
git status
git commit -m "feat: backend Fastify con auth, operaciones y WebSocket"
git branch -M main
git remote add origin https://github.com/TU-USUARIO/nexopay-backend.git
git push -u origin main
```

> **Antes de commitear:** ejecuta `git status` y confirma que no aparecen `.env`, `keystore.properties`, `.jks`, `node_modules`, `data/` ni APK. Si aparecen, corrige `.gitignore` y `git rm --cached`.

### 14.10. Release en GitHub

1. Ve a **Releases → Draft a new release**.
2. Tag: `v1.0.0`.
3. Título: `NexoPay UC1 — entrega final`.
4. Adjunta `app-prod-release.apk`.
5. Pega el SHA-256:

```powershell
Get-FileHash app\build\outputs\apk\prod\release\app-prod-release.apk -Algorithm SHA256
```

6. Publica. Copia el enlace en el informe y en el README.

> **Nota:** el APK release apunta a Render. Si Render está dormido, la primera operación tarda. Abre `/health` antes de la defensa.

### 14.11. GitHub Actions para Android

Crea `.github/workflows/android.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: android-actions/setup-android@v3

      - uses: gradle/actions/setup-gradle@v4

      - name: Make Gradle wrapper executable
        run: chmod +x ./gradlew

      - name: Unit tests
        run: ./gradlew testLocalDebugUnitTest

      - name: Build debug APK
        run: ./gradlew assembleLocalDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: nexopay-local-debug
          path: app/build/outputs/apk/local/debug/*.apk
```

> **Sin secretos:** este workflow no firma release. Para firmar en CI necesitarías guardar la keystore en Secrets, lo cual no es necesario para el curso. El release lo generas localmente.

### 14.12. GitHub Actions para el backend

Crea `.github/workflows/backend.yml`:

```yaml
name: Backend CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm

      - run: npm ci
      - run: npm test
        env:
          NODE_ENV: test
          JWT_ACCESS_SECRET: test-secret-with-at-least-32-characters
```

> **`npm ci`** requiere `package-lock.json`. Commitéalo. Si no lo tienes, ejecuta `npm install` y vuelve a intentar.

### 14.13. Checklist de entrega

- [ ] APK release firmado e instalado en celular real.
- [ ] APK apunta a Render por HTTPS/WSS.
- [ ] Repositorios públicos con README completo.
- [ ] Release `v1.0.0` con APK y SHA-256.
- [ ] CI en verde.
- [ ] Sin secretos en Git (`git log --all --full-history -- .env` no devuelve nada).
- [ ] Informe en PDF con enlaces y capturas.
- [ ] Colección Postman exportada.

---

## 15. FASE K — INFORME APA 7

**Requisito del Word:** informe técnico con arquitectura, seguridad, pruebas y conclusiones. Esta estructura está pensada para 18–25 páginas y se puede adaptar.

### 15.1. Portada

```text
UNIVERSIDAD [NOMBRE]
FACULTAD DE [FACULTAD]
CARRERA DE [CARRERA]

“NexoPay: billetera móvil distribuida con actualización de saldo en tiempo real”

TRABAJO FINAL DE LA UNIDAD DIDÁCTICA
SISTEMAS DISTRIBUIDOS — UC1

Autor: [Nombres y apellidos]
Código: [Código]
Docente: [Nombre del docente]
Sección: [Sección]
Lima, Perú
2026
```

> **Formato APA 7:** Times New Roman 12 pt, interlineado 1.5 o doble según tu facultad, márgenes de 2.54 cm, sangría de primera línea de 1.27 cm, numeración arriba a la derecha. Revisa la plantilla oficial de tu universidad; algunas piden Arial 11.

### 15.2. Resumen

Ejemplo para adaptar (150–250 palabras):

> El presente trabajo implementa NexoPay, una billetera móvil distribuida compuesta por una aplicación Android en Kotlin con interfaz XML y Material 3, y un backend en Node.js con Fastify y SQLite. La aplicación permite autenticarse, consultar saldo, realizar depósitos, retiros, transferencias entre cuentas y pagos de servicios básicos. La arquitectura separa la interfaz, la lógica de presentación, el repositorio y las fuentes de datos; las operaciones de red y base de datos se ejecutan con corrutinas en hilos de entrada/salida, mientras la interfaz se actualiza desde el hilo principal mediante `StateFlow`. El historial se persiste en Room cifrado con SQLCipher y se sincroniza con el servidor. Las transferencias se notifican por WebSocket autenticado con tickets de un solo uso, lo que permite actualizar el saldo sin recargar la pantalla. En seguridad se aplican Argon2id para contraseñas, JWT de acceso de corta duración, refresh tokens rotativos con revocación por reutilización, idempotencia en operaciones monetarias, límites de tasa, validación de entradas y almacenamiento de secretos en Android Keystore. Las pruebas automatizadas y manuales verificaron los flujos principales, la persistencia tras el reinicio, la reconexión del socket y la ausencia de bloqueos en el hilo principal. Se concluye que el diseño cumple los requisitos de concurrencia, persistencia y comunicación en tiempo real, y se identifican mejoras para un entorno productivo.

**Palabras clave:** sistemas distribuidos, Android, Kotlin, Fastify, SQLite, WebSocket, Room, seguridad, corrutinas.

### 15.3. Índice

Genera el índice automático en Word (**Referencias → Tabla de contenido**). Incluye:

1. Introducción
2. Planteamiento del problema
3. Objetivos
4. Marco teórico
5. Metodología
6. Desarrollo de la solución
7. Seguridad
8. Pruebas y resultados
9. Conclusiones
10. Recomendaciones
11. Referencias
12. Anexos

> Actualiza el índice al final. Un índice con números de página desactualizados baja presentación.

### 15.4. Introducción

Incluye:

- Contexto: crecimiento de pagos móviles y necesidad de consistencia de saldo.
- Problema: aplicaciones que dependen de recargas manuales, doble gasto, historial que se pierde, ANR.
- Propósito: construir una billetera distribuida con actualización en tiempo real y persistencia local.
- Alcance: dos cuentas demo, cuatro operaciones, un backend, un cliente Android.
- Limitaciones: SQLite en Render free es efímero; no se integra pasarela de pago real; no hay MFA.

> **No empieces con “En la actualidad, la tecnología avanza…”.** Empieza con el problema concreto: “Una billetera móvil debe garantizar que el saldo mostrado coincida con el saldo autoritativo del servidor, incluso si la red falla o el usuario reintenta una operación.”

### 15.5. Planteamiento del problema

Preguntas de investigación sugeridas:

1. ¿Cómo garantizar que el saldo no se duplique cuando el usuario reintenta una operación por mala conectividad?
2. ¿Cómo actualizar el saldo de una cuenta sin que el usuario recargue la pantalla?
3. ¿Cómo persistir el historial localmente sin exponerlo si el dispositivo se compromete?
4. ¿Cómo mantener la interfaz fluida mientras se ejecutan operaciones de red y base de datos?

> Formula una pregunta general y tres específicas. Cada una debe responderse en resultados/conclusiones.

### 15.6. Objetivos

**Objetivo general**

Implementar una billetera móvil distribuida que permita operar depósitos, retiros, transferencias y pagos de servicios, con actualización de saldo en tiempo real, persistencia local cifrada y comunicación segura.

**Objetivos específicos**

1. Diseñar una API REST con autenticación JWT y refresh rotativo.
2. Implementar las cuatro operaciones monetarias con validación e idempotencia.
3. Persistir el historial en Room con SQLCipher y demostrar su supervivencia al reinicio.
4. Implementar un canal WebSocket con tickets de un solo uso para notificar transferencias.
5. Aplicar corrutinas y `StateFlow` para evitar ANR.
6. Verificar los flujos con pruebas automatizadas y manuales.

### 15.7. Marco teórico

Subsecciones sugeridas y qué citar:

#### 15.7.1. Sistemas distribuidos y arquitectura cliente-servidor

Explica nodos, comunicación por red, fallos parciales, consistencia y tolerancia a fallos. Cita a Tanenbaum y van Steen (2017) y a Sommerville (2016). Relaciona: el backend es la autoridad; el cliente es una caché/representación.

#### 15.7.2. Arquitectura REST

Define recursos, métodos, statelessness, códigos HTTP. Cita a Fielding (2000). Explica por qué las operaciones son `POST` y las consultas `GET`.

#### 15.7.3. WebSocket y comunicación full-duplex

Explica handshake HTTP, frames, ping/pong, cierre. Cita RFC 6455 (Fette & Melnikov, 2011). Justifica WebSocket frente a polling: menor latencia y menos requests.

#### 15.7.4. Persistencia local en Android

Room como abstracción sobre SQLite, entidades, DAO, Flow. Cita Android Developers (2026a). Explica que SQLite es transaccional y que Room verifica consultas en compilación.

#### 15.7.5. Concurrencia y corrutinas

Hilos, dispatchers, structured concurrency, cancelación, `viewModelScope`. Cita Kotlin (2026). Explica por qué IO para red/BD y Main para UI.

#### 15.7.6. Seguridad de aplicaciones móviles

Autenticación, autorización, almacenamiento seguro, transporte, OWASP Mobile Top 10 (2024), NIST SP 800-63B (2020). Explica Argon2id (RFC 9106), JWT (RFC 7519), OAuth 2.0 (RFC 6749).

#### 15.7.7. Dinero y precisión

Explica por qué no usar `Double`: representación binaria, errores de redondeo. Usa enteros en centavos y `BigDecimal` solo para formateo/parseo.

### 15.8. Metodología

Sugerencia: **investigación aplicada + desarrollo incremental + pruebas**.

Fases:

1. Análisis de requisitos del Word.
2. Diseño de arquitectura y contrato de API.
3. Implementación del backend y pruebas con Postman.
4. Implementación del cliente Android por capas.
5. Integración REST + WebSocket + Room.
6. Pruebas unitarias, instrumentadas y manuales.
7. Documentación y defensa.

Instrumentos:

- Matriz de trazabilidad requisito → implementación → evidencia.
- Postman para API.
- Logcat, Profiler y StrictMode para concurrencia.
- `od` y `adb backup` para verificar cifrado/backup.
- Checklist de 18 casos.

> **Población/muestra:** no aplica un experimento social. Habla de “un entorno de pruebas compuesto por un emulador Pixel 6 API 34 y un celular Android físico, contra un backend local y Render”.

### 15.9. Desarrollo de la solución

#### 15.9.1. Arquitectura general

Incluye el diagrama de la sección 4.1 y explica cada componente:

- **Cliente Android:** Activities, ViewModels, Repository, Retrofit/OkHttp, Room/SQLCipher, WebSocket.
- **Backend:** Fastify, rutas, servicios, SQLite, hub WebSocket.
- **Comunicación:** REST/JSON sobre HTTPS y WSS.
- **Autoridad:** el saldo vive en SQLite del servidor; el cliente nunca lo decide.

#### 15.9.2. Modelo de datos

Tabla `accounts`, `transactions`, `refresh_tokens`, `idempotency_keys`, `ws_tickets`, `audit_log`. Explica PK, FK, índices y WAL. Incluye el DDL de la Fase A.

#### 15.9.3. Contrato de API

Tabla de endpoints (sección 4.5). Incluye un ejemplo de request/response de transferencia y el envelope de error.

#### 15.9.4. Flujo de una transferencia

1. La app envía `POST /transfer` con Bearer e `Idempotency-Key`.
2. Fastify valida y abre transacción.
3. Descuenta origen, acredita destino, inserta dos movimientos.
4. Responde al emisor.
5. El hub envía `transfer.received` al destino.
6. La app del destino guarda en Room y actualiza el saldo.

#### 15.9.5. Persistencia y sincronización

Explica que Room es caché local del historial y el backend la fuente de verdad. Al iniciar sesión se sincroniza; los eventos WS insertan/reemplazan por UUID. El historial sobrevive al reinicio y al modo avión.

#### 15.9.6. Concurrencia

Explica `Dispatchers.IO`, `viewModelScope`, `StateFlow`, `Mutex`, `repeatOnLifecycle`. Incluye un párrafo sobre por qué no hay ANR y cómo se probó.

#### 15.9.7. Interfaz

Capturas del login, dashboard, sheets e historial. Explica Material 3, ViewBinding, RecyclerView/ListAdapter y accesibilidad.

### 15.10. Seguridad

Usa la tabla de la Fase H. Subsecciones:

1. Autenticación y sesión.
2. Autorización por cuenta.
3. Protección de datos en tránsito.
4. Protección de datos en reposo.
5. Integridad de operaciones (idempotencia, transacciones).
6. Prevención de abuso (rate limit, bloqueo).
7. Registro y auditoría.
8. Limitaciones y mejoras.

> **No pegues solo la tabla.** Explica el “por qué” de cada control y qué amenaza mitiga. El profesor evalúa criterio, no memorización.

### 15.11. Pruebas y resultados

Incluye:

- Entorno: emulador Pixel 6 API 34, celular [modelo], Android [versión], backend local/Render.
- Pruebas automatizadas: `npm test` (N pruebas), unitarias Android, instrumentadas.
- Tabla de 18 casos con resultado “OK” y evidencia.
- Métricas: tiempo de login, tiempo de transferencia, latencia WS, uso de memoria.
- Incidentes y correcciones: por ejemplo, “el primer diseño guardaba el JWT en query; se cambió a ticket por seguridad”.

Tabla resumen:

| Métrica | Resultado |
|---|---|
| Login (Render despierto) | ~0.8 s |
| Transferencia REST | ~0.4 s |
| Evento WS recibido | < 1 s |
| ANR | 0 |
| Casos aprobados | 18/18 |
| Cobertura backend | [tu %] |

> Ajusta los números a tus mediciones reales. No inventes métricas: si no mediste, escribe “observación cualitativa” o mide antes de entregar.

### 15.12. Conclusiones

Ejemplo de párrafos:

> La implementación demostró que es posible construir una billetera móvil con actualización de saldo en tiempo real usando una arquitectura cliente-servidor simple, siempre que el servidor conserve la autoridad sobre el saldo y las operaciones se ejecuten dentro de transacciones. La incorporación de idempotencia evitó duplicados ante reintentos, y el ticket de un solo uso eliminó la exposición del JWT en la URL del WebSocket.

> Room con SQLCipher permitió que el historial sobreviviera al reinicio y al modo avión sin exponer los datos en texto plano. El uso de corrutinas y `StateFlow` mantuvo la interfaz fluida; no se registraron ANR durante las pruebas.

> La separación por capas facilitó las pruebas y el mantenimiento. Sin embargo, SQLite en un servicio gratuito con disco efímero no es adecuado para producción; se recomienda migrar a PostgreSQL y añadir respaldo, MFA y observabilidad.

> **Cada conclusión debe responder a un objetivo específico.** No repitas el resumen.

### 15.13. Recomendaciones

- Migrar a PostgreSQL y usar migraciones versionadas.
- Implementar MFA/TOTP y biometría.
- Añadir Play Integrity y certificate pinning con rotación.
- Incorporar un gateway de pagos real y conciliación.
- Añadir observabilidad (métricas, trazas, alertas).
- Internacionalizar la UI y accesibilidad avanzada.
- Añadir pruebas de carga con k6 o Artillery.
- Documentar el threat model con STRIDE.

### 15.14. Referencias (APA 7)

Orden alfabético. Ajusta autores y fechas según las fuentes que realmente consultes.

```text
Android Developers. (2026a). Save data in a local database using Room.
https://developer.android.com/training/data-storage/room

Android Developers. (2026b). Android Keystore system.
https://developer.android.com/privacy-and-security/keystore

Biryukov, A., Dinu, D., Khovratovich, D., & Josefsson, S. (2021).
Argon2 password hashing (RFC 9106). Internet Engineering Task Force.
https://doi.org/10.17487/RFC9106

Fastify. (2026). Fastify documentation. https://fastify.dev/docs/latest/

Fette, I., & Melnikov, A. (2011). The WebSocket protocol (RFC 6455).
Internet Engineering Task Force. https://doi.org/10.17487/RFC6455

Fielding, R. T. (2000). Architectural styles and the design of
network-based software architectures [Tesis doctoral, University of
California, Irvine]. https://www.ics.uci.edu/~fielding/pubs/dissertation/

Hardt, D. (2012). The OAuth 2.0 authorization framework (RFC 6749).
Internet Engineering Task Force. https://doi.org/10.17487/RFC6749

Jones, M., Bradley, J., & Sakimura, N. (2015). JSON Web Token (JWT)
(RFC 7519). Internet Engineering Task Force.
https://doi.org/10.17487/RFC7519

Kotlin. (2026). Coroutines guide. https://kotlinlang.org/docs/coroutines-guide.html

Material Design. (2026). Material 3. https://m3.material.io/

National Institute of Standards and Technology. (2020). Digital identity
guidelines: Authentication and lifecycle management (NIST SP 800-63B).
https://doi.org/10.6028/NIST.SP.800-63b

Node.js. (2026). Node.js v22 documentation.
https://nodejs.org/docs/latest-v22.x/api/

OWASP Foundation. (2024). OWASP Mobile Top 10.
https://owasp.org/www-project-mobile-top-10/

Sommerville, I. (2016). Software engineering (10.ª ed.). Pearson.

SQLite. (2026). Write-ahead logging. https://www.sqlite.org/wal.html

Tanenbaum, A. S., & van Steen, M. (2017). Distributed systems
(3.ª ed.). Pearson.

Zetetic. (2026). SQLCipher for Android.
https://www.zetetic.net/sqlcipher/sqlcipher-for-android/
```

> **Citas en el texto:** (Android Developers, 2026a), (Fette & Melnikov, 2011), (OWASP Foundation, 2024). No cites una fuente que no leíste. Si usas una guía o documentación, cítala como fuente web.

### 15.15. Anexos

- Anexo A: Colección Postman exportada.
- Anexo B: Capturas de pantalla (login, dashboard, sheets, historial).
- Anexo C: Resultados de pruebas (`npm test`, Gradle, Logcat).
- Anexo D: Fragmentos de código clave (opcional).
- Anexo E: Enlaces a repositorios, APK y video.

> **Paginación:** cada anexo empieza en página nueva. Numera las figuras y tablas (“Figura 1. Arquitectura…”, “Tabla 3. Casos de prueba…”). Cita cada figura en el texto.

### 15.16. Declaración de uso de herramientas de IA (opcional)

Si tu universidad lo permite y lo exige, agrega una declaración honesta:

> “Se utilizaron herramientas de asistencia basadas en modelos de lenguaje para revisar redacción y detectar errores. El diseño, la implementación, las pruebas y las conclusiones fueron verificados por el autor. Las fuentes se citan en la sección de referencias.”

> Si tu facultad prohíbe estas herramientas, no las uses para redactar el informe. Pregunta a tu docente antes.

### 15.17. Checklist del informe

- [ ] Portada con datos correctos.
- [ ] Resumen y palabras clave.
- [ ] Índice actualizado.
- [ ] Objetivos medibles.
- [ ] Arquitectura explicada con diagrama propio.
- [ ] Tabla de endpoints.
- [ ] Capítulo de seguridad con amenazas y controles.
- [ ] Resultados con evidencia y métricas reales.
- [ ] Conclusiones que responden a los objetivos.
- [ ] Referencias APA 7 con sangría francesa.
- [ ] Anexos con enlaces.
- [ ] PDF revisado (sin “TODO”, sin capturas borrosas).

---

## 16. FASE L — GUION DE DEFENSA (29-10-2026)

**Objetivo:** en 5–7 minutos demostrar arquitectura, las 4 operaciones, persistencia, WebSocket en vivo y seguridad. No leas diapositivas: muestra el sistema.

### 16.1. Montaje de la demo (15 minutos antes)

1. Backend local o Render despierto:

```powershell
Invoke-RestMethod http://localhost:3000/health
# o
Invoke-RestMethod https://TU-BACKEND.onrender.com/health
```

2. Si usas celular real con backend local:

```powershell
adb reverse tcp:3000 tcp:3000
adb devices
```

3. App de **Ana** (`10020001`) abierta en el celular/emulador, con chip **En línea**.
4. Postman abierto con la colección y el entorno. Login de **Luis** (`10020002`) ya ejecutado; `accessTokenLuis` guardado.
5. Segunda ventana: Postman WebSocket o terminal para el ticket.
6. GitHub, informe PDF y video de respaldo en pestañas.
7. Silencia notificaciones. Cierra apps que consuman CPU. Graba la sesión por si falla Zoom.

> **Regla:** nunca demuestres con datos reales. Usa las cuentas demo y montos pequeños (`S/ 5.00`, `S/ 10.00`).

### 16.2. Guion minuto a minuto

#### 0:00–0:40 — Presentación y arquitectura

> “Buenos días. Soy [nombre]. Presento NexoPay, una billetera móvil distribuida. El cliente está hecho en Android con Kotlin, XML y Material 3. El backend está en Node.js con Fastify y SQLite. La app consume REST para las operaciones y un WebSocket para recibir transferencias en tiempo real. El servidor es la autoridad del saldo; el cliente solo representa el estado autorizado y guarda el historial local cifrado con Room y SQLCipher.”

Muestra el diagrama del informe o la app. No te detengas en cada flecha.

#### 0:40–1:30 — Login y seguridad de sesión

- Abre la app si no está abierta.
- Inicia sesión como Ana.
- Menciona:

> “El login valida Argon2id en el servidor. El access token dura 15 minutos y vive solo en memoria. El refresh token se guarda cifrado con una clave de Android Keystore. Si el access vence, OkHttp hace un refresh automático y reintenta una sola vez.”

No muestres tokens en pantalla.

#### 1:30–2:10 — Dashboard

Señala:

- Saldo grande.
- Chip **En línea**.
- Cuatro botones con iconos.
- Historial.

> “El saldo viene del backend. El chip refleja el estado del WebSocket. El historial viene de Room y se actualiza con un Flow; por eso la lista reacciona sola.”

#### 2:10–2:50 — Depósito

- Toca **Depositar**.
- Ingresa `10.00`.
- Confirma.
- Muestra el saldo actualizado y la fila verde.

> “El monto se envía en centavos enteros, nunca en Double. La operación lleva un Idempotency-Key; si se reintenta con la misma key, el servidor devuelve la misma respuesta y no duplica el movimiento.”

#### 2:50–3:30 — Transferencia desde la app

- Toca **Transferir**.
- Cuenta destino: `10020002`.
- Monto: `5.00`.
- Confirma.
- Muestra el saldo de Ana y la fila roja.

> “El backend abre una transacción SQLite, descuenta a Ana, abona a Luis e inserta los dos movimientos. Si algo falla, hace rollback.”

#### 3:30–4:30 — WebSocket en vivo (el momento clave)

- Deja la app de Ana visible.
- En Postman, envía la transferencia de Luis a Ana:

```http
POST https://TU-BACKEND.onrender.com/api/v1/operations/transfer
Authorization: Bearer {{accessTokenLuis}}
Idempotency-Key: {{$guid}}
Content-Type: application/json

{
  "toAccountId": "10020001",
  "amountCents": 500
}
```

- La app debe subir el saldo y mostrar “Transferencia recibida” **sin recargar**.
- Señala el chip y el Snackbar.

> “Luis envía por REST. El servidor guarda la operación y publica `transfer.received` por el hub. La app de Ana ya tenía un WebSocket autenticado con un ticket de un solo uso. Al recibir el evento, guarda en Room y actualiza el StateFlow. No hay polling ni recarga.”

> **Si el evento no llega:** revisa el chip. Si está rojo, muestra la reconexión; si está verde, revisa que estés logueado como Ana y que el `toAccountId` sea `10020001`. Ten un video de respaldo.

#### 4:30–5:15 — Persistencia y modo avión

- Cierra la app por completo (no solo la pantalla).
- Vuelve a abrirla.
- Muestra que el historial sigue.
- Activa modo avión.
- Intenta un depósito: muestra el error controlado y el chip desconectado.
- Desactiva modo avión: el chip vuelve a verde.

> “Room con SQLCipher guarda el historial en el dispositivo. Al reabrir, la lista aparece antes de que termine la red. En modo avión, la app no crashea: informa el error y mantiene el historial. Al volver la red, el WebSocket pide un ticket nuevo y se reconecta.”

#### 5:15–6:00 — Evidencia de seguridad

Muestra, sin exponer secretos:

- `od` sobre la base: sin `SQLite format 3`.
- `adb backup`: vacío/rechazado.
- Logcat filtrado sin `Bearer`.
- `aapt dump badging`: sin `debuggable=true`.
- Postman: ticket reutilizado → cierre `4401`.

> “El archivo de base está cifrado. La clave está en Keystore. El backup está deshabilitado. El ticket del WebSocket es de un solo uso y dura 30 segundos. El APK release está firmado y no es debuggable.”

#### 6:00–6:40 — Pruebas, repositorio e informe

- Muestra GitHub Actions en verde o `npm test` corriendo.
- Muestra el release con el APK.
- Muestra el informe PDF y la matriz de 18 casos.

> “El backend tiene pruebas de autenticación, idempotencia, saldo insuficiente, ticket de un solo uso y WebSocket. La app tiene pruebas unitarias de formato/errores e instrumentadas de Room y Keystore. El informe documenta arquitectura, seguridad, resultados y limitaciones.”

#### 6:40–7:00 — Cierre

> “NexoPay cumple los requisitos de cliente Android, Room, REST, WebSocket, corrutinas y seguridad. Como trabajo futuro migraría a PostgreSQL, añadiría MFA y observabilidad. Gracias; estoy listo para preguntas.”

### 16.3. Preguntas frecuentes y respuestas cortas

| Pregunta | Respuesta sugerida |
|---|---|
| ¿Por qué Fastify y no Express? | Mejor rendimiento, validación/esquemas, plugins encapsulados y manejo de errores consistente. |
| ¿Por qué XML y no Compose? | El Word permite ambos. XML + ViewBinding + Material 3 es estable, explícito y demuestra manejo de layouts, RecyclerView y lifecycle. |
| ¿Por qué SQLite? | Es transaccional, zero-config y suficiente para una demo de un nodo. En producción usaría PostgreSQL. |
| ¿Cómo evitas el doble gasto? | Toda la operación corre dentro de una transacción; el saldo se lee y actualiza en la misma transacción; idempotencia por key. |
| ¿Qué pasa con dos transferencias simultáneas? | SQLite serializa escrituras; `busy_timeout` y `BEGIN IMMEDIATE` evitan carreras. En producción, bloqueo a nivel de fila en PostgreSQL. |
| ¿Por qué JWT corto? | Reduce la ventana de abuso si se roba. El refresh rotativo permite sesiones largas sin guardar la contraseña. |
| ¿Dónde guardas el JWT? | El access token en memoria; el refresh cifrado con Keystore. Nunca en `SharedPreferences` plano ni en la URL. |
| ¿Por qué un ticket WS? | La URL puede quedar en logs; un ticket de 30 s y un solo uso no sirve después. El servidor guarda solo su hash. |
| ¿Cómo reconecta el WebSocket? | Backoff exponencial con jitter, pide ticket nuevo y vuelve a conectar mientras la sesión exista. |
| ¿Cómo evitas ANR? | Red/BD en IO, sin `runBlocking` en Main, ViewModel + StateFlow, RecyclerView con ListAdapter. |
| ¿Room no duplica la verdad? | Room es caché del historial; el backend es la autoridad del saldo. Los UUID y `REPLACE` evitan duplicados. |
| ¿Qué pasa si se cae la red después de enviar? | El servidor es idempotente. El cliente debe reintentar con la misma key; la app deshabilita el doble toque. |
| ¿Qué pasa si roban el refresh? | El servidor rota el token; si el atacante lo reutiliza, se revoca la familia completa. |
| ¿Por qué Argon2id y no SHA-256? | SHA-256 es rápido y permite fuerza bruta; Argon2id es memory-hard y resistente a GPU/ASIC. |
| ¿Qué pasa si extraen la base? | Está cifrada con SQLCipher; la clave está en Keystore y no se exporta. |
| ¿Cómo probaste SQLCipher? | `od` sobre `nexopay.db`: no aparece `SQLite format 3`; además el archivo no abre con `sqlite3`. |
| ¿Por qué `10.0.2.2`? | Es el alias del host desde el emulador Android. `127.0.0.1` dentro del emulador es el propio emulador. |
| ¿Por qué `adb reverse`? | Permite que el celular real use `127.0.0.1:3000` y llegue al backend de la PC por USB. |
| ¿Render free se duerme? | Sí; abro `/health` antes de la demo y tengo plan B local. |
| ¿Cómo escalarías? | PostgreSQL, Redis para rate limit/idempotencia, varias instancias detrás de un balanceador, WebSocket con pub/sub. |
| ¿Qué falta para producción? | MFA, biometría, Play Integrity, pinning con rotación, observabilidad, respaldo y gestión de secretos. |
| ¿Qué fue lo más difícil? | La consistencia del saldo y la reconexión del WebSocket sin duplicar eventos. |
| ¿Qué aprendiste? | Que el cliente no es confiable y que la seguridad es una cadena: transporte, tokens, almacenamiento, validación y auditoría. |

> **Regla de oro:** si no sabes algo, di “no lo implementé; lo resolvería con X”. No inventes.

### 16.4. Plan B si falla la demo

| Falla | Acción inmediata |
|---|---|
| Render no responde | Cambia a backend local; usa emulador con flavor `local` o celular con `adb reverse` y `deviceDebug`. |
| Sin Internet | Usa backend local y emulador; el WebSocket funciona en localhost. |
| El celular no instala el APK | Muestra el emulador; ten el APK debug instalado. |
| El WebSocket no actualiza | Muestra la grabación de respaldo; explica el flujo y revisa el chip. |
| Postman falla | Usa `curl` o PowerShell; ten el comando listo. |
| La app crashea | Abre Logcat, explica el error con calma y reinicia; muestra el video. |
| No carga el dashboard | Cierra sesión, vuelve a entrar; verifica `/health` y que el seed esté cargado. |

Comandos de respaldo:

```powershell
# Backend local
Set-Location C:\NexoPayBackend
npm start

# Celular real
adb reverse tcp:3000 tcp:3000

# Emulador: usa flavor local (10.0.2.2)
.\gradlew.bat installLocalDebug

# Transferencia de respaldo con PowerShell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:3000/api/v1/auth/login `
  -ContentType 'application/json' `
  -Body '{"accountId":"10020002","password":"Nexo#2026"}'

$headers = @{
  Authorization = "Bearer $($login.accessToken)"
  "Idempotency-Key" = [guid]::NewGuid().ToString()
}

Invoke-RestMethod -Method Post -Uri http://localhost:3000/api/v1/operations/transfer `
  -Headers $headers -ContentType 'application/json' `
  -Body '{"toAccountId":"10020001","amountCents":500}'
```

### 16.5. Frases que suman puntos

- “El saldo se calcula en el servidor dentro de una transacción; el cliente no es confiable.”
- “El access token es corto y el refresh rota; si se reutiliza, se revoca la familia.”
- “El WebSocket no lleva JWT en la URL; usa un ticket efímero de un solo uso.”
- “Room es la caché persistente; el backend es la fuente de verdad.”
- “La UI es una función del estado; por eso no hay ANR.”
- “SQLCipher cifra la base completa; Keystore protege la clave.”
- “La idempotencia evita el doble cobro cuando la red falla.”

### 16.6. Frases que restan puntos (evítalas)

- “Usé JWT, así que es seguro.”
- “Guardé el token en SharedPreferences porque es más fácil.”
- “El WebSocket va con el token en la URL.”
- “Uso Double para el dinero.”
- “No probé en celular real.”
- “El APK está en mi laptop, no lo subí.”
- “Eso lo hizo la IA.” (Si usaste asistencia, decláralo según las reglas; no lo uses como excusa.)

### 16.7. Checklist final de defensa

- [ ] Backend despierto y `/health` OK.
- [ ] App de Ana logueada y chip verde.
- [ ] Postman con Luis logueado y token vigente.
- [ ] Transferencia de prueba ensayada 3 veces.
- [ ] Video de respaldo descargado.
- [ ] Informe PDF y GitHub abiertos.
- [ ] Celular cargado y en modo No molestar.
- [ ] Micrófono y cámara probados en Zoom.
- [ ] Guion cronometrado (máximo 7 min).
- [ ] Respuestas a las 20 preguntas repasadas.

---

## 17. CHECKLISTS Y ERRORES QUE BAJAN NOTA

### 17.1. Checklist 20/20

#### Entregables

- [ ] Repositorio Android público con README.
- [ ] Repositorio backend público o carpeta enlazada con README.
- [ ] APK release firmado, instalable y probado en celular real.
- [ ] Colección Postman exportada.
- [ ] Informe APA 7 en PDF.
- [ ] Video o grabación de la demo.
- [ ] Release `v1.0.0` con APK y SHA-256.

#### Funcionalidad

- [ ] Login con cuentas demo.
- [ ] Dashboard con saldo.
- [ ] Chip En línea/Desconectado.
- [ ] 4 botones con iconos.
- [ ] Depósito funcional.
- [ ] Retiro funcional.
- [ ] Retiro sin fondos controlado (sin crash).
- [ ] Transferencia P2P funcional.
- [ ] Pago de servicio con código de contrato.
- [ ] Historial reactivo.
- [ ] Historial sobrevive reinicio.
- [ ] Historial visible en modo avión.
- [ ] Ingresos verdes / egresos rojos.
- [ ] Logout funcional.

#### Técnico

- [ ] Room con entidad, DAO y base.
- [ ] SQLCipher activo y verificado.
- [ ] Retrofit con 4 operaciones.
- [ ] Refresh automático una sola vez.
- [ ] WebSocket con ticket de un solo uso.
- [ ] Reconexión con backoff.
- [ ] Corrutinas IO/Main.
- [ ] StateFlow + ViewModel.
- [ ] Sin ANR.
- [ ] Sin `runBlocking` en Main.
- [ ] Idempotencia en operaciones.
- [ ] Validación de montos/cuentas/contratos.
- [ ] Manejo de errores uniforme.
- [ ] `StrictMode` limpio.

#### Seguridad

- [ ] Argon2id en backend.
- [ ] Access token corto.
- [ ] Refresh rotativo con revocación.
- [ ] Bloqueo por intentos fallidos.
- [ ] Rate limit.
- [ ] Ticket WS efímero.
- [ ] Sin JWT en URL.
- [ ] SQLCipher + Keystore.
- [ ] `allowBackup=false`.
- [ ] NSC sin cleartext en release.
- [ ] Logcat sin secretos.
- [ ] R8 + release firmado.
- [ ] Secretos fuera de Git.

#### Presentación

- [ ] README claro y con capturas.
- [ ] Informe con portada, índice, referencias.
- [ ] Diagrama de arquitectura propio.
- [ ] Tabla de pruebas con evidencia.
- [ ] Capítulo de seguridad con amenazas/controles.
- [ ] Conclusiones que responden a objetivos.
- [ ] Defensa ensayada y cronometrada.
- [ ] Plan B local listo.

### 17.2. Errores que te mandan a 0–1

| Error | Por qué es grave | Cómo evitarlo |
|---|---|---|
| No entregar APK | El Word lo exige explícitamente | Genera release y pruébalo en celular |
| No usar Room | Es un requisito central | Entidad, DAO y prueba de reinicio |
| No usar WebSocket | Es el diferenciador del proyecto | Ticket + evento + demo |
| No usar corrutinas | El Word evalúa concurrencia | IO para red/BD, Main para UI |
| Historial que se pierde al reiniciar | Incumple persistencia | SQLCipher + Room + prueba |
| App que crashea con saldo insuficiente | Mala experiencia y poca robustez | Result + ErrorMapper |
| JWT hardcodeado o en URL | Falla de seguridad evidente | Login + ticket WS |
| Contraseñas en texto plano | Falla crítica | Argon2id |
| `Double` para dinero | Errores de precisión | Centavos enteros |
| Backend sin validación | Inyección/datos inválidos | Zod + prepared statements |
| No hay repositorio | El Word lo exige | GitHub público |
| No hay informe | El Word lo exige | PDF APA 7 |
| No demostrar en vivo | La defensa es obligatoria | Ensayo + plan B |
| Copiar la guía sin entender | Preguntas básicas te delatan | Estudia el flujo y las decisiones |

### 17.3. Rúbrica estimada (referencial)

| Criterio | Peso | Dónde se cubre |
|---|---|---|
| Cliente Android funcional | 20 % | Fases B, G |
| Persistencia Room | 15 % | Fase C |
| REST + 4 operaciones | 20 % | Fases A, D |
| WebSocket tiempo real | 15 % | Fase E |
| Corrutinas / sin ANR | 10 % | Fase F |
| Seguridad | 10 % | Fase H |
| Pruebas y evidencias | 5 % | Fase I |
| Informe y defensa | 5 % | Fases K, L |

> Ajusta según la rúbrica real de tu docente. La tabla sirve para priorizar: si te falta tiempo, asegura cliente, Room, REST, WS y corrutinas antes que los adornos visuales.

---

## 18. ANEXOS: POSTMAN, CURL, SQL, FAQ Y SOLUCIÓN DE PROBLEMAS

### 18.1. Colección Postman (importar como Raw text)

Crea una colección nueva → **Import** → **Raw text** → pega este JSON. Luego configura `baseUrl` en las variables de la colección.

```json
{
  "info": {
    "_postman_id": "b7f6e2a0-4f5e-4f6a-9d1e-nexopay0001",
    "name": "NexoPay UC1",
    "description": "Login, refresh, operaciones, ticket WS y verificación.",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    { "key": "baseUrl", "value": "http://localhost:3000" },
    { "key": "accessToken", "value": "" },
    { "key": "refreshToken", "value": "" },
    { "key": "accessTokenLuis", "value": "" },
    { "key": "refreshTokenLuis", "value": "" },
    { "key": "ticket", "value": "" }
  ],
  "item": [
    {
      "name": "01 Health",
      "request": {
        "method": "GET",
        "header": [],
        "url": { "raw": "{{baseUrl}}/health", "host": ["{{baseUrl}}"], "path": ["health"] }
      }
    },
    {
      "name": "02 Login Ana",
      "event": [
        {
          "listen": "test",
          "script": {
            "type": "text/javascript",
            "exec": [
              "const body = pm.response.json();",
              "pm.collectionVariables.set('accessToken', body.accessToken);",
              "pm.collectionVariables.set('refreshToken', body.refreshToken);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"accountId\":\"10020001\",\"password\":\"Nexo#2026\"}"
        },
        "url": { "raw": "{{baseUrl}}/api/v1/auth/login", "host": ["{{baseUrl}}"], "path": ["api","v1","auth","login"] }
      }
    },
    {
      "name": "03 Login Luis",
      "event": [
        {
          "listen": "test",
          "script": {
            "type": "text/javascript",
            "exec": [
              "const body = pm.response.json();",
              "pm.collectionVariables.set('accessTokenLuis', body.accessToken);",
              "pm.collectionVariables.set('refreshTokenLuis', body.refreshToken);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"accountId\":\"10020002\",\"password\":\"Nexo#2026\"}"
        },
        "url": { "raw": "{{baseUrl}}/api/v1/auth/login", "host": ["{{baseUrl}}"], "path": ["api","v1","auth","login"] }
      }
    },
    {
      "name": "04 Refresh Ana",
      "event": [
        {
          "listen": "test",
          "script": {
            "type": "text/javascript",
            "exec": [
              "const body = pm.response.json();",
              "pm.collectionVariables.set('accessToken', body.accessToken);",
              "pm.collectionVariables.set('refreshToken', body.refreshToken);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"refreshToken\":\"{{refreshToken}}\"}"
        },
        "url": { "raw": "{{baseUrl}}/api/v1/auth/refresh", "host": ["{{baseUrl}}"], "path": ["api","v1","auth","refresh"] }
      }
    },
    {
      "name": "05 Me Ana",
      "request": {
        "method": "GET",
        "header": [],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessToken}}", "type": "string" }] },
        "url": { "raw": "{{baseUrl}}/api/v1/auth/me", "host": ["{{baseUrl}}"], "path": ["api","v1","auth","me"] }
      }
    },
    {
      "name": "06 Balance Ana",
      "request": {
        "method": "GET",
        "header": [],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessToken}}", "type": "string" }] },
        "url": { "raw": "{{baseUrl}}/api/v1/operations/balance", "host": ["{{baseUrl}}"], "path": ["api","v1","operations","balance"] }
      }
    },
    {
      "name": "07 Movimientos Ana",
      "request": {
        "method": "GET",
        "header": [],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessToken}}", "type": "string" }] },
        "url": {
          "raw": "{{baseUrl}}/api/v1/operations/transactions?limit=50",
          "host": ["{{baseUrl}}"],
          "path": ["api","v1","operations","transactions"],
          "query": [{ "key": "limit", "value": "50" }]
        }
      }
    },
    {
      "name": "08 Deposito Ana",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Idempotency-Key", "value": "{{$guid}}" }
        ],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessToken}}", "type": "string" }] },
        "body": {
          "mode": "raw",
          "raw": "{\"amountCents\":1000}"
        },
        "url": { "raw": "{{baseUrl}}/api/v1/operations/deposit", "host": ["{{baseUrl}}"], "path": ["api","v1","operations","deposit"] }
      }
    },
    {
      "name": "09 Retiro Ana",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Idempotency-Key", "value": "{{$guid}}" }
        ],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessToken}}", "type": "string" }] },
        "body": {
          "mode": "raw",
          "raw": "{\"amountCents\":500}"
        },
        "url": { "raw": "{{baseUrl}}/api/v1/operations/withdraw", "host": ["{{baseUrl}}"], "path": ["api","v1","operations","withdraw"] }
      }
    },
    {
      "name": "10 Transferencia Luis a Ana",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Idempotency-Key", "value": "{{$guid}}" }
        ],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessTokenLuis}}", "type": "string" }] },
        "body": {
          "mode": "raw",
          "raw": "{\"toAccountId\":\"10020001\",\"amountCents\":500}"
        },
        "url": { "raw": "{{baseUrl}}/api/v1/operations/transfer", "host": ["{{baseUrl}}"], "path": ["api","v1","operations","transfer"] }
      }
    },
    {
      "name": "11 Pago servicio Ana",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Idempotency-Key", "value": "{{$guid}}" }
        ],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessToken}}", "type": "string" }] },
        "body": {
          "mode": "raw",
          "raw": "{\"contractCode\":\"LUZ-001\",\"amountCents\":300}"
        },
        "url": { "raw": "{{baseUrl}}/api/v1/operations/pay-service", "host": ["{{baseUrl}}"], "path": ["api","v1","operations","pay-service"] }
      }
    },
    {
      "name": "12 Ticket WS Ana",
      "event": [
        {
          "listen": "test",
          "script": {
            "type": "text/javascript",
            "exec": [
              "pm.collectionVariables.set('ticket', pm.response.json().ticket);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [],
        "auth": { "type": "bearer", "bearer": [{ "key": "token", "value": "{{accessToken}}", "type": "string" }] },
        "url": { "raw": "{{baseUrl}}/api/v1/realtime/ticket", "host": ["{{baseUrl}}"], "path": ["api","v1","realtime","ticket"] }
      }
    }
  ]
}
```

> **WebSocket en Postman:** no se importa como request HTTP. Crea **New → WebSocket**, URL `ws://localhost:3000/ws?ticket={{ticket}}` (o `wss://` en Render), y conecta. Debes recibir `connection.ready`.

> **Si `{{$guid}}` no se resuelve:** usa `{{$randomUUID}}` o genera un UUID y pégalo. La key debe ser única por operación.

### 18.2. Comandos curl de respaldo

```powershell
# Login Ana
curl.exe -s -X POST "http://localhost:3000/api/v1/auth/login" `
  -H "Content-Type: application/json" `
  -d "{\"accountId\":\"10020001\",\"password\":\"Nexo#2026\"}"

# Deposito (reemplaza TOKEN y KEY)
curl.exe -s -X POST "http://localhost:3000/api/v1/operations/deposit" `
  -H "Authorization: Bearer TOKEN" `
  -H "Idempotency-Key: KEY" `
  -H "Content-Type: application/json" `
  -d "{\"amountCents\":1000}"

# Transferencia de Luis a Ana
curl.exe -s -X POST "http://localhost:3000/api/v1/operations/transfer" `
  -H "Authorization: Bearer TOKEN_LUIS" `
  -H "Idempotency-Key: KEY" `
  -H "Content-Type: application/json" `
  -d "{\"toAccountId\":\"10020001\",\"amountCents\":500}"
```

> En PowerShell, `curl` es un alias de `Invoke-WebRequest` en algunas versiones. Usa `curl.exe` explícitamente.

### 18.3. Consultas SQL de verificación

Con el backend detenido o en modo lectura:

```powershell
Set-Location C:\NexoPayBackend

node -e "const D=require('better-sqlite3');const db=new D('data/nexopay.db');console.table(db.prepare('SELECT id,owner,balance_cents FROM accounts').all());"

node -e "const D=require('better-sqlite3');const db=new D('data/nexopay.db');console.table(db.prepare('SELECT id,account_id,type,amount_cents,balance_after_cents,created_at FROM transactions ORDER BY created_at DESC LIMIT 10').all());"

node -e "const D=require('better-sqlite3');const db=new D('data/nexopay.db');console.table(db.prepare('SELECT account_id,action,detail,created_at FROM audit_log ORDER BY id DESC LIMIT 10').all());"
```

Verificar que no haya saldo negativo:

```sql
SELECT id, owner, balance_cents
FROM accounts
WHERE balance_cents < 0;
```

Verificar que cada transferencia tenga exactamente dos filas:

```sql
SELECT reference_id, COUNT(*) AS filas
FROM transactions
WHERE reference_id IS NOT NULL
GROUP BY reference_id
HAVING COUNT(*) <> 2;
```

> Si la segunda consulta devuelve filas, hay un bug de atomicidad. Revisa que la transferencia esté dentro de `db.transaction()`.

### 18.4. Resetear los datos demo

```powershell
Set-Location C:\NexoPayBackend
# Detén el backend con Ctrl+C
Remove-Item -Force .\data\nexopay.db, .\data\nexopay.db-shm, .\data\nexopay.db-wal -ErrorAction SilentlyContinue
npm run seed
npm start
```

> **No borres la base en producción** sin respaldo. En Render free es efímera; el seed se ejecuta al arrancar si no existe.

### 18.5. Solución de problemas

| Problema | Causa probable | Solución |
|---|---|---|
| `EADDRINUSE :3000` | Otro proceso usa el puerto | `Get-NetTCPConnection -LocalPort 3000` y `Stop-Process -Id <PID>` |
| `better-sqlite3` no instala | Node incompatible o falta build | Node 22; `npm rebuild better-sqlite3`; instala VS Build Tools si no hay prebuilt |
| `FST_ERR_PLUGIN_VERSION_MISMATCH` | Fastify 5 con plugin 4 | Usa las versiones de la guía |
| `401 invalid_credentials` | Cuenta/clave mal o seed no ejecutado | `npm run seed`; revisa la clave `Nexo#2026` |
| `423 account_locked` | 5 fallos | Espera 15 min o resetea la BD demo |
| `429 rate_limited` | Ráfaga de requests | Espera; reinicia backend en demo |
| `404` en Android | URL base mal o falta `/` | `baseUrl` termina en `/`; `@POST("api/...")` sin `/` inicial |
| `CLEARTEXT communication not permitted` | Release contra HTTP | Usa HTTPS o build debug local |
| `Unable to create converter` | Falta plugin serialization | Aplica el plugin y `@Serializable` |
| `JsonDecodingException` | DTO no coincide | Compara con el JSON real de Postman |
| `UnsatisfiedLinkError: sqlcipher` | AAR/ABI ausente | Clean/rebuild; verifica `sqlcipher-android` |
| `Room cannot verify data integrity` | Passphrase distinta | Borra datos de la app o reinstala |
| `SQLiteDatabaseCorruptException` | Base a medio escribir | Detén la app, borra datos, re-sincroniza |
| `4401 invalid_ticket` | Ticket vencido/reutilizado | Pide ticket nuevo |
| Chip rojo en emulador | Backend apagado o URL mal | `/health`; flavor `local` |
| Chip rojo en celular | Falta `adb reverse` | `adb reverse tcp:3000 tcp:3000` |
| `adb: device unauthorized` | Falta aceptar RSA | Acepta el diálogo en el celular |
| APK no instala | Firma distinta o ABI | `adb uninstall`; usa APK universal |
| Release no conecta | `TU-BACKEND` sin reemplazar | Edita `prod` y rebuild |
| Render 502 | Cold start | Abre `/health`, espera 30–60 s |
| Render pierde datos | Disco efímero | Re-seed; para persistencia, disco pago/Postgres |
| Gradle falla por JDK | JDK incorrecto | Usa JDK 17 embebido |
| `Duplicate class` | Dependencias mezcladas | `.\gradlew.bat clean`; revisa árbol |
| `Cannot fit requested classes` | Multidex | No aplica con minSdk 26; revisa dependencias |
| Informe sin índice | No actualizado | Word → Referencias → Actualizar tabla |
| GitHub rechaza push | Rama/credenciales | `git branch -M main`; usa token/SSH |

### 18.6. Preguntas rápidas de sustentación (fuera del guion)

| Pregunta | Respuesta en una línea |
|---|---|
| ¿Qué es un sistema distribuido? | Varios nodos que cooperan por red y pueden fallar de forma independiente. |
| ¿Dónde está la consistencia? | En el servidor: transacción SQLite por operación. |
| ¿Qué es idempotencia? | Repetir la misma solicitud produce el mismo efecto, no un duplicado. |
| ¿Qué es un refresh rotativo? | Cada refresh genera uno nuevo e invalida el anterior. |
| ¿Qué es un ticket WS? | Credencial efímera de un solo uso para abrir el socket. |
| ¿Qué es ANR? | Application Not Responding: el hilo principal se bloquea >5 s. |
| ¿Qué es un StateFlow? | Flujo de estado observable con valor actual, ideal para UI. |
| ¿Qué es ViewBinding? | Genera clases tipadas para las vistas del layout. |
| ¿Qué es R8? | Optimizador/ofuscador de código y recursos en release. |
| ¿Qué es SQLCipher? | Extensión de SQLite que cifra páginas y WAL. |
| ¿Qué es Keystore? | Almacén de claves respaldado por hardware/TEE en Android. |
| ¿Qué es WAL? | Write-Ahead Logging: lecturas concurrentes mientras se escribe. |
| ¿Qué es backoff exponencial? | Espera creciente entre reintentos para no saturar. |
| ¿Qué es un envelope de error? | Formato uniforme `{error:{code,message,details,requestId}}`. |
| ¿Por qué no polling? | Latencia y tráfico; WebSocket empuja eventos. |
| ¿Por qué no guardar la contraseña? | Se verifica en servidor; el cliente no la necesita después. |

### 18.7. Enlaces internos y mantenimiento de la guía

- Si cambias el nombre de una sección, actualiza el **ÍNDICE** del inicio.
- Si cambias un endpoint, actualiza la tabla 4.5, `NexoApi`, la colección Postman y el informe.
- Si cambias un campo de la base, actualiza entidad, DTO, mapper y prueba.
- Si cambias un string, actualiza `strings.xml` y las capturas.
- Si cambias la URL de Render, actualiza `prod`, README, Postman, informe y defensa.

> **Antes de entregar:** busca en el proyecto `TU-BACKEND`, `TODO`, `CAMBIA_ESTA_CLAVE`, `localhost` y `10.0.2.2` para asegurarte de que no quedaron marcadores en el release.

### 18.8. Cierre

Si llegaste hasta aquí y ejecutaste las fases en orden, tienes:

- Un backend real con autenticación, operaciones, idempotencia y WebSocket.
- Una app Android con XML/Material 3, Room/SQLCipher, Retrofit, corrutinas y estado reactivo.
- Seguridad reforzada y demostrable.
- Pruebas y evidencia para el informe.
- Un APK release y un repositorio presentable.

**Lo que decides ahora:** ensayar la demo al menos tres veces y medir tus tiempos reales. La diferencia entre un 16 y un 20 suele estar en la defensa, no en el código.
