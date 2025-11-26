package com.web.test.data.cloak.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject

class DeviceInfoDataSource @Inject constructor(
    private val context: Context
) {
    fun deviceModel(): String = Build.MODEL
    fun deviceBrand(): String = Build.BRAND
    fun osVersion(): String = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()

    fun appVersion(): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun isPlayStoreInstall(): Boolean {
        return try {
            val installer = context.packageManager.getInstallerPackageName(context.packageName)
            installer != null && installer.contains("com.android.vending")
        } catch (_: Exception) {
            false
        }
    }
}
