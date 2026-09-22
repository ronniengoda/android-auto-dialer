package ke.payhero.autodial

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkAddresses {

    fun lanIpv4(): List<String> {
        val addresses = mutableListOf<String>()
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        for (networkInterface in interfaces) {
            if (!networkInterface.isUp || networkInterface.isLoopback) continue
            for (address in networkInterface.inetAddresses) {
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    address.hostAddress?.let { addresses.add(it) }
                }
            }
        }
        return addresses.distinct()
    }
}
