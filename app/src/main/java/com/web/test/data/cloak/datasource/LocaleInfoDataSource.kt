package com.web.test.data.cloak.datasource

import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class LocaleInfoDataSource @Inject constructor() {
    fun countryCode(): String = Locale.getDefault().country
    fun language(): String = Locale.getDefault().language
    fun timezone(): String = TimeZone.getDefault().id
}
