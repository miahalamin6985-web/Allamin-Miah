package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AssistantState
import com.example.ui.NovaViewModel
import com.example.ui.components.ChatFeed
import com.example.ui.components.GlowingOrb
import com.example.ui.components.PermissionItem
import com.example.ui.components.PermissionsDashboard
import com.example.ui.components.QuickActionChips
import com.example.ui.components.RestrictionExplanationDialog
import com.example.ui.components.SensitiveActionDialog
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.StatusIndicator
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaRose
import com.example.ui.theme.NovaViolet

class MainActivity : ComponentActivity() {

    private val viewModel: NovaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                NovaAssistantApp(
                    viewModel = viewModel,
                    onRequestAdminIntent = {
                        val intent = viewModel.actionEngine.deviceAdminHelper.getActivateAdminIntent()
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDeviceAdminStatus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaAssistantApp(
    viewModel: NovaViewModel,
    onRequestAdminIntent: () -> Unit
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val audioAmplitude by viewModel.audioAmplitude.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val partialTranscript by viewModel.partialTranscript.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val restrictionDialog by viewModel.restrictionDialog.collectAsState()
    val isDeviceAdminActive by viewModel.isDeviceAdminActive.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Permission launchers
    var hasMicPermission by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasContactsPermission by remember { mutableStateOf(false) }
    var hasCallPermission by remember { mutableStateOf(false) }
    var hasNotifPermission by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleListening()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasContactsPermission = granted }

    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCallPermission = granted }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifPermission = granted }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMicPermission = results[Manifest.permission.RECORD_AUDIO] ?: hasMicPermission
        hasCameraPermission = results[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasContactsPermission = results[Manifest.permission.READ_CONTACTS] ?: hasContactsPermission
        hasCallPermission = results[Manifest.permission.CALL_PHONE] ?: hasCallPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifPermission = results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotifPermission
        }
    }

    LaunchedEffect(Unit) {
        val initialPerms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            initialPerms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(initialPerms.toTypedArray())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NovaCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = settings.assistantName.uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NovaCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NovaCyan
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPermissionsSheet = true },
                        modifier = Modifier.testTag("privacy_dashboard_btn")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Security,
                            contentDescription = "Privacy & Permissions",
                            tint = NovaCyan
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color(0xFF0D1424),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chat history stream (taking flexible middle space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && partialTranscript.isBlank()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tap the orb or mic to begin",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    ChatFeed(
                        messages = messages,
                        partialTranscript = partialTranscript,
                        assistantName = settings.assistantName,
                        onReplaySpeech = { text -> viewModel.replaySpeech(text) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Quick action chips
            QuickActionChips(
                onActionClick = { cmd -> viewModel.processUserQuery(cmd) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Waveform Audio Frequency visualizer
            WaveformVisualizer(
                isListening = assistantState == AssistantState.LISTENING,
                isSpeaking = assistantState == AssistantState.SPEAKING,
                amplitude = audioAmplitude
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Glowing Orb Hero Unit
            GlowingOrb(
                state = assistantState,
                audioAmplitude = audioAmplitude,
                isSpeaking = assistantState == AssistantState.SPEAKING,
                size = 170.dp,
                onClick = {
                    if (assistantState == AssistantState.SPEAKING) {
                        viewModel.stopSpeaking()
                    } else {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.testTag("glowing_orb_canvas")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Status Indicator Chip
            StatusIndicator(
                state = assistantState,
                assistantName = settings.assistantName
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Input Controls: Mic FAB and text fallback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "Type or ask in English, বাংলা, हिन्दी...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.processUserQuery(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        }
                    ),
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.processUserQuery(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.testTag("send_query_btn")
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = NovaCyan
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("query_text_input")
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Microphone Activation FAB
                FloatingActionButton(
                    onClick = {
                        if (assistantState == AssistantState.SPEAKING) {
                            viewModel.stopSpeaking()
                        } else {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    containerColor = when (assistantState) {
                        AssistantState.LISTENING -> NovaCyan
                        AssistantState.SPEAKING -> NovaMagenta
                        else -> NovaViolet
                    },
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("voice_mic_fab")
                ) {
                    Icon(
                        imageVector = when (assistantState) {
                            AssistantState.SPEAKING -> Icons.Default.Stop
                            AssistantState.LISTENING -> Icons.Default.Mic
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Microphone Trigger",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            settings = settings,
            isDeviceAdminActive = isDeviceAdminActive,
            onSaveSettings = { updated -> viewModel.updateSettings(updated) },
            onTestVoice = { p, s -> viewModel.testVoice(p, s) },
            onRequestDeviceAdmin = onRequestAdminIntent,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Permissions Dashboard Bottom Sheet
    if (showPermissionsSheet) {
        val permSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPermissionsSheet = false },
            sheetState = permSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val permsList = listOf(
                PermissionItem(
                    title = "Microphone",
                    purpose = "Required for speech recognition and hands-free wake word detection.",
                    isGranted = hasMicPermission,
                    icon = Icons.Default.Mic,
                    onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                ),
                PermissionItem(
                    title = "Camera & Flashlight",
                    purpose = "Used to turn the device flashlight on and off with voice commands.",
                    isGranted = hasCameraPermission,
                    icon = androidx.compose.material.icons.Icons.Default.Security,
                    onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                ),
                PermissionItem(
                    title = "Contacts",
                    purpose = "Used to search contact names when saying 'Call Mom' or 'Message John'.",
                    isGranted = hasContactsPermission,
                    icon = androidx.compose.material.icons.Icons.Default.Security,
                    onRequest = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                ),
                PermissionItem(
                    title = "Direct Calling",
                    purpose = "Allows Nova to initiate confirmed phone calls after user authorization.",
                    isGranted = hasCallPermission,
                    icon = androidx.compose.material.icons.Icons.Default.Security,
                    onRequest = { callLauncher.launch(Manifest.permission.CALL_PHONE) }
                ),
                PermissionItem(
                    title = "Device Administrator",
                    purpose = "Enables secure screen locking when you command 'Nova, lock my phone'.",
                    isGranted = isDeviceAdminActive,
                    icon = androidx.compose.material.icons.Icons.Default.Security,
                    onRequest = onRequestAdminIntent
                )
            )

            PermissionsDashboard(
                permissions = permsList,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }

    // Sensitive Action Confirmation Dialog (Calls / SMS)
    pendingConfirmation?.let { conf ->
        SensitiveActionDialog(
            confirmation = conf,
            onDismiss = { viewModel.dismissConfirmation() }
        )
    }

    // Restriction Explanation Dialog
    restrictionDialog?.let { dialog ->
        RestrictionExplanationDialog(
            title = dialog.title,
            explanation = dialog.explanation,
            primaryButtonText = dialog.primaryButtonText,
            onPrimaryAction = dialog.onPrimaryAction,
            onDismiss = { viewModel.dismissRestrictionDialog() }
        )
    }
}
