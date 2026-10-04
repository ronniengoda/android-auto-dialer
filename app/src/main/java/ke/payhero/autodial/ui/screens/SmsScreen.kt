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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ke.payhero.autodial.SmsFilterRule
import ke.payhero.autodial.SmsHistoryStore
import ke.payhero.autodial.SmsRecord
import ke.payhero.autodial.ui.components.FeatherIcons
import ke.payhero.autodial.ui.components.FilterRuleDialog
import ke.payhero.autodial.ui.components.SmsHistoryCard
import ke.payhero.autodial.ui.components.SmsRuleCard

private const val PAGE_SIZE = 10

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
    onRetryFailed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<SmsFilterRule?>(null) }
    var openRuleId by remember { mutableStateOf<String?>(null) }

    val openRule = rules.firstOrNull { it.id == openRuleId }

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

    if (openRule != null) {
        RuleDeliveryHistory(
            rule = openRule,
            historyRecords = historyRecords,
            onBack = { openRuleId = null },
            onRetryRecord = onRetryRecord,
            onRetryFailed = { onRetryFailed(openRule.id) },
            modifier = modifier
        )
        return
    }

    ListenerRulesList(
        smsEnabled = smsEnabled,
        autoRetry = autoRetry,
        rules = rules,
        historyRecords = historyRecords,
        onToggleSmsEnabled = onToggleSmsEnabled,
        onToggleAutoRetry = onToggleAutoRetry,
        onAddRule = {
            editingRule = null
            showRuleDialog = true
        },
        onOpenRule = { openRuleId = it.id },
        onToggleRule = { rule, enabled -> onSaveRule(rule.copy(enabled = enabled)) },
        onEditRule = { rule ->
            editingRule = rule
            showRuleDialog = true
        },
        onDeleteRule = onDeleteRule,
        onTestRule = onTestRule,
        modifier = modifier
    )
}

@Composable
private fun ListenerRulesList(
    smsEnabled: Boolean,
    autoRetry: Boolean,
    rules: List<SmsFilterRule>,
    historyRecords: List<SmsRecord>,
    onToggleSmsEnabled: (Boolean) -> Unit,
    onToggleAutoRetry: (Boolean) -> Unit,
    onAddRule: () -> Unit,
    onOpenRule: (SmsFilterRule) -> Unit,
    onToggleRule: (SmsFilterRule, Boolean) -> Unit,
    onEditRule: (SmsFilterRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onTestRule: (SmsFilterRule) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }

    val filteredRules = remember(rules, query) {
        rules.filter { it.matchesRuleQuery(query) }
    }
    val pageCount = pageCount(filteredRules.size)
    val safePage = page.coerceIn(0, pageCount - 1)
    val visibleRules = filteredRules.drop(safePage * PAGE_SIZE).take(PAGE_SIZE)
    val deliveryCounts = remember(historyRecords) {
        historyRecords.groupingBy { it.ruleId }.eachCount()
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

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ruleListTitle(query, filteredRules.size, rules.size),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                Button(onClick = onAddRule) {
                    Icon(FeatherIcons.Plus, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule")
                }
            }
        }

        item {
            SearchField(
                value = query,
                onValueChange = {
                    query = it
                    page = 0
                },
                placeholder = "Search rules by name, sender, or webhook"
            )
        }

        if (rules.isEmpty()) {
            item {
                EmptyCard(
                    title = "No Listener Rules Configured",
                    body = "Tap 'Add Rule' above to setup custom webhook filters for M-PESA, bank SMS, or SIM slots."
                )
            }
        } else if (filteredRules.isEmpty()) {
            item {
                EmptyCard(
                    title = "No matching rules",
                    body = "Try a different name, sender ID, or webhook."
                )
            }
        } else {
            items(visibleRules, key = { it.id }) { rule ->
                SmsRuleCard(
                    rule = rule,
                    deliveryCount = deliveryCounts[rule.id] ?: 0,
                    onOpen = { onOpenRule(rule) },
                    onToggleEnabled = { enabled -> onToggleRule(rule, enabled) },
                    onEdit = { onEditRule(rule) },
                    onDelete = { onDeleteRule(rule.id) },
                    onTestRule = { onTestRule(rule) }
                )
            }

            item {
                PageControls(
                    page = safePage,
                    pageCount = pageCount,
                    onPrevious = { page = safePage - 1 },
                    onNext = { page = safePage + 1 }
                )
            }
        }
    }
}

