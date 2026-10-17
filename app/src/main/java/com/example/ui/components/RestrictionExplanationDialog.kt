package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import com.example.ui.theme.NovaCyan

@Composable
fun RestrictionExplanationDialog(
    title: String,
    explanation: String,
    primaryButtonText: String = "Enable Permission",
    onPrimaryAction: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = NovaCyan
            )
        },
        title = { Text(text = title) },
        text = {
            Text(
                text = explanation,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            if (onPrimaryAction != null) {
                Button(
                    onClick = {
                        onPrimaryAction()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaCyan),
                    modifier = Modifier.testTag("restriction_primary_btn")
                ) {
                    Text(primaryButtonText, color = MaterialTheme.colorScheme.surface)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NovaCyan)
                ) {
                    Text("Understood", color = MaterialTheme.colorScheme.surface)
                }
            }
        },
        dismissButton = {
            if (onPrimaryAction != null) {
                TextButton(onClick = onDismiss) {
                    Text("Not Now")
                }
            }
        }
    )
}
