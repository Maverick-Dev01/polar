# Polar para Android · Rediseño y nuevas funciones

**Fecha:** 2026-10-02
**Estado:** propuesta para revisión del usuario
**Prototipo aprobado:** https://claude.ai/artifact/TVJe3AQG7ZQ5YtgFERHi8v (claro, oscuro, teléfonos chico/mediano/grande, tablet)
**Reemplaza:** `2026-10-01-polar-android-design.md` (port inicial)

---

## 1. Objetivo

Convertir la versión Android de Polar en una app **profesional, sencilla y lista para Play Store**, con la identidad de la versión Mac, todas sus funciones y otras nuevas que la hagan más útil.

**Para quién:** personas que imprimen recuerdos o regalos en casa o en papelería, y pequeños negocios que venden polaroids o fotos personalizadas. Por defecto el flujo es simple; las herramientas de producción (biblioteca, duplicar, copias) están a mano sin estorbar.

**Éxito:**
- Alguien sin experiencia elige diseño, pone fotos e imprime o exporta en minutos, sin instrucciones.
- Ningún trabajo se pierde: autoguardado y deshacer en todo.
- Lo que se ve en pantalla es exactamente lo que se imprime, a tamaño real.
- Pasa la revisión de Play Store: objetivo API 36, AAB firmado, privacidad, accesibilidad.

**Principios fijos:**
- **Teléfono primero.** La tablet se adapta, nunca al revés.
- **Sin conexión, sin cuentas, sin anuncios.** Las fotos no salen del teléfono.
- **Los originales nunca se modifican.**
- **Compatible con la Mac:** los `.polar` se abren en ambas direcciones; los campos nuevos son opcionales.

## 2. Reglas de sencillez (aplican a todo)

1. **Manipulación directa antes que controles:** tocar el texto o la tarjeta para editarlo, pellizcar para encuadrar. Los sliders quedan sólo como respaldo accesible.
2. **Revelación progresiva:** lo básico está a la vista; lo avanzado vive en "Más opciones" (columnas/filas, margen, separación, guías, medidas en mm, bordes).
3. **Valores listos:** al elegir un diseño ya trae papel y distribución; al agregar fotos se acomodan solas.
4. **Nada da miedo:** todo se deshace y lo destructivo muestra un Snackbar con "Deshacer".
5. **Lenguaje simple:** "Fotos por hoja", nunca "capacidad"; colores para tocar, nunca hex.
6. **Accesible:** objetivos táctiles de 48dp, contraste AA, `contentDescription` en todo lo que no es texto, texto en sp, probado con letra al 130 %, nada depende sólo del color.

## 3. Identidad visual

- **Color** (roles de Material 3, tema estático, sin colores del fondo de pantalla):

  | Rol | Claro | Oscuro |
  |---|---|---|
  | primary | `#7A293B` | `#FFB2BC` |
  | onPrimary | `#FFFFFF` | `#561D2B` |
  | primaryContainer | `#F2DDE1` | `#5E2231` |
  | onPrimaryContainer | `#4D1522` | `#FFD9DE` |
  | background | `#F7F2EB` | `#1D1A19` |
  | surface | `#FFFFFF` | `#2A2524` |
  | surfaceContainer | `#F1EAE2` | `#332D2C` |
  | mesa del lienzo | `#E8E3DC` | `#141211` |
  | onSurface | `#2B2221` | `#EDE0DD` |
  | onSurfaceVariant | `#6B5D5A` | `#B9AAA7` |
  | outline | `#B5A6A1` | `#7D6D6A` |
  | outlineVariant | `#E4DAD0` | `#3D3534` |

  La **hoja siempre es blanca** porque representa el papel.
- **Tipografía:** Gelasio (métricamente compatible con la Georgia de Mac) para marca y títulos; Roboto del sistema para la interfaz, todo mapeado a la escala de tipo de Material 3 en sp.
- **Forma:** tarjetas de 12dp, paneles de 16–28dp, chips de 10dp.
- **Detalles propios:**
  - La biblioteca muestra cada proyecto como una polaroid con su nombre escrito a mano (Caveat).
  - Al colocar una foto, se "revela" en 400 ms desde crema. Se apaga si el sistema pide quitar animaciones.
- **Movimiento:** transiciones de Material (shared axis entre pantallas, container transform de la polaroid al editor).

