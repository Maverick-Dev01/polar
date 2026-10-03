# Verificación de Polar Android · 2 octubre 2026

Se utilizó un solo agente para reproducir y corregir la concurrencia del guardado. La revisión restante y las pruebas del emulador se hicieron en esta sesión. Se conservó Kotlin/Compose y las dependencias existentes.

## Correcciones

- **Autoguardados cruzados:** cancelar un Job no detenía el trabajo síncrono ya iniciado. Tres pruebas forzaron una escritura antigua que terminaba después de la última edición. Autosave, flush y salida ahora comparten una cola; flush cancela y espera el trabajo anterior. La última edición prevalece.
- **Archivos:** ProjectStore serializa guardado, lectura, listado, cambio de nombre y duplicación. Proyecto, metadatos y miniatura se reemplazan individualmente de forma atómica; los temporales se limpian incluso al fallar.
- **Salida:** la flecha y el botón Atrás esperan el guardado. Si falla por espacio o escritura, el editor conserva los cambios y permanece abierto. El indicador diferencia lo pendiente, lo guardado y el fallo.
- **Apertura repetida:** dos toques rápidos ya no apilan dos editores del mismo diseño. Se utiliza launchSingleTop de Navigation.
- **Arranque:** los ajustes se leen fuera del primer renderizado para que la pantalla de inicio no dependa del frame que está reteniendo.
- **Plantillas JPG:** la detección de huecos aplica el mismo giro/reflejo EXIF que la foto. Una prueba comprueba las ocho orientaciones.
- **Accesibilidad y texto grande:** «Nuevo diseño» tiene una etiqueta accesible; el contador y los filtros de Inicio se acomodan sin partir palabras en una columna estrecha.
- **PDF con símbolos de color:** el corazón se veía en Android y PNG, pero desaparecía en Poppler. Las líneas con emojis/símbolos compatibles con emoji se dibujan como imagen a un mínimo de 300 ppp. Las líneas normales permanecen vectoriales. La línea convertida en imagen deja de ser texto seleccionable en el PDF; sigue siendo editable en el proyecto.

## Comprobaciones

| Comprobación | Resultado |
| --- | --- |
| Suite JVM/Robolectric | 171 pruebas, 0 fallos, 0 omitidas |
| Concurrencia e I/O del agente | 38 pruebas; incluidas en la suite local |
| Teléfono original | 4 de 4 pruebas instrumentadas correctas sobre MainActivity y navegación reales, incluido doble toque al abrir |
| Pantalla compacta | 360 × 640 dp; texto al 130 %; edición/exportación y ajustes/catálogo correctos |
| Pantalla ancha | 1000 × 800 dp; edición con panel fijo, encuadre, reapertura, exportación y ajustes/catálogo correctos |
| Motor nativo | 19 diseños × 8 papeles × 2 orientaciones = 304 combinaciones dibujadas |
| Fuentes nativas | 20 fuentes en normal, negrita, cursiva y ambas: 80 cargas correctas |
| Exportación real | PDF de 2 hojas A4 horizontales de 842 × 595 puntos; PNG de 3508 × 2480 píxeles a 300 ppp |
| Huecos vacíos | Segunda hoja con una foto: no imprime los otros tres marcos/textos; revisión visual y píxeles blancos |
| Corazón en PDF externo | Regresión reproducida con 0 píxeles rojos; corregida con 1222 píxeles en el mismo ejemplo |
| Android lint | 0 errores; 55 avisos de mantenimiento, recursos y traducción |
| Release | AAB optimizado con R8 generado; certificado Android Debug, para pruebas |
| Alineación nativa | Librerías ELF con segmentos PT_LOAD de 16 KB; APK comprobado con zipalign -P 16 |

Emulador: **mercatto_test / emulator-5554**, arm64, Android 14/API 34. Se restablecieron la resolución física de 1080 × 2400, densidad 420 y escala de texto 1.0. La interfaz se probó mediante sus controles de accesibilidad. La herramienta de ventanas no reconoce QEMU, por lo que no hubo un recorrido táctil manual.

Los recorridos verifican cinco imágenes sintéticas importadas mediante PhotoImporter, dos hojas, giro/encuadre, texto, deshacer/rehacer, papel/orientación, recreación de Activity, salida/reapertura, ajustes, catálogo y exportación. La prueba de edición también abre el proyecto dos veces rápidamente para detectar un editor duplicado.

## Archivos y reproducción

- [APK actualizado](/Users/cattaherrrera/Downloads/Polar/Polar.apk), instalado en el emulador.
- [PDF de ejemplo](/Users/cattaherrrera/Downloads/Polar/outputs/android-qa-2026-10-02/ejemplo-2-hojas.pdf) y [PNG de la segunda hoja](/Users/cattaherrrera/Downloads/Polar/outputs/android-qa-2026-10-02/ejemplo-hoja-2-300ppp.png), con fotos sintéticas.
- [Prueba instrumentada](/Users/cattaherrrera/Downloads/Polar/PolarAndroid/app/src/androidTest/java/com/polar/app/DeviceSmokeTest.kt).
- [Regresión con un visor externo](/Users/cattaherrrera/Downloads/Polar/PolarAndroid/scripts/verify_pdf_emoji.py).
- [Resultado final del emulador](/Users/cattaherrrera/Downloads/Polar/outputs/android-qa-2026-10-02/pruebas-emulador.txt): 4 pruebas, 0 fallos.

Desde PolarAndroid, con JDK 21 y el SDK configurados:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
./gradlew connectedDebugAndroidTest
```

La comprobación externa usa Python con Pillow y Poppler (pdftoppm):

```sh
python3 scripts/verify_pdf_emoji.py ../outputs/android-qa-2026-10-02/ejemplo-2-hojas.pdf
```

## Límites y publicación

La carrera reproducida está corregida. Un cierre forzado del proceso antes del próximo guardado puede perder cambios aún pendientes. Los tres archivos son atómicos por separado, pero no constituyen una transacción conjunta ante un corte abrupto o disco lleno.

Falta validar dispositivos físicos, Android API 26 y 36, memoria limitada con muchas fotos y PNG de papeles grandes, selector de galería/almacenamiento de distintos fabricantes, aplicaciones receptoras e impresora real. «Guardar documento», compartir e imprimir no se verificaron de extremo a extremo con proveedores externos.

El AAB actual está firmado con la llave de depuración y **no es el paquete para publicar**. Antes de Play Store hacen falta la firma definitiva, ficha/privacidad y verificaciones de Play Console. El proyecto utiliza targetSdk 36, conforme al [requisito actual de Google Play](https://support.google.com/googleplay/android-developer/answer/11926878). Se comprobó la alineación estática de 16 KB; falta ejecución en un dispositivo con páginas de 16 KB, según la [guía oficial](https://developer.android.com/guide/practices/page-sizes).

El intercambio .polar guarda referencias de las fotos; no empaqueta las imágenes. Al abrirlo en otro dispositivo puede ser necesario agregarlas de nuevo, con el aviso correspondiente.

No se publicó la aplicación ni se modificaron las fotografías personales.
