package com.polar.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
data class UpdateInfo(
    val schemaVersion: Int,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val minSdk: Int,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val notes: String = ""
) {
    fun validated(): UpdateInfo {
        require(schemaVersion == 1 && packageName == PACKAGE)
        require(versionCode in 1..2_100_000_000L && minSdk in 26..100)
        require(versionName.matches(Regex("[0-9]{1,4}\\.[0-9]{1,4}\\.[0-9]{1,4}")))
        require(apkUrl == "$REPO/releases/download/android-v$versionName/Polar-$versionName.apk")
        require(sha256.matches(Regex("[a-f0-9]{64}")) && sizeBytes in 1..MAX_APK_BYTES)
        require(notes.length <= 4000)
        return this
    }

    fun validateFile(file: File) {
        validated()
        require(file.isFile && file.length() == sizeBytes) { "APK incompleto" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) } }
        require(digest.digest().hex() == sha256) { "APK alterado" }
    }

    fun validateIdentity(installedPackage: String, installedVersion: Long, apkPackage: String?, apkVersion: Long,
                         apkVersionName: String?, installedSigners: Set<String>, apkSigners: Set<String>) {
        require(packageName == installedPackage && apkPackage == installedPackage)
        require(apkVersion == versionCode && apkVersion > installedVersion && apkVersionName == versionName)
        require(installedSigners.isNotEmpty() && installedSigners == apkSigners) { "Firma distinta" }
    }

    companion object {
        const val PACKAGE = "io.github.maverickdev01.polar"
        const val REPO = "https://github.com/Maverick-Dev01/polar"
        const val LATEST = "$REPO/releases/latest/download/update.json"
        const val MAX_APK_BYTES = 200L * 1024 * 1024
        val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): UpdateInfo {
            require(text.toByteArray(Charsets.UTF_8).size <= 65536)
            return json.decodeFromString<UpdateInfo>(text).validated()
        }
    }
}

internal fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }
