# Polar Android · Fase 1 (base sólida) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reconstruir Polar Android con la identidad de Mac, la navegación Inicio + Editor, autoguardado, `.polar` compatible con Mac, texto por tarjeta (con color, fuente y fecha), paridad completa con Mac y preparación técnica para Play Store.

**Architecture:** Modelo inmutable serializable, compatible con el JSON de Mac. Lógica pura y probada en JVM: `TextResolver`, `ProjectEdits`, `UndoStack`, `CardTextLayout`, `ProjectStore`. Un solo motor de dibujo (`PolarRenderer`) para vista previa, miniaturas, PDF y PNG. UI en Jetpack Compose + Material 3 con tres destinos (Bienvenida, Inicio, Catálogo, Editor y Ajustes); Encuadrar y Terminar son subpantallas del Editor que comparten su ViewModel. La disposición cambia a 3 paneles cuando el ancho es ≥ 840dp.

**Tech Stack:** Kotlin 2.2, AGP 8.11, Compose BOM 2025.06.01, Material 3, Navigation Compose 2.9 (rutas tipadas), DataStore, kotlinx-serialization 1.9, ExifInterface, ZXing, JUnit 4 y Robolectric 4.15 (pruebas de Compose en JVM).

**Spec:** `docs/superpowers/specs/2026-10-02-polar-android-rediseno-design.md` (léelo completo antes de empezar). Prototipo visual aprobado: https://claude.ai/artifact/TVJe3AQG7ZQ5YtgFERHi8v

## Global Constraints

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`; `applicationId = "io.github.maverickdev01.polar"`; `namespace` y paquetes Kotlin siguen siendo `com.polar.app`.
- `versionCode = 2`, `versionName = "2.0.0"`.
- Sin conexión, sin cuentas, sin anuncios, sin permisos peligrosos: Photo Picker y SAF.
- Los originales nunca se modifican; las fotos se copian a `filesDir/projects/<id>/photos/`.
- **Compatibilidad Mac:**
  - Al escribir se incluyen todas las claves (`encodeDefaults = true`).
  - `settings.textStyles` sólo usa las claves `title`, `subtitle`, `caption`, `song`, `artist`. Cualquier otra hace que la Mac rechace el archivo.
  - Los ids de fotos y regiones son UUID en mayúsculas.
  - `version = 1`.
  - La fecha especial va en `settings.specialDate`, en segundos desde 2001-01-01 UTC.
- **Colores de la interfaz** sólo desde el tema (`MaterialTheme.colorScheme` y `PolarColors`), nunca hex sueltos en pantallas; la hoja es siempre blanca. Valores exactos en la sección 3 del spec.
- **Accesibilidad:**
  - Objetivos táctiles ≥ 48dp.
  - Texto en `sp` mediante `MaterialTheme.typography`.
  - `contentDescription` en todo ícono que actúe solo.
  - Nada se comunica sólo con color.
- **Textos de la interfaz** en `res/values/strings.xml`, en español, frases en mayúscula inicial, sin "por favor" ni "exitosamente". La excepción son los nombres de diseños y fuentes que vienen del modelo.
- **Git:** identidad local del repo (José Catalino <josecatalino.code@gmail.com>). Nunca agregar `Co-Authored-By` ni menciones a IA en commits. Rama `fase-1-base`.
- **JDK para Gradle:** `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` en cada terminal que corra `./gradlew`; todos los comandos se corren desde `PolarAndroid/`.

## Review Focus

1. **`.polar` de Mac con fotos que no existen en el teléfono:** debe abrir, contar las fotos que faltan y dibujar el marcador de posición, sin caerse (Task 9, `ProjectStoreTest.openMacProjectWithMissingPhotos`).
2. **App cerrada o matada a media edición:** el último cambio queda guardado (Task 13, `EditorViewModelTest.flushSavesPendingChanges`).
3. **Texto propio vacío en una tarjeta:** esa tarjeta no muestra pie de foto y las demás siguen con el general. Un texto propio vacío no es lo mismo que no tener texto propio (Task 7, `CardTextLayoutTest.emptyOwnTextHidesOnlyThatCard`).
4. **Cambiar a un diseño con 5 fotos por tarjeta teniendo textos propios:** cada texto pasa a la tarjeta que contiene su primera foto; si dos chocan gana el primero (Task 4, `ProjectEditsTest.selectStyleRemapsOverridesWhenPhotosPerCardChanges`).
5. **Fotos enormes o rotadas por EXIF:** se decodifican con muestreo (sin quedarse sin memoria) y con orientación correcta; las dimensiones guardadas ya vienen rotadas (Task 7, `BitmapMathTest`).

---

## Mapa de archivos

```
PolarAndroid/
├── gradle/libs.versions.toml                     (Task 1: versiones nuevas)
├── app/build.gradle.kts                          (Task 1, Task 21)
├── app/proguard-rules.pro                        (Task 21)
├── tools/download-fonts.sh                       (Task 6)
└── app/src/
    ├── main/
    │   ├── AndroidManifest.xml                   (Task 10, Task 21)
    │   ├── assets/licenses/*.txt                 (Task 6)
    │   ├── res/font/*.ttf                        (Task 6)
    │   ├── res/values/strings.xml                (todas las tareas de UI)
    │   ├── res/values/themes.xml                 (Task 10)
    │   ├── res/drawable/ic_launcher_*.xml        (Task 21)
    │   ├── res/mipmap-anydpi-v26/ic_launcher.xml (Task 21)
    │   └── java/com/polar/app/
    │       ├── MainActivity.kt                   (Task 2 temporal, Task 10)
    │       ├── PolarApplication.kt               (Task 10: contenedor de dependencias)
    │       ├── model/Models.kt                   (Task 2: modelo inmutable)
    │       ├── model/PolarJson.kt                (Task 2)
    │       ├── model/StyleInfo.kt                (Task 2: categorías, roles, descripciones)
    │       ├── core/text/DateText.kt             (Task 3)
    │       ├── core/text/TextResolver.kt         (Task 3)
    │       ├── core/edit/ProjectEdits.kt         (Task 4)
    │       ├── core/edit/MoodPreset.kt           (Task 4)
    │       ├── core/history/UndoStack.kt         (Task 5)
    │       ├── data/FontCatalog.kt               (Task 6)
    │       ├── data/AndroidFontProvider.kt       (Task 6)
    │       ├── data/BitmapMath.kt                (Task 7)
    │       ├── data/BitmapLoader.kt              (Task 7)
    │       ├── data/ProjectStore.kt              (Task 9)
    │       ├── data/PhotoImporter.kt             (Task 9)
    │       ├── data/SettingsRepository.kt        (Task 9)
    │       ├── engine/FontProvider.kt            (Task 7)
    │       ├── engine/CardTextLayout.kt          (Task 7)
    │       ├── engine/PolarRenderer.kt           (Task 7: usa CardTextLayout + fuentes)
    │       ├── engine/Thumbnailer.kt             (Task 12)
    │       ├── engine/QrGenerator.kt             (sin cambios)
    │       ├── export/PolarExporter.kt           (Task 20)
    │       ├── template/TemplateImporter.kt      (Task 8)
    │       └── ui/
    │           ├── theme/{Color,Type,Shape,Theme}.kt                  (Task 10)
    │           ├── LocalLayout.kt                                     (Task 10)
    │           ├── navigation/{Routes,PolarNavHost}.kt               (Task 10)
    │           ├── components/{PolaroidCard,ChipRow,SwitchRow,Section}.kt (Task 10–11)
    │           ├── onboarding/OnboardingScreen.kt                    (Task 10)
    │           ├── settings/{SettingsScreen,SettingsViewModel}.kt    (Task 10)
    │           ├── home/{HomeScreen,HomeViewModel}.kt                (Task 11)
    │           ├── catalog/{CatalogScreen,CatalogContent}.kt         (Task 12)
    │           └── editor/
    │               ├── EditorViewModel.kt, EditorState.kt            (Task 13)
    │               ├── EditorScreen.kt, SheetCanvas.kt, ContextBar.kt, ToolNav.kt (Task 14)
    │               ├── panels/{PhotosPanel,DesignPanel,TextPanel,PaperPanel}.kt (Task 15–18)
    │               ├── CropScreen.kt                                 (Task 19)
    │               └── FinishScreen.kt                               (Task 20)
    └── test/
        ├── resources/fixtures/*.polar            (Task 2: copias de los archivos de Mac)
        └── java/com/polar/app/…                  (pruebas de cada tarea)
```

**Se borran en la Task 2:** `ui/screens/StudioScreen.kt`, `ui/components/{CanvasSheet,DesignSelector,GalleryBar,InspectorDialog}.kt`, `ui/viewmodel/StudioViewModel.kt`, `test/.../viewmodel/StudioViewModelTest.kt` y `ui/theme/Theme.kt` (se reescribe en la Task 10).

---

### Task 1: Rama de trabajo, SDK 36 y dependencias

**Files:**
- Modify: `PolarAndroid/gradle/libs.versions.toml` (reemplazo completo)
- Modify: `PolarAndroid/app/build.gradle.kts` (reemplazo completo)

**Interfaces:**
- Produces: alias de catálogo `libs.androidx.navigation.compose`, `libs.androidx.datastore.preferences`, `libs.androidx.core.splashscreen`, `libs.androidx.exifinterface`, `libs.robolectric`, `libs.androidx.compose.ui.test.junit4`, `libs.androidx.compose.ui.test.manifest`, `libs.androidx.test.core`.

- [ ] **Step 1: Crear la rama**

```bash
cd /Users/cattaherrrera/Downloads/Polar
git checkout -b fase-1-base
git config user.email   # debe imprimir josecatalino.code@gmail.com
```

- [ ] **Step 2: Reemplazar `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.11.1"
kotlin = "2.2.0"
coreKtx = "1.16.0"
lifecycle = "2.9.1"
activityCompose = "1.10.1"
composeBom = "2025.06.01"
navigationCompose = "2.9.0"
datastore = "1.1.7"
splashscreen = "1.0.1"
exifinterface = "1.4.1"
kotlinxSerialization = "1.9.0"
coroutines = "1.10.2"
zxing = "3.5.3"
junit = "4.13.2"
robolectric = "4.15.1"
androidxTestCore = "1.6.1"
androidxTestExt = "1.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "splashscreen" }
androidx-exifinterface = { group = "androidx.exifinterface", name = "exifinterface", version.ref = "exifinterface" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
zxing-core = { group = "com.google.zxing", name = "core", version.ref = "zxing" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-core = { group = "androidx.test", name = "core-ktx", version.ref = "androidxTestCore" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

> Si alguna versión no resuelve, usa la versión estable más reciente que aparezca en `https://maven.google.com/web/index.html` (AndroidX) o Maven Central (el resto), y anótalo en el mensaje del commit.

- [ ] **Step 3: Reemplazar `app/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.polar.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.maverickdev01.polar"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
```

Si `app/proguard-rules.pro` no existe, créalo vacío: `touch app/proguard-rules.pro`.

- [ ] **Step 4: Compilar y correr las pruebas existentes**

```bash
cd PolarAndroid
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew testDebugUnitTest assembleDebug --console=plain
```
Expected: `BUILD SUCCESSFUL`. Las advertencias de APIs obsoletas en la UI vieja se aceptan, porque esa UI se borra en la Task 2.

- [ ] **Step 5: Commit**

```bash
git add PolarAndroid/gradle/libs.versions.toml PolarAndroid/app/build.gradle.kts PolarAndroid/app/proguard-rules.pro
git commit -m "build: SDK 36, applicationId personal y dependencias de la fase 1"
```

---

### Task 2: Modelo inmutable y JSON compatible con Mac

**Files:**
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/model/Models.kt` (reemplazo completo)
- Create: `PolarAndroid/app/src/main/java/com/polar/app/model/StyleInfo.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/model/PolarJson.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/MainActivity.kt` (temporal)
- Delete: archivos de la UI vieja listados arriba, más `test/.../viewmodel/StudioViewModelTest.kt`
- Create: `PolarAndroid/app/src/test/resources/fixtures/{mac_polaroid,mac_oficio_textstyles,mac_imported,mac_calendar}.polar`
- Modify: `PolarAndroid/app/src/test/java/com/polar/app/model/ModelTest.kt` (reemplazo completo)
- Create: `PolarAndroid/app/src/test/java/com/polar/app/model/MacCompat.kt`
- Create: `PolarAndroid/app/src/test/java/com/polar/app/model/PolarJsonTest.kt`

**Interfaces:**
- Produces:
  - `fun newId(): String` (UUID en mayúsculas).
  - `object SwiftDate { fromEpochMs(Long): Double; toEpochMs(Double): Long; now(): Double }`.
  - `enum TextRole(key: String, displayName: String)` con `TITLE, SUBTITLE, CAPTION, SONG, ARTIST, DATE` y `val isMacRole: Boolean`.
  - `enum DateSource { NONE, PHOTO, CHOSEN }` y `enum DateStyle { DAY_MONTH_YEAR, NUMERIC, MONTH_YEAR }`.
  - `data class CardOverride(texts: Map<String,String>, styles: Map<String,TextAppearance>, dateSource: DateSource?, chosenDate: Double?)` con `val isEmpty`.
  - `PrintSettings.text(role)`, `withText(role, value)`, `textStyle(role)` y `withTextStyle(role, appearance)`.
  - `PolarProject` inmutable con `name`, `updatedAtEpochMs` y `cardOverrides: Map<String, CardOverride>`; `cardsPerPage`, `cardOfSlot(slot)`, `firstSlotOfCard(card)`, `override(card)`, `normalized(): PolarProject`, `withStyle(style): PolarProject`, `validated()`, `effectiveDPI(...)`.
  - `PhotoAsset.takenAtEpochMs: Long?`.
  - `object PolarJson { encode(PolarProject): String; decode(String): PolarProject }`.
  - En `StyleInfo.kt`: `enum DesignCategory`, `TemplateStyle.category`, `.textRoles`, `.supportsDate`, `.description`.

- [ ] **Step 1: Copiar los archivos de Mac como fixtures**

```bash
cd /Users/cattaherrrera/Downloads/Polar
mkdir -p PolarAndroid/app/src/test/resources/fixtures
cp "Mi primer diseño.polar" PolarAndroid/app/src/test/resources/fixtures/mac_polaroid.polar
cp "Prueba de edición.polar" PolarAndroid/app/src/test/resources/fixtures/mac_oficio_textstyles.polar
cp "PolarMac/Examples/Plantilla importada.polar" PolarAndroid/app/src/test/resources/fixtures/mac_imported.polar
cp "Diseño - Calendario.polar" PolarAndroid/app/src/test/resources/fixtures/mac_calendar.polar
```

- [ ] **Step 2: Escribir el verificador de reglas de Mac (helper de prueba)**

`app/src/test/java/com/polar/app/model/MacCompat.kt`:

```kotlin
package com.polar.app.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Replica las reglas de decodificación y validación de PolarMac (Models.swift). */
object MacCompat {
    private val uuid = Regex("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$")
    private val macRoles = setOf("title", "subtitle", "caption", "song", "artist")
    private val photoKeys = setOf("id", "path", "pixelWidth", "pixelHeight")
    private val placementKeys = setOf("assetID", "zoom", "offsetX", "offsetY", "quarterTurns")
    private val appearanceKeys = setOf("fontName", "size", "hex", "bold", "italic", "alignment", "offsetX", "offsetY", "visible")
    private val regionKeys = setOf("id", "x", "y", "width", "height", "isTransparent")

    fun assertReadable(text: String) {
        val root = Json.parseToJsonElement(text).jsonObject
        assertEquals("version", 1, root["version"]!!.jsonPrimitive.intOrNull)
        val photos = root["photos"] as JsonArray
        for (p in photos) {
            val o = p.jsonObject
            assertTrue("foto sin claves: ${photoKeys - o.keys}", o.keys.containsAll(photoKeys))
            assertTrue("id de foto no es UUID", uuid.matches(o["id"]!!.jsonPrimitive.content))
        }
        for (p in root["placements"] as JsonArray) {
            if (p is JsonNull) continue
            val o = p.jsonObject
            assertTrue("colocación sin claves: ${placementKeys - o.keys}", o.keys.containsAll(placementKeys))
            assertTrue("assetID no es UUID", uuid.matches(o["assetID"]!!.jsonPrimitive.content))
        }
        val settings = root["settings"]!!.jsonObject
        assertTrue("specialDate debe ser número", settings["specialDate"]?.jsonPrimitive?.content?.toDoubleOrNull() != null)
        val styles = settings["textStyles"] as? JsonObject ?: JsonObject(emptyMap())
        assertTrue("rol desconocido para Mac: ${styles.keys - macRoles}", macRoles.containsAll(styles.keys))
        for ((_, a) in styles) assertTrue("apariencia sin claves", a.jsonObject.keys.containsAll(appearanceKeys))
        (settings["importedTemplate"] as? JsonObject)?.let { t ->
            for (r in t["regions"] as JsonArray) {
                val o = r.jsonObject
                assertTrue("región sin claves", o.keys.containsAll(regionKeys))
                assertTrue("id de región no es UUID", uuid.matches(o["id"]!!.jsonPrimitive.content))
            }
        }
    }
}
```

- [ ] **Step 3: Escribir las pruebas de JSON (fallan)**

`app/src/test/java/com/polar/app/model/PolarJsonTest.kt`:

```kotlin
package com.polar.app.model

import org.junit.Assert.*
import org.junit.Test

class PolarJsonTest {
    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test
    fun decodesMacPolaroidProject() {
        val p = PolarJson.decode(fixture("mac_polaroid.polar"))
        assertEquals(TemplateStyle.POLAROID, p.settings.style)
        assertTrue(p.photos.isNotEmpty())
        assertEquals(p.photos[0].id, p.placements[0]!!.assetID)
        assertEquals(812592909.24548, p.settings.specialDate, 0.0001)
        assertEquals(PaperSize.LETTER, p.settings.paperSize)
    }

    @Test
    fun decodesMacTextStylesAndPaper() {
        val p = PolarJson.decode(fixture("mac_oficio_textstyles.polar"))
        assertEquals(PaperSize.OFICIO, p.settings.paperSize)
        assertEquals(PaperOrientation.LANDSCAPE, p.settings.orientation)
        assertEquals("Recuerdos para siempre", p.settings.title)
        val title = p.settings.textStyle(TextRole.TITLE)
        assertTrue(title.bold)
        assertEquals(13.0, title.size, 0.0)
        assertEquals(12.0, title.offsetX, 0.0)
        assertEquals("Georgia", title.fontName)
    }

    @Test
    fun decodesMacImportedTemplate() {
        val p = PolarJson.decode(fixture("mac_imported.polar"))
        assertEquals(TemplateStyle.IMPORTED, p.settings.style)
        assertTrue(p.settings.importedTemplate!!.regions.isNotEmpty())
    }

    @Test
    fun roundTripStaysMacReadable() {
        for (name in listOf("mac_polaroid.polar", "mac_oficio_textstyles.polar", "mac_imported.polar", "mac_calendar.polar")) {
            val p = PolarJson.decode(fixture(name))
            val text = PolarJson.encode(p)
            MacCompat.assertReadable(text)
            assertEquals(p, PolarJson.decode(text))
        }
    }

    @Test
    fun newFieldsNeverBreakMac() {
        val photo = PhotoAsset(path = "photos/a.jpg", pixelWidth = 100, pixelHeight = 100, takenAtEpochMs = 1_771_070_400_000)
        val dateStyle = TextAppearance(fontName = "Caveat", hex = "FF8C2E")
        val p = PolarProject(
            settings = PrintSettings(dateSource = DateSource.PHOTO).withTextStyle(TextRole.DATE, dateStyle),
            photos = listOf(photo),
            placements = listOf(PhotoPlacement(assetID = photo.id)),
            name = "Boda",
            cardOverrides = mapOf("0" to CardOverride(texts = mapOf("caption" to "El brindis")))
        )
        val text = PolarJson.encode(p)
        MacCompat.assertReadable(text) // falla si "date" aparece en settings.textStyles
        val back = PolarJson.decode(text)
        assertEquals("FF8C2E", back.settings.textStyle(TextRole.DATE).hex)
        assertEquals("El brindis", back.cardOverrides["0"]!!.texts["caption"])
    }

    @Test
    fun rejectsInvalidFileWithSpanishMessage() {
        val e = assertThrows(PolarException::class.java) { PolarJson.decode("{\"version\": 7}") }
        assertTrue(e.message!!.contains("versión"))
        assertThrows(PolarException::class.java) { PolarJson.decode("no es json") }
    }
}
```

Reemplaza `app/src/test/java/com/polar/app/model/ModelTest.kt` por:

```kotlin
package com.polar.app.model

import org.junit.Assert.*
import org.junit.Test

class ModelTest {
    @Test
    fun defaultProject() {
        val p = PolarProject().normalized()
        assertEquals(9, p.settings.capacity)
        assertEquals(1, p.pageCount)
        assertEquals(9, p.placements.size)
        assertEquals(9, p.cardsPerPage)
    }

    @Test
    fun pageCountFromPlacements() {
        val p = PolarProject(placements = List(20) { null })
        assertEquals(3, p.pageCount)
        assertEquals(27, p.normalized().placements.size)
    }

    @Test
    fun filmHasFivePhotosPerCard() {
        val s = PrintSettings(style = TemplateStyle.FILM_VERTICAL, columns = 2, rows = 1)
        val p = PolarProject(settings = s)
        assertEquals(10, s.capacity)
        assertEquals(2, p.cardsPerPage)
        assertEquals(1, p.cardOfSlot(7))
        assertEquals(5, p.firstSlotOfCard(1))
    }

    @Test
    fun paperSizesInPoints() {
        assertEquals(612.0, PrintSettings(paperSize = PaperSize.LETTER).paperSizePoints.width, 0.01)
        assertEquals(792.0, PrintSettings(paperSize = PaperSize.LETTER).paperSizePoints.height, 0.01)
        val landscape = PrintSettings(paperSize = PaperSize.LETTER, orientation = PaperOrientation.LANDSCAPE).paperSizePoints
        assertEquals(792.0, landscape.width, 0.01)
        assertEquals(595.28, PrintSettings(paperSize = PaperSize.A4).paperSizePoints.width, 0.01)
    }

    @Test
    fun dateRoleGoesToExtraStyles() {
        val s = PrintSettings().withTextStyle(TextRole.DATE, TextAppearance(hex = "112233"))
        assertFalse(s.textStyles.containsKey("date"))
        assertEquals("112233", s.extraTextStyles["date"]!!.hex)
        assertEquals("112233", s.textStyle(TextRole.DATE).hex)
    }

    @Test
    fun withStyleSetsDefaultGridAndTrimsTrailingEmpty() {
        val photo = PhotoAsset(path = "a", pixelWidth = 1, pixelHeight = 1)
        val p = PolarProject(photos = listOf(photo), placements = listOf(PhotoPlacement(photo.id), null, null))
        val film = p.withStyle(TemplateStyle.FILM_VERTICAL)
        assertEquals(2, film.settings.columns)
        assertEquals(1, film.settings.rows)
        assertEquals(10, film.placements.size)
    }

    @Test
    fun swiftDateRoundTrip() {
        val ms = 1_771_070_400_000L
        assertEquals(ms, SwiftDate.toEpochMs(SwiftDate.fromEpochMs(ms)))
        assertEquals(0.0, SwiftDate.fromEpochMs(978_307_200_000L), 0.0)
    }

    @Test
    fun validationRejectsBadColorAndUnknownOverrideRole() {
        assertThrows(PolarException::class.java) { PolarProject(settings = PrintSettings(accentHex = "zzz")).validated() }
        val bad = PolarProject(cardOverrides = mapOf("0" to CardOverride(texts = mapOf("nope" to "x"))))
        assertThrows(PolarException::class.java) { bad.validated() }
    }

    @Test
    fun newIdsAreUppercaseUuids() {
        assertTrue(Regex("^[0-9A-F-]{36}$").matches(newId()))
    }
}
```

- [ ] **Step 4: Correr y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.model.*' --console=plain`
Expected: FAIL por errores de compilación (`PolarJson`, `CardOverride`, `withTextStyle`, … no existen).

- [ ] **Step 5: Borrar la UI vieja y dejar un `MainActivity` temporal**

```bash
cd PolarAndroid/app/src
git rm -q main/java/com/polar/app/ui/screens/StudioScreen.kt \
  main/java/com/polar/app/ui/components/CanvasSheet.kt \
  main/java/com/polar/app/ui/components/DesignSelector.kt \
  main/java/com/polar/app/ui/components/GalleryBar.kt \
  main/java/com/polar/app/ui/components/InspectorDialog.kt \
  main/java/com/polar/app/ui/viewmodel/StudioViewModel.kt \
  main/java/com/polar/app/ui/theme/Theme.kt \
  test/java/com/polar/app/viewmodel/StudioViewModelTest.kt
```

`main/java/com/polar/app/MainActivity.kt` (temporal hasta la Task 10):

```kotlin
package com.polar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Polar") }
    }
}
```

- [ ] **Step 6: Reemplazar `model/Models.kt`**

```kotlin
package com.polar.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToInt

/** Ids compatibles con `UUID` de Swift. */
fun newId(): String = UUID.randomUUID().toString().uppercase()

/** Fechas como las guarda la Mac: segundos desde 2001-01-01 00:00 UTC. */
object SwiftDate {
    private const val REFERENCE_EPOCH_MS = 978_307_200_000L
    fun fromEpochMs(ms: Long): Double = (ms - REFERENCE_EPOCH_MS) / 1000.0
    fun toEpochMs(value: Double): Long = Math.round(value * 1000.0) + REFERENCE_EPOCH_MS
    fun now(): Double = fromEpochMs(System.currentTimeMillis())
}

@Serializable
enum class PaperSize(val displayName: String) {
    @SerialName("letter") LETTER("Carta"),
    @SerialName("oficio") OFICIO("Oficio"),
    @SerialName("legal") LEGAL("Legal"),
    @SerialName("a4") A4("A4"),
    @SerialName("a3") A3("A3"),
    @SerialName("photo4x6") PHOTO4X6("4 × 6"),
    @SerialName("photo5x7") PHOTO5X7("5 × 7"),
    @SerialName("custom") CUSTOM("Personalizado");
}

@Serializable
enum class PaperOrientation(val displayName: String) {
    @SerialName("portrait") PORTRAIT("Vertical"),
    @SerialName("landscape") LANDSCAPE("Horizontal");
}

@Serializable
enum class CardFormat(val displayName: String) {
    @SerialName("original") ORIGINAL("Del diseño"),
    @SerialName("portrait") PORTRAIT("Vertical"),
    @SerialName("landscape") LANDSCAPE("Horizontal"),
    @SerialName("square") SQUARE("Cuadrado"),
    @SerialName("fill") FILL("Llenar espacio");
}

@Serializable
enum class CutStyle(val displayName: String) {
    @SerialName("corners") CORNERS("Marcas en esquinas"),
    @SerialName("lines") LINES("Líneas completas");
}

@Serializable
enum class TextRole(val key: String, val displayName: String) {
    @SerialName("title") TITLE("title", "Título"),
    @SerialName("subtitle") SUBTITLE("subtitle", "Subtítulo"),
    @SerialName("caption") CAPTION("caption", "Pie de foto"),
    @SerialName("song") SONG("song", "Canción"),
    @SerialName("artist") ARTIST("artist", "Artista"),
    @SerialName("date") DATE("date", "Fecha");

    /** La Mac sólo acepta estos roles dentro de `settings.textStyles`. */
    val isMacRole: Boolean get() = this != DATE

    companion object {
        fun fromKey(key: String): TextRole? = entries.firstOrNull { it.key == key }
    }
}

@Serializable
enum class TextAlignment(val displayName: String) {
    @SerialName("automatic") AUTOMATIC("Del diseño"),
    @SerialName("left") LEFT("Izquierda"),
    @SerialName("center") CENTER("Centro"),
    @SerialName("right") RIGHT("Derecha");
}

@Serializable
enum class DateSource(val displayName: String) {
    @SerialName("none") NONE("Sin fecha"),
    @SerialName("photo") PHOTO("De la foto"),
    @SerialName("chosen") CHOSEN("Elegir fecha");
}

@Serializable
enum class DateStyle {
    @SerialName("dayMonthYear") DAY_MONTH_YEAR,
    @SerialName("numeric") NUMERIC,
    @SerialName("monthYear") MONTH_YEAR;
}

@Serializable
data class TextAppearance(
    val fontName: String = ".System",
    val size: Double = 0.0,
    val hex: String = "",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val alignment: TextAlignment = TextAlignment.AUTOMATIC,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val visible: Boolean = true
)

@Serializable
enum class TemplateStyle(
    val displayName: String,
    val defaultGrid: Pair<Int, Int>,
    val photosPerCard: Int,
    val defaultAspect: Double
) {
    @SerialName("polaroid") POLAROID("Polaroid", 3 to 3, 1, 0.70),
    @SerialName("mini") MINI("Instantánea mini", 3 to 3, 1, 0.667),
    @SerialName("spotify") SPOTIFY("Foto + canción", 3 to 3, 1, 0.75),
    @SerialName("playerRed") PLAYER_RED("Reproductor rojo", 3 to 3, 1, 0.66),
    @SerialName("playerGray") PLAYER_GRAY("Reproductor horizontal", 2 to 5, 1, 1.9),
    @SerialName("ticket") TICKET("Boleto vintage", 1 to 4, 1, 2.27),
    @SerialName("filmVertical") FILM_VERTICAL("Película vertical", 2 to 1, 5, 0.292),
    @SerialName("filmHorizontal") FILM_HORIZONTAL("Película horizontal", 1 to 6, 5, 5.3),
    @SerialName("calendar") CALENDAR("Calendario", 3 to 4, 1, 0.683),
    @SerialName("instagram") INSTAGRAM("Instagram", 3 to 4, 1, 0.705),
    @SerialName("custom") CUSTOM("Mi diseño", 2 to 3, 1, 0.80),
    @SerialName("borderless") BORDERLESS("Sin bordes", 2 to 3, 1, 0.75),
    @SerialName("square") SQUARE("Cuadrada", 3 to 3, 1, 1.0),
    @SerialName("postcard") POSTCARD("Postal", 2 to 3, 1, 1.5),
    @SerialName("botanical") BOTANICAL("Botánico", 2 to 3, 1, 0.75),
    @SerialName("celebration") CELEBRATION("Celebración", 3 to 3, 1, 0.70),
    @SerialName("pets") PETS("Mi mascota", 2 to 3, 1, 0.80),
    @SerialName("heart") HEART("Corazón", 2 to 3, 1, 0.75),
    @SerialName("editorial") EDITORIAL("Editorial", 2 to 3, 1, 0.75),
    @SerialName("imported") IMPORTED("Plantilla importada", 1 to 1, 1, 0.75);
}

@Serializable
data class TemplateRegion(
    val id: String = newId(),
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val isTransparent: Boolean = false
) {
    fun clamped(): TemplateRegion {
        val w = width.coerceIn(0.02, 1.0)
        val h = height.coerceIn(0.02, 1.0)
        return copy(width = w, height = h, x = x.coerceIn(0.0, 1.0 - w), y = y.coerceIn(0.0, 1.0 - h))
    }
}

@Serializable
data class ImportedTemplate(
    val path: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val regions: List<TemplateRegion> = emptyList()
)

@Serializable
data class PhotoAsset(
    val id: String = newId(),
    val path: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val takenAtEpochMs: Long? = null
) {
    val name: String get() = path.substringAfterLast('/').substringBeforeLast('.')
}

@Serializable
data class PhotoPlacement(
    val assetID: String,
    val zoom: Double = 1.0,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val quarterTurns: Int = 0
)

data class SizeF(val width: Double, val height: Double)

@Serializable
data class PrintSettings(
    val style: TemplateStyle = TemplateStyle.POLAROID,
    val columns: Int = 3,
    val rows: Int = 3,
    val margin: Double = 24.0,
    val gap: Double = 12.0,
    val title: String = "Nuestros momentos",
    val subtitle: String = "Tú y yo",
    val caption: String = "Una historia para guardar",
    val song: String = "Nuestra canción",
    val artist: String = "Artista",
    val songURL: String = "",
    val accentHex: String = "92394A",
    val cutGuides: Boolean = true,
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val specialDate: Double = SwiftDate.now(),
    val highlightDate: Boolean = true,
    val roundedPhotos: Boolean = false,
    val paperSize: PaperSize = PaperSize.LETTER,
    val orientation: PaperOrientation = PaperOrientation.PORTRAIT,
    val customWidthMM: Double = 215.9,
    val customHeightMM: Double = 279.4,
    val cardFormat: CardFormat = CardFormat.ORIGINAL,
    val importedTemplate: ImportedTemplate? = null,
    val textStyles: Map<String, TextAppearance> = emptyMap(),
    val drawBorders: Boolean = false,
    val cutStyle: CutStyle = CutStyle.CORNERS,
    // Campos sólo de Android: la Mac los ignora.
    val extraTextStyles: Map<String, TextAppearance> = emptyMap(),
    val dateSource: DateSource = DateSource.NONE,
    val chosenDate: Double? = null,
    val dateStyle: DateStyle = DateStyle.DAY_MONTH_YEAR
) {
    val capacity: Int
        get() {
            if (style == TemplateStyle.IMPORTED) return (importedTemplate?.regions?.size ?: 1).coerceAtLeast(1)
            if (columns !in 1..4 || rows !in 1..6) return 1
            return columns * rows * style.photosPerCard
        }

    val paperSizePoints: SizeF
        get() {
            val base = when (paperSize) {
                PaperSize.LETTER -> SizeF(612.0, 792.0)
                PaperSize.OFICIO -> SizeF(216.0 * 72.0 / 25.4, 340.0 * 72.0 / 25.4)
                PaperSize.LEGAL -> SizeF(612.0, 1008.0)
                PaperSize.A4 -> SizeF(210.0 * 72.0 / 25.4, 297.0 * 72.0 / 25.4)
                PaperSize.A3 -> SizeF(297.0 * 72.0 / 25.4, 420.0 * 72.0 / 25.4)
                PaperSize.PHOTO4X6 -> SizeF(288.0, 432.0)
                PaperSize.PHOTO5X7 -> SizeF(360.0, 504.0)
                PaperSize.CUSTOM -> SizeF(customWidthMM * 72.0 / 25.4, customHeightMM * 72.0 / 25.4)
            }
            return if (orientation == PaperOrientation.PORTRAIT) base else SizeF(base.height, base.width)
        }

    val paperDescription: String
        get() {
            val size = paperSizePoints
            val wMM = (size.width * 25.4 / 72.0).roundToInt()
            val hMM = (size.height * 25.4 / 72.0).roundToInt()
            return "${paperSize.displayName} · $wMM × $hMM mm · ${orientation.displayName}"
        }

    val cardAspect: Double?
        get() = when (cardFormat) {
            CardFormat.ORIGINAL -> style.defaultAspect
            CardFormat.PORTRAIT -> 0.70
            CardFormat.LANDSCAPE -> 1.40
            CardFormat.SQUARE -> 1.0
            CardFormat.FILL -> null
        }

    fun text(role: TextRole): String = when (role) {
        TextRole.TITLE -> title
        TextRole.SUBTITLE -> subtitle
        TextRole.CAPTION -> caption
        TextRole.SONG -> song
        TextRole.ARTIST -> artist
        TextRole.DATE -> ""
    }

    fun withText(role: TextRole, value: String): PrintSettings = when (role) {
        TextRole.TITLE -> copy(title = value)
        TextRole.SUBTITLE -> copy(subtitle = value)
        TextRole.CAPTION -> copy(caption = value)
        TextRole.SONG -> copy(song = value)
        TextRole.ARTIST -> copy(artist = value)
        TextRole.DATE -> this
    }

    fun textStyle(role: TextRole): TextAppearance =
        (if (role.isMacRole) textStyles[role.key] else extraTextStyles[role.key]) ?: TextAppearance()

    fun withTextStyle(role: TextRole, appearance: TextAppearance): PrintSettings =
        if (role.isMacRole) copy(textStyles = textStyles + (role.key to appearance))
        else copy(extraTextStyles = extraTextStyles + (role.key to appearance))

    fun withoutTextStyle(role: TextRole): PrintSettings =
        if (role.isMacRole) copy(textStyles = textStyles - role.key)
        else copy(extraTextStyles = extraTextStyles - role.key)
}

/** Lo que una tarjeta cambia respecto al texto general. Claves = `TextRole.key`. */
@Serializable
data class CardOverride(
    val texts: Map<String, String> = emptyMap(),
    val styles: Map<String, TextAppearance> = emptyMap(),
    val dateSource: DateSource? = null,
    val chosenDate: Double? = null
) {
    val isEmpty: Boolean get() = texts.isEmpty() && styles.isEmpty() && dateSource == null && chosenDate == null
}

class PolarException(message: String) : Exception(message)

@Serializable
data class PolarProject(
    val version: Int = 1,
    val settings: PrintSettings = PrintSettings(),
    val photos: List<PhotoAsset> = emptyList(),
    val placements: List<PhotoPlacement?> = emptyList(),
    // Campos sólo de Android: la Mac los ignora.
    val name: String = "",
    val updatedAtEpochMs: Long = 0,
    val cardOverrides: Map<String, CardOverride> = emptyMap()
) {
    val pageCount: Int
        get() {
            val cap = settings.capacity.coerceAtLeast(1)
            return ((placements.size + cap - 1) / cap).coerceAtLeast(1)
        }

    val placedCount: Int get() = placements.count { it != null }

    /** Tarjetas por hoja (en película, cada tarjeta reúne 5 fotos). */
    val cardsPerPage: Int
        get() = if (settings.style == TemplateStyle.IMPORTED) settings.capacity
        else (settings.capacity / settings.style.photosPerCard).coerceAtLeast(1)

    fun cardOfSlot(slot: Int): Int = slot / settings.style.photosPerCard
    fun firstSlotOfCard(card: Int): Int = card * settings.style.photosPerCard
    fun override(card: Int): CardOverride? = cardOverrides[card.toString()]

    fun asset(forPlacement: PhotoPlacement?): PhotoAsset? {
        if (forPlacement == null) return null
        return photos.firstOrNull { it.id == forPlacement.assetID }
    }

    fun normalized(): PolarProject {
        val total = pageCount * settings.capacity.coerceAtLeast(1)
        return if (placements.size >= total) this else copy(placements = placements + List(total - placements.size) { null })
    }

    /** Cambia de diseño como la Mac: cuadrícula por defecto y sin huecos vacíos al final. */
    fun withStyle(style: TemplateStyle): PolarProject {
        val (cols, rows) = style.defaultGrid
        val trimmed = placements.dropLastWhile { it == null }
        return copy(settings = settings.copy(style = style, columns = cols, rows = rows), placements = trimmed).normalized()
    }

    fun validated() {
        if (version != 1) throw PolarException("La versión del archivo no es compatible.")
        if (settings.columns !in 1..4 || settings.rows !in 1..6) throw PolarException("El tamaño o la distribución no son válidos.")
        if (!settings.margin.isFinite() || settings.margin !in 0.0..60.0) throw PolarException("El margen no es válido.")
        if (!settings.gap.isFinite() || settings.gap !in 0.0..30.0) throw PolarException("La separación no es válida.")
        if (!settings.customWidthMM.isFinite() || settings.customWidthMM !in 80.0..600.0) throw PolarException("El ancho personalizado no es válido.")
        if (!settings.customHeightMM.isFinite() || settings.customHeightMM !in 80.0..600.0) throw PolarException("El alto personalizado no es válido.")
        if (settings.calendarYear !in 1900..2100) throw PolarException("El año del calendario no es válido.")
        if (placements.size > 2000 + settings.capacity - 1 || placedCount > 2000 || photos.size > 2000) {
            throw PolarException("Este diseño tiene más de 2000 fotos.")
        }
        val paper = settings.paperSizePoints
        if (paper.width <= 2 * settings.margin + (settings.columns - 1) * settings.gap ||
            paper.height <= 2 * settings.margin + (settings.rows - 1) * settings.gap
        ) throw PolarException("El papel es demasiado pequeño para estos márgenes y espacios.")
        val hex = Regex("^[0-9A-Fa-f]{6}$")
        if (!hex.matches(settings.accentHex)) throw PolarException("El color no es válido.")

        fun checkAppearance(a: TextAppearance) {
            if (a.fontName.length > 128) throw PolarException("El nombre de una fuente es demasiado largo.")
            if (a.hex.isNotEmpty() && !hex.matches(a.hex)) throw PolarException("El color de un texto no es válido.")
            if (!a.size.isFinite() || (a.size != 0.0 && a.size !in 6.0..96.0)) throw PolarException("El tamaño de un texto no es válido.")
            if (!a.offsetX.isFinite() || a.offsetX !in -60.0..60.0 || !a.offsetY.isFinite() || a.offsetY !in -60.0..60.0) {
                throw PolarException("La posición de un texto no es válida.")
            }
        }
        for ((role, a) in settings.textStyles) {
            if (TextRole.fromKey(role)?.isMacRole != true) throw PolarException("Hay un texto desconocido: $role")
            checkAppearance(a)
        }
        for ((role, a) in settings.extraTextStyles) {
            if (TextRole.fromKey(role) == null) throw PolarException("Hay un texto desconocido: $role")
            checkAppearance(a)
        }
        for ((card, o) in cardOverrides) {
            if (card.toIntOrNull()?.let { it >= 0 } != true) throw PolarException("Una tarjeta no es válida.")
            for ((role, value) in o.texts) {
                if (TextRole.fromKey(role) == null) throw PolarException("Hay un texto desconocido: $role")
                if (value.length > 500) throw PolarException("Un texto es demasiado largo.")
            }
            for ((role, a) in o.styles) {
                if (TextRole.fromKey(role) == null) throw PolarException("Hay un texto desconocido: $role")
                checkAppearance(a)
            }
        }
        if (settings.style == TemplateStyle.IMPORTED && settings.importedTemplate == null) {
            throw PolarException("Agrega una plantilla antes de usar este diseño.")
        }
        settings.importedTemplate?.let { t ->
            if (t.path.isEmpty() || t.pixelWidth !in 1..30000 || t.pixelHeight !in 1..30000 ||
                t.pixelWidth.toDouble() * t.pixelHeight.toDouble() > 150_000_000
            ) throw PolarException("La plantilla importada no es válida.")
            if (t.regions.size !in 1..64 || t.regions.map { it.id }.toSet().size != t.regions.size) {
                throw PolarException("La plantilla importada no es válida.")
            }
            for (r in t.regions) {
                if (!r.x.isFinite() || !r.y.isFinite() || !r.width.isFinite() || !r.height.isFinite() ||
                    r.x < 0.0 || r.y < 0.0 || r.width <= 0.0 || r.height <= 0.0 ||
                    r.x + r.width > 1.000001 || r.y + r.height > 1.000001
                ) throw PolarException("Un espacio de la plantilla queda fuera de la imagen.")
            }
        }
        val ids = photos.map { it.id }.toSet()
        if (ids.size != photos.size || photos.any { it.path.isEmpty() || it.pixelWidth <= 0 || it.pixelHeight <= 0 }) {
            throw PolarException("La lista de fotos no es válida.")
        }
        for (slot in placements.filterNotNull()) {
            if (slot.assetID !in ids) throw PolarException("Falta una foto que usa el diseño.")
            if (!slot.zoom.isFinite() || slot.zoom !in 1.0..4.0 ||
                !slot.offsetX.isFinite() || slot.offsetX !in -1.0..1.0 ||
                !slot.offsetY.isFinite() || slot.offsetY !in -1.0..1.0 || slot.quarterTurns !in 0..3
            ) throw PolarException("El encuadre de una foto no es válido.")
        }
    }

    fun effectiveDPI(slot: Int, rectWidthPoints: Double, rectHeightPoints: Double): Double? {
        val p = placements.getOrNull(slot) ?: return null
        val photo = asset(p) ?: return null
        val w = if (p.quarterTurns % 2 == 0) photo.pixelWidth else photo.pixelHeight
        val h = if (p.quarterTurns % 2 == 0) photo.pixelHeight else photo.pixelWidth
        return minOf(w / rectWidthPoints, h / rectHeightPoints) * 72.0 / p.zoom
    }
}
```

- [ ] **Step 7: Crear `model/StyleInfo.kt`**

```kotlin
package com.polar.app.model

import com.polar.app.model.TemplateStyle.*

enum class DesignCategory(val displayName: String) {
    CLASSIC("Clásicos"), MUSIC("Música"), FILM("Cine"), DATES("Fechas"), OCCASIONS("Ocasiones"), FREE("Libre")
}

val TemplateStyle.category: DesignCategory
    get() = when (this) {
        POLAROID, MINI, SQUARE, BORDERLESS, INSTAGRAM -> DesignCategory.CLASSIC
        SPOTIFY, PLAYER_RED, PLAYER_GRAY -> DesignCategory.MUSIC
        FILM_VERTICAL, FILM_HORIZONTAL -> DesignCategory.FILM
        CALENDAR -> DesignCategory.DATES
        TICKET, CELEBRATION, HEART, PETS, BOTANICAL -> DesignCategory.OCCASIONS
        CUSTOM, POSTCARD, EDITORIAL, IMPORTED -> DesignCategory.FREE
    }

/** Fecha disponible salvo donde no hay lugar para ella o ya trae sus propias fechas. */
val TemplateStyle.supportsDate: Boolean
    get() = this !in setOf(FILM_VERTICAL, FILM_HORIZONTAL, CALENDAR, IMPORTED)

/** Roles editables, en el mismo orden que la Mac, más la fecha. */
val TemplateStyle.textRoles: List<TextRole>
    get() {
        val base = when (this) {
            FILM_VERTICAL, FILM_HORIZONTAL, CALENDAR, BORDERLESS, IMPORTED -> emptyList()
            SPOTIFY, PLAYER_RED, PLAYER_GRAY -> listOf(TextRole.SONG, TextRole.ARTIST)
            INSTAGRAM -> listOf(TextRole.TITLE, TextRole.CAPTION)
            POSTCARD, EDITORIAL, CELEBRATION -> listOf(TextRole.TITLE, TextRole.SUBTITLE, TextRole.CAPTION)
            else -> listOf(TextRole.TITLE, TextRole.SUBTITLE)
        }
        return if (supportsDate) base + TextRole.DATE else base
    }

val TemplateStyle.description: String
    get() = when (this) {
        POLAROID -> "Marco blanco con pie de foto"
        MINI -> "Más alta, como una instantánea mini"
        SPOTIFY -> "Reproductor y QR a tu canción"
        PLAYER_RED -> "Reproductor a color"
        PLAYER_GRAY -> "Reproductor ancho"
        TICKET -> "Para viajes y conciertos"
        FILM_VERTICAL -> "Tira de cinco fotos"
        FILM_HORIZONTAL -> "Tira horizontal de cinco fotos"
        CALENDAR -> "El mes con su fecha especial"
        INSTAGRAM -> "Como una publicación"
        CUSTOM -> "Marco de color"
        BORDERLESS -> "Sólo la foto, de orilla a orilla"
        SQUARE -> "Polaroid cuadrada"
        POSTCARD -> "Foto grande y un mensaje"
        BOTANICAL -> "Papel crema y hojas"
        CELEBRATION -> "Para cumpleaños y fiestas"
        PETS -> "Para tu mejor compañía"
        HEART -> "Tu foto recortada en corazón"
        EDITORIAL -> "Como una revista"
        IMPORTED -> "Tu propio molde"
    }
```

- [ ] **Step 8: Crear `model/PolarJson.kt`**

```kotlin
package com.polar.app.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object PolarJson {
    /** Mismo formato que la Mac: todas las claves, nulos omitidos, legible. */
    val format: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
        coerceInputValues = true
    }

    fun encode(project: PolarProject): String = format.encodeToString(PolarProject.serializer(), project)

    fun decode(text: String): PolarProject {
        if (text.length > 5_000_000) throw PolarException("El archivo es demasiado grande.")
        val project = try {
            format.decodeFromString(PolarProject.serializer(), text)
        } catch (e: SerializationException) {
            throw PolarException("El archivo no es un diseño de Polar.")
        } catch (e: IllegalArgumentException) {
            throw PolarException("El archivo no es un diseño de Polar.")
        }
        project.validated()
        return project.normalized()
    }
}
```

> Nota: `roundTripStaysMacReadable` compara `decode(encode(p))` con `p` ya normalizado, así que la igualdad se cumple.

- [ ] **Step 9: Correr las pruebas del modelo**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.model.*' --console=plain`
Expected: PASS (todas). Después arregla las referencias viejas en el resto del código:
- `PolarRenderer.drawCalendar` usa `settings.specialDateEpochMs` (milisegundos) → cámbialo por `SwiftDate.toEpochMs(settings.specialDate)`. Mac guarda **segundos desde 2001**; no lo sustituyas tal cual.
- Cualquier `clamp()` sobre `TemplateRegion` → `clamped()` (devuelve copia).

- [ ] **Step 10: Correr todas las pruebas y compilar**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`. Si `PolarRenderer`/`PolarExporter` usaban algún campo eliminado, ajusta únicamente esa referencia.

- [ ] **Step 11: Commit**

```bash
git add -A PolarAndroid
git commit -m "feat(model): modelo inmutable y JSON compatible con la Mac"
```

---
### Task 3: Fechas y `TextResolver` (texto general contra texto propio)

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/core/text/DateText.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/core/text/TextResolver.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/core/text/DateTextTest.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/core/text/TextResolverTest.kt`

**Interfaces:**
- Consumes (Task 2): `PolarProject.override(card)`, `firstSlotOfCard`, `asset`, `PrintSettings.text/textStyle`, `SwiftDate`, `DateSource`, `DateStyle`, `TextRole`.
- Produces:
  - `object DateText { fun format(epochMs: Long, style: DateStyle, zone: TimeZone = TimeZone.getDefault()): String }`.
  - `object TextResolver`:
    - `text(project, card, role, zone = TimeZone.getDefault()): String`
    - `hasOwnText(project, card, role): Boolean`
    - `appearance(project, card, role): TextAppearance`
    - `hasOwnAppearance(project, card, role): Boolean`
    - `dateSource(project, card): DateSource`
    - `ownTextCount(project, role): Int`

- [ ] **Step 1: Pruebas que fallan**

`DateTextTest.kt`:

```kotlin
package com.polar.app.core.text

import com.polar.app.model.DateStyle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateTextTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun ms(y: Int, m: Int, d: Int) = Calendar.getInstance(utc).apply { clear(); set(y, m - 1, d, 12, 0) }.timeInMillis

    @Test
    fun formatsTheThreeStyles() {
        val v = ms(2026, 2, 14)
        assertEquals("14 feb 2026", DateText.format(v, DateStyle.DAY_MONTH_YEAR, utc))
        assertEquals("14.02.26", DateText.format(v, DateStyle.NUMERIC, utc))
        assertEquals("febrero 2026", DateText.format(v, DateStyle.MONTH_YEAR, utc))
    }

    @Test
    fun singleDigitDaysAndDecember() {
        val v = ms(2027, 12, 3)
        assertEquals("3 dic 2027", DateText.format(v, DateStyle.DAY_MONTH_YEAR, utc))
        assertEquals("03.12.27", DateText.format(v, DateStyle.NUMERIC, utc))
    }
}
```

`TextResolverTest.kt`:

```kotlin
package com.polar.app.core.text

import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class TextResolverTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val photo = PhotoAsset(path = "a.jpg", pixelWidth = 10, pixelHeight = 10, takenAtEpochMs = 1_771_070_400_000) // 14 feb 2026 12:00 UTC
    private val base = PolarProject(
        settings = PrintSettings(title = "Lu y Max"),
        photos = listOf(photo),
        placements = listOf(PhotoPlacement(photo.id), PhotoPlacement(photo.id), null)
    ).normalized()

    @Test
    fun generalTextWhenNoOverride() {
        assertEquals("Lu y Max", TextResolver.text(base, 0, TextRole.TITLE))
        assertFalse(TextResolver.hasOwnText(base, 0, TextRole.TITLE))
    }

    @Test
    fun ownTextOnlyForThatCard() {
        val p = base.copy(cardOverrides = mapOf("1" to CardOverride(texts = mapOf("title" to "El brindis"))))
        assertEquals("Lu y Max", TextResolver.text(p, 0, TextRole.TITLE))
        assertEquals("El brindis", TextResolver.text(p, 1, TextRole.TITLE))
        assertTrue(TextResolver.hasOwnText(p, 1, TextRole.TITLE))
        assertEquals(1, TextResolver.ownTextCount(p, TextRole.TITLE))
    }

    @Test
    fun emptyOwnTextIsStillOwn() {
        val p = base.copy(cardOverrides = mapOf("1" to CardOverride(texts = mapOf("title" to ""))))
        assertEquals("", TextResolver.text(p, 1, TextRole.TITLE))
        assertTrue(TextResolver.hasOwnText(p, 1, TextRole.TITLE))
    }

    @Test
    fun appearanceFallsBackToGeneral() {
        val general = TextAppearance(hex = "112233")
        val p = base.copy(
            settings = base.settings.withTextStyle(TextRole.TITLE, general),
            cardOverrides = mapOf("1" to CardOverride(styles = mapOf("title" to general.copy(hex = "AABBCC"))))
        )
        assertEquals("112233", TextResolver.appearance(p, 0, TextRole.TITLE).hex)
        assertEquals("AABBCC", TextResolver.appearance(p, 1, TextRole.TITLE).hex)
        assertTrue(TextResolver.hasOwnAppearance(p, 1, TextRole.TITLE))
    }

    @Test
    fun dateFromPhotoChosenOrNone() {
        val fromPhoto = base.copy(settings = base.settings.copy(dateSource = DateSource.PHOTO))
        assertEquals("14 feb 2026", TextResolver.text(fromPhoto, 0, TextRole.DATE, utc))
        assertEquals("", TextResolver.text(fromPhoto, 2, TextRole.DATE, utc)) // tarjeta vacía
        assertEquals("", TextResolver.text(base, 0, TextRole.DATE, utc))      // NONE por defecto

        val chosen = SwiftDate.fromEpochMs(1_791_806_400_000) // 12 oct 2026 12:00 UTC
        val perCard = base.copy(cardOverrides = mapOf("1" to CardOverride(dateSource = DateSource.CHOSEN, chosenDate = chosen)))
        assertEquals("12 oct 2026", TextResolver.text(perCard, 1, TextRole.DATE, utc))
        assertEquals(DateSource.NONE, TextResolver.dateSource(perCard, 0))
    }

    @Test
    fun photoWithoutExifDateShowsNothing() {
        val noDate = photo.copy(id = newId(), takenAtEpochMs = null)
        val p = base.copy(photos = listOf(noDate), placements = listOf(PhotoPlacement(noDate.id)), settings = base.settings.copy(dateSource = DateSource.PHOTO))
        assertEquals("", TextResolver.text(p, 0, TextRole.DATE, utc))
    }
}
```

- [ ] **Step 2: Correr y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.core.text.*' --console=plain`
Expected: FAIL (no existen `DateText`/`TextResolver`).

- [ ] **Step 3: Implementar `DateText.kt`**

```kotlin
package com.polar.app.core.text

import com.polar.app.model.DateStyle
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Meses escritos a mano: los nombres de `Locale("es")` cambian entre versiones de Android ("feb." vs "feb"). */
object DateText {
    private val SHORT = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val LONG = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

    fun format(epochMs: Long, style: DateStyle, zone: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(zone).apply { timeInMillis = epochMs }
        val day = c.get(Calendar.DAY_OF_MONTH)
        val month = c.get(Calendar.MONTH)
        val year = c.get(Calendar.YEAR)
        return when (style) {
            DateStyle.DAY_MONTH_YEAR -> "$day ${SHORT[month]} $year"
            DateStyle.NUMERIC -> String.format(Locale.ROOT, "%02d.%02d.%02d", day, month + 1, year % 100)
            DateStyle.MONTH_YEAR -> "${LONG[month]} $year"
        }
    }
}
```

- [ ] **Step 4: Implementar `TextResolver.kt`**

```kotlin
package com.polar.app.core.text

import com.polar.app.model.*
import java.util.TimeZone

/** Texto y estilo efectivos de cada tarjeta: el propio si existe, si no el general. */
object TextResolver {

    fun text(project: PolarProject, card: Int, role: TextRole, zone: TimeZone = TimeZone.getDefault()): String {
        if (role == TextRole.DATE) return dateText(project, card, zone)
        return project.override(card)?.texts?.get(role.key) ?: project.settings.text(role)
    }

    fun hasOwnText(project: PolarProject, card: Int, role: TextRole): Boolean =
        project.override(card)?.texts?.containsKey(role.key) == true

    fun appearance(project: PolarProject, card: Int, role: TextRole): TextAppearance =
        project.override(card)?.styles?.get(role.key) ?: project.settings.textStyle(role)

    fun hasOwnAppearance(project: PolarProject, card: Int, role: TextRole): Boolean =
        project.override(card)?.styles?.containsKey(role.key) == true

    fun dateSource(project: PolarProject, card: Int): DateSource =
        project.override(card)?.dateSource ?: project.settings.dateSource

    fun ownTextCount(project: PolarProject, role: TextRole): Int =
        project.cardOverrides.values.count { it.texts.containsKey(role.key) }

    private fun dateText(project: PolarProject, card: Int, zone: TimeZone): String {
        val epochMs = when (dateSource(project, card)) {
            DateSource.NONE -> return ""
            DateSource.PHOTO -> {
                val placement = project.placements.getOrNull(project.firstSlotOfCard(card)) ?: return ""
                project.asset(placement)?.takenAtEpochMs ?: return ""
            }
            DateSource.CHOSEN -> {
                val swift = project.override(card)?.chosenDate ?: project.settings.chosenDate ?: return ""
                SwiftDate.toEpochMs(swift)
            }
        }
        return DateText.format(epochMs, project.settings.dateStyle, zone)
    }
}
```

- [ ] **Step 5: Correr las pruebas**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.core.text.*' --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add PolarAndroid/app/src/main/java/com/polar/app/core/text PolarAndroid/app/src/test/java/com/polar/app/core/text
git commit -m "feat(text): texto propio por tarjeta y fechas en español"
```

---

### Task 4: `ProjectEdits` (todas las ediciones como funciones puras)

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/core/edit/MoodPreset.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/core/edit/ProjectEdits.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/core/edit/ProjectEditsTest.kt`

**Interfaces:**
- Consumes (Task 2): modelo completo.
- Produces:
  - `enum class MoodPreset(displayName, hex, fontName, title, subtitle, caption)`. Valores: `COUPLE, FRIENDS, FAMILY, PETS, TRAVEL, CELEBRATION, MINIMAL`. Los `fontName` son ids de `FontCatalog` (Task 6): `"Gelasio"`, `"Josefin Sans"`, `"Libre Baskerville"`, `"Patrick Hand"`, `"Dancing Script"`, `".System"`.
  - `object ProjectEdits`, cada función devuelve un `PolarProject` nuevo:
    - Texto:
      - `setText(p, role, value, card: Int? = null)`
      - `clearOwnText(p, card, role)`
      - `applyTextToAll(p, role)`
      - `editAppearance(p, role, card: Int?, change: (TextAppearance) -> TextAppearance)`
      - `clearOwnAppearance(p, card, role)`
      - `applyAppearanceToAll(p, role)`
      - `resetAppearance(p, role)`
    - Fecha:
      - `setDateSource(p, source, card: Int? = null)`
      - `setChosenDate(p, epochMs: Long, card: Int? = null)`
      - `setDateStyle(p, style)`
    - Estilos rápidos:
      - `applyMood(p, mood)`
      - `applySuggestedPhrases(p, mood)`
    - Diseño y distribución:
      - `selectStyle(p, style)`
      - `setGrid(p, columns, rows)`
      - `applyLayoutPreset(p, count)`
      - `updateSettings(p, change: (PrintSettings) -> PrintSettings)`
      - `setCustomPaper(p, widthMM, heightMM)`
    - Fotos:
      - `addPhotos(p, assets: List<PhotoAsset>, fillFrom: Int?)`
      - `fillAll(p)`
      - `assign(p, slot, assetId)`
      - `clearSlot(p, slot)`
      - `editPlacement(p, slot, change: (PhotoPlacement) -> PhotoPlacement)`
    - Hojas:
      - `clearPage(p, page)`
      - `addPage(p)`
      - `removePage(p, page)`
    - Plantilla:
      - `editTemplateRegion(p, index, change)`
      - `addTemplateRegion(p)`
      - `removeTemplateRegion(p, index)`
  - `ProjectEdits.LAYOUT_PRESETS: List<Int> = listOf(1, 2, 4, 6, 8, 9, 12, 16)`.

- [ ] **Step 1: Pruebas que fallan**

`ProjectEditsTest.kt`:

```kotlin
package com.polar.app.core.edit

import com.polar.app.core.text.TextResolver
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test

class ProjectEditsTest {
    private fun photos(n: Int) = List(n) { PhotoAsset(path = "p$it.jpg", pixelWidth = 100, pixelHeight = 100) }
    private fun filled(n: Int): PolarProject {
        val ps = photos(n)
        return PolarProject(photos = ps, placements = ps.map { PhotoPlacement(it.id) }).normalized()
    }

    @Test
    fun setTextGeneralAndOwn() {
        var p = filled(3)
        p = ProjectEdits.setText(p, TextRole.TITLE, "Lu y Max")
        p = ProjectEdits.setText(p, TextRole.TITLE, "El brindis", card = 1)
        assertEquals("Lu y Max", TextResolver.text(p, 0, TextRole.TITLE))
        assertEquals("El brindis", TextResolver.text(p, 1, TextRole.TITLE))
        p = ProjectEdits.clearOwnText(p, 1, TextRole.TITLE)
        assertEquals("Lu y Max", TextResolver.text(p, 1, TextRole.TITLE))
        assertTrue("un override vacío se elimina", p.cardOverrides.isEmpty())
    }

    @Test
    fun applyTextToAllRemovesOnlyThatRole() {
        var p = filled(3)
        p = ProjectEdits.setText(p, TextRole.TITLE, "A", card = 0)
        p = ProjectEdits.setText(p, TextRole.SUBTITLE, "B", card = 0)
        p = ProjectEdits.setText(p, TextRole.TITLE, "C", card = 2)
        p = ProjectEdits.applyTextToAll(p, TextRole.TITLE)
        assertEquals(0, TextResolver.ownTextCount(p, TextRole.TITLE))
        assertEquals("B", TextResolver.text(p, 0, TextRole.SUBTITLE))
    }

    @Test
    fun ownAppearanceStartsFromGeneral() {
        var p = filled(2)
        p = ProjectEdits.editAppearance(p, TextRole.TITLE, null) { it.copy(fontName = "Caveat", bold = true) }
        p = ProjectEdits.editAppearance(p, TextRole.TITLE, 1) { it.copy(hex = "C34048") }
        val own = TextResolver.appearance(p, 1, TextRole.TITLE)
        assertEquals("Caveat", own.fontName)
        assertTrue(own.bold)
        assertEquals("C34048", own.hex)
        assertEquals("", TextResolver.appearance(p, 0, TextRole.TITLE).hex)
    }

    @Test
    fun dateSourceGeneralAndPerCard() {
        var p = filled(2)
        p = ProjectEdits.setDateSource(p, DateSource.PHOTO)
        p = ProjectEdits.setChosenDate(p, 1_791_806_400_000, card = 1)
        assertEquals(DateSource.PHOTO, TextResolver.dateSource(p, 0))
        assertEquals(DateSource.CHOSEN, TextResolver.dateSource(p, 1))
    }

    @Test
    fun moodChangesFontAndColorButKeepsPhrases() {
        var p = ProjectEdits.setText(filled(1), TextRole.TITLE, "Mi frase")
        p = ProjectEdits.applyMood(p, MoodPreset.CELEBRATION)
        assertEquals("C34048", p.settings.accentHex)
        assertEquals("Dancing Script", p.settings.textStyle(TextRole.TITLE).fontName)
        assertEquals("Dancing Script", p.settings.textStyle(TextRole.DATE).fontName)
        assertEquals("Mi frase", p.settings.title)
        assertFalse(p.settings.textStyles.containsKey("date"))
    }

    @Test
    fun suggestedPhrasesReplaceGeneralAndOwnTexts() {
        var p = ProjectEdits.setText(filled(2), TextRole.TITLE, "Propio", card = 1)
        p = ProjectEdits.applySuggestedPhrases(p, MoodPreset.FRIENDS)
        assertEquals("Siempre juntos", TextResolver.text(p, 1, TextRole.TITLE))
        assertEquals("Amigos de verdad", p.settings.subtitle)
    }

    @Test
    fun selectStyleRemapsOverridesWhenPhotosPerCardChanges() {
        var p = filled(12)
        p = ProjectEdits.setText(p, TextRole.TITLE, "cero", card = 0)
        p = ProjectEdits.setText(p, TextRole.TITLE, "uno", card = 1)  // fotos 1 → tarjeta 0 de película: choca, gana "cero"
        p = ProjectEdits.setText(p, TextRole.TITLE, "seis", card = 6) // foto 6 → tarjeta 1 de película
        val film = ProjectEdits.selectStyle(p, TemplateStyle.FILM_VERTICAL)
        assertEquals("cero", film.cardOverrides["0"]!!.texts["title"])
        assertEquals("seis", film.cardOverrides["1"]!!.texts["title"])
        assertEquals(2, film.cardOverrides.size)
        val sameShape = ProjectEdits.selectStyle(p, TemplateStyle.MINI)
        assertEquals(p.cardOverrides, sameShape.cardOverrides)
    }

    @Test
    fun setGridKeepsPhotoOrderAndOverrides() {
        val p0 = ProjectEdits.setText(filled(5), TextRole.TITLE, "x", card = 3)
        val p = ProjectEdits.setGrid(p0, 2, 2)
        assertEquals(4, p.settings.capacity)
        assertEquals(p0.placements.take(5), p.placements.take(5))
        assertEquals(8, p.placements.size)
        assertEquals("x", TextResolver.text(p, 3, TextRole.TITLE))
    }

    @Test
    fun layoutPresetsFollowOrientation() {
        val portrait = ProjectEdits.applyLayoutPreset(filled(1), 2)
        assertEquals(1 to 2, portrait.settings.columns to portrait.settings.rows)
        val landscape = ProjectEdits.applyLayoutPreset(
            ProjectEdits.updateSettings(filled(1)) { it.copy(orientation = PaperOrientation.LANDSCAPE) }, 8
        )
        assertEquals(4 to 2, landscape.settings.columns to landscape.settings.rows)
        val one = filled(1)
        assertEquals("un número no soportado no cambia nada", one, ProjectEdits.applyLayoutPreset(one, 5))
    }

    @Test
    fun addPhotosDedupesAndFillsEmptySlots() {
        val p0 = PolarProject().normalized()
        val ps = photos(3)
        val p1 = ProjectEdits.addPhotos(p0, ps, fillFrom = 0)
        assertEquals(3, p1.placedCount)
        val p2 = ProjectEdits.addPhotos(p1, ps + photos(1).map { it.copy(path = "nueva.jpg") }, fillFrom = 0)
        assertEquals(4, p2.photos.size)
        assertEquals(p2.photos.last().id, p2.placements[3]!!.assetID)
        val noFill = ProjectEdits.addPhotos(p0, ps, fillFrom = null)
        assertEquals(0, noFill.placedCount)
    }

    @Test
    fun fillAllUsesEveryPhotoAcrossPages() {
        val ps = photos(20)
        val p = ProjectEdits.fillAll(PolarProject(photos = ps))
        assertEquals(20, p.placedCount)
        assertEquals(3, p.pageCount)
    }

    @Test
    fun editPlacementClampsValues() {
        val p = ProjectEdits.editPlacement(filled(1), 0) { it.copy(zoom = 9.0, offsetX = -3.0, quarterTurns = 5) }
        val pl = p.placements[0]!!
        assertEquals(4.0, pl.zoom, 0.0)
        assertEquals(-1.0, pl.offsetX, 0.0)
        assertEquals(1, pl.quarterTurns)
        val untouched = filled(1)
        assertEquals("un espacio vacío no cambia", untouched, ProjectEdits.editPlacement(untouched, 5) { it })
    }

    @Test
    fun removePageShiftsLaterOverrides() {
        var p = filled(27) // 3 hojas de 9
        p = ProjectEdits.setText(p, TextRole.TITLE, "hoja1", card = 3)
        p = ProjectEdits.setText(p, TextRole.TITLE, "hoja2", card = 10)
        p = ProjectEdits.setText(p, TextRole.TITLE, "hoja3", card = 20)
        p = ProjectEdits.removePage(p, 1)
        assertEquals(2, p.pageCount)
        assertEquals("hoja1", p.cardOverrides["3"]!!.texts["title"])
        assertNull(p.cardOverrides["10"])
        assertEquals("hoja3", p.cardOverrides["11"]!!.texts["title"])
    }

    @Test
    fun removeOnlyPageClearsIt() {
        val p = ProjectEdits.removePage(filled(3), 0)
        assertEquals(0, p.placedCount)
        assertEquals(1, p.pageCount)
    }

    @Test
    fun customPaperIsClamped() {
        val p = ProjectEdits.setCustomPaper(filled(1), 10.0, 9000.0)
        assertEquals(80.0, p.settings.customWidthMM, 0.0)
        assertEquals(600.0, p.settings.customHeightMM, 0.0)
    }
}
```

- [ ] **Step 2: Correr y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.core.edit.*' --console=plain`
Expected: FAIL (no existen `ProjectEdits`/`MoodPreset`).

- [ ] **Step 3: Implementar `MoodPreset.kt`**

```kotlin
package com.polar.app.core.edit

/** Mismos estilos y frases que la Mac; las fuentes son ids de FontCatalog. */
enum class MoodPreset(
    val displayName: String,
    val hex: String,
    val fontName: String,
    val title: String,
    val subtitle: String,
    val caption: String
) {
    COUPLE("Parejas", "92394A", "Gelasio", "Nuestros momentos", "Tú y yo", "Una historia para guardar"),
    FRIENDS("Amigos", "38536F", "Josefin Sans", "Siempre juntos", "Amigos de verdad", "Los mejores recuerdos son compartidos"),
    FAMILY("Familia", "486855", "Libre Baskerville", "Nuestra familia", "Donde empieza todo", "El cariño que nos une"),
    PETS("Mascotas", "BC8952", "Patrick Hand", "Mi mejor compañía", "Huellas en el corazón", "Pequeñas patas, grandes aventuras"),
    TRAVEL("Viajes", "486855", "Josefin Sans", "Nuestra aventura", "Un lugar para recordar", "Coleccionando momentos"),
    CELEBRATION("Celebraciones", "C34048", "Dancing Script", "Un día especial", "Celebremos juntos", "Un recuerdo para siempre"),
    MINIMAL("Minimalista", "20242C", ".System", "Un instante", "Para recordar", "")
}
```

- [ ] **Step 4: Implementar `ProjectEdits.kt`**

```kotlin
package com.polar.app.core.edit

import com.polar.app.model.*

object ProjectEdits {
    const val MAX_PHOTOS = 2000
    val LAYOUT_PRESETS = listOf(1, 2, 4, 6, 8, 9, 12, 16)

    // ---------- Texto ----------

    fun setText(p: PolarProject, role: TextRole, value: String, card: Int? = null): PolarProject =
        if (card == null) p.copy(settings = p.settings.withText(role, value))
        else p.updateOverride(card) { it.copy(texts = it.texts + (role.key to value)) }

    fun clearOwnText(p: PolarProject, card: Int, role: TextRole): PolarProject =
        p.updateOverride(card) { it.copy(texts = it.texts - role.key) }

    fun applyTextToAll(p: PolarProject, role: TextRole): PolarProject =
        p.mapOverrides { it.copy(texts = it.texts - role.key) }

    fun editAppearance(
        p: PolarProject, role: TextRole, card: Int?, change: (TextAppearance) -> TextAppearance
    ): PolarProject =
        if (card == null) p.copy(settings = p.settings.withTextStyle(role, change(p.settings.textStyle(role))))
        else p.updateOverride(card) { o ->
            val base = o.styles[role.key] ?: p.settings.textStyle(role)
            o.copy(styles = o.styles + (role.key to change(base)))
        }

    fun clearOwnAppearance(p: PolarProject, card: Int, role: TextRole): PolarProject =
        p.updateOverride(card) { it.copy(styles = it.styles - role.key) }

    fun applyAppearanceToAll(p: PolarProject, role: TextRole): PolarProject =
        p.mapOverrides { it.copy(styles = it.styles - role.key) }

    fun resetAppearance(p: PolarProject, role: TextRole): PolarProject =
        p.copy(settings = p.settings.withoutTextStyle(role))

    // ---------- Fecha ----------

    fun setDateSource(p: PolarProject, source: DateSource, card: Int? = null): PolarProject =
        if (card == null) p.copy(settings = p.settings.copy(dateSource = source))
        else p.updateOverride(card) { it.copy(dateSource = source) }

    fun setChosenDate(p: PolarProject, epochMs: Long, card: Int? = null): PolarProject {
        val swift = SwiftDate.fromEpochMs(epochMs)
        return if (card == null) p.copy(settings = p.settings.copy(chosenDate = swift, dateSource = DateSource.CHOSEN))
        else p.updateOverride(card) { it.copy(chosenDate = swift, dateSource = DateSource.CHOSEN) }
    }

    fun setDateStyle(p: PolarProject, style: DateStyle): PolarProject =
        p.copy(settings = p.settings.copy(dateStyle = style))

    // ---------- Estilos rápidos ----------

    fun applyMood(p: PolarProject, mood: MoodPreset): PolarProject {
        var s = p.settings.copy(accentHex = mood.hex)
        for (role in TextRole.entries) {
            s = s.withTextStyle(role, s.textStyle(role).copy(fontName = mood.fontName, hex = ""))
        }
        return p.copy(settings = s)
    }

    fun applySuggestedPhrases(p: PolarProject, mood: MoodPreset): PolarProject {
        val s = p.settings.copy(title = mood.title, subtitle = mood.subtitle, caption = mood.caption)
        val keys = setOf(TextRole.TITLE.key, TextRole.SUBTITLE.key, TextRole.CAPTION.key)
        return p.copy(settings = s).mapOverrides { it.copy(texts = it.texts - keys) }
    }

    // ---------- Diseño y distribución ----------

    fun selectStyle(p: PolarProject, style: TemplateStyle): PolarProject {
        val oldPer = p.settings.style.photosPerCard
        val next = p.withStyle(style)
        val newPer = style.photosPerCard
        if (oldPer == newPer || p.cardOverrides.isEmpty()) return next
        val remapped = LinkedHashMap<String, CardOverride>()
        p.cardOverrides.entries
            .sortedBy { it.key.toInt() }
            .forEach { (key, value) -> remapped.putIfAbsent(((key.toInt() * oldPer) / newPer).toString(), value) }
        return next.copy(cardOverrides = remapped)
    }

    fun setGrid(p: PolarProject, columns: Int, rows: Int): PolarProject {
        if (p.settings.style == TemplateStyle.IMPORTED || columns !in 1..4 || rows !in 1..6) return p
        return p.copy(
            settings = p.settings.copy(columns = columns, rows = rows),
            placements = p.placements.dropLastWhile { it == null }
        ).normalized()
    }

    fun applyLayoutPreset(p: PolarProject, count: Int): PolarProject {
        val landscape = p.settings.orientation == PaperOrientation.LANDSCAPE
        val grid = when (count) {
            1 -> 1 to 1
            2 -> if (landscape) 2 to 1 else 1 to 2
            4 -> 2 to 2
            6 -> if (landscape) 3 to 2 else 2 to 3
            8 -> if (landscape) 4 to 2 else 2 to 4
            9 -> 3 to 3
            12 -> if (landscape) 4 to 3 else 3 to 4
            16 -> 4 to 4
            else -> return p
        }
        return setGrid(p, grid.first, grid.second)
    }

    fun updateSettings(p: PolarProject, change: (PrintSettings) -> PrintSettings): PolarProject =
        p.copy(settings = change(p.settings)).normalized()

    fun setCustomPaper(p: PolarProject, widthMM: Double, heightMM: Double): PolarProject {
        fun clamp(v: Double, fallback: Double) = if (v.isFinite()) v.coerceIn(80.0, 600.0) else fallback
        return updateSettings(p) {
            it.copy(paperSize = PaperSize.CUSTOM, customWidthMM = clamp(widthMM, 215.9), customHeightMM = clamp(heightMM, 279.4))
        }
    }

    // ---------- Fotos ----------

    fun addPhotos(p: PolarProject, assets: List<PhotoAsset>, fillFrom: Int?): PolarProject {
        val known = p.photos.map { it.path }.toSet()
        val room = (MAX_PHOTOS - p.photos.size).coerceAtLeast(0)
        val added = assets.filter { it.path !in known }.distinctBy { it.path }.take(room)
        if (added.isEmpty()) return p
        val placements = p.placements.toMutableList()
        if (fillFrom != null) {
            var slot = fillFrom.coerceAtLeast(0)
            for (asset in added) {
                while (slot < placements.size && placements[slot] != null) slot++
                while (placements.size <= slot) placements.add(null)
                placements[slot] = PhotoPlacement(assetID = asset.id)
                slot++
            }
        }
        return p.copy(photos = p.photos + added, placements = placements).normalized()
    }

    fun fillAll(p: PolarProject): PolarProject =
        if (p.photos.isEmpty()) p
        else p.copy(placements = p.photos.map { PhotoPlacement(assetID = it.id) }).normalized()

    fun assign(p: PolarProject, slot: Int, assetId: String): PolarProject {
        if (slot < 0 || p.photos.none { it.id == assetId }) return p
        val list = p.placements.toMutableList()
        while (list.size <= slot) list.add(null)
        list[slot] = PhotoPlacement(assetID = assetId)
        return p.copy(placements = list).normalized()
    }

    fun clearSlot(p: PolarProject, slot: Int): PolarProject {
        if (slot !in p.placements.indices) return p
        return p.copy(placements = p.placements.toMutableList().also { it[slot] = null })
    }

    fun editPlacement(p: PolarProject, slot: Int, change: (PhotoPlacement) -> PhotoPlacement): PolarProject {
        val current = p.placements.getOrNull(slot) ?: return p
        val next = change(current).let {
            it.copy(
                zoom = it.zoom.coerceIn(1.0, 4.0),
                offsetX = it.offsetX.coerceIn(-1.0, 1.0),
                offsetY = it.offsetY.coerceIn(-1.0, 1.0),
                quarterTurns = ((it.quarterTurns % 4) + 4) % 4
            )
        }
        return p.copy(placements = p.placements.toMutableList().also { it[slot] = next })
    }

    // ---------- Hojas ----------

    fun clearPage(p: PolarProject, page: Int): PolarProject {
        val cap = p.settings.capacity
        val list = p.normalized().placements.toMutableList()
        for (i in page * cap until minOf(list.size, (page + 1) * cap)) list[i] = null
        return p.copy(placements = list)
    }

    fun addPage(p: PolarProject): PolarProject {
        val cap = p.settings.capacity
        val base = p.normalized()
        if (base.placements.size + cap > MAX_PHOTOS + cap - 1) return p
        return base.copy(placements = base.placements + List(cap) { null })
    }

    fun removePage(p: PolarProject, page: Int): PolarProject {
        val base = p.normalized()
        if (base.pageCount <= 1) return clearPage(base, 0)
        val cap = base.settings.capacity
        val list = base.placements.toMutableList()
        list.subList(page * cap, minOf(list.size, (page + 1) * cap)).clear()
        val perPage = base.cardsPerPage
        val first = page * perPage
        val shifted = base.cardOverrides.mapNotNull { (key, value) ->
            val card = key.toInt()
            when {
                card < first -> key to value
                card < first + perPage -> null
                else -> (card - perPage).toString() to value
            }
        }.toMap()
        return base.copy(placements = list, cardOverrides = shifted).normalized()
    }

    // ---------- Plantilla importada ----------

    fun editTemplateRegion(p: PolarProject, index: Int, change: (TemplateRegion) -> TemplateRegion): PolarProject {
        val t = p.settings.importedTemplate ?: return p
        if (index !in t.regions.indices) return p
        val regions = t.regions.toMutableList().also { it[index] = change(it[index]).clamped() }
        return p.copy(settings = p.settings.copy(importedTemplate = t.copy(regions = regions)))
    }

    fun addTemplateRegion(p: PolarProject): PolarProject {
        val t = p.settings.importedTemplate ?: return p
        if (t.regions.size >= 64) return p
        val region = TemplateRegion(x = 0.3, y = 0.3, width = 0.4, height = 0.3)
        return p.copy(settings = p.settings.copy(importedTemplate = t.copy(regions = t.regions + region))).normalized()
    }

    fun removeTemplateRegion(p: PolarProject, index: Int): PolarProject {
        val t = p.settings.importedTemplate ?: return p
        if (t.regions.size <= 1 || index !in t.regions.indices) return p
        return p.copy(settings = p.settings.copy(importedTemplate = t.copy(regions = t.regions.filterIndexed { i, _ -> i != index })))
    }
}

private fun PolarProject.updateOverride(card: Int, change: (CardOverride) -> CardOverride): PolarProject {
    val key = card.toString()
    val next = change(cardOverrides[key] ?: CardOverride())
    val map = cardOverrides.toMutableMap()
    if (next.isEmpty) map.remove(key) else map[key] = next
    return copy(cardOverrides = map)
}

private fun PolarProject.mapOverrides(change: (CardOverride) -> CardOverride): PolarProject =
    copy(cardOverrides = cardOverrides.mapValues { change(it.value) }.filterValues { !it.isEmpty })
```

- [ ] **Step 5: Correr las pruebas**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.core.edit.*' --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add PolarAndroid/app/src/main/java/com/polar/app/core/edit PolarAndroid/app/src/test/java/com/polar/app/core/edit
git commit -m "feat(edit): ediciones puras de texto, diseño, fotos y hojas"
```

---

### Task 5: `UndoStack` con transacciones

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/core/history/UndoStack.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/core/history/UndoStackTest.kt`

**Interfaces:**
- Produces: `class UndoStack<T>(limit: Int = 50)`:
  - `val canUndo: Boolean`, `val canRedo: Boolean`
  - `record(before: T)`: no hace nada si hay una transacción abierta
  - `beginTransaction(before: T)`, `endTransaction(current: T)`
  - `undo(current: T): T?`, `redo(current: T): T?`
  - `clear()`

- [ ] **Step 1: Pruebas que fallan**

```kotlin
package com.polar.app.core.history

import org.junit.Assert.*
import org.junit.Test

class UndoStackTest {
    @Test
    fun recordUndoRedo() {
        val s = UndoStack<String>()
        s.record("a")            // estado previo a convertirlo en "b"
        assertTrue(s.canUndo)
        assertEquals("a", s.undo("b"))
        assertTrue(s.canRedo)
        assertEquals("b", s.redo("a"))
    }

    @Test
    fun newChangeClearsRedo() {
        val s = UndoStack<String>()
        s.record("a"); s.undo("b")
        s.record("a")
        assertFalse(s.canRedo)
    }

    @Test
    fun transactionGroupsManyEditsInOneStep() {
        val s = UndoStack<String>()
        s.beginTransaction("")
        s.record("L"); s.record("Lu")   // se ignoran dentro de la transacción
        s.endTransaction("Lu y Max")
        assertEquals("", s.undo("Lu y Max"))
        assertFalse(s.canUndo)
    }

    @Test
    fun transactionWithoutChangeAddsNothing() {
        val s = UndoStack<String>()
        s.beginTransaction("x"); s.endTransaction("x")
        assertFalse(s.canUndo)
    }

    @Test
    fun respectsLimit() {
        val s = UndoStack<Int>(limit = 3)
        (1..5).forEach { s.record(it) }
        assertEquals(5, s.undo(6)); assertEquals(4, s.undo(5)); assertEquals(3, s.undo(4))
        assertNull(s.undo(3))
    }
}
```

- [ ] **Step 2: Correr y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.core.history.*' --console=plain`
Expected: FAIL (no existe `UndoStack`).

- [ ] **Step 3: Implementar**

```kotlin
package com.polar.app.core.history

/** Historial con transacciones: un campo de texto o un slider cuentan como un solo paso. */
class UndoStack<T>(private val limit: Int = 50) {
    private val undoList = ArrayDeque<T>()
    private val redoList = ArrayDeque<T>()
    private var transactionBase: T? = null
    private var inTransaction = false

    val canUndo: Boolean get() = undoList.isNotEmpty()
    val canRedo: Boolean get() = redoList.isNotEmpty()

    fun record(before: T) {
        if (inTransaction) return
        push(before)
    }

    fun beginTransaction(before: T) {
        if (inTransaction) return
        inTransaction = true
        transactionBase = before
    }

    fun endTransaction(current: T) {
        if (!inTransaction) return
        inTransaction = false
        val base = transactionBase
        transactionBase = null
        if (base != null && base != current) push(base)
    }

    fun undo(current: T): T? {
        val previous = undoList.removeLastOrNull() ?: return null
        redoList.addLast(current)
        return previous
    }

    fun redo(current: T): T? {
        val next = redoList.removeLastOrNull() ?: return null
        undoList.addLast(current)
        return next
    }

    fun clear() {
        undoList.clear(); redoList.clear(); inTransaction = false; transactionBase = null
    }

    private fun push(before: T) {
        undoList.addLast(before)
        while (undoList.size > limit) undoList.removeFirst()
        redoList.clear()
    }
}
```

- [ ] **Step 4: Correr las pruebas**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.core.history.*' --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add PolarAndroid/app/src/main/java/com/polar/app/core/history PolarAndroid/app/src/test/java/com/polar/app/core/history
git commit -m "feat(history): deshacer y rehacer con transacciones"
```

---
### Task 6: Fuentes incluidas y `FontCatalog`

**Files:**
- Create: `PolarAndroid/tools/download-fonts.sh`
- Create (descargados): `PolarAndroid/app/src/main/res/font/*.ttf` (20) y `PolarAndroid/app/src/main/assets/licenses/*.txt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/data/FontCatalog.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/engine/FontProvider.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/data/AndroidFontProvider.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/data/FontCatalogTest.kt`

**Interfaces:**
- Consumes (Task 4): `MoodPreset.fontName`.
- Produces:
  - `fun interface FontProvider { fun typeface(fontName: String, bold: Boolean, italic: Boolean): Typeface }` y `object SystemFontProvider : FontProvider` (paquete `engine`).
  - `enum FontGroup`.
  - `data class FontChoice(id, group, res: Int?, previewScale: Float)` con `displayName`.
  - `object FontCatalog { const val SYSTEM_ID = ".System"; val all: List<FontChoice>; fun find(id: String): FontChoice; fun groups(): List<Pair<FontGroup, List<FontChoice>>> }`.
  - `class AndroidFontProvider(context): FontProvider`.

- [ ] **Step 1: DETENTE y pide autorización al usuario para descargar**

Mensaje exacto para el usuario:

> Voy a descargar 20 fuentes libres del repositorio oficial de Google Fonts en GitHub (`github.com/google/fonts`). Son 4–6 MB en total y cada fuente viene con su licencia:
> - **Manuscritas:** Caveat, Kalam, Homemade Apple, Sacramento, Dancing Script, Patrick Hand, Shadows Into Light, Amatic SC.
> - **De libro:** Gelasio, Libre Baskerville, EB Garamond.
> - **Palo seco:** Josefin Sans, Nunito, Quicksand, Montserrat.
> - **Máquina de escribir:** Special Elite, Courier Prime.
> - **Decorativas:** Abril Fatface, Pacifico, Lobster.
>
> ¿Autorizas la descarga?

No sigas sin un "sí" explícito.

- [ ] **Step 2: Crear el script de descarga**

`PolarAndroid/tools/download-fonts.sh`:

```bash
#!/usr/bin/env bash
# Descarga las fuentes de Polar desde github.com/google/fonts con su licencia.
set -euo pipefail
cd "$(dirname "$0")/.."
RES=app/src/main/res/font
LIC=app/src/main/assets/licenses
mkdir -p "$RES" "$LIC"

FONTS=(
  "ofl/caveat|caveat" "ofl/kalam|kalam" "apache/homemadeapple|homemade_apple" "ofl/sacramento|sacramento"
  "ofl/dancingscript|dancing_script" "ofl/patrickhand|patrick_hand" "ofl/shadowsintolight|shadows_into_light"
  "ofl/amaticsc|amatic_sc" "ofl/gelasio|gelasio" "ofl/librebaskerville|libre_baskerville" "ofl/ebgaramond|eb_garamond"
  "ofl/josefinsans|josefin_sans" "ofl/nunito|nunito" "ofl/quicksand|quicksand" "ofl/montserrat|montserrat"
  "apache/specialelite|special_elite" "ofl/courierprime|courier_prime" "ofl/abrilfatface|abril_fatface"
  "ofl/pacifico|pacifico" "ofl/lobster|lobster"
)

pick_ttf='import json,sys
files=[f for f in json.load(sys.stdin) if f["name"].endswith(".ttf")]
upright=[f for f in files if "Italic" not in f["name"]] or files
upright.sort(key=lambda f:(0 if ("Regular" in f["name"] or "[" in f["name"]) else 1, len(f["name"])))
print(upright[0]["download_url"])'
pick_lic='import json,sys
print([f["download_url"] for f in json.load(sys.stdin) if f["name"] in ("OFL.txt","LICENSE.txt")][0])'

for entry in "${FONTS[@]}"; do
  dir="${entry%%|*}"; res="${entry##*|}"
  listing=$(curl -fsSL "https://api.github.com/repos/google/fonts/contents/$dir")
  curl -fsSL "$(printf '%s' "$listing" | python3 -c "$pick_ttf")" -o "$RES/$res.ttf"
  curl -fsSL "$(printf '%s' "$listing" | python3 -c "$pick_lic")" -o "$LIC/$res.txt"
  echo "ok  $res  $(du -h "$RES/$res.ttf" | cut -f1)"
done
```

- [ ] **Step 3: Correr el script y revisar el resultado**

```bash
chmod +x PolarAndroid/tools/download-fonts.sh
PolarAndroid/tools/download-fonts.sh
ls PolarAndroid/app/src/main/res/font | wc -l     # 20
du -sh PolarAndroid/app/src/main/res/font
```
Expected: 20 archivos `.ttf`, 20 licencias y un total menor a 8 MB. Si una carpeta no existe en el repositorio (la API responde 404), búscala con `curl -fsSL https://api.github.com/repos/google/fonts/contents/ofl | python3 -c 'import json,sys;print([f["name"] for f in json.load(sys.stdin)])' | tr ',' '\n' | grep -i <nombre>` y corrige la entrada.

- [ ] **Step 4: Pruebas que fallan**

`FontCatalogTest.kt`:

```kotlin
package com.polar.app.data

import com.polar.app.core.edit.MoodPreset
import org.junit.Assert.*
import org.junit.Test

class FontCatalogTest {
    @Test
    fun twentyBundledFontsPlusSystem() {
        assertEquals(21, FontCatalog.all.size)
        assertEquals(FontCatalog.all.size, FontCatalog.all.map { it.id }.toSet().size)
        assertEquals(20, FontCatalog.all.count { it.res != null })
    }

    @Test
    fun macNamesMapToBundledFonts() {
        assertEquals("Gelasio", FontCatalog.find("Georgia").id)
        assertEquals("Libre Baskerville", FontCatalog.find("Baskerville").id)
        assertEquals("Dancing Script", FontCatalog.find("SnellRoundhand").id)
        assertEquals(FontCatalog.SYSTEM_ID, FontCatalog.find(".System").id)
        assertEquals(FontCatalog.SYSTEM_ID, FontCatalog.find("Fuente que no existe").id)
    }

    @Test
    fun everyMoodFontExists() {
        for (mood in MoodPreset.entries) {
            assertEquals(mood.fontName, FontCatalog.find(mood.fontName).id)
        }
    }

    @Test
    fun groupsKeepCatalogOrder() {
        val groups = FontCatalog.groups()
        assertEquals(FontGroup.SYSTEM, groups.first().first)
        assertEquals(21, groups.sumOf { it.second.size })
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.data.FontCatalogTest' --console=plain`
Expected: FAIL (no existe `FontCatalog`).

- [ ] **Step 5: Implementar `engine/FontProvider.kt`**

```kotlin
package com.polar.app.engine

import android.graphics.Typeface

/** Da la tipografía de un texto; el motor no sabe de recursos de Android. */
fun interface FontProvider {
    fun typeface(fontName: String, bold: Boolean, italic: Boolean): Typeface
}

object SystemFontProvider : FontProvider {
    override fun typeface(fontName: String, bold: Boolean, italic: Boolean): Typeface =
        Typeface.create(Typeface.DEFAULT, styleOf(bold, italic))
}

fun styleOf(bold: Boolean, italic: Boolean): Int = when {
    bold && italic -> Typeface.BOLD_ITALIC
    bold -> Typeface.BOLD
    italic -> Typeface.ITALIC
    else -> Typeface.NORMAL
}
```

- [ ] **Step 6: Implementar `data/FontCatalog.kt`**

```kotlin
package com.polar.app.data

import com.polar.app.R

enum class FontGroup(val displayName: String) {
    SYSTEM("Del sistema"), HANDWRITTEN("Manuscritas"), BOOK("De libro"),
    SANS("Palo seco"), TYPEWRITER("Máquina de escribir"), DISPLAY("Decorativas")
}

data class FontChoice(val id: String, val group: FontGroup, val res: Int?, val previewScale: Float = 1f) {
    val displayName: String get() = if (res == null) "Sistema" else id
}

object FontCatalog {
    const val SYSTEM_ID = ".System"

    val all: List<FontChoice> = listOf(
        FontChoice(SYSTEM_ID, FontGroup.SYSTEM, null),
        FontChoice("Caveat", FontGroup.HANDWRITTEN, R.font.caveat, 1.2f),
        FontChoice("Kalam", FontGroup.HANDWRITTEN, R.font.kalam),
        FontChoice("Homemade Apple", FontGroup.HANDWRITTEN, R.font.homemade_apple, 0.75f),
        FontChoice("Sacramento", FontGroup.HANDWRITTEN, R.font.sacramento, 1.3f),
        FontChoice("Dancing Script", FontGroup.HANDWRITTEN, R.font.dancing_script, 1.1f),
        FontChoice("Patrick Hand", FontGroup.HANDWRITTEN, R.font.patrick_hand, 1.05f),
        FontChoice("Shadows Into Light", FontGroup.HANDWRITTEN, R.font.shadows_into_light),
        FontChoice("Amatic SC", FontGroup.HANDWRITTEN, R.font.amatic_sc, 1.25f),
        FontChoice("Gelasio", FontGroup.BOOK, R.font.gelasio),
        FontChoice("Libre Baskerville", FontGroup.BOOK, R.font.libre_baskerville, 0.9f),
        FontChoice("EB Garamond", FontGroup.BOOK, R.font.eb_garamond, 1.05f),
        FontChoice("Josefin Sans", FontGroup.SANS, R.font.josefin_sans),
        FontChoice("Nunito", FontGroup.SANS, R.font.nunito),
        FontChoice("Quicksand", FontGroup.SANS, R.font.quicksand),
        FontChoice("Montserrat", FontGroup.SANS, R.font.montserrat, 0.9f),
        FontChoice("Special Elite", FontGroup.TYPEWRITER, R.font.special_elite, 0.9f),
        FontChoice("Courier Prime", FontGroup.TYPEWRITER, R.font.courier_prime, 0.9f),
        FontChoice("Abril Fatface", FontGroup.DISPLAY, R.font.abril_fatface, 0.9f),
        FontChoice("Pacifico", FontGroup.DISPLAY, R.font.pacifico, 0.9f),
        FontChoice("Lobster", FontGroup.DISPLAY, R.font.lobster)
    )

    /** Nombres de fuentes de la Mac y de la versión anterior de Android. */
    private val aliases = mapOf(
        "Georgia" to "Gelasio", "Baskerville" to "Libre Baskerville", "AvenirNext-Medium" to "Josefin Sans",
        "Avenir Next" to "Josefin Sans", "ChalkboardSE-Regular" to "Patrick Hand", "SnellRoundhand" to "Dancing Script",
        "Courier" to "Courier Prime", "serif" to "Gelasio", "sans-serif-medium" to "Montserrat",
        "casual" to "Patrick Hand", "cursive" to "Dancing Script"
    )

    fun find(id: String): FontChoice =
        all.firstOrNull { it.id == id }
            ?: aliases[id]?.let { alias -> all.first { it.id == alias } }
            ?: all.first()

    fun groups(): List<Pair<FontGroup, List<FontChoice>>> =
        FontGroup.entries.map { g -> g to all.filter { it.group == g } }.filter { it.second.isNotEmpty() }
}
```

- [ ] **Step 7: Implementar `data/AndroidFontProvider.kt`**

```kotlin
package com.polar.app.data

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.polar.app.engine.FontProvider
import com.polar.app.engine.styleOf
import java.util.concurrent.ConcurrentHashMap

class AndroidFontProvider(private val context: Context) : FontProvider {
    private val cache = ConcurrentHashMap<String, Typeface>()

    override fun typeface(fontName: String, bold: Boolean, italic: Boolean): Typeface {
        val choice = FontCatalog.find(fontName)
        val base = cache.getOrPut(choice.id) {
            choice.res?.let { ResourcesCompat.getFont(context, it) } ?: Typeface.DEFAULT
        }
        val style = styleOf(bold, italic)
        return if (style == Typeface.NORMAL) base else Typeface.create(base, style)
    }
}
```

- [ ] **Step 8: Correr las pruebas y compilar**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.data.FontCatalogTest' assembleDebug --console=plain`
Expected: PASS y `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add PolarAndroid/tools PolarAndroid/app/src/main/res/font PolarAndroid/app/src/main/assets PolarAndroid/app/src/main/java/com/polar/app/data PolarAndroid/app/src/main/java/com/polar/app/engine/FontProvider.kt PolarAndroid/app/src/test/java/com/polar/app/data
git commit -m "feat(fonts): 20 fuentes libres incluidas y catálogo con nombres de Mac"
```

---

### Task 7: Motor: texto por tarjeta, fuentes, fecha y orientación EXIF

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/engine/CardTextLayout.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/engine/PolarRenderer.kt` (`drawPage`, `drawCard`, `drawCardElements`, `drawText`, `drawCalendar`; borrar `drawInstagram`)
- Create: `PolarAndroid/app/src/main/java/com/polar/app/data/BitmapMath.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/data/BitmapLoader.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/engine/CardTextLayoutTest.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/data/BitmapMathTest.kt`

**Interfaces:**
- Consumes: `TextResolver` (Task 3), `FontProvider` (Task 6), `PolarRenderer.calculatePhotoRects` (existe).
- Produces:
  - `enum class TextAlign { LEFT, CENTER, RIGHT }`.
  - `data class TextItem(role: TextRole?, text: String, rect: PolarRect, sizePt: Double, defaultColor: Int, defaultBold: Boolean, align: TextAlign, appearance: TextAppearance?)`.
  - `object CardTextLayout { fun items(project, card: PolarRect, cardIndex: Int, photoSlots: List<PolarRect>, accent: Int, zone: TimeZone = TimeZone.getDefault()): List<TextItem> }`.
  - `PolarRenderer.drawPage(canvas, project, page, isPreview, scale = 1f, bitmapProvider = { null }, templateBitmap = null, fonts: FontProvider = SystemFontProvider)`.
  - `object BitmapMath { sampleSize(w, h, maxDim): Int; orientedSize(w, h, exif): Pair<Int,Int>; isQuarterTurned(exif): Boolean }`.
  - `class BitmapLoader(context) { fun load(path: String, maxDim: Int): Bitmap?; fun readInfo(uri: Uri): PhotoInfo?; fun clear() }` con `data class PhotoInfo(width: Int, height: Int, takenAtEpochMs: Long?)`. El ancho y el alto ya vienen orientados.
  - Constantes `BitmapLoader.PREVIEW_MAX = 1600`, `BitmapLoader.EXPORT_MAX = 3600`.

- [ ] **Step 1: Pruebas que fallan**

`CardTextLayoutTest.kt`:

```kotlin
package com.polar.app.engine

import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class CardTextLayoutTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val card = PolarRect(0.0, 0.0, 100.0, 140.0)
    private val accent = 0xFF92394A.toInt()
    private val photo = PhotoAsset(path = "a.jpg", pixelWidth = 10, pixelHeight = 10, takenAtEpochMs = 1_771_070_400_000)

    private fun project(style: TemplateStyle = TemplateStyle.POLAROID) =
        PolarProject(settings = PrintSettings(style = style, title = "Lu y Max"), photos = listOf(photo),
            placements = List(9) { PhotoPlacement(photo.id) }).normalized()

    private fun items(p: PolarProject, index: Int) =
        CardTextLayout.items(p, card, index, PolarRenderer.calculatePhotoRects(card, p.settings.style, p.settings), accent, utc)

    @Test
    fun polaroidShowsTitleAndSubtitleWithoutDateByDefault() {
        val roles = items(project(), 0).map { it.role }
        assertEquals(listOf(TextRole.TITLE, TextRole.SUBTITLE), roles)
    }

    @Test
    fun dateAppearsWhenEnabled() {
        val p = ProjectEdits.setDateSource(project(), DateSource.PHOTO)
        val date = items(p, 0).single { it.role == TextRole.DATE }
        assertEquals("14 feb 2026", date.text)
    }

    @Test
    fun ownTextAppliesOnlyToItsCard() {
        val p = ProjectEdits.setText(project(), TextRole.TITLE, "El brindis", card = 1)
        assertEquals("Lu y Max", items(p, 0).first { it.role == TextRole.TITLE }.text)
        assertEquals("El brindis", items(p, 1).first { it.role == TextRole.TITLE }.text)
    }

    @Test
    fun emptyOwnTextHidesOnlyThatCard() {
        val p = ProjectEdits.setText(project(), TextRole.TITLE, "", card = 1)
        assertEquals("", items(p, 1).first { it.role == TextRole.TITLE }.text)
        assertEquals("Lu y Max", items(p, 2).first { it.role == TextRole.TITLE }.text)
    }

    @Test
    fun ownAppearanceTravelsWithItem() {
        val p = ProjectEdits.editAppearance(project(), TextRole.TITLE, 1) { it.copy(hex = "C34048", fontName = "Caveat") }
        assertEquals("C34048", items(p, 1).first { it.role == TextRole.TITLE }.appearance!!.hex)
        assertEquals("", items(p, 0).first { it.role == TextRole.TITLE }.appearance!!.hex)
    }

    @Test
    fun filmAndCalendarHaveNoUserText() {
        val film = ProjectEdits.setDateSource(project(TemplateStyle.FILM_VERTICAL), DateSource.PHOTO)
        assertTrue(items(film, 0).none { it.role != null })
    }

    @Test
    fun ticketNumberUsesCardSlot() {
        val fixed = items(project(TemplateStyle.TICKET), 3).filter { it.role == null }.map { it.text }
        assertTrue(fixed.contains("Nº 004"))
    }

    @Test
    fun instagramUsesTitleAndCaptionRoles() {
        val roles = items(project(TemplateStyle.INSTAGRAM), 0).mapNotNull { it.role }
        assertEquals(listOf(TextRole.TITLE, TextRole.CAPTION), roles)
    }
}
```

`BitmapMathTest.kt`:

```kotlin
package com.polar.app.data

import org.junit.Assert.*
import org.junit.Test

class BitmapMathTest {
    @Test
    fun sampleSizeKeepsLongSideUnderLimit() {
        assertEquals(1, BitmapMath.sampleSize(1000, 800, 1600))
        assertEquals(4, BitmapMath.sampleSize(8000, 6000, 2000))
        assertEquals(4, BitmapMath.sampleSize(4001, 10, 2000))
        assertEquals(8, BitmapMath.sampleSize(10000, 7500, 1600)) // foto de 75 MP
    }

    @Test
    fun exifQuarterTurnsSwapSize() {
        assertEquals(3000 to 4000, BitmapMath.orientedSize(4000, 3000, 6))
        assertEquals(3000 to 4000, BitmapMath.orientedSize(4000, 3000, 8))
        assertEquals(4000 to 3000, BitmapMath.orientedSize(4000, 3000, 3))
        assertEquals(4000 to 3000, BitmapMath.orientedSize(4000, 3000, 1))
        assertTrue(BitmapMath.isQuarterTurned(5))
        assertFalse(BitmapMath.isQuarterTurned(2))
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.engine.CardTextLayoutTest' --tests 'com.polar.app.data.BitmapMathTest' --console=plain`
Expected: FAIL (clases inexistentes).

- [ ] **Step 2: Implementar `engine/CardTextLayout.kt`**

```kotlin
package com.polar.app.engine

import com.polar.app.core.text.TextResolver
import com.polar.app.model.*
import java.util.Locale
import java.util.TimeZone

enum class TextAlign { LEFT, CENTER, RIGHT }

data class TextItem(
    val role: TextRole?,
    val text: String,
    val rect: PolarRect,
    val sizePt: Double,
    val defaultColor: Int,
    val defaultBold: Boolean,
    val align: TextAlign,
    val appearance: TextAppearance?
)

/** Qué textos lleva cada tarjeta y dónde. Puro Kotlin: se prueba sin Canvas. */
object CardTextLayout {
    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val DARK = 0xFF444444.toInt()
    private const val GRAY = 0xFF888888.toInt()
    private const val STAMP = 0xFFFF8C2E.toInt() // naranja de fechador de cámara

    fun items(
        project: PolarProject,
        card: PolarRect,
        cardIndex: Int,
        photoSlots: List<PolarRect>,
        accent: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): List<TextItem> {
        val style = project.settings.style
        val fontSize = card.width * 0.06
        val firstSlot = project.firstSlotOfCard(cardIndex)
        val out = mutableListOf<TextItem>()

        fun r(x: Double, y: Double, w: Double, h: Double) = PolarRect(
            card.minX + x * card.width, card.minY + y * card.height,
            card.minX + (x + w) * card.width, card.minY + (y + h) * card.height
        )
        fun role(role: TextRole, rect: PolarRect, size: Double, color: Int, bold: Boolean = false, align: TextAlign = TextAlign.CENTER) {
            out += TextItem(role, TextResolver.text(project, cardIndex, role, zone), rect, size, color, bold, align,
                TextResolver.appearance(project, cardIndex, role))
        }
        fun fixed(text: String, rect: PolarRect, size: Double, color: Int, bold: Boolean = false, align: TextAlign = TextAlign.CENTER) {
            out += TextItem(null, text, rect, size, color, bold, align, null)
        }

        when (style) {
            TemplateStyle.POLAROID, TemplateStyle.MINI -> {
                role(TextRole.TITLE, r(0.07, 0.82, 0.86, 0.075), fontSize, accent, true)
                role(TextRole.SUBTITLE, r(0.07, 0.905, 0.86, 0.05), fontSize * 0.66, DARK)
            }
            TemplateStyle.SPOTIFY -> {
                role(TextRole.SONG, r(0.065, 0.686, 0.78, 0.065), fontSize * 1.12, BLACK, true, TextAlign.LEFT)
                role(TextRole.ARTIST, r(0.065, 0.757, 0.78, 0.045), fontSize * 0.78, GRAY, false, TextAlign.LEFT)
            }
            TemplateStyle.PLAYER_RED -> {
                role(TextRole.SONG, r(0.07, 0.634, 0.80, 0.055), fontSize * 0.83, WHITE, true, TextAlign.LEFT)
                role(TextRole.ARTIST, r(0.07, 0.693, 0.80, 0.04), fontSize * 0.62, WHITE, false, TextAlign.LEFT)
            }
            TemplateStyle.PLAYER_GRAY -> {
                role(TextRole.SONG, r(0.56, 0.16, 0.38, 0.11), card.height * 0.064, WHITE, true, TextAlign.LEFT)
                role(TextRole.ARTIST, r(0.56, 0.30, 0.38, 0.09), card.height * 0.05, WHITE, false, TextAlign.LEFT)
            }
            TemplateStyle.TICKET -> {
                role(TextRole.TITLE, r(0.06, 0.79, 0.64, 0.095), card.height * 0.075, accent, true)
                role(TextRole.SUBTITLE, r(0.06, 0.895, 0.64, 0.07), card.height * 0.05, DARK)
                fixed("POLAR", r(0.76, 0.14, 0.22, 0.12), card.height * 0.10, WHITE, true)
                fixed(String.format(Locale.ROOT, "Nº %03d", firstSlot + 1), r(0.76, 0.65, 0.22, 0.12), card.height * 0.075, WHITE, true)
            }
            TemplateStyle.INSTAGRAM -> {
                fixed("Instagram", r(0.16, 0.026, 0.53, 0.06), fontSize * 0.88, BLACK, true, TextAlign.LEFT)
                role(TextRole.TITLE, r(0.17, 0.116, 0.69, 0.044), fontSize * 0.7, BLACK, true, TextAlign.LEFT)
                fixed("⋮", r(0.89, 0.106, 0.06, 0.065), fontSize, BLACK, true)
                role(TextRole.CAPTION, r(0.065, 0.863, 0.87, 0.04), fontSize * 0.63, BLACK, false, TextAlign.LEFT)
            }
            TemplateStyle.CUSTOM -> {
                role(TextRole.TITLE, r(0.06, 0.81, 0.88, 0.08), fontSize * 1.15, accent, true)
                role(TextRole.SUBTITLE, r(0.06, 0.91, 0.88, 0.045), fontSize * 0.75, DARK)
            }
            TemplateStyle.SQUARE -> {
                role(TextRole.TITLE, r(0.06, 0.845, 0.88, 0.075), fontSize, accent, true)
                role(TextRole.SUBTITLE, r(0.06, 0.93, 0.88, 0.045), fontSize * 0.66, DARK)
            }
            TemplateStyle.POSTCARD -> {
                role(TextRole.TITLE, r(0.70, 0.23, 0.26, 0.19), card.height * 0.09, accent, true)
                role(TextRole.SUBTITLE, r(0.70, 0.46, 0.26, 0.16), card.height * 0.055, DARK)
                role(TextRole.CAPTION, r(0.70, 0.69, 0.26, 0.15), card.height * 0.043, DARK)
            }
            TemplateStyle.BOTANICAL -> {
                role(TextRole.TITLE, r(0.08, 0.80, 0.84, 0.08), fontSize * 1.10, accent, true)
                role(TextRole.SUBTITLE, r(0.08, 0.90, 0.84, 0.055), fontSize * 0.70, DARK)
            }
            TemplateStyle.CELEBRATION -> {
                role(TextRole.TITLE, r(0.07, 0.78, 0.86, 0.07), fontSize * 1.1, accent, true)
                role(TextRole.CAPTION, r(0.07, 0.85, 0.86, 0.08), fontSize * 0.70, DARK)
                role(TextRole.SUBTITLE, r(0.07, 0.94, 0.86, 0.035), fontSize * 0.55, accent)
            }
            TemplateStyle.PETS -> {
                role(TextRole.TITLE, r(0.14, 0.79, 0.72, 0.08), fontSize * 1.15, accent, true)
                role(TextRole.SUBTITLE, r(0.07, 0.90, 0.86, 0.055), fontSize * 0.75, DARK)
            }
            TemplateStyle.HEART -> {
                role(TextRole.TITLE, r(0.07, 0.80, 0.86, 0.075), fontSize * 1.05, accent, true)
                role(TextRole.SUBTITLE, r(0.07, 0.905, 0.86, 0.05), fontSize * 0.72, DARK)
            }
            TemplateStyle.EDITORIAL -> {
                role(TextRole.TITLE, r(0.065, 0.045, 0.87, 0.08), fontSize * 1.5, accent, true)
                role(TextRole.SUBTITLE, r(0.065, 0.135, 0.87, 0.04), fontSize * 0.55, BLACK, false, TextAlign.LEFT)
                role(TextRole.CAPTION, r(0.065, 0.79, 0.87, 0.12), fontSize * 0.80, DARK, false, TextAlign.LEFT)
                fixed(String.format(Locale.ROOT, "POLAR / %03d", firstSlot + 1), r(0.065, 0.95, 0.87, 0.025), fontSize * 0.42, accent, false, TextAlign.RIGHT)
            }
            TemplateStyle.FILM_VERTICAL, TemplateStyle.FILM_HORIZONTAL, TemplateStyle.CALENDAR,
            TemplateStyle.BORDERLESS, TemplateStyle.IMPORTED -> Unit
        }

        if (style.supportsDate && TextResolver.dateSource(project, cardIndex) != DateSource.NONE) {
            dateSlot(style, ::r, fontSize, card, accent)?.let { (rect, size, color, align) ->
                role(TextRole.DATE, rect, size, color, false, align)
            }
        }
        return out
    }

    private data class DateSlot(val rect: PolarRect, val size: Double, val color: Int, val align: TextAlign)

    private fun dateSlot(
        style: TemplateStyle, r: (Double, Double, Double, Double) -> PolarRect, fontSize: Double, card: PolarRect, accent: Int
    ): DateSlot? = when (style) {
        TemplateStyle.POLAROID, TemplateStyle.MINI, TemplateStyle.PETS, TemplateStyle.HEART ->
            DateSlot(r(0.07, 0.955, 0.86, 0.035), fontSize * 0.5, GRAY, TextAlign.CENTER)
        TemplateStyle.SPOTIFY -> DateSlot(r(0.55, 0.757, 0.30, 0.045), fontSize * 0.6, GRAY, TextAlign.RIGHT)
        TemplateStyle.PLAYER_RED -> DateSlot(r(0.55, 0.693, 0.32, 0.04), fontSize * 0.55, WHITE, TextAlign.RIGHT)
        TemplateStyle.PLAYER_GRAY -> DateSlot(r(0.56, 0.86, 0.38, 0.08), card.height * 0.045, WHITE, TextAlign.LEFT)
        TemplateStyle.TICKET -> DateSlot(r(0.76, 0.40, 0.22, 0.10), card.height * 0.06, WHITE, TextAlign.CENTER)
        TemplateStyle.INSTAGRAM -> DateSlot(r(0.065, 0.91, 0.87, 0.035), fontSize * 0.5, GRAY, TextAlign.LEFT)
        TemplateStyle.CUSTOM -> DateSlot(r(0.06, 0.955, 0.88, 0.035), fontSize * 0.5, GRAY, TextAlign.CENTER)
        TemplateStyle.SQUARE -> DateSlot(r(0.06, 0.975, 0.88, 0.022), fontSize * 0.42, GRAY, TextAlign.CENTER)
        TemplateStyle.POSTCARD -> DateSlot(r(0.70, 0.86, 0.26, 0.07), card.height * 0.04, GRAY, TextAlign.CENTER)
        TemplateStyle.BOTANICAL -> DateSlot(r(0.08, 0.955, 0.84, 0.035), fontSize * 0.5, GRAY, TextAlign.CENTER)
        TemplateStyle.CELEBRATION -> DateSlot(r(0.07, 0.07, 0.86, 0.05), fontSize * 0.55, accent, TextAlign.CENTER)
        TemplateStyle.EDITORIAL -> DateSlot(r(0.065, 0.95, 0.40, 0.025), fontSize * 0.42, accent, TextAlign.LEFT)
        TemplateStyle.BORDERLESS -> DateSlot(r(0.40, 0.90, 0.56, 0.06), card.height * 0.045, STAMP, TextAlign.RIGHT)
        else -> null
    }
}
```

> En `BORDERLESS` el `when` principal no agrega textos, pero la fecha sí aparece como fechador naranja sobre la foto. `textRoles` de BORDERLESS devuelve `[DATE]`, lo que concuerda.

- [ ] **Step 3: Implementar `data/BitmapMath.kt`**

```kotlin
package com.polar.app.data

/** Cálculos de decodificación sin Android, para poder probarlos. Valores EXIF estándar 1–8. */
object BitmapMath {
    fun sampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        val longSide = maxOf(width, height)
        while (longSide / sample > maxDim) sample *= 2
        return sample
    }

    fun isQuarterTurned(exifOrientation: Int): Boolean = exifOrientation in setOf(5, 6, 7, 8)

    fun orientedSize(width: Int, height: Int, exifOrientation: Int): Pair<Int, Int> =
        if (isQuarterTurned(exifOrientation)) height to width else width to height
}
```

- [ ] **Step 4: Implementar `data/BitmapLoader.kt`**

```kotlin
package com.polar.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class PhotoInfo(val width: Int, val height: Int, val takenAtEpochMs: Long?)

/** Decodifica fotos con muestreo y orientación EXIF; guarda en memoria las recientes. */
class BitmapLoader(private val context: Context) {
    companion object {
        const val PREVIEW_MAX = 1600
        const val EXPORT_MAX = 3600
    }

    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private fun open(path: String): InputStream? =
        if (path.startsWith("content:")) context.contentResolver.openInputStream(Uri.parse(path))
        else File(path).takeIf { it.exists() }?.inputStream()

    private fun orientation(path: String): Int = try {
        open(path)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: 1
    } catch (_: Exception) { 1 }

    fun load(path: String, maxDim: Int): Bitmap? {
        val key = "$path@$maxDim"
        cache.get(key)?.takeIf { !it.isRecycled }?.let { return it }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(path)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = BitmapMath.sampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = open(path)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
            val oriented = applyOrientation(raw, orientation(path))
            cache.put(key, oriented)
            oriented
        } catch (_: OutOfMemoryError) {
            cache.evictAll(); null
        } catch (_: Exception) { null }
    }

    /** Medidas ya orientadas y fecha de captura (EXIF `DateTimeOriginal`, si existe). */
    fun readInfo(uri: Uri): PhotoInfo? = try {
        val path = uri.toString()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(path)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
        else {
            var taken: Long? = null
            var exifOrientation = 1
            open(path)?.use { stream ->
                val exif = ExifInterface(stream)
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                val text = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                taken = text?.let { runCatching { SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ROOT).parse(it)?.time }.getOrNull() }
            }
            val (w, h) = BitmapMath.orientedSize(bounds.outWidth, bounds.outHeight, exifOrientation)
            PhotoInfo(w, h, taken)
        }
    } catch (_: Exception) { null }

    fun clear() = cache.evictAll()

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            2 -> m.setScale(-1f, 1f)
            3 -> m.setRotate(180f)
            4 -> m.setScale(1f, -1f)
            5 -> { m.setRotate(90f); m.postScale(-1f, 1f) }
            6 -> m.setRotate(90f)
            7 -> { m.setRotate(-90f); m.postScale(-1f, 1f) }
            8 -> m.setRotate(-90f)
            else -> return bitmap
        }
        val out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        if (out != bitmap) bitmap.recycle()
        return out
    }
}
```

- [ ] **Step 5: Cambiar `PolarRenderer.kt` para que use `CardTextLayout` y fuentes**

1. Agrega `fonts: FontProvider = SystemFontProvider` como último parámetro de `drawPage`. Agrega a `drawCard` y a `drawCardElements` el parámetro `cardIndex: Int` (justo después de `firstSlot`) y `fonts: FontProvider` (al final), con esos nombres exactos, porque la Task 12 llama a `drawCard` con argumentos con nombre. En `drawPage`, dentro del `for ((index, card) in cards.withIndex())`, calcula `val cardIndex = page * project.cardsPerPage + index` y pásalo junto con `fonts`. `drawImported` recibe `fonts` y lo ignora.
2. En `drawCardElements`, reemplaza la función local `drawUserText` y todas sus llamadas, junto con los `drawText(...)` fijos de TICKET y EDITORIAL y la llamada a `drawInstagram`, por este bucle al final de la función:

```kotlin
        for (item in CardTextLayout.items(project, card, cardIndex, photoSlots, accent)) {
            drawText(
                canvas = canvas, value = item.text, rect = item.rect, sizePt = item.sizePt,
                defaultColor = item.defaultColor, defaultBold = item.defaultBold,
                align = when (item.align) { TextAlign.LEFT -> Paint.Align.LEFT; TextAlign.CENTER -> Paint.Align.CENTER; TextAlign.RIGHT -> Paint.Align.RIGHT },
                appearance = item.appearance, scale = scale, fonts = fonts
            )
        }
```

   Conserva en el `when (style)` sólo lo que no es texto: `drawPlayer` y QR (SPOTIFY, PLAYER_RED, PLAYER_GRAY), `drawTicketNotches`, `drawPerforations`, `drawCalendar`, la línea de POSTCARD y el contorno de HEART. Borra la función `drawInstagram`.
3. Reemplaza `drawText` completa por:

```kotlin
    private fun drawText(
        canvas: Canvas,
        value: String,
        rect: PolarRect,
        sizePt: Double,
        defaultColor: Int = Color.BLACK,
        defaultBold: Boolean = false,
        align: Paint.Align = Paint.Align.CENTER,
        appearance: TextAppearance? = null,
        scale: Float,
        fonts: FontProvider = SystemFontProvider
    ) {
        if (value.isBlank()) return
        if (appearance != null && !appearance.visible) return
        val target = if (appearance != null) rect.offsetBy(appearance.offsetX, appearance.offsetY) else rect
        val color = if (appearance != null && appearance.hex.isNotBlank()) parseColor(appearance.hex) else defaultColor
        val bold = defaultBold || appearance?.bold == true
        val italic = appearance?.italic == true
        val finalAlign = when (appearance?.alignment) {
            TextAlignment.LEFT -> Paint.Align.LEFT
            TextAlignment.CENTER -> Paint.Align.CENTER
            TextAlignment.RIGHT -> Paint.Align.RIGHT
            else -> align
        }
        var pointSize = if (appearance != null && appearance.size > 0) appearance.size else max(6.0, sizePt)
        val paint = TextPaint().apply {
            this.color = color
            isAntiAlias = true
            typeface = fonts.typeface(appearance?.fontName ?: FontNames.SYSTEM, bold, italic)
        }
        for (i in 0 until 16) {
            paint.textSize = (pointSize * scale).toFloat()
            val fm = paint.fontMetrics
            if ((paint.measureText(value) <= target.width * scale && fm.descent - fm.ascent <= target.height * scale) || pointSize <= 6.0) break
            pointSize = max(6.0, pointSize * 0.88)
        }
        val width = paint.measureText(value)
        val fm = paint.fontMetrics
        val x = when (finalAlign) {
            Paint.Align.LEFT -> (target.left * scale).toFloat()
            Paint.Align.RIGHT -> (target.right * scale).toFloat() - width
            else -> (target.midX * scale).toFloat() - width / 2f
        }
        val y = (target.midY * scale).toFloat() - (fm.descent + fm.ascent) / 2f
        canvas.save()
        canvas.clipRect(target.toAndroidRectF(scale))
        canvas.drawText(value, x, y, paint)
        canvas.restore()
    }
```

   Al inicio del archivo, junto a `PolarRect`, agrega:

```kotlin
object FontNames { const val SYSTEM = ".System" }
```

4. En `drawCalendar` deja las llamadas a `drawText` como están: los valores por defecto de `fonts` y `appearance` las mantienen válidas.

- [ ] **Step 6: Correr todas las pruebas y compilar**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: PASS (incluye `PolarRendererTest`, que ya existía) y `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add PolarAndroid/app/src/main/java/com/polar/app/engine PolarAndroid/app/src/main/java/com/polar/app/data/BitmapMath.kt PolarAndroid/app/src/main/java/com/polar/app/data/BitmapLoader.kt PolarAndroid/app/src/test/java/com/polar/app/engine/CardTextLayoutTest.kt PolarAndroid/app/src/test/java/com/polar/app/data/BitmapMathTest.kt
git commit -m "feat(engine): texto por tarjeta, fuentes, fecha y orientación EXIF"
```

---

### Task 8: Importador de plantillas con errores claros

**Files:**
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/template/TemplateImporter.kt` (sólo `loadTemplate`, los imports y el tipo de retorno)
- Modify: `PolarAndroid/app/src/test/java/com/polar/app/template/TemplateImporterTest.kt`

**Interfaces:**
- Produces: `data class TemplateLoad(val template: ImportedTemplate, val detectedCount: Int)` y `TemplateImporter.loadTemplate(file: File): TemplateLoad`, que lanza `PolarException` con mensajes en español. `findRegionsFromPixels(...)` no cambia de firma.

- [ ] **Step 1: Agregar la prueba que falla**

Añade a `TemplateImporterTest`:

```kotlin
    @Test
    fun regionIdsAreMacCompatibleUuids() {
        val w = 40; val h = 40
        val pixels = IntArray(w * h) { 0xFF333333.toInt() }
        for (y in 10 until 30) for (x in 10 until 30) pixels[y * w + x] = 0xFFFFFFFF.toInt()
        val regions = TemplateImporter.findRegionsFromPixels(pixels, w, h)
        assertTrue(regions.all { Regex("^[0-9A-F-]{36}$").matches(it.id) })
    }
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.template.*' --console=plain`
Expected: PASS si todas las regiones se crean con el `id` por defecto, porque ya usa `newId()` desde la Task 2. Si algún `TemplateRegion(...)` dentro de `scanRegions` pasa `id = UUID.randomUUID().toString()`, la prueba FALLA: quita ese argumento.

- [ ] **Step 2: Cambiar `loadTemplate`**

- Reemplaza `import android.media.ExifInterface` por `import androidx.exifinterface.media.ExifInterface`.
- Agrega la clase de resultado arriba del `object`:

```kotlin
data class TemplateLoad(val template: ImportedTemplate, val detectedCount: Int)
```

- Cambia la firma a `fun loadTemplate(file: File): TemplateLoad` y el final de la función a:

```kotlin
        return TemplateLoad(
            template = ImportedTemplate(path = file.absolutePath, pixelWidth = origWidth, pixelHeight = origHeight, regions = found),
            detectedCount = detectedCount
        )
```

- Cambia los mensajes de error por estos tres, exactos:
  - Archivo ilegible: `"No pudimos leer esa imagen. Prueba con un PNG o JPG."`
  - Dimensiones: `"La imagen es demasiado grande. Usa una de menos de 150 megapíxeles."`
  - No se pudo decodificar: `"No pudimos abrir esa imagen. Prueba con un PNG o JPG."`

- [ ] **Step 3: Correr las pruebas**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.template.*' --console=plain`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add PolarAndroid/app/src/main/java/com/polar/app/template PolarAndroid/app/src/test/java/com/polar/app/template
git commit -m "fix(template): ids compatibles con Mac y errores claros al importar"
```

---
### Task 9: Biblioteca en disco, importación de fotos y ajustes

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/data/ProjectStore.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/data/PhotoImporter.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/data/SettingsRepository.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/data/ProjectStoreTest.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/data/SettingsRepositoryTest.kt`

**Interfaces:**
- Consumes: `PolarJson`, `PolarProject`, `newId`, `PhotoInfo` y `BitmapLoader.readInfo` (Task 7).
- Produces:
  - `@Serializable data class ProjectMeta(id: String, name: String, style: TemplateStyle, sheets: Int, updatedAtEpochMs: Long)`.
  - `data class LoadedProject(project: PolarProject, missingPhotos: Int)`.
  - `class ProjectStore(root: File, clock: () -> Long = System::currentTimeMillis)`:
    - `list(): List<ProjectMeta>` (más recientes primero)
    - `create(project, name): String`
    - `load(id): LoadedProject`
    - `save(id, project, thumbnailPng: ByteArray? = null): ProjectMeta`
    - `importPhoto(id, source: InputStream, extension: String, info: PhotoInfo): PhotoAsset`
    - `importTemplateFile(id, source: InputStream, extension: String): File`
    - `importPolar(text: String, name: String): String`
    - `exportPolar(id): String`
    - `duplicate(id, newName): String`
    - `rename(id, name)`
    - `delete(id)` (va a la papelera)
    - `restore(id)`
    - `emptyTrash()`
    - `thumbnailFile(id): File?`
    - `projectDir(id): File`
  - `interface PhotoSource { suspend fun import(projectId: String, uris: List<String>): ImportResult }` con `data class ImportResult(assets: List<PhotoAsset>, failed: Int)`, y la implementación `class PhotoImporter(context, store, loader): PhotoSource`.
  - `enum ThemeMode { SYSTEM, LIGHT, DARK }`, `enum Units { MM, INCHES }`.
  - `data class AppSettings(theme, units, defaultPaper: PaperSize, onboardingSeen: Boolean)`.
  - `class SettingsRepository(context) { val settings: Flow<AppSettings>; suspend fun setTheme/setUnits/setDefaultPaper/setOnboardingSeen }`.

- [ ] **Step 1: Pruebas que fallan**

`ProjectStoreTest.kt`:

```kotlin
package com.polar.app.data

import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1_000L
    private fun store() = ProjectStore(tmp.root) { now }
    private val info = PhotoInfo(800, 600, 1_771_070_400_000)

    @Test
    fun createSaveLoadKeepsPhotosRelativeOnDisk() {
        val s = store()
        val id = s.create(PolarProject().normalized(), "Boda")
        val asset = s.importPhoto(id, "jpegbytes".byteInputStream(), "jpg", info)
        assertTrue(File(asset.path).exists())
        val p = PolarProject(photos = listOf(asset), placements = listOf(PhotoPlacement(asset.id))).normalized()
        s.save(id, p)
        val onDisk = File(s.projectDir(id), "project.polar").readText()
        assertTrue(onDisk.contains("\"photos/${asset.id}.jpg\""))
        val loaded = s.load(id)
        assertEquals(0, loaded.missingPhotos)
        assertEquals(asset.path, loaded.project.photos[0].path)
        assertEquals("Boda", loaded.project.name)
        assertEquals(1_771_070_400_000, loaded.project.photos[0].takenAtEpochMs)
    }

    @Test
    fun listIsNewestFirstWithMeta() {
        val s = store()
        val a = s.create(PolarProject(), "A"); now = 2_000
        val b = s.create(PolarProject(settings = PrintSettings(style = TemplateStyle.CALENDAR)), "B")
        val list = s.list()
        assertEquals(listOf(b, a), list.map { it.id })
        assertEquals(TemplateStyle.CALENDAR, list[0].style)
        assertEquals("B", list[0].name)
    }

    @Test
    fun duplicateCopiesPhotosAndRenames() {
        val s = store()
        val id = s.create(PolarProject(), "Pedido Ana")
        val asset = s.importPhoto(id, "x".byteInputStream(), "png", info)
        s.save(id, PolarProject(photos = listOf(asset), placements = listOf(PhotoPlacement(asset.id))))
        val copy = s.duplicate(id, "Pedido Ana (copia)")
        val loaded = s.load(copy)
        assertEquals("Pedido Ana (copia)", loaded.project.name)
        assertTrue(loaded.project.photos[0].path.startsWith(s.projectDir(copy).absolutePath))
        assertEquals(0, loaded.missingPhotos)
    }

    @Test
    fun deleteGoesToTrashAndCanBeRestored() {
        val s = store()
        val id = s.create(PolarProject(), "Borrar")
        s.delete(id)
        assertTrue(s.list().isEmpty())
        s.restore(id)
        assertEquals(listOf(id), s.list().map { it.id })
        s.delete(id); s.emptyTrash()
        s.restore(id)
        assertTrue(s.list().isEmpty())
    }

    @Test
    fun openMacProjectWithMissingPhotos() {
        val s = store()
        val text = javaClass.classLoader!!.getResource("fixtures/mac_polaroid.polar")!!.readText()
        val id = s.importPolar(text, "Mi primer diseño")
        val loaded = s.load(id)
        assertEquals(loaded.project.photos.size, loaded.missingPhotos)
        assertTrue(loaded.project.photos.isNotEmpty())
        assertEquals("Mi primer diseño", loaded.project.name)
    }

    @Test
    fun exportPolarIsMacReadable() {
        val s = store()
        val id = s.create(PolarProject().normalized(), "Exportar")
        com.polar.app.model.MacCompat.assertReadable(s.exportPolar(id))
    }

    @Test
    fun renameUpdatesMeta() {
        val s = store()
        val id = s.create(PolarProject(), "Viejo")
        s.rename(id, "Nuevo")
        assertEquals("Nuevo", s.list().single().name)
        assertEquals("Nuevo", s.load(id).project.name)
    }

    @Test
    fun corruptProjectIsSkippedInList() {
        val s = store()
        s.create(PolarProject(), "Bien")
        File(tmp.root, "projects/ROTO").mkdirs()
        File(tmp.root, "projects/ROTO/project.polar").writeText("{")
        assertEquals(listOf("Bien"), s.list().map { it.name })
    }
}
```

`SettingsRepositoryTest.kt`:

```kotlin
package com.polar.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.model.PaperSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsRepositoryTest {
    @Test
    fun savesAndReadsSettings() = runTest {
        val repo = SettingsRepository(ApplicationProvider.getApplicationContext())
        assertEquals(AppSettings(), repo.settings.first())
        repo.setTheme(ThemeMode.DARK); repo.setUnits(Units.INCHES)
        repo.setDefaultPaper(PaperSize.A4); repo.setOnboardingSeen(true)
        assertEquals(AppSettings(ThemeMode.DARK, Units.INCHES, PaperSize.A4, true), repo.settings.first())
    }
}
```

Agrega `testImplementation(libs.androidx.test.ext.junit)` en `app/build.gradle.kts` (la Task 1 ya lo tiene en `androidTestImplementation`).

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.data.*' --console=plain`
Expected: FAIL (clases inexistentes).

- [ ] **Step 2: Implementar `ProjectStore.kt`**

```kotlin
package com.polar.app.data

import com.polar.app.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

@Serializable
data class ProjectMeta(
    val id: String,
    val name: String,
    val style: TemplateStyle,
    val sheets: Int,
    val updatedAtEpochMs: Long
)

data class LoadedProject(val project: PolarProject, val missingPhotos: Int)

/**
 * Cada proyecto vive en `projects/<id>/` con project.polar (rutas relativas), photos/, thumb.png y meta.json.
 * Borrar mueve la carpeta a `trash/` para poder deshacer.
 */
class ProjectStore(private val root: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val projects = File(root, "projects").apply { mkdirs() }
    private val trash = File(root, "trash").apply { mkdirs() }
    private val metaJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun projectDir(id: String): File = File(projects, id)
    fun thumbnailFile(id: String): File? = File(projectDir(id), "thumb.png").takeIf { it.exists() }

    fun list(): List<ProjectMeta> =
        (projects.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { dir -> runCatching { readMeta(dir.name) }.getOrNull() }
            .sortedByDescending { it.updatedAtEpochMs }

    fun create(project: PolarProject, name: String): String {
        val id = newId()
        File(projectDir(id), "photos").mkdirs()
        save(id, project.copy(name = name))
        return id
    }

    fun load(id: String): LoadedProject {
        val dir = projectDir(id)
        val stored = PolarJson.decode(File(dir, "project.polar").readText())
        val photos = stored.photos.map { it.copy(path = absolute(dir, it.path)) }
        val template = stored.settings.importedTemplate?.let { it.copy(path = absolute(dir, it.path)) }
        val project = stored.copy(photos = photos, settings = stored.settings.copy(importedTemplate = template))
        val missing = photos.count { !it.path.startsWith("content:") && !File(it.path).exists() }
        return LoadedProject(project, missing)
    }

    fun save(id: String, project: PolarProject, thumbnailPng: ByteArray? = null): ProjectMeta {
        val dir = projectDir(id).apply { mkdirs() }
        val now = clock()
        val stored = project.copy(
            updatedAtEpochMs = now,
            photos = project.photos.map { it.copy(path = relative(dir, it.path)) },
            settings = project.settings.copy(importedTemplate = project.settings.importedTemplate?.let { it.copy(path = relative(dir, it.path)) })
        )
        writeAtomic(File(dir, "project.polar"), PolarJson.encode(stored))
        val meta = ProjectMeta(id, project.name, project.settings.style, project.pageCount, now)
        writeAtomic(File(dir, "meta.json"), metaJson.encodeToString(ProjectMeta.serializer(), meta))
        thumbnailPng?.let { File(dir, "thumb.png").writeBytes(it) }
        return meta
    }

    fun importPhoto(id: String, source: InputStream, extension: String, info: PhotoInfo): PhotoAsset {
        val assetId = newId()
        val file = File(File(projectDir(id), "photos").apply { mkdirs() }, "$assetId.${extension.lowercase()}")
        source.use { input -> file.outputStream().use { input.copyTo(it) } }
        return PhotoAsset(id = assetId, path = file.absolutePath, pixelWidth = info.width, pixelHeight = info.height, takenAtEpochMs = info.takenAtEpochMs)
    }

    fun importTemplateFile(id: String, source: InputStream, extension: String): File {
        val file = File(projectDir(id), "template-${newId()}.${extension.lowercase()}")
        source.use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    fun importPolar(text: String, name: String): String {
        val project = PolarJson.decode(text)
        return create(project, name)
    }

    fun exportPolar(id: String): String = File(projectDir(id), "project.polar").readText()

    fun duplicate(id: String, newName: String): String {
        val copyId = newId()
        projectDir(id).copyRecursively(projectDir(copyId))
        val loaded = load(copyId) // rutas relativas → apuntan a la copia
        save(copyId, loaded.project.copy(name = newName))
        return copyId
    }

    fun rename(id: String, name: String) {
        save(id, load(id).project.copy(name = name))
    }

    fun delete(id: String) {
        val target = File(trash, id)
        target.deleteRecursively()
        projectDir(id).renameTo(target)
    }

    fun restore(id: String) {
        val source = File(trash, id)
        if (source.exists()) source.renameTo(projectDir(id))
    }

    fun emptyTrash() {
        trash.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun readMeta(id: String): ProjectMeta {
        val file = File(projectDir(id), "meta.json")
        if (file.exists()) return metaJson.decodeFromString(ProjectMeta.serializer(), file.readText())
        val p = load(id).project
        return ProjectMeta(id, p.name, p.settings.style, p.pageCount, p.updatedAtEpochMs)
    }

    private fun relative(dir: File, path: String): String {
        val prefix = dir.absolutePath + File.separator
        return if (path.startsWith(prefix)) path.removePrefix(prefix) else path
    }

    private fun absolute(dir: File, path: String): String =
        if (path.startsWith("/") || path.startsWith("content:")) path else File(dir, path).absolutePath

    private fun writeAtomic(file: File, text: String) {
        val tmp = File(file.parentFile, ".${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() }
    }
}
```

> `corruptProjectIsSkippedInList`: la carpeta `ROTO` no tiene `meta.json` y su `project.polar` es inválido, así que `readMeta` lanza una excepción y `runCatching` la omite.

- [ ] **Step 3: Implementar `PhotoImporter.kt`**

```kotlin
package com.polar.app.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.polar.app.model.PhotoAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(val assets: List<PhotoAsset>, val failed: Int)

interface PhotoSource {
    suspend fun import(projectId: String, uris: List<String>): ImportResult
}

/** Copia cada foto elegida al proyecto; los originales del usuario no se tocan. */
class PhotoImporter(
    private val context: Context,
    private val store: ProjectStore,
    private val loader: BitmapLoader
) : PhotoSource {
    override suspend fun import(projectId: String, uris: List<String>): ImportResult = withContext(Dispatchers.IO) {
        val assets = mutableListOf<PhotoAsset>()
        var failed = 0
        for (text in uris) {
            val uri = Uri.parse(text)
            val info = loader.readInfo(uri)
            val stream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            if (info == null || stream == null) { failed++; stream?.close(); continue }
            val mime = context.contentResolver.getType(uri)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            assets += store.importPhoto(projectId, stream, ext, info)
        }
        ImportResult(assets, failed)
    }
}
```

- [ ] **Step 4: Implementar `SettingsRepository.kt`**

```kotlin
package com.polar.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.polar.app.model.PaperSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Units { MM, INCHES }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val units: Units = Units.MM,
    val defaultPaper: PaperSize = PaperSize.LETTER,
    val onboardingSeen: Boolean = false
)

private val Context.polarDataStore by preferencesDataStore(name = "polar_settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val unitsKey = stringPreferencesKey("units")
    private val paperKey = stringPreferencesKey("default_paper")
    private val onboardingKey = booleanPreferencesKey("onboarding_seen")

    val settings: Flow<AppSettings> = context.polarDataStore.data.map { p ->
        AppSettings(
            theme = p[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            units = p[unitsKey]?.let { runCatching { Units.valueOf(it) }.getOrNull() } ?: Units.MM,
            defaultPaper = p[paperKey]?.let { runCatching { PaperSize.valueOf(it) }.getOrNull() } ?: PaperSize.LETTER,
            onboardingSeen = p[onboardingKey] ?: false
        )
    }

    suspend fun setTheme(mode: ThemeMode) { context.polarDataStore.edit { it[themeKey] = mode.name } }
    suspend fun setUnits(units: Units) { context.polarDataStore.edit { it[unitsKey] = units.name } }
    suspend fun setDefaultPaper(paper: PaperSize) { context.polarDataStore.edit { it[paperKey] = paper.name } }
    suspend fun setOnboardingSeen(seen: Boolean) { context.polarDataStore.edit { it[onboardingKey] = seen } }
}
```

- [ ] **Step 5: Correr las pruebas**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.data.*' --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add PolarAndroid/app/build.gradle.kts PolarAndroid/app/src/main/java/com/polar/app/data PolarAndroid/app/src/test/java/com/polar/app/data
git commit -m "feat(data): biblioteca con autoguardado en disco, papelera e importación de fotos"
```

---

### Task 10: Tema, contenedor de la app, navegación, bienvenida y ajustes

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/PolarApplication.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/MainActivity.kt` (reemplazo)
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/theme/{Color,Type,Shape,Theme}.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/LocalLayout.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/Motion.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/navigation/Routes.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/navigation/PolarNavHost.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/components/PolaroidStack.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/onboarding/OnboardingScreen.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/settings/SettingsScreen.kt`
- Modify: `PolarAndroid/app/src/main/AndroidManifest.xml`
- Modify: `PolarAndroid/app/src/main/res/values/themes.xml`; Create: `res/values-night/themes.xml`
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`, `res/values/colors.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/OnboardingScreenTest.kt`

**Interfaces:**
- Consumes: `ProjectStore`, `BitmapLoader`, `AndroidFontProvider`, `PhotoImporter`, `SettingsRepository` (Tasks 6, 7, 9).
- Produces:
  - `class AppContainer(context)` con `store`, `bitmaps`, `fonts`, `photos`, `settings`, y `class PolarApplication : Application { val container }`. Las Tasks 12 y 20 agregan `thumbnails` y `exports` al contenedor.
  - `@Composable fun PolarTheme(themeMode: ThemeMode, content)`.
  - `object PolarColors` (`table`, `warningContainer`, `onWarningContainer`, `successContainer`, `onSuccessContainer`) como `@Composable get()`.
  - `val GelasioFamily: FontFamily`, `val CaveatFamily: FontFamily`.
  - `enum LayoutKind { COMPACT, EXPANDED }`, `val LocalLayout`, `@Composable fun ProvideLayout(content)`.
  - `@Composable fun rememberReduceMotion(): Boolean`.
  - Rutas `OnboardingRoute`, `HomeRoute`, `CatalogRoute`, `EditorRoute(projectId)`, `SettingsRoute`.
  - `@Composable fun PolarNavHost(container, startOnboarding: Boolean)`. Las Tasks 11, 12 y 14 conectan Home, Catálogo y Editor; aquí quedan pantallas provisionales.
  - `@Composable fun PolaroidStack(modifier, caption: String)`.
  - `@Composable fun OnboardingScreen(onDone: () -> Unit)`.
  - `@Composable fun SettingsScreen(settings: AppSettings, onTheme, onUnits, onPaper, onShowOnboarding, onBack)`.

- [ ] **Step 1: Prueba de interfaz que falla**

`OnboardingScreenTest.kt`:

```kotlin
package com.polar.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.ui.onboarding.OnboardingScreen
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun threeStepsThenDone() {
        var done = false
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { OnboardingScreen(onDone = { done = true }) } }
        compose.onNodeWithText("Elige un diseño").assertExists()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Pon tus fotos").assertExists()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Empezar").performClick()
        assertTrue(done)
    }

    @Test
    fun skipFinishesImmediately() {
        var done = false
        compose.setContent { PolarTheme(ThemeMode.DARK) { OnboardingScreen(onDone = { done = true }) } }
        compose.onNodeWithText("Saltar").performClick()
        assertTrue(done)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.OnboardingScreenTest' --console=plain`
Expected: FAIL (no existen `PolarTheme`/`OnboardingScreen`).

- [ ] **Step 2: Textos (`res/values/strings.xml`, reemplazo completo)**

```xml
<resources>
    <string name="app_name">Polar</string>
    <string name="tagline">Tu pequeño estudio de recuerdos</string>
    <string name="action_skip">Saltar</string>
    <string name="action_next">Siguiente</string>
    <string name="action_start">Empezar</string>
    <string name="action_back">Atrás</string>
    <string name="action_close">Cerrar</string>
    <string name="action_undo">Deshacer</string>
    <string name="action_redo">Rehacer</string>
    <string name="action_save">Guardar</string>
    <string name="action_cancel">Cancelar</string>
    <string name="action_ok">Entendido</string>
    <string name="onb_1_title">Elige un diseño</string>
    <string name="onb_1_body">Polaroid, canción con QR, boleto, película, calendario y más.</string>
    <string name="onb_2_title">Pon tus fotos</string>
    <string name="onb_2_body">Se acomodan solas. Toca cualquier tarjeta para cambiar su foto o su texto.</string>
    <string name="onb_3_title">Imprime o comparte</string>
    <string name="onb_3_body">PDF y PNG a tamaño real, listos para tu impresora o tu papelería.</string>
    <string name="settings_title">Ajustes</string>
    <string name="settings_appearance">Apariencia</string>
    <string name="settings_theme_system">Sistema</string>
    <string name="settings_theme_light">Claro</string>
    <string name="settings_theme_dark">Oscuro</string>
    <string name="settings_units">Medidas</string>
    <string name="settings_units_mm">Milímetros</string>
    <string name="settings_units_in">Pulgadas</string>
    <string name="settings_default_paper">Papel para diseños nuevos</string>
    <string name="settings_show_onboarding">Ver la bienvenida otra vez</string>
    <string name="settings_how_print">Cómo imprimir a tamaño real</string>
    <string name="settings_how_print_body">En tu impresora elige el mismo papel que en Polar y la opción «Tamaño real» o escala 100 %. Así cada tarjeta sale de su medida y las marcas de corte quedan en su lugar.</string>
    <string name="settings_licenses">Licencias de las fuentes</string>
    <string name="settings_privacy_title">Tus fotos se quedan en tu teléfono</string>
    <string name="settings_privacy_body">Polar funciona sin internet, sin cuentas y sin anuncios. Nada se sube a ningún lado.</string>
    <string name="settings_version">Polar %1$s · Hecho para imprimir recuerdos</string>
</resources>
```

`res/values/colors.xml` (reemplazo):

```xml
<resources>
    <color name="polar_ink">#7A293B</color>
    <color name="polar_cream">#F7F2EB</color>
    <color name="polar_night">#1D1A19</color>
</resources>
```

- [ ] **Step 3: Tema (`ui/theme`)**

`Color.kt`:

```kotlin
package com.polar.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF7A293B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2DDE1), onPrimaryContainer = Color(0xFF4D1522),
    secondary = Color(0xFF92394A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2DDE1), onSecondaryContainer = Color(0xFF4D1522),
    background = Color(0xFFF7F2EB), onBackground = Color(0xFF2B2221),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF2B2221),
    surfaceVariant = Color(0xFFF1EAE2), onSurfaceVariant = Color(0xFF6B5D5A),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFBF7F2),
    surfaceContainer = Color(0xFFF1EAE2), surfaceContainerHigh = Color(0xFFEDE5DC), surfaceContainerHighest = Color(0xFFE8E0D6),
    outline = Color(0xFFB5A6A1), outlineVariant = Color(0xFFE4DAD0),
    inverseSurface = Color(0xFF362F2E), inverseOnSurface = Color(0xFFF8EEEC), inversePrimary = Color(0xFFFFB2BC),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF), scrim = Color(0xFF000000)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2BC), onPrimary = Color(0xFF561D2B),
    primaryContainer = Color(0xFF5E2231), onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = Color(0xFFFFB2BC), onSecondary = Color(0xFF561D2B),
    secondaryContainer = Color(0xFF5E2231), onSecondaryContainer = Color(0xFFFFD9DE),
    background = Color(0xFF1D1A19), onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF2A2524), onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF332D2C), onSurfaceVariant = Color(0xFFB9AAA7),
    surfaceContainerLowest = Color(0xFF181514), surfaceContainerLow = Color(0xFF241F1E),
    surfaceContainer = Color(0xFF2A2524), surfaceContainerHigh = Color(0xFF332D2C), surfaceContainerHighest = Color(0xFF3A3332),
    outline = Color(0xFF7D6D6A), outlineVariant = Color(0xFF3D3534),
    inverseSurface = Color(0xFFEDE0DD), inverseOnSurface = Color(0xFF362F2E), inversePrimary = Color(0xFF7A293B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), scrim = Color(0xFF000000)
)

@Immutable
data class PolarExtraColors(
    val table: Color,
    val warningContainer: Color, val onWarningContainer: Color,
    val successContainer: Color, val onSuccessContainer: Color
)

val LightExtra = PolarExtraColors(Color(0xFFE8E3DC), Color(0xFFFBEBD0), Color(0xFF7A5200), Color(0xFFE2EFE5), Color(0xFF2F6B45))
val DarkExtra = PolarExtraColors(Color(0xFF141211), Color(0xFF3A2D12), Color(0xFFF2C46B), Color(0xFF1F3427), Color(0xFF8FD3A3))

val LocalPolarColors = staticCompositionLocalOf { LightExtra }

object PolarColors {
    val table: Color @Composable get() = LocalPolarColors.current.table
    val warningContainer: Color @Composable get() = LocalPolarColors.current.warningContainer
    val onWarningContainer: Color @Composable get() = LocalPolarColors.current.onWarningContainer
    val successContainer: Color @Composable get() = LocalPolarColors.current.successContainer
    val onSuccessContainer: Color @Composable get() = LocalPolarColors.current.onSuccessContainer
}
```

`Type.kt`:

```kotlin
package com.polar.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.polar.app.R

val GelasioFamily = FontFamily(Font(R.font.gelasio, FontWeight.Normal), Font(R.font.gelasio, FontWeight.Medium))
val CaveatFamily = FontFamily(Font(R.font.caveat))

private val base = Typography()

val PolarTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    headlineLarge = base.headlineLarge.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    headlineMedium = base.headlineMedium.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    headlineSmall = base.headlineSmall.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall,
    bodyLarge = base.bodyLarge, bodyMedium = base.bodyMedium, bodySmall = base.bodySmall,
    labelLarge = base.labelLarge, labelMedium = base.labelMedium, labelSmall = base.labelSmall
)

/** Marca grande "Polar" del inicio. */
val BrandStyle = TextStyle(fontFamily = GelasioFamily, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp)
/** Nombre escrito a mano bajo cada polaroid. */
val HandwrittenStyle = TextStyle(fontFamily = CaveatFamily, fontSize = 21.sp, lineHeight = 24.sp)
```

`Shape.kt`:

```kotlin
package com.polar.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PolarShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
```

`Theme.kt`:

```kotlin
package com.polar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.polar.app.data.ThemeMode

@Composable
fun PolarTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalPolarColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = PolarTypography,
            shapes = PolarShapes,
            content = content
        )
    }
}
```

- [ ] **Step 4: Tamaño de ventana y movimiento**

`ui/LocalLayout.kt`:

```kotlin
package com.polar.app.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

enum class LayoutKind { COMPACT, EXPANDED }

val LocalLayout = staticCompositionLocalOf { LayoutKind.COMPACT }

/** ≥ 840dp = tres paneles (barra lateral + hoja + panel fijo); si no, barra inferior y paneles a media altura. */
@Composable
fun ProvideLayout(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val kind = if (maxWidth >= 840.dp) LayoutKind.EXPANDED else LayoutKind.COMPACT
        CompositionLocalProvider(LocalLayout provides kind) { content() }
    }
}
```

`ui/Motion.kt`:

```kotlin
package com.polar.app.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** true cuando el usuario apagó las animaciones del sistema. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
```

- [ ] **Step 5: Contenedor de la app**

`PolarApplication.kt`:

```kotlin
package com.polar.app

import android.app.Application
import android.content.Context
import com.polar.app.data.AndroidFontProvider
import com.polar.app.data.BitmapLoader
import com.polar.app.data.PhotoImporter
import com.polar.app.data.ProjectStore
import com.polar.app.data.SettingsRepository

class AppContainer(context: Context) {
    val store = ProjectStore(context.filesDir)
    val bitmaps = BitmapLoader(context)
    val fonts = AndroidFontProvider(context)
    val photos = PhotoImporter(context, store, bitmaps)
    val settings = SettingsRepository(context)
}

class PolarApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.store.emptyTrash() // lo borrado en la sesión anterior ya no se puede deshacer
    }
}
```

- [ ] **Step 6: Ilustración y bienvenida**

`ui/components/PolaroidStack.kt`:

```kotlin
package com.polar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polar.app.ui.theme.HandwrittenStyle

/** Tres polaroids encimadas; decorativa (se oculta al lector de pantalla). Colores de "fotos" fijos: van sobre papel. */
@Composable
fun PolaroidStack(caption: String, modifier: Modifier = Modifier, photo: Color = Color(0xFFC9A99A)) {
    Box(modifier.size(250.dp, 240.dp).clearAndSetSemantics { }) {
        Polaroid(Modifier.offset(10.dp, 28.dp).rotate(-9f), Color(0xFF9FB3A3), null)
        Polaroid(Modifier.offset(94.dp, 6.dp).rotate(7f), Color(0xFFA9B7C9), null)
        Polaroid(Modifier.offset(48.dp, 40.dp).rotate(-1.5f), photo, caption)
    }
}

@Composable
private fun Polaroid(modifier: Modifier, photo: Color, caption: String?) {
    Column(
        modifier.size(156.dp, 192.dp).shadow(10.dp).background(Color.White).padding(start = 10.dp, end = 10.dp, top = 10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(136.dp).background(photo))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (caption != null) Text(caption, style = HandwrittenStyle, color = Color(0xFF7A293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
```

`ui/onboarding/OnboardingScreen.kt`:

```kotlin
package com.polar.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.ui.components.PolaroidStack

private data class Step(val title: Int, val body: Int, val caption: String, val photo: Color)

private val steps = listOf(
    Step(R.string.onb_1_title, R.string.onb_1_body, "Polaroid", Color(0xFFC9A99A)),
    Step(R.string.onb_2_title, R.string.onb_2_body, "Lu y Max", Color(0xFF9FB3A3)),
    Step(R.string.onb_3_title, R.string.onb_3_body, "¡Listo!", Color(0xFFA9B7C9))
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().widthIn(max = 460.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone) { Text(stringResource(R.string.action_skip)) }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(targetState = index, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onboarding") { i ->
                val s = steps[i]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PolaroidStack(caption = s.caption, photo = s.photo)
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(s.title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(s.body), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 420.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clearAndSetSemantics { }) {
                steps.indices.forEach { i ->
                    Box(
                        Modifier.height(8.dp).width(if (i == index) 22.dp else 8.dp).background(
                            if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape
                        )
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (index < steps.lastIndex) index++ else onDone() },
                modifier = Modifier.widthIn(min = 220.dp).height(52.dp)
            ) {
                Text(stringResource(if (index < steps.lastIndex) R.string.action_next else R.string.action_start))
            }
        }
    }
}
```

- [ ] **Step 7: Ajustes**

`ui/settings/SettingsScreen.kt`:

```kotlin
package com.polar.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.data.AppSettings
import com.polar.app.data.ThemeMode
import com.polar.app.data.Units
import com.polar.app.model.PaperSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onUnits: (Units) -> Unit,
    onPaper: (PaperSize) -> Unit,
    onShowOnboarding: () -> Unit,
    onBack: () -> Unit
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    val licenses = remember { runCatching { context.assets.list("licenses")?.sorted().orEmpty() }.getOrDefault(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp).widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Labeled(stringResource(R.string.settings_appearance)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(ThemeMode.SYSTEM to R.string.settings_theme_system, ThemeMode.LIGHT to R.string.settings_theme_light, ThemeMode.DARK to R.string.settings_theme_dark)
                    options.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(selected = settings.theme == mode, onClick = { onTheme(mode) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) { Text(stringResource(label)) }
                    }
                }
            }
            Labeled(stringResource(R.string.settings_units)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(Units.MM to R.string.settings_units_mm, Units.INCHES to R.string.settings_units_in)
                    options.forEachIndexed { i, (u, label) ->
                        SegmentedButton(selected = settings.units == u, onClick = { onUnits(u) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) { Text(stringResource(label)) }
                    }
                }
            }
            Labeled(stringResource(R.string.settings_default_paper)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PaperSize.LETTER, PaperSize.A4, PaperSize.OFICIO).forEach { p ->
                        FilterChip(selected = settings.defaultPaper == p, onClick = { onPaper(p) }, label = { Text(p.displayName) })
                    }
                }
            }
            OutlinedCard {
                RowItem(stringResource(R.string.settings_show_onboarding), onShowOnboarding)
                HorizontalDivider()
                RowItem(stringResource(R.string.settings_how_print)) { dialog = "print" }
                HorizontalDivider()
                RowItem(stringResource(R.string.settings_licenses)) { dialog = "licenses" }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                stringResource(R.string.settings_version, version), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    when (dialog) {
        "print" -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_ok)) } },
            title = { Text(stringResource(R.string.settings_how_print)) },
            text = { Text(stringResource(R.string.settings_how_print_body)) }
        )
        "licenses" -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_close)) } },
            title = { Text(stringResource(R.string.settings_licenses)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    licenses.forEach { name ->
                        Text(name.removeSuffix(".txt").replace('_', ' '), style = MaterialTheme.typography.titleSmall)
                        Text(
                            remember(name) { context.assets.open("licenses/$name").bufferedReader().use { it.readText() }.take(600) + "…" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        )
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun RowItem(text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        modifier = Modifier.heightIn(min = 56.dp).clickable(onClick = onClick)
    )
}
```

- [ ] **Step 8: Rutas y navegación**

`ui/navigation/Routes.kt`:

```kotlin
package com.polar.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object HomeRoute
@Serializable object CatalogRoute
@Serializable data class EditorRoute(val projectId: String)
@Serializable object SettingsRoute
```

`ui/navigation/PolarNavHost.kt` (las Tasks 11, 12 y 14 reemplazan los `Text` provisionales):

```kotlin
package com.polar.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.polar.app.AppContainer
import com.polar.app.data.AppSettings
import com.polar.app.ui.onboarding.OnboardingScreen
import com.polar.app.ui.rememberReduceMotion
import com.polar.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun PolarNavHost(container: AppContainer, startOnboarding: Boolean) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val reduce = rememberReduceMotion()
    val enter: EnterTransition = if (reduce) EnterTransition.None else fadeIn() + slideInHorizontally { it / 8 }
    val exit: ExitTransition = if (reduce) ExitTransition.None else fadeOut()

    NavHost(
        navController = nav,
        startDestination = if (startOnboarding) OnboardingRoute else HomeRoute,
        enterTransition = { enter }, exitTransition = { exit },
        popEnterTransition = { if (reduce) EnterTransition.None else fadeIn() }, popExitTransition = { exit }
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(onDone = {
                scope.launch { container.settings.setOnboardingSeen(true) }
                nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            })
        }
        composable<HomeRoute> { Text("Inicio") }
        composable<CatalogRoute> { Text("Catálogo") }
        composable<EditorRoute> { Text("Editor") }
        composable<SettingsRoute> {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            SettingsScreen(
                settings = settings,
                onTheme = { scope.launch { container.settings.setTheme(it) } },
                onUnits = { scope.launch { container.settings.setUnits(it) } },
                onPaper = { scope.launch { container.settings.setDefaultPaper(it) } },
                onShowOnboarding = { nav.navigate(OnboardingRoute) },
                onBack = { nav.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 9: `MainActivity`, temas XML y manifiesto**

`MainActivity.kt`:

```kotlin
package com.polar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.app.data.AppSettings
import com.polar.app.ui.ProvideLayout
import com.polar.app.ui.navigation.PolarNavHost
import com.polar.app.ui.theme.PolarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PolarApplication).container
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        setContent {
            val settings: AppSettings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settings ?: return@setContent
            ready = true
            PolarTheme(current.theme) {
                ProvideLayout { PolarNavHost(container, startOnboarding = !current.onboardingSeen) }
            }
        }
    }
}
```

`res/values/themes.xml` (reemplazo):

```xml
<resources>
    <style name="Theme.Polar" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@color/polar_cream</item>
    </style>
    <style name="Theme.Polar.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/polar_cream</item>
        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="postSplashScreenTheme">@style/Theme.Polar</item>
    </style>
</resources>
```

`res/values-night/themes.xml`:

```xml
<resources>
    <style name="Theme.Polar" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/polar_night</item>
    </style>
    <style name="Theme.Polar.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/polar_night</item>
        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="postSplashScreenTheme">@style/Theme.Polar</item>
    </style>
</resources>
```

`AndroidManifest.xml`: en `<application>` agrega `android:name=".PolarApplication"` y `android:enableOnBackInvokedCallback="true"`; cambia `android:allowBackup="true"` por `android:allowBackup="false"` (las fotos de los proyectos no se respaldan en la nube); en `<activity>` cambia `android:theme` a `@style/Theme.Polar.Starting`. Deja el `<provider>` como está.

- [ ] **Step 10: Correr las pruebas, compilar y probar en el emulador**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: PASS y `BUILD SUCCESSFUL`.

```bash
~/Library/Android/sdk/emulator/emulator -list-avds          # elige un AVD de teléfono
~/Library/Android/sdk/emulator/emulator -avd <AVD> -no-snapshot-save &
~/Library/Android/sdk/platform-tools/adb wait-for-device
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb shell am start -n io.github.maverickdev01.polar.debug/com.polar.app.MainActivity
```
Expected: aparece la pantalla de arranque crema, luego la bienvenida con las polaroids; "Empezar" lleva al texto provisional "Inicio".

- [ ] **Step 11: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(ui): tema Polar claro y oscuro, navegación, bienvenida y ajustes"
```

---
### Task 11: Inicio · Tus diseños

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/home/HomeViewModel.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/home/HomeScreen.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/Share.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/navigation/PolarNavHost.kt` (destino `HomeRoute`)
- Modify: `PolarAndroid/app/src/main/res/xml/file_paths.xml`, `res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/MainDispatcherRule.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `ProjectStore`, `ProjectMeta` (Task 9).
- Produces:
  - `enum SortMode { RECENT, NAME }`.
  - `data class HomeUiState(all, query, sort, loaded) { val visible: List<ProjectMeta> }`.
  - `sealed interface HomeEvent { Deleted(id, name); Message(text); SharePolar(file) }`.
  - `class HomeViewModel(store, cacheDir: File, io: CoroutineDispatcher = Dispatchers.IO)` con `state`, `events: Flow<HomeEvent>`, `refresh()`, `setQuery`, `setSort`, `rename(id, name)`, `duplicate(id)`, `delete(id)`, `undoDelete(id)` y `sharePolar(id)`.
  - `@Composable fun HomeScreen(vm, thumbnailFile: (String) -> File?, onOpen: (String) -> Unit, onNew: () -> Unit, onSettings: () -> Unit)`.
  - `object Share { fun file(context, file, mime, title) }`.
  - Prueba: `class MainDispatcherRule(dispatcher: TestDispatcher = StandardTestDispatcher())`.

- [ ] **Step 1: Pruebas que fallan**

`test/java/com/polar/app/MainDispatcherRule.kt`:

```kotlin
package com.polar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

`test/java/com/polar/app/ui/home/HomeViewModelTest.kt`:

```kotlin
package com.polar.app.ui.home

import com.polar.app.MainDispatcherRule
import com.polar.app.data.ProjectStore
import com.polar.app.model.PolarProject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun setup(): Pair<ProjectStore, HomeViewModel> {
        var t = 0L
        val store = ProjectStore(tmp.newFolder("files")) { ++t }
        store.create(PolarProject(), "Viaje a Oaxaca")
        store.create(PolarProject(), "Boda Lu y Max")
        store.create(PolarProject(), "Álbum de Rocky")
        return store to HomeViewModel(store, tmp.newFolder("cache"), main.dispatcher)
    }

    @Test
    fun listsNewestFirstAndSortsByNameInSpanish() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        assertEquals(listOf("Álbum de Rocky", "Boda Lu y Max", "Viaje a Oaxaca"), vm.state.value.visible.map { it.name })
        vm.setSort(SortMode.NAME)
        assertEquals("Álbum de Rocky", vm.state.value.visible.first().name)
    }

    @Test
    fun searchIgnoresCaseAndSpaces() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        vm.setQuery("  boda ")
        assertEquals(listOf("Boda Lu y Max"), vm.state.value.visible.map { it.name })
    }

    @Test
    fun deleteThenUndo() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        val target = vm.state.value.visible.first()
        vm.delete(target.id); advanceUntilIdle()
        val event = vm.events.first() as HomeEvent.Deleted
        assertEquals(target.name, event.name)
        assertEquals(2, vm.state.value.visible.size)
        vm.undoDelete(target.id); advanceUntilIdle()
        assertEquals(3, vm.state.value.visible.size)
    }

    @Test
    fun duplicateAddsCopy() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        val target = vm.state.value.visible.first { it.name == "Boda Lu y Max" }
        vm.duplicate(target.id); advanceUntilIdle()
        assertTrue(vm.state.value.visible.any { it.name == "Boda Lu y Max (copia)" })
    }

    @Test
    fun renameIgnoresBlank() = runTest(main.dispatcher) {
        val (_, vm) = setup()
        vm.refresh(); advanceUntilIdle()
        val id = vm.state.value.visible.first().id
        vm.rename(id, "   "); advanceUntilIdle()
        assertEquals("Álbum de Rocky", vm.state.value.visible.first { it.id == id }.name)
        vm.rename(id, "Rocky 2026"); advanceUntilIdle()
        assertEquals("Rocky 2026", vm.state.value.visible.first { it.id == id }.name)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.home.*' --console=plain`
Expected: FAIL (no existe `HomeViewModel`).

- [ ] **Step 2: Implementar `HomeViewModel.kt`**

```kotlin
package com.polar.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.app.data.ProjectMeta
import com.polar.app.data.ProjectStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Collator
import java.util.Locale

enum class SortMode { RECENT, NAME }

data class HomeUiState(
    val all: List<ProjectMeta> = emptyList(),
    val query: String = "",
    val sort: SortMode = SortMode.RECENT,
    val loaded: Boolean = false
) {
    val visible: List<ProjectMeta>
        get() {
            val q = query.trim()
            val filtered = if (q.isEmpty()) all else all.filter { it.name.contains(q, ignoreCase = true) }
            return if (sort == SortMode.NAME) filtered.sortedWith(compareBy(Collator.getInstance(Locale("es"))) { it.name }) else filtered
        }
}

sealed interface HomeEvent {
    data class Deleted(val id: String, val name: String) : HomeEvent
    data class Message(val text: String) : HomeEvent
    data class SharePolar(val file: File) : HomeEvent
}

class HomeViewModel(
    private val store: ProjectStore,
    private val cacheDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(io) { store.list() }
            _state.update { it.copy(all = list, loaded = true) }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun setSort(sort: SortMode) = _state.update { it.copy(sort = sort) }

    fun rename(id: String, name: String) {
        val clean = name.trim().take(80)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            withContext(io) { store.rename(id, clean) }
            refresh(); _events.send(HomeEvent.Message("Nombre cambiado"))
        }
    }

    fun duplicate(id: String) {
        val meta = _state.value.all.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            withContext(io) { store.duplicate(id, "${meta.name} (copia)") }
            refresh(); _events.send(HomeEvent.Message("Diseño duplicado"))
        }
    }

    fun delete(id: String) {
        val meta = _state.value.all.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            withContext(io) { store.delete(id) }
            refresh(); _events.send(HomeEvent.Deleted(id, meta.name))
        }
    }

    fun undoDelete(id: String) {
        viewModelScope.launch { withContext(io) { store.restore(id) }; refresh() }
    }

    fun sharePolar(id: String) {
        val meta = _state.value.all.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val file = withContext(io) {
                val dir = File(cacheDir, "exports").apply { mkdirs() }
                File(dir, meta.name.replace(Regex("[^\\p{L}\\p{N} _-]"), "").ifBlank { "Polar" } + ".polar")
                    .apply { writeText(store.exportPolar(id)) }
            }
            _events.send(HomeEvent.SharePolar(file))
        }
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.home.*' --console=plain`
Expected: PASS.

- [ ] **Step 3: Compartir (`ui/Share.kt`) y rutas del FileProvider**

`res/xml/file_paths.xml` (reemplazo):

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="exports" path="exports/" />
</paths>
```

`ui/Share.kt`:

```kotlin
package com.polar.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object Share {
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun file(context: Context, file: File, mime: String, title: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title))
    }

    fun open(context: Context, uri: Uri, mime: String) {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(view) }
    }
}
```

- [ ] **Step 4: Textos de Inicio (agregar a `strings.xml`)**

```xml
    <string name="home_search">Buscar en tus diseños</string>
    <string name="home_sort_recent">Recientes</string>
    <string name="home_sort_name">Por nombre</string>
    <string name="home_new">Nuevo diseño</string>
    <string name="home_empty_title">Crea tu primer diseño</string>
    <string name="home_empty_body">Elige un molde, pon tus fotos y queda listo para imprimir. Todo se guarda solo.</string>
    <string name="home_no_results">No hay diseños con «%1$s». Prueba con otra palabra.</string>
    <string name="home_count">%1$d diseños</string>
    <string name="home_options">Opciones de %1$s</string>
    <string name="home_open">Abrir</string>
    <string name="home_duplicate">Duplicar para otro pedido</string>
    <string name="home_rename">Cambiar nombre</string>
    <string name="home_share">Compartir archivo .polar</string>
    <string name="home_delete">Borrar</string>
    <string name="home_deleted">«%1$s» borrado</string>
    <string name="home_rename_label">Nombre del diseño</string>
    <plurals name="home_sheets"><item quantity="one">%1$d hoja</item><item quantity="other">%1$d hojas</item></plurals>
```

- [ ] **Step 5: `HomeScreen.kt`**

```kotlin
package com.polar.app.ui.home

import android.graphics.BitmapFactory
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.app.R
import com.polar.app.data.ProjectMeta
import com.polar.app.ui.Share
import com.polar.app.ui.components.PolaroidStack
import com.polar.app.ui.theme.BrandStyle
import com.polar.app.ui.theme.HandwrittenStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    thumbnailFile: (String) -> File?,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuFor by remember { mutableStateOf<ProjectMeta?>(null) }
    var renaming by remember { mutableStateOf<ProjectMeta?>(null) }
    val deletedLabel = stringResource(R.string.home_deleted, "%s")
    val undoLabel = stringResource(R.string.action_undo)

    LifecycleResumeEffect(Unit) { vm.refresh(); onPauseOrDispose { } }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is HomeEvent.Deleted -> scope.launch {
                    val r = snackbar.showSnackbar(deletedLabel.format(e.name), undoLabel, duration = SnackbarDuration.Long)
                    if (r == SnackbarResult.ActionPerformed) vm.undoDelete(e.id)
                }
                is HomeEvent.Message -> scope.launch { snackbar.showSnackbar(e.text) }
                is HomeEvent.SharePolar -> Share.file(context, e.file, "application/octet-stream", e.file.name)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.home_new)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val columns = when {
                maxWidth >= 1200.dp -> 5
                maxWidth >= 840.dp -> 4
                maxWidth >= 560.dp -> 3
                else -> 2
            }
            val side = if (maxWidth >= 840.dp) 32.dp else 20.dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = side, end = side, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Header(state, onSettings, vm::setQuery, vm::setSort)
                }
                if (state.loaded && state.all.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyLibrary(onNew) }
                } else if (state.loaded && state.visible.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.home_no_results, state.query.trim()),
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                        )
                    }
                }
                items(state.visible, key = { it.id }) { meta ->
                    ProjectPolaroid(meta, thumbnailFile(meta.id), onOpen = { onOpen(meta.id) }, onMenu = { menuFor = meta })
                }
            }
        }
    }

    menuFor?.let { meta ->
        ModalBottomSheet(onDismissRequest = { menuFor = null }) {
            Text(meta.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            MenuRow(Icons.Outlined.FolderOpen, stringResource(R.string.home_open)) { menuFor = null; onOpen(meta.id) }
            MenuRow(Icons.Outlined.ContentCopy, stringResource(R.string.home_duplicate)) { menuFor = null; vm.duplicate(meta.id) }
            MenuRow(Icons.Outlined.Edit, stringResource(R.string.home_rename)) { menuFor = null; renaming = meta }
            MenuRow(Icons.Outlined.Share, stringResource(R.string.home_share)) { menuFor = null; vm.sharePolar(meta.id) }
            MenuRow(Icons.Outlined.Delete, stringResource(R.string.home_delete), MaterialTheme.colorScheme.error) { menuFor = null; vm.delete(meta.id) }
            Spacer(Modifier.height(24.dp))
        }
    }

    renaming?.let { meta ->
        var text by remember(meta.id) { mutableStateOf(meta.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.home_rename)) },
            text = { OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.home_rename_label)) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.rename(meta.id, text); renaming = null }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun Header(state: HomeUiState, onSettings: () -> Unit, onQuery: (String) -> Unit, onSort: (SortMode) -> Unit) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = BrandStyle, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Tune, stringResource(R.string.settings_title)) }
        }
        if (state.all.isNotEmpty()) {
            OutlinedTextField(
                value = state.query, onValueChange = onQuery, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(stringResource(R.string.home_search)) },
                shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.sort == SortMode.RECENT, { onSort(SortMode.RECENT) }, { Text(stringResource(R.string.home_sort_recent)) })
                FilterChip(state.sort == SortMode.NAME, { onSort(SortMode.NAME) }, { Text(stringResource(R.string.home_sort_name)) })
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.home_count, state.visible.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProjectPolaroid(meta: ProjectMeta, thumb: File?, onOpen: () -> Unit, onMenu: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, meta.id, meta.updatedAtEpochMs) {
        value = withContext(Dispatchers.IO) { thumb?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() } }
    }
    val tilt = remember(meta.id) { ((meta.id.hashCode() % 5) - 2) * 0.8f }
    val context = LocalContext.current
    val sheets = pluralStringResource(R.plurals.home_sheets, meta.sheets, meta.sheets)
    val whenText = remember(meta.updatedAtEpochMs) { DateUtils.getRelativeTimeSpanString(meta.updatedAtEpochMs).toString() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.rotate(tilt).shadow(6.dp, RoundedCornerShape(3.dp)).background(Color.White, RoundedCornerShape(3.dp))
                .clickable(onClick = onOpen).semantics { contentDescription = context.getString(R.string.home_open) + " " + meta.name }
                .padding(start = 8.dp, end = 8.dp, top = 8.dp)
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFE8E3DC))) {
                image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Text(
                meta.name, style = HandwrittenStyle, color = Color(0xFF4D1522), maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(42.dp).wrapContentHeight()
            )
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(meta.style.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$sheets · $whenText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = onMenu) { Icon(Icons.Filled.MoreVert, stringResource(R.string.home_options, meta.name)) }
        }
    }
}

@Composable
private fun EmptyLibrary(onNew: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PolaroidStack(caption = stringResource(R.string.app_name))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onNew) { Text(stringResource(R.string.home_new)) }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = tint) },
        leadingContent = { Icon(icon, null, tint = tint) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

> El fondo y el texto de la polaroid son fijos (blanco y vino) a propósito: representan papel impreso y no cambian con el tema.

- [ ] **Step 6: Conectar en `PolarNavHost`**

Reemplaza `composable<HomeRoute> { Text("Inicio") }` por:

```kotlin
        composable<HomeRoute> {
            val context = LocalContext.current
            val vm: HomeViewModel = viewModel(factory = viewModelFactory {
                initializer { HomeViewModel(container.store, context.cacheDir) }
            })
            HomeScreen(
                vm = vm,
                thumbnailFile = container.store::thumbnailFile,
                onOpen = { nav.navigate(EditorRoute(it)) },
                onNew = { nav.navigate(CatalogRoute) },
                onSettings = { nav.navigate(SettingsRoute) }
            )
        }
```

Imports nuevos: `androidx.compose.ui.platform.LocalContext`, `androidx.lifecycle.viewmodel.compose.viewModel`, `androidx.lifecycle.viewmodel.initializer`, `androidx.lifecycle.viewmodel.viewModelFactory`, `com.polar.app.ui.home.*`.

- [ ] **Step 7: Correr pruebas, compilar y revisar en el emulador**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`, instala el APK como en la Task 10 y abre la app.
Expected: tras la bienvenida aparece "Polar", la frase y el estado vacío con polaroids y el botón "Nuevo diseño". El botón lleva todavía al texto provisional "Catálogo".

- [ ] **Step 8: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(home): biblioteca Tus diseños con búsqueda, duplicar, renombrar y borrar con deshacer"
```

---

### Task 12: Catálogo con miniaturas reales, importar plantilla y abrir `.polar`

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/engine/Thumbnailer.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/engine/PolarRenderer.kt` (agregar `drawCardPreview`)
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/PolarApplication.kt` (agregar `thumbnails`)
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/catalog/CatalogViewModel.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/catalog/CatalogContent.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/catalog/CatalogScreen.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/navigation/{Routes,PolarNavHost}.kt`
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/catalog/CatalogViewModelTest.kt`

**Interfaces:**
- Consumes: `ProjectStore`, `ProjectEdits.selectStyle`, `TemplateImporter.loadTemplate`, `StyleInfo` (`category`, `description`), `FontProvider`.
- Produces:
  - `PolarRenderer.drawCardPreview(canvas, project, card: PolarRect, scale: Float, fonts: FontProvider)`.
  - `class Thumbnailer(fonts) { fun styleCard(style, widthPx): Bitmap; fun page(project, page, widthPx, bitmapProvider, template): Bitmap; fun projectPng(project, bitmapProvider, template): ByteArray }`.
  - `AppContainer.thumbnails`.
  - `sealed interface CatalogEvent { Created(id, notice: String?); Error(message) }`.
  - `class CatalogViewModel(store, readText: suspend (String) -> String, copyTemplate: suspend (projectId: String, uri: String) -> File, defaultPaper: suspend () -> PaperSize, io)` con `createFromStyle(style)`, `createFromTemplate(uri)` y `openPolar(uri, displayName)`.
  - `@Composable fun CatalogContent(title, current: TemplateStyle?, thumbnails, showImport: Boolean, onPick, onImportTemplate: (Uri) -> Unit, onOpenPolar: (Uri) -> Unit, onBack)`.
  - `EditorRoute(projectId: String, notice: String? = null)`.

- [ ] **Step 1: Pruebas que fallan**

```kotlin
package com.polar.app.ui.catalog

import com.polar.app.MainDispatcherRule
import com.polar.app.data.ProjectStore
import com.polar.app.model.PaperSize
import com.polar.app.model.TemplateStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CatalogViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun vm(store: ProjectStore, text: String = "") = CatalogViewModel(
        store = store, readText = { text }, copyTemplate = { _, _ -> File("x") },
        defaultPaper = { PaperSize.A4 }, io = main.dispatcher
    )

    @Test
    fun createFromStyleUsesDefaultPaperAndStyleGrid() = runTest(main.dispatcher) {
        val store = ProjectStore(tmp.root)
        val v = vm(store)
        v.createFromStyle(TemplateStyle.FILM_VERTICAL); advanceUntilIdle()
        val id = (v.events.first() as CatalogEvent.Created).id
        val p = store.load(id).project
        assertEquals(TemplateStyle.FILM_VERTICAL, p.settings.style)
        assertEquals(PaperSize.A4, p.settings.paperSize)
        assertEquals(2, p.settings.columns)
        assertEquals("Nuevo diseño", p.name)
    }

    @Test
    fun openMacPolarCreatesProjectAndReportsMissingPhotos() = runTest(main.dispatcher) {
        val store = ProjectStore(tmp.root)
        val text = javaClass.classLoader!!.getResource("fixtures/mac_polaroid.polar")!!.readText()
        val v = vm(store, text)
        v.openPolar("content://x", "Mi primer diseño.polar"); advanceUntilIdle()
        val e = v.events.first() as CatalogEvent.Created
        assertEquals("Mi primer diseño", store.load(e.id).project.name)
        assertTrue(e.notice!!.contains("fotos"))
    }

    @Test
    fun invalidPolarShowsError() = runTest(main.dispatcher) {
        val v = vm(ProjectStore(tmp.root), "no es un diseño")
        v.openPolar("content://x", "roto.polar"); advanceUntilIdle()
        assertTrue(v.events.first() is CatalogEvent.Error)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.catalog.*' --console=plain`
Expected: FAIL (no existe `CatalogViewModel`).

- [ ] **Step 2: Implementar `CatalogViewModel.kt`**

```kotlin
package com.polar.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.ProjectStore
import com.polar.app.model.*
import com.polar.app.template.TemplateImporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface CatalogEvent {
    data class Created(val id: String, val notice: String? = null) : CatalogEvent
    data class Error(val message: String) : CatalogEvent
}

class CatalogViewModel(
    private val store: ProjectStore,
    private val readText: suspend (String) -> String,
    private val copyTemplate: suspend (projectId: String, uri: String) -> File,
    private val defaultPaper: suspend () -> PaperSize,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _events = Channel<CatalogEvent>(Channel.BUFFERED)
    val events: Flow<CatalogEvent> = _events.receiveAsFlow()

    fun createFromStyle(style: TemplateStyle) {
        viewModelScope.launch {
            val id = withContext(io) {
                val base = PolarProject(settings = PrintSettings(paperSize = defaultPaper()))
                store.create(ProjectEdits.selectStyle(base, style), "Nuevo diseño")
            }
            _events.send(CatalogEvent.Created(id))
        }
    }

    fun createFromTemplate(uri: String) {
        viewModelScope.launch {
            var id: String? = null
            try {
                val (newId, detected) = withContext(io) {
                    val created = store.create(PolarProject(), "Mi plantilla")
                    id = created
                    val load = TemplateImporter.loadTemplate(copyTemplate(created, uri))
                    val project = PolarProject(
                        settings = PrintSettings(style = TemplateStyle.IMPORTED, columns = 1, rows = 1, paperSize = defaultPaper(), importedTemplate = load.template),
                        name = "Mi plantilla"
                    ).normalized()
                    store.save(created, project)
                    created to load.detectedCount
                }
                val notice = if (detected == 0) "No encontramos huecos: agregamos uno para que lo ajustes en Diseño."
                else "Encontramos $detected huecos. Puedes ajustarlos en Diseño."
                _events.send(CatalogEvent.Created(newId, notice))
            } catch (e: PolarException) {
                id?.let { withContext(io) { store.delete(it); store.emptyTrash() } }
                _events.send(CatalogEvent.Error(e.message ?: "No pudimos abrir esa imagen."))
            }
        }
    }

    fun openPolar(uri: String, displayName: String) {
        viewModelScope.launch {
            try {
                val (id, missing) = withContext(io) {
                    val created = store.importPolar(readText(uri), displayName.removeSuffix(".polar").ifBlank { "Diseño abierto" })
                    created to store.load(created).missingPhotos
                }
                val notice = if (missing > 0) "Faltan $missing fotos de este diseño. Agrégalas y colócalas de nuevo." else null
                _events.send(CatalogEvent.Created(id, notice))
            } catch (e: PolarException) {
                _events.send(CatalogEvent.Error(e.message ?: "El archivo no es un diseño de Polar."))
            }
        }
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.catalog.*' --console=plain`
Expected: PASS.

- [ ] **Step 3: `drawCardPreview` y `Thumbnailer`**

Agrega a `PolarRenderer` (pública, debajo de `drawPage`):

```kotlin
    /** Una sola tarjeta con marcadores de posición, para las miniaturas del catálogo. */
    fun drawCardPreview(canvas: Canvas, project: PolarProject, card: PolarRect, scale: Float, fonts: FontProvider = SystemFontProvider) {
        drawCard(canvas, card, project, firstSlot = 0, cardIndex = 0, isPreview = true, scale = scale, bitmapProvider = { null }, fonts = fonts)
    }
```

(Si en la Task 7 `drawCard` quedó con otro orden de parámetros, usa argumentos con nombre como arriba).

`engine/Thumbnailer.kt`:

```kotlin
package com.polar.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.LruCache
import com.polar.app.model.*
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class Thumbnailer(private val fonts: FontProvider) {
    private val styles = LruCache<String, Bitmap>(48)

    fun styleCard(style: TemplateStyle, widthPx: Int): Bitmap {
        val key = "${style.name}@$widthPx"
        styles.get(key)?.let { return it }
        val aspect = if (style == TemplateStyle.IMPORTED) 0.75 else style.defaultAspect
        val card = PolarRect(0.0, 0.0, 100.0, 100.0 / aspect)
        val scale = widthPx / 100f
        val bitmap = Bitmap.createBitmap(widthPx, (card.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val project = PolarProject(settings = PrintSettings(style = style, cutGuides = false)).normalized()
        PolarRenderer.drawCardPreview(Canvas(bitmap), project, card, scale, fonts)
        styles.put(key, bitmap)
        return bitmap
    }

    fun page(project: PolarProject, page: Int, widthPx: Int, bitmapProvider: (PhotoAsset) -> Bitmap?, template: Bitmap?): Bitmap {
        val paper = project.settings.paperSizePoints
        val scale = widthPx / paper.width.toFloat()
        val bitmap = Bitmap.createBitmap(widthPx, (paper.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }
        PolarRenderer.drawPage(canvas, project, page, isPreview = false, scale = scale, bitmapProvider = bitmapProvider, templateBitmap = template, fonts = fonts)
        return bitmap
    }

    fun projectPng(project: PolarProject, bitmapProvider: (PhotoAsset) -> Bitmap?, template: Bitmap?): ByteArray {
        val bitmap = page(project, 0, 480, bitmapProvider, template)
        return ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out); bitmap.recycle(); out.toByteArray() }
    }
}
```

En `AppContainer` agrega: `val thumbnails = Thumbnailer(fonts)`.

- [ ] **Step 4: Textos del catálogo (agregar a `strings.xml`)**

```xml
    <string name="catalog_new">Nuevo diseño</string>
    <string name="catalog_change">Cambiar diseño</string>
    <string name="catalog_all">Todos</string>
    <string name="catalog_own_title">¿Ya tienes tu propio molde?</string>
    <string name="catalog_own_body">Importa una imagen con huecos blancos o transparentes, o abre un diseño guardado en tu Mac.</string>
    <string name="catalog_import">Importar plantilla</string>
    <string name="catalog_open_polar">Abrir archivo .polar</string>
    <string name="catalog_design">Diseño %1$s</string>
```

- [ ] **Step 5: `CatalogContent.kt` (lo usa también el Editor en "Cambiar diseño")**

```kotlin
package com.polar.app.ui.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.engine.Thumbnailer
import com.polar.app.model.*
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogContent(
    title: String,
    current: TemplateStyle?,
    thumbnails: Thumbnailer,
    showImport: Boolean,
    onPick: (TemplateStyle) -> Unit,
    onImportTemplate: (Uri) -> Unit,
    onOpenPolar: (Uri) -> Unit,
    onBack: () -> Unit
) {
    var category by rememberSaveable { mutableStateOf<DesignCategory?>(null) }
    val styles = remember(category) {
        TemplateStyle.entries.filter { it != TemplateStyle.IMPORTED && (category == null || it.category == category) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(onImportTemplate) }
    val pickPolar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onOpenPolar) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } }
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val columns = when { maxWidth >= 1000.dp -> 5; maxWidth >= 700.dp -> 4; maxWidth >= 540.dp -> 3; else -> 2 }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        item { FilterChip(category == null, { category = null }, { Text(stringResource(R.string.catalog_all)) }) }
                        items(DesignCategory.entries) { c -> FilterChip(category == c, { category = c }, { Text(c.displayName) }) }
                    }
                }
                items(styles, key = { it.name }) { style ->
                    DesignCard(style, selected = style == current, thumbnails = thumbnails, onClick = { onPick(style) })
                }
                if (showImport) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.padding(top = 8.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.catalog_own_title), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.catalog_own_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                                        Icon(Icons.Outlined.Upload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.catalog_import))
                                    }
                                    OutlinedButton(onClick = { pickPolar.launch(arrayOf("*/*")) }) {
                                        Icon(Icons.Outlined.FileOpen, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.catalog_open_polar))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignCard(style: TemplateStyle, selected: Boolean, thumbnails: Thumbnailer, onClick: () -> Unit) {
    val px = with(LocalDensity.current) { 120.dp.roundToPx() }
    val image by produceState<ImageBitmap?>(null, style, px) {
        value = withContext(Dispatchers.Default) { thumbnails.styleCard(style, px).asImageBitmap() }
    }
    val label = stringResource(R.string.catalog_design, style.displayName)
    OutlinedCard(
        onClick = onClick,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.semantics { contentDescription = label }
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.fillMaxWidth().height(124.dp).background(PolarColors.table, MaterialTheme.shapes.small).padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                image?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
            }
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(style.displayName, style = MaterialTheme.typography.titleSmall)
                Text(style.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}
```

> `FlowRow` necesita `@OptIn(ExperimentalLayoutApi::class)` en algunas versiones de Compose; agrégalo a la función si el compilador lo pide.

- [ ] **Step 6: `CatalogScreen.kt` y navegación**

```kotlin
package com.polar.app.ui.catalog

import android.provider.OpenableColumns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.polar.app.AppContainer
import com.polar.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.net.Uri

@Composable
fun CatalogScreen(container: AppContainer, onCreated: (id: String, notice: String?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: CatalogViewModel = viewModel(factory = viewModelFactory {
        initializer {
            CatalogViewModel(
                store = container.store,
                readText = { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.bufferedReader().readText() } ?: ""
                    }
                },
                copyTemplate = { id, uri ->
                    withContext(Dispatchers.IO) {
                        val parsed = Uri.parse(uri)
                        val ext = context.contentResolver.getType(parsed)?.substringAfter('/') ?: "png"
                        container.store.importTemplateFile(id, context.contentResolver.openInputStream(parsed)!!, ext)
                    }
                },
                defaultPaper = { container.settings.settings.first().defaultPaper }
            )
        }
    })
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is CatalogEvent.Created -> onCreated(e.id, e.notice)
                is CatalogEvent.Error -> error = e.message
            }
        }
    }
    CatalogContent(
        title = stringResource(R.string.catalog_new),
        current = null,
        thumbnails = container.thumbnails,
        showImport = true,
        onPick = vm::createFromStyle,
        onImportTemplate = { vm.createFromTemplate(it.toString()) },
        onOpenPolar = { uri ->
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Diseño abierto"
            vm.openPolar(uri.toString(), name)
        },
        onBack = onBack
    )
    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.action_ok)) } },
            text = { Text(it) }
        )
    }
}
```

`Routes.kt`: cambia `EditorRoute` a `@Serializable data class EditorRoute(val projectId: String, val notice: String? = null)`.

`PolarNavHost`: reemplaza `composable<CatalogRoute> { Text("Catálogo") }` por:

```kotlin
        composable<CatalogRoute> {
            CatalogScreen(
                container = container,
                onCreated = { id, notice -> nav.navigate(EditorRoute(id, notice)) { popUpTo<HomeRoute>() } },
                onBack = { nav.popBackStack() }
            )
        }
```

- [ ] **Step 7: Correr pruebas, compilar y revisar en el emulador**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`, instala y abre.
Expected: "Nuevo diseño" muestra las 19 miniaturas reales con chips de categoría; filtrar por "Música" deja 3; elegir un diseño lleva al texto provisional "Editor".

- [ ] **Step 8: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(catalog): catálogo con miniaturas reales, importar plantilla y abrir .polar"
```

---

### Task 13: `EditorViewModel` (estado, ediciones, deshacer, autoguardado, calidad)

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/EditorState.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/EditorViewModel.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/EditorViewModelTest.kt`

**Interfaces:**
- Consumes: `ProjectEdits`, `MoodPreset`, `UndoStack`, `TextResolver`, `ProjectStore`, `PhotoSource`, `PolarRenderer.calculateCardRects`/`calculatePhotoRects`, `StyleInfo.textRoles`.
- Produces (`EditorState.kt`):
  - `enum Tool { PHOTOS, DESIGN, TEXT, PAPER }`, `enum EditorMode { EDIT, CROP, FINISH, CHANGE_DESIGN }`, `enum TextScope { ALL, CARD }`.
  - `data class EditorUiState(...)` con `selectedCard`, `selectedPlacement`, `editCard`, `usedAssetIds`, `selectedCardNumber`.
  - `sealed interface EditorEvent { Message(text, undoable); Exported(file, mime, pdf) }`.
  - `interface ExportService { suspend fun pdf(project, template: Bitmap?): File; suspend fun png(project, page, template: Bitmap?): File }`.
  - `class EditorDeps(store, photos, exports, thumbnail: suspend (PolarProject, Bitmap?) -> ByteArray?, loadTemplate: (String) -> Bitmap?, io, appScope, autosaveDelayMs = 800)`.
- Produces (`EditorViewModel(projectId, deps)`):
  - `state: StateFlow<EditorUiState>`, `events: Flow<EditorEvent>`, `templateBitmap: Bitmap?`.
  - Historial: `beginGesture()`, `endGesture()`, `undo()`, `redo()`, `flush()` (suspend), `flushAsync()`.
  - Selección y navegación: `selectSlot(slot)`, `clearSelection()`, `setPage(page)`, `setTool(tool?)`, `openTextForSelected()`, `setTextRole(role)`, `setTextScope(scope)`, `setMode(mode)`, `setEditingRegions(on)`.
  - Texto: `setText(value)`, `clearOwnText()`, `applyTextToAll()`, `editAppearance(change)`, `resetAppearance()`.
  - Fecha y QR: `setDateSource(source)`, `setChosenDate(ms)`, `setDateStyle(style)`, `setSongUrl(url)`.
  - Diseño: `selectStyle(style)`, `applyMood(mood)`, `applySuggestedPhrases()`, `setAccent(hex)`, `applyLayout(count)`, `setGrid(c, r)`, `setCardFormat(f)`, `setGap(v)`, `setRounded(b)`.
  - Calendario: `setCalendarYear(y)`, `setHighlightDate(b)`, `setSpecialDate(ms)`.
  - Plantilla: `editRegion(i, change)`, `moveRegion(i, dx, dy)`, `addRegion()`, `removeRegion(i)`.
  - Papel: `setPaper(size)`, `setOrientation(o)`, `setCustomPaper(w, h)`, `setMargin(v)`, `setGuides(b)`, `setCutStyle(c)`, `setBorders(b)`.
  - Fotos: `addPhotos(uris)`, `fillAll()`, `placePhoto(assetId)`, `removeSelectedPhoto()`, `rotateSelected()`, `editSelectedPlacement(change)`, `resetSelectedPlacement()`.
  - Hojas: `addPage()`, `clearPage()`, `removePage()`.
  - Proyecto: `rename(name)`.
  - Calidad: `dpiOf(slot): Double?`, `lowResSlots(): List<Int>`, `emptySlotsOnUsedPages(): Int`.
  - Exportar: `exportPdf()`, `exportPng()`.

- [ ] **Step 1: Pruebas que fallan**

```kotlin
package com.polar.app.ui.editor

import android.graphics.Bitmap
import com.polar.app.MainDispatcherRule
import com.polar.app.core.text.TextResolver
import com.polar.app.data.*
import com.polar.app.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var store: ProjectStore
    private lateinit var id: String

    private fun vm(): EditorViewModel {
        store = ProjectStore(tmp.root)
        id = store.create(PolarProject().normalized(), "Boda")
        val photos = object : PhotoSource {
            override suspend fun import(projectId: String, uris: List<String>) = ImportResult(
                uris.map { store.importPhoto(projectId, it.byteInputStream(), "jpg", PhotoInfo(3000, 3000, null)) }, 0
            )
        }
        val exports = object : ExportService {
            override suspend fun pdf(project: PolarProject, template: Bitmap?) = File(tmp.root, "a.pdf")
            override suspend fun png(project: PolarProject, page: Int, template: Bitmap?) = File(tmp.root, "a.png")
        }
        return EditorViewModel(id, EditorDeps(store, photos, exports, { _, _ -> null }, { null }, main.dispatcher, CoroutineScope(main.dispatcher)))
    }

    @Test
    fun loadsProject() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        assertFalse(v.state.value.loading)
        assertEquals("Boda", v.state.value.project.name)
        assertEquals(TextRole.TITLE, v.state.value.textRole)
    }

    @Test
    fun editRecordsUndoAndRedo() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("C34048")
        assertTrue(v.state.value.canUndo)
        v.undo()
        assertEquals("92394A", v.state.value.project.settings.accentHex)
        v.redo()
        assertEquals("C34048", v.state.value.project.settings.accentHex)
    }

    @Test
    fun textTransactionIsOneStep() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val original = v.state.value.project.settings.title
        v.beginGesture(); v.setText("L"); v.setText("Lu"); v.setText("Lu y Max"); v.endGesture()
        assertEquals("Lu y Max", v.state.value.project.settings.title)
        v.undo()
        assertEquals(original, v.state.value.project.settings.title)
        assertFalse(v.state.value.canUndo)
    }

    @Test
    fun perCardTextNeedsSelection() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setTextScope(TextScope.CARD)
        assertEquals(TextScope.ALL, v.state.value.textScope)
        assertTrue(v.events.first() is EditorEvent.Message)
        v.selectSlot(1); v.setTextScope(TextScope.CARD); v.setText("El brindis")
        val p = v.state.value.project
        assertEquals("El brindis", TextResolver.text(p, 1, TextRole.TITLE))
        assertEquals(p.settings.title, TextResolver.text(p, 0, TextRole.TITLE))
    }

    @Test
    fun autosaveAfterDelay() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("486855")
        advanceTimeBy(500)
        assertEquals("92394A", store.load(id).project.settings.accentHex)
        advanceTimeBy(500); advanceUntilIdle()
        assertEquals("486855", store.load(id).project.settings.accentHex)
    }

    @Test
    fun flushSavesPendingChanges() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.setAccent("38536F")
        v.flush()
        assertEquals("38536F", store.load(id).project.settings.accentHex)
    }

    @Test
    fun addPhotosFillsFromSelectionAndPlaceAdvances() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.selectSlot(2)
        v.addPhotos(listOf("a", "b")); advanceUntilIdle()
        val pl = v.state.value.project.placements
        assertNotNull(pl[2]); assertNotNull(pl[3]); assertNull(pl[0])
        v.selectSlot(0)
        v.placePhoto(v.state.value.project.photos[0].id)
        assertNotNull(v.state.value.project.placements[0])
        assertEquals(1, v.state.value.selectedSlot) // siguiente espacio vacío de la hoja
    }

    @Test
    fun lowResolutionIsDetected() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        val small = store.importPhoto(id, "x".byteInputStream(), "jpg", PhotoInfo(120, 120, null))
        v.addPhotosForTest(listOf(small))
        assertTrue(v.lowResSlots().isNotEmpty())
    }

    @Test
    fun removeSelectedPhotoIsUndoable() = runTest(main.dispatcher) {
        val v = vm(); advanceUntilIdle()
        v.addPhotos(listOf("a")); advanceUntilIdle()
        v.events.first() // "1 foto agregada"
        v.selectSlot(0); v.removeSelectedPhoto()
        val e = v.events.first() as EditorEvent.Message
        assertTrue(e.undoable)
        v.undo()
        assertNotNull(v.state.value.project.placements[0])
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.*' --console=plain`
Expected: FAIL (no existen las clases del editor).

- [ ] **Step 2: `EditorState.kt`**

```kotlin
package com.polar.app.ui.editor

import android.graphics.Bitmap
import com.polar.app.core.edit.MoodPreset
import com.polar.app.data.PhotoSource
import com.polar.app.data.ProjectStore
import com.polar.app.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

enum class Tool { PHOTOS, DESIGN, TEXT, PAPER }
enum class EditorMode { EDIT, CROP, FINISH, CHANGE_DESIGN }
enum class TextScope { ALL, CARD }

data class EditorUiState(
    val loading: Boolean = true,
    val project: PolarProject = PolarProject().normalized(),
    val page: Int = 0,
    val selectedSlot: Int? = null,
    val tool: Tool? = null,
    val textRole: TextRole = TextRole.TITLE,
    val textScope: TextScope = TextScope.ALL,
    val mode: EditorMode = EditorMode.EDIT,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saving: Boolean = false,
    val missingPhotos: Int = 0,
    val editingRegions: Boolean = false,
    val templateVersion: Int = 0,
    val busy: Boolean = false,
    val lastMood: MoodPreset = MoodPreset.COUPLE
) {
    val selectedCard: Int? get() = selectedSlot?.let { project.cardOfSlot(it) }
    val selectedPlacement: PhotoPlacement? get() = selectedSlot?.let { project.placements.getOrNull(it) }
    val editCard: Int? get() = if (textScope == TextScope.CARD) selectedCard else null
    val usedAssetIds: Set<String> get() = project.placements.mapNotNull { it?.assetID }.toSet()
    val selectedCardNumber: Int? get() = selectedCard?.let { it % project.cardsPerPage + 1 }
}

sealed interface EditorEvent {
    data class Message(val text: String, val undoable: Boolean = false) : EditorEvent
    data class Exported(val file: File, val mime: String, val pdf: Boolean) : EditorEvent
}

interface ExportService {
    suspend fun pdf(project: PolarProject, template: Bitmap?): File
    suspend fun png(project: PolarProject, page: Int, template: Bitmap?): File
}

class EditorDeps(
    val store: ProjectStore,
    val photos: PhotoSource,
    val exports: ExportService,
    val thumbnail: suspend (PolarProject, Bitmap?) -> ByteArray?,
    val loadTemplate: (String) -> Bitmap?,
    val io: CoroutineDispatcher = Dispatchers.IO,
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    val autosaveDelayMs: Long = 800
)
```

- [ ] **Step 3: `EditorViewModel.kt`**

```kotlin
package com.polar.app.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.app.core.edit.MoodPreset
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.core.history.UndoStack
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(private val projectId: String, private val deps: EditorDeps) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private val history = UndoStack<PolarProject>()
    private var saveJob: Job? = null
    private var dirty = false
    var templateBitmap: Bitmap? = null
        private set

    init { viewModelScope.launch { load() } }

    private suspend fun load() {
        val loaded = withContext(deps.io) { deps.store.load(projectId) }
        templateBitmap = loaded.project.settings.importedTemplate?.let { t -> withContext(deps.io) { deps.loadTemplate(t.path) } }
        _state.update {
            it.copy(
                loading = false, project = loaded.project, missingPhotos = loaded.missingPhotos,
                textRole = loaded.project.settings.style.textRoles.firstOrNull() ?: TextRole.TITLE,
                templateVersion = it.templateVersion + 1
            )
        }
        if (loaded.missingPhotos > 0) message("Faltan ${loaded.missingPhotos} fotos de este diseño. Agrégalas y colócalas de nuevo.")
    }

    // ---------- núcleo ----------

    private fun message(text: String, undoable: Boolean = false) { _events.trySend(EditorEvent.Message(text, undoable)) }

    private fun edit(message: String? = null, undoable: Boolean = message != null, change: (PolarProject) -> PolarProject) {
        val current = _state.value.project
        val next = change(current)
        if (next == current) return
        history.record(current)
        commit(next)
        message?.let { message(it, undoable) }
    }

    private fun commit(next: PolarProject) {
        _state.update { s ->
            val page = s.page.coerceIn(0, next.pageCount - 1)
            val slot = s.selectedSlot?.takeIf { it < next.placements.size }
            s.copy(project = next, page = page, selectedSlot = slot, canUndo = history.canUndo, canRedo = history.canRedo)
        }
        dirty = true
        scheduleSave()
    }

    fun beginGesture() = history.beginTransaction(_state.value.project)

    fun endGesture() {
        history.endTransaction(_state.value.project)
        _state.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    }

    fun undo() { history.undo(_state.value.project)?.let(::commit) }
    fun redo() { history.redo(_state.value.project)?.let(::commit) }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { delay(deps.autosaveDelayMs); save() }
    }

    suspend fun flush() {
        saveJob?.cancel()
        if (dirty) save()
    }

    fun flushAsync() { viewModelScope.launch { flush() } }

    private suspend fun save() {
        val project = _state.value.project
        _state.update { it.copy(saving = true) }
        withContext(deps.io) {
            val thumb = runCatching { deps.thumbnail(project, templateBitmap) }.getOrNull()
            deps.store.save(projectId, project, thumb)
        }
        if (_state.value.project == project) dirty = false
        _state.update { it.copy(saving = false) }
    }

    override fun onCleared() {
        if (dirty) {
            val project = _state.value.project
            deps.appScope.launch { deps.store.save(projectId, project) }
        }
    }

    // ---------- selección y navegación ----------

    fun selectSlot(slot: Int) = _state.update { s ->
        val page = slot / s.project.settings.capacity
        s.copy(selectedSlot = if (s.selectedSlot == slot) null else slot, page = page)
    }

    fun clearSelection() = _state.update { it.copy(selectedSlot = null) }

    fun setPage(page: Int) = _state.update { s ->
        val p = page.coerceIn(0, s.project.pageCount - 1)
        val keep = s.selectedSlot?.takeIf { it / s.project.settings.capacity == p }
        s.copy(page = p, selectedSlot = keep)
    }

    fun setTool(tool: Tool?) = _state.update { it.copy(tool = tool, textScope = if (tool == Tool.TEXT) TextScope.ALL else it.textScope) }

    fun openTextForSelected() = _state.update { it.copy(tool = Tool.TEXT, textScope = if (it.selectedSlot != null) TextScope.CARD else TextScope.ALL) }

    fun setTextRole(role: TextRole) = _state.update { it.copy(textRole = role) }

    fun setTextScope(scope: TextScope) {
        if (scope == TextScope.CARD && _state.value.selectedSlot == null) {
            message("Toca una tarjeta de la hoja para darle su propio texto"); return
        }
        _state.update { it.copy(textScope = scope) }
    }

    fun setMode(mode: EditorMode) = _state.update { it.copy(mode = mode) }
    fun setEditingRegions(on: Boolean) = _state.update { it.copy(editingRegions = on) }

    // ---------- texto ----------

    fun setText(value: String) {
        val s = _state.value
        edit { ProjectEdits.setText(it, s.textRole, value.take(500), s.editCard) }
    }

    fun clearOwnText() {
        val s = _state.value
        val card = s.selectedCard ?: return
        edit("La tarjeta ${s.selectedCardNumber} vuelve al texto general") { ProjectEdits.clearOwnText(it, card, s.textRole) }
    }

    fun applyTextToAll() {
        val role = _state.value.textRole
        edit("Todas las tarjetas usan el mismo ${role.displayName.lowercase()}") { ProjectEdits.applyTextToAll(it, role) }
    }

    fun editAppearance(change: (TextAppearance) -> TextAppearance) {
        val s = _state.value
        edit { ProjectEdits.editAppearance(it, s.textRole, s.editCard, change) }
    }

    fun resetAppearance() {
        val s = _state.value
        val card = s.editCard
        edit("Estilo restablecido") {
            if (card != null) ProjectEdits.clearOwnAppearance(it, card, s.textRole) else ProjectEdits.resetAppearance(it, s.textRole)
        }
    }

    fun setDateSource(source: DateSource) { val c = _state.value.editCard; edit { ProjectEdits.setDateSource(it, source, c) } }
    fun setChosenDate(epochMs: Long) { val c = _state.value.editCard; edit { ProjectEdits.setChosenDate(it, epochMs, c) } }
    fun setDateStyle(style: DateStyle) = edit { ProjectEdits.setDateStyle(it, style) }
    fun setSongUrl(url: String) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(songURL = url.trim().take(500)) } }

    // ---------- diseño ----------

    fun selectStyle(style: TemplateStyle) {
        edit("Ahora es ${style.displayName}. Tus fotos y frases siguen igual.") { ProjectEdits.selectStyle(it, style) }
        _state.update { it.copy(mode = EditorMode.EDIT, selectedSlot = null, textRole = style.textRoles.firstOrNull() ?: TextRole.TITLE) }
    }

    fun applyMood(mood: MoodPreset) {
        _state.update { it.copy(lastMood = mood) }
        edit("Estilo ${mood.displayName}: cambió la letra y el color, tus frases siguen igual") { ProjectEdits.applyMood(it, mood) }
    }

    fun applySuggestedPhrases() {
        val mood = _state.value.lastMood
        edit("Frases sugeridas en todas las tarjetas") { ProjectEdits.applySuggestedPhrases(it, mood) }
    }

    fun setAccent(hex: String) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(accentHex = hex) } }

    fun applyLayout(count: Int) {
        edit("$count por hoja") { ProjectEdits.applyLayoutPreset(it, count) }
        _state.update { it.copy(page = 0, selectedSlot = null) }
    }

    fun setGrid(columns: Int, rows: Int) = edit { ProjectEdits.setGrid(it, columns, rows) }
    fun setCardFormat(format: CardFormat) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(cardFormat = format) } }
    fun setGap(value: Double) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(gap = value.coerceIn(0.0, 30.0)) } }
    fun setRounded(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(roundedPhotos = on) } }
    fun setCalendarYear(year: Int) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(calendarYear = year.coerceIn(1900, 2100)) } }
    fun setHighlightDate(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(highlightDate = on) } }
    fun setSpecialDate(epochMs: Long) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(specialDate = SwiftDate.fromEpochMs(epochMs)) } }

    fun editRegion(index: Int, change: (TemplateRegion) -> TemplateRegion) = edit { ProjectEdits.editTemplateRegion(it, index, change) }
    fun moveRegion(index: Int, dx: Double, dy: Double) = editRegion(index) { r -> r.copy(x = r.x + dx, y = r.y + dy) }
    fun addRegion() = edit("Hueco agregado") { ProjectEdits.addTemplateRegion(it) }
    fun removeRegion(index: Int) = edit("Hueco quitado") { ProjectEdits.removeTemplateRegion(it, index) }

    // ---------- papel ----------

    fun setPaper(size: PaperSize) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(paperSize = size) } }
    fun setOrientation(o: PaperOrientation) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(orientation = o) } }
    fun setCustomPaper(widthMM: Double, heightMM: Double) = edit { ProjectEdits.setCustomPaper(it, widthMM, heightMM) }
    fun setMargin(value: Double) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(margin = value.coerceIn(0.0, 60.0)) } }
    fun setGuides(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(cutGuides = on) } }
    fun setCutStyle(style: CutStyle) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(cutStyle = style) } }
    fun setBorders(on: Boolean) = edit { ProjectEdits.updateSettings(it) { s -> s.copy(drawBorders = on) } }

    // ---------- fotos ----------

    fun addPhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = deps.photos.import(projectId, uris)
            _state.update { it.copy(busy = false) }
            addAssets(result.assets)
            val added = result.assets.size
            val text = buildString {
                append(if (added == 1) "1 foto agregada" else "$added fotos agregadas")
                if (result.failed > 0) append(" · ${result.failed} no se pudieron leer")
            }
            message(text, undoable = added > 0)
        }
    }

    /** Sólo para pruebas: agrega fotos ya copiadas al proyecto. */
    internal fun addPhotosForTest(assets: List<PhotoAsset>) = addAssets(assets)

    private fun addAssets(assets: List<PhotoAsset>) {
        val s = _state.value
        val from = s.selectedSlot ?: s.project.placements.indexOfFirst { it == null }.takeIf { it >= 0 } ?: s.project.placements.size
        edit { ProjectEdits.addPhotos(it, assets, from) }
    }

    fun fillAll() {
        val p = _state.value.project
        if (p.photos.isEmpty()) { message("Primero agrega tus fotos"); return }
        val pages = ProjectEdits.fillAll(p).pageCount
        edit("${p.photos.size} fotos acomodadas en $pages ${if (pages == 1) "hoja" else "hojas"}") { ProjectEdits.fillAll(it) }
        _state.update { it.copy(page = 0, selectedSlot = null) }
    }

    fun placePhoto(assetId: String) {
        val s = _state.value
        val slot = s.selectedSlot ?: run { message("Primero toca la tarjeta donde quieres esta foto"); return }
        edit { ProjectEdits.assign(it, slot, assetId) }
        val p = _state.value.project
        val cap = p.settings.capacity
        val pageEnd = (slot / cap + 1) * cap
        val next = (slot + 1 until pageEnd).firstOrNull { p.placements.getOrNull(it) == null }
        _state.update { it.copy(selectedSlot = next ?: slot) }
    }

    fun removeSelectedPhoto() {
        val slot = _state.value.selectedSlot ?: return
        edit("Foto quitada", undoable = true) { ProjectEdits.clearSlot(it, slot) }
    }

    fun rotateSelected() {
        val slot = _state.value.selectedSlot ?: return
        edit { ProjectEdits.editPlacement(it, slot) { p -> p.copy(quarterTurns = p.quarterTurns + 1) } }
    }

    fun editSelectedPlacement(change: (PhotoPlacement) -> PhotoPlacement) {
        val slot = _state.value.selectedSlot ?: return
        edit { ProjectEdits.editPlacement(it, slot, change) }
    }

    fun resetSelectedPlacement() = editSelectedPlacement { it.copy(zoom = 1.0, offsetX = 0.0, offsetY = 0.0, quarterTurns = 0) }

    // ---------- hojas ----------

    fun addPage() {
        edit("Hoja agregada") { ProjectEdits.addPage(it) }
        _state.update { it.copy(page = it.project.pageCount - 1, selectedSlot = null) }
    }

    fun clearPage() { val page = _state.value.page; edit("Hoja ${page + 1} vaciada", undoable = true) { ProjectEdits.clearPage(it, page) } }
    fun removePage() { val page = _state.value.page; edit("Hoja quitada", undoable = true) { ProjectEdits.removePage(it, page) } }

    fun rename(name: String) {
        val clean = name.trim().take(80)
        if (clean.isNotEmpty()) edit { it.copy(name = clean) }
    }

    // ---------- calidad ----------

    fun dpiOf(slot: Int): Double? {
        val p = _state.value.project
        val s = p.settings
        val local = slot % s.capacity
        val cards = PolarRenderer.calculateCardRects(s)
        val rect = if (s.style == TemplateStyle.IMPORTED) cards.getOrNull(local) ?: return null
        else {
            val card = cards.getOrNull(local / s.style.photosPerCard) ?: return null
            PolarRenderer.calculatePhotoRects(card, s.style, s).getOrNull(local % s.style.photosPerCard) ?: return null
        }
        return p.effectiveDPI(slot, rect.width, rect.height)
    }

    fun lowResSlots(): List<Int> =
        _state.value.project.placements.indices.filter { slot -> dpiOf(slot)?.let { it < 150.0 } == true }

    fun emptySlotsOnUsedPages(): Int {
        val p = _state.value.project
        val cap = p.settings.capacity
        return (0 until p.pageCount).sumOf { page ->
            val slice = p.placements.subList(page * cap, minOf(p.placements.size, (page + 1) * cap))
            if (slice.any { it != null }) slice.count { it == null } else 0
        }
    }

    // ---------- exportar ----------

    fun exportPdf() = export(pdf = true)
    fun exportPng() = export(pdf = false)

    private fun export(pdf: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val s = _state.value
                val file = if (pdf) deps.exports.pdf(s.project, templateBitmap) else deps.exports.png(s.project, s.page, templateBitmap)
                _events.send(EditorEvent.Exported(file, if (pdf) "application/pdf" else "image/png", pdf))
            } catch (e: PolarException) {
                message(e.message ?: "No pudimos crear el archivo.")
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
```

> **Sobre `edit()` durante un gesto:** mientras hay una transacción abierta (`beginGesture`), `history.record` no hace nada, así que los cambios en vivo de un slider o de un campo de texto no llenan el historial. `endGesture` guarda un solo paso.

- [ ] **Step 4: Correr las pruebas**

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.*' --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add PolarAndroid/app/src/main/java/com/polar/app/ui/editor PolarAndroid/app/src/test/java/com/polar/app/ui/editor
git commit -m "feat(editor): estado del editor con deshacer por acción y autoguardado"
```

---
### Task 14: Interfaz del Editor: hoja, selección, barra contextual y herramientas

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/export/AndroidExportService.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/export/PolarExporter.kt` (parámetro `fonts`)
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/PolarApplication.kt` (agregar `exports`)
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/components/Controls.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/SheetGeometry.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/SheetCanvas.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/ContextBar.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/ToolNav.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/ToolPanel.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/EditorScreen.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/navigation/PolarNavHost.kt` (destino `EditorRoute`)
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/SheetGeometryTest.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/ContextBarTest.kt`

**Interfaces:**
- Consumes: `EditorViewModel`, `EditorUiState`, `EditorDeps`, `ExportService` (Task 13); `Thumbnailer` (Task 12); `BitmapLoader` (Task 7).
- Produces:
  - `PolarExporter.exportPdf(project, outputFile, bitmapProvider, templateBitmap, fonts)` y `exportPng(project, page, outputFile, dpi, bitmapProvider, templateBitmap, fonts)`.
  - `class AndroidExportService(context, bitmaps, fonts) : ExportService`; `AppContainer.exports`.
  - `object SheetGeometry { fun slotRects(settings): List<PolarRect>; fun slotAt(rects, xPt, yPt): Int? }`.
  - En `Controls.kt`:
    - `SectionLabel(text)`
    - `SwitchRow(title, subtitle?, checked, onChange)`
    - `LabeledSlider(label, value, range, valueText, onChange, onStart, onEnd, steps = 0)`
    - `MoreOptions(expanded, onToggle, summary, content)`
    - `ColorSwatch(hex, name, selected, onClick)`
    - `ColorChoiceDialog(current, onPick, onDismiss)`
    - `val ColorChoices: List<Pair<String, String>>` (hex a nombre)
    - `fun hexColor(hex): Color`
  - `@Composable fun ContextBar(hasPhoto: Boolean, onChange, onCrop, onRotate, onText, onRemove)`.
  - `@Composable fun ToolNavBar(tool: Tool?, onSelect: (Tool) -> Unit)` y `ToolRail(tool, onSelect)`.
  - `@Composable fun ToolPanel(tool, state, vm, container, compact: Boolean, onClose)`. Las Tasks 15–18 reemplazan su contenido provisional.
  - `@Composable fun EditorScreen(vm, container, notice: String?, onBack)`.

- [ ] **Step 1: Pruebas que fallan**

`SheetGeometryTest.kt`:

```kotlin
package com.polar.app.ui.editor

import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test

class SheetGeometryTest {
    @Test
    fun oneRectPerSlotInRendererOrder() {
        assertEquals(9, SheetGeometry.slotRects(PrintSettings()).size)
        val film = PrintSettings(style = TemplateStyle.FILM_VERTICAL, columns = 2, rows = 1)
        assertEquals(10, SheetGeometry.slotRects(film).size)
        val tpl = ImportedTemplate("t.png", 100, 100, listOf(TemplateRegion(x = 0.1, y = 0.1, width = 0.3, height = 0.3), TemplateRegion(x = 0.5, y = 0.5, width = 0.3, height = 0.3)))
        assertEquals(2, SheetGeometry.slotRects(PrintSettings(style = TemplateStyle.IMPORTED, columns = 1, rows = 1, importedTemplate = tpl)).size)
    }

    @Test
    fun hitTestFindsSlotOrNothing() {
        val rects = SheetGeometry.slotRects(PrintSettings())
        val r = rects[4]
        assertEquals(4, SheetGeometry.slotAt(rects, r.midX, r.midY))
        assertNull(SheetGeometry.slotAt(rects, 1.0, 1.0)) // margen de la hoja
    }
}
```

`ContextBarTest.kt`:

```kotlin
package com.polar.app.ui.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ContextBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fullCardShowsAllActions() {
        var last = ""
        compose.setContent {
            PolarTheme(ThemeMode.LIGHT) {
                ContextBar(true, { last = "cambiar" }, { last = "encuadrar" }, { last = "girar" }, { last = "texto" }, { last = "quitar" })
            }
        }
        compose.onNodeWithText("Quitar").performClick(); assertEquals("quitar", last)
        compose.onNodeWithText("Texto").performClick(); assertEquals("texto", last)
        compose.onNodeWithText("Encuadrar").performClick(); assertEquals("encuadrar", last)
    }

    @Test
    fun emptyCardOffersToPlacePhoto() {
        var changed = false
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { ContextBar(false, { changed = true }, {}, {}, {}, {}) } }
        compose.onNodeWithText("Poner una foto aquí").performClick()
        assertEquals(true, changed)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.SheetGeometryTest' --tests 'com.polar.app.ui.editor.ContextBarTest' --console=plain`
Expected: FAIL (no existen `SheetGeometry` ni `ContextBar`).

- [ ] **Step 2: Exportación con fuentes**

En `PolarExporter.kt`:
- Agrega `fonts: FontProvider = SystemFontProvider` como último parámetro de `renderPageToBitmap`, `exportPng` y `exportPdf`, y pásalo a cada `PolarRenderer.drawPage(..., fonts = fonts)` y de `exportPng` a `renderPageToBitmap`.
- Borra `saveToPublicStorage`, `shareFile` y `openFile`; los reemplazan `ui/Share.kt` (Task 11) y el selector "Guardar como" (Task 20).

`export/AndroidExportService.kt`:

```kotlin
package com.polar.app.export

import android.content.Context
import android.graphics.Bitmap
import com.polar.app.data.BitmapLoader
import com.polar.app.engine.FontProvider
import com.polar.app.model.PolarProject
import com.polar.app.ui.editor.ExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidExportService(
    private val context: Context,
    private val bitmaps: BitmapLoader,
    private val fonts: FontProvider
) : ExportService {
    private fun dir() = File(context.cacheDir, "exports").apply { mkdirs() }
    private fun fileName(p: PolarProject, ext: String) =
        p.name.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifBlank { "Polar" } + ".$ext"

    override suspend fun pdf(project: PolarProject, template: Bitmap?): File = withContext(Dispatchers.IO) {
        File(dir(), fileName(project, "pdf")).also {
            PolarExporter.exportPdf(project, it, { a -> bitmaps.load(a.path, BitmapLoader.EXPORT_MAX) }, template, fonts)
        }
    }

    override suspend fun png(project: PolarProject, page: Int, template: Bitmap?): File = withContext(Dispatchers.IO) {
        File(dir(), fileName(project, "png").replace(".png", " · hoja ${page + 1}.png")).also {
            PolarExporter.exportPng(project, page, it, 300, { a -> bitmaps.load(a.path, BitmapLoader.EXPORT_MAX) }, template, fonts)
        }
    }
}
```

En `AppContainer` agrega `val exports = AndroidExportService(context, bitmaps, fonts)` (debajo de `fonts`).

- [ ] **Step 3: `SheetGeometry.kt`**

```kotlin
package com.polar.app.ui.editor

import com.polar.app.engine.PolarRect
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.PrintSettings
import com.polar.app.model.TemplateStyle

/** Rectángulos (en puntos) de cada espacio de una hoja, en el mismo orden que usa el motor. */
object SheetGeometry {
    fun slotRects(settings: PrintSettings): List<PolarRect> {
        val cards = PolarRenderer.calculateCardRects(settings)
        if (settings.style == TemplateStyle.IMPORTED) return cards
        return cards.flatMap { PolarRenderer.calculatePhotoRects(it, settings.style, settings) }
    }

    fun slotAt(rects: List<PolarRect>, xPt: Double, yPt: Double): Int? =
        rects.indexOfFirst { xPt in it.left..it.right && yPt in it.top..it.bottom }.takeIf { it >= 0 }
}
```

- [ ] **Step 4: Textos del editor (agregar a `strings.xml`)**

```xml
    <string name="editor_back">Volver a tus diseños</string>
    <string name="editor_saved">Guardado</string>
    <string name="editor_saving">Guardando…</string>
    <string name="editor_print">Imprimir</string>
    <string name="editor_more">Más opciones</string>
    <string name="editor_add_page">Agregar hoja</string>
    <string name="editor_clear_page">Vaciar esta hoja</string>
    <string name="editor_remove_page">Quitar esta hoja</string>
    <string name="editor_rename">Cambiar nombre</string>
    <string name="editor_page">Hoja %1$d de %2$d</string>
    <string name="editor_prev_page">Hoja anterior</string>
    <string name="editor_next_page">Hoja siguiente</string>
    <string name="editor_hint">Toca una tarjeta para cambiar su foto o su texto</string>
    <string name="editor_empty_title">Pon tus fotos</string>
    <string name="editor_empty_body">Se acomodan solas en la hoja. Luego cambias lo que quieras.</string>
    <string name="editor_add_photos">Agregar fotos</string>
    <string name="editor_card">Tarjeta %1$d</string>
    <string name="editor_card_empty">Tarjeta %1$d, vacía</string>
    <string name="editor_card_low">, poca resolución</string>
    <string name="ctx_change">Cambiar</string>
    <string name="ctx_crop">Encuadrar</string>
    <string name="ctx_rotate">Girar</string>
    <string name="ctx_text">Texto</string>
    <string name="ctx_remove">Quitar</string>
    <string name="ctx_place">Poner una foto aquí</string>
    <string name="tool_photos">Fotos</string>
    <string name="tool_design">Diseño</string>
    <string name="tool_text">Texto</string>
    <string name="tool_paper">Papel</string>
    <string name="tool_close">Cerrar panel</string>
    <string name="more_hide">Ocultar</string>
    <string name="color_other">Otro color</string>
    <string name="color_choose">Elige un color</string>
```

- [ ] **Step 5: Controles compartidos (`ui/components/Controls.kt`)**

```kotlin
package com.polar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polar.app.R

fun hexColor(hex: String): Color = runCatching { Color(("FF" + hex.removePrefix("#")).toLong(16)) }.getOrDefault(Color.Black)

/** Colores para tocar, con nombre para el lector de pantalla. */
val ColorChoices: List<Pair<String, String>> = listOf(
    "92394A" to "Vino", "C34048" to "Rojo", "E07A5F" to "Coral", "F28C28" to "Naranja", "BC8952" to "Dorado",
    "D4A72C" to "Mostaza", "7A8450" to "Oliva", "486855" to "Verde", "7FB7A4" to "Menta", "38536F" to "Azul",
    "6C9BCF" to "Cielo", "8E7CC3" to "Lavanda", "D98CA8" to "Rosa", "7B5544" to "Café", "C8B49A" to "Arena",
    "8A8580" to "Gris", "20242C" to "Carbón", "FFFFFF" to "Blanco"
)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    steps: Int = 0
) {
    var dragging by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value, valueRange = range, steps = steps,
            onValueChange = { if (!dragging) { dragging = true; onStart() }; onChange(it) },
            onValueChangeFinished = { dragging = false; onEnd() },
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

@Composable
fun MoreOptions(expanded: Boolean, onToggle: () -> Unit, summary: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.editor_more), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(if (expanded) stringResource(R.string.more_hide) else summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
        }
        if (expanded) Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun ColorSwatch(hex: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val color = hexColor(hex)
    Box(
        Modifier.size(48.dp)
            .semantics { contentDescription = name; this.selected = selected; role = Role.RadioButton }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .border(if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape)
            .padding(3.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Filled.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorChoiceDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        title = { Text(stringResource(R.string.color_choose)) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorChoices.forEach { (hex, name) -> ColorSwatch(hex, name, hex.equals(current, true)) { onPick(hex); onDismiss() } }
            }
        }
    )
}
```

- [ ] **Step 6: `ContextBar.kt` y `ToolNav.kt`**

`ContextBar.kt`:

```kotlin
package com.polar.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.R

@Composable
fun ContextBar(hasPhoto: Boolean, onChange: () -> Unit, onCrop: () -> Unit, onRotate: () -> Unit, onText: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge, tonalElevation = 3.dp, shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (hasPhoto) {
            Row(Modifier.height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Action(Icons.Outlined.SwapHoriz, stringResource(R.string.ctx_change), onChange)
                Action(Icons.Outlined.Crop, stringResource(R.string.ctx_crop), onCrop)
                Action(Icons.Outlined.RotateRight, stringResource(R.string.ctx_rotate), onRotate)
                Action(Icons.Outlined.TextFields, stringResource(R.string.ctx_text), onText)
                Action(Icons.Outlined.Delete, stringResource(R.string.ctx_remove), onRemove)
            }
        } else {
            Box(Modifier.height(64.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = onChange) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ctx_place))
                }
            }
        }
    }
}

@Composable
private fun RowScope.Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
```

`ToolNav.kt`:

```kotlin
package com.polar.app.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.polar.app.R

private data class ToolItem(val tool: Tool, val icon: ImageVector, val label: Int)

private val tools = listOf(
    ToolItem(Tool.PHOTOS, Icons.Outlined.PhotoLibrary, R.string.tool_photos),
    ToolItem(Tool.DESIGN, Icons.Outlined.Dashboard, R.string.tool_design),
    ToolItem(Tool.TEXT, Icons.Outlined.TextFields, R.string.tool_text),
    ToolItem(Tool.PAPER, Icons.Outlined.Description, R.string.tool_paper)
)

@Composable
fun ToolNavBar(tool: Tool?, onSelect: (Tool) -> Unit) {
    NavigationBar {
        tools.forEach { item ->
            NavigationBarItem(
                selected = tool == item.tool, onClick = { onSelect(item.tool) },
                icon = { Icon(item.icon, null) }, label = { Text(stringResource(item.label)) }
            )
        }
    }
}

@Composable
fun ToolRail(tool: Tool, onSelect: (Tool) -> Unit) {
    NavigationRail {
        tools.forEach { item ->
            NavigationRailItem(
                selected = tool == item.tool, onClick = { onSelect(item.tool) },
                icon = { Icon(item.icon, null) }, label = { Text(stringResource(item.label)) }
            )
        }
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.SheetGeometryTest' --tests 'com.polar.app.ui.editor.ContextBarTest' --console=plain`
Expected: PASS.

- [ ] **Step 7: `SheetCanvas.kt` (hoja, páginas, toque, doble toque y arrastre de huecos)**

```kotlin
package com.polar.app.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.data.BitmapLoader
import com.polar.app.engine.PolarRenderer
import com.polar.app.model.PolarProject
import com.polar.app.model.TemplateStyle
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SheetArea(state: EditorUiState, vm: EditorViewModel, container: AppContainer, modifier: Modifier = Modifier) {
    val count = state.project.pageCount
    val pager = rememberPagerState(initialPage = state.page) { count }
    var zoomed by remember { mutableStateOf(false) }
    LaunchedEffect(pager.currentPage) { if (pager.currentPage != state.page) vm.setPage(pager.currentPage) }
    LaunchedEffect(state.page) { if (pager.currentPage != state.page) pager.animateScrollToPage(state.page) }

    Column(modifier.background(PolarColors.table)) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = !zoomed && !state.editingRegions,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            SheetPage(state, page, vm, container, zoomed = zoomed && page == state.page, onToggleZoom = { zoomed = !zoomed })
        }
        PagerRow(state.page, count, onPrev = { vm.setPage(state.page - 1) }, onNext = { vm.setPage(state.page + 1) })
    }
}

@Composable
private fun PagerRow(page: Int, count: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = page > 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.editor_prev_page)) }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clearAndSetSemantics { }) {
            repeat(minOf(count, 8)) { i ->
                Box(Modifier.height(7.dp).width(if (i == page) 18.dp else 7.dp).background(
                    if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
            }
        }
        Text(stringResource(R.string.editor_page, page + 1, count), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp))
        IconButton(onClick = onNext, enabled = page < count - 1) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.editor_next_page)) }
    }
}

@Composable
private fun SheetPage(state: EditorUiState, page: Int, vm: EditorViewModel, container: AppContainer, zoomed: Boolean, onToggleZoom: () -> Unit) {
    val project = state.project
    val settings = project.settings
    val paper = settings.paperSizePoints
    val ratio = (paper.width / paper.height).toFloat()
    val density = LocalDensity.current
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val warning = PolarColors.onWarningContainer
    val rects = remember(settings) { SheetGeometry.slotRects(settings) }
    val lowRes = remember(project) { vm.lowResSlots().toSet() }
    val cap = settings.capacity

    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val (w, h) = fit(maxWidth, maxHeight, ratio)
        val widthPx = with(density) { w.toPx() }
        val ptToPx = widthPx / paper.width.toFloat()
        val image = rememberPageImage(project, page, widthPx.roundToInt(), vm.templateBitmap, state.templateVersion, container)
        var pan by remember(zoomed) { mutableStateOf(Offset.Zero) }

        Box(
            Modifier.size(w, h)
                .graphicsLayer {
                    val s = if (zoomed) 2f else 1f
                    scaleX = s; scaleY = s; translationX = pan.x; translationY = pan.y
                }
                .shadow(6.dp, RoundedCornerShape(2.dp))
                .background(Color.White)
                .pointerInput(settings, page, state.editingRegions) {
                    detectTapGestures(
                        onDoubleTap = { onToggleZoom() },
                        onTap = { pos ->
                            val hit = SheetGeometry.slotAt(rects, (pos.x / ptToPx).toDouble(), (pos.y / ptToPx).toDouble())
                            if (hit != null) vm.selectSlot(page * cap + hit) else vm.clearSelection()
                        }
                    )
                }
                .pointerInput(zoomed, state.editingRegions, state.selectedSlot) {
                    if (zoomed) detectDragGestures { change, drag -> change.consume(); pan += drag }
                    else if (state.editingRegions && settings.style == TemplateStyle.IMPORTED) {
                        val index = (state.selectedSlot ?: return@pointerInput) % cap
                        val frame = PolarRenderer.templateRect(settings)
                        detectDragGestures(
                            onDragStart = { vm.beginGesture() },
                            onDragEnd = { vm.endGesture() },
                            onDragCancel = { vm.endGesture() }
                        ) { change, drag ->
                            change.consume()
                            vm.moveRegion(index, drag.x / ptToPx / frame.width, drag.y / ptToPx / frame.height)
                        }
                    }
                }
        ) {
            image?.let { Image(it, null, Modifier.fillMaxSize()) }
            val selectedLocal = state.selectedSlot?.takeIf { it / cap == page }?.rem(cap)
            Canvas(Modifier.fillMaxSize()) {
                selectedLocal?.let { rects.getOrNull(it) }?.let { r ->
                    val inset = 3.dp.toPx()
                    drawRect(
                        primary, topLeft = Offset(r.left.toFloat() * ptToPx - inset, r.top.toFloat() * ptToPx - inset),
                        size = Size(r.width.toFloat() * ptToPx + 2 * inset, r.height.toFloat() * ptToPx + 2 * inset),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                rects.forEachIndexed { i, r ->
                    if (page * cap + i in lowRes) drawCircle(warning, radius = 7.dp.toPx(),
                        center = Offset(r.right.toFloat() * ptToPx - 9.dp.toPx(), r.bottom.toFloat() * ptToPx - 9.dp.toPx()))
                }
            }
            selectedLocal?.let { rects.getOrNull(it) }?.let { r ->
                Box(
                    Modifier.offset(with(density) { (r.left.toFloat() * ptToPx).toDp() } - 10.dp, with(density) { (r.top.toFloat() * ptToPx).toDp() } - 10.dp)
                        .background(primary, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                        .clearAndSetSemantics { }
                ) { Text("${state.selectedCardNumber ?: 1}", color = onPrimary, style = MaterialTheme.typography.labelSmall) }
            }
            // Nodos sólo para TalkBack: no reciben toques, el toque lo maneja la hoja.
            rects.forEachIndexed { i, r ->
                val slot = page * cap + i
                val empty = project.placements.getOrNull(slot) == null
                val number = project.cardOfSlot(slot) % project.cardsPerPage + 1
                val label = (if (empty) context.getString(R.string.editor_card_empty, number) else context.getString(R.string.editor_card, number)) +
                    (if (slot in lowRes) context.getString(R.string.editor_card_low) else "")
                Box(
                    Modifier.offset(with(density) { (r.left.toFloat() * ptToPx).toDp() }, with(density) { (r.top.toFloat() * ptToPx).toDp() })
                        .size(with(density) { (r.width.toFloat() * ptToPx).toDp() }, with(density) { (r.height.toFloat() * ptToPx).toDp() })
                        .semantics {
                            contentDescription = label
                            role = Role.Button
                            selected = state.selectedSlot == slot
                            onClick { vm.selectSlot(slot); true }
                        }
                )
            }
        }
    }
}

private fun fit(maxW: Dp, maxH: Dp, ratio: Float): Pair<Dp, Dp> =
    if (maxW / maxH > ratio) (maxH * ratio) to maxH else maxW to (maxW / ratio)

@Composable
private fun rememberPageImage(project: PolarProject, page: Int, widthPx: Int, template: Bitmap?, version: Int, container: AppContainer): ImageBitmap? {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(project, page, widthPx, version) {
        delay(16) // junta cambios muy seguidos (sliders, escritura)
        image = withContext(Dispatchers.Default) {
            val paper = project.settings.paperSizePoints
            val scale = widthPx / paper.width.toFloat()
            val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), (paper.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            PolarRenderer.drawPage(
                AndroidCanvas(bitmap), project, page, isPreview = true, scale = scale,
                bitmapProvider = { container.bitmaps.load(it.path, BitmapLoader.PREVIEW_MAX) },
                templateBitmap = template, fonts = container.fonts
            )
            bitmap.asImageBitmap()
        }
    }
    return image
}
```

- [ ] **Step 8: `ToolPanel.kt` (contenedor; las Tasks 15–18 llenan cada panel)**

```kotlin
package com.polar.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R

@Composable
fun ToolPanel(tool: Tool, state: EditorUiState, vm: EditorViewModel, container: AppContainer, compact: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val title = when (tool) {
        Tool.PHOTOS -> stringResource(R.string.tool_photos)
        Tool.DESIGN -> stringResource(R.string.tool_design)
        Tool.TEXT -> if (state.editCard != null) stringResource(R.string.text_title_card, state.selectedCardNumber ?: 1) else stringResource(R.string.text_title_all)
        Tool.PAPER -> stringResource(R.string.tool_paper)
    }
    Surface(
        modifier = modifier,
        shape = if (compact) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) else RoundedCornerShape(0.dp),
        tonalElevation = 1.dp, shadowElevation = if (compact) 8.dp else 0.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            if (compact) Box(Modifier.padding(top = 10.dp).size(36.dp, 4.dp).align(Alignment.CenterHorizontally)
                .background(MaterialTheme.colorScheme.outline, CircleShape))
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (compact) IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.tool_close)) }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (tool) {
                    Tool.PHOTOS -> Text("Fotos")   // Task 15 → PhotosPanel(...)
                    Tool.DESIGN -> Text("Diseño")  // Task 16 → DesignPanel(...)
                    Tool.TEXT -> Text("Texto")     // Task 17 → TextPanel(...)
                    Tool.PAPER -> Text("Papel")    // Task 18 → PaperPanel(...)
                }
            }
        }
    }
}
```

Agrega a `strings.xml`:

```xml
    <string name="text_title_all">Texto · todas las tarjetas</string>
    <string name="text_title_card">Texto · tarjeta %1$d</string>
```

- [ ] **Step 9: `EditorScreen.kt`**

```kotlin
package com.polar.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.ui.LayoutKind
import com.polar.app.ui.LocalLayout
import com.polar.app.ui.Share
import com.polar.app.ui.catalog.CatalogContent
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(vm: EditorViewModel, container: AppContainer, notice: String?, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.action_undo)

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushAsync() }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it, duration = SnackbarDuration.Long) } }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is EditorEvent.Message -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val r = snackbar.showSnackbar(e.text, if (e.undoable) undoLabel else null)
                    if (r == SnackbarResult.ActionPerformed) vm.undo()
                }
                is EditorEvent.Exported -> Share.file(context, e.file, e.mime, e.file.name) // la Task 20 cambia esto
            }
        }
    }

    BackHandler(enabled = state.mode != EditorMode.EDIT) { vm.setMode(EditorMode.EDIT) }
    BackHandler(enabled = state.mode == EditorMode.EDIT && state.tool != null && LocalLayout.current == LayoutKind.COMPACT) { vm.setTool(null) }
    BackHandler(enabled = state.mode == EditorMode.EDIT && state.tool == null && state.selectedSlot != null) { vm.clearSelection() }

    when (state.mode) {
        EditorMode.CHANGE_DESIGN -> CatalogContent(
            title = stringResource(R.string.catalog_change), current = state.project.settings.style,
            thumbnails = container.thumbnails, showImport = false,
            onPick = vm::selectStyle, onImportTemplate = {}, onOpenPolar = {},
            onBack = { vm.setMode(EditorMode.EDIT) }
        )
        // La Task 19 agrega CROP y la Task 20 agrega FINISH; mientras tanto muestran el editor.
        else -> EditorLayout(state, vm, container, snackbar, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorLayout(state: EditorUiState, vm: EditorViewModel, container: AppContainer, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val expanded = LocalLayout.current == LayoutKind.EXPANDED
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        vm.addPhotos(uris.map { it.toString() })
    }
    val pickPhotos = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    var renaming by remember { mutableStateOf(false) }
    val noPhotos = state.project.photos.isEmpty()
    val hasSelection = state.selectedSlot != null
    val selectedHasPhoto = state.selectedPlacement != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { EditorTopBar(state, vm, onBack, onRename = { renaming = true }) }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (expanded) {
            Row(Modifier.padding(padding).fillMaxSize()) {
                ToolRail(state.tool ?: Tool.PHOTOS) { vm.setTool(it) }
                Column(Modifier.weight(1f)) {
                    SheetArea(state, vm, container, Modifier.weight(1f))
                    when {
                        noPhotos -> EmptyPhotosCta(pickPhotos)
                        hasSelection -> ContextBar(selectedHasPhoto, { vm.setTool(Tool.PHOTOS) }, { vm.setMode(EditorMode.CROP) }, vm::rotateSelected, vm::openTextForSelected, vm::removeSelectedPhoto)
                    }
                }
                ToolPanel(state.tool ?: Tool.PHOTOS, state, vm, container, compact = false, onClose = {}, modifier = Modifier.width(360.dp).fillMaxHeight())
            }
        } else {
            Column(Modifier.padding(padding).fillMaxSize()) {
                SheetArea(state, vm, container, Modifier.weight(1f))
                when {
                    state.tool != null -> ToolPanel(state.tool, state, vm, container, compact = true, onClose = { vm.setTool(null) }, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.46f))
                    noPhotos -> EmptyPhotosCta(pickPhotos)
                    hasSelection -> ContextBar(selectedHasPhoto, { vm.setTool(Tool.PHOTOS) }, { vm.setMode(EditorMode.CROP) }, vm::rotateSelected, vm::openTextForSelected, vm::removeSelectedPhoto)
                    else -> Hint()
                }
                ToolNavBar(state.tool) { vm.setTool(if (state.tool == it) null else it) }
            }
        }
    }

    if (renaming) {
        var text by remember { mutableStateOf(state.project.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.editor_rename)) },
            text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text(stringResource(R.string.home_rename_label)) }) },
            confirmButton = { TextButton(onClick = { vm.rename(text); renaming = false }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(state: EditorUiState, vm: EditorViewModel, onBack: () -> Unit, onRename: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.editor_back)) } },
        title = {
            Column(Modifier.clickable(onClick = onRename)) {
                Text(state.project.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    state.project.settings.style.displayName + " · " + stringResource(if (state.saving) R.string.editor_saving else R.string.editor_saved),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                )
            }
        },
        actions = {
            IconButton(onClick = vm::undo, enabled = state.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.action_undo)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.editor_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.action_redo)) }, { menu = false; vm.redo() }, enabled = state.canRedo,
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, null) })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_add_page)) }, { menu = false; vm.addPage() })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_clear_page)) }, { menu = false; vm.clearPage() })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_remove_page)) }, { menu = false; vm.removePage() })
                    DropdownMenuItem({ Text(stringResource(R.string.editor_rename)) }, { menu = false; onRename() })
                }
            }
            Button(onClick = { vm.setMode(EditorMode.FINISH) }, contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.padding(end = 8.dp)) {
                Icon(Icons.Outlined.Print, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.editor_print))
            }
        }
    )
}

@Composable
private fun Hint() {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.editor_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyPhotosCta(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(12.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.editor_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.editor_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onAdd) { Text(stringResource(R.string.editor_add_photos)) }
        }
    }
}
```

- [ ] **Step 10: Conectar el destino `EditorRoute`**

En `PolarNavHost.kt`, reemplaza `composable<EditorRoute> { Text("Editor") }` por:

```kotlin
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            val vm: EditorViewModel = viewModel(key = route.projectId, factory = viewModelFactory {
                initializer {
                    EditorViewModel(
                        route.projectId,
                        EditorDeps(
                            store = container.store,
                            photos = container.photos,
                            exports = container.exports,
                            thumbnail = { p, t ->
                                withContext(Dispatchers.Default) {
                                    container.thumbnails.projectPng(p, { a -> container.bitmaps.load(a.path, BitmapLoader.PREVIEW_MAX) }, t)
                                }
                            },
                            loadTemplate = { path -> container.bitmaps.load(path, BitmapLoader.EXPORT_MAX) }
                        )
                    )
                }
            })
            EditorScreen(vm, container, route.notice, onBack = { nav.popBackStack() })
        }
```

Imports: `androidx.navigation.toRoute`, `com.polar.app.ui.editor.*`, `com.polar.app.data.BitmapLoader`, `kotlinx.coroutines.Dispatchers`, `kotlinx.coroutines.withContext`.

- [ ] **Step 11: Correr pruebas, compilar y revisar en el emulador**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`, instala y abre.
Expected:
- Crear un diseño Polaroid abre el editor con la hoja blanca, los 9 marcadores y la tarjeta "Pon tus fotos".
- "Agregar fotos" abre el selector de Android; al elegir 12 fotos se acomodan en 2 hojas y deslizar cambia de hoja.
- Tocar una tarjeta la enmarca en vino y muestra la barra contextual; "Girar" gira la foto y "Quitar" muestra un Snackbar con "Deshacer".
- Doble toque acerca la hoja y otro doble toque la regresa.

- [ ] **Step 12: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(editor): hoja deslizable, selección de tarjetas, barra contextual y herramientas"
```

---
### Task 15: Panel Fotos

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/panels/PhotosPanel.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/panels/PanelColumn.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/ToolPanel.kt` (cada panel maneja su propio desplazamiento)
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/panels/PhotosPanelTest.kt`

**Interfaces:**
- Consumes: `EditorUiState.usedAssetIds`, `EditorViewModel.lowResSlots/addPhotos/fillAll/placePhoto` (Task 13); `BitmapLoader.load` (Task 7).
- Produces:
  - `@Composable fun PanelColumn(content: @Composable ColumnScope.() -> Unit)`.
  - `@Composable fun PhotosPanel(photos, used: Set<String>, lowRes: Set<String>, missingPhotos: Int, thumbnail: suspend (PhotoAsset) -> ImageBitmap?, onAdd, onFill, onPlace: (String) -> Unit)`.

- [ ] **Step 1: Prueba que falla**

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.model.PhotoAsset
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PhotosPanelTest {
    @get:Rule val compose = createComposeRule()
    private val photos = List(3) { PhotoAsset(path = "p$it.jpg", pixelWidth = 10, pixelHeight = 10) }

    @Test
    fun summaryAndPlacing() {
        var placed: String? = null
        compose.setContent {
            PolarTheme(ThemeMode.LIGHT) {
                PhotosPanel(photos, used = setOf(photos[0].id), lowRes = setOf(photos[1].id), missingPhotos = 2,
                    thumbnail = { null }, onAdd = {}, onFill = {}, onPlace = { placed = it })
            }
        }
        compose.onNodeWithText("3 fotos · 1 en la hoja").assertExists()
        compose.onNodeWithText("Faltan 2 fotos de este diseño. Agrégalas y colócalas de nuevo.").assertExists()
        compose.onNodeWithContentDescription("Foto 2, poca resolución").performClick()
        assertEquals(photos[1].id, placed)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.panels.PhotosPanelTest' --console=plain`
Expected: FAIL (no existe `PhotosPanel`).

- [ ] **Step 2: Textos (agregar a `strings.xml`)**

```xml
    <string name="photos_summary">%1$d fotos · %2$d en la hoja</string>
    <string name="photos_add">Agregar</string>
    <string name="photos_fill">Rellenar todo</string>
    <string name="photos_tip">Toca una foto para ponerla en la tarjeta elegida. Si no eliges tarjeta, usa Rellenar todo.</string>
    <string name="photos_missing">Faltan %1$d fotos de este diseño. Agrégalas y colócalas de nuevo.</string>
    <string name="photos_item">Foto %1$d</string>
    <string name="photos_in_use">, en la hoja</string>
    <string name="photos_low">, poca resolución</string>
    <string name="photos_low_badge">Baja</string>
```

- [ ] **Step 3: `PanelColumn.kt` y `PhotosPanel.kt`**

`PanelColumn.kt`:

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PanelColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}
```

`PhotosPanel.kt`:

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.model.PhotoAsset
import com.polar.app.ui.theme.PolarColors

@Composable
fun PhotosPanel(
    photos: List<PhotoAsset>,
    used: Set<String>,
    lowRes: Set<String>,
    missingPhotos: Int,
    thumbnail: suspend (PhotoAsset) -> ImageBitmap?,
    onAdd: () -> Unit,
    onFill: () -> Unit,
    onPlace: (String) -> Unit
) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = (maxWidth / 84.dp).toInt().coerceIn(3, 6)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    if (missingPhotos > 0) {
                        Surface(color = PolarColors.warningContainer, shape = MaterialTheme.shapes.medium) {
                            Text(stringResource(R.string.photos_missing, missingPhotos), color = PolarColors.onWarningContainer,
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.photos_summary, photos.size, used.size), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = onAdd) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.photos_add)) }
                        if (photos.isNotEmpty()) FilledTonalButton(onClick = onFill) {
                            Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.photos_fill))
                        }
                    }
                }
            }
            itemsIndexed(photos, key = { _, p -> p.id }) { i, photo ->
                val label = context.getString(R.string.photos_item, i + 1) +
                    (if (photo.id in used) context.getString(R.string.photos_in_use) else "") +
                    (if (photo.id in lowRes) context.getString(R.string.photos_low) else "")
                val image by produceState<ImageBitmap?>(null, photo.id) { value = thumbnail(photo) }
                Box(
                    Modifier.aspectRatio(1f).clip(MaterialTheme.shapes.small).background(PolarColors.table)
                        .clickable { onPlace(photo.id) }.semantics { contentDescription = label }
                ) {
                    image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    if (photo.id in used) Box(
                        Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) }
                    if (photo.id in lowRes) Surface(
                        color = PolarColors.warningContainer, shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    ) { Text(stringResource(R.string.photos_low_badge), color = PolarColors.onWarningContainer, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp)) }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.photos_tip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
```

- [ ] **Step 4: Cada panel con su propio desplazamiento**

En `ToolPanel.kt` reemplaza el bloque `Column(Modifier.weight(1f).verticalScroll(...)...) { when (tool) { ... } }` por el siguiente. Los paneles de las Tasks 16–18 usan `PanelColumn`.

```kotlin
            Box(Modifier.weight(1f)) {
                when (tool) {
                    Tool.PHOTOS -> {
                        val lowResIds = remember(state.project) {
                            vm.lowResSlots().mapNotNull { state.project.placements.getOrNull(it)?.assetID }.toSet()
                        }
                        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
                            vm.addPhotos(uris.map { it.toString() })
                        }
                        PhotosPanel(
                            photos = state.project.photos, used = state.usedAssetIds, lowRes = lowResIds,
                            missingPhotos = state.missingPhotos,
                            thumbnail = { a -> withContext(Dispatchers.IO) { container.bitmaps.load(a.path, 256)?.asImageBitmap() } },
                            onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onFill = vm::fillAll, onPlace = vm::placePhoto
                        )
                    }
                    Tool.DESIGN -> PanelColumn { Text("Diseño") } // Task 16
                    Tool.TEXT -> PanelColumn { Text("Texto") }    // Task 17
                    Tool.PAPER -> PanelColumn { Text("Papel") }   // Task 18
                }
            }
```

Imports: `androidx.activity.compose.rememberLauncherForActivityResult`, `androidx.activity.result.PickVisualMediaRequest`, `androidx.activity.result.contract.ActivityResultContracts`, `androidx.compose.runtime.remember`, `androidx.compose.ui.graphics.asImageBitmap`, `com.polar.app.ui.editor.panels.*`, `kotlinx.coroutines.Dispatchers`, `kotlinx.coroutines.withContext`. Borra los imports de `rememberScrollState`/`verticalScroll`, que ya no se usan.

- [ ] **Step 5: Correr pruebas y compilar**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: PASS y `BUILD SUCCESSFUL`. En el emulador, el panel Fotos muestra las miniaturas con ✓ en las fotos usadas; con una tarjeta elegida, tocar una foto la coloca y la selección pasa al siguiente espacio vacío.

- [ ] **Step 6: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(editor): panel Fotos con fotos usadas, baja resolución y fotos faltantes"
```

---

### Task 16: Panel Diseño (color, distribución, estilos rápidos, calendario y huecos de plantilla)

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/panels/DesignPanel.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/ToolPanel.kt` (rama `Tool.DESIGN`)
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/panels/DesignPanelTest.kt`

**Interfaces:**
- Consumes: `EditorUiState`; `MoodPreset`; `ProjectEdits.LAYOUT_PRESETS`; `ColorSwatch`, `ColorChoiceDialog`, `LabeledSlider`, `SwitchRow`, `MoreOptions`, `SectionLabel` (Task 14).
- Produces:
  - `class DesignCallbacks` con: `onChangeDesign`, `onAccent(String)`, `onLayout(Int)`, `onMood(MoodPreset)`, `onSuggested`, `onFormat(CardFormat)`, `onGrid(Int, Int)`, `onGap(Float)`, `onRounded(Boolean)`, `onYear(Int)`, `onHighlight(Boolean)`, `onSpecialDate(Long)`, `onEditRegions(Boolean)`, `onRegion((TemplateRegion) -> TemplateRegion)`, `onAddRegion`, `onRemoveRegion`, `onGestureStart`, `onGestureEnd`.
  - `@Composable fun DesignPanel(state: EditorUiState, cb: DesignCallbacks)`.

- [ ] **Step 1: Prueba que falla**

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.core.edit.MoodPreset
import com.polar.app.data.ThemeMode
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DesignPanelTest {
    @get:Rule val compose = createComposeRule()

    private fun callbacks(log: MutableList<String>) = DesignCallbacks(
        onChangeDesign = { log += "cambiar" }, onAccent = { log += "color:$it" }, onLayout = { log += "layout:$it" },
        onMood = { log += "mood:${it.name}" }, onSuggested = { log += "frases" }, onFormat = {}, onGrid = { _, _ -> },
        onGap = {}, onRounded = {}, onYear = {}, onHighlight = {}, onSpecialDate = {}, onEditRegions = { log += "huecos:$it" },
        onRegion = {}, onAddRegion = {}, onRemoveRegion = {}, onGestureStart = {}, onGestureEnd = {}
    )

    @Test
    fun layoutMoodAndChangeDesign() {
        val log = mutableListOf<String>()
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { DesignPanel(EditorUiState(loading = false), callbacks(log)) } }
        compose.onNodeWithText("Cambiar diseño").performClick()
        compose.onNodeWithText("4").performScrollTo().performClick()
        compose.onNodeWithText("Familia").performScrollTo().performClick()
        compose.onNodeWithText("Usar frases sugeridas").performScrollTo().performClick()
        assertEquals(listOf("cambiar", "layout:4", "mood:FAMILY", "frases"), log)
    }

    @Test
    fun importedTemplateShowsHoleEditing() {
        val log = mutableListOf<String>()
        val tpl = ImportedTemplate("t.png", 100, 100, listOf(TemplateRegion(x = 0.1, y = 0.1, width = 0.3, height = 0.3)))
        val state = EditorUiState(loading = false, project = PolarProject(settings = PrintSettings(style = TemplateStyle.IMPORTED, columns = 1, rows = 1, importedTemplate = tpl)).normalized())
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { DesignPanel(state, callbacks(log)) } }
        compose.onNodeWithText("4").assertDoesNotExist()
        compose.onNodeWithText("Editar los huecos").performScrollTo().performClick()
        assertEquals(listOf("huecos:true"), log)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.panels.DesignPanelTest' --console=plain`
Expected: FAIL.

- [ ] **Step 2: Textos (agregar a `strings.xml`)**

```xml
    <string name="design_current">Diseño actual</string>
    <string name="design_change">Cambiar diseño</string>
    <string name="design_color">Color</string>
    <string name="design_per_sheet">Por hoja</string>
    <string name="design_per_sheet_film">En las películas, cada tira reúne cinco fotos.</string>
    <string name="design_moods">Estilo rápido</string>
    <string name="design_moods_help">Cambia la letra y el color. Tus frases no se tocan.</string>
    <string name="design_suggested">Usar frases sugeridas</string>
    <string name="design_more_summary">Forma, filas, separación</string>
    <string name="design_format">Forma de la tarjeta</string>
    <string name="design_columns">Columnas</string>
    <string name="design_rows">Filas</string>
    <string name="design_gap">Separación entre tarjetas</string>
    <string name="design_rounded">Fotos con esquinas redondas</string>
    <string name="design_calendar">Calendario</string>
    <string name="design_year">Año</string>
    <string name="design_highlight">Marcar una fecha especial</string>
    <string name="design_pick_date">Elegir la fecha</string>
    <string name="design_template">Tu plantilla</string>
    <string name="design_edit_holes">Editar los huecos</string>
    <string name="design_holes_help">Toca un hueco de la hoja y arrástralo, o ajusta sus medidas aquí.</string>
    <string name="design_hole_n">Hueco %1$d de %2$d</string>
    <string name="design_left">Izquierda</string>
    <string name="design_top">Arriba</string>
    <string name="design_width">Ancho</string>
    <string name="design_height">Alto</string>
    <string name="design_add_hole">Agregar hueco</string>
    <string name="design_remove_hole">Quitar hueco</string>
    <string name="less">Menos</string>
    <string name="more">Más</string>
```

- [ ] **Step 3: `DesignPanel.kt`**

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.core.edit.MoodPreset
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import com.polar.app.ui.components.*
import com.polar.app.ui.editor.EditorUiState
import java.text.DateFormat
import java.util.Date

class DesignCallbacks(
    val onChangeDesign: () -> Unit,
    val onAccent: (String) -> Unit,
    val onLayout: (Int) -> Unit,
    val onMood: (MoodPreset) -> Unit,
    val onSuggested: () -> Unit,
    val onFormat: (CardFormat) -> Unit,
    val onGrid: (Int, Int) -> Unit,
    val onGap: (Float) -> Unit,
    val onRounded: (Boolean) -> Unit,
    val onYear: (Int) -> Unit,
    val onHighlight: (Boolean) -> Unit,
    val onSpecialDate: (Long) -> Unit,
    val onEditRegions: (Boolean) -> Unit,
    val onRegion: ((TemplateRegion) -> TemplateRegion) -> Unit,
    val onAddRegion: () -> Unit,
    val onRemoveRegion: () -> Unit,
    val onGestureStart: () -> Unit,
    val onGestureEnd: () -> Unit
)

private val DESIGN_PALETTE = listOf("92394A" to "Vino", "C34048" to "Rojo", "486855" to "Verde", "38536F" to "Azul", "20242C" to "Carbón", "BC8952" to "Dorado")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DesignPanel(state: EditorUiState, cb: DesignCallbacks) {
    val s = state.project.settings
    val imported = s.style == TemplateStyle.IMPORTED
    var more by rememberSaveable { mutableStateOf(false) }
    var otherColor by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    PanelColumn {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.design_current), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.style.displayName, style = MaterialTheme.typography.titleMedium)
                }
                FilledTonalButton(onClick = cb.onChangeDesign) { Text(stringResource(R.string.design_change)) }
            }
        }

        SectionLabel(stringResource(R.string.design_color))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DESIGN_PALETTE.forEach { (hex, name) -> ColorSwatch(hex, name, s.accentHex.equals(hex, true)) { cb.onAccent(hex) } }
            TextButton(onClick = { otherColor = true }, modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.color_other)) }
        }

        if (!imported) {
            SectionLabel(stringResource(R.string.design_per_sheet))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectEdits.LAYOUT_PRESETS.forEach { n ->
                    FilterChip(selected = s.columns * s.rows == n, onClick = { cb.onLayout(n) }, label = { Text("$n") })
                }
            }
            if (s.style.photosPerCard > 1) Text(stringResource(R.string.design_per_sheet_film), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionLabel(stringResource(R.string.design_moods))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoodPreset.entries.forEach { m ->
                FilterChip(selected = s.accentHex.equals(m.hex, true) && s.textStyle(TextRole.TITLE).fontName == m.fontName, onClick = { cb.onMood(m) }, label = { Text(m.displayName) })
            }
        }
        Text(stringResource(R.string.design_moods_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = cb.onSuggested) { Text(stringResource(R.string.design_suggested)) }

        if (s.style == TemplateStyle.CALENDAR) {
            SectionLabel(stringResource(R.string.design_calendar))
            Stepper(stringResource(R.string.design_year), s.calendarYear, 1900..2100) { cb.onYear(it) }
            SwitchRow(stringResource(R.string.design_highlight), s.highlightDate, cb.onHighlight)
            if (s.highlightDate) {
                OutlinedButton(onClick = { pickDate = true }) {
                    Text(stringResource(R.string.design_pick_date) + " · " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(SwiftDate.toEpochMs(s.specialDate))))
                }
            }
        }

        if (imported) {
            SectionLabel(stringResource(R.string.design_template))
            SwitchRow(stringResource(R.string.design_edit_holes), state.editingRegions, cb.onEditRegions, stringResource(R.string.design_holes_help))
            val regions = s.importedTemplate?.regions.orEmpty()
            val index = state.selectedSlot?.rem(s.capacity)
            if (state.editingRegions && index != null && index in regions.indices) {
                val r = regions[index]
                Text(stringResource(R.string.design_hole_n, index + 1, regions.size), style = MaterialTheme.typography.titleSmall)
                RegionSlider(stringResource(R.string.design_left), r.x, cb) { v -> { it.copy(x = v) } }
                RegionSlider(stringResource(R.string.design_top), r.y, cb) { v -> { it.copy(y = v) } }
                RegionSlider(stringResource(R.string.design_width), r.width, cb) { v -> { it.copy(width = v) } }
                RegionSlider(stringResource(R.string.design_height), r.height, cb) { v -> { it.copy(height = v) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = cb.onAddRegion, enabled = regions.size < 64) { Text(stringResource(R.string.design_add_hole)) }
                OutlinedButton(onClick = cb.onRemoveRegion, enabled = regions.size > 1 && state.selectedSlot != null) { Text(stringResource(R.string.design_remove_hole)) }
            }
        } else {
            MoreOptions(more, { more = !more }, stringResource(R.string.design_more_summary)) {
                SectionLabel(stringResource(R.string.design_format))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardFormat.entries.forEach { f -> FilterChip(s.cardFormat == f, { cb.onFormat(f) }, { Text(f.displayName) }) }
                }
                Stepper(stringResource(R.string.design_columns), s.columns, 1..4) { cb.onGrid(it, s.rows) }
                Stepper(stringResource(R.string.design_rows), s.rows, 1..6) { cb.onGrid(s.columns, it) }
                LabeledSlider(stringResource(R.string.design_gap), s.gap.toFloat(), 0f..30f, "${s.gap.toInt()} pt", cb.onGap, cb.onGestureStart, cb.onGestureEnd)
                SwitchRow(stringResource(R.string.design_rounded), s.roundedPhotos, cb.onRounded)
            }
        }
    }

    if (otherColor) ColorChoiceDialog(s.accentHex, cb.onAccent) { otherColor = false }
    if (pickDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = SwiftDate.toEpochMs(s.specialDate))
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let(cb.onSpecialDate); pickDate = false }) { Text(stringResource(R.string.action_ok)) } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(picker) }
    }
}

@Composable
private fun RegionSlider(label: String, value: Double, cb: DesignCallbacks, change: (Double) -> (TemplateRegion) -> TemplateRegion) {
    LabeledSlider(label, (value * 100).toFloat(), 0f..100f, "${(value * 100).toInt()} %",
        onChange = { cb.onRegion(change(it / 100.0)) }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) { Icon(Icons.Filled.Remove, stringResource(R.string.less) + " " + label) }
        Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 48.dp))
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) { Icon(Icons.Filled.Add, stringResource(R.string.more) + " " + label) }
    }
}
```

- [ ] **Step 4: Conectar en `ToolPanel`**

Reemplaza `Tool.DESIGN -> PanelColumn { Text("Diseño") }` por:

```kotlin
                    Tool.DESIGN -> DesignPanel(state, DesignCallbacks(
                        onChangeDesign = { vm.setMode(EditorMode.CHANGE_DESIGN) }, onAccent = vm::setAccent, onLayout = vm::applyLayout,
                        onMood = vm::applyMood, onSuggested = vm::applySuggestedPhrases, onFormat = vm::setCardFormat,
                        onGrid = vm::setGrid, onGap = { vm.setGap(it.toDouble()) }, onRounded = vm::setRounded,
                        onYear = vm::setCalendarYear, onHighlight = vm::setHighlightDate, onSpecialDate = vm::setSpecialDate,
                        onEditRegions = vm::setEditingRegions,
                        onRegion = { change -> state.selectedSlot?.let { vm.editRegion(it % state.project.settings.capacity, change) } },
                        onAddRegion = vm::addRegion,
                        onRemoveRegion = { state.selectedSlot?.let { vm.removeRegion(it % state.project.settings.capacity) } },
                        onGestureStart = vm::beginGesture, onGestureEnd = vm::endGesture
                    ))
```

- [ ] **Step 5: Correr pruebas y compilar**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: PASS y `BUILD SUCCESSFUL`. En el emulador: el color cambia el título; "4" deja 4 tarjetas por hoja; "Cambiar diseño" abre el catálogo y conserva fotos y frases; con una plantilla importada se pueden arrastrar los huecos.

- [ ] **Step 6: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(editor): panel Diseño con color, distribución, estilos, calendario y huecos"
```

---

### Task 17: Panel Texto (todas las tarjetas o sólo una, color, fuente y fecha)

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/panels/TextPanelState.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/panels/TextPanel.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/ToolPanel.kt` (rama `Tool.TEXT`)
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/panels/TextPanelStateTest.kt`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/panels/TextPanelTest.kt`

**Interfaces:**
- Consumes: `TextResolver`, `DateText`, `FontCatalog`, `StyleInfo.textRoles`, controles de la Task 14.
- Produces:
  - `data class TextPanelState(roles, role, cardScope: Boolean, cardNumber: Int?, hasSelection, text, generalText, hasOwnText, ownCount, appearance, hasOwnAppearance, dateSource, dateStyle, dateSamples: List<String>, accentHex, showSongUrl, songUrl, emptyReason: String?)`.
  - `fun textPanelState(state: EditorUiState, zone: TimeZone = TimeZone.getDefault()): TextPanelState`.
  - `class TextCallbacks(onRole, onScope, onText, onFocus(Boolean), onRevert, onApplyAll, onAppearance, onReset, onDateSource, onChosenDate(Long), onDateStyle, onSongUrl, onGestureStart, onGestureEnd)`.
  - `@Composable fun TextPanel(s: TextPanelState, cb: TextCallbacks)`.

- [ ] **Step 1: Pruebas que fallan**

`TextPanelStateTest.kt`:

```kotlin
package com.polar.app.ui.editor.panels

import com.polar.app.core.edit.ProjectEdits
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.editor.TextScope
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class TextPanelStateTest {
    private val base = PolarProject(settings = PrintSettings(title = "Lu y Max")).normalized()

    @Test
    fun cardScopeShowsOwnTextAndGeneralForComparison() {
        val p = ProjectEdits.setText(base, TextRole.TITLE, "El brindis", card = 1)
        val s = textPanelState(EditorUiState(loading = false, project = p, selectedSlot = 1, textScope = TextScope.CARD))
        assertTrue(s.cardScope)
        assertEquals(2, s.cardNumber)
        assertEquals("El brindis", s.text)
        assertEquals("Lu y Max", s.generalText)
        assertTrue(s.hasOwnText)
    }

    @Test
    fun allScopeCountsOwnTexts() {
        val p = ProjectEdits.setText(ProjectEdits.setText(base, TextRole.TITLE, "a", 0), TextRole.TITLE, "b", 4)
        val s = textPanelState(EditorUiState(loading = false, project = p))
        assertFalse(s.cardScope)
        assertEquals("Lu y Max", s.text)
        assertEquals(2, s.ownCount)
    }

    @Test
    fun rolesFollowDesignAndDateSamplesAreSpanish() {
        val s = textPanelState(EditorUiState(loading = false, project = base), TimeZone.getTimeZone("UTC"))
        assertEquals(listOf(TextRole.TITLE, TextRole.SUBTITLE, TextRole.DATE), s.roles)
        assertEquals(listOf("14 feb 2026", "14.02.26", "febrero 2026"), s.dateSamples)
        val film = textPanelState(EditorUiState(loading = false, project = base.withStyle(TemplateStyle.FILM_VERTICAL)))
        assertTrue(film.roles.isEmpty())
        assertNotNull(film.emptyReason)
    }
}
```

`TextPanelTest.kt`:

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.core.edit.ProjectEdits
import com.polar.app.data.ThemeMode
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import com.polar.app.ui.editor.TextScope
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TextPanelTest {
    @get:Rule val compose = createComposeRule()
    private val log = mutableListOf<String>()
    private val cb = TextCallbacks(
        onRole = { log += "rol:${it.key}" }, onScope = { log += "alcance:${it.name}" }, onText = { log += "texto:$it" },
        onFocus = {}, onRevert = { log += "volver" }, onApplyAll = { log += "todas" }, onAppearance = { log += "estilo" },
        onReset = {}, onDateSource = { log += "fecha:${it.name}" }, onChosenDate = {}, onDateStyle = {}, onSongUrl = {},
        onGestureStart = {}, onGestureEnd = {}
    )

    private fun show(state: EditorUiState) = compose.setContent { PolarTheme(ThemeMode.LIGHT) { TextPanel(textPanelState(state), cb) } }

    @Test
    fun typingAndApplyToAll() {
        val p = ProjectEdits.setText(PolarProject().normalized(), TextRole.TITLE, "propio", card = 3)
        show(EditorUiState(loading = false, project = p))
        compose.onNodeWithText("1 tarjeta tiene su propio texto").assertExists()
        compose.onNodeWithText("Aplicar a todas").performClick()
        compose.onNodeWithText("Nuestros momentos").performTextReplacement("Lu y Max")
        assertEquals(listOf("todas", "texto:Lu y Max"), log)
    }

    @Test
    fun ownTextShowsRevert() {
        val p = ProjectEdits.setText(PolarProject().normalized(), TextRole.TITLE, "El brindis", card = 1)
        show(EditorUiState(loading = false, project = p, selectedSlot = 1, textScope = TextScope.CARD))
        compose.onNodeWithText("Sólo tarjeta 2").assertIsSelected()
        compose.onNodeWithText("Volver al texto general").performClick()
        assertEquals(listOf("volver"), log)
    }

    @Test
    fun dateRoleOffersSources() {
        show(EditorUiState(loading = false, project = PolarProject().normalized(), textRole = TextRole.DATE))
        compose.onNodeWithText("De la foto").performClick()
        assertEquals(listOf("fecha:PHOTO"), log)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.panels.Text*' --console=plain`
Expected: FAIL.

- [ ] **Step 2: Textos (agregar a `strings.xml`)**

```xml
    <string name="text_scope_all">Todas las tarjetas</string>
    <string name="text_scope_card">Sólo tarjeta %1$d</string>
    <string name="text_scope_one">Sólo una tarjeta</string>
    <string name="text_for_all">%1$s para todas</string>
    <string name="text_for_card">%1$s de la tarjeta %2$d</string>
    <string name="text_others_keep">Las demás tarjetas siguen con «%1$s».</string>
    <string name="text_revert">Volver al texto general</string>
    <plurals name="text_own_count"><item quantity="one">%1$d tarjeta tiene su propio texto</item><item quantity="other">%1$d tarjetas tienen su propio texto</item></plurals>
    <string name="text_apply_all">Aplicar a todas</string>
    <string name="text_which_date">Qué fecha mostrar</string>
    <string name="text_date_photo">De la foto</string>
    <string name="text_date_choose">Elegir fecha</string>
    <string name="text_date_none">Sin fecha</string>
    <string name="text_date_format">Formato</string>
    <string name="text_font">Letra</string>
    <string name="text_color">Color del texto</string>
    <string name="text_color_design">Del diseño</string>
    <string name="text_size">Tamaño y estilo</string>
    <string name="text_size_auto">Auto</string>
    <string name="text_size_s">Chica</string>
    <string name="text_size_m">Mediana</string>
    <string name="text_size_l">Grande</string>
    <string name="text_bold">Negrita</string>
    <string name="text_italic">Cursiva</string>
    <string name="text_align_auto">Del diseño</string>
    <string name="text_align_left">Izquierda</string>
    <string name="text_align_center">Centro</string>
    <string name="text_align_right">Derecha</string>
    <string name="text_more_summary">Mostrar, posición, tamaño exacto</string>
    <string name="text_visible">Mostrar este texto</string>
    <string name="text_exact_size">Tamaño exacto</string>
    <string name="text_move_x">Mover a los lados</string>
    <string name="text_move_y">Mover arriba o abajo</string>
    <string name="text_reset">Restablecer este texto</string>
    <string name="text_own_style">Esta tarjeta tiene su propio estilo.</string>
    <string name="text_song_url">Enlace de la canción (opcional)</string>
    <string name="text_song_url_help">Se imprime un QR que abre ese enlace.</string>
    <string name="text_fit_help">Si el texto no cabe, se reduce solo para no tapar la foto.</string>
```

- [ ] **Step 3: `TextPanelState.kt`**

```kotlin
package com.polar.app.ui.editor.panels

import com.polar.app.core.text.DateText
import com.polar.app.core.text.TextResolver
import com.polar.app.model.*
import com.polar.app.ui.editor.EditorUiState
import java.util.TimeZone

data class TextPanelState(
    val roles: List<TextRole>,
    val role: TextRole,
    val cardScope: Boolean,
    val cardNumber: Int?,
    val hasSelection: Boolean,
    val text: String,
    val generalText: String,
    val hasOwnText: Boolean,
    val ownCount: Int,
    val appearance: TextAppearance,
    val hasOwnAppearance: Boolean,
    val dateSource: DateSource,
    val dateStyle: DateStyle,
    val dateSamples: List<String>,
    val accentHex: String,
    val showSongUrl: Boolean,
    val songUrl: String,
    val emptyReason: String?
)

private const val SAMPLE_DATE_MS = 1_771_070_400_000L // 14 feb 2026, para mostrar los formatos

fun textPanelState(state: EditorUiState, zone: TimeZone = TimeZone.getDefault()): TextPanelState {
    val p = state.project
    val s = p.settings
    val roles = s.style.textRoles
    val role = state.textRole.takeIf { it in roles } ?: roles.firstOrNull() ?: TextRole.TITLE
    val card = state.editCard
    return TextPanelState(
        roles = roles,
        role = role,
        cardScope = card != null,
        cardNumber = state.selectedCardNumber,
        hasSelection = state.selectedSlot != null,
        text = if (card != null) TextResolver.text(p, card, role, zone) else s.text(role),
        generalText = s.text(role),
        hasOwnText = card != null && TextResolver.hasOwnText(p, card, role),
        ownCount = TextResolver.ownTextCount(p, role),
        appearance = if (card != null) TextResolver.appearance(p, card, role) else s.textStyle(role),
        hasOwnAppearance = card != null && TextResolver.hasOwnAppearance(p, card, role),
        dateSource = if (card != null) TextResolver.dateSource(p, card) else s.dateSource,
        dateStyle = s.dateStyle,
        dateSamples = DateStyle.entries.map { DateText.format(SAMPLE_DATE_MS, it, zone) },
        accentHex = s.accentHex,
        showSongUrl = s.style == TemplateStyle.SPOTIFY,
        songUrl = s.songURL,
        emptyReason = when {
            roles.isNotEmpty() -> null
            s.style == TemplateStyle.IMPORTED -> "El texto de tu plantilla ya viene impreso en la imagen."
            else -> "Este diseño reserva todo el espacio para las fotos."
        }
    )
}
```

- [ ] **Step 4: `TextPanel.kt`**

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polar.app.R
import com.polar.app.data.FontCatalog
import com.polar.app.data.FontChoice
import com.polar.app.model.*
import com.polar.app.ui.components.*
import com.polar.app.ui.editor.TextScope

class TextCallbacks(
    val onRole: (TextRole) -> Unit,
    val onScope: (TextScope) -> Unit,
    val onText: (String) -> Unit,
    val onFocus: (Boolean) -> Unit,
    val onRevert: () -> Unit,
    val onApplyAll: () -> Unit,
    val onAppearance: ((TextAppearance) -> TextAppearance) -> Unit,
    val onReset: () -> Unit,
    val onDateSource: (DateSource) -> Unit,
    val onChosenDate: (Long) -> Unit,
    val onDateStyle: (DateStyle) -> Unit,
    val onSongUrl: (String) -> Unit,
    val onGestureStart: () -> Unit,
    val onGestureEnd: () -> Unit
)

private val TEXT_COLORS = listOf("20242C" to "Carbón", "FFFFFF" to "Blanco", "92394A" to "Vino", "C34048" to "Rojo",
    "F28C28" to "Naranja de fechador", "BC8952" to "Dorado", "486855" to "Verde", "38536F" to "Azul")

fun FontChoice.family(): FontFamily = res?.let { FontFamily(Font(it)) } ?: FontFamily.Default

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TextPanel(s: TextPanelState, cb: TextCallbacks) {
    var more by rememberSaveable { mutableStateOf(false) }
    var otherColor by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val a = s.appearance
    val font = FontCatalog.find(a.fontName)

    PanelColumn {
        if (s.emptyReason != null) {
            Text(s.emptyReason, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@PanelColumn
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !s.cardScope, onClick = { cb.onScope(TextScope.ALL) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                Text(stringResource(R.string.text_scope_all), maxLines = 1)
            }
            SegmentedButton(selected = s.cardScope, onClick = { cb.onScope(TextScope.CARD) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                Text(if (s.cardNumber != null) stringResource(R.string.text_scope_card, s.cardNumber) else stringResource(R.string.text_scope_one), maxLines = 1)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.roles.forEach { r -> FilterChip(selected = r == s.role, onClick = { cb.onRole(r) }, label = { Text(r.displayName) }) }
        }

        if (s.role != TextRole.DATE) {
            OutlinedTextField(
                value = s.text, onValueChange = cb.onText,
                label = {
                    Text(if (s.cardScope) stringResource(R.string.text_for_card, s.role.displayName, s.cardNumber ?: 1)
                    else stringResource(R.string.text_for_all, s.role.displayName))
                },
                textStyle = TextStyle(fontFamily = font.family(), fontSize = (18 * font.previewScale).sp),
                modifier = Modifier.fillMaxWidth().onFocusChanged { cb.onFocus(it.isFocused) }
            )
            if (s.cardScope && s.hasOwnText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.text_others_keep, s.generalText), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = cb.onRevert) { Text(stringResource(R.string.text_revert)) }
                }
            }
            if (!s.cardScope && s.ownCount > 0) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(pluralStringResource(R.plurals.text_own_count, s.ownCount, s.ownCount), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = cb.onApplyAll) { Text(stringResource(R.string.text_apply_all)) }
                    }
                }
            }
            Text(stringResource(R.string.text_fit_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            SectionLabel(stringResource(R.string.text_which_date))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.dateSource == DateSource.PHOTO, { cb.onDateSource(DateSource.PHOTO) }, { Text(stringResource(R.string.text_date_photo)) })
                FilterChip(s.dateSource == DateSource.CHOSEN, { pickDate = true }, { Text(stringResource(R.string.text_date_choose)) })
                FilterChip(s.dateSource == DateSource.NONE, { cb.onDateSource(DateSource.NONE) }, { Text(stringResource(R.string.text_date_none)) })
            }
            SectionLabel(stringResource(R.string.text_date_format))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateStyle.entries.forEachIndexed { i, st -> FilterChip(s.dateStyle == st, { cb.onDateStyle(st) }, { Text(s.dateSamples[i]) }) }
            }
        }

        SectionLabel(stringResource(R.string.text_font))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FontCatalog.all, key = { it.id }) { choice ->
                FilterChip(
                    selected = font.id == choice.id,
                    onClick = { cb.onAppearance { it.copy(fontName = choice.id) } },
                    label = { Text(choice.displayName, fontFamily = choice.family(), fontSize = (15 * choice.previewScale).sp) }
                )
            }
        }

        SectionLabel(stringResource(R.string.text_color))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(a.hex.isEmpty(), { cb.onAppearance { it.copy(hex = "") } }, { Text(stringResource(R.string.text_color_design)) }, modifier = Modifier.height(48.dp))
            TEXT_COLORS.forEach { (hex, name) -> ColorSwatch(hex, name, a.hex.equals(hex, true)) { cb.onAppearance { it.copy(hex = hex) } } }
            TextButton(onClick = { otherColor = true }, modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.color_other)) }
        }

        SectionLabel(stringResource(R.string.text_size))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.0 to R.string.text_size_auto, 8.0 to R.string.text_size_s, 12.0 to R.string.text_size_m, 18.0 to R.string.text_size_l).forEach { (size, label) ->
                FilterChip(a.size == size, { cb.onAppearance { it.copy(size = size) } }, { Text(stringResource(label)) })
            }
            IconToggleButton(checked = a.bold, onCheckedChange = { v -> cb.onAppearance { it.copy(bold = v) } }) {
                Icon(Icons.Filled.FormatBold, stringResource(R.string.text_bold))
            }
            IconToggleButton(checked = a.italic, onCheckedChange = { v -> cb.onAppearance { it.copy(italic = v) } }) {
                Icon(Icons.Filled.FormatItalic, stringResource(R.string.text_italic))
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val options = listOf(TextAlignment.AUTOMATIC, TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT)
            options.forEachIndexed { i, al ->
                SegmentedButton(selected = a.alignment == al, onClick = { cb.onAppearance { it.copy(alignment = al) } }, shape = SegmentedButtonDefaults.itemShape(i, options.size), icon = {}) {
                    when (al) {
                        TextAlignment.AUTOMATIC -> Text(stringResource(R.string.text_size_auto))
                        TextAlignment.LEFT -> Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, stringResource(R.string.text_align_left))
                        TextAlignment.CENTER -> Icon(Icons.Filled.FormatAlignCenter, stringResource(R.string.text_align_center))
                        TextAlignment.RIGHT -> Icon(Icons.AutoMirrored.Filled.FormatAlignRight, stringResource(R.string.text_align_right))
                    }
                }
            }
        }

        if (s.cardScope && s.hasOwnAppearance) Text(stringResource(R.string.text_own_style), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        MoreOptions(more, { more = !more }, stringResource(R.string.text_more_summary)) {
            SwitchRow(stringResource(R.string.text_visible), a.visible, { v -> cb.onAppearance { it.copy(visible = v) } })
            LabeledSlider(stringResource(R.string.text_exact_size), if (a.size == 0.0) 6f else a.size.toFloat(), 6f..96f,
                if (a.size == 0.0) stringResource(R.string.text_size_auto) else "${a.size.toInt()} pt",
                onChange = { v -> cb.onAppearance { it.copy(size = v.toDouble().let(Math::round).toDouble()) } }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
            LabeledSlider(stringResource(R.string.text_move_x), a.offsetX.toFloat(), -60f..60f, "${a.offsetX.toInt()} pt",
                onChange = { v -> cb.onAppearance { it.copy(offsetX = v.toDouble()) } }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
            LabeledSlider(stringResource(R.string.text_move_y), a.offsetY.toFloat(), -60f..60f, "${a.offsetY.toInt()} pt",
                onChange = { v -> cb.onAppearance { it.copy(offsetY = v.toDouble()) } }, onStart = cb.onGestureStart, onEnd = cb.onGestureEnd)
            TextButton(onClick = cb.onReset) { Text(stringResource(R.string.text_reset)) }
        }

        if (s.showSongUrl) {
            OutlinedTextField(
                value = s.songUrl, onValueChange = cb.onSongUrl, singleLine = true,
                label = { Text(stringResource(R.string.text_song_url)) },
                supportingText = { Text(stringResource(R.string.text_song_url_help)) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { cb.onFocus(it.isFocused) }
            )
        }
    }

    if (otherColor) ColorChoiceDialog(a.hex.ifEmpty { s.accentHex }, { hex -> cb.onAppearance { it.copy(hex = hex) } }) { otherColor = false }
    if (pickDate) {
        val picker = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let(cb.onChosenDate); pickDate = false }) { Text(stringResource(R.string.action_ok)) } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(picker) }
    }
}
```

> El `return@PanelColumn` dentro de la lambda corta el contenido cuando el diseño no tiene textos. Si el compilador no acepta la etiqueta, envuelve el resto del contenido en `if (s.emptyReason == null) { … }`.

- [ ] **Step 5: Conectar en `ToolPanel`**

Reemplaza `Tool.TEXT -> PanelColumn { Text("Texto") }` por:

```kotlin
                    Tool.TEXT -> TextPanel(textPanelState(state), TextCallbacks(
                        onRole = vm::setTextRole, onScope = vm::setTextScope, onText = vm::setText,
                        onFocus = { focused -> if (focused) vm.beginGesture() else vm.endGesture() },
                        onRevert = vm::clearOwnText, onApplyAll = vm::applyTextToAll, onAppearance = vm::editAppearance,
                        onReset = vm::resetAppearance, onDateSource = vm::setDateSource, onChosenDate = vm::setChosenDate,
                        onDateStyle = vm::setDateStyle, onSongUrl = vm::setSongUrl,
                        onGestureStart = vm::beginGesture, onGestureEnd = vm::endGesture
                    ))
```

- [ ] **Step 6: Correr pruebas y compilar**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: PASS y `BUILD SUCCESSFUL`. En el emulador:
- Con "Todas las tarjetas", escribir cambia el título de todas.
- Al tocar la tarjeta 2 → "Texto", aparece "Sólo tarjeta 2": escribir cambia sólo esa tarjeta y aparece "Volver al texto general".
- La fecha "De la foto" muestra la fecha de captura bajo el título.
- Elegir "Caveat" y el color rojo cambia la vista previa.
- Escribir una palabra completa y deshacer la quita de un solo paso.

- [ ] **Step 7: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(editor): panel Texto con texto por tarjeta, color, fuentes y fecha"
```

---

### Task 18: Panel Papel

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/panels/PaperPanel.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/ToolPanel.kt` (rama `Tool.PAPER`)
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/panels/PaperPanelTest.kt`

**Interfaces:**
- Consumes: `PrintSettings`, `Units` (Task 9), controles de la Task 14.
- Produces:
  - `class PaperCallbacks(onPaper, onOrientation, onCustom(widthMM, heightMM), onGuides, onCutStyle, onBorders, onMargin(Float), onGestureStart, onGestureEnd)`.
  - `@Composable fun PaperPanel(settings: PrintSettings, units: Units, cb: PaperCallbacks)`.
  - `fun parseMeasure(text: String, units: Units): Double?` (mm, o null si está fuera de 80–600 mm).

- [ ] **Step 1: Pruebas que fallan**

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.data.ThemeMode
import com.polar.app.data.Units
import com.polar.app.model.*
import com.polar.app.ui.theme.PolarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PaperPanelTest {
    @get:Rule val compose = createComposeRule()
    private val log = mutableListOf<String>()
    private val cb = PaperCallbacks(
        onPaper = { log += "papel:${it.name}" }, onOrientation = { log += "orient:${it.name}" },
        onCustom = { w, h -> log += "custom:${w.toInt()}x${h.toInt()}" }, onGuides = {}, onCutStyle = {}, onBorders = {},
        onMargin = {}, onGestureStart = {}, onGestureEnd = {}
    )

    @Test
    fun choosingPaperAndOrientation() {
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { PaperPanel(PrintSettings(), Units.MM, cb) } }
        compose.onNodeWithText("A4").performClick()
        compose.onNodeWithText("Horizontal").performClick()
        assertEquals(listOf("papel:A4", "orient:LANDSCAPE"), log)
    }

    @Test
    fun customPaperValidatesRange() {
        compose.setContent { PolarTheme(ThemeMode.LIGHT) { PaperPanel(PrintSettings(paperSize = PaperSize.CUSTOM), Units.MM, cb) } }
        compose.onNodeWithText("Ancho (mm)").performTextReplacement("50")
        compose.onNodeWithText("Aplicar medidas").performClick()
        compose.onNodeWithText("Usa medidas entre 80 y 600 mm.").assertExists()
        compose.onNodeWithText("Ancho (mm)").performTextReplacement("150")
        compose.onNodeWithText("Aplicar medidas").performClick()
        assertEquals(listOf("custom:150x279"), log)
    }

    @Test
    fun parseMeasureConvertsInches() {
        assertEquals(152.4, parseMeasure("6", Units.INCHES)!!, 0.01)
        assertEquals(100.0, parseMeasure("100,0", Units.MM)!!, 0.01)
        assertNull(parseMeasure("2", Units.INCHES))
        assertNull(parseMeasure("abc", Units.MM))
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.panels.PaperPanelTest' --console=plain`
Expected: FAIL.

- [ ] **Step 2: Textos (agregar a `strings.xml`)**

```xml
    <string name="paper_size">Tamaño de hoja</string>
    <string name="paper_orientation">Orientación</string>
    <string name="paper_width">Ancho (%1$s)</string>
    <string name="paper_height">Alto (%1$s)</string>
    <string name="paper_apply">Aplicar medidas</string>
    <string name="paper_range_mm">Usa medidas entre 80 y 600 mm.</string>
    <string name="paper_range_in">Usa medidas entre 3.2 y 23.6 pulgadas.</string>
    <string name="paper_guides">Marcas de corte</string>
    <string name="paper_guides_help">Quedan fuera de las fotos</string>
    <string name="paper_tip">Al imprimir elige %1$s y «Tamaño real» o escala 100 %%.</string>
    <string name="paper_more_summary">Margen, bordes</string>
    <string name="paper_margin">Margen de la hoja</string>
    <string name="paper_borders">Imprimir el borde de cada tarjeta</string>
    <string name="paper_borders_help">Si lo apagas, el borde no aparece en la foto recortada.</string>
```

- [ ] **Step 3: `PaperPanel.kt`**

```kotlin
package com.polar.app.ui.editor.panels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polar.app.R
import com.polar.app.data.Units
import com.polar.app.model.*
import com.polar.app.ui.components.*
import java.util.Locale

class PaperCallbacks(
    val onPaper: (PaperSize) -> Unit,
    val onOrientation: (PaperOrientation) -> Unit,
    val onCustom: (Double, Double) -> Unit,
    val onGuides: (Boolean) -> Unit,
    val onCutStyle: (CutStyle) -> Unit,
    val onBorders: (Boolean) -> Unit,
    val onMargin: (Float) -> Unit,
    val onGestureStart: () -> Unit,
    val onGestureEnd: () -> Unit
)

fun parseMeasure(text: String, units: Units): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    val mm = if (units == Units.INCHES) value * 25.4 else value
    return mm.takeIf { it in 80.0..600.0 }
}

private fun show(mm: Double, units: Units): String =
    if (units == Units.INCHES) String.format(Locale.ROOT, "%.2f", mm / 25.4) else String.format(Locale.ROOT, "%.0f", mm)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PaperPanel(settings: PrintSettings, units: Units, cb: PaperCallbacks) {
    var more by rememberSaveable { mutableStateOf(false) }
    val unitLabel = if (units == Units.INCHES) "in" else "mm"
    var width by remember(settings.customWidthMM, units) { mutableStateOf(show(settings.customWidthMM, units)) }
    var height by remember(settings.customHeightMM, units) { mutableStateOf(show(settings.customHeightMM, units)) }
    var error by remember { mutableStateOf(false) }

    PanelColumn {
        SectionLabel(stringResource(R.string.paper_size))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaperSize.entries.forEach { p -> FilterChip(settings.paperSize == p, { cb.onPaper(p) }, { Text(p.displayName) }) }
        }
        if (settings.paperSize == PaperSize.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(width, { width = it; error = false }, label = { Text(stringResource(R.string.paper_width, unitLabel)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(height, { height = it; error = false }, label = { Text(stringResource(R.string.paper_height, unitLabel)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            if (error) Text(stringResource(if (units == Units.INCHES) R.string.paper_range_in else R.string.paper_range_mm),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                val w = parseMeasure(width, units); val h = parseMeasure(height, units)
                if (w == null || h == null) error = true else cb.onCustom(w, h)
            }) { Text(stringResource(R.string.paper_apply)) }
        }
        Text(settings.paperDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionLabel(stringResource(R.string.paper_orientation))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PaperOrientation.entries.forEachIndexed { i, o ->
                SegmentedButton(settings.orientation == o, { cb.onOrientation(o) }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(o.displayName) }
            }
        }

        SwitchRow(stringResource(R.string.paper_guides), settings.cutGuides, cb.onGuides, stringResource(R.string.paper_guides_help))
        if (settings.cutGuides) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CutStyle.entries.forEach { c -> FilterChip(settings.cutStyle == c, { cb.onCutStyle(c) }, { Text(c.displayName) }) }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Print, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.paper_tip, settings.paperSize.displayName), style = MaterialTheme.typography.bodyMedium)
            }
        }

        MoreOptions(more, { more = !more }, stringResource(R.string.paper_more_summary)) {
            LabeledSlider(stringResource(R.string.paper_margin), settings.margin.toFloat(), 0f..60f, "${settings.margin.toInt()} pt",
                cb.onMargin, cb.onGestureStart, cb.onGestureEnd)
            SwitchRow(stringResource(R.string.paper_borders), settings.drawBorders, cb.onBorders, stringResource(R.string.paper_borders_help))
        }
    }
}
```

- [ ] **Step 4: Conectar en `ToolPanel`**

Reemplaza `Tool.PAPER -> PanelColumn { Text("Papel") }` por:

```kotlin
                    Tool.PAPER -> {
                        val app by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
                        PaperPanel(state.project.settings, app.units, PaperCallbacks(
                            onPaper = vm::setPaper, onOrientation = vm::setOrientation, onCustom = vm::setCustomPaper,
                            onGuides = vm::setGuides, onCutStyle = vm::setCutStyle, onBorders = vm::setBorders,
                            onMargin = { vm.setMargin(it.toDouble()) }, onGestureStart = vm::beginGesture, onGestureEnd = vm::endGesture
                        ))
                    }
```

Imports: `androidx.lifecycle.compose.collectAsStateWithLifecycle`, `androidx.compose.runtime.getValue`, `com.polar.app.data.AppSettings`.

- [ ] **Step 5: Correr pruebas y compilar**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: PASS y `BUILD SUCCESSFUL`. En el emulador: A4 y "Horizontal" cambian la hoja al instante, y "Personalizado" acepta 150 × 200 mm y rechaza 50.

- [ ] **Step 6: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(editor): panel Papel con tamaños, medidas propias, guías y márgenes"
```

---
### Task 19: Encuadrar

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/CropMath.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/CropScreen.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/EditorScreen.kt` (rama `EditorMode.CROP`)
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/ui/editor/CropMathTest.kt`

**Interfaces:**
- Consumes: `EditorViewModel.editSelectedPlacement/rotateSelected/resetSelectedPlacement/dpiOf/beginGesture/endGesture/setMode`, `SheetGeometry.slotRects`, `DarkColors` (Task 10).
- Produces:
  - `data class CropDraw(centerX: Float, centerY: Float, width: Float, height: Float, degrees: Float)`.
  - `object CropMath { fun draw(boxW: Float, boxH: Float, bmpW: Int, bmpH: Int, p: PhotoPlacement): CropDraw }`. Usa la misma matemática que `PolarRenderer.drawPhoto`.
  - `@Composable fun CropScreen(state, vm, container)`.

- [ ] **Step 1: Prueba que falla**

```kotlin
package com.polar.app.ui.editor

import com.polar.app.model.PhotoPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {
    @Test
    fun coversBoxAtZoomOne() {
        val d = CropMath.draw(100f, 100f, 400, 200, PhotoPlacement("x"))
        assertEquals(50f, d.centerX, 0.01f); assertEquals(50f, d.centerY, 0.01f)
        assertEquals(200f, d.width, 0.01f); assertEquals(100f, d.height, 0.01f) // cubre el alto, sobra a los lados
    }

    @Test
    fun offsetMovesWithinOverflowAndZoomScales() {
        val d = CropMath.draw(100f, 100f, 400, 200, PhotoPlacement("x", zoom = 2.0, offsetX = 1.0))
        assertEquals(400f, d.width, 0.01f)
        assertEquals(50f + (400f - 100f) / 2f, d.centerX, 0.01f)
    }

    @Test
    fun quarterTurnSwapsSides() {
        val d = CropMath.draw(100f, 200f, 400, 200, PhotoPlacement("x", quarterTurns = 1))
        assertEquals(90f, d.degrees, 0f)
        // girada, la foto mide 200×400 y cubre 100×200 con escala 0.5: el bitmap se dibuja de 200×100
        assertEquals(200f, d.width, 0.01f); assertEquals(100f, d.height, 0.01f)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.ui.editor.CropMathTest' --console=plain`
Expected: FAIL.

- [ ] **Step 2: `CropMath.kt`**

```kotlin
package com.polar.app.ui.editor

import com.polar.app.model.PhotoPlacement
import kotlin.math.max

data class CropDraw(val centerX: Float, val centerY: Float, val width: Float, val height: Float, val degrees: Float)

/** Igual que PolarRenderer.drawPhoto: cubrir el hueco, zoom, desplazamiento dentro de lo que sobra y giro. */
object CropMath {
    fun draw(boxW: Float, boxH: Float, bmpW: Int, bmpH: Int, p: PhotoPlacement): CropDraw {
        val rotated = p.quarterTurns % 2 != 0
        val srcW = if (rotated) bmpH.toFloat() else bmpW.toFloat()
        val srcH = if (rotated) bmpW.toFloat() else bmpH.toFloat()
        val fit = max(boxW / srcW, boxH / srcH) * p.zoom.toFloat()
        val drawnW = srcW * fit
        val drawnH = srcH * fit
        val cx = boxW / 2f + p.offsetX.toFloat() * (drawnW - boxW) / 2f
        val cy = boxH / 2f + p.offsetY.toFloat() * (drawnH - boxH) / 2f
        return CropDraw(cx, cy, bmpW * fit, bmpH * fit, (p.quarterTurns * 90).toFloat())
    }
}
```

- [ ] **Step 3: Textos (agregar a `strings.xml`)**

```xml
    <string name="crop_title">Encuadrar · tarjeta %1$d</string>
    <string name="crop_done">Listo</string>
    <string name="crop_zoom">Acercar</string>
    <string name="crop_horizontal">Mover a los lados</string>
    <string name="crop_vertical">Mover arriba o abajo</string>
    <string name="crop_rotate">Girar</string>
    <string name="crop_reset">Restablecer</string>
    <string name="crop_quality_ok">Calidad buena para imprimir</string>
    <string name="crop_quality_low">Poca resolución para este tamaño: puede verse borrosa</string>
```

- [ ] **Step 4: `CropScreen.kt`**

```kotlin
package com.polar.app.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.data.BitmapLoader
import com.polar.app.ui.components.LabeledSlider
import com.polar.app.ui.theme.DarkColors
import com.polar.app.ui.theme.DarkExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(state: EditorUiState, vm: EditorViewModel, container: AppContainer) {
    val slot = state.selectedSlot
    val placement = state.selectedPlacement
    val asset = state.project.asset(placement)
    if (slot == null || placement == null || asset == null) { LaunchedEffect(Unit) { vm.setMode(EditorMode.EDIT) }; return }
    val rect = remember(state.project.settings, slot) { SheetGeometry.slotRects(state.project.settings)[slot % state.project.settings.capacity] }
    val aspect = (rect.width / rect.height).toFloat()
    val bitmap by produceState<Bitmap?>(null, asset.path) { value = withContext(Dispatchers.IO) { container.bitmaps.load(asset.path, BitmapLoader.PREVIEW_MAX) } }
    val dpi = vm.dpiOf(slot)
    val low = dpi != null && dpi < 150

    MaterialTheme(colorScheme = DarkColors) {
        Scaffold(
            containerColor = DarkColors.background,
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { vm.setMode(EditorMode.EDIT) }) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) } },
                    title = { Text(stringResource(R.string.crop_title, state.selectedCardNumber ?: 1)) },
                    actions = { TextButton(onClick = { vm.setMode(EditorMode.EDIT) }) { Text(stringResource(R.string.crop_done)) } }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(380.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.aspectRatio(aspect).fillMaxHeight().background(DarkExtra.table).clipToBounds()) {
                        Canvas(Modifier.fillMaxSize()) {
                            val bmp = bitmap ?: return@Canvas
                            val d = CropMath.draw(size.width, size.height, bmp.width, bmp.height, placement)
                            translate(d.centerX, d.centerY) {
                                rotate(d.degrees, pivot = Offset.Zero) {
                                    drawImage(
                                        bmp.asImageBitmap(),
                                        dstOffset = IntOffset((-d.width / 2).roundToInt(), (-d.height / 2).roundToInt()),
                                        dstSize = IntSize(d.width.roundToInt(), d.height.roundToInt())
                                    )
                                }
                            }
                            val line = Color.White.copy(alpha = 0.55f)
                            for (i in 1..2) {
                                drawLine(line, Offset(size.width * i / 3f, 0f), Offset(size.width * i / 3f, size.height))
                                drawLine(line, Offset(0f, size.height * i / 3f), Offset(size.width, size.height * i / 3f))
                            }
                        }
                    }
                }
                Surface(
                    color = if (low) DarkExtra.warningContainer else DarkExtra.successContainer,
                    shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(if (low) R.string.crop_quality_low else R.string.crop_quality_ok),
                        color = if (low) DarkExtra.onWarningContainer else DarkExtra.onSuccessContainer,
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LabeledSlider(stringResource(R.string.crop_zoom), placement.zoom.toFloat(), 1f..4f, String.format(Locale.ROOT, "%.1f×", placement.zoom),
                        onChange = { v -> vm.editSelectedPlacement { it.copy(zoom = v.toDouble()) } }, onStart = vm::beginGesture, onEnd = vm::endGesture)
                    LabeledSlider(stringResource(R.string.crop_horizontal), placement.offsetX.toFloat(), -1f..1f, "${(placement.offsetX * 100).roundToInt()} %",
                        onChange = { v -> vm.editSelectedPlacement { it.copy(offsetX = v.toDouble()) } }, onStart = vm::beginGesture, onEnd = vm::endGesture)
                    LabeledSlider(stringResource(R.string.crop_vertical), placement.offsetY.toFloat(), -1f..1f, "${(placement.offsetY * 100).roundToInt()} %",
                        onChange = { v -> vm.editSelectedPlacement { it.copy(offsetY = v.toDouble()) } }, onStart = vm::beginGesture, onEnd = vm::endGesture)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = vm::rotateSelected, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.RotateRight, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.crop_rotate))
                        }
                        OutlinedButton(onClick = vm::resetSelectedPlacement, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.crop_reset)) }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: Conectar en `EditorScreen`**

En el `when (state.mode)` de `EditorScreen`, antes de `else ->`, agrega:

```kotlin
        EditorMode.CROP -> CropScreen(state, vm, container)
```

- [ ] **Step 6: Correr pruebas y compilar**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`
Expected: PASS y `BUILD SUCCESSFUL`. En el emulador: "Encuadrar" abre la pantalla oscura; el zoom acerca la foto, "Girar" la gira y "Listo" vuelve a la hoja con el encuadre aplicado. Un zoom alto en una foto chica muestra el aviso ámbar.

- [ ] **Step 7: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(editor): pantalla Encuadrar con aviso de calidad"
```

---

### Task 20: Terminar: imprimir, guardar como y compartir

**Files:**
- Create: `PolarAndroid/app/src/main/java/com/polar/app/export/FilePrintAdapter.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/export/PrintMedia.kt`
- Create: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/FinishScreen.kt`
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/EditorScreen.kt` (rama `FINISH` y manejo de `Exported`)
- Modify: `PolarAndroid/app/src/main/java/com/polar/app/ui/editor/EditorState.kt` (`enum ExportAction`)
- Modify: `PolarAndroid/app/src/main/res/values/strings.xml`
- Test: `PolarAndroid/app/src/test/java/com/polar/app/export/PrintMediaTest.kt`

**Interfaces:**
- Consumes: `EditorViewModel.exportPdf/exportPng/lowResSlots/emptySlotsOnUsedPages/selectSlot/setMode`, `Thumbnailer.page`, `Share`.
- Produces:
  - `enum class ExportAction { PRINT, SAVE, SHARE }` (en `EditorState.kt`).
  - `class FilePrintAdapter(file, name) : PrintDocumentAdapter`.
  - `object PrintMedia { fun mediaFor(settings: PrintSettings): PrintAttributes.MediaSize?; fun attributes(settings): PrintAttributes }`.
  - `@Composable fun FinishScreen(state, vm, container, onAction: (ExportAction, pdf: Boolean) -> Unit)`.

- [ ] **Step 1: Prueba que falla**

```kotlin
package com.polar.app.export

import android.print.PrintAttributes
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polar.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PrintMediaTest {
    @Test
    fun mapsPolarPaperToAndroidMedia() {
        assertEquals(PrintAttributes.MediaSize.NA_LETTER.id, PrintMedia.mediaFor(PrintSettings())!!.id)
        assertEquals(PrintAttributes.MediaSize.ISO_A4.id, PrintMedia.mediaFor(PrintSettings(paperSize = PaperSize.A4))!!.id)
        assertFalse(PrintMedia.mediaFor(PrintSettings(paperSize = PaperSize.A4, orientation = PaperOrientation.LANDSCAPE))!!.isPortrait)
        assertNull(PrintMedia.mediaFor(PrintSettings(paperSize = PaperSize.CUSTOM)))
    }
}
```

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.export.PrintMediaTest' --console=plain`
Expected: FAIL.

- [ ] **Step 2: `PrintMedia.kt` y `FilePrintAdapter.kt`**

```kotlin
package com.polar.app.export

import android.print.PrintAttributes
import com.polar.app.model.PaperOrientation
import com.polar.app.model.PaperSize
import com.polar.app.model.PrintSettings

/** Papel de Polar → papel de Android, para que la impresora proponga el tamaño correcto. */
object PrintMedia {
    fun mediaFor(settings: PrintSettings): PrintAttributes.MediaSize? {
        val base = when (settings.paperSize) {
            PaperSize.LETTER -> PrintAttributes.MediaSize.NA_LETTER
            PaperSize.LEGAL -> PrintAttributes.MediaSize.NA_LEGAL
            PaperSize.A4 -> PrintAttributes.MediaSize.ISO_A4
            PaperSize.A3 -> PrintAttributes.MediaSize.ISO_A3
            PaperSize.PHOTO4X6 -> PrintAttributes.MediaSize.NA_INDEX_4X6
            PaperSize.OFICIO, PaperSize.PHOTO5X7, PaperSize.CUSTOM -> return null
        }
        return if (settings.orientation == PaperOrientation.LANDSCAPE) base.asLandscape() else base.asPortrait()
    }

    fun attributes(settings: PrintSettings): PrintAttributes = PrintAttributes.Builder().apply {
        mediaFor(settings)?.let { setMediaSize(it) }
        setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        setMinMargins(PrintAttributes.Margins.NO_MARGINS)
    }.build()
}
```

```kotlin
package com.polar.app.export

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File
import java.io.FileOutputStream

/** Manda a la impresora el PDF que Polar ya generó a tamaño real. */
class FilePrintAdapter(private val file: File, private val name: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback, extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
        val info = PrintDocumentInfo.Builder(name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
        try {
            file.inputStream().use { input -> FileOutputStream(destination.fileDescriptor).use { input.copyTo(it) } }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}
```

Agrega a `EditorState.kt`: `enum class ExportAction { PRINT, SAVE, SHARE }`.

Run: `./gradlew testDebugUnitTest --tests 'com.polar.app.export.PrintMediaTest' --console=plain`
Expected: PASS.

- [ ] **Step 3: Textos (agregar a `strings.xml`)**

```xml
    <string name="finish_title">Terminar</string>
    <string name="finish_summary">%1$s · %2$d fotos · %3$s</string>
    <string name="finish_low">%1$d fotos pueden verse borrosas a este tamaño</string>
    <string name="finish_review">Revisar</string>
    <string name="finish_empty">%1$d espacios vacíos: no se imprimen</string>
    <string name="finish_missing">Faltan %1$d fotos: se imprime un espacio en blanco</string>
    <string name="finish_ok">Textos y marcas de corte dentro de la hoja</string>
    <string name="finish_print">Imprimir</string>
    <string name="finish_save_pdf">Guardar PDF</string>
    <string name="finish_save_pdf_hint">todas las hojas</string>
    <string name="finish_save_png">Guardar imagen PNG</string>
    <string name="finish_save_png_hint">esta hoja · 300 ppp</string>
    <string name="finish_share">Compartir PDF</string>
    <string name="finish_share_hint">WhatsApp, correo, Drive</string>
    <string name="finish_tip">Al imprimir elige %1$s y «Tamaño real» o escala 100 %%. Así cada tarjeta sale de su medida.</string>
    <string name="finish_saved">Archivo guardado</string>
    <string name="finish_open">Abrir</string>
    <string name="finish_preparing">Preparando el archivo…</string>
```

- [ ] **Step 4: `FinishScreen.kt`**

```kotlin
package com.polar.app.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polar.app.AppContainer
import com.polar.app.R
import com.polar.app.data.BitmapLoader
import com.polar.app.ui.LayoutKind
import com.polar.app.ui.LocalLayout
import com.polar.app.ui.theme.PolarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishScreen(state: EditorUiState, vm: EditorViewModel, container: AppContainer, onAction: (ExportAction, Boolean) -> Unit) {
    val p = state.project
    val low = remember(p) { vm.lowResSlots() }
    val empty = remember(p) { vm.emptySlotsOnUsedPages() }
    val expanded = LocalLayout.current == LayoutKind.EXPANDED
    val sheets = pluralStringResource(R.plurals.home_sheets, p.pageCount, p.pageCount)
    val paper = p.settings.paperSize.displayName + " " + p.settings.orientation.displayName.lowercase()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.finish_title)) },
            navigationIcon = { IconButton(onClick = { vm.setMode(EditorMode.EDIT) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } }
        )
    }) { padding ->
        val summary: @Composable ColumnScope.() -> Unit = {
            LazyRow(
                Modifier.fillMaxWidth().background(PolarColors.table, MaterialTheme.shapes.large).padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items((0 until minOf(p.pageCount, 12)).toList()) { page -> PageThumb(state, vm, container, page) }
            }
            Text(stringResource(R.string.finish_summary, sheets, p.placedCount, paper), style = MaterialTheme.typography.titleMedium)
            OutlinedCard {
                if (low.isNotEmpty()) CheckRow(Icons.Outlined.Warning, PolarColors.warningContainer, PolarColors.onWarningContainer,
                    stringResource(R.string.finish_low, low.size)) {
                    TextButton(onClick = { vm.selectSlot(low.first()); vm.setMode(EditorMode.CROP) }) { Text(stringResource(R.string.finish_review)) }
                }
                if (state.missingPhotos > 0) CheckRow(Icons.Outlined.ImageNotSupported, PolarColors.warningContainer, PolarColors.onWarningContainer,
                    stringResource(R.string.finish_missing, state.missingPhotos))
                if (empty > 0) CheckRow(Icons.Outlined.Info, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant,
                    stringResource(R.string.finish_empty, empty))
                CheckRow(Icons.Outlined.Check, PolarColors.successContainer, PolarColors.onSuccessContainer, stringResource(R.string.finish_ok))
            }
        }
        val actions: @Composable ColumnScope.() -> Unit = {
            Button(onClick = { onAction(ExportAction.PRINT, true) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Outlined.Print, null); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.finish_print))
            }
            ActionButton(Icons.Outlined.PictureAsPdf, stringResource(R.string.finish_save_pdf), stringResource(R.string.finish_save_pdf_hint), !state.busy) { onAction(ExportAction.SAVE, true) }
            ActionButton(Icons.Outlined.Image, stringResource(R.string.finish_save_png), stringResource(R.string.finish_save_png_hint), !state.busy) { onAction(ExportAction.SAVE, false) }
            ActionButton(Icons.Outlined.Share, stringResource(R.string.finish_share), stringResource(R.string.finish_share_hint), !state.busy) { onAction(ExportAction.SHARE, true) }
            if (state.busy) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.finish_preparing))
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.finish_tip, p.settings.paperSize.displayName), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        val scroll = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        if (expanded) {
            Row(scroll, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(16.dp), content = summary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), content = actions)
            }
        } else {
            Column(scroll, verticalArrangement = Arrangement.spacedBy(16.dp)) { summary(); actions() }
        }
    }
}

@Composable
private fun PageThumb(state: EditorUiState, vm: EditorViewModel, container: AppContainer, page: Int) {
    val image by produceState<ImageBitmap?>(null, state.project, page) {
        value = withContext(Dispatchers.Default) {
            container.thumbnails.page(state.project, page, 220, { container.bitmaps.load(it.path, BitmapLoader.PREVIEW_MAX) }, vm.templateBitmap).asImageBitmap()
        }
    }
    val ratio = (state.project.settings.paperSizePoints.width / state.project.settings.paperSizePoints.height).toFloat()
    Box(Modifier.height(150.dp).aspectRatio(ratio).shadow(3.dp).background(Color.White)) {
        image?.let { Image(it, null, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun CheckRow(icon: ImageVector, bg: Color, fg: Color, text: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(32.dp).background(bg, MaterialTheme.shapes.extraLarge), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)) }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, hint: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Icon(icon, null); Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

- [ ] **Step 5: Manejo de la exportación en `EditorScreen`**

En `EditorScreen` (antes del `LaunchedEffect(vm)`), agrega:

```kotlin
    var pendingAction by remember { mutableStateOf(ExportAction.SAVE) }
    var pendingFile by remember { mutableStateOf<java.io.File?>(null) }
    val savedLabel = stringResource(R.string.finish_saved)
    val openLabel = stringResource(R.string.finish_open)
    fun copyTo(uri: android.net.Uri?, mime: String) {
        val file = pendingFile ?: return
        if (uri == null) return
        scope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            }
            val r = snackbar.showSnackbar(savedLabel, openLabel)
            if (r == SnackbarResult.ActionPerformed) Share.open(context, uri, mime)
        }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { copyTo(it, "application/pdf") }
    val savePng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { copyTo(it, "image/png") }
```

En el `when (e)` de los eventos, reemplaza la rama `EditorEvent.Exported` por:

```kotlin
                is EditorEvent.Exported -> {
                    pendingFile = e.file
                    when (pendingAction) {
                        ExportAction.PRINT -> {
                            val pm = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                            pm.print(e.file.nameWithoutExtension, FilePrintAdapter(e.file, e.file.name), PrintMedia.attributes(vm.state.value.project.settings))
                        }
                        ExportAction.SAVE -> if (e.pdf) savePdf.launch(e.file.name) else savePng.launch(e.file.name)
                        ExportAction.SHARE -> Share.file(context, e.file, e.mime, e.file.name)
                    }
                }
```

Y en el `when (state.mode)` agrega, antes de `else ->`:

```kotlin
        EditorMode.FINISH -> FinishScreen(state, vm, container) { action, pdf ->
            pendingAction = action
            if (pdf) vm.exportPdf() else vm.exportPng()
        }
```

Imports: `androidx.activity.compose.rememberLauncherForActivityResult`, `androidx.activity.result.contract.ActivityResultContracts`, `com.polar.app.export.FilePrintAdapter`, `com.polar.app.export.PrintMedia`.

> Dentro del colector de eventos se lee `vm.state.value` (el valor actual), no `state`, que quedaría congelado al iniciar el `LaunchedEffect`.

- [ ] **Step 6: Correr pruebas, compilar y probar la exportación real**

Run: `./gradlew testDebugUnitTest assembleDebug --console=plain`, instala y abre.
Expected:
- "Imprimir" abre el diálogo de impresión de Android con Carta propuesta.
- "Guardar PDF" abre "Guardar como"; al guardar en Descargas aparece "Archivo guardado · Abrir".
- "Compartir PDF" abre la hoja de compartir.

Verifica el archivo:

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB shell ls /sdcard/Download/
$ADB pull "/sdcard/Download/<nombre>.pdf" /tmp/polar-check.pdf
python3 -c "import re;d=open('/tmp/polar-check.pdf','rb').read();print('páginas:',len(re.findall(rb'/Type\s*/Page[^s]',d)));print(re.findall(rb'/MediaBox\s*\[[^\]]*\]',d)[:1])"
```
Expected: tantas páginas como hojas del proyecto y `MediaBox [0 0 612 792]` para Carta vertical.

Guarda un PNG y revisa sus medidas:

```bash
$ADB pull "/sdcard/Download/<nombre> · hoja 1.png" /tmp/polar-check.png
python3 -c "import struct;d=open('/tmp/polar-check.png','rb').read(24);print(struct.unpack('>II',d[16:24]))"
```
Expected: `(2550, 3300)` para Carta.

- [ ] **Step 7: Commit**

```bash
git add -A PolarAndroid/app/src
git commit -m "feat(finish): imprimir desde el teléfono, guardar como PDF o PNG y compartir"
```

---

### Task 21: Preparación para Play Store

**Files:**
- Create: `PolarAndroid/app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `PolarAndroid/app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Create: `PolarAndroid/app/src/main/res/values/ic_launcher_background.xml`
- Create: `PolarAndroid/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `PolarAndroid/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Delete: `PolarAndroid/app/src/main/res/mipmap-*/ic_launcher.png`
- Modify: `PolarAndroid/app/src/main/AndroidManifest.xml` (`roundIcon`)
- Modify: `PolarAndroid/app/src/main/res/values/themes.xml` y `values-night/themes.xml` (ícono de arranque)
- Modify: `PolarAndroid/app/build.gradle.kts` (firma y R8)
- Modify: `PolarAndroid/app/proguard-rules.pro`
- Create: `docs/privacidad.md`
- Create: `docs/play-store.md`

**Interfaces:**
- Consumes: nada nuevo.
- Produces: `./gradlew bundleRelease` genera `app/build/outputs/bundle/release/app-release.aab`, firmado si existen las propiedades `POLAR_STORE_FILE`, `POLAR_STORE_PASSWORD`, `POLAR_KEY_ALIAS` y `POLAR_KEY_PASSWORD` en `~/.gradle/gradle.properties`, y con la llave de depuración si no existen (sólo para pruebas locales).

- [ ] **Step 1: Ícono adaptativo (polaroid inclinada, vino sobre crema)**

`res/values/ic_launcher_background.xml`:

```xml
<resources>
    <color name="ic_launcher_background">#F7F2EB</color>
</resources>
```

`res/drawable/ic_launcher_foreground.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <group android:rotation="-6" android:pivotX="54" android:pivotY="54">
        <path android:fillColor="#FFFFFF" android:strokeColor="#E4DAD0" android:strokeWidth="1" android:pathData="M33,27h42v54h-42z" />
        <path android:fillColor="#7A293B" android:pathData="M37,31h34v34h-34z" />
        <path android:fillColor="#F2DDE1" android:pathData="M37,65 L47,53 L54,61 L59,55 L71,65 Z" />
        <path android:fillColor="#F2DDE1" android:pathData="M58,40a4,4 0,1 1,8 0a4,4 0,1 1,-8 0" />
        <path android:fillColor="#7A293B" android:pathData="M44,72h20v2h-20z" />
    </group>
</vector>
```

`res/drawable/ic_launcher_monochrome.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <group android:rotation="-6" android:pivotX="54" android:pivotY="54">
        <path android:fillColor="#000000" android:fillType="evenOdd" android:pathData="M33,27h42v54h-42z M37,31h34v34h-34z" />
        <path android:fillColor="#000000" android:pathData="M37,65 L47,53 L54,61 L59,55 L71,65 Z" />
    </group>
</vector>
```

`res/mipmap-anydpi-v26/ic_launcher.xml` (y una copia idéntica como `ic_launcher_round.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

```bash
git rm -q PolarAndroid/app/src/main/res/mipmap-*/ic_launcher.png
```

En el manifiesto cambia `android:roundIcon="@mipmap/ic_launcher"` por `@mipmap/ic_launcher_round`. En ambos `themes.xml`, cambia `windowSplashScreenAnimatedIcon` a `@drawable/ic_launcher_foreground`.

- [ ] **Step 2: Firma y R8 en `app/build.gradle.kts`**

Dentro de `android { }`, antes de `buildTypes`:

```kotlin
    val storeFile = providers.gradleProperty("POLAR_STORE_FILE").orNull
    signingConfigs {
        if (storeFile != null) create("release") {
            this.storeFile = file(storeFile)
            storePassword = providers.gradleProperty("POLAR_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("POLAR_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("POLAR_KEY_PASSWORD").get()
        }
    }
```

Reemplaza el bloque `release { … }` por:

```kotlin
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sin llave propia se firma con la de depuración: sirve para probar, no para subir a Play.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
```

`app/proguard-rules.pro`:

```
# kotlinx-serialization: modelo .polar y rutas de navegación
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.polar.app.**$$serializer { *; }
-keepclassmembers class com.polar.app.** { *** Companion; }
-keepclasseswithmembers class com.polar.app.** { kotlinx.serialization.KSerializer serializer(...); }
```

- [ ] **Step 3: Compilar la versión de publicación y probarla**

```bash
./gradlew bundleRelease assembleRelease --console=plain
ls -la app/build/outputs/bundle/release/ app/build/outputs/apk/release/
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
~/Library/Android/sdk/platform-tools/adb shell am start -n io.github.maverickdev01.polar/com.polar.app.MainActivity
```
Expected: `BUILD SUCCESSFUL`, existen el `.aab` y el `.apk`, y la app minificada abre un `.polar` existente sin errores. Ese paso prueba que R8 no rompió la serialización.

- [ ] **Step 4: `docs/privacidad.md`**

```markdown
# Política de privacidad de Polar

**Última actualización:** 2 de octubre de 2026

Polar es una app para diseñar e imprimir tus fotos. Funciona sin internet y no tiene cuentas ni anuncios.

- **Qué datos usa:** sólo las fotos y plantillas que tú eliges. Polar hace una copia dentro de la app para que tu diseño no se pierda; tus originales no se modifican.
- **Dónde quedan:** en tu teléfono. Polar no envía tus fotos, diseños ni ningún otro dato a servidores, ni propios ni de terceros.
- **Qué compartes tú:** cuando eliges Imprimir, Guardar o Compartir, el archivo va sólo a la impresora, carpeta o app que tú selecciones.
- **Permisos:** Polar usa el selector de fotos y el selector de archivos del sistema; no pide acceso a toda tu galería ni a tu almacenamiento.
- **Borrar tus datos:** borra un diseño desde Tus diseños, o desinstala la app para eliminar todo.
- **Niños:** Polar no recopila datos de nadie, incluidos menores.

Contacto: josecatalino.code@gmail.com
```

- [ ] **Step 5: `docs/play-store.md` (ficha y lista de verificación)**

```markdown
# Polar en Play Store

## Antes de la primera subida (lo hace el dueño de la cuenta)

1. Confirmar el `applicationId` definitivo (hoy `io.github.maverickdev01.polar`): no se puede cambiar después de publicar.
2. Crear la llave de subida (pide una contraseña; guárdala en un gestor de contraseñas):
   `keytool -genkeypair -v -keystore ~/polar-upload.jks -alias polar -keyalg RSA -keysize 4096 -validity 10000`
3. Agregar a `~/.gradle/gradle.properties` (nunca al repositorio):
   `POLAR_STORE_FILE=/Users/<usuario>/polar-upload.jks`, `POLAR_STORE_PASSWORD=…`, `POLAR_KEY_ALIAS=polar`, `POLAR_KEY_PASSWORD=…`
4. `cd PolarAndroid && ./gradlew bundleRelease` → subir `app/build/outputs/bundle/release/app-release.aab` con *Play App Signing* activado.
5. Publicar `docs/privacidad.md` en una URL pública (por ejemplo GitHub Pages de la cuenta personal) y pegarla en Play Console.

## Ficha

- **Nombre:** Polar · Fotos para imprimir
- **Descripción corta (≤ 80):** Convierte tus fotos en polaroids, boletos y calendarios listos para imprimir.
- **Descripción completa:** Elige un diseño (Polaroid, foto con canción y QR, boleto, película, calendario y más), pon tus fotos y se acomodan solas. Escribe un pie de foto para todas o uno distinto para cada tarjeta, con fecha, color y 20 tipos de letra. Imprime desde tu teléfono o guarda un PDF a tamaño real en Carta, Oficio, A4, 4 × 6 y más. Funciona sin internet, sin cuentas y sin anuncios: tus fotos nunca salen de tu teléfono.
- **Categoría:** Fotografía.
- **Capturas:** teléfono (4–8) y tablet de 10" (2–4), en claro y oscuro: Tus diseños, Catálogo, Editor, Texto por tarjeta, Terminar.

## Seguridad de los datos (Data safety)

- ¿Recopila datos? **No.** ¿Comparte datos? **No.**
- Datos cifrados en tránsito: no aplica (no hay red). Eliminación de datos: el usuario borra sus diseños dentro de la app.

## Clasificación de contenido

Sin violencia, sin contenido generado por usuarios compartido públicamente, sin compras ni anuncios.
```

- [ ] **Step 6: Commit**

```bash
git add -A PolarAndroid docs/privacidad.md docs/play-store.md
git commit -m "build: ícono adaptativo, versión firmable con R8, privacidad y ficha de Play Store"
```

---

### Task 22: Limpieza, verificación completa y APK para el usuario

**Files:**
- Modify: `PolarAndroid/README.md` (reemplazo)
- Create (no se versionan): `docs/capturas/fase-1/*.png`

**Interfaces:** ninguna nueva.

- [ ] **Step 1: Buscar restos**

```bash
cd /Users/cattaherrrera/Downloads/Polar/PolarAndroid
grep -rn 'Text("Inicio")\|Text("Catálogo")\|Text("Editor")\|Text("Fotos")\|Text("Diseño")\|Text("Texto")\|Text("Papel")\|// Task 1[5-9]\|// Task 20' app/src/main || echo "sin provisionales"
grep -rln "StudioViewModel\|InspectorSheet\|saveToPublicStorage" app/src || echo "sin código viejo"
grep -rn "0xFF[0-9A-Fa-f]\{6\}" app/src/main/java/com/polar/app/ui --include=*.kt | grep -v "theme/\|PolaroidStack\|HomeScreen.kt" || echo "colores sólo en el tema"
```
Expected: "sin provisionales", "sin código viejo" y "colores sólo en el tema". Las excepciones permitidas son el papel de las polaroids y las fotos de muestra.

- [ ] **Step 2: Reemplazar `PolarAndroid/README.md`**

````markdown
# Polar para Android

Diseña e imprime tus fotos: polaroids, canción con QR, boletos, películas, calendarios y tus propias plantillas. Funciona sin internet; tus fotos no salen del teléfono. Compatible con los proyectos `.polar` de Polar para Mac.

## Instalar el APK de prueba

El último APK está en `../Polar.apk`. Envíalo al teléfono, ábrelo y permite instalar desde esa fuente si te lo pide.

## Compilar

```sh
cd PolarAndroid
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew testDebugUnitTest assembleDebug
```

Para publicar, sigue `../docs/play-store.md`.

## Cómo está hecho

- `model/`: proyecto `.polar` inmutable con las mismas claves que la Mac.
- `core/`: texto por tarjeta (`TextResolver`), ediciones puras (`ProjectEdits`), deshacer (`UndoStack`).
- `engine/`: un solo motor de dibujo para la vista previa, las miniaturas, el PDF, el PNG y la impresión.
- `data/`: biblioteca en disco con autoguardado, fotos copiadas al proyecto, fuentes incluidas y ajustes.
- `ui/`: Compose + Material 3. Inicio, Catálogo, Editor (Encuadrar y Terminar), Ajustes. Con ancho ≥ 840dp se usan tres paneles.

Diseño completo: `../docs/superpowers/specs/2026-10-02-polar-android-rediseno-design.md`.
````

- [ ] **Step 3: Verificación completa**

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleRelease --console=plain
```
Expected: `BUILD SUCCESSFUL`, 0 pruebas fallidas y 0 errores de lint (las advertencias se revisan, pero no bloquean).

- [ ] **Step 4: Recorrido manual en el emulador (teléfono)**

Instala el APK de depuración y recorre, anotando cualquier falla:

1. Primera apertura → bienvenida → "Empezar" → estado vacío.
2. Nuevo diseño → Polaroid → Agregar fotos (12) → se acomodan en 2 hojas.
3. Tocar la tarjeta 2 → Texto → "Sólo tarjeta 2" → escribir "El brindis" → sólo cambia esa tarjeta.
4. "Todas las tarjetas" → "Aplicar a todas" → la tarjeta 2 vuelve al texto general → Deshacer la regresa.
5. Rol "Fecha" → "De la foto" → aparece la fecha de captura (o nada, si la foto no tiene EXIF).
6. Letra Caveat y color rojo → se ven en la hoja.
7. Diseño → "4" por hoja → Papel → A4 horizontal.
8. Encuadrar → zoom 2× → Listo.
9. Imprimir → aparece el diálogo de Android. Guardar PDF → archivo en Descargas.
10. Volver a Tus diseños → la polaroid muestra la miniatura → cerrar la app desde recientes, reabrir → todo sigue igual.
11. Menú ⋮ → Duplicar → Borrar la copia → Deshacer.
12. Catálogo → Abrir `.polar` con `Mi primer diseño.polar` copiado al teléfono (`adb push "../Mi primer diseño.polar" /sdcard/Download/`) → abre y avisa de las fotos que faltan.

- [ ] **Step 5: Capturas por tamaño, tema y letra grande**

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
OUT=../docs/capturas/fase-1; mkdir -p $OUT
shot() { sleep 2; $ADB exec-out screencap -p > "$OUT/$1.png"; }
# teléfono: abre el editor del diseño del paso 2 antes de cada captura
shot telefono-claro
$ADB shell cmd uimode night yes; shot telefono-oscuro; $ADB shell cmd uimode night no
$ADB shell settings put system font_scale 1.3; shot telefono-letra-grande; $ADB shell settings put system font_scale 1.0
# tablet simulada en el mismo emulador
$ADB shell wm size 2560x1600; $ADB shell wm density 320; shot tablet-horizontal
$ADB shell wm size 1600x2560; shot tablet-vertical
$ADB shell wm size reset; $ADB shell wm density reset
```
Expected:
- Textos sin cortes con letra al 130 %.
- El modo oscuro mantiene la hoja blanca.
- La tablet horizontal muestra barra lateral, hoja y panel fijo.
- La tablet vertical usa la barra inferior con cuadrícula de 4 columnas en Inicio.

Corrige lo que salga mal en una sola tanda, vuelve a capturar una vez y no hagas más rondas.

- [ ] **Step 6: Entregar el APK y cerrar la fase**

```bash
cp app/build/outputs/apk/debug/app-debug.apk ../Polar.apk
cd .. && git add -A PolarAndroid/README.md && git commit -m "docs: README de Polar Android fase 1"
git log --oneline main..fase-1-base | cat
```

Después invoca la skill `superpowers:finishing-a-development-branch` para decidir con el usuario cómo integrar `fase-1-base` a `main`. Envíale `../Polar.apk` y las capturas de `docs/capturas/fase-1/`.

---

## Cobertura del spec (auto-revisión)

| Spec | Tarea |
|---|---|
| 2 · Reglas de sencillez | 10 (accesibilidad/tema), 14–18 (Más opciones, Snackbars con Deshacer, valores listos), 22 (letra al 130 %) |
| 3 · Identidad visual | 10 (colores, Gelasio, formas), 11 (polaroids con letra manuscrita), 21 (ícono) |
| 4 · Pantallas y adaptación | 10, 11, 12, 14, 19, 20 (tres paneles ≥ 840dp en 14 y 20) |
| 5.1 · Texto por tarjeta, color, fuentes, fecha, estilos rápidos | 2, 3, 4, 6, 7, 17 |
| 5.2 · Fase 1, puntos 1–8 | 8 + 13 (plantilla dibujada), 9 + 11 (biblioteca, duplicar), 5 + 13 (deshacer), 16–18 (paridad), 15 (fotos usadas y baja resolución), 12 (miniaturas), 20 (imprimir, guardar, compartir), 21 (Play Store) |
| 5.6 · Se quita | 2 (UI vieja), 14 (doble toque en vez de pellizco), 20 (Terminar en vez del diálogo) |
| 6 · Arquitectura y persistencia | 2, 3, 4, 5, 7, 9, 13 |
| 7 · Play Store | 1 (SDK 36, id), 21 |
| 8 · Pruebas | cada tarea con sus pruebas; 20 (PDF/PNG reales); 22 (capturas) |

**Fuera de esta fase, a propósito:** encuadre con gestos, arrastrar e intercambiar fotos, filtros, copias, orden de rellenado, animación de "revelado" (Fase 2) y creador de moldes (Fase 3).
