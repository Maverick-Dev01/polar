# Actualizaciones Android desde GitHub Releases

La versión Android distribuida por GitHub tiene **Ajustes → Actualizaciones**: buscar, descargar con progreso, cancelar/descartar e instalar. El usuario confirma cada descarga y la instalación en Android. Diseñar, imprimir y guardar sigue funcionando sin conexión. Mac conserva su versión y no usa este actualizador.

## Flujo y contrato

- `UpdateInfo`: JSON de publicación, sin fotos ni información de usuarios. Contiene versión numérica, nombre, paquete, SDK mínimo, tamaño, SHA-256, notas y URL del APK fijada a su tag.
- `UpdateRepository`: consulta HTTPS de `https://github.com/Maverick-Dev01/polar/releases/latest/download/update.json` con límite de 64 KiB/timeouts. Sólo acepta APK del repositorio y nombre/tag esperados. Android DownloadManager descarga a `Download/updates` dentro del almacenamiento de la app; no necesita permiso de almacenamiento.
- El ID nativo y el manifiesto se guardan juntos. Volver a Ajustes o recrear la app reconcilia la descarga; al cancelar o actualizar se elimina el archivo temporal.
- Antes de mostrar Instalar y de nuevo antes de abrir el instalador se verifica tamaño, SHA-256, paquete, versión superior, SDK, que no sea debug y el conjunto completo de certificados de firma. Archivo incompleto, alterado o de otro firmante se rechaza y permite reintentar.
- FileProvider sólo concede lectura del APK al instalador. Android puede pedir «Permitir desde esta fuente» para Polar; volver sin concederlo permite reintentar. La instalación conserva la biblioteca existente del mismo paquete.
- El cliente consulta sólo cuando pulsas Buscar; no hay cuentas, tokens incluidos en la app, comprobaciones periódicas, servidor propio ni subida de fotos.

## Primera instalación y firma

