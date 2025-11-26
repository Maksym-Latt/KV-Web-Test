package com.web.test.di

import android.content.Context
import com.web.test.data.cloak.CloakInfoRepositoryImpl
import com.web.test.data.cloak.datasource.BatteryInfoDataSource
import com.web.test.data.cloak.datasource.DeviceInfoDataSource
import com.web.test.data.cloak.datasource.EmulatorCheckDataSource
import com.web.test.data.cloak.datasource.LocaleInfoDataSource
import com.web.test.data.cloak.datasource.NetworkInfoDataSource
import com.web.test.data.cloak.datasource.RootCheckDataSource
import com.web.test.data.cloak.datasource.UsbDebugCheckDataSource
import com.web.test.data.cloak.datasource.VpnProxyCheckDataSource
import com.web.test.data.decision.LocalDecisionRepository
import com.web.test.data.local.DecisionCacheRepositoryImpl
import com.web.test.domain.repository.CloakInfoRepository
import com.web.test.domain.repository.DecisionCacheRepository
import com.web.test.domain.repository.DecisionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBindings {
    @Binds
    @Singleton
    abstract fun bindDecisionRepository(impl: LocalDecisionRepository): DecisionRepository

    @Binds
    @Singleton
    abstract fun bindCloakInfoRepository(impl: CloakInfoRepositoryImpl): CloakInfoRepository

    @Binds
    @Singleton
    abstract fun bindDecisionCacheRepository(impl: DecisionCacheRepositoryImpl): DecisionCacheRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModuleProvides {
    @Provides
    @Singleton
    fun provideRootCheckDataSource(): RootCheckDataSource = RootCheckDataSource()

    @Provides
    @Singleton
    fun provideEmulatorCheckDataSource(@ApplicationContext context: Context): EmulatorCheckDataSource =
        EmulatorCheckDataSource(context)

    @Provides
    @Singleton
    fun provideUsbDebugCheckDataSource(@ApplicationContext context: Context): UsbDebugCheckDataSource =
        UsbDebugCheckDataSource(context)

    @Provides
    @Singleton
    fun provideVpnProxyCheckDataSource(@ApplicationContext context: Context): VpnProxyCheckDataSource =
        VpnProxyCheckDataSource(context)

    @Provides
    @Singleton
    fun provideBatteryInfoDataSource(@ApplicationContext context: Context): BatteryInfoDataSource =
        BatteryInfoDataSource(context)

    @Provides
    @Singleton
    fun provideLocaleInfoDataSource(): LocaleInfoDataSource = LocaleInfoDataSource()

    @Provides
    @Singleton
    fun provideDeviceInfoDataSource(@ApplicationContext context: Context): DeviceInfoDataSource =
        DeviceInfoDataSource(context)

    @Provides
    @Singleton
    fun provideNetworkInfoDataSource(@ApplicationContext context: Context): NetworkInfoDataSource =
        NetworkInfoDataSource(context)
}
