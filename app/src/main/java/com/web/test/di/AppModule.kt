package com.web.test.di

import android.content.Context
import com.web.test.data.cloak.CloakInfoRepositoryImpl
import com.web.test.data.cloak.datasource.DeviceInfoDataSource
import com.web.test.data.cloak.datasource.UsbDebugCheckDataSource
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
    abstract fun bindCloakInfoRepository(
        impl: CloakInfoRepositoryImpl
    ): CloakInfoRepository

    @Binds
    abstract fun bindDecisionRepository(
        impl: LocalDecisionRepository
    ): DecisionRepository

    @Binds
    abstract fun bindDecisionCacheRepository(
        impl: DecisionCacheRepositoryImpl
    ): DecisionCacheRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModuleProvides {

    @Provides
    @Singleton
    fun provideUsbDebugCheckDataSource(@ApplicationContext context: Context): UsbDebugCheckDataSource =
        UsbDebugCheckDataSource(context)


    @Provides
    @Singleton
    fun provideDeviceInfoDataSource(@ApplicationContext context: Context): DeviceInfoDataSource =
        DeviceInfoDataSource(context)


}
