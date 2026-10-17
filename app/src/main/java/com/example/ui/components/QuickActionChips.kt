package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaCyan

data class QuickAction(
    val title: String,
    val command: String,
    val icon: ImageVector
)

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickAction("Open YouTube", "open youtube", Icons.Default.PlayCircle),
        QuickAction("Flashlight", "turn on flashlight", Icons.Default.FlashlightOn),
        QuickAction("Timer 10m", "set a timer for 10 minutes", Icons.Default.Timer),
        QuickAction("Volume Up", "increase the volume", androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp),
        QuickAction("Lock Phone", "lock my phone", Icons.Default.Lock),
        QuickAction("Open Settings", "open settings", Icons.Default.Settings),
        QuickAction("Alarm 6 AM", "set an alarm for 6 AM", Icons.Default.Alarm),
        QuickAction("কেমন আছো?", "কেমন আছো?", Icons.Default.WavingHand),
        QuickAction("Flashlight On Koro", "flashlight on koro", Icons.Default.FlashlightOn)
    )

    Row(
        modifier = modifier
            .testTag("quick_action_chips_row")
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            AssistChip(
                onClick = { onActionClick(action.command) },
                label = { Text(action.title, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = NovaCyan,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("chip_${action.title.replace(" ", "_")}")
            )
        }
    }
}
