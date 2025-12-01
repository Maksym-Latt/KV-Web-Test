package com.web.test.domain.model

data class CloakInfo(
    val isUsbDebuggingEnabled: Boolean = false,
    val deviceModel: String = "",
    val deviceBrand: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val isPlayStoreInstall: Boolean = false
)