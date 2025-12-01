package com.web.test.di

import android.content.Context
import com.web.test.data.cloak.datasource.DeviceInfoDataSource
import com.web.test.data.cloak.datasource.UsbDebugCheckDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBindings {

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
