# Fase 3 · Subproyecto 3: Diseños nuevos e importar molde v2 — plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.

**Goal:**
- Seis diseños nuevos idénticos en Android y Mac.
- QR en todos los diseños musicales.
- Asistente de 3 pasos para importar moldes, con detección de formas (rectángulo, redondeado, óvalo), la biblioteca «Mis moldes» y detección de duplicados por huella.

**Spec:** `docs/superpowers/specs/2026-10-08-fase-3-solidez-design.md`, sección «Subproyecto 3».

## Global Constraints
Las de los subproyectos 1 y 2. Además:
- **Compatibilidad del `.polar`:** los campos nuevos son opcionales. `TemplateRegion.shape` vale `"rect"` por defecto y `radius` vale 0.0. Los ids de estilo nuevos que no conozca la otra plataforma (por ejemplo, una versión anterior) deben degradar a `polaroid` sin fallar.
  - **Mac:** ya rechaza claves desconocidas en `settings.textStyles`. No agregues nada ahí.
  - **Android:** usa `coerceInputValues` para los enums.
- **Geometría compartida:** las posiciones de foto y de texto de cada diseño nuevo se definen en puntos relativos a la tarjeta, en una tabla que se escribe igual en `StyleInfo.kt` y `Models.swift`.
- **Prueba de paridad:** un fixture JSON (`shared-fixtures/estilos-geometria.json` en la raíz del repo) que ambas suites leen y contra el que comparan, con tolerancia de 0.5 pt.

## Review Focus
1. Un proyecto con un estilo nuevo, abierto en la versión 2.2.0 de la otra plataforma, no debe crashear; debe degradar a Polaroid.
2. Un molde con agujeros redondos debe detectarse como óvalo y recortar la foto con esa forma, tanto en vista previa como en PDF.
3. El mismo molde guardado como JPG y como PNG, o reescalado, debe reconocerse como duplicado.
4. Dos moldes distintos pero parecidos (misma plantilla con otro color de texto) **sí** pueden marcarse como parecidos; el usuario decide.
5. Borrar un molde de «Mis moldes» no debe romper los proyectos que lo usan, porque cada proyecto lleva su propia copia.

---

### Task 1: Tabla de geometría compartida y prueba de paridad
- Crea `shared-fixtures/estilos-geometria.json` con los 6 estilos nuevos:
  - proporción de la tarjeta;
  - rectángulos de foto (uno o cuatro);
  - rectángulos de texto por rol;
  - forma de la foto: rect, círculo en vinilo;
  - adornos: cinta washi, agujeros del casete, surcos del vinilo, como primitivas simples.
- Pruebas en ambas plataformas que leen el fixture y lo comparan con la geometría del código.

### Task 2: Android — los 6 estilos
- `photobooth`, `instaxWide`, `vinyl`, `cassette`, `collage`, `washi` en `Models.kt`/`StyleInfo.kt`, con su categoría, nombre y descripción.
- Dibujo en `PolarRenderer`, miniatura en `Thumbnailer.styleCard` y fotos por tarjeta (fotomatón = 4, collage = 3).
- Pruebas: cada estilo × Carta/A4/4×6 × vertical/horizontal renderiza sin excepción, y la geometría coincide con el fixture.

### Task 3: Mac — los 6 estilos
Equivalente a la tarea 2 en `Models.swift`, `Renderer.swift` y la miniatura del catálogo, con pruebas en `RendererChecks` y `ParityChecks`.

### Task 4: QR en todos los diseños musicales (ambas)
- `spotify`, `playerRed`, `playerGray`, `vinyl` y `cassette` dibujan el QR cuando hay enlace.
- La posición se define en la tabla de geometría.

### Task 5: Modelo de molde v2 y «Mis moldes» (ambas)
- `TemplateRegion`: `shape` y `radius`.
- **Biblioteca:**
  - Android: `filesDir/templates/<id>/`.
  - Mac: `Application Support/Polar/templates/<id>/`.
  - Cada molde guarda `molde.png` y `meta.json` (`id`, `nombre`, `dhash` en 16 hex, `sha256`, `regiones`, `creado`).
- **API:** `list`, `save`, `delete`, `findDuplicate(image) -> (molde, distancia)?`.
- **dHash:** escala de grises a 9×8, se compara cada píxel con el de su derecha (64 bits). Duplicado si `sha256` es igual o si la distancia de Hamming es de 6 o menos.
- **Pruebas:**
  - dHash estable al reescalar 50 % y 200 % y al convertir a JPG calidad 80;
  - distancia mayor que 12 entre moldes distintos;
  - duplicado por SHA;
  - persistencia y borrado.

### Task 6: Detección de formas (ambas)
- **Para cada componente detectado:**
  - si el área llena es 90 % del rectángulo o más, la forma es `rect`;
  - si no, calcula el ajuste a una elipse inscrita y a un rectángulo redondeado (radio estimado por las esquinas) y elige el de mayor IoU con la máscara;
  - si el IoU es menor que 0.85, cae a `rect` y avisa «forma aproximada».
- Recorte de la foto por forma en el render.
- **Pruebas** con máscaras sintéticas: cuadrado, rectángulo redondeado con radio de 20 % y círculo. Verifican la forma elegida y que el radio estimado está dentro de ±5 %.

### Task 7: Asistente de importación, Android
Pantalla de 3 pasos:
1. **Elegir la imagen:** explicación ilustrada y selector.
2. **Revisar los espacios:**
   - superposición numerada; arrastrar para mover; asas en las 4 esquinas para cambiar el tamaño;
   - un selector de forma por espacio; «＋ Agregar» y «Quitar»;
   - accesible: cada espacio tiene acciones personalizadas de mover y redimensionar.
3. **Nombre y guardar.**

Si `findDuplicate` encuentra un molde parecido, el aviso aparece entre los pasos 1 y 2: «Usar el existente» / «Guardar como nuevo».

**Entradas:**
- desde el catálogo («Tu propio molde»);
- desde Diseño → «Usar mi molde»;
- desde «Mis moldes» en el catálogo, una nueva categoría que muestra los moldes guardados como tarjetas.

**Pruebas:** el estado del asistente y la navegación entre pasos.

### Task 8: Asistente de importación, Mac
Lo mismo que la tarea 7 en una hoja de SwiftUI, con arrastre directo y asas. Las entradas son el menú Proyecto, el riel y la categoría «Mis moldes» del catálogo. Pruebas en `TemplateChecks`.

### Task 9: Verificación
- Builds y recorridos en las dos plataformas.
- Prueba cruzada: un proyecto con un estilo nuevo y un molde redondo creado en Android se abre en Mac con el mismo resultado, y viceversa (fixtures en `shared-fixtures/`).
