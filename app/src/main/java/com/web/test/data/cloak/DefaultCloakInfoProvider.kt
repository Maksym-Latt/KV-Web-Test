package com.web.test.data.cloak

import android.content.Context
import com.web.test.domain.model.CloakInfo
import com.web.test.domain.repository.CloakInfoProvider

class DefaultCloakInfoProvider(
    private val context: Context,
    private val rootChecker: RootCheckDataSource,
    private val emulatorChecker: EmulatorCheckDataSource,
    private val usbDebugCheckDataSource: UsbDebugCheckDataSource,
    private val vpnProxyCheckDataSource: VpnProxyCheckDataSource,
    private val batteryInfoDataSource: BatteryInfoDataSource,
    private val localeInfoDataSource: LocaleInfoDataSource,
    private val deviceInfoDataSource: DeviceInfoDataSource,
    private val networkInfoDataSource: NetworkInfoDataSource,
) : CloakInfoProvider {

    override suspend fun collect(): CloakInfo {
        val isRooted = rootChecker.isRooted()
        val isEmulator = emulatorChecker.isEmulator()
        val isUsbDebuggingEnabled = usbDebugCheckDataSource.isUsbDebuggingEnabled()
        val isVpnEnabled = vpnProxyCheckDataSource.isVpnActive()
        val hasProxy = vpnProxyCheckDataSource.hasProxy()
        val batteryLevel = batteryInfoDataSource.batteryLevel()
        val countryCode = localeInfoDataSource.countryCode()
        val timezone = localeInfoDataSource.timezone()
        val language = localeInfoDataSource.language()
        val deviceModel = deviceInfoDataSource.deviceModel()
        val deviceBrand = deviceInfoDataSource.deviceBrand()
        val osVersion = deviceInfoDataSource.osVersion()
        val appVersion = deviceInfoDataSource.appVersion()
        val isPlayStoreInstall = deviceInfoDataSource.isPlayStoreInstall()
        val networkType = networkInfoDataSource.networkType()

        return CloakInfo(
            isRooted = isRooted,
            isEmulator = isEmulator,
            isUsbDebuggingEnabled = isUsbDebuggingEnabled,
            isVpnEnabled = isVpnEnabled,
            hasProxy = hasProxy,
            batteryLevel = batteryLevel,
            countryCode = countryCode,
            timezone = timezone,
            language = language,
            deviceModel = deviceModel,
            deviceBrand = deviceBrand,
            osVersion = osVersion,
            appVersion = appVersion,
            isPlayStoreInstall = isPlayStoreInstall,
            networkType = networkType,
        )
    }
}