## 4. Pantallas y navegación

Rutas: `Bienvenida → Inicio → Catálogo → Editor → (Encuadrar | Terminar) · Ajustes · Crear molde`. El gesto Atrás del sistema funciona siempre (Back predictivo).

| Pantalla | Contenido |
|---|---|
| **Bienvenida** | Sólo la primera vez; 3 pasos; se puede saltar; se vuelve a ver desde Ajustes. |
| **Inicio · Tus diseños** | Cuadrícula de polaroids (2 columnas en teléfono, 3–4 en tablet vertical, 4–5 en horizontal). Búsqueda y orden (recientes / nombre). Menú ⋮ con Abrir, Duplicar para otro pedido, Cambiar nombre, Compartir `.polar`, Borrar (Snackbar con Deshacer). Botón flotante extendido "Nuevo diseño". Estado vacío que invita a crear. |
| **Catálogo** | Miniaturas reales dibujadas por el motor. Chips por ocasión: Todos, Clásicos, Música, Cine, Fechas, Ocasiones, Libre, Mis moldes. Al final: Importar plantilla, Abrir `.polar`, Crear mi molde (fase 3). En modo "Cambiar diseño" conserva fotos y frases. |
| **Editor** | Barra superior: atrás, nombre, "Guardado", deshacer, **Imprimir**. Hoja centrada; deslizar o ‹ › para cambiar de hoja; puntos de página. **Tocar tarjeta** → barra contextual (Cambiar, Encuadrar, Filtro, Texto, Copias, Quitar). Herramientas: **Fotos · Diseño · Texto · Papel**, en panel a media altura en teléfono y panel fijo de 360dp con barra lateral (rail) en tablet horizontal. Proyecto vacío: tarjeta "Pon tus fotos". |
| **Encuadrar** | Pantalla completa oscura; pellizcar/arrastrar; zoom como respaldo; girar; restablecer; aviso de calidad. |
| **Terminar** | Miniaturas de hojas y resumen (hojas, fotos, papel). Avisos: baja resolución (con "Revisar"), espacios vacíos. Acciones: **Imprimir**, Guardar PDF (todas), Guardar PNG (esta hoja, 300 ppp), Compartir. Tip "Tamaño real 100 %". |
| **Ajustes** | Tema (Sistema / Claro / Oscuro), unidades (mm / pulgadas), papel para diseños nuevos, ver bienvenida, cómo imprimir, privacidad, versión. |

**Reglas de adaptación:** la estructura se elige por *window size class*, nunca por modelo de dispositivo.
- **Compacta y mediana** (< 840dp): barra inferior y paneles a media altura; las cuadrículas ganan columnas según el ancho.
- **Expandida** (≥ 840dp): rail + hoja + panel fijo.
- La hoja se escala para caber en el espacio libre que dejan las barras y el panel.

## 5. Funciones

### 5.1 Texto (pedido explícito)

- **Partes de texto** según el diseño: Título, Subtítulo, Pie de foto, Canción, Artista y la nueva **Fecha**.
- **Alcance:** "Todas las tarjetas" o "Sólo esta tarjeta".
  - Una tarjeta muestra el texto general salvo que tenga uno propio.
  - "Volver al texto general" borra el propio.
  - "Aplicar a todas" borra los propios de esa parte; aparece cuando existen e indica cuántos son.
- **Estilo por parte y con el mismo alcance:**
  - Fuente (20 incluidas, agrupadas: manuscritas, libro, palo seco, máquina de escribir, decorativas; cada chip muestra su propia letra).
  - Tamaño (Auto, Chica, Mediana, Grande y valor exacto en "Más opciones").
  - **Color** (paleta del diseño + "Usar color del diseño" + selector de otro color).
  - Negrita, cursiva, alineación.
  - Posición fina (en "Más opciones").
  - Mostrar u ocultar.
- **Fecha:**
  - Origen: de la foto (EXIF `DateTimeOriginal`, si no existe la fecha del archivo), una fecha elegida, o ninguna.
  - Formatos: `14 feb 2026`, `14.02.26`, `febrero 2026`.
- **Estilos rápidos** (Parejas, Amigos, Familia, Mascotas, Viajes, Celebraciones): cambian fuente y color, nunca las frases. "Usar frases sugeridas" las reemplaza y se puede deshacer.
- El texto se reduce solo para no tapar la foto (igual que en Mac).

