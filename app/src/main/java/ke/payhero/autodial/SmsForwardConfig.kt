package ke.payhero.autodial

import android.content.Context
import org.json.JSONArray
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

    fun getRules(): List<SmsFilterRule> {
        val jsonStr = prefs.getString(KEY_RULES, null)
        if (jsonStr.isNullOrBlank()) {
            val legacyUrl = prefs.getString(KEY_URL, "")?.trim().orEmpty()
            if (legacyUrl.isNotBlank()) {
                val legacyRule = SmsFilterRule(
                    id = "default_legacy",
                    name = "Default Listener Rule",
                    enabled = true,
                    senderFilter = prefs.getString(KEY_SENDERS, "") ?: "",
                    simFilter = prefs.getString(KEY_SIM, SIM_ALL) ?: SIM_ALL,
                    keywordFilter = "",
                    webhookUrl = legacyUrl,
                    headersJson = prefs.getString(KEY_HEADERS, "{}") ?: "{}"
                )
                val initialList = listOf(legacyRule)
                saveRules(initialList)
                return initialList
            }
            return emptyList()
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SmsFilterRule>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(SmsFilterRule.fromJsonObject(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRules(rules: List<SmsFilterRule>) {
        val array = JSONArray()
        rules.forEach { rule -> array.put(rule.toJson()) }
        prefs.edit().putString(KEY_RULES, array.toString()).apply()
    }

    fun addRule(rule: SmsFilterRule) {
        val current = getRules().toMutableList()
        current.add(0, rule)
        saveRules(current)
    }

    fun updateRule(updated: SmsFilterRule) {
        val current = getRules().map { if (it.id == updated.id) updated else it }
        saveRules(current)
    }

    fun deleteRule(id: String) {
        val current = getRules().filterNot { it.id == id }
        saveRules(current)
    }

    fun getRule(id: String): SmsFilterRule? {
        return getRules().firstOrNull { it.id == id }
    }

    companion object {
        const val SIM_ALL = "all"
        const val SIM_1 = "sim1"
        const val SIM_2 = "sim2"
        private const val PREFS = "sms_forward_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_AUTO_RETRY = "auto_retry"
        private const val KEY_RULES = "sms_rules"
        private const val KEY_SENDERS = "senders"
        private const val KEY_SIM = "sim"
        private const val KEY_URL = "url"
        private const val KEY_HEADERS = "headers"
    }
}
