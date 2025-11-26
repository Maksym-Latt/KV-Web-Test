package com.web.test.data.cloak.datasource

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.ProxyInfo
import android.os.Build
import java.net.ProxySelector
import javax.inject.Inject

class VpnProxyCheckDataSource @Inject constructor(
    private val context: Context
) {
    fun isVpnEnabled(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    fun hasProxy(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val proxy: ProxyInfo? = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.defaultProxy
            proxy != null
        } else {
            val selector = ProxySelector.getDefault()
            selector?.select(java.net.URI("http://www.example.com"))?.isNotEmpty() == true
        }
    }
}
