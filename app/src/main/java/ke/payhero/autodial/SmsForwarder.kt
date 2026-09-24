package ke.payhero.autodial

import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import java.util.concurrent.Executors

object SmsForwarder {
    private val io = Executors.newSingleThreadExecutor()

    fun onSmsReceived(context: Context, intent: Intent) {
        val app = context.applicationContext
        val config = SmsForwardConfig(app)
        if (!config.enabled) return

        val parsed = parseMessages(intent) ?: return
        if (!config.matchesSender(parsed.from)) return

        val sim = resolveSim(app, intent)
        if (!config.matchesSim(sim)) return

        val store = SmsHistoryStore.get(app)
        val id = store.insertPending(
            sender = parsed.from,
            text = parsed.text,
            sentStamp = parsed.sentStamp,
            receivedStamp = System.currentTimeMillis(),
            sim = sim
        )
        forward(app, id)
    }

    fun retry(context: Context, id: Long) {
        io.execute { forward(context.applicationContext, id) }
    }

    fun retryPendingIfOnline(context: Context) {
        if (!SmsForwardConfig(context.applicationContext).autoRetryOnline) return
        retryFailed(context)
    }

    fun retryFailed(context: Context) {
        val app = context.applicationContext
        io.execute {
            SmsHistoryStore.get(app).retryable().forEach { record ->
                forward(app, record.id)
            }
        }
    }

    fun enqueueIncoming(context: Context, intent: Intent) {
        io.execute { onSmsReceived(context, intent) }
    }

    private fun forward(context: Context, id: Long) {
        val store = SmsHistoryStore.get(context)
        val record = store.get(id) ?: return
        val config = SmsForwardConfig(context)
        val result = SmsForwardClient.post(record, config)
        store.mark(
            id,
            if (result.success) SmsHistoryStore.STATUS_SUCCESS else SmsHistoryStore.STATUS_FAILED,
            result.error
        )
    }

    private fun parseMessages(intent: Intent): ParsedSms? {
        val extras = intent.extras ?: return null
        val pdus = extras.get("pdus") as? Array<*> ?: return null
        val format = extras.getString("format")
        val messages = pdus.mapNotNull { pdu ->
            val bytes = pdu as? ByteArray ?: return@mapNotNull null
            if (format != null) {
                SmsMessage.createFromPdu(bytes, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(bytes)
            }
        }
        if (messages.isEmpty()) return null
        val from = messages.first().displayOriginatingAddress
            ?: messages.first().originatingAddress
            ?: return null
        val text = messages.joinToString("") { message ->
            message.displayMessageBody ?: message.messageBody.orEmpty()
        }
        if (text.isBlank()) return null
        return ParsedSms(
            from = from,
            text = text,
            sentStamp = messages.first().timestampMillis
        )
    }

    private fun resolveSim(context: Context, intent: Intent): String {
        val slotExtras = listOf("slot", "simId", "sim_slot", "slot_id", "phone")
        for (key in slotExtras) {
            val value = if (intent.hasExtra(key)) intent.getIntExtra(key, -1) else -1
            if (value >= 0) return "sim${value + 1}"
        }

        val subscription = when {
            intent.hasExtra("subscription") -> intent.getIntExtra("subscription", -1)
            intent.hasExtra("android.telephony.extra.SUBSCRIPTION_INDEX") ->
                intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
            else -> -1
        }
        if (subscription >= 0 && Build.VERSION.SDK_INT >= 22) {
            try {
                val manager = context.getSystemService(SubscriptionManager::class.java)
                val info = manager?.getActiveSubscriptionInfo(subscription)
                val slot = info?.simSlotIndex ?: -1
                if (slot >= 0) return "sim${slot + 1}"
            } catch (_: SecurityException) {
            }
        }
        return "unknown"
    }

    private data class ParsedSms(
        val from: String,
        val text: String,
        val sentStamp: Long
    )
}
