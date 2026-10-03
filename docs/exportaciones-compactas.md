# Exportaciones compactas · Polar 2.1.2

## Causa y corrección

Android PdfDocument guardaba las fotos como RGB con Flate, una compresión sin pérdida poco eficaz para fotografías con ruido; además podía incluir píxeles fuera del encuadre. Una foto de prueba ocupaba 2,462,747 bytes, con una imagen de 2,391 KiB a 443 ppp. Mac ya usaba JPEG 94, pero los encuadres neutros/zoom podían conservar partes invisibles del original.

Ambos motores ahora rasterizan solamente la fotografía visible a 300 ppp. El PDF conserva texto y guías vectoriales. Android identifica exactamente los RGB opacos de fotografías del motor y reemplaza sólo esos streams por JPEG 94, reconstruyendo offsets/xref; QR, emojis, moldes y transparencia se conservan sin cambios. No se interpreta contenido binario mediante separadores de texto. Un formato nativo no reconocido, una imagen incompatible o una optimización que no reduzca tamaño conserva la salida PDF válida sin optimizar. Mac usa el encoder nativo de ImageIO.

La decodificación Android para imprimir también se corrigió: el muestreo por potencias de dos ya no entrega imágenes menores que la resolución solicitada. Las vistas previas mantienen su comportamiento anterior.

## Elegir formato

- **PDF**: todas las hojas, fotografías a 300 ppp con JPEG 94 de alta calidad; texto, marcos y guías nítidos.
- **JPG**: sólo la hoja seleccionada a 300 ppp; incluye dimensiones físicas en metadatos. Carta vertical produce 2550×3300 píxeles. Conviene para enviar fotografías con menos peso.
- **PNG**: la hoja seleccionada a 300 ppp, compresión sin pérdida.
- **PDF sin compresión JPEG**: todas las hojas, sin la codificación JPEG adicional de las fotos renderizadas; puede pesar más.

JPEG introduce una pequeña pérdida de información: conservar 300 ppp no equivale a compresión matemáticamente sin pérdida. Para evitar esa pérdida adicional, están las dos opciones anteriores. Las fotos originales y el proyecto editable nunca se modifican. Exportar a 300 ppp tampoco crea detalles ausentes en una foto de baja resolución. El tamaño depende del ruido, grano, número y tamaño físico de fotos y de las plantillas; no se fija un límite de peso del PDF que reduzca automáticamente sus 300 ppp.

## Mediciones locales

Mismo papel Carta y distribución de cinco hojas con seis fotografías por hoja. Los MB de esta tabla son decimales. Son comparaciones contra el modo sin JPEG con los mismos píxeles visibles, no una promesa sobre cualquier archivo ni el archivo de 46 MB referido por el usuario.

| Motor / archivo | Sin JPEG / PNG | PDF compacto / JPG | Reducción |
| --- | ---: | ---: | ---: |
| Android PDF, 30 encuadres/filtros distintos de una fotografía sintética |28,289,328 bytes|4,736,378 bytes|83.3%|
| Android imagen, hoja 1 | 4,326,853 bytes PNG|1,106,954 bytes JPG|74.4%|
| Mac PDF, 30 fotografías locales distintas |28,875,022 bytes|5,048,906 bytes|82.5%|
| Mac imagen, hoja 1 | 5,465,175 bytes PNG|1,271,707 bytes JPG|76.7%|

Poppler confirmó cinco páginas Carta (612×792 pt), treinta fotografías JPEG a 300×300 ppp y texto seleccionable en ambos PDF compactos. Los archivos de prueba con fotos personales permanecen locales, excluidos de Git y Releases.

La comparación de la primera hoja sintética Android renderizada a 300 ppp dio una diferencia absoluta media de 1.023 niveles RGB y PSNR 45.28 dB sobre píxeles no blancos. Se inspeccionaron ambos renderizados: el encuadre, los títulos y las guías coinciden. Este dato describe esa prueba; no garantiza una métrica universal.

## Resultados de comprobación

- Android: 200 pruebas unitarias Debug sin fallos/errores/omisiones; lintDebug y lintRelease sin errores y 70 advertencias en cada variante.
- Emulador Android 14/API34: 11 comprobaciones nativas verificadas. Diez pasaron en el recorrido completo; una captura PixelCopy del catálogo agotó su espera y pasó al repetir esa prueba sola sin cambiar código. Las cinco nuevas de exportación pasaron en el primer recorrido.
- Mac: 10 suites completas aprobadas y app 2.1.2/build 5 compilada, firma local verificada y ZIP íntegro.
- Se intentó adicionalmente `testReleaseUnitTest`: 182 pruebas pasaron y 18 de Compose fallaron al arrancar por ausencia de `androidx.activity.ComponentActivity` en el manifiesto de producción. La configuración actual mantiene `ui-test-manifest` sólo en Debug, siguiendo el [patrón oficial de Compose](https://developer.android.com/develop/ui/compose/testing/common-patterns). Es un límite del arnés de pruebas Release: no se añade una actividad de pruebas al APK distribuido. Las mismas 18 pruebas UI pasaron en Debug.

No se verificó impresión física ni se ejecutaron estos recorridos en un teléfono real o Windows.

## Verificación reproducible

Android: `./gradlew testDebugUnitTest lintDebug connectedDebugAndroidTest` con JDK 21, SDK 36 y un emulador/dispositivo conectado. Las pruebas nuevas cubren JPEG real dentro del PDF, offsets/xref y binario con marcadores de texto, fallback de PDF incompatible, muestreo de impresión, selección de hoja/MIME, modo sin JPEG, cinco páginas que vuelven a abrir/renderizar, PNG/JPG 300 ppp, originales intactos, QR legible, transparencia y plantilla importada.

Mac: `./PolarMac/check.sh` y `./PolarMac/build.sh`. Las comprobaciones del renderer cubren 300 ppp con zoom, texto seleccionable, PDF sin JPEG, JPG con DPI/tamaño/peso, QR legible, EXIF/rotación, papel, textos, espacios vacíos, transparencia, moldes y protección de originales.

La publicación utiliza la misma llave release Android que 2.1.1 y código 5; el ZIP Mac contiene `Polar.app` 2.1.2/build 5 con firma local para Apple Silicon/macOS 14+. El actualizador Android usa el `update.json` de la release marcada Latest. Mac abre la página de descargas desde Ayuda; reemplazar la app con Polar cerrada conserva la biblioteca.

## Archivos de publicación

- APK `Polar-2.1.2.apk`: 4,531,523 bytes; SHA-256 `31e774b079ae9104bffadbd1b7d08d9aaa854c16f2ff2e14b4a6c25cadb48661`.
- ZIP `Polar-Mac-2.1.2.zip`: 3,469,109 bytes; SHA-256 `ae0396b70bfa88945db364fb86ad843a1d76d3eeadeb935a5a79d6f19c7cf63f`.
- Certificado release Android SHA-256 `cd5a37bea894d1f30d53704caa7fe1af83afe6aa60d957951cd3bf9f07c4444e`, igual que 2.1.1; `apksigner` verifica el APK. Se comprobó versión 2.1.2/código 5, paquete de producción y SDK 26 mínimo/36 target.
- Mac: ejecutable arm64, plist 2.1.2/build 5, `codesign --verify --deep --strict` y contenido/integridad ZIP comprobados. La app se reabrió con su biblioteca intacta y los nuevos formatos aparecen en el menú Imprimir.
