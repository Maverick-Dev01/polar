# Fase 3 · Subproyecto 2: Paridad y navegación — plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.

**Goal:** Que Android y Mac tengan las mismas funciones en el mismo lugar lógico, sacar a la vista lo que está escondido y pulir la simetría de todas las pantallas.

**Architecture:** Cambios en la capa de interfaz:
- Android: `ui/*`.
- Mac: `PolarApp.swift`, `LibraryViews.swift`, `LookViews.swift`, `PhotoStudioViews.swift`.

Más dos funciones nuevas en datos:
- Android: importar una carpeta (`PhotoImporter`) y buscar diseños (`CatalogViewModel`).
- Mac: navegar entre hojas con gestos y teclado.

**Spec:** `docs/superpowers/specs/2026-10-08-fase-3-solidez-design.md`, sección «Subproyecto 2».

## Global Constraints
Las mismas del plan del subproyecto 1 (builds, textos en `strings.xml`, git sin menciones a IA, `.polar` sin cambios). Además:
- **Usa la skill `impeccable`** (`/impeccable audit` y luego `polish`) y `redesign-existing-projects` como guía de criterio. En las apps nativas aplica los principios, no el código web.
- **Simetría:**
  - tarjetas de la misma altura;
  - filas de botones con el mismo ancho (`weight(1f)` / `.frame(maxWidth: .infinity)`) y la misma altura mínima (48 dp/pt; 56 en acciones principales);
  - sin alturas fijas que recorten;
  - tokens de espaciado y radios.
- **Evidencia visual obligatoria:** capturas antes y después en `docs/capturas/fase-3/sub2/` (no se versionan las PNG).
  - Android en 360 dp, 411 dp, 1280×800, claro, oscuro y letra al 130 %.
  - Mac en 1100×720 y 1600×1000, con la copia demo de `PolarVideo/tools` (**nunca** con la app real).

## Review Focus
1. Con letra al 130 % en 360 dp, la barra superior del editor nunca debe encimar Rehacer e Imprimir.
2. Importar una carpeta con archivos no imagen o corruptos debe ignorarlos y avisar cuántos.
3. La búsqueda de diseños sin resultados debe mostrar un estado vacío útil.
4. La sección «Canción» solo aparece en diseños musicales y su campo de enlace siempre está visible sin desplazarse.
5. Deslizar entre hojas en Mac no debe cambiar de hoja mientras se arrastra una foto o se mueve un hueco.

---

### Task 1: Android — agregar una carpeta de fotos
- `PhotosPanel` y `PhotoImporter`.
- `ACTION_OPEN_DOCUMENT_TREE`; lista las imágenes del nivel superior (jpg, jpeg, png, heic, webp), ordenadas por nombre, con un tope de 500.
- Importa con la misma tubería que el selector de fotos.
- Mensaje al terminar: «N fotos agregadas · M archivos ignorados».
- Pruebas con un `DocumentFile` falso: orden, tope y filtrado.

### Task 2: Android — buscar en el catálogo
- `CatalogViewModel` y `CatalogContent`.
- Campo de búsqueda arriba de los chips; normaliza acentos y mayúsculas; busca en nombre y descripción.
- Estado vacío: «No hay diseños con "x"» con un botón «Ver todos».
- Pruebas del filtro.

### Task 3: Android — paridad de Papel y Ajustes
- Margen en las unidades elegidas: mm o pulgadas, con conversión exacta y redondeo de presentación.
- Papel predeterminado con los 8 tamaños.
- «Borde de las tarjetas» y el margen fuera de «Más opciones».
- Pruebas de conversión.

### Task 4: Android — barra superior y menú contextual
- **Barra del editor:** Atrás · título y estado · Deshacer · Rehacer · ⋮ · botón «Imprimir» (ícono y texto).
  - En anchos menores de 400 dp o con letra mayor a 1.15, Rehacer pasa al menú.
- **Menú contextual de la tarjeta:** Cambiar · Encuadrar · **Quitar fondo** · Girar · Texto.
  - «Quitar fondo» abre Encuadrar con la sección de fondo enfocada.
- Pruebas de Compose: los botones existen con su descripción accesible; con fuente 1.3 a 360 dp no se encima nada (verifica los límites).

### Task 5: Ambas — panel Texto reorganizado
- **Diseños musicales:** sección «Canción» primero, con título, artista y «Enlace para el QR».
- «Mostrar este texto» y «Tamaño» (Auto/S/M/L más el exacto) visibles; posición, desplazamiento y alineación en «Más opciones».
- Mantener el orden equivalente en Mac.
- Pruebas de estado del panel en Android (`TextPanelState`).

### Task 6: Mac — navegar entre hojas
- Gesto de deslizar con dos dedos (umbral y debounce) y flechas ← → cuando la hoja tiene el foco.
- Desactivado mientras se arrastra una foto o se editan huecos.
- Prueba de la lógica de umbral en `StudioChecks`.

### Task 7: Auditoría y pulido visual de Android (Impeccable)
- Recorre Inicio, Catálogo, Editor (5 paneles), Encuadrar, Terminar, Ajustes y bienvenida en las configuraciones de Global Constraints.
- Corrige todo lo que viole la simetría o se enciame; anota cada hallazgo en `docs/auditoria-fase-3-android.md` con su estado.
- Un commit por pantalla.

### Task 8: Auditoría y pulido visual de Mac (Impeccable)
- Igual que la tarea 7 para Mac: Biblioteca, Editor (riel, hoja, inspector y pestañas), Encuadrar, Terminar, Ajustes y Bienvenida.
- Anota los hallazgos en `docs/auditoria-fase-3-mac.md`.

### Task 9: Verificación
- Builds completas en ambas plataformas.
- Recorrido en el emulador (usuario Demo) y en la Mac demo.
- Tabla de paridad final en `docs/paridad-fase-3.md`, con la ubicación de cada función en ambas apps.
