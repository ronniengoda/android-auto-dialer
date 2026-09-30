package ke.payhero.autodial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.payhero.autodial.SmsFilterRule
import ke.payhero.autodial.SmsHistoryStore
import ke.payhero.autodial.SmsRecord
import ke.payhero.autodial.ui.components.FeatherIcons
import ke.payhero.autodial.ui.components.FilterRuleDialog
import ke.payhero.autodial.ui.components.SmsHistoryCard
import ke.payhero.autodial.ui.components.SmsRuleCard

@Composable
fun SmsScreen(
    smsEnabled: Boolean,
    autoRetry: Boolean,
    rules: List<SmsFilterRule>,
    historyRecords: List<SmsRecord>,
    onToggleSmsEnabled: (Boolean) -> Unit,
    onToggleAutoRetry: (Boolean) -> Unit,
    onSaveRule: (SmsFilterRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onTestRule: (SmsFilterRule) -> Unit,
    onRetryRecord: (Long) -> Unit,
    onRetryAllFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<SmsFilterRule?>(null) }
    var selectedTabFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Sent, 2: Pending, 3: Failed

    val filteredRecords = remember(historyRecords, selectedTabFilter) {
        when (selectedTabFilter) {
            1 -> historyRecords.filter { it.status == SmsHistoryStore.STATUS_SUCCESS }
            2 -> historyRecords.filter { it.status == SmsHistoryStore.STATUS_PENDING }
            3 -> historyRecords.filter { it.status == SmsHistoryStore.STATUS_FAILED }
            else -> historyRecords
        }
    }

    if (showRuleDialog) {
        FilterRuleDialog(
            initialRule = editingRule,
            onDismiss = {
                showRuleDialog = false
                editingRule = null
            },
            onSave = { ruleToSave ->
                onSaveRule(ruleToSave)
                showRuleDialog = false
                editingRule = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SMS Forwarder & Webhooks",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Global Toggles Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMS Forwarding Service",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (smsEnabled) "Listening for matching incoming SMS" else "Service is paused",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = smsEnabled,
                            onCheckedChange = onToggleSmsEnabled
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-retry When Online",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically retry failed webhooks when network connects",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = autoRetry,
                            onCheckedChange = onToggleAutoRetry
                        )
                    }
                }
            }
        }

        // Section: Listener Filters Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Listener Rules (${rules.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = {
                        editingRule = null
                        showRuleDialog = true
                    }
                ) {
                    Icon(FeatherIcons.Plus, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule")
                }
            }
        }

        // Listener Rules List
        if (rules.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Listener Rules Configured",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add Rule' above to setup custom webhook filters for M-PESA, bank SMS, or SIM slots.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                SmsRuleCard(
                    rule = rule,
                    onToggleEnabled = { enabled ->
                        onSaveRule(rule.copy(enabled = enabled))
                    },
                    onEdit = {
                        editingRule = rule
                        showRuleDialog = true
                    },
                    onDelete = { onDeleteRule(rule.id) },
                    onTestRule = { onTestRule(rule) }
                )
            }
        }

        // Section: Delivery Logs Header
        item {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Delivery History (${historyRecords.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedButton(onClick = onRetryAllFailed) {
                    Icon(FeatherIcons.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry Failed")
                }
            }
        }

        // Logs Filter Tabs
        item {
            val tabTitles = listOf("All", "Sent", "Pending", "Failed")
            TabRow(selectedTabIndex = selectedTabFilter) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabFilter == index,
                        onClick = { selectedTabFilter = index },
                        text = { Text(title) }
                    )
                }
            }
        }

        // Delivery Logs List
        if (filteredRecords.isEmpty()) {
            item {
                Text(
                    text = "No message history matching filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                SmsHistoryCard(
                    record = record,
                    onRetry = { onRetryRecord(record.id) }
                )
            }
        }
    }
}
