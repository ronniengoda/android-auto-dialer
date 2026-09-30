package ke.payhero.autodial

import org.json.JSONObject
import java.util.UUID

data class SmsFilterRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Listener Rule",
    val enabled: Boolean = true,
    val senderFilter: String = "",
    val simFilter: String = SmsForwardConfig.SIM_ALL,
    val keywordFilter: String = "",
    val webhookUrl: String = "",
    val headersJson: String = "{}"
) {
    fun senders(): List<String> {
        return senderFilter.split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun headers(): Map<String, String> {
        return try {
            val obj = JSONObject(headersJson)
            obj.keys().asSequence().associateWith { key -> obj.optString(key) }
                .filter { it.key.isNotBlank() && it.value.isNotBlank() }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun matchesSender(from: String): Boolean {
        val filters = senders()
        if (filters.isEmpty()) return true
        if (filters.any { it == "*" }) return true
        val incoming = from.trim()
        return filters.any { incoming.equals(it, ignoreCase = true) || incoming.contains(it, ignoreCase = true) }
    }

    fun matchesSim(sim: String): Boolean {
        return when (simFilter.lowercase()) {
            SmsForwardConfig.SIM_1 -> sim.equals(SmsForwardConfig.SIM_1, ignoreCase = true)
            SmsForwardConfig.SIM_2 -> sim.equals(SmsForwardConfig.SIM_2, ignoreCase = true)
            else -> true
        }
    }

    fun matchesKeyword(body: String): Boolean {
        val kw = keywordFilter.trim()
        if (kw.isEmpty()) return true
        return body.contains(kw, ignoreCase = true)
    }

    fun matches(sender: String, body: String, sim: String): Boolean {
        if (!enabled) return false
        return matchesSender(sender) && matchesSim(sim) && matchesKeyword(body)
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("enabled", enabled)
            put("senderFilter", senderFilter)
            put("simFilter", simFilter)
            put("keywordFilter", keywordFilter)
            put("webhookUrl", webhookUrl)
            put("headersJson", headersJson)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): SmsFilterRule {
            return SmsFilterRule(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Listener Rule"),
                enabled = obj.optBoolean("enabled", true),
                senderFilter = obj.optString("senderFilter", ""),
                simFilter = obj.optString("simFilter", SmsForwardConfig.SIM_ALL),
                keywordFilter = obj.optString("keywordFilter", ""),
                webhookUrl = obj.optString("webhookUrl", ""),
                headersJson = obj.optString("headersJson", "{}")
            )
        }
    }
}
