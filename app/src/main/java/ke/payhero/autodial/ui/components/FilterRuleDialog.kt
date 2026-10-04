package ke.payhero.autodial.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ke.payhero.autodial.SmsFilterRule
import ke.payhero.autodial.SmsForwardConfig

@Composable
fun FilterRuleDialog(
    initialRule: SmsFilterRule?,
    onDismiss: () -> Unit,
    onSave: (SmsFilterRule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "New Listener Rule") }
    var enabled by remember { mutableStateOf(initialRule?.enabled ?: true) }
    var senderFilter by remember { mutableStateOf(initialRule?.senderFilter ?: "") }
    var simFilter by remember { mutableStateOf(initialRule?.simFilter ?: SmsForwardConfig.SIM_ALL) }
    var keywordFilter by remember { mutableStateOf(initialRule?.keywordFilter ?: "") }
    var webhookUrl by remember { mutableStateOf(initialRule?.webhookUrl ?: "") }
    var headersJson by remember { mutableStateOf(initialRule?.headersJson ?: "{}") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialRule == null) "Add Listener Rule" else "Edit Listener Rule",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name & Enabled
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enable Rule",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Senders
                OutlinedTextField(
                    value = senderFilter,
                    onValueChange = { senderFilter = it },
                    label = { Text("Sender ID") },
                    placeholder = { Text("airtelmoney, MPESA") },
                    supportingText = {
                        Text("Case-sensitive. airtelmoney does not match AirtelMoney. Separate senders with commas, or use * for every sender.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // SIM Slot Filter
                Text(
                    text = "SIM Filter",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    FilterChip(
                        selected = simFilter == SmsForwardConfig.SIM_ALL,
                        onClick = { simFilter = SmsForwardConfig.SIM_ALL },
                        label = { Text("All SIMs") }
                    )
                    FilterChip(
                        selected = simFilter == SmsForwardConfig.SIM_1,
                        onClick = { simFilter = SmsForwardConfig.SIM_1 },
                        label = { Text("SIM 1") }
                    )
                    FilterChip(
                        selected = simFilter == SmsForwardConfig.SIM_2,
                        onClick = { simFilter = SmsForwardConfig.SIM_2 },
                        label = { Text("SIM 2") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Keyword Filter
                OutlinedTextField(
                    value = keywordFilter,
                    onValueChange = { keywordFilter = it },
                    label = { Text("Message Keyword Filter (Optional)") },
                    placeholder = { Text("e.g. Confirmed or Ksh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Webhook URL
                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Target Webhook URL") },
                    placeholder = { Text("https://your-server.com/api/sms-webhook") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Headers Editor
                HeaderEditor(
                    headersJson = headersJson,
                    onHeadersChanged = { headersJson = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val ruleToSave = (initialRule ?: SmsFilterRule()).copy(
                                name = name.trim().ifBlank { "Listener Rule" },
                                enabled = enabled,
                                senderFilter = senderFilter.trim(),
                                simFilter = simFilter,
                                keywordFilter = keywordFilter.trim(),
                                webhookUrl = webhookUrl.trim(),
                                headersJson = headersJson
                            )
                            onSave(ruleToSave)
                        },
                        enabled = webhookUrl.isNotBlank()
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    }
}
