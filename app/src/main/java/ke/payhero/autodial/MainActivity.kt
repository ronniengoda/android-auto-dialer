package ke.payhero.autodial

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CALL_PHONE = 100
        private const val REQUEST_DIALER_ROLE = 101
    }

    private var pendingNumber: String? = null
    private var exitAfterCall = false
    private var uiReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingNumber = extractTelNumber(intent)
        if (incomingNumber != null) {
            placeCallAndReturn(incomingNumber)
            return
        }

        showSetupUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incomingNumber = extractTelNumber(intent)
        if (incomingNumber != null) {
            placeCallAndReturn(incomingNumber)
        }
    }

    override fun onResume() {
        super.onResume()
        if (uiReady) refreshStatus()
    }

    private fun showSetupUi() {
        if (uiReady) {
            refreshStatus()
            return
        }

        setContentView(R.layout.activity_main)
        uiReady = true

        findViewById<AppCompatButton>(R.id.roleButton).setOnClickListener {
            requestDialerRole()
        }
        findViewById<AppCompatButton>(R.id.testButton).setOnClickListener {
            placeCall("254700000000", returnToPreviousApp = false)
        }
        findViewById<AppCompatButton>(R.id.exitButton).setOnClickListener {
            exitToPreviousApp()
        }
        refreshStatus()
    }

    private fun extractTelNumber(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (!data.scheme.equals("tel", ignoreCase = true)) return null
        return data.schemeSpecificPart?.takeIf { it.isNotBlank() }
    }

    private fun placeCallAndReturn(number: String) {
        placeCall(number, returnToPreviousApp = true)
    }

    private fun placeCall(number: String, returnToPreviousApp: Boolean) {
        pendingNumber = number
        exitAfterCall = returnToPreviousApp

        if (checkSelfPermission(Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (!uiReady) showSetupUi()
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
            return
        }

        val uri = Uri.parse("tel:${Uri.encode(number)}")
        startActivity(
            Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        pendingNumber = null

        if (returnToPreviousApp) {
            exitToPreviousApp()
        }
    }

    private fun exitToPreviousApp() {
        moveTaskToBack(true)
        finishAndRemoveTask()
    }

    private fun requestDialerRole() {
        if (android.os.Build.VERSION.SDK_INT < 29) {
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(
                        TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                        packageName
                    )
                }
            )
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

    private fun refreshStatus() {
        val statusText = findViewById<TextView>(R.id.statusText)
        val statusDot = findViewById<View>(R.id.statusDot)
        val roleButton = findViewById<AppCompatButton>(R.id.roleButton)

        val isDefaultDialer = isDefaultDialer()
        statusText.text = if (isDefaultDialer) {
            "Ready as the default phone app"
        } else {
            "Not the default phone app yet"
        }

        val dot = statusDot.background as? GradientDrawable
            ?: GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                statusDot.background = this
            }
        dot.setColor(
            ContextCompat.getColor(
                this,
                if (isDefaultDialer) R.color.ready else R.color.pending
            )
        )

        roleButton.visibility = if (isDefaultDialer) View.GONE else View.VISIBLE
    }

    private fun isDefaultDialer(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            return getSystemService(RoleManager::class.java)
                .isRoleHeld(RoleManager.ROLE_DIALER)
        }
        val telecom = getSystemService(TELECOM_SERVICE) as TelecomManager
        return telecom.defaultDialerPackage == packageName
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CALL_PHONE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            val number = pendingNumber ?: return
            placeCall(number, returnToPreviousApp = exitAfterCall)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DIALER_ROLE && uiReady) {
            refreshStatus()
        }
    }
}