### 5.2 Fase 1 · Base sólida (P0)

1. Corregir que la plantilla importada no se dibuje (`templateBitmap` nunca se asigna) y mostrar los errores de importación en lugar de tragarlos.
2. **Biblioteca con autoguardado** (debounce de ~1 s y al salir); abrir y guardar `.polar` compatible con Mac; **duplicar proyecto** como molde para otro pedido. Las fotos se **copian al almacenamiento interno** del proyecto; los originales no se tocan.
3. **Deshacer/rehacer correcto:** un paso por acción. Un campo de texto agrupa desde que recibe el foco hasta que lo pierde; un slider agrupa desde que empieza a moverse hasta que se suelta. Máximo 50 pasos.
4. Texto completo de 5.1.
5. **Paridad con Mac:**
   - Paleta de color del diseño.
   - Fotos por hoja (1, 2, 4, 6, 8, 9, 12, 16), columnas y filas.
   - Formato de tarjeta y separación.
   - Fotos con esquinas redondas.
   - Calendario: año y fecha especial.
   - Papel personalizado de 80–600 mm, margen, guías (esquinas / líneas) y bordes.
   - Editar huecos de plantilla importada: arrastrar y medidas.
   - Aviso de baja resolución (< 150 ppp).
   - Miniaturas reales en el catálogo y marca en las fotos ya usadas.
   - QR real a la canción.
6. Navegación, identidad visual, tema claro/oscuro y adaptación a tamaños de la sección 4 (Bienvenida mínima incluida).
7. Desde Terminar: **imprimir desde el teléfono** (`PrintManager` con el mismo PDF a tamaño real), guardar PDF o PNG con el selector «Guardar como» del sistema (sin permisos de almacenamiento, también en Android 8–9) y compartir.
8. Preparación técnica para Play Store (sección 7).

### 5.3 Fase 2 · Lo que la hace destacar (P1)

1. Encuadre con gestos dentro de la tarjeta y en la pantalla Encuadrar.
2. **Arrastrar** una foto de la bandeja a una tarjeta; mantener presionado una tarjeta y soltarla en otra para **intercambiar**. Su texto propio viaja con la tarjeta de origen (se intercambian ambos).
3. **Filtros** no destructivos por foto: Original, Blanco y negro, Sepia, Cálido, Frío, Vintage; brillo y contraste simples; "Usar en todas".
4. **Copias:** "×N" duplica esa foto con su encuadre, filtro y texto en los espacios siguientes; se refleja en el resumen.
5. Rellenar por fecha de la foto o en el orden en que se eligieron.
6. Bienvenida completa, estados vacíos guiados y la animación de "revelado".

### 5.4 Fase 3 · Crear mi molde

- **Lienzo:** la hoja con cuadrícula opcional. Herramientas: Hueco, Adorno, Texto, Mover. Deshacer en todo.
- **Huecos a mano:** el trazo libre se clasifica como rectángulo, círculo u óvalo, o corazón y se reemplaza por la forma limpia.
  - Rectángulo: caja de la nube de puntos, rotación ajustada a 0°/90°, salvo que el usuario la gire a propósito en pasos de 15°.
  - Si el trazo no se reconoce, se ofrece convertirlo en rectángulo.
- **Guías imanes:** al mover o redimensionar, el elemento se ajusta (umbral ~8dp) a orillas y centro de la hoja, márgenes y orillas o centros de otros elementos. También propone igualar tamaño y repartir espacio parejo.
- **Adornos:** biblioteca vectorial incluida (flores, hojas, corazones, estrellas, cintas, marcos, unos 40), con color y tamaño editables.
  - **Dibujar para sugerir:** un reconocedor de trazos sin conexión (algoritmo $Q) compara tu dibujo con unas 10 clases y ofrece los 3 adornos más parecidos, o conservar el trazo suavizado.
- **Espejo y simetría:** duplicar en espejo respecto al eje vertical u horizontal de la hoja; modo simetría en vivo mientras se dibuja.
- **Guardar:** queda en "Mis moldes" y funciona como cualquier diseño (rellenar, texto, imprimir).
  - En el `.polar` se guarda como `customTemplate` (huecos y adornos vectoriales).
  - Para que la Mac actual lo abra, se exporta además como plantilla importada: PNG rasterizado más regiones.
