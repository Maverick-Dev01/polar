# Verificación de actualizaciones Android · 2026-10-02

Release pública: [Polar Android 2.1.1](https://github.com/Maverick-Dev01/polar/releases/tag/android-v2.1.1), código **4**, compilada desde el commit **a54ca0b**. Mac no cambia con esta publicación.

## Archivo publicado

- APK: `Polar-2.1.1.apk`, **4 513 343 bytes**.
- SHA-256: `d853d1033796b9785ec0860ed1867dfa1d87e7054009b63f158cc228fe3294c1`.
- Paquete: `io.github.maverickdev01.polar`; versión 2.1.1/4; mínimo Android 8/API 26; target API 36; no depurable.
- Firma release RSA de 4096 bits; certificado SHA-256: `cd5a37bea894d1f30d53704caa7fe1af83afe6aa60d957951cd3bf9f07c4444e`.
- `apksigner` verificó la firma. Los tamaños y SHA de los tres archivos publicados coinciden con los assets de GitHub. El enlace público `releases/latest/download/update.json` devuelve el mismo manifiesto local sin autenticación.

## Comprobaciones automáticas

- `testDebugUnitTest`: **195 pruebas**, cero fallos, errores u omisiones. Incluye validación del manifiesto, URL, tamaño/hash, paquete, versiones y certificados; persistencia de descargas y cancelaciones en API 26 y 34.
- `connectedDebugAndroidTest`: **6 pruebas nativas**, cero fallos u omisiones, en el emulador `mercatto_test`, Android 14/API 34.
- `lintDebug`: cero errores y 69 advertencias. Las recomendaciones nuevas de SharedPreferences/URI son de estilo; la escritura síncrona de la descarga conserva el resultado de persistencia.
- APK release firmado y APK/AAB de la variante Play compilados. El manifiesto Play carece de `REQUEST_INSTALL_PACKAGES` y desactiva el actualizador externo.
- `packageRelease` sin configuración privada de firma falla como corresponde. `python3 tools/check-release-signing.py` verifica que una carpeta de firma incompleta no regenere ni sustituya la llave.
- Una revisión independiente detectó y se corrigieron la cancelación de una descarga nueva por un monitor antiguo y el riesgo de reemplazar una llave incompleta.

## Recorrido de actualización en el emulador

Se instaló una **compilación de prueba** 2.1.0/código 3, con el actualizador nuevo y la misma firma permanente. Esta prueba no supone que la versión antigua ya distribuida tuviera el actualizador.

1. Crear un diseño Polaroid y activar el modo oscuro.
2. En Ajustes, buscar y encontrar la release pública 2.1.1; descargarla mediante DownloadManager y verificar el APK.
3. Cerrar y reabrir la app: recuperar la descarga y volver a ofrecer la instalación verificada.
4. Abrir el permiso de instalación y volver sin concederlo: mostrar el mensaje correspondiente y permitir reintentar.
5. Conceder «Permitir desde esta fuente», confirmar **Actualizar** en el instalador nativo e instalar el APK descargado de GitHub.
6. Abrir la app actualizada: verificar código 4/versión 2.1.1, el mismo diseño en la biblioteca y el modo oscuro conservado.
7. Buscar otra vez: mostrar «Tienes la versión más reciente».

La sección se inspeccionó visualmente en modo claro y oscuro, y en oscuro con tamaño de letra del sistema al 130 %, sin cortes en el texto ni controles inaccesibles. Se restauró después el tamaño de letra original del emulador. Las capturas están en `docs/capturas/actualizaciones-android/`, permanecen locales y están excluidas de Git.

La instalación se probó en emulador; no se ejecutó este recorrido en un teléfono físico ni la compilación en Windows. Las actualizaciones descargan únicamente archivos de la aplicación: no se envían fotos ni proyectos.

## Continuidad

Las nuevas versiones se publican siguiendo [la guía de actualizaciones](actualizaciones-android.md). Conserva una copia privada de **toda `.polar-signing/`** para poder firmar desde otro equipo. Esa carpeta no se publica en Git ni en Releases. Google Play usa la variante `play` y sus propios mecanismos de actualización.
