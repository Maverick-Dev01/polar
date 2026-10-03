# Polar para Android - Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir una versión nativa de Polar para Android en Kotlin y Jetpack Compose, idéntica en estética y funcionalidades a la versión de Mac, adaptativa para móviles y tablets, y generar el archivo APK instalable.

**Architecture:** Arquitectura limpia MVVM con Compose. Motor de renderizado vectorial sobre Canvas de Android para vista previa fluida y exportación precisa a 300 ppp / PDF multipágina. Modelos de datos compatibles con archivos `.polar`.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (Material 3), Android SDK 34/35/36, ZXing (QR codes), kotlinx.serialization / Gson, Android PdfDocument, Coroutines.

**Spec:** `docs/superpowers/specs/2026-10-01-polar-android-design.md`

## Global Constraints
- Ubicación del proyecto Android: `/Users/cattaherrrera/Downloads/Polar/PolarAndroid`
- Compatibilidad mínima: Android 8.0 (API 26)
- Compatibilidad objetivo: Android 14/15 (API 34/35)
- Salida final requerida: APK compilado y verificado en `PolarAndroid/app/build/outputs/apk/debug/app-debug.apk`

## Review Focus
- Serialización y deserialización idéntica de proyectos `.polar` entre Mac y Android.
- Cálculo exacto de las 19 plantillas (incluyendo 5 fotos por tira en películas y días reales de calendario).
- Generación de PDF vectorial multipágina respetando las medidas físicas en puntos tipográficos (Carta, Oficio, A4, etc.).
- Exportación de PNG a 300 ppp sin desbordamiento de memoria (OOM).
- Experiencia táctil fluida en móviles: selección de ranura, zoom por pellizco, desplazamiento táctil.

---

### Task 1: Andamiaje y Configuración de Gradle para Android Studio

**Files:**
- Create: `PolarAndroid/settings.gradle.kts`
- Create: `PolarAndroid/build.gradle.kts`
- Create: `PolarAndroid/gradle.properties`
- Create: `PolarAndroid/app/build.gradle.kts`
- Create: `PolarAndroid/app/src/main/AndroidManifest.xml`
- Create: `PolarAndroid/app/src/main/res/values/strings.xml`
- Create: `PolarAndroid/app/src/main/res/values/colors.xml`
- Create: `PolarAndroid/app/src/main/res/values/themes.xml`

**Interfaces:**
- Produces: Estructura de proyecto estándar de Android Studio con Gradle wrapper funcional.

- [ ] **Paso 1: Configurar settings.gradle.kts y build.gradle.kts raíz**
- [ ] **Paso 2: Configurar app/build.gradle.kts con Compose, Android SDK 34 y dependencias**
- [ ] **Paso 3: Configurar AndroidManifest.xml y recursos base de tema (colores ink y cream)**
- [ ] **Paso 4: Copiar gradlew y verificar que Gradle sincroniza**

---

### Task 2: Modelos de Dominio y Compatibilidad `.polar`

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/model/Models.kt`
- Create: `PolarAndroid/app/src/test/java/com/polar/app/model/ModelTest.kt`

**Interfaces:**
- Produces: `PolarProject`, `PrintSettings`, `TemplateStyle`, `PaperSize`, `PaperOrientation`, `CardFormat`, `CutStyle`, `TextRole`, `TextAppearance`, `PhotoAsset`, `PhotoPlacement`, `ImportedTemplate`, `TemplateRegion`.

- [ ] **Paso 1: Escribir prueba unitaria ModelTest para serializar y deserializar .polar de ejemplo**
- [ ] **Paso 2: Implementar modelos y lógica de normalización y validación en Models.kt**
- [ ] **Paso 3: Ejecutar ./gradlew test y comprobar que pasan los tests de modelos**

---

### Task 3: Algoritmo de Detección de Huecos en Plantillas

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/template/TemplateImporter.kt`
- Create: `PolarAndroid/app/src/test/java/com/polar/app/template/TemplateImporterTest.kt`

**Interfaces:**
- Produces: `TemplateImporter.detectRegions(bitmap: Bitmap): List<TemplateRegion>`

- [ ] **Paso 1: Escribir prueba unitaria para TemplateImporter con imagen sintética con aperturas transparentes y blancas**
- [ ] **Paso 2: Implementar algoritmo BFS de detección de regiones rectangulares y ordenación por filas**
- [ ] **Paso 3: Ejecutar pruebas y verificar detección precisa de hasta 64 ranuras**

