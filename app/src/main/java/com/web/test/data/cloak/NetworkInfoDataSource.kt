package com.web.test.data.cloak

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.web.test.domain.model.NetworkType

class NetworkInfoDataSource(private val context: Context) {
    fun networkType(): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            else -> NetworkType.NONE
        }
    }
}
