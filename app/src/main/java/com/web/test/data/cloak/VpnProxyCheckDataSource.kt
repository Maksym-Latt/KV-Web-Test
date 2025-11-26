package com.web.test.data.cloak

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.net.ProxySelector

class VpnProxyCheckDataSource(private val context: Context) {
    fun isVpnActive(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    fun hasProxy(): Boolean {
        val defaultProxy = ProxySelector.getDefault()?.select(null)?.firstOrNull()
        if (defaultProxy != null && defaultProxy.address() != null) {
            return true
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            !System.getProperty("http.proxyHost").isNullOrBlank() || !System.getProperty("https.proxyHost").isNullOrBlank()
        } else {
            false
        }
    }
}
