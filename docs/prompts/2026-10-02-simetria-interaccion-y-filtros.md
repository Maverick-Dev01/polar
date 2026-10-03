# Instrucción: simetría visual, interacción propia y filtros de foto (Android + Mac)

> Copia este documento completo como instrucción para el agente que hará el trabajo. Está escrito para alguien que no conoce el proyecto.

---

## 0. Contexto del proyecto

**Polar** convierte fotos en hojas imprimibles. Los diseños son polaroid, mini, canción con QR (Spotify y reproductores), boleto, película vertical y horizontal, calendario, Instagram y moldes propios. Existen dos apps que comparten el mismo formato de proyecto `.polar` (JSON):

| App | Ruta | Stack |
|---|---|---|
| Android | `PolarAndroid/` | Kotlin 2.2, Compose BOM 2025.06, Material 3, minSdk 26, targetSdk 36 |
| Mac | `PolarMac/` | SwiftUI, macOS 14+, Apple Silicon. Compilación con `./build.sh` y pruebas con `./check.sh` |

Lee antes de empezar:
- `docs/superpowers/specs/2026-10-02-polar-android-rediseno-design.md`: diseño aprobado, identidad visual y fases.
- `PolarAndroid/README.md`, `PolarMac/README.md`.
- `docs/verificacion-android-2026-10-02.md` y `docs/verificacion-mac-2026-10-02.md`: estado actual y lo que ya está probado.

**Identidad visual (consérvala, no la reemplaces):**
- Fondo crema.
- Acento vino.
- Títulos en Gelasio y letra manuscrita Caveat en las polaroids.
- Hojas siempre blancas, también en modo oscuro (`PaperColors`).
- Modo claro y oscuro completos.
- En Android los colores salen sólo de los tokens de `ui/theme/` (`LightColors`, `DarkColors`, `PolarColors`, `PaperColors`). En Mac salen de sus equivalentes en SwiftUI.

**Principios del usuario (no negociables):**
- El teléfono es lo primero, pero tablet y Mac deben verse cuidados.
- Simple, accesible y nada tedioso.
- Todo en español, y en Android todos los textos van en `strings.xml`.
- **Lo que se ve en pantalla es lo que se imprime.** Vista previa, miniatura, PDF, PNG e impresión salen del mismo motor (`engine/PolarRenderer.kt` en Android, `Renderer.swift` en Mac).

---

## 1. Problema A — Simetría y contención en todas las pantallas

### Lo que el usuario observa
En **Catálogo («Nuevo diseño»)** cada diseño es una tarjeta con miniatura, nombre y descripción. Si una descripción es más larga, esa tarjeta crece y la de al lado no, así que la fila queda dispareja.

Causa confirmada en `PolarAndroid/app/src/main/java/com/polar/app/ui/catalog/CatalogContent.kt`, `DesignCard`:
- La descripción usa `maxLines = 2` sin `minLines`, así que una descripción de una línea deja la tarjeta más baja.
- En `LazyVerticalGrid` la fila toma la altura de su elemento más alto, pero las tarjetas no se estiran para llenarla.
- El título no tiene `maxLines = 1`.

El usuario quiere que esto se resuelva **como regla de todo el sistema**, no sólo en esa tarjeta: «el contenido no debe influir en la estructura del contenedor». Debe valer igual que en la web, donde una cuadrícula de tarjetas mantiene medidas iguales sin importar el texto. Aplica a tarjetas, secciones, filas de botones, el texto dentro de los botones, chips, listas y paneles, **en Android y en Mac**.

### Reglas que debes implementar

1. **Tarjetas en cuadrícula de altura fija por fila.**
   - Cada tarjeta tiene una estructura idéntica: zona de imagen con proporción fija, título y descripción.
   - El título va siempre en **1 línea** con elipsis.
   - La descripción ocupa siempre **2 líneas reservadas**:
     - Compose: `minLines = 2, maxLines = 2, overflow = Ellipsis`.
     - SwiftUI: `.lineLimit(2, reservesSpace: true)`.
   - El texto completo queda disponible para accesibilidad (`contentDescription` / `accessibilityLabel`). Si lo necesitas, añade una pulsación larga que lo muestre.
   - Las tarjetas llenan el ancho de su columna y comparten altura.
