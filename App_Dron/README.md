# Drinix GCS — Estación de Control para Dron

App Android nativa (Kotlin + Jetpack Compose, Material 3, MVVM + Hilt) que actúa como
Ground Control Station para un dron ArduPilot, comunicándose con un backend FastAPI
en una Raspberry Pi dentro de la red local. **Sin pantalla de login**: la app se
conecta automáticamente al abrirse.

## Estructura

```
com.drinix.gcs
├── DrinixApp.kt              (@HiltAndroidApp)
├── MainActivity.kt           (apply FLAG_KEEP_SCREEN_ON)
├── theme/Theme.kt            (tema azul oscuro "cabina de vuelo")
├── data/
│   ├── model/                (Telemetry, comandos, Waypoint, CommandResult, FlightAlert)
│   ├── network/DroneApi.kt   (Retrofit) + Interceptors (Bearer + rewrite de host)
│   ├── SettingsRepository.kt (DataStore: host, puerto, token, keep-screen-on)
│   ├── SessionManager.kt     (host/puerto/token en memoria para OkHttp/WS)
│   ├── TelemetryRepository.kt(WebSocket + backoff exponencial 1s→30s)
│   └── CommandRepository.kt  (envoltura REST → CommandResult)
├── di/AppModule.kt           (Gson, OkHttp, Retrofit, DroneApi)
└── ui/
    ├── DroneViewModel.kt     (ViewModel compartido: StateFlow<Telemetry>, alertas, comandos)
    ├── common/               (MetricCard, StatusBanner, GcsTopBar, ConnectionChip)
    ├── navigation/           (NavHost + bottom bar: Inicio/Mapa/Telemetría/Más)
    ├── home/                 (tarjeta del dron, conectar, métricas rápidas)
    ├── map/                  (MapScreen con mini-mapa + FullMapScreen con goto)
    ├── telemetry/            (tablero de telemetría + vibración en batería baja)
    ├── control/              (armar/despegar/RTL + joysticks RC propios)
    ├── mission/              (waypoints: tocar mapa, editar alt, reordenar, subir)
    ├── status/               (estado del dron por subsistemas)
    ├── alerts/               (lista de alertas generadas por la telemetría)
    ├── more/                 (menú de subpantallas)
    └── settings/             (IP/puerto, reconectar, mantener pantalla)
```

## Navegación

Pestañas inferiores: **Inicio**, **Mapa**, **Telemetría**, **Más**
(Más → Control, Misión, Estado del dron, Alertas, Ajustes).
Desde la pestaña Mapa se puede expandir a pantalla completa (long-press = "¿Volar aquí?").

## Cómo compilar

1. Abrir la carpeta `DrinixGCS` en **Android Studio** y dejar que sincronice Gradle.
   Android Studio usa su propio JDK embebido, no requiere configuración extra.
2. Ejecutar en un dispositivo con API 24+ (o emulador).

Por línea de comandos en este equipo:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

> Nota: el JDK del sistema es Java 8 y NO sirve para compilar; se debe usar el JBR
> de Android Studio (Java 25) con Gradle 9.6. El APK queda en
> `app\build\outputs\apk\debug\app-debug.apk`.

## API Key de Google Maps

La app compila sin clave válida, pero el mapa aparecerá vacío hasta configurarla:

1. Obtener una key en [Google Cloud Console](https://console.cloud.google.com/)
   (habilitar **Maps SDK for Android**).
2. Ponerla en `local.properties` (recomendado, no se sube a git):
   ```
   MAPS_API_KEY=AIza...tu_clave
   ```
   o editar `gradle.properties` (raíz del proyecto), línea `MAPS_API_KEY=`.

La clave se inyecta en el `AndroidManifest.xml` vía `manifestPlaceholders`.

## Cambiar la IP del backend

- **Por defecto**: `172.20.10.2:8000` (constantes en `SettingsRepository`).
- **En tiempo de ejecución**: pestaña **Más → Ajustes** → *Host IP* y *Puerto* →
  **Guardar y reconectar**. Persiste en DataStore y reinicia REST + WebSocket.

## Notas

- **HTTP plano**: LAN sin TLS, por eso el Manifest incluye `usesCleartextTraffic="true"`.
- **Reconexión WS**: backoff exponencial 1s, 2s, 4s... tope 30s. OkHttp responde
  automáticamente los PING del servidor con PONG.
- **RC override**: los joysticks envían `POST /api/command/rc` a 5 Hz con PWM
  (1000–2000, centro 1500). Throttle (izq.) se mantiene al soltar; yaw/roll/pitch
  vuelven a 1500.
- **Alertas**: batería < 20% (crítica, con vibración), GPS < 6 satélites (advertencia),
  pérdida de telemetría y RTH iniciado; se listan en **Más → Alertas** con hora.
- Si el backend sigue requiriendo JWT, la app lo toma del DataStore si existe
  (`AuthInterceptor`); sin token igual intenta conectar (depende del backend).
