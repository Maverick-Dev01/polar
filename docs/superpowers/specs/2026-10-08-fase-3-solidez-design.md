# Polar · Fase 3: solidez, paridad, diseños, moldes y guía

**Fecha:** 2026-10-08 · **Rama:** `fase-3-solidez` (desde `codex/photo-studio`, versión 2.2.0 build 6)

**Fuente:** auditoría de paridad del 2026-10-08 y lo que pidió el usuario: dos apps sólidas e idénticas en funciones; navegación cómoda; simetría sin elementos encimados; más diseños; importar molde con reglas claras y detección de duplicados; exportación sin pixelado y ligera; guía de uso animada; lista para Play Store y con un modelo de negocio sin anuncios. La app web queda **fuera** de esta fase y irá al final.

**Aprobación:** el usuario aprobó el orden de los subproyectos y pidió no detenerse («comienza… no pares hasta que termines»). Las decisiones de este documento las tomó el agente y quedan registradas como *Decisión* para que el usuario las revise.

## Restricciones globales

- **Paridad:** toda función de usuario existe en Android y Mac con el mismo nombre, el mismo lugar lógico y el mismo resultado impreso. Las excepciones solo pueden ser de plataforma y quedan listadas en la sección 6.
- **Formato del proyecto:**
  - `.polar` sigue siendo compatible en ambos sentidos;
  - los campos nuevos son opcionales, con un valor por defecto que no cambia el resultado de los proyectos anteriores;
  - las claves desconocidas se ignoran.
- **Identidad:**
  - colores crema y vino, títulos en Gelasio;
  - temas Sistema, Claro y Oscuro;
  - la hoja siempre es blanca;
  - todos los textos en español; en Android, en `strings.xml`, sin literales en el código.
- **Calidad:**
  - TDD en la lógica;
  - Android: `./gradlew testDebugUnitTest lintDebug assembleDebug` con 0 fallos y 0 errores de lint;
  - Mac: `./check.sh && ./build.sh` en verde.
- **Diseño:**
  - simetría (tarjetas de la misma altura, filas de botones del mismo ancho y alto, `heightIn`/`minHeight` en vez de alturas fijas);
  - nada encimado con letra al 130 %, en teléfono de 360 dp y en ventana Mac de 1100×720.
- **Git:**
  - identidad José Catalino <josecatalino.code@gmail.com>;
  - commits en español;
  - sin Co-Authored-By ni ninguna mención a IA;
  - sin push.

---

## Subproyecto 1 · Exportación y limpieza

### 1.1 PDF ligero en Android
- **Problema:** `PdfPhotoFingerprint.fromBitmap` descarta capas con píxeles no opacos. Las fotos en «Ajustar» (con franjas), con fondo transparente o con esquinas redondeadas quedan como RGB+SMask sin JPEG.
- **Decisión:** antes de entregar la capa al optimizador, componerla sobre el color opaco que realmente hay debajo de la foto en esa tarjeta (color de la tarjeta o papel blanco). Así queda opaca y es apta para JPEG, igual que hace Mac (`Renderer.swift` `jpegForPDF`). Si el área de la foto se superpone a un molde importado o a un fondo no uniforme, la capa conserva la transparencia; ese caso queda fuera de la optimización y se documenta.
- **Pruebas:** un PDF de una hoja con 9 fotos en «Ajustar» pesa menos de la mitad que hoy, y las fotos se ven iguales: comparación de píxeles a 72 ppp con tolerancia.

### 1.2 Calidad de reescalado
- **Android API 26–27:** reemplazar el `createScaledBitmap` en un solo paso por una reducción progresiva a la mitad, con filtro, hasta el tamaño final.
- **Ambas plataformas:** la capa de la foto se dibuja al tamaño exacto requerido, sin un segundo reescalado al componer.
- **Mac:** `interpolationQuality = .high` también en la ruta con filtro, fondo o PDF.

