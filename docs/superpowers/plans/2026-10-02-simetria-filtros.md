# Polar · Implementación de simetría, interacción y filtros

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** implementar el prototipo aprobado en Android y Mac, con filtros no destructivos por proyecto, página(s) y foto, y modo oscuro legible.

**Architecture:** conservar los renderers, PhotoFit, almacenamiento y Undo existentes. `settings.photoLook` es el ajuste general, `cardOverrides.photoLook` permite aplicar páginas mediante sus tarjetas y `placements[].photoLook` permite una foto individual, también dentro de película. Resolver: colocación > tarjeta > general > neutral. No se agregan capas persistentes por página.

**Tech Stack:** Kotlin/Compose/ColorMatrix y SwiftUI/Core Image/CoreGraphics; sin dependencias nuevas.

**Spec:** `docs/prompts/2026-10-02-simetria-interaccion-y-filtros.md`, `docs/auditoria-simetria-2026-10.md`, `docs/filtros.md`, aprobación y ampliación del usuario del 2 de octubre.

## Global Constraints

- Rama `fase-2-simetria-filtros`; José Catalino <josecatalino.code@gmail.com>; commits convencionales en español, sin coautorías ni referencias a herramientas. Sin push/merge.
- Español; Android usa `strings.xml`. Crema/vino/Gelasio/Caveat; papel blanco en ambos temas.
- Espaciado 4/8/16/24/32; margen 16 teléfono/24 ancho; tarjetas con título1/descripción2 reservadas, selección con marca; controles mínimo48/principales56, filas simétricas y adaptables.
- Mantener autosave serializado, atomicidad, bloqueo ante carga fallida, papelera/Deshacer, fechas Swift/UUID y roles/fuentes de texto.
- Presets/matrices/grano exactos de `docs/filtros.md`; no modificar fotos originales. Todas las salidas proceden del motor.
- La aprobación ya autoriza implementar: no se solicita otra aprobación del plan. Un único agente trabaja Mac; el principal trabaja Android, documentación y verificación.

## Review Focus

- Película y plantillas importadas: aplicar a una foto no debe filtrar las vecinas; todas las rutas de dibujo deben resolver el mismo look.
- Aplicar una página o varias debe eliminar los filtros individuales de esas páginas y conservar el resto; aplicar al proyecto elimina excepciones anteriores.
- Cambiar distribución/diseño o eliminar páginas no debe perder filtros de fotos ni desplazar overrides incorrectamente.
- Letra130 %, modo oscuro y ventana pequeña: textos legibles, filas que crecen/cambian a columna, hoja libre y acciones accesibles.
- Fotos faltantes, datos no finitos y papel imposible: rechazar sin guardar un proyecto inválido; Deshacer/Rehacer y carga bloqueada deben seguir funcionando.

## Contrato compartido

`PhotoLook(preset="original", intensity=1, light=0, contrast=0, warmth=0, grain=0)`: campos numéricos finitos; intensidad/grano0…1, otros−1…1; desconocido→original. Campo opcional en settings, override y placement; defaults compatibles con archivos anteriores. Zoom finito `>0 && <=4`, manteniendo las claves existentes. PhotoFit calcula Ajustar mediante min/max de factores; UI no duplica esa fórmula ni la de mover offsets.

Aplicar proyecto: guardar look general y eliminar looks de tarjetas/colocaciones. Aplicar páginas: actualizar todas sus tarjetas, incluso vacías, y limpiar looks propios de sus colocaciones. Aplicar foto: actualizar sólo el placement. Quitar filtro pone look neutral en el alcance elegido. Aplicar a todas copia el look visible y ofrece Deshacer. Seleccionar páginas permite una o varias; «Esta página» es un atajo. Una transacción por gesto y un paso por aplicación.

## Tareas

### 1. Tokens y simetría (Android y Mac)

**Files:** `ui/theme/Spacing.kt`, `ui/theme/Color.kt`, `ui/Motion.kt`, Catálogo/Inicio/controles/Terminar/Ajustes/Bienvenida; equivalentes en `PolarMac/Sources/PolarApp.swift` y `LibraryViews.swift`.

- [x] Revisar colores reales y localizar foregrounds fijos que contradicen el tema; conservar colores de papel.
- [x] Implementar reservas de texto, tamaños mínimos, marcas, márgenes y pares de acciones de igual ancho con alternativa vertical.
- [x] Verificar tarjetas largas y controles al100/130 %, claro/oscuro; registrar contrastes de texto y fondo.

### 2. Modelo y operaciones puras

**Files:** Android `model/Models.kt`, `model/PolarJson.kt`, `core/look/LookResolver.kt`, `core/edit/ProjectEdits.kt`; Mac `Models.swift`, `PhotoLook.swift`; fixture en `PolarAndroid/app/src/test/resources/fixtures/photo_looks.polar`.

**Interfaces:** `LookResolver.resolve(project, slot): PhotoLook`; `ProjectEdits.setAllLooks/setPageLooks/setPhotoLook`; Mac ofrece la misma semántica mediante operaciones sobre PolarProject.

