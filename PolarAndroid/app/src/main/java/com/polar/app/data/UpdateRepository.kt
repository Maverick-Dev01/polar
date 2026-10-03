package com.polar.app.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.polar.app.BuildConfig
import com.polar.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@Serializable
data class PendingUpdate(val downloadId: Long, val info: UpdateInfo)
data class DownloadProgress(val status: Int, val bytes: Long, val total: Long)

class UpdateRepository(private val context: Context) {
    private val downloads = context.getSystemService(DownloadManager::class.java)
    private val preferences = context.getSharedPreferences("polar_updates", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    val version: Long get() = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    val canUpdate: Boolean get() = BuildConfig.GITHUB_UPDATES_ENABLED && context.packageName == UpdateInfo.PACKAGE

    suspend fun latest(): UpdateInfo? = withContext(Dispatchers.IO) {
        var url = URL(UpdateInfo.LATEST)
        repeat(6) {
            require(url.protocol == "https" && url.host in setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com") && url.userInfo == null)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.setRequestProperty("User-Agent", "Polar-Android")
                connection.setRequestProperty("Accept", "application/json")
                when (connection.responseCode) {
                    in 300..399 -> url = URL(url, connection.getHeaderField("Location") ?: error("Redirect incompleto"))
                    404 -> return@withContext null
                    200 -> {
                        require(connection.contentLengthLong <= 65536)
                        val bytes = connection.inputStream.use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            while (output.size() <= 65536) {
                                val count = input.read(buffer, 0, minOf(buffer.size, 65537 - output.size()))
                                if (count < 0) break
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        require(bytes.size <= 65536)
                        return@withContext UpdateInfo.parse(bytes.toString(Charsets.UTF_8))
                    }
                    else -> error("HTTP ${connection.responseCode}")
                }
            } finally { connection.disconnect() }
        }
        error("Demasiadas redirecciones")
    }

    private fun pending(): PendingUpdate? {
        val text = preferences.getString("pending", null) ?: return null
        return runCatching { UpdateInfo.json.decodeFromString<PendingUpdate>(text).also { require(it.downloadId >= 0); it.info.validated() } }
            .getOrElse { preferences.edit().remove("pending").commit(); null }
    }

    suspend fun resume(): PendingUpdate? = withContext(Dispatchers.IO) {
        mutex.withLock {
            pending()?.takeIf { if (it.info.versionCode <= version) { discard(it); false } else true }
        }
    }

    private fun apkFile(info: UpdateInfo): File {
        info.validated()
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: error("Almacenamiento no disponible")
        return File(folder, "updates/Polar-${info.versionCode}.apk")
    }

    suspend fun start(info: UpdateInfo): PendingUpdate = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(canUpdate)
            info.validated()
            require(info.versionCode > version && info.minSdk <= Build.VERSION.SDK_INT)
            pending()?.let { return@withLock it }
            val target = apkFile(info)
            check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
            check(!target.exists() || target.delete())
            val request = DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle(context.getString(R.string.updates_download_title, info.versionName))
                .setDescription(context.getString(R.string.updates_download_description))
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(target))
            val result = PendingUpdate(downloads.enqueue(request), info)
            if (!preferences.edit().putString("pending", UpdateInfo.json.encodeToString(result)).commit()) {
                downloads.remove(result.downloadId)
                error("No se pudo guardar la descarga")
            }
            result
        }
    }

    suspend fun progress(pending: PendingUpdate): DownloadProgress = withContext(Dispatchers.IO) {
        downloads.query(DownloadManager.Query().setFilterById(pending.downloadId)).use { cursor ->
            check(cursor != null && cursor.moveToFirst()) { "Descarga eliminada" }
            DownloadProgress(cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)))
        }
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex() }.toSet()
    }

    @Suppress("DEPRECATION")
    suspend fun verifiedIntent(pending: PendingUpdate): Intent = withContext(Dispatchers.IO) {
        check(canUpdate)
        val file = apkFile(pending.info)
        pending.info.validateFile(file)
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = pm.getPackageInfo(context.packageName, flags)
        val candidate = pm.getPackageArchiveInfo(file.path, flags) ?: error("APK inválido")
        pending.info.validateIdentity(context.packageName, PackageInfoCompat.getLongVersionCode(installed), candidate.packageName,
            PackageInfoCompat.getLongVersionCode(candidate), candidate.versionName, signers(installed), signers(candidate))
        require(candidate.applicationInfo?.minSdkVersion == pending.info.minSdk && pending.info.minSdk <= Build.VERSION.SDK_INT)
        require((candidate.applicationInfo?.flags ?: ApplicationInfo.FLAG_DEBUGGABLE) and ApplicationInfo.FLAG_DEBUGGABLE == 0)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun discard(pending: PendingUpdate) {
        downloads.remove(pending.downloadId)
        apkFile(pending.info).delete()
        check(preferences.edit().remove("pending").commit())
    }

    suspend fun cancel(expectedDownloadId: Long) = withContext(Dispatchers.IO) {
        mutex.withLock { pending()?.takeIf { it.downloadId == expectedDownloadId }?.let { discard(it) } }
    }
}
