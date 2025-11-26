package com.web.test.data.cloak

import android.content.Context
import android.provider.Settings

class UsbDebugCheckDataSource(private val context: Context) {
    fun isUsbDebuggingEnabled(): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }
}