### 1.3 Avisos de resolución en tres niveles (ambas apps)
- **Nivel 1, menos de 150 ppp, «Baja»:** rojo. Ya existe.
- **Nivel 2, entre 150 y 220 ppp, «Aceptable»:** ámbar. Nuevo.
- **Nivel 3, 220 ppp o más:** sin aviso.
- El umbral es una constante compartida (`PhotoFit.FAIR_DPI = 220`).
- Terminar muestra «N fotos con resolución baja o aceptable» y abre una lista para encuadrarlas.

### 1.4 Calidad de exportación seleccionable (Terminar, ambas apps)

| Opción | Fotos | Uso |
|---|---|---|
| **Ligero** | 200 ppp, JPEG 85 | WhatsApp y correo |
| **Alta** (por defecto) | 300 ppp, JPEG 94 | Imprimir; es el modo actual |
| **Máxima** | 300 ppp, sin compresión JPEG (Flate) | Impresión profesional; reemplaza el menú «PDF sin compresión JPEG» |

- Se aplica a PDF y JPG; PNG siempre es sin pérdida.
- Se guarda en los ajustes como preferencia, no en el proyecto.
- Pruebas: los tres modos producen tamaños estrictamente decrecientes (Máxima > Alta > Ligero).

### 1.5 QR robusto
- Si el enlace no cabe en un QR (versión 40, corrección M), el panel muestra en el momento el aviso «El enlace es demasiado largo para un QR», y la vista previa y la exportación dibujan un marco con «Enlace muy largo» en lugar de no dibujar nada (Android) o fallar (Mac).
- Se validan solo enlaces `http(s)` o texto plano.

### 1.6 Limpieza de Mac
- Quitar el menú «Fotitos» y la carga automática de `Fotitos/Mejoradas` (`PolarApp.swift:286`, `Studio.swift:129, 579`).
- Quitar el respaldo de fuentes en `#filePath` (`FontCatalog.swift`).
- **Papelera:**
  - al iniciar, eliminar del disco los proyectos de `trash/` con más de 7 días;
  - en Ajustes, agregar «Vaciar papelera (N diseños)»;
  - aplicar lo mismo en Android (`ProjectStore.emptyTrash` ya existe; llamarlo con la misma regla de 7 días).
- Versiones: Ajustes lee `CFBundleShortVersionString`, sin respaldo fijo; actualizar el README de Mac a 2.2.0.
- El botón «Comparar» de Mac funciona al pulsarlo (alterna mientras está presionado); hoy tiene acción vacía.

### 1.7 Limpieza de Android
- Pasar los ~32 literales en español a `strings.xml`: `PhraseDialog`, `BackgroundControls`, `TextEditDialog`, `BatchPhotosPanel`, `EditorScreen`, `PhotosPanel`, `FinishScreen`.

---

## Subproyecto 2 · Paridad y navegación

### 2.1 Funciones que faltan en Android
- **Agregar una carpeta:**
  - en Fotos → «Agregar carpeta», con `ACTION_OPEN_DOCUMENT_TREE`;
  - importa las imágenes de la carpeta en orden por nombre, sin subcarpetas y con un tope de 500;
  - mantiene el selector de fotos del sistema para elegir sueltas.
- **Buscar diseños** en el catálogo: campo de búsqueda arriba de las categorías, que filtra por nombre y descripción.
- **Importar molde en un proyecto existente:** Diseño → «Usar mi molde» (lleva al flujo del subproyecto 3).
- **Margen en las unidades elegidas** (mm o pulgadas), igual que en Mac.
- **Papel predeterminado** con los 8 tamaños.

### 2.2 Funciones que faltan en Mac
- Deslizar entre hojas con el trackpad (dos dedos) y con las flechas ← → cuando el foco está en la hoja.

### 2.3 Sacar a la vista lo escondido (ambas)
- **Panel Texto:**
  - con un diseño musical (Foto + canción), la sección **«Canción»** (título, artista y enlace del QR) va **primero**;
  - «Mostrar este texto» y «Tamaño» (Auto/S/M/L y el control exacto) salen de «Más opciones».
