/*
 * NetworkUtils centraliza utilidades de conectividad de red.
 * Verifica si hay internet real (capacidad INTERNET + VALIDATED)
 * y expone un flujo reactivo para observar cambios de conexión.
 * Incluye un retraso corto antes de reportar "sin conexión" para
 * evitar falsos negativos durante cambios rápidos de red o wake/unlock.
 */

package com.example.esteticaapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object NetworkUtils {
    private fun isNetworkValidated(connectivityManager: ConnectivityManager): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return isNetworkValidated(connectivityManager)
    }

    /**
     * Observa los cambios en la conectividad de red de forma reactiva.
     */
    fun observeConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var pendingOfflineJob: Job? = null

        fun emitCurrentState() {
            val isOnline = isNetworkValidated(connectivityManager)
            if (isOnline) {
                pendingOfflineJob?.cancel()
                pendingOfflineJob = null
                trySend(true)
                return
            }

            // Evita falsos "sin conexion" durante wake/unlock o cambios rapidos de red.
            pendingOfflineJob?.cancel()
            pendingOfflineJob = launch {
                delay(1800)
                trySend(isNetworkValidated(connectivityManager))
            }
        }
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                emitCurrentState()
            }

            override fun onLost(network: Network) {
                emitCurrentState()
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                emitCurrentState()
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        
        // Valor inicial
        emitCurrentState()

        awaitClose {
            pendingOfflineJob?.cancel()
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
