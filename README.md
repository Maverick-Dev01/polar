# Polar · Fotos para imprimir

Aplicaciones nativas para Android y Mac que convierten fotografías en hojas listas para imprimir: polaroids, boletos, tiras de película, calendarios, fotos con canción y moldes propios. Incluyen texto editable, encuadre, filtros no destructivos, autoguardado y exportación PDF/JPG/PNG.

Android y Mac: **2.2.0**, build **6**. La rama de trabajo publicada es **`global`**.

[Descargar Android](https://github.com/Maverick-Dev01/polar/releases/download/android-v2.2.0/Polar-2.2.0.apk) · [Descargar Mac](https://github.com/Maverick-Dev01/polar/releases/download/android-v2.2.0/Polar-Mac-2.2.0.zip). Después de instalarla una vez, **Ajustes → Actualizaciones** busca y descarga nuevas versiones, con confirmación del instalador Android. [Preparar y publicar actualizaciones](docs/actualizaciones-android.md); la llave privada debe conservarse para poder publicar desde otro equipo.

PDF conserva texto, guías y marcos nítidos, con fotos recortadas al área visible a 300 ppp y JPEG de alta calidad (94). JPG exporta una hoja más ligera a 300 ppp. Para evitar compresión fotográfica adicional, elige **PDF sin compresión JPEG** o **PNG**; estos archivos pueden pesar más. Los originales y el proyecto editable se conservan. [Diagnóstico y verificación de exportaciones](docs/exportaciones-compactas.md).

Mac se distribuye como ZIP de `Polar.app` para Apple Silicon/macOS 14+. Descomprímelo y sustituye la app anterior con Polar cerrada; la biblioteca permanece en Application Support. **Ayuda → Descargar última versión…** abre la publicación. La firma es local, sin notarización de Apple: macOS puede solicitar abrirla desde Privacidad y seguridad.

## Stack tecnológico

| Parte | Tecnologías |
| --- | --- |
| Android | Kotlin **2.2.0**, Jetpack Compose (BOM **2025.06.01**), Material 3, AndroidX/ViewModel, coroutines **1.10.2**, kotlinx.serialization **1.9.0** y DataStore. |
| Compilación Android | Android Gradle Plugin **8.11.1**, Gradle Wrapper **8.14.3**, bytecode Java/Kotlin **17**. `compileSdk`/`targetSdk` **36**, `minSdk` **26** (Android 8.0). |
| Fotos, PDF y QR | Canvas/Bitmap, ColorMatrix, PdfDocument/PdfRenderer, AndroidX ExifInterface, ZXing **3.5.3** y ML Kit Subject Segmentation **16.0.0-beta1** mediante Servicios de Google Play. Un motor compartido dentro de cada app genera la vista previa y la exportación. |
| Mac | **Swift**, **SwiftUI**, **AppKit**, Core Graphics, ImageIO y Core Image y Vision para separar sujetos del fondo. Compilación con `swiftc`/Command Line Tools, macOS **14+**, **Apple Silicon**. |
| Pruebas | JUnit 4, Robolectric, Compose UI/instrumentación Android y ejecutables de pruebas Swift. |
| Datos | Proyectos `.polar` en JSON compatibles entre las dos apps; biblioteca y copias de fotos guardadas localmente. |

Son dos aplicaciones nativas, con código de interfaz separado y el mismo contrato de proyecto/filtros. No requieren servidor, base de datos externa, Node.js, Flutter, API keys ni cuenta para usar la aplicación. Internet se necesita para clonar y descargar herramientas/dependencias la primera vez, y para buscar/descargar actualizaciones Android desde GitHub. Diseñar e imprimir sigue funcionando sin conexión. Quitar fondo en Android requiere Servicios de Google Play y descargar su modelo la primera vez; las fotos se procesan localmente. Mac usa Vision del sistema. El catálogo de 120 frases originales es local y no requiere una API.

## Continuar Android en Windows

**Sí se puede desarrollar y compilar Android desde Windows.** La app Mac requiere macOS para compilar y ejecutar SwiftUI/AppKit.

1. Instala Git y [Android Studio para Windows](https://developer.android.com/studio/install), en una versión compatible con AGP 8.11.1.
2. Usa **JDK 21** para reproducir el entorno probado; AGP requiere como mínimo JDK 17. Selecciónalo en **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**. Para la terminal, configura `JAVA_HOME` con la misma carpeta del JDK, sin añadir `bin`. [Configuración oficial del JDK](https://developer.android.com/build/jdks).
3. En SDK Manager instala **Android SDK Platform 36**, **Build Tools 35.0.0** y **Platform-Tools**. Para usar emulador, instala además Android Emulator y una imagen del sistema. [Compatibilidad de AGP 8.11](https://developer.android.com/build/releases/agp-8-11-0-release-notes).
4. Clona y abre **la carpeta `PolarAndroid`**, que contiene `settings.gradle.kts`, en Android Studio. Espera a que termine la sincronización de Gradle.

```powershell
git clone --branch global https://github.com/Maverick-Dev01/polar.git
cd polar\PolarAndroid
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Gradle se descarga con el wrapper incluido: **no hace falta instalarlo por separado**. Si Android Studio no encuentra el SDK, crea `PolarAndroid/local.properties` con tu ruta local, usando barras `/`, por ejemplo:

```properties
sdk.dir=C:/Users/TU_USUARIO/AppData/Local/Android/Sdk
```

No copies `local.properties` de la Mac: cada equipo tiene su propia ruta. No se versiona. Para probar la app, selecciona un teléfono con depuración USB o crea un dispositivo en Device Manager y pulsa Run. El emulador requiere virtualización habilitada; puedes usar un teléfono físico si prefieres evitarlo.

APK generado: `PolarAndroid/app/build/outputs/apk/debug/app-debug.apk`. Para ejecutar las pruebas nativas, conecta **un** dispositivo/emulador y usa `.\gradlew.bat connectedDebugAndroidTest`. Más detalles en [PolarAndroid/README.md](PolarAndroid/README.md).

## Android desde macOS o Linux

Instala el SDK y JDK 21, configura la ruta local del SDK y ejecuta:

```sh
cd PolarAndroid
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Compilar Mac

En un Mac con Apple Silicon y macOS 14 o posterior, instala las Command Line Tools si no las tienes:

```sh
xcode-select --install
```

Después, desde el repositorio:

```sh
cd PolarMac
./check.sh
./build.sh
```

Se genera `Polar.app` en la raíz, con firma local. Conserva también `PolarAndroid`: el script Mac toma de ahí las fuentes tipográficas y sus licencias. [Uso y detalles Mac](PolarMac/README.md).

## Contenido del repositorio

| Ruta | Contenido |
| --- | --- |
| `PolarAndroid/` | App Android, wrapper, recursos y pruebas. |
| `PolarMac/` | App Mac, scripts, pruebas y un molde vacío de ejemplo. |
| `docs/` | Diseño, contrato de filtros, planes, verificación y guía de publicación. |

Las fotos personales, proyectos exportados con datos personales, capturas, bibliotecas, APK/APP compilados, SDK local y llaves de firma **no se incluyen en el historial Git**. Los APK release firmados se publican como archivos de GitHub Releases. En otro equipo puedes descargar esa release o compilar la app y elegir tus propias fotos. Las licencias de las fuentes incluidas están en `PolarAndroid/app/src/main/assets/licenses/`.

Para publicar en Play Store, sigue [docs/play-store.md](docs/play-store.md) y usa `bundlePlay`: esa build desactiva las actualizaciones externas. La firma de publicación es privada y obligatoria; no se usa una firma debug como alternativa. Python 3 y GitHub CLI sólo se requieren para el script que prepara/publica releases, no para desarrollar Android normalmente.

## Verificación y continuidad

[Edición y calidad 2.2.0](docs/edicion-y-calidad-2.2.0.md): fondos editables, frases con selección de fragmentos, ampliación de tarjeta, selección múltiple y diseños por hoja/tarjeta. Incluye los límites y las comprobaciones realizadas.

[Exportaciones compactas 2.1.2](docs/exportaciones-compactas.md): mediciones de cinco hojas × seis fotografías, comprobaciones de calidad/formatos y mecanismo de reducción.

[Verificación de las actualizaciones Android 2.1.1](docs/verificacion-actualizaciones-android-2026-10-02.md): 195 pruebas unitarias, 6 pruebas nativas y actualización completa desde GitHub en emulador, con conservación del diseño y modo oscuro.

[Verificación de la versión 2.1.0](docs/verificacion-simetria-filtros-2026-10-02.md): 187 pruebas Android, 6 pruebas nativas y 10 suites Mac; compilación y firma verificadas en macOS. **No se ha ejecutado la suite en un equipo Windows**. Las capturas personales referidas en ese informe permanecen locales y no aparecen al clonar.

[Contrato de filtros y compatibilidad](docs/filtros.md). Los `.polar` exportados guardan referencias a fotos y plantillas: mover sólo ese JSON a otro equipo no lleva los archivos de imagen.

La publicación inicial de `global` conserva el commit original del remoto y añade una copia limpia de la versión actual. El historial de desarrollo anterior permanece en las ramas locales, porque incluía una imagen personal y proyectos de trabajo; esas ramas antiguas no se publican.