Instala una vez el APK release de [GitHub Releases](https://github.com/Maverick-Dev01/polar/releases). Las siguientes versiones se obtienen desde Ajustes. La app de desarrollo termina en `.debug` y es otra instalación: no se elimina ni se modifica su biblioteca al instalar release. Una APK anterior firmada con otra llave no puede actualizarse con la llave nueva.

La llave permanente y su configuración quedan en **`.polar-signing/polar-release.jks`** y **`.polar-signing/signing.json`**, ignorados por Git. Guarda una copia privada de **toda esa carpeta**: para publicar desde otro equipo hay que recuperarla además de clonar el código. No regeneres la llave para una app ya distribuida. La contraseña no se imprime ni se añade a los argumentos de procesos.

Si quieres usar una llave propia, configura las cuatro propiedades `POLAR_STORE_FILE`, `POLAR_STORE_PASSWORD`, `POLAR_KEY_ALIAS` y `POLAR_KEY_PASSWORD` en tus propiedades privadas de Gradle o variables de entorno y ejecuta Gradle directamente. El build release ya no utiliza una firma debug como alternativa: publicar sin las cuatro propiedades falla.

## Preparar una nueva versión (Windows, Mac o Linux)

Requiere el entorno Android del README, **Python 3** para el pequeño script de publicación y [GitHub CLI](https://cli.github.com/) para subir los archivos. La app y la compilación Android normal no requieren Python.

1. Actualiza `versionCode` (siempre mayor) y `versionName` en `PolarAndroid/app/build.gradle.kts`. El nombre usa tres números, por ejemplo `2.1.2`.
2. Desde la raíz, con `JAVA_HOME` apuntando al JDK 21, ejecuta:

```sh
python tools/android-release.py --channel both
```

En Mac puedes usar `python3`. El script conserva la llave existente y prepara ambos canales por defecto:

| Canal | Archivo en `release-assets/` | Actualizaciones |
| --- | --- | --- |
| GitHub (`release`) | `Polar-VERSION.apk`, `update.json`, `SHA256SUMS.txt` y `Polar-VERSION-github-mapping.txt` | Ajustes → Actualizaciones |
| Google Play (`play`) | `Polar-VERSION-play.aab`, `SHA256SUMS-play.txt` y `Polar-VERSION-play-mapping.txt` | Google Play |

`--channel github` ejecuta sólo `prepareGithubRelease`; `--channel play` sólo `preparePlayRelease`. Play no genera ni reemplaza `update.json`. Conserva los archivos `mapping` de cada versión para interpretar informes de fallos del código minificado. Para cambiar las notas agrega `-PPOLAR_RELEASE_NOTES="Descripción del cambio"`. APK, manifiesto y SHA deben pertenecer a **la misma compilación**.

3. Ejecuta `testDebugUnitTest`, `lintDebug` y prueba la instalación sobre una versión anterior en un dispositivo. Confirma firma y versión del APK. Guarda y sube los cambios de código; publica sólo cuando el código corresponda al APK.
4. Autentica GitHub CLI con una cuenta que tenga permiso de escritura en `Maverick-Dev01/polar`. Escribe las notas en un archivo y publica, sustituyendo versión y SHA por los reales:

```sh
gh release create android-v2.1.2 release-assets/Polar-2.1.2.apk release-assets/update.json release-assets/SHA256SUMS.txt --repo Maverick-Dev01/polar --target SHA_DEL_COMMIT --title "Polar Android 2.1.2" --notes-file notas-release.md --latest
```

GitHub Releases aloja directamente el APK y mantiene el historial. No requiere GitHub Pages. No reemplaces el APK de un tag ya instalado por otra compilación: aumenta la versión y crea otra release. Reserva la release marcada **Latest** para Android, porque el enlace del cliente busca su `update.json`. Puedes adjuntar también el ZIP Mac y añadir su SHA-256 a SHA256SUMS.txt; no publiques una release Latest sin el manifiesto Android.

## Google Play

La build **release** incluye el actualizador de GitHub desde `src/github/`. La build **play** no compila esas fuentes y `src/play/` muestra únicamente el canal Google Play; su manifiesto elimina `REQUEST_INSTALL_PACKAGES`. Google Play gestiona sus actualizaciones. Ajustes identifica el canal. Compila el AAB con `python3 tools/android-release.py --channel play` desde la raíz, o con la configuración privada de firma y `./gradlew bundlePlay` / `.\gradlew.bat bundlePlay`.

Ambas usan `io.github.maverickdev01.polar`: son distribuciones de la misma app, y no se instalan simultáneamente como dos iconos. La versión local conserva la firma permanente y no borra datos. Para conservar esa continuidad cuando Google genere los APK de Play, configura **Play App Signing con la llave de firma existente** en la primera publicación, siguiendo el procedimiento cifrado de Play Console. La firma del AAB es la de subida y no garantiza por sí sola la firma final del APK. Si Google usa otra llave, no intercambies APK entre canales; respalda los recursos del diseño antes de cualquier migración. [Firma oficial de Android](https://developer.android.com/studio/publish/app-signing).

Una app distribuida por Play no puede actualizarse fuera de Play. Además, Play App Signing puede usar otra llave: no se deben mezclar APK de ambos canales para actualizar una instalación. [Política oficial](https://support.google.com/googleplay/android-developer/answer/9888379), [permiso de instalación Android](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()), [descargas nativas](https://developer.android.com/reference/android/app/DownloadManager).

## Validación de esta función

- Pruebas de manifiesto válido, URL ajena/insegura/tag equivocado, tamaño/JSON inválido, SHA alterado/truncado, downgrade, paquete y firma incompleta/distinta.
- Robolectric API 26 y 34: una sola descarga, restauración al recrear, cancelar/reintentar, limpiar tras actualizar y estado local inválido.
- Revisión independiente, build firmado, manifiesto Play sin permiso de instalación y recorrido real en emulador antes de dar la release por terminada.
