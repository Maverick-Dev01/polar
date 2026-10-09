# Fase 3 · Subproyecto 1: Exportación y limpieza — plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Exportar PDFs ligeros y sin pixelado en las dos apps, con calidad seleccionable, avisos de resolución en tres niveles y QR robusto, y quitar los restos de desarrollo (Fotitos, papelera infinita, versiones viejas, textos fijos en el código).

**Architecture:** Cambios localizados en el motor de exportación de cada plataforma: Android `engine/PolarRenderer.kt`, `export/*`, `data/BitmapLoader.kt`; Mac `Renderer.swift`. Se agregan una preferencia `exportQuality` y una constante compartida `FAIR_DPI`. El formato `.polar` no cambia.

**Tech Stack:** Kotlin, Compose, JUnit, Robolectric (Android); Swift, SwiftUI y suites de `check.sh` (Mac).

**Spec:** `docs/superpowers/specs/2026-10-08-fase-3-solidez-design.md` (sección «Subproyecto 1»).

## Global Constraints
- Android desde `PolarAndroid/` con `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`. Debe pasar `./gradlew testDebugUnitTest lintDebug assembleDebug` con 0 fallos y 0 errores de lint.
- Mac desde `PolarMac/`: `./check.sh && ./build.sh` en verde. Agrega pruebas a las suites existentes en `PolarMac/Tests/` (mira cómo las registra `check.sh`).
- Textos de interfaz en español. En Android van en `res/values/strings.xml`, nunca literales.
- Compatibilidad del `.polar`: no se agregan ni se cambian claves en esta fase.
- Git: identidad José Catalino <josecatalino.code@gmail.com>. Commits en español con el formato `fix(android): …` / `feat(mac): …`, **sin Co-Authored-By ni menciones a IA**, sin push.
- No toques `PolarVideo/`.

## Review Focus
1. Un PDF con fotos en «Ajustar», con esquinas redondeadas o con fondo de color debe aprovechar la compresión JPEG en Android y verse igual que antes.
2. Las fotos con fondo transparente sobre un molde importado deben seguir viéndose correctas, aunque no se compriman.
3. «Ligero» nunca debe pesar más que «Alta», y «Alta» nunca más que «Máxima».
4. Un enlace largo de QR nunca debe tumbar la exportación de Mac ni desaparecer en silencio en Android.
5. Vaciar la papelera nunca debe borrar un proyecto activo ni el último borrado que aún se puede deshacer en la sesión.

---

### Task 1: Android — capas opacas para el optimizador JPEG del PDF
**Files:** `engine/PolarRenderer.kt` (~L440–490, `pdfPhoto` y la capa ARGB por foto), `export/PdfPhotoOptimizer.kt`, prueba en `app/src/test/.../export/`.
- [ ] Prueba que falle: renderiza un PDF de una hoja Polaroid con 9 fotos en «Ajustar» (zoom < 1) y con `cornerRadius > 0`. Comprueba que **todas** las fotos se registran para optimizar (fingerprints no nulos) y que el PDF optimizado pesa menos de la mitad que el no optimizado.
- [ ] Implementa: cuando el área bajo la foto es un color uniforme (el fondo de la tarjeta, o el blanco del papel si la tarjeta es transparente), compón la capa sobre ese color antes de `pdfPhoto`. Así queda opaca y `fromBitmap` la acepta. Si la tarjeta está sobre un molde importado, conserva la ruta actual.
- [ ] Prueba visual: rasteriza la página del PDF con `PdfRenderer` a 72 ppp y compárala con el render en pantalla; la diferencia media por canal debe ser ≤ 3.
- [ ] Commit: `fix(android): PDF compacto también para fotos ajustadas o con esquinas`.

### Task 2: Reescalado de alta calidad (ambas)
**Files:** Android `data/BitmapLoader.kt` (ruta API < 28), `engine/PolarRenderer.kt` (dibujar la capa al tamaño exacto). Mac `Renderer.swift` (`interpolationQuality = .high` en todas las rutas).
- [ ] Android: prueba de que la reducción de 4000 px a 1000 px con la nueva función `downscaleProgressive` produce menos aliasing que la versión de un solo paso; usa una imagen de líneas finas y mide la energía de alta frecuencia. Implementa la reducción progresiva a la mitad con `filter=true` y luego un ajuste final.
- [ ] Android: confirma que la capa de la foto se crea al tamaño en píxeles requerido y se dibuja 1:1, sin escala en el `drawBitmap` final. Si no es así, corrígelo.
- [ ] Mac: establece `.high` en los contextos de filtro, fondo y `jpegForPDF`. Prueba en `RendererChecks` que la ruta PDF usa la interpolación alta, por ejemplo con un helper testeable.
- [ ] Commits por plataforma.