- **Barra superior del editor en Android:**
  - Atrás · nombre y estado · Deshacer · **Rehacer** · ⋮ · **«Imprimir»** (botón con texto e ícono);
  - en anchos menores de 400 dp, Rehacer pasa al menú.
- **Quitar fondo:** además de estar en Encuadrar, aparece como acción directa en el menú contextual de la tarjeta (Cambiar · Encuadrar · **Quitar fondo** · Girar · Texto) en Android, y en Fotos en Mac.
- **Papel:** margen y «Borde de las tarjetas» visibles sin «Más opciones».

### 2.4 Pulido visual y de simetría (ambas)
- Auditoría con la skill **Impeccable** (`audit` → `polish`) de cada pantalla: Inicio, Catálogo, Editor y sus 5 paneles, Encuadrar, Terminar, Ajustes y bienvenida.
- **Corregir:**
  - alturas fijas que recortan;
  - filas de botones de distintos anchos;
  - textos que se cortan a media palabra;
  - controles encimados;
  - radios y espaciados fuera de la escala de tokens.
- **Evidencia:** capturas antes y después en Android (360 dp, 411 dp, tablet 1280×800, claro y oscuro, letra 130 %) y en Mac (1100×720 y 1600×1000).

### 2.5 Excepción de plataforma aceptada
**Decisión:** Mac ofrece además las fuentes instaladas en el sistema. Android no puede enumerarlas de forma fiable, así que no tiene equivalente. Ambas comparten las 20 fuentes incluidas, y un proyecto con una fuente de sistema de Mac se abre en Android con Gelasio como respaldo. Ya ocurre hoy; se documenta.

---

## Subproyecto 3 · Diseños nuevos e importar molde v2

### 3.1 Seis diseños nuevos (ambas apps, mismo renderizado)

| id | Nombre | Categoría | Fotos por tarjeta | Descripción |
|---|---|---|---|---|
| `photobooth` | Fotomatón | Clásicos | 4 | Tira vertical de 4 fotos con pie de texto, como cabina de fotos |
| `instaxWide` | Instantánea ancha | Clásicos | 1 | Formato horizontal ancho (proporción tipo Wide) con pie |
| `vinyl` | Vinilo | Música | 1 | Disco negro con la foto como etiqueta circular, título y artista; QR opcional |
| `cassette` | Casete | Música | 1 | Casete con la foto en la ventana de la etiqueta, título y artista; QR opcional |
| `magazine` | Portada de revista | Libre | 1 | Foto a sangre, cabecera grande con el título, subtítulos tipo titular |
| `washi` | Cinta washi | Ocasiones | 1 | Foto con borde blanco y dos tiras de cinta decorativa en las esquinas, pie manuscrito |

- **Decisión:** el QR se habilita en todos los diseños musicales (`spotify`, `playerRed`, `playerGray`, `vinyl`, `cassette`), siempre que exista el campo de enlace.
- Cada diseño tiene una miniatura en el catálogo, aparece en la búsqueda y es compatible con filtros, fondo y texto por tarjeta.
- **Pruebas de renderizado:** cada diseño × 3 papeles × 2 orientaciones dibuja sin errores. Una prueba de paridad compara la geometría de Android y Mac (los huecos de foto y de texto en puntos) con tolerancia de 0.5 pt.

### 3.2 Importar molde v2
El flujo es el mismo en ambas apps: un asistente de 3 pasos.

1. **Elige la imagen.** PNG con transparencia o JPG/PNG con espacios blancos.
   - Una explicación corta con ilustración: «Los espacios **transparentes** o **blancos** de tu imagen serán los lugares de las fotos. El resto (texto, adornos) se imprime tal cual».
2. **Revisa los espacios detectados.**
   - Se ven numerados sobre la imagen.
   - Puedes arrastrar para mover, usar las asas para cambiar el tamaño, «＋ Agregar espacio» y «Quitar».
   - Forma por espacio: **Rectángulo**, **Redondeado** (con radio) u **Óvalo**. La detección propone la forma: si el área llena es menor al 90 % del rectángulo, prueba con elipse y con rectángulo redondeado y elige la que mejor encaja.