2. **Filas de botones simétricas.**
   - Si dos o más botones están en una fila, tienen el mismo ancho (`Modifier.weight(1f)`, o `.frame(maxWidth: .infinity)` en SwiftUI) y la misma altura (`heightIn(min = 48.dp)`; 56dp en acciones principales).
   - El texto va centrado vertical y horizontalmente, y el ícono y el texto se alinean siempre igual.
   - Si el texto no cabe con letra al 130 %, la fila pasa a columna (`FlowRow`, o `ViewThatFits` en SwiftUI). Nunca se recorta a media palabra.
   - **Prohibidas las alturas fijas que recortan texto.** Por ejemplo, `FinishScreen.kt` usa `height(56.dp)`; cámbialo por `heightIn`.
3. **Sistema de espaciado único.**
   - Base de 4dp/pt, con márgenes de pantalla de 16 (teléfono) y 24 (tablet/Mac).
   - Separación entre tarjetas de 12–16 y padding interno de tarjeta de 12–16.
   - Define tokens (`Spacing.xs/s/m/l/xl`) en `ui/theme` y su equivalente en Mac. Reemplaza los valores sueltos (`10.dp`, `14.dp`, `42.dp`, `124.dp`…).
4. **Radios y bordes coherentes.** Una sola escala de radios (pequeño para miniaturas, mediano para tarjetas, grande para superficies) y el mismo grosor de borde en tarjetas iguales. Selección = borde de acento + marca visible, no sólo color.
5. **Alineación.**
   - Los textos de una fila comparten línea base.
   - Los íconos de una misma lista tienen el mismo tamaño (20 o 24).
   - Las etiquetas de los deslizadores y sus valores quedan alineados en columna.
   - Los chips de filtro tienen todos la misma altura.
6. **Listas de Inicio («Tus diseños»).**
   - La tarjeta de proyecto sigue la misma regla que la del Catálogo: nombre en 1 línea y subtítulo (tipo y «Hace 5 min») en 1–2 líneas reservadas.
   - Hoy `HomeScreen.kt` usa `height(42.dp)` para el texto; sustitúyelo por líneas reservadas.
7. **Barra superior.** Con letra al 130 % el título se recorta («Nuevo d…»). Permite 2 líneas, o mueve el estado («Guardado») a una segunda línea más pequeña. Nunca recortes el nombre del diseño sin dar acceso al nombre completo.

### Pantallas a auditar y corregir (las dos apps)

| Pantalla | Android | Mac |
|---|---|---|
| Inicio / Biblioteca | `ui/home/HomeScreen.kt` | `Sources/LibraryViews.swift` (`LibraryView`) |
| Catálogo | `ui/catalog/CatalogContent.kt` | Selector de diseño en `PolarApp.swift` / `Studio.swift` |
| Editor y paneles | `ui/editor/EditorScreen.kt`, `ToolPanel.kt`, `ContextBar.kt`, `panels/*.kt` | `PolarApp.swift` (`StudioView`, controles en las líneas ~380–700) |
| Encuadre | `ui/editor/CropScreen.kt` | Controles «Zoom/Horizontal/Vertical» en `PolarApp.swift` ~564 |
| Terminar | `ui/editor/FinishScreen.kt` | `LibraryViews.swift` (`FinishView`) |
| Ajustes / Bienvenida | `ui/settings/*`, `ui/onboarding/*` | `PreferencesView`, `WelcomeView` |
| Controles compartidos | `ui/components/Controls.kt` (`LabeledSlider`, `SwitchRow`…) | `controlSlider` en `PolarApp.swift` |

**Método:**
1. Antes de tocar código, captura cada pantalla en:
   - teléfono pequeño (360×640 dp), teléfono normal (411×891), tablet horizontal (1280×800) y Mac a 1280×800 y 1728×1117;
   - modo claro y oscuro;
   - letra al 100 % y al 130 %.
2. Escribe en `docs/auditoria-simetria-2026-10.md` una tabla: pantalla, problema concreto, regla violada y arreglo.
3. Corrige.
4. Vuelve a capturar con los mismos tamaños y pon el antes y el después en `docs/capturas/simetria/` (las PNG no se versionan).

Si tienes disponible la skill de diseño **Impeccable**, úsala para la auditoría (`audit`/`critique`) y para el pulido (`polish`).

---

## 2. Problema B — Dejar la «hoja que sube» genérica por una interacción propia

### Lo que el usuario observa
Las opciones aparecen en una **hoja inferior** que sube hasta media pantalla (con la barrita para arrastrar, esquinas de 28dp y sombra). Al usuario le parece genérica, «como en cualquier app de IA», y quiere un diseño propio, novedoso y no trivial.

