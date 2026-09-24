package ke.payhero.autodial

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CALL_PHONE = 100
        private const val REQUEST_DIALER_ROLE = 101
        private const val REQUEST_NOTIFICATIONS = 102
        private const val REQUEST_CALL_PHONE_ONBOARDING = 103
        private const val REQUEST_NOTIFICATIONS_ONBOARDING = 104
        private const val REQUEST_SMS = 106
        private const val REQUEST_SMS_ONBOARDING = 107
        private const val PREFS = "autodial_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val EXAMPLE_PAYLOAD = "{\n  \"dial\": \"*344#\"\n}"
    }

    private var pendingNumber: String? = null
    private var exitAfterCall = false
    private var uiReady = false
    private var onboardingActive = false
    private var waitingForBattery = false
    private var askedDialer = false
    private var askedBattery = false
    private var askedSms = false
    private var pendingEnableSms = false
    private var selectedSim = SmsForwardConfig.SIM_ALL
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshUi = object : Runnable {
        override fun run() {
            if (uiReady) {
                refreshHome()
                refreshApiCard()
                refreshPermissionList()
                refreshSmsHistory()
            }
            refreshHandler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DialServerService.start(this)

        val incomingNumber = extractTelNumber(intent)
        if (incomingNumber != null && isOnboardingDone() && hasCallPhone()) {
            placeCallAndReturn(incomingNumber)
            return
        }

        if (incomingNumber != null) {
            pendingNumber = incomingNumber
            exitAfterCall = true
        }

        showSetupUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incomingNumber = extractTelNumber(intent)
        if (incomingNumber != null) {
            if (isOnboardingDone() && hasCallPhone()) {
                placeCallAndReturn(incomingNumber)
            } else {
                pendingNumber = incomingNumber
                exitAfterCall = true
                if (!uiReady) showSetupUi()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.removeCallbacks(refreshUi)
        refreshHandler.post(refreshUi)
        if (waitingForBattery) {
            waitingForBattery = false
            continueOnboarding()
        }
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshUi)
    }

    private fun showSetupUi() {
        if (uiReady) {
            refreshHome()
            refreshApiCard()
            refreshPermissionList()
            return
        }

        setContentView(R.layout.activity_main)
        uiReady = true

        findViewById<TextView>(R.id.apiPayload).text = EXAMPLE_PAYLOAD
        findViewById<View>(R.id.tabHome).setOnClickListener { showTab(0) }
        findViewById<View>(R.id.tabApi).setOnClickListener { showTab(1) }
        findViewById<View>(R.id.tabSms).setOnClickListener { showTab(2) }
        findViewById<View>(R.id.tabPermissions).setOnClickListener { showTab(3) }
        findViewById<View>(R.id.headerExit).setOnClickListener { exitToPreviousApp() }
        findViewById<AppCompatButton>(R.id.exitButton).setOnClickListener { exitToPreviousApp() }
        findViewById<AppCompatButton>(R.id.testButton).setOnClickListener {
            placeCall("254700000000", returnToPreviousApp = false)
        }
        findViewById<AppCompatButton>(R.id.grantRemainingButton).setOnClickListener {
            startOnboarding(force = true)
        }
        bindSmsTab()

        bindHomeRows()
        showTab(0)
        refreshHome()
        refreshApiCard()
        refreshPermissionList()
        refreshSmsHistory()

        if (!isOnboardingDone()) {
            refreshHandler.post { startOnboarding(force = false) }
        }
    }

    private fun bindHomeRows() {
        bindStatusRow(
            findViewById(R.id.homePhoneRow),
            R.drawable.ic_perm_dialer,
            "Default phone app",
            "Required for silent dialing"
        )
        bindStatusRow(
            findViewById(R.id.homeListenerRow),
            R.drawable.ic_tab_api,
            "Local listener",
            "HTTP API on port ${DialApiState.PORT}"
        )
    }

    private fun bindStatusRow(row: View, icon: Int, title: String, subtitle: String) {
        row.findViewById<ImageView>(R.id.rowIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.rowTitle).text = title
        row.findViewById<TextView>(R.id.rowSubtitle).text = subtitle
    }

    private fun showTab(index: Int) {
        val panels = listOf(
            findViewById<View>(R.id.panelHome),
            findViewById<View>(R.id.panelApi),
            findViewById<View>(R.id.panelSms),
            findViewById<View>(R.id.panelPermissions)
        )
        val icons = listOf(
            findViewById<ImageView>(R.id.tabHomeIcon),
            findViewById<ImageView>(R.id.tabApiIcon),
            findViewById<ImageView>(R.id.tabSmsIcon),
            findViewById<ImageView>(R.id.tabPermissionsIcon)
        )
        val labels = listOf(
            findViewById<TextView>(R.id.tabHomeLabel),
            findViewById<TextView>(R.id.tabApiLabel),
            findViewById<TextView>(R.id.tabSmsLabel),
            findViewById<TextView>(R.id.tabPermissionsLabel)
        )
        val lines = listOf(
            findViewById<View>(R.id.tabHomeLine),
            findViewById<View>(R.id.tabApiLine),
            findViewById<View>(R.id.tabSmsLine),
            findViewById<View>(R.id.tabPermissionsLine)
        )
        val active = ContextCompat.getColor(this, R.color.mint_deep)
        val idle = ContextCompat.getColor(this, R.color.ink_muted)

        panels.forEachIndexed { i, panel ->
            panel.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        icons.forEachIndexed { i, icon ->
            icon.setColorFilter(if (i == index) active else idle, PorterDuff.Mode.SRC_IN)
        }
        labels.forEachIndexed { i, label ->
            label.setTextColor(if (i == index) active else idle)
        }
        lines.forEachIndexed { i, line ->
            line.visibility = if (i == index) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun startOnboarding(force: Boolean) {
        if (onboardingActive) return
        if (!force && isOnboardingDone()) return
        onboardingActive = true
        askedDialer = false
        askedBattery = false
        askedSms = false
        showTab(3)
        AlertDialog.Builder(this)
            .setTitle("Allow required access")
            .setMessage("AutoDial will now ask for each permission in order: phone calls, notifications, SMS, default phone app, then start on reboot.")
            .setPositiveButton("Continue") { _, _ -> continueOnboarding() }
            .setCancelable(false)
            .show()
    }

    private fun continueOnboarding() {
        if (!onboardingActive) return

        when {
            !hasCallPhone() -> requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_ONBOARDING
            )
            Build.VERSION.SDK_INT >= 33 && !hasNotifications() -> requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS_ONBOARDING
            )
            !hasSmsReceive() && !askedSms -> {
                askedSms = true
                requestSmsPermissions(REQUEST_SMS_ONBOARDING)
            }
            !isDefaultDialer() && !askedDialer -> {
                askedDialer = true
                requestDialerRole()
            }
            !canStartOnReboot() && !askedBattery -> {
                askedBattery = true
                waitingForBattery = true
                requestUnrestrictedBattery()
            }
            else -> finishOnboarding()
        }
        refreshPermissionList()
        refreshHome()
    }

    private fun finishOnboarding() {
        onboardingActive = false
        waitingForBattery = false
        prefs().edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        refreshPermissionList()
        maybePlacePendingCall()
    }

    private fun permissionItems(): List<PermissionItem> {
        return listOf(
            PermissionItem(
                title = "Phone calls",
                subtitle = "Place cellular and USSD calls automatically",
                icon = R.drawable.ic_perm_phone,
                enabled = { hasCallPhone() },
                request = {
                    requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
                }
            ),
            PermissionItem(
                title = "Notifications",
                subtitle = "Keeps the listener visible while it is running",
                icon = R.drawable.ic_perm_bell,
                enabled = { hasNotifications() },
                request = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            REQUEST_NOTIFICATIONS
                        )
                    }
                }
            ),
            PermissionItem(
                title = "SMS access",
                subtitle = "Read incoming messages and identify SIM 1 or SIM 2",
                icon = R.drawable.ic_perm_sms,
                enabled = { hasSmsReceive() },
                request = { requestSmsPermissions(REQUEST_SMS) }
            ),
            PermissionItem(
                title = "Default phone app",
                subtitle = "Required for silent dialing from tel: and the API",
                icon = R.drawable.ic_perm_dialer,
                enabled = { isDefaultDialer() },
                request = { requestDialerRole() }
            ),
            PermissionItem(
                title = "Start on reboot",
                subtitle = "Wakes the HTTP listener after the phone powers on",
                icon = R.drawable.ic_perm_reboot,
                enabled = { canStartOnReboot() },
                request = { requestUnrestrictedBattery() }
            )
        )
    }

    private fun refreshPermissionList() {
        if (!uiReady) return
        val items = permissionItems()
        val enabledCount = items.count { it.enabled() }
        findViewById<TextView>(R.id.permissionSummary).text =
            "$enabledCount of ${items.size} enabled"
        findViewById<AppCompatButton>(R.id.grantRemainingButton).visibility =
            if (enabledCount < items.size) View.VISIBLE else View.GONE

        val list = findViewById<LinearLayout>(R.id.permissionList)
        list.removeAllViews()
        items.forEachIndexed { index, item ->
            if (index > 0) {
                val divider = View(this)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                params.marginStart = (74 * resources.displayMetrics.density).toInt()
                divider.layoutParams = params
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.line))
                list.addView(divider)
            }

            val row = layoutInflater.inflate(R.layout.view_permission_row, list, false)
            row.findViewById<ImageView>(R.id.permIcon).setImageResource(item.icon)
            row.findViewById<TextView>(R.id.permTitle).text = item.title
            row.findViewById<TextView>(R.id.permSubtitle).text = item.subtitle

            val enabled = item.enabled()
            val chip = row.findViewById<TextView>(R.id.permChip)
            chip.text = if (enabled) "Enabled" else "Available"
            chip.setBackgroundResource(if (enabled) R.drawable.bg_chip_on else R.drawable.bg_chip_off)
            chip.setTextColor(
                ContextCompat.getColor(this, if (enabled) R.color.mint_deep else R.color.pending)
            )
            row.setOnClickListener {
                if (!item.enabled()) item.request()
            }
            list.addView(row)
        }
    }

    private fun refreshHome() {
        if (!uiReady) return
        val phoneRow = findViewById<View>(R.id.homePhoneRow)
        phoneRow.findViewById<TextView>(R.id.rowSubtitle).text = if (isDefaultDialer()) {
            "Enabled as the default phone app"
        } else {
            "Available — set this as the default phone app"
        }

        val listenerRow = findViewById<View>(R.id.homeListenerRow)
        listenerRow.findViewById<TextView>(R.id.rowSubtitle).text = if (DialApiState.running) {
            "Enabled — listening on port ${DialApiState.PORT}"
        } else {
            DialApiState.lastMessage ?: "Starting local listener…"
        }
    }

    private fun refreshApiCard() {
        if (!uiReady) return

        findViewById<TextView>(R.id.apiStatusText).text = if (DialApiState.running) {
            "Listening on port ${DialApiState.PORT}"
        } else {
            DialApiState.lastMessage ?: "Starting local API…"
        }

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

        if (!hasCallPhone()) {
            if (!uiReady) showSetupUi()
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
            return
        }

        Dialer.place(this, number)
        pendingNumber = null

        if (returnToPreviousApp && isOnboardingDone()) {
            exitToPreviousApp()
        }
    }

    private fun maybePlacePendingCall() {
        val number = pendingNumber ?: return
        if (!hasCallPhone()) return
        placeCall(number, returnToPreviousApp = exitAfterCall && isOnboardingDone())
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
        } else if (onboardingActive) {
            continueOnboarding()
        }
    }

    private fun requestUnrestrictedBattery() {
        if (Build.VERSION.SDK_INT < 23) {
            if (onboardingActive) continueOnboarding()
            return
        }
        val power = getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) {
            if (onboardingActive) continueOnboarding()
            return
        }
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

    private fun bindSmsTab() {
        val config = SmsForwardConfig(this)
        selectedSim = config.simFilter
        findViewById<SwitchCompat>(R.id.smsEnabledSwitch).isChecked = config.enabled
        findViewById<SwitchCompat>(R.id.smsAutoRetrySwitch).isChecked = config.autoRetryOnline
        findViewById<EditText>(R.id.smsSenders).setText(config.senderIdsRaw)
        findViewById<EditText>(R.id.smsWebhookUrl).setText(config.webhookUrl)
        renderHeaderRows(config.headers())
        updateSimChips()
        updateSmsListenerHint()

        findViewById<SwitchCompat>(R.id.smsEnabledSwitch).setOnCheckedChangeListener { _, checked ->
            if (checked && !hasSmsReceive()) {
                pendingEnableSms = true
                findViewById<SwitchCompat>(R.id.smsEnabledSwitch).isChecked = false
                requestSmsPermissions(REQUEST_SMS)
                return@setOnCheckedChangeListener
            }
            SmsForwardConfig(this).enabled = checked
            updateSmsListenerHint()
        }
        findViewById<SwitchCompat>(R.id.smsAutoRetrySwitch).setOnCheckedChangeListener { _, checked ->
            SmsForwardConfig(this).autoRetryOnline = checked
            if (checked) SmsForwarder.retryPendingIfOnline(this)
        }
        findViewById<View>(R.id.smsSimAll).setOnClickListener {
            selectedSim = SmsForwardConfig.SIM_ALL
            updateSimChips()
        }
        findViewById<View>(R.id.smsSim1).setOnClickListener {
            selectedSim = SmsForwardConfig.SIM_1
            updateSimChips()
        }
        findViewById<View>(R.id.smsSim2).setOnClickListener {
            selectedSim = SmsForwardConfig.SIM_2
            updateSimChips()
        }
        findViewById<View>(R.id.smsAddHeader).setOnClickListener {
            addHeaderRow("", "")
        }
        findViewById<AppCompatButton>(R.id.smsSaveButton).setOnClickListener {
            saveSmsConfig()
        }
        findViewById<View>(R.id.smsRetryFailed).setOnClickListener {
            SmsForwarder.retryFailed(this)
            refreshHandler.postDelayed({ refreshSmsHistory() }, 400)
        }
    }

    private fun renderHeaderRows(headers: Map<String, String>) {
        val list = findViewById<LinearLayout>(R.id.smsHeaderList)
        list.removeAllViews()
        if (headers.isEmpty()) {
            addHeaderRow("", "")
        } else {
            headers.forEach { (key, value) -> addHeaderRow(key, value) }
        }
    }

    private fun addHeaderRow(key: String, value: String) {
        val list = findViewById<LinearLayout>(R.id.smsHeaderList)
        val row = layoutInflater.inflate(R.layout.view_header_row, list, false)
        row.findViewById<EditText>(R.id.headerKey).setText(key)
        row.findViewById<EditText>(R.id.headerValue).setText(value)
        row.findViewById<View>(R.id.headerRemove).setOnClickListener {
            list.removeView(row)
            if (list.childCount == 0) addHeaderRow("", "")
        }
        list.addView(row)
    }

    private fun collectHeadersJson(): String {
        val list = findViewById<LinearLayout>(R.id.smsHeaderList)
        val obj = JSONObject()
        for (i in 0 until list.childCount) {
            val row = list.getChildAt(i)
            val key = row.findViewById<EditText>(R.id.headerKey).text.toString().trim()
            val value = row.findViewById<EditText>(R.id.headerValue).text.toString().trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                obj.put(key, value)
            }
        }
        return obj.toString()
    }

    private fun saveSmsConfig() {
        val config = SmsForwardConfig(this)
        config.senderIdsRaw = findViewById<EditText>(R.id.smsSenders).text.toString()
        config.simFilter = selectedSim
        config.webhookUrl = findViewById<EditText>(R.id.smsWebhookUrl).text.toString()
        config.headersJson = collectHeadersJson()
        config.enabled = findViewById<SwitchCompat>(R.id.smsEnabledSwitch).isChecked
        config.autoRetryOnline = findViewById<SwitchCompat>(R.id.smsAutoRetrySwitch).isChecked
        updateSmsListenerHint()
        Toast.makeText(this, "SMS forwarder saved", Toast.LENGTH_SHORT).show()
    }

    private fun updateSimChips() {
        val chips = mapOf(
            SmsForwardConfig.SIM_ALL to findViewById<TextView>(R.id.smsSimAll),
            SmsForwardConfig.SIM_1 to findViewById<TextView>(R.id.smsSim1),
            SmsForwardConfig.SIM_2 to findViewById<TextView>(R.id.smsSim2)
        )
        chips.forEach { (value, view) ->
            val selected = value == selectedSim
            view.isSelected = selected
            view.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.white else R.color.ink)
            )
        }
    }

    private fun updateSmsListenerHint() {
        val enabled = findViewById<SwitchCompat>(R.id.smsEnabledSwitch).isChecked
        findViewById<TextView>(R.id.smsListenerHint).text = if (enabled) {
            "Listening for matching incoming SMS"
        } else {
            "Disabled"
        }
    }

    private fun refreshSmsHistory() {
        if (!uiReady) return
        val records = SmsHistoryStore.get(this).recent()
        val list = findViewById<LinearLayout>(R.id.smsHistoryList)
        val empty = findViewById<TextView>(R.id.smsHistoryEmpty)
        list.removeAllViews()
        empty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE

        records.forEachIndexed { index, record ->
            if (index > 0) {
                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.line))
                list.addView(divider)
            }
            val row = layoutInflater.inflate(R.layout.view_sms_history_row, list, false)
            row.findViewById<TextView>(R.id.smsFrom).text = record.sender
            row.findViewById<TextView>(R.id.smsBody).text = record.text
            row.findViewById<TextView>(R.id.smsMeta).text =
                "${record.sim} · ${record.attempts} attempt${if (record.attempts == 1) "" else "s"}" +
                    (record.error?.let { " · $it" } ?: "")

            val chip = row.findViewById<TextView>(R.id.smsStatus)
            when (record.status) {
                SmsHistoryStore.STATUS_SUCCESS -> {
                    chip.text = "Sent"
                    chip.setBackgroundResource(R.drawable.bg_chip_on)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.mint_deep))
                }
                SmsHistoryStore.STATUS_PENDING -> {
                    chip.text = "Pending"
                    chip.setBackgroundResource(R.drawable.bg_chip_off)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.pending))
                }
                else -> {
                    chip.text = "Failed"
                    chip.setBackgroundResource(R.drawable.bg_chip_fail)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.failed))
                }
            }

            val retry = row.findViewById<TextView>(R.id.smsRetry)
            val canRetry = record.status != SmsHistoryStore.STATUS_SUCCESS
            retry.visibility = if (canRetry) View.VISIBLE else View.GONE
            retry.setOnClickListener {
                SmsForwarder.retry(this, record.id)
                refreshHandler.postDelayed({ refreshSmsHistory() }, 400)
            }
            list.addView(row)
        }
    }

    private fun requestSmsPermissions(code: Int) {
        requestPermissions(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE
            ),
            code
        )
    }

    private fun hasSmsReceive(): Boolean {
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasCallPhone(): Boolean {
        return checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun canStartOnReboot(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        return getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(packageName)
    }

    private fun isDefaultDialer(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            return getSystemService(RoleManager::class.java)
                .isRoleHeld(RoleManager.ROLE_DIALER)
        }
        val telecom = getSystemService(TELECOM_SERVICE) as TelecomManager
        return telecom.defaultDialerPackage == packageName
    }

    private fun isOnboardingDone(): Boolean {
        return prefs().getBoolean(KEY_ONBOARDING_DONE, false)
    }

    private fun prefs() = getSharedPreferences(PREFS, MODE_PRIVATE)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissionList()
        refreshHome()

        if (requestCode == REQUEST_CALL_PHONE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            maybePlacePendingCall()
        }

        if (requestCode == REQUEST_SMS &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED &&
            pendingEnableSms
        ) {
            pendingEnableSms = false
            SmsForwardConfig(this).enabled = true
            findViewById<SwitchCompat>(R.id.smsEnabledSwitch).isChecked = true
            updateSmsListenerHint()
        }

        if (requestCode == REQUEST_CALL_PHONE_ONBOARDING ||
            requestCode == REQUEST_NOTIFICATIONS_ONBOARDING ||
            requestCode == REQUEST_SMS_ONBOARDING
        ) {
            continueOnboarding()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshPermissionList()
        refreshHome()
        if (requestCode == REQUEST_DIALER_ROLE && onboardingActive) {
            continueOnboarding()
        }
    }

    private data class PermissionItem(
        val title: String,
        val subtitle: String,
        val icon: Int,
        val enabled: () -> Boolean,
        val request: () -> Unit
    )
}