- **No promete:** reconocer cualquier dibujo ni recortes con formas libres complejas.

### 5.5 Ideas para después (P2, fuera de este alcance)

Calculadora de costo por pedido, QR a cualquier enlace (video, álbum, mensaje de voz), exportar cada tarjeta suelta para redes, PNG de todas las hojas y compartir un proyecto con sus fotos incluidas.

### 5.6 Se quita

- El campo de color hex escrito a mano.
- El pellizco que agrandaba toda la hoja; se reemplaza por doble toque para acercar o volver.
- El diálogo de exportar; su lugar es la pantalla Terminar.

## 6. Arquitectura

### 6.1 Módulo y paquetes (un solo módulo `app`)

```
com.polar.app
├── core/model      datos serializables (PolarProject, PrintSettings, CardOverride, …)
├── core/text       TextResolver: texto/estilo efectivo por tarjeta y parte
├── core/layout     geometría de hoja, tarjetas y huecos (extraída de PolarRenderer)
├── core/history    UndoStack con transacciones
├── data            ProjectRepository, PhotoImporter (copia + EXIF), SettingsRepository (DataStore), FontCatalog
├── engine          PolarRenderer (vista previa = exportación), QrGenerator, Thumbnailer
├── export          PdfExporter, PngExporter, PrintAdapter (fase 2), ShareHelper
├── template        TemplateImporter (detección de huecos)
├── maker           ShapeRecognizer, Snapper, Symmetry, DoodleMatcher ($Q) — fase 3
└── ui
    ├── theme       colores, tipografía, formas, movimiento
    ├── navigation  rutas y NavHost
    ├── components  hoja, tarjeta, chips, switches, paneles, Snackbar
    └── screens     onboarding, home, catalog, editor, crop, finish, settings, maker
```

- **ViewModels:** `HomeViewModel`, `EditorViewModel` (proyecto, selección, panel activo, historial), `SettingsViewModel`.
- **Estado inmutable:** `StateFlow` con `data class` de puros `val` y listas o mapas inmutables. El modelo actual usa `var` y `copy()` superficial, que es el origen de errores de deshacer.
- **Dependencias nuevas:** `navigation-compose`, `material3-adaptive` (window size class), `datastore-preferences`, `core-splashscreen`, `exifinterface`. Se mantienen `kotlinx-serialization` y `zxing`. BOM de Compose y AGP actualizados a versiones estables recientes.

### 6.2 Modelo de datos (cambios, todos opcionales con valor por defecto)

- `TextRole.DATE` nuevo.
- `PrintSettings`:
  - `dateSource` (`photo` | `chosen` | `none`, por defecto `none`).
  - `chosenDateEpochMs`.
  - `dateFormat` (`dayMonthYear` | `numeric` | `monthYear`).
- `PolarProject`:
  - `name`, `updatedAt`.
  - `cardOverrides: Map<String, CardOverride>`. La clave es el índice global de tarjeta (`página × tarjetasPorHoja + tarjeta`); es posicional, igual que las colocaciones.
- `CardOverride`:
  - `texts: Map<rol, String>`.
  - `styles: Map<rol, TextAppearance>`. Al crear el propio se copia el estilo general.
  - `dateSource?`, `chosenDateEpochMs?`.
- `PhotoPlacement`: `filter` (`none` por defecto), `brightness`, `contrast` (fase 2).
- `PhotoAsset`: `takenAtEpochMs?` (EXIF).
- `customTemplate` (fase 3): `slots[{shape, x, y, w, h, rotation}]` y `decorations[{asset | path, x, y, w, h, rotation, mirrorX, mirrorY, hex}]`, todo normalizado 0–1.
- **Copias:** se representan como colocaciones repetidas. No se agrega un campo nuevo, así la Mac las entiende.
- **Al cambiar "Fotos por hoja"** las colocaciones conservan su orden. En diseños de 1 foto por tarjeta el índice de tarjeta es el de la foto, así que los textos propios no se mueven. Al cambiar a un diseño con otra cantidad de fotos por tarjeta (película: 5), cada texto propio pasa a la tarjeta que contiene la primera foto de su tarjeta anterior; si dos caen en la misma, gana la primera. Se prueba.

### 6.3 Persistencia

