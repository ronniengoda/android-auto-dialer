package ke.payhero.autodial

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ke.payhero.autodial.ui.components.FeatherIcons
import ke.payhero.autodial.ui.screens.ApiScreen
import ke.payhero.autodial.ui.screens.HomeScreen
import ke.payhero.autodial.ui.screens.PermissionsScreen
import ke.payhero.autodial.ui.screens.SmsScreen
import ke.payhero.autodial.ui.theme.AutoDialTheme
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CALL_PHONE = 100
        private const val REQUEST_NOTIFICATIONS = 102
        private const val REQUEST_CALL_PHONE_ONBOARDING = 103
        private const val REQUEST_NOTIFICATIONS_ONBOARDING = 104
        private const val REQUEST_SMS = 106
        private const val REQUEST_SMS_ONBOARDING = 107
        private const val PREFS = "autodial_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }

    private var pendingNumber: String? = null
    private var exitAfterCall = false
    private var onboardingActive = false
    private var waitingForBattery = false
    private var askedBattery = false
    private var askedSms = false
    private var pendingEnableSms = false

    private val bgExecutor = Executors.newSingleThreadExecutor()

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

        setContent {
            AutoDialApp()
        }

        if (!isOnboardingDone()) {
            Handler(Looper.getMainLooper()).postDelayed({
                startOnboarding(force = false)
            }, 500)
        }
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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForBattery) {
            waitingForBattery = false
            continueOnboarding()
        }
    }

    @Composable
    private fun AutoDialApp() {
        var selectedTab by remember { mutableIntStateOf(0) }
        var isListening by remember { mutableStateOf(DialApiState.running) }
        var lastDial by remember { mutableStateOf(DialApiState.lastDial) }
        var lastMessage by remember { mutableStateOf(DialApiState.lastMessage) }

        val config = remember { SmsForwardConfig(this) }
        var smsEnabled by remember { mutableStateOf(config.enabled) }
        var autoRetry by remember { mutableStateOf(config.autoRetryOnline) }
        var rules by remember { mutableStateOf(config.getRules()) }

        val store = remember { SmsHistoryStore.get(this) }
        var historyRecords by remember { mutableStateOf(store.recent(200)) }

        var hasCallPhonePermission by remember { mutableStateOf(hasCallPhone()) }
        var hasNotificationsPermission by remember { mutableStateOf(hasNotifications()) }
        var hasSmsPermission by remember { mutableStateOf(hasSmsReceive()) }
        var canStartOnRebootState by remember { mutableStateOf(canStartOnReboot()) }

        fun refreshState() {
            isListening = DialApiState.running
            lastDial = DialApiState.lastDial
            lastMessage = DialApiState.lastMessage
            smsEnabled = config.enabled
            autoRetry = config.autoRetryOnline
            rules = config.getRules()
            historyRecords = store.recent(200)
            hasCallPhonePermission = hasCallPhone()
            hasNotificationsPermission = hasNotifications()
            hasSmsPermission = hasSmsReceive()
            canStartOnRebootState = canStartOnReboot()
        }

        LaunchedEffect(Unit) {
            while (true) {
                refreshState()
                delay(1200)
            }
        }

        AutoDialTheme {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(FeatherIcons.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(FeatherIcons.Globe, contentDescription = "Dial API") },
                            label = { Text("Dial API") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(FeatherIcons.MessageSquare, contentDescription = "SMS") },
                            label = { Text("SMS") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(FeatherIcons.Shield, contentDescription = "Access") },
                            label = { Text("Access") }
                        )
                    }
                }
            ) { paddingValues ->
                val modifier = Modifier.padding(paddingValues)
                when (selectedTab) {
                    0 -> HomeScreen(
                        isListening = isListening,
                        hasCallPermission = hasCallPhonePermission,
                        smsEnabled = smsEnabled,
                        rulesCount = rules.size,
                        smsTotalCount = store.count(),
                        smsFailedCount = store.countWhere(SmsHistoryStore.STATUS_FAILED) + store.countWhere(SmsHistoryStore.STATUS_PENDING),
                        grantedPermissionsCount = listOf(hasCallPhonePermission, hasNotificationsPermission, hasSmsPermission, canStartOnRebootState).count { it },
                        totalPermissionsCount = 4,
                        lastDialActivity = when {
                            lastDial != null && lastMessage != null -> "$lastDial · $lastMessage"
                            lastMessage != null -> lastMessage
                            else -> null
                        },
                        onNavigateTab = { tab -> selectedTab = tab },
                        onAddRuleClicked = { selectedTab = 2 },
                        modifier = modifier
                    )
                    1 -> ApiScreen(
                        isListening = isListening,
                        lastDial = lastDial,
                        lastMessage = lastMessage,
                        onSendTestDial = { number ->
                            placeCall(number, returnToPreviousApp = false)
                            refreshState()
                        },
                        modifier = modifier
                    )
                    2 -> SmsScreen(
                        smsEnabled = smsEnabled,
                        autoRetry = autoRetry,
                        rules = rules,
                        historyRecords = historyRecords,
                        onToggleSmsEnabled = { enabled ->
                            if (enabled && !hasSmsReceive()) {
                                pendingEnableSms = true
                                requestSmsPermissions(REQUEST_SMS)
                            } else {
                                config.enabled = enabled
                                refreshState()
                            }
                        },
                        onToggleAutoRetry = { enabled ->
                            config.autoRetryOnline = enabled
                            if (enabled) SmsForwarder.retryPendingIfOnline(this@MainActivity)
                            refreshState()
                        },
                        onSaveRule = { ruleToSave ->
                            val existing = config.getRule(ruleToSave.id)
                            if (existing != null) {
                                config.updateRule(ruleToSave)
                            } else {
                                config.addRule(ruleToSave)
                            }
                            refreshState()
                            Toast.makeText(this@MainActivity, "Rule Saved", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteRule = { ruleId ->
                            config.deleteRule(ruleId)
                            refreshState()
                            Toast.makeText(this@MainActivity, "Rule Deleted", Toast.LENGTH_SHORT).show()
                        },
                        onTestRule = { ruleToTest ->
                            bgExecutor.execute {
                                val result = SmsForwardClient.testRule(ruleToTest)
                                Handler(Looper.getMainLooper()).post {
                                    if (result.success) {
                                        Toast.makeText(this@MainActivity, "Test Webhook Delivered Successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Test Webhook Failed: ${result.error}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onRetryRecord = { recordId ->
                            SmsForwarder.retry(this@MainActivity, recordId)
                            Handler(Looper.getMainLooper()).postDelayed({ refreshState() }, 500)
                        },
                        onRetryFailed = { ruleId ->
                            SmsForwarder.retryFailed(this@MainActivity, ruleId)
                            Handler(Looper.getMainLooper()).postDelayed({ refreshState() }, 500)
                        },
                        modifier = modifier
                    )
                    3 -> PermissionsScreen(
                        hasCallPhone = hasCallPhonePermission,
                        hasNotifications = hasNotificationsPermission,
                        hasSms = hasSmsPermission,
                        canStartOnReboot = canStartOnRebootState,
                        onRequestCallPhone = {
                            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
                        },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
                            }
                        },
                        onRequestSms = { requestSmsPermissions(REQUEST_SMS) },
                        onRequestUnrestrictedBattery = { requestUnrestrictedBattery() },
                        onGrantRemainingClicked = { startOnboarding(force = true) },
                        modifier = modifier
                    )
                }
            }
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

    private fun startOnboarding(force: Boolean) {
        if (onboardingActive) return
        if (!force && isOnboardingDone()) return
        onboardingActive = true
        askedBattery = false
        askedSms = false
        AlertDialog.Builder(this)
            .setTitle("Allow Access")
            .setMessage("AutoDial will ask for each permission in order: phone calls, notifications, SMS, then start on reboot.")
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
            !canStartOnReboot() && !askedBattery -> {
                askedBattery = true
                waitingForBattery = true
                requestUnrestrictedBattery()
            }
            else -> finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        onboardingActive = false
        waitingForBattery = false
        prefs().edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        maybePlacePendingCall()
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
        }

        if (requestCode == REQUEST_CALL_PHONE_ONBOARDING ||
            requestCode == REQUEST_NOTIFICATIONS_ONBOARDING ||
            requestCode == REQUEST_SMS_ONBOARDING
        ) {
            continueOnboarding()
        }
    }
}