Dónde está hoy:
- **Editor (teléfono):** `ToolPanel.kt` dibuja una superficie de ~46 % de alto con la barrita (`size(36.dp, 4.dp)`) y la «X». Abarca Fotos, Diseño, Texto y Papel y tapa la hoja.
- **Inicio:** el menú ⋮ de cada diseño abre un `ModalBottomSheet` (`HomeScreen.kt` ~135).
- **Mac:** `.sheet(...)` para Ajustes, Bienvenida, Terminar y Renombrar (`LibraryViews.swift` 28–30, 111).

### Dirección de diseño: «mesa de revelado»

Polar trata de fotos impresas, así que el lenguaje visual debe salir de ese mundo (mesa de trabajo, cuarto oscuro, álbum) y no de los componentes estándar. Propuesta base; puedes mejorarla, pero justifica los cambios en la auditoría:

1. **Bandeja acoplada en lugar de hoja flotante (editor en teléfono).**
   - Las herramientas viven en una **bandeja** que forma parte del diseño de la pantalla, no en una capa encima.
   - Al abrir una herramienta, la hoja (el lienzo) **se reduce y se desplaza hacia arriba** con una animación coordinada. Nunca queda tapada ni oscurecida.
   - Sin barrita para arrastrar, sin velo y sin esquinas de hoja modal.
   - La bandeja tiene un borde superior con «pestañas de fichero» o de separadores de álbum, una por herramienta (Fotos, Diseño, Texto, Papel, y **Filtros**, nueva; ver el Problema C). La pestaña activa se funde con la bandeja.
   - La bandeja tiene **dos alturas fijas**: compacta (una tira horizontal de opciones grandes y táctiles, ~120dp) y amplia (~50 %). Se cambia con un control explícito («Más opciones») y también con un deslizamiento. Nunca queda a una altura arbitraria.
   - Las opciones de primer nivel se muestran como **tira horizontal de fichas** (tiles cuadrados idénticos con ícono e imagen y etiqueta de 1 línea), no como una lista vertical de Material.
   - En tablet y Mac (≥ 840dp) se conserva el panel lateral fijo, pero con el mismo lenguaje de pestañas y fichas, para que las tres plataformas se sientan de la misma familia.
2. **Menú contextual anclado (Inicio).** En lugar de la hoja inferior, el ⋮ abre una **tarjeta flotante anclada** a la propia tarjeta del diseño, que «se levanta» como una polaroid tomada de la mesa (escala y sombra ligera). Muestra las acciones en una cuadrícula de 2×2 o 3×2 de fichas iguales (Abrir, Renombrar, Duplicar, Compartir, Borrar). En Mac, un `popover` anclado con el mismo contenido.
3. **Diálogos de Mac.** Ajustes y Terminar no deben ser hojas modales genéricas:
   - Ajustes → ventana de preferencias nativa (`Settings` scene).
   - Terminar → vista completa dentro de la ventana, con transición desde el editor.
   - Renombrar → edición en línea sobre el nombre.
4. **Movimiento.**
   - Curvas de `ui/Motion.kt` (crear equivalentes en Mac), de 200–300 ms.
   - Respetar «reducir movimiento» (Android: escala de animación del sistema; Mac: `accessibilityReduceMotion`).
5. **Accesibilidad.**
   - Cada pestaña y ficha mide al menos 48dp, tiene `Role` correcto y etiqueta, y se recorre en orden lógico con TalkBack o VoiceOver.
   - El botón Atrás cierra la bandeja antes de salir del editor (ya existe esa lógica en `EditorScreen.kt`; consérvala).

Entrega primero un **prototipo visual** de la bandeja, el menú anclado y el nuevo encuadre (sección 3), en teléfono y tablet, claro y oscuro. Puede ser un HTML estático o capturas de Compose `@Preview`. Espera el visto bueno del usuario antes de implementarlo completo.

---

## 3. Problema C — Encuadre directo, sin tres deslizadores

### Lo que el usuario observa
Para encuadrar una foto (`CropScreen.kt` en Android; «Zoom / Horizontal / Vertical» en Mac) hay **tres deslizadores** apilados. No le gusta: es indirecto y se siente como un formulario.