- `filesDir/projects/<uuid>/`:
  - `project.polar`: JSON con las mismas claves que Mac; las rutas de fotos son relativas (`photos/<id>.<ext>`).
  - `photos/`.
  - `thumb.png`.
  - `meta.json`: nombre, diseño, hojas, fecha, para listar rápido.
- **Abrir un `.polar` de Mac:** se leen los campos; las fotos con rutas que no existen se marcan como "faltan" y se pide volver a agregarlas (reemplazo guiado).
- **Guardar o compartir `.polar`:** con el selector del sistema (SAF) y la hoja de compartir.
- **Ajustes de la app** en DataStore: tema, unidades, papel predeterminado, bienvenida vista.

### 6.4 Motor y fuentes

- `PolarRenderer` es la única fuente de verdad para vista previa, miniaturas, PDF, PNG e impresión. Recibe un `TextResolver` y aplica filtros con `ColorMatrix`.
- **20 fuentes OFL / Apache** en `res/font`, cargadas como `Typeface` tanto para Compose como para `Canvas`.
- Las fuentes se descargan del repositorio oficial `google/fonts`, **previa autorización del usuario** (se le indican nombres, origen y tamaño). Se guardan sus licencias en `assets/licenses/` y se muestran en Ajustes → Acerca de.

## 7. Preparación para Play Store

- `compileSdk`/`targetSdk` 36, `minSdk` 26.
- `applicationId` provisional `io.github.maverickdev01.polar` (personal, sin dominio de la empresa), cambiable hasta la primera publicación. Nombre en tienda propuesto "Polar · Fotos para imprimir", para no confundirse con la marca Polar de relojes deportivos.
- Release con R8 (`minify` + `shrinkResources`) y reglas para `kotlinx-serialization`; se publica como AAB.
- **Firma:** el usuario crea su llave (lleva contraseña). Se entrega el comando `keytool` y la configuración que lee la contraseña de `~/.gradle/gradle.properties` o de variables de entorno, nunca del repositorio.
- Ícono adaptativo vectorial con capa monocromática (ícono temático), Splash Screen API, edge-to-edge con insets, Back predictivo (`enableOnBackInvokedCallback`).
- Todos los textos en `strings.xml` (español; preparado para inglés).
- Permisos: ninguno peligroso. Se usa el Photo Picker y el selector de archivos (SAF).
- Política de privacidad (no se recolectan ni comparten datos) y respuestas para la sección *Data safety*. Lista de capturas y gráfico de tienda para el final.
- `versionCode`/`versionName`: 2 / 2.0.0.

## 8. Pruebas

- **Unitarias (JUnit, JVM):**
  - Ida y vuelta de `.polar`, más compatibilidad usando como fixtures `Mi primer diseño.polar` y `Diseño - Calendario.polar` de la Mac.
  - `TextResolver`: general, propio, volver al general, aplicar a todas.
  - Fechas y formatos.
  - Transacciones de `UndoStack`.
  - Geometría de hoja y tarjetas para todos los diseños y papeles.
  - Recalcular overrides al cambiar fotos por hoja.
  - Detección de huecos.
  - Fase 3: rectificación de trazos, imanes, espejo, $Q.
- **De interfaz (Compose + Robolectric):** crear proyecto y rellenar, texto por tarjeta, deshacer, borrar con deshacer en Inicio.
- **Visuales en emulador:** capturas con `adb` de teléfono chico, mediano y grande y tablet, en claro, oscuro y letra al 130 %. Una ronda de revisión y una de corrección.
- **Exportación:** el PDF tiene N páginas del tamaño de papel exacto; el PNG mide 2550 × 3300 px para Carta a 300 ppp.
- **Cada fase termina con:** `./gradlew testDebugUnitTest assembleDebug` en verde y un `Polar.apk` actualizado para que lo pruebe el usuario.

## 9. Fuera de alcance

Cuentas, nube, anuncios, compras dentro de la app, la versión web o Windows, y los cambios a la app de Mac (se harán después, siguiendo este mismo diseño). El repositorio no está bajo git; se recomienda iniciarlo antes de implementar.

## 10. Decisiones pendientes del usuario

1. `applicationId` y nombre definitivo en la tienda (antes de publicar).
2. Autorizar la descarga de las fuentes (al empezar la fase 1).
