# Especificación de Diseño: Polar para Android

**Fecha:** 2026-10-01  
**Autor:** Antigravity (Senior Android Engineer)  
**Estado:** Aprobado para implementación  

---

## 1. Resumen Ejecutivo
Portar la aplicación **Polar** de macOS a **Android**, creando un proyecto nativo en **Kotlin** con **Jetpack Compose** y **Material 3**, listo para compilar en **Android Studio** y empaquetar como APK instalable y distribuible.

La aplicación permite componer e imprimir recuerdos fotográficos usando 19 estilos prediseñados (Polaroid, Mini, Canción con QR, Boletos vintage, Tiras de película, Calendarios, Instagram, etc.) o plantillas personalizadas. Exporta hojas en **PDF multipágina** y **PNG a 300 ppp** con medidas exactas para impresión física (Carta, Oficio, A4, etc.).

---

## 2. Metas y Requisitos

### 2.1 Requisitos Funcionales
1. **19 Estilos de Diseño + Plantillas Importadas**:
   - Polaroid, Instantánea mini, Foto + canción (con QR funcional), Reproductor rojo, Reproductor horizontal, Boleto vintage, Película vertical (5 fotos), Película horizontal (5 fotos), Calendario (días y mes reales dinámicos), Instagram, Mi diseño, Sin bordes, Cuadrada, Postal, Botánico, Celebración, Mi mascota, Corazón, Editorial y Plantilla importada.
2. **Detección Automática de Huecos en Plantillas**:
   - Detección de zonas rectangulares transparentes o blancas en imágenes externas (BFS de conectividad de píxeles), ordenación de lectura por filas y ajuste táctil manual.
3. **Gestión de Fotos**:
   - Selector moderno `ActivityResultContracts.PickMultipleVisualMedia` (Android PhotoPicker nativo sin permisos intrusivos).
   - Ajuste de encuadre en cada ranura: Zoom (1x a 4x), desplazamiento horizontal y vertical (pan táctil o sliders), rotación por cuartos de vuelta (90°/180°/270°).
   - Detección de orientación EXIF automática.
   - Cálculo y advertencia de resolución efectiva (DPI) para evitar fotos borrosas en impresión.
4. **Tipografía y Estilos de Texto**:
   - Roles de texto: Título, Subtítulo, Pie de foto, Canción, Artista.
   - Personalización de tamaño (con auto-fit para no solapar la foto), color hexadecimal, negrita, cursiva, alineación (izquierda, centro, derecha) y visibilidad.
5. **Papeles y Geometría de Impresión**:
   - Tamaños: Carta (8.5×11"), Oficio (216×340 mm), Legal (8.5×14"), A4, A3, 4×6", 5×7" y Personalizado (80 a 600 mm).
   - Orientación vertical y horizontal.
   - Márgenes y separación entre tarjetas configurables.
   - Marcas de corte en esquinas o líneas completas punteadas.
   - Bordes opcionales de tarjeta y fotos redondeadas.
6. **Exportación**:
   - PDF vectorial multipágina a escala 100% real para imprimir.
   - PNG a 300 ppp de la hoja actual.
   - Guardado mediante `MediaStore` en Descargas/Imágenes + Hoja de Compartir (`Intent.ACTION_SEND` / `ACTION_VIEW`).
7. **Persistencia de Proyectos**:
   - Guardar y abrir proyectos `.polar` compatibles con la versión de Mac (formato JSON estructurado).

### 2.2 Requisitos de UX / UI Mobile & Responsive
1. **Identidad Visual**:
   - Color principal Tinta: `#7A293B` (pantone burdeos clásico).
   - Fondo Crema: `#F7F2EB`.
   - Tarjetas blancas limpias con elevación sutil y tipografía refinada (estilo Georgia / Roboto Slab).
2. **Adaptación según tamaño de pantalla (WindowSizeClass)**:
   - **Teléfonos (Compact / Medium)**:
     - Lienzo de la hoja centrado con interacción táctil (zoom con dos dedos, arrastre).
     - Barra de navegación inferior ergonómica:
       - *Diseño*: Selector visual de las 19 plantillas con iconos y vista previa.
       - *Fotos*: Carrusel/cuadrícula de fotos cargadas, botón para añadir más fotos, "Rellenar todo" y "Vaciar hoja".
       - *Ajustes*: Inspector modal o deslizable organizado en pestañas (Foto, Texto, Papel, Moods).
       - *Exportar*: Acceso rápido con selección de PDF/PNG y compartir instantáneo.
   - **Tablets / Pantallas Grandes (Expanded)**:
     - Disposición de 3 columnas simultáneas similar a la versión Mac: Catálogo a la izquierda, Lienzo + Galería en el centro, Inspector a la derecha.
3. **Accesibilidad y Micro-interacciones**:
   - Retroalimentación háptica en selección y cambios.
   - Deshacer y Rehacer (`Undo` / `Redo`).
   - Textos descriptivos para lectores de pantalla TalkBack.

---

## 3. Arquitectura del Software

```
com.polar.app
├── data
│   ├── model          // PolarProject, PrintSettings, TemplateStyle, etc.
│   ├── repository     // ProjectRepository (guardar/abrir .polar, caché de fotos)
│   └── template       // TemplateDetector (detección de huecos en moldes PNG/JPG)
├── engine
│   ├── PolarRenderer  // Motor de dibujo en android.graphics.Canvas
│   ├── PdfExporter    // Exportador de PDF multipágina con PdfDocument
│   └── ImageExporter  // Exportador de PNG a 300 ppp
├── ui
│   ├── theme          // Colores, tipografía y formas Polar
│   ├── viewmodel      // StudioViewModel (estado observable, historial undo/redo)
│   ├── components     // SheetCanvas, CardPreview, GalleryStrip, ColorPicker, etc.
│   └── screens
│       ├── PhoneStudioScreen    // Layout adaptado a móviles
│       └── TabletStudioScreen   // Layout adaptado a tablets / pantallas grandes
└── MainActivity.kt
```

### 3.1 Compatibilidad con Android Studio y Build System
- **Lenguaje**: Kotlin 2.0+
- **Min SDK**: 26 (Android 8.0 Oreo, cubriendo >97% de los dispositivos activos)
- **Target SDK**: 35 / 36 (Android 15+)
- **Build Tool**: Gradle 8.14.3 con Android Gradle Plugin 8.7+
- **Librerías principales**:
  - `androidx.compose.ui`, `material3`, `foundation`
  - `androidx.lifecycle:lifecycle-viewmodel-compose`
  - `org.jetbrains.kotlinx:kotlinx-serialization-json` (o Gson)
  - `com.google.zxing:core:3.5.3` (para generación nativa y limpia de códigos QR)
  - `androidx.activity:activity-compose`

---

## 4. Plan de Pruebas y Criterios de Aceptación
1. **Pruebas Unitarias**:
   - Modelos: serialización/deserialización JSON de proyectos `.polar`.
   - Renderizado: cálculo matemático de rectángulos de tarjetas y ranuras para las 19 plantillas.
   - Detección de plantillas: lectura de huecos transparentes y blancos con algoritmo BFS.
   - Calendario: cálculo exacto de días del mes y resaltado de fecha especial.
2. **Pruebas de Integración y Exportación**:
   - Generación de PDF válido con múltiples páginas.
   - Generación de PNG a 300 ppp exactos.
3. **Prueba de Build y Empaquetado**:
   - Compilación completa con `./gradlew assembleDebug` y generación exitosa del APK `app-debug.apk`.
