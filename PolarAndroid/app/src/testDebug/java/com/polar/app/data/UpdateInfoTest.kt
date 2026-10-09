package com.polar.app.data

import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class UpdateInfoTest {
    private val bytes = "apk de prueba".toByteArray()
    private val info = UpdateInfo(1, UpdateInfo.PACKAGE, 4, "2.1.1", 26,
        "${UpdateInfo.REPO}/releases/download/android-v2.1.1/Polar-2.1.1.apk",
        MessageDigest.getInstance("SHA-256").digest(bytes).hex(), bytes.size.toLong())

    private fun rejected(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }

    @Test fun parsesValidManifestAndIgnoresFutureFields() {
        val json = UpdateInfo.json.encodeToString(info)
        assertEquals(info, UpdateInfo.parse(json.dropLast(1) + ",\"future\":true}"))
    }
    @Test fun rejectsOtherSourcesAndMalformedMetadata() {
        listOf(info.apkUrl.replace("https:", "http:"), info.apkUrl.replace("Maverick-Dev01", "otro"),
            info.apkUrl.replace("github.com", "github.com.evil.example"), info.apkUrl + "?file=otro.apk",
            info.apkUrl.replace("android-v2.1.1", "latest"), info.apkUrl.replace("2.1.1.apk", "2.1.0.apk"))
            .forEach { rejected { info.copy(apkUrl = it).validated() } }
        listOf(info.copy(schemaVersion = 2), info.copy(packageName = "otra.app"), info.copy(versionCode = 0),
            info.copy(versionName = "../x"), info.copy(sizeBytes = 0), info.copy(sizeBytes = UpdateInfo.MAX_APK_BYTES + 1),
            info.copy(sha256 = "abc"), info.copy(minSdk = 25), info.copy(notes = "x".repeat(4001)))
            .forEach { rejected { it.validated() } }
        rejected { UpdateInfo.parse("x".repeat(65537)) }
    }
    @Test fun rejectsTruncatedOrTamperedDownload() {
        val file = File.createTempFile("polar-update", ".apk")
        try {
            file.writeBytes(bytes)
            info.validateFile(file)
            file.writeBytes(bytes.dropLast(1).toByteArray())
            rejected { info.validateFile(file) }
            file.writeBytes(ByteArray(bytes.size))
            rejected { info.validateFile(file) }
        } finally { file.delete() }
    }
    @Test fun enforcesVersionPackageAndWholeSignerSet() {
        val signers = setOf("certificado-1", "certificado-2")
        fun verify(version: Long = 4, name: String? = "2.1.1", pkg: String? = UpdateInfo.PACKAGE,
                   installed: Long = 3, certs: Set<String> = signers, original: Set<String> = signers) =
            info.validateIdentity(UpdateInfo.PACKAGE, installed, pkg, version, name, original, certs)
        verify()
        rejected { verify(installed = 4) }
        rejected { verify(installed = 5) }
        rejected { verify(version = 5) }
        rejected { verify(name = "2.1.0") }
        rejected { verify(pkg = UpdateInfo.PACKAGE + ".debug") }
        rejected { verify(certs = setOf("certificado-1")) }
        rejected { verify(certs = emptySet()) }
        rejected { verify(certs = emptySet(), original = emptySet()) }
        rejected { verify(certs = setOf("otro")) }
    }
}
