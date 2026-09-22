package ke.payhero.autodial

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.telecom.TelecomManager
import android.content.Context

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_CALL_PHONE = 100
        private const val REQUEST_DIALER_ROLE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "AutoDial"
            textSize = 28f
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        val roleButton = Button(this).apply {
            text = "Make AutoDial the default phone app"
            setOnClickListener { requestDialerRole() }
        }

        val testButton = Button(this).apply {
            text = "Test call"
            setOnClickListener { placeCall("254700000000") }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(roleButton)
        layout.addView(testButton)
        setContentView(layout)

        updateStatus(status)
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme.equals("tel", ignoreCase = true)) {
            val number = data.schemeSpecificPart
            if (!number.isNullOrBlank()) {
                placeCall(number)
            }
        }
    }

    private fun placeCall(number: String) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE
            )
            return
        }

        val uri = Uri.parse("tel:${Uri.encode(number)}")

        // ACTION_CALL starts the cellular call directly rather than
        // displaying the green Call button.
        startActivity(
            Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun requestDialerRole() {
        if (android.os.Build.VERSION.SDK_INT < 29) {
            // Older Android versions can use the legacy TelecomManager flow.
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    packageName
                )
            }
            startActivity(intent)
            return
        }

        val roleManager = getSystemService(RoleManager::class.java)

        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER),
                REQUEST_DIALER_ROLE
            )
        }
    }

    private fun updateStatus(status: TextView) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val rm = getSystemService(RoleManager::class.java)
            status.text = if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                "✓ AutoDial is the default phone app"
            } else {
                "AutoDial is not the default phone app"
            }
        } else {
            status.text = "Android version supports legacy dialer selection"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CALL_PHONE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            val number = intent?.data?.schemeSpecificPart
            if (!number.isNullOrBlank()) placeCall(number)
        }
    }
}
