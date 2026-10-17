package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.PendingConfirmation
import com.example.ui.theme.NovaCyan

@Composable
fun SensitiveActionDialog(
    confirmation: PendingConfirmation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = NovaCyan
            )
        },
        title = {
            Text(text = confirmation.title)
        },
        text = {
            Text(
                text = confirmation.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    confirmation.onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NovaCyan),
                modifier = Modifier.testTag("confirm_sensitive_action_btn")
            ) {
                Text("Confirm & Execute", color = MaterialTheme.colorScheme.surface)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_sensitive_action_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