### Diseño nuevo
1. **Manipulación directa sobre la foto**, dentro del marco real de la tarjeta (con su borde de polaroid, boleto o cuadro de película):
   - **arrastrar** para mover;
   - **pellizcar** para acercar (en Mac: trackpad con `MagnifyGesture` y rueda con ⌘);
   - **doble toque** para alternar entre «Llenar» y «Ajustar».
2. **Rueda de zoom tipo anillo de lente.** Bajo la foto va una regla horizontal con marcas, que se arrastra lateralmente como el anillo de enfoque de una cámara (1× a 4×, con muesca y vibración ligera en 1×, 2× y 3×). Sustituye al deslizador de zoom.
3. **Guías:**
   - cuadrícula de tercios mientras se arrastra (con el token `CropGuide`);
   - la zona que se pierde fuera del marco se ve atenuada;
   - el aviso de **baja resolución** (`PhotoFit.LOW_RES_DPI = 150`) aparece en vivo al acercar demasiado.
4. **Barra de acciones simétrica** (fichas iguales): Girar 90°, Llenar, Ajustar, Centrar y Restablecer. Más «Listo» como acción principal.
5. **Navegación entre fotos:** flechas o deslizamiento en el borde para pasar a la foto anterior o siguiente sin salir del encuadre, con un indicador «3 de 12».
6. **Precisión y accesibilidad (obligatorio, sustituye a los deslizadores):**
   - un botón «Ajuste fino» muestra flechas de empuje (↑↓←→ de 1 % y zoom ±0.1);
   - acciones personalizadas de TalkBack y VoiceOver («mover a la izquierda», «acercar»…);
   - en Mac, las flechas del teclado mueven la foto y ⌘+/⌘− hacen zoom.
7. **Fidelidad:** usa exclusivamente `engine/PhotoFit.kt` (Android) y su equivalente en `Renderer.swift` para la matemática. Lo que se ve en el encuadre debe coincidir píxel a píxel con lo que se imprime. No dupliques la fórmula en la UI.
8. **Modelo sin cambios:** `PhotoPlacement(zoom, offsetX, offsetY, quarterTurns)` sigue igual; sólo cambia la interfaz. Un gesto completo = **un solo paso de Deshacer** (`beginGesture`/`endGesture` en `EditorViewModel`, transacciones de `UndoStack`).

---

## 4. Problema D — Filtros de foto (función nueva, en las dos apps)

### Lo que pide el usuario
- Aplicar un filtro a **todas** las fotos (por ejemplo, todo en blanco y negro), o sólo a **una**.
- Cambiar de filtro cuando quiera.
- Ajustes básicos de luz.
- Que los diseños de **película** (`filmVertical`, `filmHorizontal`) sugieran blanco y negro, sin tener que editar la foto en otra app.

### Modelo de datos (compatible entre Mac y Android)

Sigue el patrón que ya existe para el texto por tarjeta («Todas las tarjetas / Sólo esta tarjeta», con `cardOverrides`):

```jsonc
// settings (ajuste general del proyecto)
"photoLook": { "preset": "bw", "intensity": 1.0, "light": 0.0, "contrast": 0.0, "warmth": 0.0, "grain": 0.0 }

// cardOverrides["<índice de tarjeta>"] (sólo si esa tarjeta difiere)
"photoLook": { "preset": "sepia", "intensity": 0.8, ... }
```

- Agrega el tipo `PhotoLook` en `model/Models.kt` (Android) y en `Sources/Models.swift` (Mac). Debe ser inmutable y con valores por defecto neutros (`preset = "original"`, todo en 0, `intensity = 1`).
- **Resolución:** la `photoLook` de la tarjeta gana a la de `settings`; si no hay ninguna, la foto va sin filtro. Hazlo con funciones puras junto a `TextResolver` (por ejemplo `LookResolver`) en las dos apps.
- **Compatibilidad:**
  - Android ya usa `ignoreUnknownKeys` y `explicitNulls=false`.
  - En Mac, verifica que el decodificador acepta proyectos sin `photoLook` y que **rechaza o normaliza** presets desconocidos sin fallar (usa `original`).
  - Agrega fixtures cruzados en las suites (`ParityChecks.swift` y las pruebas JSON de Android): proyecto con filtro guardado en Android → abre en Mac con el mismo resultado, y viceversa.
  - `settings.textStyles` sigue aceptando sólo sus roles actuales; no mezcles filtros ahí.
- Deshacer/Rehacer: aplicar o cambiar un filtro es un paso, y mover un deslizador de ajuste es un gesto, que también cuenta como un solo paso.

