package ke.payhero.autodial.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.payhero.autodial.DialApiState
import ke.payhero.autodial.ui.components.FeatherIcons
import ke.payhero.autodial.ui.components.StatusBadge
import ke.payhero.autodial.ui.components.StatusType
import java.util.Calendar

@Composable
fun HomeScreen(
    isListening: Boolean,
    hasCallPermission: Boolean,
    smsEnabled: Boolean,
    rulesCount: Int,
    smsTotalCount: Int,
    smsFailedCount: Int,
    grantedPermissionsCount: Int,
    totalPermissionsCount: Int,
    lastDialActivity: String?,
    onNavigateTab: (Int) -> Unit,
    onAddRuleClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Greeting & App Header
        Text(
            text = greetingText(),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Live Status Banner
        HeroStatusCard(
            isListening = isListening,
            lastActivity = lastDialActivity,
            onCardClick = { onNavigateTab(1) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live Metrics Grid
        Text(
            text = "Overview & Quick Stats",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                title = "HTTP Listener",
                value = if (isListening) "Port ${DialApiState.PORT}" else "Stopped",
                subtitle = "Local Dial API",
                icon = FeatherIcons.Globe,
                onClick = { onNavigateTab(1) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "SMS Forwarder",
                value = if (!smsEnabled) "Disabled" else if (smsFailedCount > 0) "$smsFailedCount Failed" else "$rulesCount Rules",
                subtitle = if (smsTotalCount == 1) "1 message" else "$smsTotalCount messages",
                icon = FeatherIcons.MessageSquare,
                onClick = { onNavigateTab(2) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                title = "Call Access",
                value = if (hasCallPermission) "Granted" else "Action Needed",
                subtitle = if (hasCallPermission) "Auto dialer ready" else "Tap to grant",
                icon = FeatherIcons.Phone,
                onClick = { onNavigateTab(3) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Permissions",
                value = "$grantedPermissionsCount / $totalPermissionsCount",
                subtitle = if (grantedPermissionsCount == totalPermissionsCount) "All granted" else "Missing required",
                icon = FeatherIcons.Shield,
                onClick = { onNavigateTab(3) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Section
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onAddRuleClicked,
                modifier = Modifier.weight(1f)
            ) {
                Icon(FeatherIcons.Plus, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add SMS Filter")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Activity Row Shortcuts
        NavigationShortcutRow(
            title = "Dial API Endpoints",
            subtitle = "Test endpoints and view local IP addresses",
            icon = FeatherIcons.Globe,
            onClick = { onNavigateTab(1) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        NavigationShortcutRow(
            title = "SMS Listener Rules & Logs",
            subtitle = "Configure webhooks and inspect message forwarding",
            icon = FeatherIcons.MessageSquare,
            onClick = { onNavigateTab(2) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        NavigationShortcutRow(
            title = "Permissions & App Settings",
            subtitle = "Manage background access and phone permissions",
            icon = FeatherIcons.Shield,
            onClick = { onNavigateTab(3) }
        )
    }
}

@Composable
private fun HeroStatusCard(
    isListening: Boolean,
    lastActivity: String?,
    onCardClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            .alpha(if (isListening) alphaAnim else 1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "HTTP LISTENER LIVE" else "LISTENER STOPPED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                StatusBadge(
                    text = if (isListening) "Port ${DialApiState.PORT}" else "Offline",
                    type = if (isListening) StatusType.SUCCESS else StatusType.ERROR
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isListening) "Local Dial Service Running" else "Listener Waking Up",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = lastActivity ?: "Waiting for incoming dial request or SMS message…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationShortcutRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = FeatherIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
