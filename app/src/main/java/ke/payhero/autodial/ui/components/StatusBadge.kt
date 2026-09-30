package ke.payhero.autodial.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.payhero.autodial.ui.theme.StatusError
import ke.payhero.autodial.ui.theme.StatusSuccess
import ke.payhero.autodial.ui.theme.StatusWarning

@Composable
fun StatusBadge(
    text: String,
    type: StatusType,
    modifier: Modifier = Modifier
) {
    val bgColor: Color
    val textColor: Color
    val dotColor: Color

    when (type) {
        StatusType.SUCCESS -> {
            bgColor = StatusSuccess.copy(alpha = 0.15f)
            textColor = StatusSuccess
            dotColor = StatusSuccess
        }
        StatusType.WARNING -> {
            bgColor = StatusWarning.copy(alpha = 0.15f)
            textColor = StatusWarning
            dotColor = StatusWarning
        }
        StatusType.ERROR -> {
            bgColor = StatusError.copy(alpha = 0.15f)
            textColor = StatusError
            dotColor = StatusError
        }
        StatusType.NEUTRAL -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
            dotColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Spacer(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

enum class StatusType {
    SUCCESS, WARNING, ERROR, NEUTRAL
}
