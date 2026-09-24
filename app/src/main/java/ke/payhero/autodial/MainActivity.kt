package ke.payhero.autodial

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
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
        private const val REQUEST_NOTIFICATIONS = 102
        private const val EXAMPLE_PAYLOAD = "{\n  \"dial\": \"*344#\"\n}"
    }

    private var pendingNumber: String? = null
    private var exitAfterCall = false
    private var uiReady = false
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshUi = object : Runnable {
        override fun run() {
            if (uiReady) {
                refreshStatus()
                refreshApiCard()
            }
            refreshHandler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DialServerService.start(this)
        requestNotificationPermission()

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
        refreshHandler.removeCallbacks(refreshUi)
        refreshHandler.post(refreshUi)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshUi)
    }

    private fun showSetupUi() {
        if (uiReady) {
            refreshStatus()
            refreshApiCard()
            return
        }

        setContentView(R.layout.activity_main)
        uiReady = true

        findViewById<TextView>(R.id.apiPayload).text = EXAMPLE_PAYLOAD
        findViewById<TextView>(R.id.tabHome).setOnClickListener { showTab(0) }
        findViewById<TextView>(R.id.tabApi).setOnClickListener { showTab(1) }
        findViewById<TextView>(R.id.tabSetup).setOnClickListener { showTab(2) }
        findViewById<AppCompatButton>(R.id.roleButton).setOnClickListener {
            requestDialerRole()
        }
        findViewById<AppCompatButton>(R.id.autostartButton).setOnClickListener {
            requestUnrestrictedBattery()
        }
        findViewById<AppCompatButton>(R.id.testButton).setOnClickListener {
            placeCall("254700000000", returnToPreviousApp = false)
        }
        findViewById<AppCompatButton>(R.id.exitButton).setOnClickListener {
            exitToPreviousApp()
        }
        showTab(0)
        refreshStatus()
        refreshApiCard()
    }

    private fun showTab(index: Int) {
        val panels = listOf(
            findViewById<View>(R.id.panelHome),
            findViewById<View>(R.id.panelApi),
            findViewById<View>(R.id.panelSetup)
        )
        val tabs = listOf(
            findViewById<TextView>(R.id.tabHome),
            findViewById<TextView>(R.id.tabApi),
            findViewById<TextView>(R.id.tabSetup)
        )
        panels.forEachIndexed { i, panel ->
            panel.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        tabs.forEachIndexed { i, tab ->
            val selected = i == index
            tab.setBackgroundResource(
                if (selected) R.drawable.bg_tab_selected else R.drawable.bg_tab_idle
            )
            tab.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.ink else R.color.ink_muted)
            )
        }
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

        Dialer.place(this, number)
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

    private fun requestUnrestrictedBattery() {
        if (Build.VERSION.SDK_INT < 23) return
        val power = getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) return
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }
    }

    private fun refreshStatus() {
        if (!uiReady) return
        val statusText = findViewById<TextView>(R.id.statusText)
        val roleButton = findViewById<AppCompatButton>(R.id.roleButton)
        val isDefaultDialer = isDefaultDialer()

        statusText.text = if (isDefaultDialer) {
            "Default phone app"
        } else {
            "Not default yet"
        }
        colorDot(findViewById(R.id.statusDot), if (isDefaultDialer) R.color.ready else R.color.pending)
        roleButton.visibility = if (isDefaultDialer) View.GONE else View.VISIBLE
    }

    private fun refreshApiCard() {
        if (!uiReady) return

        val running = DialApiState.running
        val listenerText = if (running) {
            "Listening on ${DialApiState.PORT}"
        } else {
            DialApiState.lastMessage ?: "Starting…"
        }

        findViewById<TextView>(R.id.apiStatusText).text = if (running) {
            "Listening on port ${DialApiState.PORT}"
        } else {
            DialApiState.lastMessage ?: "Starting local API…"
        }
        findViewById<TextView>(R.id.homeApiStatus).text = listenerText
        colorDot(findViewById(R.id.apiDot), if (running) R.color.ready else R.color.pending)
        colorDot(findViewById(R.id.homeApiDot), if (running) R.color.ready else R.color.pending)

        findViewById<TextView>(R.id.apiLocalUrl).text =
            "http://127.0.0.1:${DialApiState.PORT}/dial"

        val lan = NetworkAddresses.lanIpv4()
        findViewById<TextView>(R.id.apiLanUrl).text = if (lan.isEmpty()) {
            "Connect to Wi‑Fi to see the LAN address"
        } else {
            lan.joinToString("\n") { ip ->
                "http://$ip:${DialApiState.PORT}/dial"
            }
        }

        val lastDial = DialApiState.lastDial
        val lastMessage = DialApiState.lastMessage
        findViewById<TextView>(R.id.apiLastRequest).text = when {
            lastDial != null && lastMessage != null -> "Last request: $lastDial · $lastMessage"
            lastMessage != null -> lastMessage
            else -> "No API requests yet"
        }
    }

    private fun colorDot(dot: View, colorRes: Int) {
        val background = dot.background as? GradientDrawable
            ?: GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                dot.background = this
            }
        background.setColor(ContextCompat.getColor(this, colorRes))
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
