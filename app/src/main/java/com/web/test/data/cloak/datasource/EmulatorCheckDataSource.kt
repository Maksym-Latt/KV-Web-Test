package com.web.test.data.cloak.datasource

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import javax.inject.Inject

class EmulatorCheckDataSource @Inject constructor(
    private val context: Context
) {
    fun isEmulator(): Boolean {
        val suspectBuild = Build.FINGERPRINT.lowercase().contains("generic") ||
            Build.MODEL.lowercase().contains("google_sdk") ||
            Build.MODEL.lowercase().contains("emulator") ||
            Build.MANUFACTURER.lowercase().contains("genymotion") ||
            Build.BRAND.lowercase().startsWith("generic") ||
            Build.DEVICE.lowercase().startsWith("generic") ||
            "google_sdk" == Build.PRODUCT

        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull()?.lowercase().orEmpty()
        val x86Abi = cpuAbi.contains("x86")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorCount = sensorManager?.getSensorList(Sensor.TYPE_ALL)?.size ?: 0

        return suspectBuild || x86Abi || sensorCount <= 1
    }
}
