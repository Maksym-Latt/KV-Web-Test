package com.web.test.data.cloak

import com.web.test.data.cloak.datasource.DeviceInfoDataSource
import com.web.test.data.cloak.datasource.UsbDebugCheckDataSource
import com.web.test.domain.model.CloakInfo
import com.web.test.domain.repository.CloakInfoRepository
import javax.inject.Inject

class CloakInfoRepositoryImpl @Inject constructor(
    private val usbDebugCheckDataSource: UsbDebugCheckDataSource,
    private val deviceInfoDataSource: DeviceInfoDataSource
) : CloakInfoRepository {
    override suspend fun collectCloakInfo(): CloakInfo = CloakInfo(
        isUsbDebuggingEnabled = usbDebugCheckDataSource.isUsbDebuggingEnabled(),
        deviceModel = deviceInfoDataSource.deviceModel(),
        deviceBrand = deviceInfoDataSource.deviceBrand(),
        osVersion = deviceInfoDataSource.osVersion(),
        appVersion = deviceInfoDataSource.appVersion(),
        isPlayStoreInstall = deviceInfoDataSource.isPlayStoreInstall()
    )
}