- [x] Escribir y ejecutar prueba de ida/vuelta del fixture: conservar general `bw`, tarjeta `sepia` y colocación `cool`; comprobar primero pérdida de campos en el modelo anterior.
- [x] Implementar campos opcionales, decodificación por defecto/desconocidos, validación, isEmpty y conservación al remapear.
- [x] Probar precedencia, página0 sin afectar página1, selección de varias páginas, una foto de película, neutral y JSON cruzado; zoom0.5 válido/zoom0 inválido.

### 3. Motor único de filtros

**Files:** `engine/PhotoFilters.kt`, `engine/PolarRenderer.kt`, `export/AndroidExportService.kt`; Mac `PhotoLook.swift`, `Renderer.swift`.

**Interfaces:** matriz4×5 compuesta; hash/grano en coordenadas lógicas de tarjeta con origen superior izquierdo; render de tarjeta seleccionada reutiliza el motor de página.

- [x] Añadir tests con cinco RGB literales de `docs/filtros.md`, intensidad0, orden de composición y determinismo antes de integrar.
- [x] Aplicar ColorMatrix/transformación equivalente sólo a fotos, incluida plantilla importada; grano monocromo con alfa conservado.
- [x] Ajustar decode PDF Android al espacio×zoom×300ppp con EXPORT_MAX; mantener muestreo/JPEG existente Mac.
- [x] Probar PNG y PDF: BW gris en fotos ±1, marco/texto con color; fixture común y tolerancia±2 entre plataformas.

### 4. Bandeja, menú y diálogos

**Files:** Android `ToolPanel.kt`, `ToolNav.kt`, `EditorScreen.kt`, `HomeScreen.kt`; Mac `PolarApp.swift`, `LibraryViews.swift`.

- [x] Sustituir apariencia modal por bandeja con cinco pestañas y alturas compacta/amplia, control explícito y gesto, animación240ms/reducir movimiento.
- [x] Conservar hoja libre, Atrás que cierra herramientas y panel lateral≥840; fichas de primer nivel iguales.
- [x] Menú anclado con cinco acciones reales, renombrado en línea Mac, Settings nativo y Terminar dentro de la ventana.
- [x] Comprobar interacción, teclado/accesibilidad y ancho estrecho/letter130 %.

### 5. Encuadre directo

**Files:** `engine/PhotoFit.kt`, `CropScreen.kt`, `EditorViewModel.kt`; Mac `Renderer.swift`, `Studio.swift`, vista de encuadre.

- [x] Tests de Ajustar y desplazamiento con rotación, foto de proporción distinta y espacio importado.
- [x] Arrastre/pellizco/trackpad/⌘rueda, doble toque, anillo con marcas1/2/3/4 y vibración Android, tercios durante gesto, sobrante atenuado y DPI vivo.
- [x] Cinco acciones iguales, navegación, flechas1 %/zoom0.1, acciones accesibles y atajos; una transacción por gesto.
- [x] Verificar coincidencia del encuadre con el render de tarjeta/exportación.

### 6. Herramienta Filtros y pendientes

**Files:** `panels/FiltersPanel.kt`, `EditorState.kt`, `EditorViewModel.kt`, `StyleInfo.kt`, `FinishScreen.kt`, `HomeViewModel.kt`, `UndoStack.kt`; Mac vistas/Studio/modelo.

- [x] Probar aplicación a todas y Deshacer, comparación mantenida sin guardar y alcance de páginas/foto.
- [x] Fichas con miniaturas reales filtradas, trabajo fuera de UI/caché limitada, anillo de intensidad y ajustes progresivos.
- [x] Sugerencia de film explícita, resumen al Terminar y Quitar filtro por alcance.
- [x] Validar antes de confirmar edición; Exception en renombrar/duplicar sin tragar cancelación; limpiar Rehacer en transacción; plurales/pulg.; Revisar abre encuadre.

### 7. Verificación y entrega

- [x] Android: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleRelease` (0 fallos/errores).
- [x] Mac: `./check.sh && ./build.sh`, comprobar firma local con `codesign --verify --deep --strict ../Polar.app`.
- [x] Repetir matriz Android antes de cambios y después; si mercatto_test vuelve a fallar en System UI, conservarlo y usar host/emulador de prueba aislado estable, anotando el límite.
- [x] Recorrido nativo con fotos de Fotitos y capturas después de ambas apps en tamaños/temas/escalas del encargo; registrar qué se probó y qué no.
- [x] Copiar debug a `Polar.apk`, actualizar reporte, hacer revisión cruzada con el mismo agente y commits pequeños. No publicar.

## Resultado final

Android: compilación limpia debug/release, 187 pruebas / 0 fallos / 0 omitidas,lint 0 errores (63 avisos), 6 pruebas nativas verdes. Mac: 10 suites verdes,build 2.1.0(3),firma válida. Capturas: 220 antes y 312 después. Debug copiado a Polar.apk y Mac a Polar.app. Revisión cruzada del único agente: corregidos alcance de página, mínimo de Ajustar y sobrante de encuadre Android; principal comprobó contraste y selector Mac.

Los controles accesibles y de teclado se revisaron en código/pruebas; el recorrido manual con lectores de pantalla/trackpad y la impresión física siguen pendientes, como consta en la verificación final. mercatto_test conservado; matriz ejecutada en perfiles aislados estables. Sin push/merge.