3. **Nombre y guardar.** El molde queda en **«Mis moldes»**, una biblioteca reutilizable entre proyectos. Se usa en el proyecto actual o en uno nuevo.

**Detección de duplicados:**
- Se calcula una huella perceptual (**dHash de 64 bits** de la imagen a 9×8 en gris) y además el SHA-256 del archivo.
- Si el SHA coincide, o la distancia de Hamming del dHash es de 6 o menos respecto a un molde guardado, el asistente avisa: «Este molde se parece a "Nombre" que ya guardaste» con dos opciones: **Usar el existente** · **Guardar como nuevo**.

**Modelo:**
- `TemplateRegion` gana `shape: "rect" | "round" | "ellipse"` (por defecto `rect`) y `radius` (fracción, por defecto 0).
- «Mis moldes» se guarda en la biblioteca (`templates/<id>/molde.png` + `meta.json` con nombre, dHash, SHA, regiones y fecha).
- El `.polar` sigue llevando su copia del molde, así que es portable.

**Pruebas:**
- detección de formas (rect, redondeado, óvalo) con imágenes sintéticas;
- dHash estable ante reescalado y JPG, y distinto entre moldes diferentes;
- duplicados por SHA y por dHash;
- el recorte por forma al renderizar.

---

## Subproyecto 4 · Guía de uso dentro de la app

1. **Recorrido inicial**, la primera vez que abres el editor y repetible desde Ayuda.
   - 5 pasos con resaltado del control real y fondo atenuado alrededor:
     - Fotos («Pon tus fotos aquí»);
     - la hoja («Toca una tarjeta para editarla»);
     - Texto;
     - Filtros;
     - Imprimir.
   - Cada paso tiene una animación corta (mano o cursor que toca, tarjeta que cambia).
   - Botones Siguiente / Saltar.
   - Respeta «reducir movimiento».
2. **Modo «?»:** en la barra superior, al activarlo, tocar cualquier control muestra una burbuja con qué hace. Se sale con «Listo».
3. **Centro de ayuda:** pantalla desde Ajustes y menú Ayuda.
   - Unos 20 artículos breves con búsqueda, en 5 categorías: Empezar, Fotos, Texto, Diseño y moldes, Imprimir.
   - Cada artículo tiene una mini animación ilustrativa y un botón «Llévame ahí» que abre la pantalla correspondiente.
   - **Contenido compartido:** un único `help.json` (id, categoría, título, cuerpo, pasos, destino) igual en ambas apps. Una prueba verifica que todos los destinos existen.

---

## Subproyecto 5 · Preparación de lanzamiento (sin cuentas)

- **Decisión:** el cobro (Polar Pro) se implementa cuando el usuario tenga la cuenta de Play Console y los IDs de producto.
  - En esta fase se deja una capa de «derechos» (`Entitlements`) que hoy devuelve todo desbloqueado, para no bloquear ninguna función.
  - Se documenta qué se volvería Pro. Propuesta: diseños nuevos de temporada, quitar fondo, Mis moldes ilimitados (gratis hasta 3) y calidad Máxima.
- `docs/play-store.md` actualizado con:
  - la build `play` (sin actualizador propio);
  - la prueba cerrada (12 testers × 14 días);
  - el formulario de Seguridad de datos;
  - la política de privacidad publicada en GitHub Pages (`docs/privacidad.md`), con los pasos;
  - los recursos de la ficha y el texto de la ficha.

## 6. Diferencias aceptadas entre plataformas
- Fuentes del sistema solo en Mac (2.5).
- Atajos de teclado solo en Mac; gestos de pellizco solo en Android.
- Actualizador por GitHub solo en el APK de GitHub de Android; la build `play` no lo incluye.

## 7. Fuera de alcance
- Versión web.
- Cobro real.
- Idiomas además del español.
- Fuentes descargables.
- Sincronización en la nube.
