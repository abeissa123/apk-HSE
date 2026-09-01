package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkStatus(
    val isConnected: Boolean = true,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val connectionType: String = "Connecté (Réseau actif)"
)

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _status = MutableStateFlow(getCurrentStatus())
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _status.value = getCurrentStatus()
        }

        override fun onLost(network: Network) {
            _status.value = getCurrentStatus()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _status.value = getCurrentStatus()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Callback registration fallback
        }
    }

    fun getCurrentStatus(): NetworkStatus {
        val cm = connectivityManager ?: return NetworkStatus(isConnected = true, connectionType = "Réseau Actif")
        val activeNetwork = cm.activeNetwork ?: return NetworkStatus(isConnected = false, connectionType = "Hors ligne (Cache local)")
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(isConnected = false, connectionType = "Hors ligne (Cache local)")

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        val label = when {
            !hasInternet -> "Hors ligne (Cache local)"
            isWifi -> "Connecté (Wi-Fi)"
            isCellular -> "Connecté (Données mobiles 4G/5G)"
            isEthernet -> "Connecté (Ethernet)"
            else -> "Connecté (Réseau de données)"
        }

        return NetworkStatus(
            isConnected = hasInternet,
            isWifi = isWifi,
            isCellular = isCellular,
            connectionType = label
        )
    }
}
