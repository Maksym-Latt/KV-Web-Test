package com.web.test.data.cloak.datasource

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import javax.inject.Inject

class BatteryInfoDataSource @Inject constructor(
    private val context: Context
) {
    fun getBatteryLevel(): Int {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level == -1 || scale == -1) return 0
        return ((level / scale.toFloat()) * 100).toInt()
    }
}
