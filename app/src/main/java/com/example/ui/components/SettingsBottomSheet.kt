package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AssistantSettings
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: AssistantSettings,
    isDeviceAdminActive: Boolean,
    onSaveSettings: (AssistantSettings) -> Unit,
    onTestVoice: (pitch: Float, speed: Float) -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nameInput by remember { mutableStateOf(settings.assistantName) }
    var pitch by remember { mutableFloatStateOf(settings.voicePitch) }
    var speed by remember { mutableFloatStateOf(settings.voiceSpeed) }
    var language by remember { mutableStateOf(settings.selectedLanguage) }
    var autoSpeak by remember { mutableStateOf(settings.autoSpeakResponse) }
    var continuousWake by remember { mutableStateOf(settings.continuousWakeMode) }
    var showAndroidPolicyInfo by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .testTag("settings_bottom_sheet_column")
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NovaViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = NovaViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Nova Assistant Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize voice, wake trigger & language",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Assistant Name
            Column {
                Text(
                    text = "Assistant Wake Name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Name used in wake-word detection (e.g. \"Hey $nameInput\")",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(20) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assistant_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 2. Language Selection
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = NovaCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Preferred Language",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languages = listOf(
                        "auto" to "Auto Detect",
                        "en" to "English",
                        "bn" to "বাংলা (Bengali)",
                        "hi" to "हिन्दी (Hindi)"
                    )
                    languages.forEach { (code, label) ->
                        FilterChip(
                            selected = language == code,
                            onClick = { language = code },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("lang_chip_$code")
                        )
                    }
                }
            }

            // 3. Voice Customization (Pitch & Speed)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = NovaViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Voice Pitch: ${String.format("%.2f", pitch)}x",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = NovaCyan,
                        activeTrackColor = NovaCyan
                    ),
                    modifier = Modifier.testTag("voice_pitch_slider")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NovaViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Speech Rate: ${String.format("%.2f", speed)}x",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.6f..1.6f,
                    colors = SliderDefaults.colors(
                        thumbColor = NovaViolet,
                        activeTrackColor = NovaViolet
                    ),
                    modifier = Modifier.testTag("voice_speed_slider")
                )

                Button(
                    onClick = { onTestVoice(pitch, speed) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.testTag("test_voice_button")
                ) {
                    Text("Test Voice Sample", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // 4. Auto Speak & Background Wake
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Speak Responses",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Read aloud assistant answers automatically",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { autoSpeak = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan),
                        modifier = Modifier.testTag("auto_speak_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continuous Wake Listening Service",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Runs a foreground notification service so you can trigger Nova hands-free",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = continuousWake,
                        onCheckedChange = { continuousWake = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaEmerald),
                        modifier = Modifier.testTag("continuous_wake_switch")
                    )
                }
            }

            // 5. Device Administrator Screen Lock Control
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isDeviceAdminActive) NovaEmerald else NovaCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Screen Lock Permission",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDeviceAdminActive) "Active (Device Administrator)" else "Required for 'Lock my phone'",
                                fontSize = 11.sp,
                                color = if (isDeviceAdminActive) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isDeviceAdminActive) {
                        Button(
                            onClick = onRequestDeviceAdmin,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NovaCyan),
                            modifier = Modifier.testTag("activate_admin_button")
                        ) {
                            Text("Activate", fontSize = 12.sp, color = MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }

            // 6. Educational: Android Platform Restrictions Note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable { showAndroidPolicyInfo = !showAndroidPolicyInfo }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NovaCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Android OS Policy & Restrictions Info",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovaCyan
                    )
                }
                if (showAndroidPolicyInfo) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Screen Locking: Android restricts third-party apps from powering off the device or locking without user-approved Device Administrator privileges.\n" +
                                "• Screen Waking: On Android 10+, background activity starts are blocked. Nova uses high-priority heads-up wake notifications and wake locks to illuminate the screen.\n" +
                                "• Sensitive Actions: Phone calls and SMS require user confirmation dialogs to prevent accidental dialing.\n" +
                                "• Background Listening: Requires a visible foreground notification to comply with Android privacy and battery standards.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    val updated = settings.copy(
                        assistantName = if (nameInput.isNotBlank()) nameInput.trim() else "Nova",
                        voicePitch = pitch,
                        voiceSpeed = speed,
                        selectedLanguage = language,
                        autoSpeakResponse = autoSpeak,
                        continuousWakeMode = continuousWake
                    )
                    onSaveSettings(updated)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaCyan)
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surface)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
