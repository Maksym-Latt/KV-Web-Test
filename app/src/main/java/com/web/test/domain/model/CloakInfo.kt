package com.web.test.domain.model

data class CloakInfo(
    val isRooted: Boolean,
    val isEmulator: Boolean,
    val isUsbDebuggingEnabled: Boolean,
    val isVpnEnabled: Boolean,
    val hasProxy: Boolean,
    val batteryLevel: Int,
    val countryCode: String,
    val timezone: String,
    val language: String,
    val deviceModel: String,
    val deviceBrand: String,
    val osVersion: String,
    val appVersion: String,
    val isPlayStoreInstall: Boolean,
    val networkType: NetworkType,
    val googleAdId: String? = null
)