### Task 3: Avisos de resolución en tres niveles (ambas)
**Files:** Android `engine/PhotoFit.kt` (`LOW_RES_DPI = 150`, agregar `FAIR_DPI = 220` y una función `quality(dpi): PhotoQuality {LOW, FAIR, GOOD}`), usos en `ui/editor/*` (insignia de la foto, `CropScreen` y el resumen de `FinishScreen`), `strings.xml`. Mac: la función equivalente en `Models.swift` o `Renderer.swift` y sus usos en `LookViews`/`CropView`/`FinishView`.
- [ ] Prueba unitaria de los límites: 149.9 → LOW, 150 → FAIR, 219.9 → FAIR, 220 → GOOD, en ambas plataformas.
- [ ] Interfaz:
  - insignia ámbar «Aceptable» y roja «Baja», con texto, no solo color;
  - en Terminar, «N fotos con resolución baja o aceptable», con un botón «Revisar» que abre la primera en Encuadrar.
- [ ] Commits.

### Task 4: Calidad de exportación seleccionable (ambas)
**Files:** Android `data/SettingsRepository.kt` (`exportQuality: LIGHT|HIGH|MAX`, por defecto HIGH), `export/AndroidExportService.kt`, `export/PolarExporter.kt`, `ui/editor/FinishScreen.kt`. Mac: preferencia en `Studio`/`LibraryStore` settings, `Renderer.swift` (ppp y calidad JPEG), `FinishView`.
- Ligero: fotos a 200 ppp, JPEG 85. Alta: 300 ppp, JPEG 94 (comportamiento actual). Máxima: 300 ppp sin optimizar a JPEG (equivale al actual «PDF sin compresión JPEG»).
- [ ] Pruebas:
  - para el mismo proyecto, tamaño Máxima > Alta > Ligero;
  - un JPG en modo Ligero declara la densidad correcta;
  - la preferencia persiste.
- [ ] Interfaz: control segmentado «Calidad: Ligero · Alta · Máxima» en Terminar, con una línea de ayuda por opción. Quita el menú «PDF sin compresión JPEG», ahora redundante; PNG sigue disponible.
- [ ] Commits.

### Task 5: QR robusto (ambas)
**Files:** Android `engine/QrGenerator.kt`, `engine/PolarRenderer.kt` (~L364), `ui/editor/panels/TextPanel.kt`. Mac `Renderer.swift` (~L624), panel de texto en `PolarApp.swift` (~L505).
- [ ] Pruebas:
  - un enlace de 3000 caracteres produce el estado `TooLong`;
  - el render dibuja un marcador «Enlace muy largo» sin lanzar error;
  - un enlace normal produce un QR decodificable (Android ya usa ZXing para decodificar en pruebas; en Mac usa `CIDetector` QR si está disponible).
- [ ] Interfaz: aviso en línea bajo el campo mientras escribes.
- [ ] Commits.

### Task 6: Limpieza de Mac
**Files:** `PolarApp.swift` (quitar el menú Fotitos, L286–288; corregir la acción vacía del botón Comparar en `LookViews.swift`), `Studio.swift` (quitar la carga automática de L129–133 y `loadFotitos`), `FontCatalog.swift` (quitar el respaldo `#filePath`), `LibraryStore.swift` (purgar `trash/` de más de 7 días al iniciar, más `emptyTrash()`), `LibraryViews.swift` (Ajustes: la versión desde el bundle sin respaldo fijo, y «Vaciar papelera (N)» con confirmación), `README.md` de Mac (2.2.0).
- [ ] Pruebas en `AutosaveChecks` o en una suite nueva:
  - la purga respeta 7 días;
  - `emptyTrash` no toca proyectos activos;
  - la build no incluye la cadena «Fotitos» (busca con grep en `Sources/`).
- [ ] Commit `fix(mac): quitar Fotitos y vaciar papelera`.

### Task 7: Limpieza de Android
**Files:** `PhraseDialog.kt`, `BackgroundControls.kt`, `TextEditDialog.kt`, `BatchPhotosPanel.kt`, `EditorScreen.kt` (~L159, 306), `PhotosPanel.kt` (~L63), `FinishScreen.kt` → `strings.xml`. `data/ProjectStore.kt`: purga de la papelera a los 7 días al iniciar la app (`PolarApplication`) y «Vaciar papelera (N)» en Ajustes.
- [ ] Prueba de la purga de 7 días con un reloj inyectado.
- [ ] Busca literales restantes con `grep -rn 'Text("' app/src/main/java`; deja 0 resultados con letras en español.
- [ ] Commit.

### Task 8: Verificación del subproyecto
- [ ] Android: build completa más un recorrido en el emulador (usuario Demo, con fotos libres).
  - exporta Ligero, Alta y Máxima y registra los tamaños;
  - un enlace largo de QR;
  - vaciar la papelera.
- [ ] Mac: `./check.sh && ./build.sh`, y la misma prueba con la copia demo (`PolarVideo/tools/reabrir_mac.sh`; nunca con la app real junto a Fotitos).
- [ ] Anota los tamaños antes y después en `docs/verificaciones/fase-3-sub1.md`.
