# NexoPay — Billetera móvil Android UC1

Aplicación Android en Kotlin con interfaz XML y Material 3. Usa Room con SQLCipher para el historial local cifrado, Retrofit/OkHttp para la API y WebSocket para actualizar el saldo en tiempo real.

Repositorio backend: `https://github.com/30621-srvillegas/nexopay-backend`

## Demo

- APK release: `https://github.com/30621-srvillegas/nexopay-android/releases`
- Backend: `https://nexopay-backend-xnr5.onrender.com`

## Cuentas de prueba

| Cuenta | Dueño | Clave |
|---|---|---|
| 10020001 | Ana Torres | Nexo#2026 |
| 10020002 | Luis Rojas | Nexo#2026 |

## Arquitectura

```text
Android -> HTTPS/REST -> Fastify -> SQLite
Android <- WSS (ticket de un solo uso) <- Fastify
```

## Funcionalidades

- Login con access token corto y refresh token rotativo.
- Depósito, retiro, transferencia P2P y pago de servicios.
- Historial local cifrado con SQLCipher y actualización reactiva con Room/Flow.
- Actualización de saldo en tiempo real por WebSocket.
- Almacenamiento seguro de tokens con Android Keystore.
- Configuración de red por flavor: local, device y prod.

## Requisitos

- Android Studio.
- JDK 17.
- Android SDK 35.
- Backend NexoPay en ejecución o desplegado en Render.

## Compilar

```powershell
# Emulador con backend local en 10.0.2.2
.\gradlew.bat assembleLocalDebug

# Celular real con adb reverse
.\gradlew.bat assembleDeviceDebug

# Release contra Render
.\gradlew.bat assembleProdRelease
```

El flavor `prod` ya utiliza el backend de Render indicado arriba. Para generar una release firmada, configura localmente `keystore.properties` y el archivo de firma; estos archivos no se versionan.

## Documentación y comprobación de endpoints

- [Render, bases de datos y diagnóstico de saldos](docs/exposicion/01-RENDER-BASES-DE-DATOS-Y-SALDOS.md).
- [Explicación sencilla del código](docs/exposicion/02-EXPLICACION-SENCILLA-DEL-CODIGO.md).
- [Seguridad implementada](docs/exposicion/03-SEGURIDAD-IMPLEMENTADA.md).
- [Guion de exposición de máximo 15 minutos](docs/exposicion/04-GUION-DE-EXPOSICION-15-MINUTOS.md).
- [Colección de Postman para Render](postman/NexoPay-Render.postman_collection.json) e [instrucciones de ejecución y capturas](postman/LEEME-EVIDENCIAS.md).
- [Guía original de construcción del proyecto](GUIA-PROYECTO-BILLETERA-V2.md), como material de referencia; los documentos de exposición describen los ajustes de la versión actual.

## Probar

```powershell
.\gradlew.bat testLocalDebugUnitTest
.\gradlew.bat connectedLocalDebugAndroidTest
```

El backend debe estar levantado para las pruebas instrumentadas que usan la API y el WebSocket.

## Seguridad

- SQLCipher para la base local.
- Tokens almacenados mediante Android Keystore.
- Network Security Config sin cleartext en release.
- Backup deshabilitado en release.
- Sin secretos ni keystore dentro del repositorio.
- El APK final se distribuye únicamente en GitHub Releases.

## Autor

Stalin Roche y Steven Villegas — Aplicaciones Distribuidas UC1 — 2026
