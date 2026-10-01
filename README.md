# Coch — MVP Android

Coch es una aplicación Android mínima con controles **Iniciar** y **Parar** para una sesión de captura de pantalla. Al iniciar, Android muestra su diálogo del sistema para pedir consentimiento; la captura nunca comienza en silencio.

## Requisitos

- Android Studio estable con Android SDK 35.
- JDK 17.
- Un dispositivo o emulador Android 9 (API 28) o posterior.

## Compilar y ejecutar

```bash
./gradlew test
./gradlew assembleDebug
./gradlew installDebug
```

También puedes abrir el proyecto en Android Studio y ejecutarlo desde allí.

1. Abre **Coch** y pulsa **Iniciar**.
2. En el diálogo del sistema, permite compartir la pantalla para esta sesión.
3. La app muestra el estado **RUNNING** y tres sugerencias de ejemplo.
4. Pulsa **Parar** para finalizar la sesión.

Si deniegas o cancelas el permiso, la app muestra un mensaje y permite volver a intentarlo. Android muestra una notificación persistente mientras el servicio de captura está activo. Al detener la app desde Recientes, el servicio libera la sesión.

## Permisos y comportamiento

El manifiesto declara `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_MEDIA_PROJECTION`, y el servicio especifica el tipo `mediaProjection`. La app solicita el consentimiento mediante `MediaProjectionManager` y solo después inicia el servicio de primer plano. Android también puede finalizar una sesión desde el sistema; la app refleja ese estado.

Las sugerencias son **marcadores de posición deterministas**. Los fotogramas capturados se descartan inmediatamente; sus píxeles no se inspeccionan ni analizan. No hay OCR, ML ni recomendaciones de red.

## Limitaciones y trabajo futuro

- Integrar análisis local de pantalla (por ejemplo OCR) solo tras diseñar explícitamente el tratamiento de imágenes y privacidad.
- Mejorar los mensajes de error y las pruebas en dispositivos Android reales.
- No controlar juegos ni otras apps: no se inyectan toques, teclado o controles; no hay `AccessibilityService`, permiso de superposición, panel flotante ni automatización.

La app está intencionalmente limitada a mostrar sugerencias dentro de su propia ventana. No se implementarán controles de juego ni superposiciones como parte de este MVP.
