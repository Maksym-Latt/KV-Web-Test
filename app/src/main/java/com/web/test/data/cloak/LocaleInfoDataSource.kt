package com.web.test.data.cloak

import android.content.Context
import java.util.Locale
import java.util.TimeZone

class LocaleInfoDataSource(private val context: Context) {
    fun countryCode(): String = context.resources.configuration.locales.get(0).country.ifBlank { Locale.getDefault().country }
    fun language(): String = context.resources.configuration.locales.get(0).language.ifBlank { Locale.getDefault().language }
    fun timezone(): String = TimeZone.getDefault().id
}
