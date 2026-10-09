# Polar para Mac

Abre **Polar.app** en la carpeta Polar. Versión **2.2.0**; macOS 14 o posterior, Mac con Apple Silicon. Si ya estaba abierta, ciérrala y vuelve a abrirla para cargar esta actualización. [Descarga el ZIP de la app](https://github.com/Maverick-Dev01/polar/releases/download/android-v2.2.0/Polar-Mac-2.2.0.zip) o compila al clonar. En Ayuda → Descargar última versión puedes abrir las publicaciones. La app tiene firma local, sin notarización de Apple; macOS puede pedir habilitar su apertura desde Privacidad y seguridad.

En **Tus diseños** puedes buscar, ordenar, cambiar nombres, duplicar para otro pedido, compartir un .polar y borrar con **Deshacer**. Usa **Nuevo diseño** para empezar. La app guarda automáticamente aproximadamente un segundo después de editar y antes de cambiar de diseño o cerrar. **Guardando…**, **Guardado** y **Sin guardar** muestran el estado; si falla la escritura se mantiene abierto el editor.

1. Elige uno de los **19 diseños** o importa una plantilla. Puedes buscarlos por nombre.
2. Agrega fotos o una carpeta; también puedes arrastrarlas a la galería.
3. Haz clic en un espacio de la hoja y después en una foto para reemplazarla.
4. En **Fotos → Encuadrar**, arrastra o acerca la foto, gira, llena o ajusta el marco; usa el anillo o el ajuste fino. En **Filtros**, elige Original, Blanco y negro, Película, Sepia, Cálido, Frío, Desvanecido o Vivo para todo el proyecto, una página, varias páginas o una foto; ajusta intensidad/luz y compara manteniendo pulsado o con Espacio. En **Texto**, elige **Todas las tarjetas** o **Sólo esta tarjeta**; edita frases, las **20 fuentes incluidas** y las instaladas en tu Mac, tamaño, color, negrita, cursiva, alineación y posición. **Volver al texto general** elimina la personalización de esa parte. Puedes ocultar cada texto. **Fecha** admite la fecha de captura de la foto, una fecha elegida o ninguna, con tres formatos. El tamaño automático adapta las letras al diseño; un tamaño manual también se reduce si hace falta para no cubrir la foto. Deshacer/rehacer conserva hasta 50 pasos y agrupa texto y gestos.
5. En **Diseño**, cambia color y distribución: 1, 2, 4, 6, 8, 9, 12 o 16 tarjetas por hoja, formato vertical, horizontal o cuadrado. Las tiras de película reúnen cinco fotos cada una. Los estilos para parejas, amigos, familia, mascotas, viajes y celebraciones cambian fuentes y colores conservando tus frases; **Usar frases sugeridas** las reemplaza explícitamente.
6. **Rellenar todo** distribuye tu galería en tantas hojas como haga falta. Los espacios vacíos se ven al editar, pero no imprimen marco, texto ni marcas de corte.
7. En **Papel**, elige Carta, Oficio (216 × 340 mm), Legal (8.5 × 14 pulgadas), A4, A3, foto 4 × 6, foto 5 × 7 o medidas personalizadas de 80 a 600 mm. La hoja puede ser vertical u horizontal.
8. **Imprimir** abre **Terminar**, con resumen de hojas y fotos, espacios vacíos y avisos de resolución. Desde ahí imprime con el diálogo de macOS, comparte el PDF o guarda **PDF** con todas las hojas o **JPG/PNG** de la hoja actual. Elige la **Calidad** de exportación: **Ligero** (fotos a 200 ppp, JPEG 85), **Alta** (300 ppp, JPEG 94, recomendada) o **Máxima** (300 ppp sin recomprimir, más peso). La preferencia es de la app, no del archivo .polar. Las fotos del PDF se guardan sólo con los píxeles visibles, conservando texto y guías vectoriales. **PNG** evita pérdidas de compresión, con más peso. Las fotos con resolución baja (< 150 ppp) o aceptable (< 220 ppp) se avisan en Encuadrar y en Terminar.

**Ajustes** permite tema Sistema/Claro/Oscuro, milímetros/pulgadas, papel predeterminado para nuevos diseños y volver a ver la bienvenida. También muestra **Vaciar papelera (N)**: los diseños borrados se eliminan solos tras 7 días. La hoja permanece blanca en todos los temas porque representa el papel.

Para imprimir: el mismo tamaño y orientación elegidos en **Papel**, **Tamaño real / 100 %**. El margen evita imprimir junto al borde. Las marcas en esquinas quedan fuera de las tarjetas; también puedes usar líneas completas o desactivar las guías. El borde de la tarjeta se imprime sólo si activas esa opción. La calidad depende del tamaño y la resolución de las fotos originales; exportar a 300 ppp no recupera detalles que la foto no tenga.

La biblioteca guarda copias de las fotos y plantillas en **~/Library/Application Support/Polar**, para que mover los originales no rompa el diseño. **Guardar diseño** exporta un .polar editable, incluyendo tipografía, papel, huecos importados, fechas y textos por tarjeta. Los proyectos de la versión anterior y de Android siguen siendo compatibles. El archivo .polar exportado guarda referencias: para usarlo en otro equipo hay que llevar también sus fotos y plantillas. Los originales nunca se modifican. La aplicación funciona sin conexión; compartir sólo ocurre cuando tú lo eliges.

## Importar un molde

Usa **Importar plantilla…** para abrir una imagen (por ejemplo PNG o JPEG). La aplicación detecta huecos rectangulares blancos o transparentes que no toquen el borde. Revisa el resultado; en **Diseño**, activa **Editar los huecos** para arrastrarlos, cambiar sus medidas o añadir y quitar espacios. Luego desactívalo y coloca fotos desde la galería.

Si la imagen contiene fotos en lugar de huecos claros, se crea un espacio manual para que lo ajustes. La detección no identifica cualquier collage ni reconstruye máscaras irregulares. El texto y adornos ya impresos en el archivo forman parte de esa imagen. La plantilla mantiene su proporción y se centra dentro del papel. Los huecos vacíos no añaden marcos o textos, aunque los elementos que ya estén dibujados en la plantilla permanecen.

Hay un molde de ejemplo en **Examples/Plantilla - dos recuerdos.png**. Los diseños Polaroid, Postal, Botánico, Celebración, Mi mascota, Corazón y Editorial tienen sus propios elementos editables.

Foto + canción admite un enlace opcional: genera un QR real hacia ese enlace. Los estilos de reproductor son diseños impresos; no buscan canciones en Spotify.

**Mi diseño** permite crear otra distribución y conservarla como proyecto.

## Compilar y comprobar

~~~sh
cd PolarMac
./check.sh
./build.sh
~~~

No requiere Xcode completo ni dependencias descargadas: basta con Command Line Tools. La compilación crea ../Polar.app y la firma localmente.

Stack: Swift, SwiftUI, AppKit, Core Graphics, ImageIO y Core Image. El script usa `swiftc -swift-version 5` y compila para `arm64-apple-macos14.0`. Requiere macOS; desde Windows puedes editar fuentes, pero compilar y ejecutar esta app necesita un Mac. Android sí puede desarrollarse desde Windows: [guía principal](../README.md).