### Presets (definición única, idéntica en las dos plataformas)

| id | Nombre en UI | Qué hace |
|---|---|---|
| `original` | Original | Sin cambios |
| `bw` | Blanco y negro | Luminancia Rec. 709 (0.2126, 0.7152, 0.0722) |
| `film` | Película | Blanco y negro + contraste +15 % + grano 0.25 |
| `sepia` | Sepia | Matriz sepia clásica |
| `warm` | Cálido | Temperatura +, ligera saturación |
| `cool` | Frío | Temperatura − |
| `faded` | Desvanecido | Negros levantados, saturación −30 % |
| `vivid` | Vivo | Saturación +25 %, contraste +10 % |

- Documenta las **matrices de color 4×5 exactas** en `docs/filtros.md` y úsalas tal cual en las dos apps. En Android: `ColorMatrix` + `ColorMatrixColorFilter` en el `Paint` de `PolarRenderer.drawPhoto`. En Mac: `CIColorMatrix` o una matriz equivalente aplicada en `Renderer.swift`.
- La **intensidad** (0–1) interpola linealmente entre la matriz identidad y la del preset.
- `light`, `contrast` y `warmth` (−1…1) se componen después del preset, en este orden fijo: preset → luz → contraste → calidez.
- **Grano:** ruido monocromo **determinista**, con una semilla derivada del `assetID` y del índice de tarjeta, y generado en coordenadas de la tarjeta (no en píxeles de pantalla). Así la vista previa, el PDF y la impresión muestran el mismo patrón a cualquier resolución.
- El filtro **nunca modifica el archivo de la foto**: es no destructivo y se aplica al dibujar. Las miniaturas de Inicio también lo reflejan.

### Interfaz

- **Nueva herramienta «Filtros»** en la bandeja o el panel (sección 2), entre Fotos y Diseño.
- **Alcance arriba, igual que en Texto:** selector segmentado «Todas las fotos / Sólo esta foto (tarjeta N)». Si hay una tarjeta seleccionada, el alcance arranca en «Sólo esta foto».
- **Tira de presets:**
  - fichas cuadradas idénticas (simetría, Problema A) con la **foto real seleccionada** (o la primera) ya filtrada y el nombre en 1 línea;
  - la ficha activa lleva borde de acento y una marca;
  - las vistas previas se generan a tamaño pequeño, en segundo plano (IO/Default) y en caché.
- **Intensidad:** la misma «rueda de anillo» del encuadre, para mantener coherencia; no uses un deslizador Material más.
- **«Ajustes»** (progresivo, oculto por defecto): Luz, Contraste, Calidez y Grano, con el mismo control.
- **Comparar:** mantener pulsada la hoja muestra la versión original mientras dure la pulsación. En Mac se hace con la tecla espacio o un botón «Comparar».
- **«Aplicar a todas»:** desde «Sólo esta foto», copia ese filtro como ajuste general y quita los ajustes propios. Ofrece Deshacer en un Snackbar, como ya se hace con el texto.
- **«Quitar filtro»:** restablece a Original en el alcance actual.
- **Sugerencia por diseño:**
  - al elegir `filmVertical` o `filmHorizontal` en el Catálogo (o al cambiar a ese diseño), muestra un aviso discreto: «Las películas lucen mejor en blanco y negro · Aplicar». No lo apliques automáticamente.
  - El preset sugerido por diseño vive en el modelo de estilos (`StyleInfo.kt` y el `enum` de Mac), para que en el futuro otros diseños puedan sugerir el suyo.
- En Terminar, el resumen indica «Filtro: Blanco y negro (todas)» o «Filtros en 3 fotos», y el aviso de baja resolución sigue funcionando.

### Pruebas obligatorias
- **Unitarias (las dos apps):**
  - resolución tarjeta > general > ninguno;
  - serialización de ida y vuelta;
  - preset desconocido → original;
  - intensidad 0 = identidad;
  - el grano es determinista (dos renders iguales producen los mismos píxeles).
- **Render:** una foto de prueba con cada preset, comparada contra los valores esperados de la matriz en 5 píxeles de muestra, con tolerancia ±1. El mismo fixture en Mac y en Android debe dar el mismo resultado (±2).
- **Export:** el PDF y el PNG de una hoja con `bw` contienen sólo grises (R=G=B ±1) en el área de las fotos y mantienen los colores en marcos y textos.
- **UI (Robolectric/Compose):** cambiar el alcance, aplicar a todas con Deshacer y comparar al mantener pulsado.

