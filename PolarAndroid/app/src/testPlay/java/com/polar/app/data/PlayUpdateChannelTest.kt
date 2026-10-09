package com.polar.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayUpdateChannelTest {
    @Test fun excludesGithubUpdaterAndInstallerPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf("com.polar.app.data.UpdateRepository", "com.polar.app.data.UpdateInfo",
            "com.polar.app.ui.settings.UpdateViewModel", "com.polar.app.ui.settings.UpdateSectionKt").forEach {
            assertThrows(ClassNotFoundException::class.java) { Class.forName(it) }
        }
        val permissions = context.packageManager.getPackageInfo(context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES"))
    }
}
