package ke.payhero.autodial

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object SmsForwardClient {

    fun post(record: SmsRecord, url: String, headers: Map<String, String>): Result {
        if (url.isBlank()) {
            return Result(false, "Webhook URL is empty")
        }

        val body = JSONObject()
            .put("from", record.sender)
            .put("text", record.text)
            .put("sentStamp", record.sentStamp)
            .put("receivedStamp", record.receivedStamp)
            .put("sim", record.sim)
            .apply {
                if (!record.ruleId.isNullOrBlank()) put("ruleId", record.ruleId)
                if (!record.ruleName.isNullOrBlank()) put("ruleName", record.ruleName)
            }
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            connection.disconnect()
            if (code in 200..299) {
                Result(true, null)
            } else {
                Result(false, "HTTP $code")
            }
        } catch (error: IOException) {
            Result(false, error.message ?: "Network error")
        } catch (error: Exception) {
            Result(false, error.message ?: "Forward failed")
        }
    }

    fun testRule(rule: SmsFilterRule): Result {
        val dummyRecord = SmsRecord(
            id = -1,
            sender = "TEST_SENDER",
            text = "Test SMS Payload from AutoDial rule '${rule.name}'",
            sentStamp = System.currentTimeMillis(),
            receivedStamp = System.currentTimeMillis(),
            sim = "sim1",
            status = SmsHistoryStore.STATUS_PENDING,
            error = null,
            attempts = 0,
            forwardedStamp = System.currentTimeMillis(),
            ruleId = rule.id,
            ruleName = rule.name,
            webhookUrl = rule.webhookUrl
        )
        return post(dummyRecord, rule.webhookUrl, rule.headers())
    }

    data class Result(val success: Boolean, val error: String?)
}
