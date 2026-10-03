# Polar para Android

Diseña e imprime tus fotos: polaroids, canción con QR, boletos, películas, calendarios y tus propias plantillas. Funciona sin internet; tus fotos no salen del teléfono. Compatible con los proyectos `.polar` de Polar para Mac.

## Instalar el APK de prueba

La versión release firmada está en [GitHub Releases](https://github.com/Maverick-Dev01/polar/releases). Instálala una vez; después usa **Ajustes → Actualizaciones** para buscar, descargar e instalar nuevas versiones. Android pide confirmar la instalación. Tus fotos no se envían a GitHub. [Publicar nuevas versiones y conservar la firma](../docs/actualizaciones-android.md).

El APK de desarrollo local `../Polar.apk` es otra instalación (`.debug`). No se elimina al instalar release. Los APK se publican como assets de release y no forman parte del historial Git.

## Compilar

Abre esta carpeta en Android Studio. Requiere SDK Platform 36, Build Tools 35.0.0 y un JDK compatible (17 mínimo; 21 usado en la verificación). Gradle 8.14.3 se descarga con el wrapper incluido.

En Windows, desde PowerShell y con `JAVA_HOME` configurado:

```powershell
cd PolarAndroid
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Android Studio crea el `local.properties` del equipo; no copies la ruta del SDK de otro sistema. La [guía Windows del README principal](../README.md#continuar-android-en-windows) explica instalación, SDK, JDK, emulador y rutas. No requiere Swift ni compilar la app Mac.

En macOS o Linux, con `JAVA_HOME` apuntando al JDK instalado:

```sh
cd PolarAndroid
./gradlew testDebugUnitTest lintDebug assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Los compilados no se incluyen en GitHub.

Para publicar, sigue `../docs/play-store.md`.

## Cómo está hecho

- `model/`: proyecto `.polar` inmutable con las mismas claves que la Mac.
- `core/`: texto por tarjeta (`TextResolver`), filtros (`LookResolver`), ediciones puras (`ProjectEdits`), deshacer (`UndoStack`).
- `engine/`: un solo motor de dibujo para la vista previa, las miniaturas, el PDF, el PNG, el JPG y la impresión.
- `data/`: biblioteca en disco con autoguardado, fotos copiadas al proyecto, fuentes incluidas y ajustes.
- `ui/`: Compose + Material 3. Inicio, Catálogo, Editor (Fotos, Filtros, Diseño, Texto, Papel, Encuadrar y Terminar), Ajustes. Con ancho ≥ 840dp se usan paneles laterales.

Stack y versiones: Kotlin 2.2.0, AGP 8.11.1, Compose BOM 2025.06.01, coroutines, kotlinx.serialization, DataStore, ExifInterface y ZXing. `minSdk=26`, `compileSdk=targetSdk=36`.

Diseño completo: `../docs/superpowers/specs/2026-10-02-polar-android-rediseno-design.md`.

## Exportación ligera

En Terminar, PDF exporta todas las hojas con fotos visibles a 300 ppp y JPEG 94, conservando texto, guías y QR. JPG exporta la hoja seleccionada a 300 ppp con menos peso que PNG en fotografías. PDF sin compresión JPEG y PNG conservan los píxeles renderizados sin pérdidas adicionales de compresión; pueden pesar más. [Detalles y verificaciones](../docs/exportaciones-compactas.md).
