# Polar 2.2.0 · Edición y calidad

Android y Mac usan el mismo proyecto editable; los campos nuevos son opcionales para conservar proyectos anteriores.

## Uso

- **Encuadrar → Fondo de la foto:** quitar fondo, blanco, negro, transparente, color de la foto, otro color o fotografía importada. Borde y sombra ajustables; Restaurar fondo original conserva la foto y vuelve a mostrarla completa. El filtro se aplica a la composición final en ambas apps.
- **Texto → Editar y ver:** tarjeta ampliada, texto editable y zoom. Android admite pellizcar/desplazar y volver a 1×; Mac tiene ampliación y desplazamiento. Los diálogos respetan teclado y márgenes del sistema; con poca altura, Android coloca vista y texto lado a lado.
- **Frases sugeridas:** 120 frases originales de Polar, 10 categorías, búsqueda local y texto propio. Selecciona un fragmento en el campo y pulsa Usar selección. Son originales, no citas atribuidas a películas, canciones o libros. No se usa un servidor de frases.
- **Fotos → Seleccionar:** varias posiciones, quitar o copiar a una hoja nueva conservando encuadre, textos, filtros y diseño efectivo. Se conservan los archivos originales y Deshacer en la barra. Los avisos de edición se cierran a los 1000 ms.
- **Diseño → Aplicar diseño a:** colección, hoja o tarjeta. Se pueden combinar estilos compatibles con la misma cantidad de fotos por tarjeta. La cuadrícula permanece común a la colección para conservar posiciones. Una tira de película con cinco fotos por tarjeta requiere cambiar la colección completa; la app muestra el motivo.
- **Imprimir → Guías para recortar:** activar/desactivar y elegir marcas en esquinas o líneas completas. Se guía el borde completo de la tarjeta, incluyendo texto; no se imprimen marcos de posiciones vacías.

## Calidad y fondos

La impresión calcula el tamaño necesario para el área física, zoom y giro de cada foto a 300 ppp, limitado por el detalle que contiene el original. Android ya no limita esas fotos a 3600 píxeles ni decodifica por debajo del tamaño solicitado. Plantillas importadas usan también su tamaño físico de impresión. Una lectura fallida detiene la exportación con un error visible.

Se mantiene el PDF compacto con fotografías visibles en JPEG de calidad 94 y texto/marcos vectoriales. PDF sin compresión JPEG y PNG siguen disponibles; no se puede garantizar un tamaño pequeño para todo contenido sin compresión. Un original de poca resolución o un acercamiento fuerte sigue mostrando advertencia de calidad; ampliar el archivo no inventa detalle.

La segmentación guarda una máscara PNG de hasta 1600 píxeles y la aplica al original a resolución de impresión. No se reconstruyen rostros. El fondo transparente deja visible el diseño o papel; las hojas exportadas son hojas imprimibles, no archivos de recorte individuales. Bordes complejos, cabello y objetos superpuestos deben revisarse. Sombra y suavizado ayudan a integrar una foto de fondo; no generan nueva iluminación, perspectiva ni retoque del rostro.

Android usa [ML Kit Subject Segmentation](https://developers.google.com/ml-kit/vision/subject-segmentation/android), API beta de Google Play Services. El modelo se descarga por separado: no se incluye una red pesada en el APK. La descarga se limita a dos minutos y el procesamiento a uno; los errores conservan el original y permiten reintentar. Mac usa [Vision](https://developer.apple.com/documentation/vision/vngenerateforegroundinstancemaskrequest) integrado en macOS 14+.

## Comprobaciones

- 212 pruebas unitarias Android: exportación A3/zoom/giro, decodificación API 27/34 y EXIF, scopes/geometría, catálogo, copia, transparencia al guardar/reabrir, máscara perdida, errores y rechazo de resultados tardíos sobre fotos reemplazadas.
- 13 pruebas nativas Android aprobadas y una prueba de ML Kit omitida por requerir foto/modelo. Composición en Canvas real: blanco/negro/transparencia y filtro sobre fondo; conserva el color del sujeto.
- 11 suites Mac: proyecto/guardado/historial, importación, filtros, geometría y exportación, diseños/copia/catálogo, sujeto con alpha parcial sobre fondo opaco y guardado con máscara perdida.
- Vision probado con una fotografía local y desde la interfaz de Mac: máscara generada, fondos blanco/negro y original sin modificar. También se seleccionó un fragmento de frase y se copió una foto para aplicar otro diseño a su hoja.

Las pruebas de interfaz cubren la vista ampliada, búsqueda y fragmento de frase, copia múltiple, diseño por hoja y guías en un teléfono de 320 dp con fuente 1,3×, tablet de 1067 dp y horizontal de 731 dp. El modo claro se comprobó también en el teléfono base. Las pruebas se ejecutan con datos de revisión y conservan la biblioteca existente.

**Limitación observada:** Google Play Services no terminó de descargar el modelo en el emulador de prueba. Se comprobó el error de tiempo de espera y se corrigió la composición usando máscaras sintéticas, pero el recorte automático de Android todavía requiere comprobarse en un dispositivo donde el modelo esté disponible. No se presenta esa prueba como aprobada.

Las fotografías y capturas de revisión se mantienen locales y no se publican en Git ni en las releases. La firma release de Android conserva la llave de las versiones anteriores; Mac continúa como aplicación Apple Silicon/macOS 14+ con firma local.

## Artefactos 2.2.0 (build 6)

- `30bd2e946ca7e589c5f28186166aa15ae19e534c0679eba1bfdbce2fbe11675c  Polar-2.2.0.apk`
- `5f68363689fd385669dbecd8be99f53a0aec0567359fe6052ff2681071291461  Polar-Mac-2.2.0.zip`

Certificado Android SHA-256: `cd5a37bea894d1f30d53704caa7fe1af83afe6aa60d957951cd3bf9f07c4444e`. Lint debug/release: 0 errores, 81 advertencias pendientes.
