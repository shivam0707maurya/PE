package com.poojaenterprises.pe.printing

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

object LanScanner {

    suspend fun scanSubnetForPrinters(
        context: Context,
        targetPort: Int = 9100,
        onProgress: (Int) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        val localIp = getLocalWifiIpAddress(context) ?: "192.168.1.1"
        val subnetPrefix = localIp.substringBeforeLast(".") + "."

        val foundIps = mutableListOf<String>()
        val batchSize = 32

        for (start in 1..254 step batchSize) {
            val end = minOf(start + batchSize - 1, 254)
            val deferredList = (start..end).map { host ->
                async {
                    val ip = "$subnetPrefix$host"
                    if (isPortOpen(ip, targetPort, 350)) {
                        ip
                    } else null
                }
            }
            val results = deferredList.awaitAll()
            results.filterNotNull().forEach { foundIps.add(it) }
            onProgress(end * 100 / 254)
        }

        foundIps
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalWifiIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            val ipInt = wifiInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }

            // Fallback: network interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }
}
