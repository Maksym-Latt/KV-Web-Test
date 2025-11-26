package com.web.test.data.cloak

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class DeviceInfoDataSource(private val context: Context) {
    fun deviceModel(): String = Build.MODEL.orEmpty()
    fun deviceBrand(): String = Build.BRAND.orEmpty()
    fun osVersion(): String = Build.VERSION.RELEASE.orEmpty()
    fun appVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName.orEmpty()
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun isPlayStoreInstall(): Boolean {
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer == "com.android.vending" || installer == "com.google.android.feedback"
    }
}
