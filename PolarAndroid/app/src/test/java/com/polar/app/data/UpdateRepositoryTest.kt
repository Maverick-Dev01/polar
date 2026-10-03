package com.polar.app.data

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
class UpdateRepositoryTest {
    private lateinit var context: Context
    private val info = UpdateInfo(1, UpdateInfo.PACKAGE, 4, "2.1.1", 26,
        "${UpdateInfo.REPO}/releases/download/android-v2.1.1/Polar-2.1.1.apk", "a".repeat(64), 1024)

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        context = object : ContextWrapper(app) { override fun getPackageName() = UpdateInfo.PACKAGE }
        context.getSharedPreferences("polar_updates", Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = UpdateInfo.PACKAGE; versionCode = 3 })
    }

    @Test fun keepsOneNativeDownloadAcrossRecreationAndCanCancelRetry(): Unit = runBlocking {
        val original = UpdateRepository(context)
        val first = original.start(info)
        assertEquals(first, original.start(info))
        val recreated = UpdateRepository(context)
        assertEquals(first, recreated.resume())
        assertEquals(0, recreated.progress(first).bytes)
        recreated.cancel(first.downloadId)
        assertNull(original.resume())
        val retry = original.start(info)
        assertNotEquals(first.downloadId, retry.downloadId)
        // Un monitor antiguo no puede borrar la descarga que lo reemplazó.
        recreated.cancel(first.downloadId)
        assertEquals(retry, original.resume())
        original.cancel(retry.downloadId)
    }

    @Test fun removesStaleDownloadsAfterAppUpdatesAndRecoversInvalidSavedState() = runBlocking {
        val repository = UpdateRepository(context)
        repository.start(info)
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = UpdateInfo.PACKAGE; versionCode = 4 })
        assertNull(repository.resume())
        context.getSharedPreferences("polar_updates", Context.MODE_PRIVATE).edit().putString("pending", "{broken}").commit()
        assertNull(repository.resume())
        assertFalse(context.getSharedPreferences("polar_updates", Context.MODE_PRIVATE).contains("pending"))
    }
}
