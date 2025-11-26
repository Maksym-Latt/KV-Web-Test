package com.web.test.data.cloak.datasource

import android.content.Context
import android.os.Build
import android.provider.Settings
import javax.inject.Inject

class UsbDebugCheckDataSource @Inject constructor(
    private val context: Context
) {
    fun isUsbDebuggingEnabled(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}