@Composable
private fun RuleDeliveryHistory(
    rule: SmsFilterRule,
    historyRecords: List<SmsRecord>,
    onBack: () -> Unit,
    onRetryRecord: (Long) -> Unit,
    onRetryFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember(rule.id) { mutableStateOf("") }
    var page by remember(rule.id) { mutableIntStateOf(0) }
    var selectedTab by remember(rule.id) { mutableIntStateOf(0) }

    val ruleRecords = remember(historyRecords, rule.id) {
        historyRecords.filter { it.ruleId == rule.id }
    }
    val statusFiltered = remember(ruleRecords, selectedTab) {
        when (selectedTab) {
            1 -> ruleRecords.filter { it.status == SmsHistoryStore.STATUS_SUCCESS }
            2 -> ruleRecords.filter { it.status == SmsHistoryStore.STATUS_PENDING }
            3 -> ruleRecords.filter { it.status == SmsHistoryStore.STATUS_FAILED }
            else -> ruleRecords
        }
    }
    val filteredRecords = remember(statusFiltered, query) {
        statusFiltered.filter { it.matchesHistoryQuery(query) }
    }
    val pageCount = pageCount(filteredRecords.size)
    val safePage = page.coerceIn(0, pageCount - 1)
    val visibleRecords = filteredRecords.drop(safePage * PAGE_SIZE).take(PAGE_SIZE)
    val canRetry = ruleRecords.any {
        it.status == SmsHistoryStore.STATUS_FAILED || it.status == SmsHistoryStore.STATUS_PENDING
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to listener rules"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ruleSummary(rule),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = historyListTitle(query, filteredRecords.size, ruleRecords.size),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onRetryFailed,
                    enabled = canRetry
                ) {
                    Icon(FeatherIcons.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry Failed")
                }
            }
        }

        item {
            SearchField(
                value = query,
                onValueChange = {
                    query = it
                    page = 0
                },
                placeholder = "Search sender or message"
            )
        }

        item {
            val tabTitles = listOf("All", "Sent", "Pending", "Failed")
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            page = 0
                        },
                        text = { Text(title) }
                    )
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                Text(
                    text = if (ruleRecords.isEmpty()) {
                        "No messages have been forwarded for this rule yet."
                    } else {
                        "No message history matching this search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(visibleRecords, key = { it.id }) { record ->
                SmsHistoryCard(
                    record = record,
                    onRetry = { onRetryRecord(record.id) }
                )
            }

            item {
                PageControls(
                    page = safePage,
                    pageCount = pageCount,
                    onPrevious = { page = safePage - 1 },
                    onNext = { page = safePage + 1 }
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PageControls(
    page: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    if (pageCount <= 1) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(onClick = onPrevious, enabled = page > 0) {
            Text("Previous")
        }
        Text(
            text = "${page + 1} / $pageCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedButton(onClick = onNext, enabled = page < pageCount - 1) {
            Text("Next")
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun pageCount(total: Int): Int {
    if (total <= 0) return 1
    return (total + PAGE_SIZE - 1) / PAGE_SIZE
}

private fun ruleListTitle(query: String, filtered: Int, total: Int): String {
    return if (query.isBlank()) {
        "Listener Rules ($total)"
    } else {
        "Listener Rules ($filtered of $total)"
    }
}

private fun historyListTitle(query: String, filtered: Int, total: Int): String {
    return if (query.isBlank()) {
        "Delivery History ($total)"
    } else {
        "Delivery History ($filtered of $total)"
    }
}

private fun ruleSummary(rule: SmsFilterRule): String {
    val senders = if (rule.senderFilter.isBlank()) "All senders" else rule.senderFilter
    val sim = when (rule.simFilter) {
        "sim1" -> "SIM 1"
        "sim2" -> "SIM 2"
        else -> "All SIMs"
    }
    return "$senders · $sim"
}

private fun SmsFilterRule.matchesRuleQuery(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return name.contains(needle, ignoreCase = true) ||
        senderFilter.contains(needle, ignoreCase = true) ||
        keywordFilter.contains(needle, ignoreCase = true) ||
        webhookUrl.contains(needle, ignoreCase = true)
}

private fun SmsRecord.matchesHistoryQuery(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return sender.contains(needle, ignoreCase = true) ||
        text.contains(needle, ignoreCase = true) ||
        status.contains(needle, ignoreCase = true) ||
        sim.contains(needle, ignoreCase = true) ||
        (error?.contains(needle, ignoreCase = true) == true)
}
