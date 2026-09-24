package ke.payhero.autodial

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object SmsForwardClient {

    fun post(record: SmsRecord, config: SmsForwardConfig): Result {
        val url = config.webhookUrl
        if (url.isBlank()) {
            return Result(false, "Webhook URL is empty")
        }

        val body = JSONObject()
            .put("from", record.sender)
            .put("text", record.text)
            .put("sentStamp", record.sentStamp)
            .put("receivedStamp", record.receivedStamp)
            .put("sim", record.sim)
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
                config.headers().forEach { (key, value) ->
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

    data class Result(val success: Boolean, val error: String?)
}
