package com.example.engine

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import com.example.data.ActionType
import com.example.data.PendingConfirmation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ExecutionResult(
    val spokenResponse: String,
    val actionType: ActionType,
    val details: String? = null,
    val pendingConfirmation: PendingConfirmation? = null,
    val triggerAdminSetup: Boolean = false
)

class ActionEngine(
    private val context: Context,
    val torchController: TorchController = TorchController(context),
    val volumeController: VolumeController = VolumeController(context),
    val appLauncher: AppLauncher = AppLauncher(context),
    val contactsHelper: ContactsHelper = ContactsHelper(context),
    val deviceAdminHelper: DeviceAdminHelper = DeviceAdminHelper(context),
    val screenWakeHelper: ScreenWakeHelper = ScreenWakeHelper(context)
) {

    /**
     * Determines whether the user command is an Android phone-control action.
     * Returns ExecutionResult if handled, or null if it should be delegated to Gemini AI brain.
     */
    fun processCommand(
        rawCommand: String,
        assistantName: String = "Nova",
        onExecuteDirectCall: ((phoneNumber: String) -> Unit)? = null,
        onShowRestrictionDialog: ((title: String, explanation: String) -> Unit)? = null
    ): ExecutionResult? {
        // Strip wake-words like "Hey Nova", "Nova", "নোভা", "हे नोवा"
        val clean = stripWakeWord(rawCommand, assistantName).trim().lowercase()

        // 1. Flashlight Controls
        if (isFlashlightOn(clean)) {
            val res = torchController.setTorch(true)
            return if (res.isSuccess) {
                ExecutionResult(
                    spokenResponse = "Flashlight turned on.",
                    actionType = ActionType.FLASHLIGHT_ON,
                    details = "Camera Flashlight ON"
                )
            } else {
                ExecutionResult(
                    spokenResponse = "Could not turn on flashlight: ${res.exceptionOrNull()?.message ?: "Camera unavailable"}",
                    actionType = ActionType.FLASHLIGHT_ON,
                    details = "Failed to toggle flashlight"
                )
            }
        }
        if (isFlashlightOff(clean)) {
            val res = torchController.setTorch(false)
            return if (res.isSuccess) {
                ExecutionResult(
                    spokenResponse = "Flashlight turned off.",
                    actionType = ActionType.FLASHLIGHT_OFF,
                    details = "Camera Flashlight OFF"
                )
            } else {
                ExecutionResult(
                    spokenResponse = "Could not turn off flashlight.",
                    actionType = ActionType.FLASHLIGHT_OFF
                )
            }
        }

        // 2. Volume Controls
        if (isVolumeUp(clean)) {
            val msg = volumeController.increaseVolume()
            return ExecutionResult(
                spokenResponse = "Volume increased.",
                actionType = ActionType.VOLUME_UP,
                details = msg
            )
        }
        if (isVolumeDown(clean)) {
            val msg = volumeController.decreaseVolume()
            return ExecutionResult(
                spokenResponse = "Volume decreased.",
                actionType = ActionType.VOLUME_DOWN,
                details = msg
            )
        }
        if (isVolumeMute(clean)) {
            val msg = volumeController.muteVolume()
            return ExecutionResult(
                spokenResponse = "Media volume muted.",
                actionType = ActionType.VOLUME_MUTE,
                details = msg
            )
        }

        // 3. Screen Lock / Screen Off
        if (isScreenLock(clean)) {
            val isAdmin = deviceAdminHelper.isDeviceAdminActive()
            return if (isAdmin) {
                deviceAdminHelper.lockScreen()
                ExecutionResult(
                    spokenResponse = "Locking your phone.",
                    actionType = ActionType.LOCK_SCREEN,
                    details = "Screen locked via Device Admin"
                )
            } else {
                ExecutionResult(
                    spokenResponse = "To lock the screen, Android requires Device Administrator permission. Tap below to activate it.",
                    actionType = ActionType.RESTRICTION_EXPLANATION,
                    details = "Device Administrator permission required for screen lock",
                    triggerAdminSetup = true
                )
            }
        }

        // 4. Screen On / Wake up
        if (isScreenOn(clean)) {
            val wakeRes = screenWakeHelper.requestScreenWake()
            return when (wakeRes) {
                is ScreenWakeResult.AlreadyOn -> ExecutionResult(
                    spokenResponse = "Your screen is already turned on.",
                    actionType = ActionType.SCREEN_ON,
                    details = wakeRes.message
                )
                is ScreenWakeResult.Success -> ExecutionResult(
                    spokenResponse = "Turning on screen display.",
                    actionType = ActionType.SCREEN_ON,
                    details = wakeRes.message
                )
                is ScreenWakeResult.Restricted -> ExecutionResult(
                    spokenResponse = "Display wake notification sent. Note that Android restricts background unlocking without user touch.",
                    actionType = ActionType.RESTRICTION_EXPLANATION,
                    details = wakeRes.explanation
                )
            }
        }

        // 5. Open Apps (YouTube, WhatsApp, Settings, Camera, Maps, etc.)
        val openAppMatch = extractAppToOpen(clean)
        if (openAppMatch != null) {
            val launchRes = appLauncher.launchAppByName(openAppMatch)
            return when (launchRes) {
                is AppLaunchResult.Success -> ExecutionResult(
                    spokenResponse = launchRes.message,
                    actionType = ActionType.OPEN_APP,
                    details = launchRes.message
                )
                is AppLaunchResult.NotFound -> ExecutionResult(
                    spokenResponse = "I couldn't find ${launchRes.queryName} installed on your device.",
                    actionType = ActionType.OPEN_APP,
                    details = "App not found: ${launchRes.queryName}"
                )
                is AppLaunchResult.Failure -> ExecutionResult(
                    spokenResponse = "Failed to open $openAppMatch.",
                    actionType = ActionType.OPEN_APP,
                    details = launchRes.error
                )
            }
        }

        // 6. Alarms & Timers
        val timerSeconds = extractTimerSeconds(clean)
        if (timerSeconds != null) {
            val ok = setTimer(timerSeconds)
            val minutes = timerSeconds / 60
            val sec = timerSeconds % 60
            val timeDesc = if (minutes > 0) "$minutes minute" + if (minutes > 1) "s" else "" else "$sec seconds"
            return if (ok) {
                ExecutionResult(
                    spokenResponse = "Setting timer for $timeDesc.",
                    actionType = ActionType.SET_TIMER,
                    details = "Timer: $timeDesc"
                )
            } else {
                ExecutionResult(
                    spokenResponse = "Opening clock to set timer.",
                    actionType = ActionType.SET_TIMER,
                    details = "Clock app launched"
                )
            }
        }

        val alarmTime = extractAlarmTime(clean)
        if (alarmTime != null) {
            val (hour, minute) = alarmTime
            val ok = setAlarm(hour, minute, "Nova Alarm")
            val formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            return if (ok) {
                ExecutionResult(
                    spokenResponse = "Alarm set for $formatted.",
                    actionType = ActionType.SET_ALARM,
                    details = "Alarm set for $formatted"
                )
            } else {
                ExecutionResult(
                    spokenResponse = "Opening alarm settings for $formatted.",
                    actionType = ActionType.SET_ALARM,
                    details = "Launched system alarm screen"
                )
            }
        }

        // 7. Do Not Disturb / Silent
        if (isDoNotDisturb(clean)) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notifManager.isNotificationPolicyAccessGranted) {
                // Direct user to policy settings as required by Android M+
                openDndSettings()
                ExecutionResult(
                    spokenResponse = "Opening Do Not Disturb access settings.",
                    actionType = ActionType.DO_NOT_DISTURB,
                    details = "Notification Policy permission required"
                )
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                ExecutionResult(
                    spokenResponse = "Do Not Disturb turned on.",
                    actionType = ActionType.DO_NOT_DISTURB,
                    details = "Ringer mode set to silent"
                )
            }
        }

        // 8. Battery Saver
        if (isBatterySaver(clean)) {
            openBatterySaverSettings()
            return ExecutionResult(
                spokenResponse = "Opening Battery Saver settings.",
                actionType = ActionType.BATTERY_SAVER,
                details = "Redirected to system Battery Saver screen"
            )
        }

        // 9. Phone Calls & Dialing
        if (isDialNumber(clean)) {
            val digits = extractPhoneNumber(clean)
            if (digits.isNotBlank()) {
                dialNumber(digits)
                return ExecutionResult(
                    spokenResponse = "Dialing $digits.",
                    actionType = ActionType.PHONE_CALL,
                    details = "Dialer opened for $digits"
                )
            }
        }

        val callTarget = extractCallTarget(clean)
        if (callTarget != null) {
            // Find contact or check if digits
            if (callTarget.all { it.isDigit() || it == '+' }) {
                return ExecutionResult(
                    spokenResponse = "Would you like to call $callTarget?",
                    actionType = ActionType.PHONE_CALL,
                    details = "Call $callTarget",
                    pendingConfirmation = PendingConfirmation(
                        title = "Confirm Phone Call",
                        message = "Call $callTarget now?",
                        actionType = ActionType.PHONE_CALL,
                        onConfirm = {
                            if (onExecuteDirectCall != null) {
                                onExecuteDirectCall(callTarget)
                            } else {
                                dialNumber(callTarget)
                            }
                        }
                    )
                )
            }

            // Look up contact
            val contacts = contactsHelper.findContactByName(callTarget)
            return if (contacts.isNotEmpty()) {
                val matched = contacts.first()
                ExecutionResult(
                    spokenResponse = "Calling ${matched.name}.",
                    actionType = ActionType.PHONE_CALL,
                    details = "Contact: ${matched.name} (${matched.phoneNumber})",
                    pendingConfirmation = PendingConfirmation(
                        title = "Call Contact",
                        message = "Do you want to call ${matched.name} (${matched.phoneNumber})?",
                        actionType = ActionType.PHONE_CALL,
                        onConfirm = {
                            if (onExecuteDirectCall != null) {
                                onExecuteDirectCall(matched.phoneNumber)
                            } else {
                                dialNumber(matched.phoneNumber)
                            }
                        }
                    )
                )
            } else {
                // If contact permission not granted or contact not found, open dialer with query
                dialNumber("")
                ExecutionResult(
                    spokenResponse = "I couldn't find $callTarget in your contacts. Opening phone dialer.",
                    actionType = ActionType.PHONE_CALL,
                    details = "Opened dialer (contact not found or permission needed)"
                )
            }
        }

        // 10. Messaging
        val messageTarget = extractMessageTarget(clean)
        if (messageTarget != null) {
            val (recipient, messageBody) = messageTarget
            return ExecutionResult(
                spokenResponse = "Opening message for $recipient.",
                actionType = ActionType.SEND_SMS,
                details = "Recipient: $recipient | Message: $messageBody",
                pendingConfirmation = PendingConfirmation(
                    title = "Send Message",
                    message = "Send to $recipient: \"$messageBody\"?",
                    actionType = ActionType.SEND_SMS,
                    onConfirm = {
                        sendSmsIntent(recipient, messageBody)
                    }
                )
            )
        }

        // 11. Time & Date (Instant local answer)
        if (isTimeQuery(clean)) {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            return ExecutionResult(
                spokenResponse = "It's $currentTime.",
                actionType = ActionType.GENERAL_AI,
                details = "Current Time: $currentTime"
            )
        }
        if (isDateQuery(clean)) {
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            return ExecutionResult(
                spokenResponse = "Today is $currentDate.",
                actionType = ActionType.GENERAL_AI,
                details = "Current Date: $currentDate"
            )
        }

        // 12. Web Search Command
        val searchQuery = extractSearchQuery(clean)
        if (searchQuery != null) {
            performWebSearch(searchQuery)
            return ExecutionResult(
                spokenResponse = "Searching the web for $searchQuery.",
                actionType = ActionType.WEB_SEARCH,
                details = "Web search query: $searchQuery"
            )
        }

        // Not a direct hardware/OS action -> delegate to Gemini AI brain
        return null
    }

    private fun stripWakeWord(text: String, assistantName: String): String {
        val pattern = Pattern.compile("^(hey\\s+$assistantName|$assistantName|হে\\s+নোভা|নোভা|हे\\s+नोवा|नोवा|ahey\\s+$assistantName)[,\\s]*", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text.trim())
        return if (matcher.find()) {
            text.substring(matcher.end())
        } else {
            text
        }
    }

    // --- Intent Matchers ---

    private fun isFlashlightOn(cmd: String): Boolean {
        return cmd.contains("flashlight on") ||
                cmd.contains("turn on flashlight") ||
                cmd.contains("turn on the flashlight") ||
                cmd.contains("torch on") ||
                cmd.contains("turn on torch") ||
                cmd.contains("ফ্ল্যাশলাইট জ্বালাও") ||
                cmd.contains("টর্চ জ্বালাও") ||
                cmd.contains("flashlight on koro") ||
                cmd.contains("torch on koro") ||
                cmd.contains("फ्लैशलाइट जलाओ") ||
                cmd.contains("टॉर्च जलाओ")
    }

    private fun isFlashlightOff(cmd: String): Boolean {
        return cmd.contains("flashlight off") ||
                cmd.contains("turn off flashlight") ||
                cmd.contains("turn off the flashlight") ||
                cmd.contains("torch off") ||
                cmd.contains("turn off torch") ||
                cmd.contains("ফ্ল্যাশলাইট বন্ধ করো") ||
                cmd.contains("টর্চ বন্ধ করো") ||
                cmd.contains("flashlight off koro") ||
                cmd.contains("torch off koro") ||
                cmd.contains("फ्लैशलाइट बंद करो") ||
                cmd.contains("टॉर्च बंद करो")
    }

    private fun isVolumeUp(cmd: String): Boolean {
        return cmd.contains("increase volume") ||
                cmd.contains("volume up") ||
                cmd.contains("raise volume") ||
                cmd.contains("ভলিউম বাড়াও") ||
                cmd.contains("volume barao") ||
                cmd.contains("वॉल्यूम बढ़ाओ")
    }

    private fun isVolumeDown(cmd: String): Boolean {
        return cmd.contains("decrease volume") ||
                cmd.contains("volume down") ||
                cmd.contains("lower volume") ||
                cmd.contains("ভলিউম কমাও") ||
                cmd.contains("volume komao") ||
                cmd.contains("वॉल्यूम कम करो")
    }

    private fun isVolumeMute(cmd: String): Boolean {
        return cmd.contains("mute volume") ||
                cmd.contains("mute") ||
                cmd.contains("silent volume") ||
                cmd.contains("ভলিউম মিউট করো") ||
                cmd.contains("वॉल्यूम म्यूट करो")
    }

    private fun isScreenLock(cmd: String): Boolean {
        return cmd.contains("lock my phone") ||
                cmd.contains("lock the phone") ||
                cmd.contains("lock screen") ||
                cmd.contains("turn off my screen") ||
                cmd.contains("turn off the screen") ||
                cmd.contains("screen off") ||
                cmd.contains("ফোন লক করো") ||
                cmd.contains("স্ক্রিন বন্ধ করো") ||
                cmd.contains("phone lock koro") ||
                cmd.contains("स्क्रीन लॉक करो") ||
                cmd.contains("फोन लॉक करो")
    }

    private fun isScreenOn(cmd: String): Boolean {
        return cmd.contains("turn on my screen") ||
                cmd.contains("turn on the screen") ||
                cmd.contains("wake up") ||
                cmd.contains("wake screen") ||
                cmd.contains("screen on") ||
                cmd.contains("স্ক্রিন অন করো") ||
                cmd.contains("জাগো") ||
                cmd.contains("screen on koro") ||
                cmd.contains("स्क्रीन ऑन करो")
    }

    private fun extractAppToOpen(cmd: String): String? {
        val prefixes = listOf(
            "open ", "launch ", "start ",
            "খোলো ", "চালু করো ", "open koro ", "kholo ",
            "खोलो ", "शुरू करो "
        )
        for (prefix in prefixes) {
            if (cmd.startsWith(prefix)) {
                return cmd.removePrefix(prefix).trim()
            }
        }
        if (cmd.endsWith(" খোলো")) {
            return cmd.removeSuffix(" খোলো").trim()
        }
        if (cmd.endsWith(" kholo")) {
            return cmd.removeSuffix(" kholo").trim()
        }
        if (cmd.endsWith(" खोलो")) {
            return cmd.removeSuffix(" खोलो").trim()
        }
        return null
    }

    private fun isDoNotDisturb(cmd: String): Boolean {
        return cmd.contains("do not disturb") ||
                cmd.contains("turn on dnd") ||
                cmd.contains("turn on do not disturb") ||
                cmd.contains("silent mode") ||
                cmd.contains("ডিএনডি চালু করো") ||
                cmd.contains("डू नॉट डिस्टर्ब चालू करो")
    }

    private fun isBatterySaver(cmd: String): Boolean {
        return cmd.contains("battery saver") ||
                cmd.contains("turn on battery saver") ||
                cmd.contains("power saver") ||
                cmd.contains("ব্যাটারি সেভার চালু করো") ||
                cmd.contains("बैटरी सेवर चालू करो")
    }

    private fun isDialNumber(cmd: String): Boolean {
        return cmd.startsWith("dial ") || cmd.contains("dial number") || cmd.startsWith("ডায়াল ") || cmd.startsWith("डायल ")
    }

    private fun extractPhoneNumber(cmd: String): String {
        return cmd.filter { it.isDigit() || it == '+' }
    }

    private fun extractCallTarget(cmd: String): String? {
        val callPrefixes = listOf("call ", "phone ", "কল করো ", "ফোন করো ", "call koro ", "कॉल करो ")
        for (prefix in callPrefixes) {
            if (cmd.startsWith(prefix)) {
                return cmd.removePrefix(prefix).trim()
            }
        }
        return null
    }

    private fun extractMessageTarget(cmd: String): Pair<String, String>? {
        val messagePatterns = listOf(
            "send a message to ",
            "send message to ",
            "message to ",
            "message ",
            "বার্তা পাঠাও ",
            "মেসেজ পাঠাও "
        )
        for (p in messagePatterns) {
            if (cmd.startsWith(p)) {
                val rest = cmd.removePrefix(p).trim()
                val parts = rest.split(" saying ", " text ", " message ", limit = 2)
                val recipient = parts[0].trim()
                val body = if (parts.size > 1) parts[1].trim() else "Hello!"
                return Pair(recipient, body)
            }
        }
        return null
    }

    private fun extractTimerSeconds(cmd: String): Int? {
        if (!cmd.contains("timer") && !cmd.contains("টাইমার") && !cmd.contains("टाइमर")) return null

        val digitRegex = Regex("(\\d+)\\s*(minute|minutes|min|mins|second|seconds|sec|secs|মিনিট|সেকেন্ড|मिनट|सेकंड)")
        val match = digitRegex.find(cmd)
        if (match != null) {
            val amount = match.groupValues[1].toIntOrNull() ?: return null
            val unit = match.groupValues[2]
            return if (unit.contains("sec") || unit.contains("সেকেন্ড") || unit.contains("सेकंड")) {
                amount
            } else {
                amount * 60
            }
        }
        return null
    }

    private fun extractAlarmTime(cmd: String): Pair<Int, Int>? {
        if (!cmd.contains("alarm") && !cmd.contains("অ্যালার্ম") && !cmd.contains("अलार्म")) return null

        // Pattern for "alarm for 6 AM", "alarm for 6:30 PM", "alarm at 7"
        val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val match = timeRegex.find(cmd)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3].lowercase()

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            return Pair(hour, minute)
        }
        return null
    }

    private fun isTimeQuery(cmd: String): Boolean {
        return cmd == "what time is it" ||
                cmd == "what's the time" ||
                cmd == "time" ||
                cmd.contains("current time") ||
                cmd.contains("কয়টা বাজে") ||
                cmd.contains("সময় কত") ||
                cmd.contains("time koto") ||
                cmd.contains("कितने बजे हैं") ||
                cmd.contains("समय क्या है")
    }

    private fun isDateQuery(cmd: String): Boolean {
        return cmd == "what's today's date" ||
                cmd == "what is today's date" ||
                cmd == "date" ||
                cmd.contains("today's date") ||
                cmd.contains("আজকের তারিখ") ||
                cmd.contains("আজ কি বার") ||
                cmd.contains("ajker date") ||
                cmd.contains("आज की तारीख") ||
                cmd.contains("आज कौन सा दिन है")
    }

    private fun extractSearchQuery(cmd: String): String? {
        val searchPrefixes = listOf(
            "search the web for ", "search for ", "google ", "web search ",
            "খুঁজে বের করো ", "সার্চ করো ", "search koro ", "सर्च करो "
        )
        for (prefix in searchPrefixes) {
            if (cmd.startsWith(prefix)) {
                return cmd.removePrefix(prefix).trim()
            }
        }
        return null
    }

    // --- Action Execution Helpers ---

    private fun setTimer(seconds: Int): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Nova Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback: launch clock app
            appLauncher.launchAppByName("clock")
            false
        }
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            appLauncher.launchAppByName("clock")
            false
        }
    }

    private fun openDndSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    private fun openBatterySaverSettings() {
        try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    fun dialNumber(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun directCallNumber(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            dialNumber(number)
        }
    }

    fun sendSmsIntent(recipient: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(recipient)}")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun performWebSearch(query: String) {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
