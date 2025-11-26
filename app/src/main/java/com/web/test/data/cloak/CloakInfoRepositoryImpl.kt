package com.web.test.data.cloak

import com.web.test.data.cloak.datasource.BatteryInfoDataSource
import com.web.test.data.cloak.datasource.DeviceInfoDataSource
import com.web.test.data.cloak.datasource.EmulatorCheckDataSource
import com.web.test.data.cloak.datasource.LocaleInfoDataSource
import com.web.test.data.cloak.datasource.NetworkInfoDataSource
import com.web.test.data.cloak.datasource.RootCheckDataSource
import com.web.test.data.cloak.datasource.UsbDebugCheckDataSource
import com.web.test.data.cloak.datasource.VpnProxyCheckDataSource
import com.web.test.domain.model.CloakInfo
import com.web.test.domain.repository.CloakInfoRepository
import javax.inject.Inject

class CloakInfoRepositoryImpl @Inject constructor(
    private val rootCheckDataSource: RootCheckDataSource,
    private val emulatorCheckDataSource: EmulatorCheckDataSource,
    private val usbDebugCheckDataSource: UsbDebugCheckDataSource,
    private val vpnProxyCheckDataSource: VpnProxyCheckDataSource,
    private val batteryInfoDataSource: BatteryInfoDataSource,
    private val localeInfoDataSource: LocaleInfoDataSource,
    private val deviceInfoDataSource: DeviceInfoDataSource,
    private val networkInfoDataSource: NetworkInfoDataSource
) : CloakInfoRepository {
    override suspend fun collectCloakInfo(): CloakInfo = CloakInfo(
        isRooted = rootCheckDataSource.isRooted(),
        isEmulator = emulatorCheckDataSource.isEmulator(),
        isUsbDebuggingEnabled = usbDebugCheckDataSource.isUsbDebuggingEnabled(),
        isVpnEnabled = vpnProxyCheckDataSource.isVpnEnabled(),
        hasProxy = vpnProxyCheckDataSource.hasProxy(),
        batteryLevel = batteryInfoDataSource.getBatteryLevel(),
        countryCode = localeInfoDataSource.countryCode(),
        timezone = localeInfoDataSource.timezone(),
        language = localeInfoDataSource.language(),
        deviceModel = deviceInfoDataSource.deviceModel(),
        deviceBrand = deviceInfoDataSource.deviceBrand(),
        osVersion = deviceInfoDataSource.osVersion(),
        appVersion = deviceInfoDataSource.appVersion(),
        isPlayStoreInstall = deviceInfoDataSource.isPlayStoreInstall(),
        networkType = networkInfoDataSource.getNetworkType()
    )
}
