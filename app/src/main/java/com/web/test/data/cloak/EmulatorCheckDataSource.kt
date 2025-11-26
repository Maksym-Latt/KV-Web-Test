package com.web.test.data.cloak

import android.os.Build

class EmulatorCheckDataSource {
    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT?.lowercase().orEmpty()
        val model = Build.MODEL?.lowercase().orEmpty()
        val product = Build.PRODUCT?.lowercase().orEmpty()
        val brand = Build.BRAND?.lowercase().orEmpty()
        val device = Build.DEVICE?.lowercase().orEmpty()

        val isGeneric = fingerprint.contains("generic") || fingerprint.contains("emulator")
        val isGoogleSdk = model.contains("google_sdk") || product.contains("google_sdk")
        val isSdk = model.contains("sdk_gphone") || product.contains("sdk")
        val isUnknownBrand = brand.contains("generic") || brand.contains("unknown")
        val isGenericDevice = device.contains("generic")

        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull()?.lowercase().orEmpty()
        val isSuspiciousCpu = cpuAbi.startsWith("x86") || cpuAbi.startsWith("arm64") && isGeneric

        return isGeneric || isGoogleSdk || isSdk || isUnknownBrand || isGenericDevice || isSuspiciousCpu
    }
}
