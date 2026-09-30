package ke.payhero.autodial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ke.payhero.autodial.ui.components.FeatherIcons
import ke.payhero.autodial.ui.components.StatusBadge
import ke.payhero.autodial.ui.components.StatusType

data class PermissionItemState(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val onRequest: () -> Unit
)

@Composable
fun PermissionsScreen(
    hasCallPhone: Boolean,
    hasNotifications: Boolean,
    hasSms: Boolean,
    canStartOnReboot: Boolean,
    onRequestCallPhone: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestSms: () -> Unit,
    onRequestUnrestrictedBattery: () -> Unit,
    onGrantRemainingClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        PermissionItemState(
            id = "phone",
            title = "Phone Calls",
            subtitle = "Place cellular and USSD calls automatically",
            icon = FeatherIcons.Phone,
            isGranted = hasCallPhone,
            onRequest = onRequestCallPhone
        ),
        PermissionItemState(
            id = "notifications",
            title = "Notifications",
            subtitle = "Keeps HTTP listener service visible in foreground",
            icon = FeatherIcons.Bell,
            isGranted = hasNotifications,
            onRequest = onRequestNotifications
        ),
        PermissionItemState(
            id = "sms",
            title = "SMS Access",
            subtitle = "Read incoming messages and identify SIM 1 or SIM 2 slot",
            icon = FeatherIcons.MessageSquare,
            isGranted = hasSms,
            onRequest = onRequestSms
        ),
        PermissionItemState(
            id = "battery",
            title = "Start On Reboot / Unrestricted Battery",
            subtitle = "Wakes the HTTP listener after phone powers on",
            icon = FeatherIcons.Zap,
            isGranted = canStartOnReboot,
            onRequest = onRequestUnrestrictedBattery
        )
    )

    val grantedCount = items.count { it.isGranted }
    val totalCount = items.size
    val progress = grantedCount.toFloat() / totalCount.toFloat()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Permissions & Security",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Card
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
                    Text(
                        text = "Access Summary",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    StatusBadge(
                        text = "$grantedCount of $totalCount Enabled",
                        type = if (grantedCount == totalCount) StatusType.SUCCESS else StatusType.ERROR
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (grantedCount < totalCount) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGrantRemainingClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Remaining Permissions")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Permission Checklist",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        items.forEach { item ->
            PermissionCard(item = item)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItemState) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isGranted, onClick = item.onRequest)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(
                text = if (item.isGranted) "Granted" else "Grant",
                type = if (item.isGranted) StatusType.SUCCESS else StatusType.ERROR
            )

            if (!item.isGranted) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = FeatherIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
