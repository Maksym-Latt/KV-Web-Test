package com.web.test.data.cloak.datasource

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.web.test.domain.model.NetworkType
import javax.inject.Inject

class NetworkInfoDataSource @Inject constructor(
    private val context: Context
) {
    fun getNetworkType(): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.MOBILE
            else -> NetworkType.NONE
        }
    }
}
