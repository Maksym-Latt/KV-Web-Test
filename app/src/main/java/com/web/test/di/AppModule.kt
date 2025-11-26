package com.web.test.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.web.test.data.cloak.BatteryInfoDataSource
import com.web.test.data.cloak.DefaultCloakInfoProvider
import com.web.test.data.cloak.DeviceInfoDataSource
import com.web.test.data.cloak.EmulatorCheckDataSource
import com.web.test.data.cloak.LocaleInfoDataSource
import com.web.test.data.cloak.NetworkInfoDataSource
import com.web.test.data.cloak.RootCheckDataSource
import com.web.test.data.cloak.UsbDebugCheckDataSource
import com.web.test.data.cloak.VpnProxyCheckDataSource
import com.web.test.data.decision.LocalDecisionRepository
import com.web.test.data.storage.DecisionPreferenceStorage
import com.web.test.domain.repository.CloakInfoProvider
import com.web.test.domain.repository.DecisionRepository
import com.web.test.domain.repository.DecisionStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) {
            context.preferencesDataStoreFile("decision_store")
        }
    }

    @Provides
    @Singleton
    fun provideDecisionStorage(dataStore: DataStore<Preferences>): DecisionStorage =
        DecisionPreferenceStorage(dataStore)

    @Provides
    @Singleton
    fun provideDecisionRepository(): DecisionRepository = LocalDecisionRepository()

    @Provides
    @Singleton
    fun provideCloakInfoProvider(@ApplicationContext context: Context): CloakInfoProvider {
        val rootCheck = RootCheckDataSource()
        val emulatorCheck = EmulatorCheckDataSource()
        val usbDebugCheck = UsbDebugCheckDataSource(context)
        val vpnProxyCheck = VpnProxyCheckDataSource(context)
        val batteryInfoDataSource = BatteryInfoDataSource(context)
        val localeInfoDataSource = LocaleInfoDataSource(context)
        val deviceInfoDataSource = DeviceInfoDataSource(context)
        val networkInfoDataSource = NetworkInfoDataSource(context)
        return DefaultCloakInfoProvider(
            context = context,
            rootChecker = rootCheck,
            emulatorChecker = emulatorCheck,
            usbDebugCheckDataSource = usbDebugCheck,
            vpnProxyCheckDataSource = vpnProxyCheck,
            batteryInfoDataSource = batteryInfoDataSource,
            localeInfoDataSource = localeInfoDataSource,
            deviceInfoDataSource = deviceInfoDataSource,
            networkInfoDataSource = networkInfoDataSource,
        )
    }
}