---

## 5. Lo que no debes romper

- Autoguardado serializado (la última edición prevalece), la escritura atómica, la papelera con Deshacer y el bloqueo de escritura cuando la carga falla (`locked` en `EditorViewModel`).
- Compatibilidad de `.polar` entre Mac y Android, incluidas las fechas en segundos Swift desde 2001 y los IDs en mayúsculas.
- Texto por tarjeta, 20 fuentes, color de texto, fechas EXIF y elegidas.
- La impresión, el guardado PDF/PNG y Compartir.
- Diseño adaptable: con ancho ≥ 840dp se usa barra lateral y panel fijo; con menos, barra inferior.
- Todo lo listado en `docs/verificacion-*.md`.

**Pendientes conocidos de la última revisión:** si encaja con lo que vas a tocar, resuélvelos; si no, déjalos anotados.
- El PDF pesa demasiado. `PdfDocument` incrusta las fotos sin pérdida a su resolución decodificada. Decodifica cada foto al tamaño de su espacio × zoom × 300 ppp, con tope `EXPORT_MAX`. Ahora que el filtro se aplica al dibujar, aprovecha para hacerlo en ese mismo paso.
- `edit()` puede guardar un proyecto que luego no pasa `validated()` (por ejemplo: papel de 80 mm, 6 filas, separación 30 y margen 60). Valida antes de confirmar y avisa en español.
- En Inicio, Renombrar y Duplicar sólo capturan `IOException`; deben capturar `Exception` y mostrar un mensaje.
- `UndoStack.record` con una transacción abierta debe limpiar Rehacer.
- Plurales («1 fotos»), unidades «in» → «pulg.».
- «Revisar» en Terminar debe abrir el Encuadre de esa foto.

---

## 6. Proceso, entregables y reglas de git

1. **Auditoría + prototipo** (secciones 1–4) → `docs/auditoria-simetria-2026-10.md` y el prototipo. **Para y pide visto bueno al usuario.**
2. **Plan de implementación** por tareas pequeñas y con pruebas → `docs/superpowers/plans/2026-10-xx-simetria-filtros.md`.
3. Implementa en este orden:
   1. tokens de espaciado y simetría;
   2. modelo `PhotoLook` en las dos apps, con sus fixtures cruzados;
   3. motor de filtros en los dos renderers;
   4. bandeja y menú anclado;
   5. encuadre nuevo;
   6. herramienta Filtros;
   7. Mac.
4. **Verificación final:**
   - Android:
     ```bash
     cd PolarAndroid && export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && ./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleRelease
     ```
     Debe terminar con 0 pruebas fallidas y 0 errores de lint.
   - Mac:
     ```bash
     cd PolarMac && ./check.sh && ./build.sh
     ```
     Las 7 suites deben pasar y la firma local debe verificarse.
   - Recorrido en el emulador `mercatto_test` con fotos reales de `Fotitos/` (no se versionan).
   - Capturas antes y después en todos los tamaños de la sección 1.
   - Copia el APK de depuración a `Polar.apk` en la raíz.
5. **Git:**
   - Rama nueva desde `fase-1-base` (por ejemplo `fase-2-simetria-filtros`).
   - Identidad **José Catalino <josecatalino.code@gmail.com>**: verifícala con `git config user.email` antes del primer commit. **Nunca** uses la cuenta de la empresa.
   - Commits pequeños en español con formato convencional (`feat(android): …`, `fix(mac): …`).
   - **Prohibido** agregar `Co-Authored-By`, «Generated with…» o cualquier mención a IA o herramientas en commits o PRs.
   - No hagas push ni merge sin que el usuario lo pida.
6. **Seguridad:** no crees llaves de firma ni manejes contraseñas. La firma de publicación la hace el usuario (`docs/play-store.md`).
7. **No agregues dependencias nuevas.** Los filtros se hacen con `ColorMatrix` en Android y Core Image en Mac. Si consideras imprescindible alguna, justifícala y pide permiso.

Al terminar, entrega un resumen con:
- lo que cambió en cada pantalla (con capturas antes y después);
- los presets y sus matrices;
- los resultados de las pruebas de las dos apps;
- cualquier decisión que tomaste por tu cuenta y qué costaría si fuera incorrecta.