---

### Task 4: Motor Gráfico de Renderizado en Canvas de Android

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/engine/PolarRenderer.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/engine/QrGenerator.kt`
- Create: `PolarAndroid/app/src/test/java/com/polar/app/engine/PolarRendererTest.kt`

**Interfaces:**
- Produces: `PolarRenderer.calculateCardRects`, `calculatePhotoRects`, `drawPage(canvas, project, page, isPreview)`

- [ ] **Paso 1: Escribir prueba de geometría para los 19 estilos y 8 tamaños de papel**
- [ ] **Paso 2: Implementar cálculo de rectángulos y ranuras de fotos**
- [ ] **Paso 3: Implementar generador de QR con ZXing**
- [ ] **Paso 4: Implementar dibujo de tarjetas: Polaroid, Cine (perforaciones), Calendario (días gregorianos reales), Instagram, Reproductor con ondas y controles, Corazón, Boleto vintage**
- [ ] **Paso 5: Implementar marcas de corte (esquinas y líneas discontinuas), bordes y fotos redondeadas**
- [ ] **Paso 6: Ejecutar pruebas unitarias de renderizado**

---

### Task 5: Módulos de Exportación (PDF Multipágina y PNG 300 ppp)

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/export/PolarExporter.kt`
- Create: `PolarAndroid/app/src/test/java/com/polar/app/export/PolarExporterTest.kt`

**Interfaces:**
- Produces: `PolarExporter.exportPdf(context, project, outputFile)`, `PolarExporter.exportPng(context, project, page, outputFile)`

- [ ] **Paso 1: Escribir prueba unitaria para exportación de PDF y PNG**
- [ ] **Paso 2: Implementar exportación PDF multipágina mediante android.graphics.pdf.PdfDocument**
- [ ] **Paso 3: Implementar exportación PNG a 300 ppp**
- [ ] **Paso 4: Implementar guardado en MediaStore público y hoja de compartir de Android**

---

### Task 6: ViewModel y Gestión de Estado del Estudio

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/viewmodel/StudioViewModel.kt`
- Create: `PolarAndroid/app/src/test/java/com/polar/app/viewmodel/StudioViewModelTest.kt`

**Interfaces:**
- Produces: `StudioViewModel` con `projectState`, `history (undo/redo)`, `selectedSlot`, `page`, `importPhotos()`, `fillAll()`, `clearPage()`

- [ ] **Paso 1: Escribir prueba para StudioViewModel (cambio de estilo, undo/redo, navegación de hojas)**
- [ ] **Paso 2: Implementar StudioViewModel con StateFlow y manejo asíncrono**
- [ ] **Paso 3: Ejecutar pruebas unitarias**

---

### Task 7: Interfaz de Usuario Adaptativa con Jetpack Compose

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/theme/Theme.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/components/CanvasSheet.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/components/GalleryBar.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/components/DesignSelector.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/components/InspectorDialog.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/screens/StudioScreen.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/MainActivity.kt`

**Interfaces:**
- Produces: Interfaz visual completa, responsiva para móviles y tablets, con tema Tinta & Crema.

- [ ] **Paso 1: Implementar tema PolarTheme (colores Ink #7A293B, Cream #F7F2EB)**
- [ ] **Paso 2: Implementar lienzo CanvasSheet con zoom/paneo táctil y selección de ranuras**
- [ ] **Paso 3: Implementar barra de navegación inferior y carrusel de fotos con PhotoPicker nativo**
- [ ] **Paso 4: Implementar selector de las 19 plantillas con iconos y búsqueda**
- [ ] **Paso 5: Implementar inspector completo (pestañas Foto, Texto, Papel, Diseño)**
- [ ] **Paso 6: Implementar pantalla adaptativa para teléfonos y tablets en MainActivity**

---

### Task 8: Compilación, Generación de APK y Verificación

**Files:**
- Output: `PolarAndroid/app/build/outputs/apk/debug/app-debug.apk`
- Create: `PolarAndroid/README.md`

- [ ] **Paso 1: Ejecutar `./gradlew assembleDebug` y verificar compilación limpia**
- [ ] **Paso 2: Verificar la existencia e integridad del APK generado**
- [ ] **Paso 3: Copiar o enlazar el APK en la raíz de Polar para fácil acceso del usuario**
- [ ] **Paso 4: Documentar instrucciones de uso e instalación en Android Studio y dispositivo**
