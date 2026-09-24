package ke.payhero.autodial

import android.content.Context
import org.json.JSONObject

class SmsForwardConfig(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var autoRetryOnline: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RETRY, true)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_RETRY, value).apply() }

    var senderIdsRaw: String
        get() = prefs.getString(KEY_SENDERS, "") ?: ""
        set(value) { prefs.edit().putString(KEY_SENDERS, value).apply() }

    var simFilter: String
        get() = prefs.getString(KEY_SIM, SIM_ALL) ?: SIM_ALL
        set(value) { prefs.edit().putString(KEY_SIM, value).apply() }

    var webhookUrl: String
        get() = prefs.getString(KEY_URL, "") ?: ""
        set(value) { prefs.edit().putString(KEY_URL, value.trim()).apply() }

    var headersJson: String
        get() = prefs.getString(KEY_HEADERS, "{}") ?: "{}"
        set(value) { prefs.edit().putString(KEY_HEADERS, value).apply() }

    fun senderIds(): List<String> {
        return senderIdsRaw.split(",", "\n")
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
        val filters = senderIds()
        if (filters.isEmpty()) return false
        if (filters.any { it == "*" }) return true
        val incoming = from.trim()
        return filters.any { incoming.equals(it, true) || incoming.contains(it, true) }
    }

    fun matchesSim(sim: String): Boolean {
        return when (simFilter) {
            SIM_1 -> sim.equals(SIM_1, true)
            SIM_2 -> sim.equals(SIM_2, true)
            else -> true
        }
    }

    companion object {
        const val SIM_ALL = "all"
        const val SIM_1 = "sim1"
        const val SIM_2 = "sim2"
        private const val PREFS = "sms_forward_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_AUTO_RETRY = "auto_retry"
        private const val KEY_SENDERS = "senders"
        private const val KEY_SIM = "sim"
        private const val KEY_URL = "url"
        private const val KEY_HEADERS = "headers"
    }
}
