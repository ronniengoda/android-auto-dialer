package ke.payhero.autodial

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

object Dialer {

    fun place(context: Context, raw: String): Boolean {
        val number = raw.trim()
        if (number.isEmpty()) return false

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val uri = Uri.fromParts("tel", number, null)
        return try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecom.placeCall(uri, Bundle())
            true
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }
    }
}
