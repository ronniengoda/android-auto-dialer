package ke.payhero.autodial

import android.Manifest
import android.animation.ObjectAnimator
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
        private const val SMS_PAGE_SIZE = 5
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
    private var smsHistoryPage = 0
    private var livePulse: ObjectAnimator? = null
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
        stopLivePulse()
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
        findViewById<View>(R.id.exitButton).setOnClickListener { exitToPreviousApp() }
        findViewById<View>(R.id.testButton).setOnClickListener {
            placeCall("254700000000", returnToPreviousApp = false)
        }
        findViewById<View>(R.id.grantRemainingButton).setOnClickListener {
            startOnboarding(force = true)
        }
        findViewById<View>(R.id.homeHero).setOnClickListener { showTab(1) }
        findViewById<View>(R.id.homeStatDialer).setOnClickListener { showTab(3) }
        findViewById<View>(R.id.homeStatSms).setOnClickListener { showTab(2) }
        findViewById<View>(R.id.homeStatAccess).setOnClickListener { showTab(3) }
        findViewById<View>(R.id.homeGoApi).setOnClickListener { showTab(1) }
        findViewById<View>(R.id.homeGoSms).setOnClickListener { showTab(2) }
        findViewById<View>(R.id.homeGoAccess).setOnClickListener { showTab(3) }
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
            R.drawable.ic_perm_phone,
            R.drawable.bg_glyph_green,
            "Default Phone App",
            "Required for silent dialing"
        )
        bindStatusRow(
            findViewById(R.id.homeListenerRow),
            R.drawable.ic_tab_api,
            R.drawable.bg_glyph_indigo,
            "Local Listener",
            "HTTP API on port ${DialApiState.PORT}"
        )
        bindDestination(
            findViewById(R.id.homeGoApi),
            R.drawable.ic_tab_api,
            R.drawable.bg_glyph_indigo,
            "Dial API",
            "Endpoints, payload, and last request"
        )
        bindDestination(
            findViewById(R.id.homeGoSms),
            R.drawable.ic_perm_sms,
            R.drawable.bg_glyph_green,
            "Messages",
            "Forward matching SMS to a webhook"
        )
        bindDestination(
            findViewById(R.id.homeGoAccess),
            R.drawable.ic_tab_permissions,
            R.drawable.bg_glyph_orange,
            "Access",
            "Permissions and default phone app"
        )
    }

    private fun bindStatusRow(
        row: View,
        icon: Int,
        well: Int,
        title: String,
        subtitle: String
    ) {
        row.findViewById<View>(R.id.rowIconWell).setBackgroundResource(well)
        row.findViewById<ImageView>(R.id.rowIcon).apply {
            setImageResource(icon)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        row.findViewById<TextView>(R.id.rowTitle).text = title
        row.findViewById<TextView>(R.id.rowSubtitle).text = subtitle
    }

    private fun bindDestination(
        row: View,
        icon: Int,
        well: Int,
        title: String,
        subtitle: String
    ) {
        row.findViewById<View>(R.id.destIconWell).setBackgroundResource(well)
        row.findViewById<ImageView>(R.id.destIcon).apply {
            setImageResource(icon)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        row.findViewById<TextView>(R.id.destTitle).text = title
        row.findViewById<TextView>(R.id.destSubtitle).text = subtitle
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
        val active = ContextCompat.getColor(this, R.color.ios_blue)
        val idle = ContextCompat.getColor(this, R.color.secondary_label)

        panels.forEachIndexed { i, panel ->
            panel.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        icons.forEachIndexed { i, icon ->
            icon.setColorFilter(if (i == index) active else idle, PorterDuff.Mode.SRC_IN)
        }
        labels.forEachIndexed { i, label ->
            label.setTextColor(if (i == index) active else idle)
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
            .setTitle("Allow Access")
            .setMessage("AutoDial will ask for each permission in order: phone calls, notifications, SMS, default phone app, then start on reboot.")
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
            val wells = listOf(
                R.drawable.bg_glyph_green,
                R.drawable.bg_glyph_red,
                R.drawable.bg_glyph_blue,
                R.drawable.bg_glyph_teal,
                R.drawable.bg_glyph_orange
            )
            row.findViewById<View>(R.id.permIconWell).setBackgroundResource(
                wells.getOrElse(index) { R.drawable.bg_glyph_blue }
            )
            val chip = row.findViewById<TextView>(R.id.permChip)
            chip.setBackgroundColor(Color.TRANSPARENT)
            if (enabled) {
                chip.text = "On"
                chip.setTextColor(ContextCompat.getColor(this, R.color.secondary_label))
            } else {
                chip.text = "Allow"
                chip.setTextColor(ContextCompat.getColor(this, R.color.ios_blue))
            }
            row.findViewById<View>(R.id.permChevron).visibility =
                if (enabled) View.GONE else View.VISIBLE
            row.setOnClickListener {
                if (!item.enabled()) item.request()
            }
            list.addView(row)
        }
    }

    private fun refreshHome() {
        if (!uiReady) return

        findViewById<TextView>(R.id.homeGreeting).text = greeting()

        val listening = DialApiState.running
        findViewById<View>(R.id.homeLiveDot).setBackgroundResource(
            if (listening) R.drawable.bg_live_dot else R.drawable.bg_live_dot_idle
        )
        val liveLabel = findViewById<TextView>(R.id.homeLiveLabel)
        liveLabel.text = if (listening) "LIVE" else "STARTING"
        liveLabel.setTextColor(
            ContextCompat.getColor(this, if (listening) R.color.ios_green else R.color.ios_orange)
        )
        findViewById<TextView>(R.id.homeHeroTitle).text =
            if (listening) "Listening" else "Waking Up"
        findViewById<TextView>(R.id.homeHeroSubtitle).text =
            if (listening) {
                "Local API on port ${DialApiState.PORT}"
            } else {
                DialApiState.lastMessage ?: "Starting the local listener…"
            }

        val lastDial = DialApiState.lastDial
        val lastMessage = DialApiState.lastMessage
        findViewById<TextView>(R.id.homeLastActivity).text = when {
            lastDial != null && lastMessage != null -> "Last request  $lastDial · $lastMessage"
            lastMessage != null -> lastMessage
            else -> "Waiting for the first companion request"
        }
        updateLivePulse(listening)

        val phoneReady = isDefaultDialer()
        findViewById<View>(R.id.homePhoneRow).findViewById<TextView>(R.id.rowSubtitle).text =
            if (phoneReady) {
                "On — set as the default phone app"
            } else {
                "Set this as the default phone app"
            }
        findViewById<TextView>(R.id.homeStatDialerValue).text = if (phoneReady) "On" else "Set Up"
        findViewById<TextView>(R.id.homeStatDialerHint).text =
            if (phoneReady) "Default app" else "Required"

        val listenerRow = findViewById<View>(R.id.homeListenerRow)
        listenerRow.findViewById<TextView>(R.id.rowSubtitle).text = if (listening) {
            "On — listening on port ${DialApiState.PORT}"
        } else {
            DialApiState.lastMessage ?: "Starting local listener…"
        }

        val store = SmsHistoryStore.get(this)
        val smsTotal = store.count()
        val smsFailed = store.countWhere(SmsHistoryStore.STATUS_FAILED) +
            store.countWhere(SmsHistoryStore.STATUS_PENDING)
        val smsOn = SmsForwardConfig(this).enabled
        findViewById<TextView>(R.id.homeStatSmsValue).text = when {
            !smsOn -> "Off"
            smsFailed > 0 -> "$smsFailed"
            else -> "$smsTotal"
        }
        findViewById<TextView>(R.id.homeStatSmsHint).text = when {
            !smsOn -> "Forwarder"
            smsFailed > 0 -> if (smsFailed == 1) "Needs retry" else "Need retry"
            smsTotal == 0 -> "No messages"
            else -> if (smsTotal == 1) "Forwarded" else "Forwarded"
        }

        val items = permissionItems()
        val accessOn = items.count { it.enabled() }
        val accessTotal = items.size
        findViewById<TextView>(R.id.homeStatAccessValue).text = "$accessOn/$accessTotal"
        findViewById<TextView>(R.id.homeStatAccessHint).text =
            if (accessOn == accessTotal) "All set" else "Remaining"

        val apiValue = findViewById<View>(R.id.homeGoApi).findViewById<TextView>(R.id.destValue)
        apiValue.text = if (listening) "On" else "…"
        val smsValue = findViewById<View>(R.id.homeGoSms).findViewById<TextView>(R.id.destValue)
        smsValue.text = if (smsOn) "On" else "Off"
        val accessValue = findViewById<View>(R.id.homeGoAccess).findViewById<TextView>(R.id.destValue)
        accessValue.text = "$accessOn/$accessTotal"
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun updateLivePulse(active: Boolean) {
        val dot = findViewById<View>(R.id.homeLiveDot)
        if (active) {
            if (livePulse == null) {
                livePulse = ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.28f).apply {
                    duration = 900
                    repeatMode = ObjectAnimator.REVERSE
                    repeatCount = ObjectAnimator.INFINITE
                    start()
                }
            }
        } else {
            stopLivePulse()
            dot.alpha = 1f
        }
    }

    private fun stopLivePulse() {
        livePulse?.cancel()
        livePulse = null
        if (uiReady) {
            findViewById<View>(R.id.homeLiveDot).alpha = 1f
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
        findViewById<View>(R.id.smsHistoryPrev).setOnClickListener {
            if (smsHistoryPage > 0) {
                smsHistoryPage -= 1
                refreshSmsHistory()
            }
        }
        findViewById<View>(R.id.smsHistoryNext).setOnClickListener {
            smsHistoryPage += 1
            refreshSmsHistory()
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
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun updateSimChips() {
        val chips = mapOf(
            SmsForwardConfig.SIM_ALL to findViewById<TextView>(R.id.smsSimAll),
            SmsForwardConfig.SIM_1 to findViewById<TextView>(R.id.smsSim1),
            SmsForwardConfig.SIM_2 to findViewById<TextView>(R.id.smsSim2)
        )
        chips.forEach { (value, view) ->
            view.isSelected = value == selectedSim
            view.setTextColor(ContextCompat.getColor(this, R.color.label))
        }
    }

    private fun updateSmsListenerHint() {
        val enabled = findViewById<SwitchCompat>(R.id.smsEnabledSwitch).isChecked
        findViewById<TextView>(R.id.smsListenerHint).text = if (enabled) {
            "Listening for matching incoming SMS"
        } else {
            "Off"
        }
    }

    private fun refreshSmsHistory() {
        if (!uiReady) return
        val store = SmsHistoryStore.get(this)
        val total = store.count()
        val pages = if (total == 0) 1 else (total + SMS_PAGE_SIZE - 1) / SMS_PAGE_SIZE
        if (smsHistoryPage > pages - 1) smsHistoryPage = pages - 1
        if (smsHistoryPage < 0) smsHistoryPage = 0
        val records = store.page(smsHistoryPage, SMS_PAGE_SIZE)
        val list = findViewById<LinearLayout>(R.id.smsHistoryList)
        val empty = findViewById<TextView>(R.id.smsHistoryEmpty)
        list.removeAllViews()
        empty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE

        val pager = findViewById<View>(R.id.smsHistoryPager)
        pager.visibility = if (total == 0) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.smsHistoryPage).text = "${smsHistoryPage + 1} / $pages"
        val prev = findViewById<TextView>(R.id.smsHistoryPrev)
        val next = findViewById<TextView>(R.id.smsHistoryNext)
        val hasPrev = smsHistoryPage > 0
        val hasNext = smsHistoryPage < pages - 1
        prev.isEnabled = hasPrev
        next.isEnabled = hasNext
        prev.alpha = if (hasPrev) 1f else 0.35f
        next.alpha = if (hasNext) 1f else 0.35f

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
            row.findViewById<TextView>(R.id.smsWhen).text = forwardedLabel(record)
            row.findViewById<TextView>(R.id.smsSim).text = simLabel(record.sim)
            val attempts = record.attempts
            row.findViewById<TextView>(R.id.smsAttempts).text =
                if (attempts == 1) "1 attempt" else "$attempts attempts"
            val errorView = row.findViewById<TextView>(R.id.smsError)
            val error = record.error
            if (error.isNullOrBlank()) {
                errorView.visibility = View.GONE
            } else {
                errorView.visibility = View.VISIBLE
                errorView.text = error
            }

            val chip = row.findViewById<TextView>(R.id.smsStatus)
            when (record.status) {
                SmsHistoryStore.STATUS_SUCCESS -> {
                    chip.text = "Sent"
                    chip.setBackgroundResource(R.drawable.bg_chip_on)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.ios_green))
                }
                SmsHistoryStore.STATUS_PENDING -> {
                    chip.text = "Pending"
                    chip.setBackgroundResource(R.drawable.bg_chip_off)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.ios_orange))
                }
                else -> {
                    chip.text = "Failed"
                    chip.setBackgroundResource(R.drawable.bg_chip_fail)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.ios_red))
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

    private fun forwardedLabel(record: SmsRecord): String {
        val stamp = if (record.forwardedStamp > 0L) record.forwardedStamp else record.receivedStamp
        val prefix = when {
            record.status == SmsHistoryStore.STATUS_SUCCESS && record.forwardedStamp > 0L -> "Forwarded"
            record.forwardedStamp > 0L -> "Tried"
            else -> "Received"
        }
        return "$prefix ${formatStamp(stamp)}"
    }

    private fun formatStamp(stamp: Long): String {
        val format = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
        return format.format(Date(stamp))
    }

    private fun simLabel(sim: String): String {
        return when (sim.lowercase(Locale.ROOT)) {
            "sim1" -> "SIM 1"
            "sim2" -> "SIM 2"
            "unknown" -> "Unknown SIM"
            else -> sim.uppercase(Locale.ROOT)
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
