package ke.payhero.autodial

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        Thread {
            try {
                SmsForwarder.onSmsReceived(context, intent)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
