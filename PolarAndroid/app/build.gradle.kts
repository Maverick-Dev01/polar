import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import groovy.json.JsonOutput
import java.security.MessageDigest

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
        versionCode = providers.gradleProperty("POLAR_VERSION_CODE").getOrElse("6").toInt()
        versionName = providers.gradleProperty("POLAR_VERSION_NAME").getOrElse("2.2.0")
        buildConfigField("boolean", "GITHUB_UPDATES_ENABLED", "true")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // La llave queda fuera de Git; una publicación nunca usa la firma de depuración.
    val signingKeys = listOf("POLAR_STORE_FILE", "POLAR_STORE_PASSWORD", "POLAR_KEY_ALIAS", "POLAR_KEY_PASSWORD")
        .map { providers.gradleProperty(it).orElse(providers.environmentVariable(it)).orNull }
    require(signingKeys.all { it == null } || signingKeys.all { !it.isNullOrBlank() }) { "Configura las cuatro propiedades POLAR_* de firma juntas." }
    signingConfigs {
        if (signingKeys.all { it != null }) create("release") {
            storeFile = file(signingKeys[0]!!)
            storePassword = signingKeys[1]
            keyAlias = signingKeys[2]
            keyPassword = signingKeys[3]
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        create("play") {
            initWith(getByName("release"))
            buildConfigField("boolean", "GITHUB_UPDATES_ENABLED", "false")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    // La geometría de los diseños es la misma que usa la Mac: se copia de shared-fixtures/ al compilar.
    sourceSets.getByName("main").resources.srcDir(layout.buildDirectory.dir("generated/shared-geometry"))
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/shared-help"))
}

val copySharedGeometry = tasks.register<Copy>("copySharedGeometry") {
    from(rootProject.projectDir.parentFile.resolve("shared-fixtures/estilos-geometria.json"))
    into(layout.buildDirectory.dir("generated/shared-geometry"))
}
// La guía de uso es la misma que muestra la Mac (shared-fixtures/help.json): se copia a los assets al compilar.
val copySharedHelp = tasks.register<Copy>("copySharedHelp") {
    from(rootProject.projectDir.parentFile.resolve("shared-fixtures/help.json"))
    into(layout.buildDirectory.dir("generated/shared-help"))
}
tasks.named("preBuild") { dependsOn(copySharedGeometry, copySharedHelp) }

tasks.matching { it.name in setOf("packageRelease", "packageReleaseBundle", "signReleaseBundle", "packagePlay", "packagePlayBundle", "signPlayBundle") }.configureEach {
    doFirst {
        check(listOf("POLAR_STORE_FILE", "POLAR_STORE_PASSWORD", "POLAR_KEY_ALIAS", "POLAR_KEY_PASSWORD").all {
            !providers.gradleProperty(it).orElse(providers.environmentVariable(it)).orNull.isNullOrBlank()
        }) { "Falta la firma privada. Ejecuta python tools/android-release.py desde la raíz o configura las cuatro propiedades POLAR_*." }
    }
}

tasks.register("prepareGithubRelease") {
    dependsOn("assembleRelease")
    doLast {
        val version = android.defaultConfig.versionName!!
        check(version.matches(Regex("[0-9]{1,4}\\.[0-9]{1,4}\\.[0-9]{1,4}")))
        val output = rootProject.projectDir.parentFile.resolve("release-assets")
        output.mkdirs()
        val apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        val published = output.resolve("Polar-$version.apk")
        apk.copyTo(published, overwrite = true)
        val digest = MessageDigest.getInstance("SHA-256")
        published.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) } }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        val info = linkedMapOf("schemaVersion" to 1, "packageName" to android.defaultConfig.applicationId,
            "versionCode" to android.defaultConfig.versionCode, "versionName" to version, "minSdk" to 26,
            "apkUrl" to "https://github.com/Maverick-Dev01/polar/releases/download/android-v$version/Polar-$version.apk",
            "sha256" to sha, "sizeBytes" to published.length(),
            "notes" to providers.gradleProperty("POLAR_RELEASE_NOTES").getOrElse("Busca actualizaciones desde Ajustes. Descarga verificada e instalación con confirmación de Android."))
        output.resolve("update.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(info)) + "\n")
        output.resolve("SHA256SUMS.txt").writeText("$sha  ${published.name}\n")
        println("Release preparado: ${published.name}, update.json y SHA256SUMS.txt")
    }
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
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
