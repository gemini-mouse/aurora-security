package app.aurorasecurity.security

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aurorasecurity.security.ui.theme.AuroraBlue
import app.aurorasecurity.security.ui.theme.AuroraTeal
import app.aurorasecurity.security.ui.theme.Ember
import app.aurorasecurity.security.ui.theme.EmberDeep
import app.aurorasecurity.security.ui.theme.Frost
import app.aurorasecurity.security.ui.theme.Graphite
import app.aurorasecurity.security.ui.theme.GradientHeroStart
import app.aurorasecurity.security.ui.theme.GradientHeroMid
import app.aurorasecurity.security.ui.theme.GradientHeroEnd
import app.aurorasecurity.security.ui.theme.GradientHeroLightStart
import app.aurorasecurity.security.ui.theme.GradientHeroLightMid
import app.aurorasecurity.security.ui.theme.GradientHeroLightEnd
import app.aurorasecurity.security.ui.theme.GradientSafeStart
import app.aurorasecurity.security.ui.theme.GradientSafeMid
import app.aurorasecurity.security.ui.theme.GradientSafeEnd
import app.aurorasecurity.security.ui.theme.GradientSafeLightStart
import app.aurorasecurity.security.ui.theme.GradientSafeLightMid
import app.aurorasecurity.security.ui.theme.GradientSafeLightEnd
import app.aurorasecurity.security.ui.theme.GradientAurora1
import app.aurorasecurity.security.ui.theme.GradientAurora2
import app.aurorasecurity.security.ui.theme.GradientSosRed1
import app.aurorasecurity.security.ui.theme.GradientSosRed2
import androidx.compose.ui.draw.scale
import app.aurorasecurity.security.ui.theme.GradientSilent1
import app.aurorasecurity.security.ui.theme.GradientSilent2
import app.aurorasecurity.security.ui.theme.GradientSilentLight1
import app.aurorasecurity.security.ui.theme.GradientSilentLight2
import app.aurorasecurity.security.ui.theme.BorderLight
import app.aurorasecurity.security.ui.theme.Mist
import app.aurorasecurity.security.ui.theme.Paper
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.aurorasecurity.security.ui.theme.AlarmTheme
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.annotation.StringRes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.aurorasecurity.security.onboarding.ContactSetupStatus
import app.aurorasecurity.security.onboarding.ContactSetupType
import app.aurorasecurity.security.onboarding.OnboardingFlowOverlay
import app.aurorasecurity.security.onboarding.QuickSetupProgress
import app.aurorasecurity.security.onboarding.QuickSetupStep
import app.aurorasecurity.security.onboarding.QuickSetupWizardOverlay
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private var latestIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        AlarmPreferences(this)
        super.onCreate(savedInstanceState)
        latestIntentState = intent
        setContent {
            AlarmApp(activityIntent = latestIntentState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntentState = intent
    }

    override fun onStart() {
        super.onStart()
        AlarmAppVisibility.isInForeground = true
    }
    override fun onStop() {
        AlarmAppVisibility.isInForeground = false
        super.onStop()
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlarmApp(activityIntent: Intent? = null) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val preferences = remember(context) { AlarmPreferences(context) }
    val aiDeviceSupport = remember(context) { context.getAiDeviceSupport() }
    val historyManager = remember(context) { HistoryManager(context) }
    var themeMode by rememberSaveable { mutableStateOf(preferences.getThemeMode()) }
    var appLanguage by rememberSaveable { mutableStateOf(preferences.getAppLanguageOption()) }
    val backendApi = remember { TelegramBackendApi() }
    val pushBackendApi = remember { PushBackendApi() }
    val locationSnapshotProvider = remember(context) { LocationSnapshotProvider(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val uiState by NoiseAlarmController.uiState.collectAsState()
    val historyRecords by historyManager.recordsFlow.collectAsState()
    val latestAlertDeliverySummary = remember(historyRecords) {
        historyRecords.firstNotNullOfOrNull { it.toAlertDeliverySummaryOrNull() }
    }
    var autoShownAlertResultId by rememberSaveable {
        mutableStateOf(latestAlertDeliverySummary?.recordId.orEmpty())
    }
    var selectedAlertResult by remember { mutableStateOf<AlertDeliverySummary?>(null) }

    LaunchedEffect(latestAlertDeliverySummary?.recordId) {
        val summary = latestAlertDeliverySummary ?: return@LaunchedEffect
        if (summary.recordId != autoShownAlertResultId) {
            autoShownAlertResultId = summary.recordId
            selectedAlertResult = summary
        }
    }
    val bindCode = remember { preferences.getBindCode() }
    val userId = remember { preferences.getUserId() }
    var hasAcceptedTermsOfUse by rememberSaveable { mutableStateOf(preferences.hasAcceptedTermsOfUse()) }
    var hasSeenOnboarding by rememberSaveable { mutableStateOf(preferences.hasSeenOnboarding()) }

    LaunchedEffect(configuration) {
        val resolvedLanguage = preferences.getAppLanguageOption()
        if (resolvedLanguage != appLanguage) {
            appLanguage = resolvedLanguage
        }
    }
    var hasCompletedQuickSetup by rememberSaveable { mutableStateOf(preferences.hasCompletedQuickSetup()) }
    var hasCompletedSetupTest by rememberSaveable { mutableStateOf(preferences.hasCompletedSetupTest()) }
    var quickSetupStep by rememberSaveable { mutableStateOf(preferences.getQuickSetupCurrentStep()) }
    var isOnboardingPreviewVisible by rememberSaveable { mutableStateOf(false) }
    var isQuickSetupVisible by rememberSaveable { mutableStateOf(false) }
    var quickSetupDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(AlarmTab.Detect) }
    var requestedHistoryFilterKey by rememberSaveable { mutableStateOf(HISTORY_FILTER_ALL) }
    var requestedHistoryFilterVersion by rememberSaveable { mutableStateOf(0) }
    var settingsScrollToTopToken by remember { mutableLongStateOf(0L) }
    var requestedSettingsSectionKey by rememberSaveable { mutableStateOf("") }
    var requestedSettingsSectionVersion by rememberSaveable { mutableStateOf(0) }
    val swipeTabs = remember { AlarmTab.entries.filter { it != AlarmTab.History } }
    val pagerState = rememberPagerState(
        initialPage = swipeTabs.indexOf(selectedTab).coerceAtLeast(0),
        pageCount = { swipeTabs.size },
    )
    val activeSwipeTab = swipeTabs[pagerState.currentPage]
    val settledSwipeTab = swipeTabs[pagerState.settledPage]
    var contactSettings by remember { mutableStateOf(preferences.getAlertContactSettings()) }
    var aiSettings by remember {
        mutableStateOf(clampAiSettingsForDevice(preferences.getAiSettings(), aiDeviceSupport))
    }
    val aiState by GemmaAudioAnalysisManager.uiState.collectAsState()
    var movementSettings by remember { mutableStateOf(preferences.getMovementSettings()) }
    val telegramContacts = remember { mutableStateListOf<TelegramContact>() }
    val pushContacts = remember { mutableStateListOf<PushContact>() }
    var contactsSyncMessage by rememberSaveable {
        mutableStateOf(context.getString(R.string.contacts_not_synced))
    }
    var pushSyncMessage by rememberSaveable {
        mutableStateOf(context.getString(R.string.push_contacts_not_synced))
    }
    var lastSyncTime by rememberSaveable { mutableLongStateOf(0L) }
    var lastPushSyncTime by rememberSaveable { mutableLongStateOf(0L) }
    var trustedPushBindCode by rememberSaveable { mutableStateOf("") }
    var hasLocationPermission by rememberSaveable { mutableStateOf(locationSnapshotProvider.hasLocationPermission()) }
    var isMonitoringPaused by rememberSaveable { mutableStateOf(preferences.getMonitoringPaused()) }
    var hasCallPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasSmsPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasNotificationPermission by rememberSaveable {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED &&
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            },
        )
    }
    var phoneCallTriggeredForCurrentAlarm by rememberSaveable { mutableStateOf(false) }
    val isTraditionalChineseUi = configuration.locales[0]?.language.equals("zh", ignoreCase = true)

    fun hasAnyContactMethodEnabled(settings: AlertContactSettings): Boolean {
        return settings.useTelegram || settings.usePush || settings.usePhoneCall || settings.useSms
    }

    val contactMethodMessage = if (hasAnyContactMethodEnabled(contactSettings)) {
        defaultContactMethodMessage(context)
    } else {
        contactMethodRequiredMessage(context)
    }

    fun updateQuickSetupStep(step: QuickSetupStep) {
        if (quickSetupStep == step) return
        quickSetupStep = step
        preferences.setQuickSetupCurrentStep(step)
    }

    fun isQuickSetupPermissionsReady(): Boolean {
        return uiState.hasMicPermission &&
            hasLocationPermission &&
            hasNotificationPermission &&
            hasCallPermission &&
            hasSmsPermission
    }

    fun isPushContactReady(): Boolean {
        return contactSettings.usePush &&
            hasNotificationPermission &&
            pushContacts.any { it.status == TelegramBindingStatus.Bound }
    }

    fun isTelegramContactReady(): Boolean {
        return contactSettings.useTelegram &&
            telegramContacts.any { it.status == TelegramBindingStatus.Bound }
    }

    fun isSmsContactReady(): Boolean {
        return contactSettings.useSms &&
            hasSmsPermission &&
            contactSettings.emergencyPhoneNumber.isNotBlank()
    }

    fun isPhoneCallContactReady(): Boolean {
        return contactSettings.usePhoneCall &&
            hasCallPermission &&
            contactSettings.emergencyPhoneNumber.isNotBlank()
    }

    val quickSetupContactStatuses = listOf(
        ContactSetupStatus(
            type = ContactSetupType.Telegram,
            isRecommended = false,
            isReady = isTelegramContactReady(),
            summary = when {
                isTelegramContactReady() -> context.getString(R.string.quick_setup_status_ready)
                !contactSettings.useTelegram -> context.getString(R.string.common_off)
                else -> context.getString(R.string.quick_setup_status_required)
            },
        ),
        ContactSetupStatus(
            type = ContactSetupType.Sms,
            isRecommended = false,
            isReady = isSmsContactReady(),
            summary = when {
                isSmsContactReady() -> context.getString(R.string.quick_setup_status_ready)
                !contactSettings.useSms -> context.getString(R.string.common_off)
                !hasSmsPermission -> context.getString(R.string.quick_setup_status_allow)
                else -> context.getString(R.string.quick_setup_status_number)
            },
        ),
        ContactSetupStatus(
            type = ContactSetupType.PhoneCall,
            isRecommended = false,
            isReady = isPhoneCallContactReady(),
            summary = when {
                isPhoneCallContactReady() -> context.getString(R.string.quick_setup_status_ready)
                !contactSettings.usePhoneCall -> context.getString(R.string.common_off)
                !hasCallPermission -> context.getString(R.string.quick_setup_status_allow)
                else -> context.getString(R.string.quick_setup_status_number)
            },
        ),
        ContactSetupStatus(
            type = ContactSetupType.Push,
            isRecommended = false,
            isReady = isPushContactReady(),
            summary = when {
                isPushContactReady() -> context.getString(R.string.quick_setup_status_ready)
                !contactSettings.usePush -> context.getString(R.string.common_off)
                else -> context.getString(R.string.quick_setup_status_required)
            },
        ),
    )

    fun isQuickSetupContactsReady(): Boolean {
        return quickSetupContactStatuses.any { it.isReady }
    }

    val quickSetupProgress = QuickSetupProgress(
        currentStep = quickSetupStep,
        permissionsReady = isQuickSetupPermissionsReady(),
        contactsReady = isQuickSetupContactsReady(),
        testComplete = hasCompletedSetupTest,
    )
    val shouldShowOnboarding = hasAcceptedTermsOfUse && (!hasSeenOnboarding || isOnboardingPreviewVisible)

    fun updateContactSettings(updatedSettings: AlertContactSettings) {
        if (updatedSettings == contactSettings) return
        contactSettings = updatedSettings
        preferences.saveAlertContactSettings(updatedSettings)
    }

    fun updateAiSettings(updatedSettings: AiSettings, prepareEngine: Boolean = false) {
        if (updatedSettings == aiSettings) return
        if (aiSettings.uploadTrainingTriggerAudio && !updatedSettings.uploadTrainingTriggerAudio) {
            preferences.getPendingTrainingAudios().forEach { payload ->
                runCatching { File(payload.wavFilePath).delete() }
            }
            preferences.clearPendingTrainingAudios()
        }
        aiSettings = updatedSettings
        preferences.saveAiSettings(updatedSettings)
        if (prepareEngine) {
            scope.launch {
                prepareAiEngineIfNeeded(context, updatedSettings)
            }
        }
    }

    fun updateMovementSettings(updatedSettings: MovementSettings, persist: Boolean = true) {
        val settingsChanged = updatedSettings != movementSettings
        if (settingsChanged) {
            movementSettings = updatedSettings
            NoiseAlarmController.setMovementSettings(
                useShake = updatedSettings.useShakeDetection,
                thresholdG = updatedSettings.shakeThresholdG,
            )
        }
        if (persist && updatedSettings != preferences.getMovementSettings()) {
            preferences.saveMovementSettings(updatedSettings)
        }
    }

    fun updateThreshold(updatedThreshold: Float, persist: Boolean = true) {
        if (updatedThreshold != uiState.thresholdDb) {
            NoiseAlarmController.setThreshold(updatedThreshold)
        }
        if (persist && updatedThreshold != preferences.getThresholdDb()) {
            preferences.setThresholdDb(updatedThreshold)
        }
    }

    fun updateThemeMode(updatedThemeMode: AppThemeMode) {
        if (updatedThemeMode == themeMode) return
        themeMode = updatedThemeMode
        preferences.setThemeMode(updatedThemeMode)
    }

    fun updateAppLanguage(updatedAppLanguage: AppLanguageOption) {
        if (updatedAppLanguage == appLanguage) return
        appLanguage = updatedAppLanguage
        preferences.setAppLanguageOption(updatedAppLanguage)
    }

    fun navigateToTab(tab: AlarmTab) {
        if (tab == AlarmTab.Settings) {
            settingsScrollToTopToken++
        }
        selectedTab = tab
        if (tab == AlarmTab.History) return
        val targetPage = swipeTabs.indexOf(tab)
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            scope.launch {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    fun openSettingsSection(sectionKey: String) {
        requestedSettingsSectionKey = sectionKey
        requestedSettingsSectionVersion += 1
        selectedTab = AlarmTab.Settings
        val targetPage = swipeTabs.indexOf(AlarmTab.Settings)
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            scope.launch {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(activityIntent) {
        val pushBindCodeFromLink = extractPushBindCodeFromUri(activityIntent?.data)
        if (pushBindCodeFromLink != null) {
            trustedPushBindCode = pushBindCodeFromLink
            pushSyncMessage = context.getString(R.string.push_setup_link_opened)
            openSettingsSection(SETTINGS_SECTION_PUSH_BIND)
            return@LaunchedEffect
        }

        if (activityIntent?.getBooleanExtra(PushNotificationHelper.EXTRA_OPEN_HISTORY, false) == true) {
            requestedHistoryFilterKey = activityIntent.getStringExtra(PushNotificationHelper.EXTRA_HISTORY_FILTER)
                ?: PushNotificationHelper.HISTORY_FILTER_INCOMING
            requestedHistoryFilterVersion += 1
            selectedTab = AlarmTab.History
        }
    }

    fun refreshPermissionState() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        NoiseAlarmController.setMicPermission(hasMicPermission)
        hasLocationPermission = locationSnapshotProvider.hasLocationPermission()
        hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
        hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun startNoiseAlarmMonitoringService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, NoiseAlarmService::class.java).setAction(NoiseAlarmService.ACTION_START),
        )
    }

    // Permission Queueing
    val permissionQueue = remember { mutableStateListOf<String>() }
    val currentRationalePermission = remember { mutableStateOf<String?>(null) }
    fun processNextInQueue() {
        if (permissionQueue.isNotEmpty()) {
            currentRationalePermission.value = permissionQueue.removeAt(0)
        } else {
            currentRationalePermission.value = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        preferences.markPermissionRequested(Manifest.permission.RECORD_AUDIO)
        NoiseAlarmController.setMicPermission(granted)
        processNextInQueue()
        if (granted) {
            if (!isMonitoringPaused) {
                startNoiseAlarmMonitoringService()
            }
        } else {
            context.stopService(Intent(context, NoiseAlarmService::class.java))
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(context, context.getString(R.string.toast_permission_microphone_restricted), Toast.LENGTH_LONG).show()
            }
        }
    }
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        preferences.markPermissionsRequested(result.keys)
        refreshPermissionState()
        if (result[Manifest.permission.RECORD_AUDIO] == true && !isMonitoringPaused) {
            startNoiseAlarmMonitoringService()
        } else if (!uiState.hasMicPermission) {
            context.stopService(Intent(context, NoiseAlarmService::class.java))
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        preferences.markPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
        hasLocationPermission = granted
        processNextInQueue()
        if (!granted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(context, context.getString(R.string.toast_permission_location_restricted), Toast.LENGTH_LONG).show()
            }
        }
    }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        preferences.markPermissionRequested(Manifest.permission.CALL_PHONE)
        hasCallPermission = granted
        processNextInQueue()
        if (!granted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE)) {
                Toast.makeText(context, context.getString(R.string.toast_permission_phone_restricted), Toast.LENGTH_LONG).show()
            }
        }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        preferences.markPermissionRequested(Manifest.permission.SEND_SMS)
        hasSmsPermission = granted
        processNextInQueue()
        if (!granted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.SEND_SMS)) {
                Toast.makeText(context, context.getString(R.string.toast_permission_sms_restricted), Toast.LENGTH_LONG).show()
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        preferences.markPermissionRequested(Manifest.permission.POST_NOTIFICATIONS)
        hasNotificationPermission = granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        processNextInQueue()
        if (!granted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(context, context.getString(R.string.toast_permission_notifications_restricted), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun launchSystemPermissionRequest(permission: String) {
        when (permission) {
            Manifest.permission.RECORD_AUDIO -> permissionLauncher.launch(permission)
            Manifest.permission.ACCESS_FINE_LOCATION -> locationPermissionLauncher.launch(permission)
            Manifest.permission.CALL_PHONE -> callPermissionLauncher.launch(permission)
            Manifest.permission.SEND_SMS -> smsPermissionLauncher.launch(permission)
            Manifest.permission.POST_NOTIFICATIONS -> notificationPermissionLauncher.launch(permission)
        }
    }

    fun requestMissingPermissions() {
        val missingPermissions = INITIAL_RUNTIME_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
            !preferences.hasRequestedPermission(permission)
        }
        if (missingPermissions.isNotEmpty()) {
            permissionQueue.clear()
            permissionQueue.addAll(missingPermissions)
            processNextInQueue()
        }
    }
    fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
        )
    }

    fun requestPermissionOrOpenSettings(permission: String) {
        val activity = context as? Activity
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) return

        val shouldShowRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
        val hasAskedBefore = preferences.hasRequestedPermission(permission)

        // If user has firmly denied multiple times (asked before but system says no more rationale),
        // we must jump to settings as the direct system popup is now blocked.
        if (hasAskedBefore && !shouldShowRationale) {
            openAppSettings()
            Toast.makeText(context, context.getString(R.string.toast_permission_direct_request_blocked), Toast.LENGTH_LONG).show()
        } else {
            // Otherwise, follow the "Why before What" flow with our custom rationale.
            currentRationalePermission.value = permission
        }
    }



    // Countdown and Auto-reset for sync message
    LaunchedEffect(lastSyncTime) {
        if (lastSyncTime == 0L) return@LaunchedEffect
        while (true) {
            val currentTime = System.currentTimeMillis()
            val elapsedSeconds = (currentTime - lastSyncTime) / 1000
            if (elapsedSeconds >= 30) {
                if (contactsSyncMessage.startsWith("Please wait") || contactsSyncMessage == "Syncing...") {
                    contactsSyncMessage = "Sync complete. (Ready)"
                }
                break
            } else {
                if (contactsSyncMessage.startsWith("Please wait")) {
                    val waitTime = 30 - elapsedSeconds
                    contactsSyncMessage = "Please wait ${waitTime}s before syncing again."
                }
            }
            delay(1000)
        }
    }

    LaunchedEffect(lastPushSyncTime) {
        if (lastPushSyncTime == 0L) return@LaunchedEffect
        while (true) {
            val currentTime = System.currentTimeMillis()
            val elapsedSeconds = (currentTime - lastPushSyncTime) / 1000
            if (elapsedSeconds >= 15) {
                if (
                    pushSyncMessage.startsWith("Please wait") ||
                    pushSyncMessage == "Syncing push contacts..."
                ) {
                    pushSyncMessage = if (pushContacts.isEmpty()) {
                        context.getString(R.string.no_push_contacts)
                    } else {
                        context.getString(R.string.push_sync_complete)
                    }
                }
                break
            } else if (pushSyncMessage.startsWith("Please wait")) {
                val waitTime = 15 - elapsedSeconds
                pushSyncMessage = "Please wait ${waitTime}s before syncing again."
            }
            delay(1000)
        }
    }

    fun syncContacts() {
        if (!contactSettings.useTelegram) {
            telegramContacts.clear()
            contactsSyncMessage = context.getString(R.string.telegram_sync_disabled)
            return
        }
        if (contactSettings.apiUrl.isBlank()) {
            contactsSyncMessage = context.getString(R.string.backend_api_url_required)
            return
        }

        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastSyncTime) / 1000
        if (elapsedSeconds < 30) {
            val waitTime = 30 - elapsedSeconds
            contactsSyncMessage = context.getString(R.string.sync_wait_seconds, waitTime)
            return
        }

        scope.launch {
            contactsSyncMessage = context.getString(R.string.telegram_sync_in_progress)
            try {
                backendApi.registerUser(
                    settings = contactSettings,
                    userId = userId,
                    bindCode = bindCode,
                )
                val contacts = backendApi.fetchContacts(
                    settings = contactSettings,
                    userId = userId,
                )
                telegramContacts.clear()
                telegramContacts.addAll(contacts)
                preferences.saveTelegramContacts(contacts.toList())
                lastSyncTime = System.currentTimeMillis()
                contactsSyncMessage = if (contacts.isEmpty()) {
                    context.getString(R.string.no_telegram_contacts)
                } else {
                    context.getString(R.string.sync_complete)
                }
            } catch (e: Exception) {
                contactsSyncMessage = context.getString(R.string.sync_failed_with_reason, e.message ?: "")
            }
        }
    }

    fun syncPushContacts() {
        if (!contactSettings.usePush) {
            pushContacts.clear()
            pushSyncMessage = context.getString(R.string.push_sync_disabled)
            return
        }
        if (contactSettings.apiUrl.isBlank()) {
            pushSyncMessage = context.getString(R.string.backend_api_url_required)
            return
        }
        if (!FirebaseInitializer.isConfigured()) {
            pushSyncMessage = context.getString(R.string.push_sync_fcm_not_configured)
            return
        }

        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastPushSyncTime) / 1000
        if (elapsedSeconds < 15) {
            val waitTime = 15 - elapsedSeconds
            pushSyncMessage = context.getString(R.string.sync_wait_seconds, waitTime)
            return
        }

        scope.launch {
            pushSyncMessage = context.getString(R.string.push_sync_in_progress)
            val registrationResult = PushRegistrationManager.registerCurrentDevice(context, contactSettings)
            if (registrationResult.isFailure) {
                pushSyncMessage = registrationResult.exceptionOrNull()?.message
                    ?: context.getString(R.string.push_register_failed)
                return@launch
            }

            try {
                val contacts = pushBackendApi.fetchContacts(
                    settings = contactSettings,
                    userId = userId,
                )
                pushContacts.clear()
                pushContacts.addAll(contacts)
                preferences.savePushContacts(contacts.toList())
                lastPushSyncTime = System.currentTimeMillis()
                pushSyncMessage = if (contacts.isEmpty()) {
                    context.getString(R.string.no_push_contacts)
                } else {
                    context.getString(R.string.push_sync_complete)
                }
            } catch (e: Exception) {
                pushSyncMessage = context.getString(R.string.push_sync_failed_with_reason, e.message ?: "")
            }
        }
    }

    fun prepareTelegramSetupLink(onPrepared: () -> Unit) {
        if (!contactSettings.useTelegram) {
            contactsSyncMessage = context.getString(R.string.telegram_enable_before_share)
            return
        }
        if (contactSettings.apiUrl.isBlank()) {
            contactsSyncMessage = context.getString(R.string.backend_api_url_required)
            return
        }

        preferences.saveAlertContactSettings(contactSettings)
        contactsSyncMessage = context.getString(R.string.telegram_setup_link_preparing)
        scope.launch {
            try {
                backendApi.registerUser(
                    settings = contactSettings,
                    userId = userId,
                    bindCode = bindCode,
                )
                contactsSyncMessage = context.getString(R.string.telegram_setup_link_ready)
                onPrepared()
            } catch (e: Exception) {
                contactsSyncMessage = context.getString(R.string.telegram_setup_link_prepare_failed)
            }
        }
    }

    fun deleteTelegramContact(contact: TelegramContact) {
        scope.launch {
            contactsSyncMessage = context.getString(
                R.string.removing_contact_with_name,
                contact.name.ifBlank { context.getString(R.string.contact_fallback_name) },
            )
            val deleted = backendApi.deleteContact(
                settings = contactSettings,
                userId = userId,
                contactId = contact.id,
            )
            if (deleted) {
                telegramContacts.removeAll { it.id == contact.id }
                preferences.saveTelegramContacts(telegramContacts.toList())
                contactsSyncMessage = context.getString(R.string.contact_removed)
            } else {
                contactsSyncMessage = context.getString(R.string.contact_remove_failed)
            }
        }
    }

    fun bindCurrentDeviceToTrustedContact() {
        val bindCodeInput = trustedPushBindCode.trim().uppercase()
        if (!contactSettings.usePush) {
            pushSyncMessage = context.getString(R.string.push_enable_before_bind)
            return
        }
        if (bindCodeInput.isBlank()) {
            pushSyncMessage = context.getString(R.string.push_bind_code_required)
            return
        }
        if (!hasNotificationPermission) {
            pushSyncMessage = context.getString(R.string.push_bind_notifications_required)
            return
        }

        scope.launch {
            pushSyncMessage = context.getString(R.string.push_binding_current_device)
            val registrationResult = PushRegistrationManager.registerCurrentDevice(
                context,
                contactSettings,
                pushBackendApi,
            )
            if (registrationResult.isFailure) {
                pushSyncMessage = registrationResult.exceptionOrNull()?.message
                    ?: context.getString(R.string.push_register_failed)
                return@launch
            }

            val bound = pushBackendApi.bindCurrentDevice(
                settings = contactSettings,
                bindCode = bindCodeInput,
                contactUserId = userId,
            )
            if (bound) {
                trustedPushBindCode = ""
                pushSyncMessage = context.getString(R.string.push_binding_ready)
            } else {
                pushSyncMessage = context.getString(R.string.push_binding_failed)
            }
        }
    }

    fun deletePushContact(contact: PushContact) {
        scope.launch {
            pushSyncMessage = context.getString(
                R.string.removing_contact_with_name,
                contact.name.ifBlank { context.getString(R.string.push_contact_fallback_name) },
            )
            val deleted = pushBackendApi.deleteContact(
                settings = contactSettings,
                userId = userId,
                contactId = contact.id,
            )
            if (deleted) {
                pushContacts.removeAll { it.id == contact.id }
                preferences.savePushContacts(pushContacts.toList())
                pushSyncMessage = context.getString(R.string.push_contact_removed)
            } else {
                pushSyncMessage = context.getString(R.string.push_contact_remove_failed)
            }
        }
    }

    LaunchedEffect(hasAcceptedTermsOfUse) {
        if (!hasAcceptedTermsOfUse) return@LaunchedEffect
        val cachedContacts = preferences.getTelegramContacts()
        if (cachedContacts.isNotEmpty()) {
            telegramContacts.clear()
            telegramContacts.addAll(cachedContacts)
            contactsSyncMessage = context.getString(R.string.loaded_from_preferences)
        }
        val cachedPushContacts = preferences.getPushContacts()
        if (cachedPushContacts.isNotEmpty()) {
            pushContacts.clear()
            pushContacts.addAll(cachedPushContacts)
            pushSyncMessage = context.getString(R.string.loaded_from_preferences)
        }
        NoiseAlarmController.setThreshold(preferences.getThresholdDb())
        NoiseAlarmController.setMovementSettings(
            useShake = movementSettings.useShakeDetection,
            thresholdG = movementSettings.shakeThresholdG
        )
        val currentModel = GemmaModelType.fromName(aiSettings.modelType)
        GemmaAudioAnalysisManager.refreshState(context, currentModel)
    }
    LaunchedEffect(
        contactSettings.usePush,
        contactSettings.userName,
        contactSettings.deviceName,
        contactSettings.apiUrl,
    ) {
        if (
            !contactSettings.usePush ||
            contactSettings.apiUrl.isBlank() ||
            !FirebaseInitializer.isConfigured()
        ) {
            return@LaunchedEffect
        }

        // Debounce repeated keystrokes so device registration runs after edits settle.
        delay(600)
        PushRegistrationManager.registerCurrentDevice(context, contactSettings, pushBackendApi)
    }
    LaunchedEffect(
        aiDeviceSupport.supportsOnDeviceAi,
        aiDeviceSupport.defaultModelType,
        aiSettings.enableAiAnalysis,
        aiSettings.preloadAiInBackground,
        aiSettings.modelType,
    ) {
        val sanitizedSettings = clampAiSettingsForDevice(aiSettings, aiDeviceSupport)
        if (sanitizedSettings != aiSettings) {
            updateAiSettings(sanitizedSettings)
        }
    }
    LaunchedEffect(hasSmsPermission, contactSettings.useSms) {
        if (!hasSmsPermission && contactSettings.useSms) {
            updateContactSettings(contactSettings.copy(useSms = false))
        }
    }
    LaunchedEffect(hasCallPermission, contactSettings.usePhoneCall) {
        if (!hasCallPermission && contactSettings.usePhoneCall) {
            updateContactSettings(contactSettings.copy(usePhoneCall = false))
        }
    }


    LaunchedEffect(hasAcceptedTermsOfUse) {
        if (!hasAcceptedTermsOfUse) return@LaunchedEffect
        refreshPermissionState()
        if (uiState.hasMicPermission && !isMonitoringPaused) {
            startNoiseAlarmMonitoringService()
        }
    }
    LaunchedEffect(
        hasAcceptedTermsOfUse,
        hasSeenOnboarding,
        hasCompletedQuickSetup,
        quickSetupDismissedThisSession,
        isOnboardingPreviewVisible,
    ) {
        if (!hasAcceptedTermsOfUse || !hasSeenOnboarding || isOnboardingPreviewVisible) {
            isQuickSetupVisible = false
            return@LaunchedEffect
        }
        if (hasCompletedQuickSetup || quickSetupDismissedThisSession) {
            isQuickSetupVisible = false
            return@LaunchedEffect
        }
        isQuickSetupVisible = true
    }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(selectedTab, aiSettings.isAiEnabled) {
        if (selectedTab == AlarmTab.AI && aiSettings.isAiEnabled) {
            val currentModel = GemmaModelType.fromName(aiSettings.modelType)
            // Always refresh file status so the UI shows correct download state.
            // If preload is enabled, prepare the model immediately while the AI tab is open.
            GemmaAudioAnalysisManager.refreshState(context, currentModel)
            if (aiSettings.preloadAiInBackground) {
                runCatching {
                    GemmaAudioAnalysisManager.downloadIfNeeded(context, currentModel)
                }
            }
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (selectedTab == AlarmTab.History) return@LaunchedEffect
        if (selectedTab != settledSwipeTab) {
            selectedTab = settledSwipeTab
        }
    }
    LaunchedEffect(hasAcceptedTermsOfUse, uiState.hasMicPermission, isMonitoringPaused, uiState.isMonitoring) {
        if (!hasAcceptedTermsOfUse || !uiState.hasMicPermission || isMonitoringPaused || uiState.isMonitoring) {
            return@LaunchedEffect
        }
        delay(900)
        if (!uiState.isMonitoring && uiState.hasMicPermission && !isMonitoringPaused) {
            startNoiseAlarmMonitoringService()
        }
    }
    LaunchedEffect(
        uiState.isMonitoring,
        aiSettings.enableAiAnalysis,
        aiSettings.preloadAiInBackground,
        aiSettings.modelType,
    ) {
        if (uiState.isMonitoring && aiSettings.enableAiAnalysis) {
            prepareAiEngineIfNeeded(context, aiSettings)
        }
    }
    // Immediately sync download status when user switches model type,
    // so the card shows the correct state before initialization begins.
    LaunchedEffect(aiSettings.modelType) {
        val currentModel = GemmaModelType.fromName(aiSettings.modelType)
        GemmaAudioAnalysisManager.refreshState(context, currentModel)
    }
    LaunchedEffect(
        uiState.alarmTriggered,
        uiState.activeEmergencyMode,
        contactSettings.usePhoneCall,
        contactSettings.emergencyPhoneNumber,
        hasCallPermission,
    ) {
        val shouldTriggerPhoneCall = uiState.alarmTriggered || uiState.activeEmergencyMode != null
        if (!shouldTriggerPhoneCall) {
            phoneCallTriggeredForCurrentAlarm = false
            return@LaunchedEffect
        }
        if (
            contactSettings.usePhoneCall &&
            hasCallPermission &&
            contactSettings.emergencyPhoneNumber.isNotBlank() &&
            !phoneCallTriggeredForCurrentAlarm
        ) {
            phoneCallTriggeredForCurrentAlarm = true
            runCatching {
                launchEmergencyCall(context, contactSettings.emergencyPhoneNumber)
            }
        }
    }

    fun completeOnboardingAndOpenQuickSetup() {
        preferences.setSeenOnboarding(true)
        hasSeenOnboarding = true
        isOnboardingPreviewVisible = false
        if (hasCompletedQuickSetup) {
            isQuickSetupVisible = false
            return
        }
        quickSetupDismissedThisSession = false
        isQuickSetupVisible = true
        updateQuickSetupStep(QuickSetupStep.Permissions)
    }

    fun skipOnboardingOverlay() {
        if (!hasSeenOnboarding) {
            preferences.setSeenOnboarding(true)
            hasSeenOnboarding = true
            quickSetupDismissedThisSession = true
            isQuickSetupVisible = false
        }
        isOnboardingPreviewVisible = false
    }

    fun closeQuickSetupForThisSession() {
        quickSetupDismissedThisSession = true
        isQuickSetupVisible = false
    }

    fun reviewOnboardingFromSettings() {
        quickSetupDismissedThisSession = true
        isQuickSetupVisible = false
        isOnboardingPreviewVisible = true
    }

    fun resumeQuickSetupFromSettings() {
        isOnboardingPreviewVisible = false
        quickSetupDismissedThisSession = false
        isQuickSetupVisible = true
    }

    fun openContactSetupFromWizard(type: ContactSetupType) {
        closeQuickSetupForThisSession()
        when (type) {
            ContactSetupType.Push -> openSettingsSection(SETTINGS_SECTION_CONTACTS_PUSH)
            ContactSetupType.Telegram -> openSettingsSection(SETTINGS_SECTION_CONTACTS_TELEGRAM)
            ContactSetupType.Sms -> openSettingsSection(SETTINGS_SECTION_CONTACTS_SMS)
            ContactSetupType.PhoneCall -> openSettingsSection(SETTINGS_SECTION_CONTACTS_PHONE)
        }
    }

    AlarmTheme(darkTheme = themeMode == AppThemeMode.Dark) {
        selectedAlertResult?.let { summary ->
            AlertDeliveryResultDialog(
                summary = summary,
                onDismiss = { selectedAlertResult = null },
            )
        }
        val isDark = themeMode == AppThemeMode.Dark
        val backgroundBrush = if (isDark) {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to ComposeColor(0xFF171D23),
                    0.35f to ComposeColor(0xFF1F262D),
                    0.65f to ComposeColor(0xFF1A2027),
                    1.0f to ComposeColor(0xFF14191F),
                ),
            )
        } else {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to ComposeColor(0xFFFBF6EF),
                    0.42f to ComposeColor(0xFFF1EAE2),
                    0.78f to ComposeColor(0xFFECE8E1),
                    1.0f to ComposeColor(0xFFFAF4EC),
                ),
            )
        }
        Scaffold(
            topBar = {
                // Transparent aurora-style top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Brand name
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                ComposeColor(0xFF8FA3BA),
                                                ComposeColor(0xFF31577D),
                                            ),
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = ComposeColor.White,
                                    modifier = Modifier.size(23.dp),
                                )
                            }
                            Text(
                                text = "Aurora",
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = 27.sp, lineHeight = 29.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Frost else ComposeColor(0xFF0C2946),
                            )
                        }
                        // History tab on the right side
                        val isHistorySelected = selectedTab == AlarmTab.History
                        IconButton(
                            onClick = {
                                requestedHistoryFilterKey = HISTORY_FILTER_ALL
                                requestedHistoryFilterVersion += 1
                                selectedTab = AlarmTab.History
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(
                                    elevation = if (isDark) 12.dp else 14.dp,
                                    shape = CircleShape,
                                    ambientColor = if (isDark) AuroraBlue.copy(alpha = 0.16f) else ComposeColor(0x22000000),
                                    spotColor = if (isDark) AuroraBlue.copy(alpha = 0.22f) else ComposeColor(0x16000000),
                                )
                                .clip(CircleShape)
                                .background(
                                    brush = if (isDark) {
                                        Brush.linearGradient(
                                            colors = if (isHistorySelected) {
                                                listOf(ComposeColor(0xFF2A3440), ComposeColor(0xFF1E2731))
                                            } else {
                                                listOf(ComposeColor(0xFF222B34), ComposeColor(0xFF171F27))
                                            },
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = if (isHistorySelected) {
                                                listOf(Paper, ComposeColor(0xFFE9DED2))
                                            } else {
                                                listOf(ComposeColor(0xFFF5ECE1), ComposeColor(0xFFE7E3DC))
                                            },
                                        )
                                    },
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) {
                                        ComposeColor.White.copy(alpha = if (isHistorySelected) 0.12f else 0.08f)
                                    } else {
                                        ComposeColor(0x140F1E2D)
                                    },
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                imageVector = AlarmTab.History.icon,
                                contentDescription = AlarmTab.History.label(context),
                                tint = if (isHistorySelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.82f else 0.72f)
                                },
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // Floating island navigation bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), ambientColor = AuroraBlue.copy(alpha = 0.15f))
                            .background(
                                color = if (isDark) ComposeColor(0xFF12212F).copy(alpha = 0.98f)
                                        else Paper.copy(alpha = 0.97f),
                                shape = RoundedCornerShape(32.dp),
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = if (isDark)
                                        listOf(
                                            AuroraBlue.copy(alpha = 0.22f),
                                            BorderLight.copy(alpha = 0.10f),
                                        )
                                    else
                                        listOf(ComposeColor(0x1A8A8177), ComposeColor(0x088A8177)),
                                ),
                                shape = RoundedCornerShape(32.dp),
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            swipeTabs.forEach { tab ->
                                val isSelected = selectedTab != AlarmTab.History && activeSwipeTab == tab
                                val interactionSource = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(interactionSource = interactionSource, indication = null) {
                                            navigateToTab(tab)
                                        }
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            if (isSelected)
                                                Brush.linearGradient(
                                                    colors = if (isDark) {
                                                        listOf(
                                                            ComposeColor(0xFF20384D),
                                                            ComposeColor(0xFF173244),
                                                        )
                                                    } else {
                                                        listOf(
                                                            ComposeColor(0xFFDDD2C6),
                                                            ComposeColor(0xFFD9D3C9),
                                                        )
                                                    },
                                                )
                                            else
                                                Brush.linearGradient(colors = listOf(ComposeColor.Transparent, ComposeColor.Transparent)),
                                        )
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    brush = if (isDark) {
                                                        Brush.linearGradient(
                                                            listOf(
                                                                AuroraBlue.copy(alpha = 0.52f),
                                                                AuroraTeal.copy(alpha = 0.24f),
                                                            ),
                                                        )
                                                    } else {
                                                        Brush.linearGradient(
                                                            listOf(
                                                                ComposeColor(0x338A8177),
                                                                ComposeColor(0x148A8177),
                                                            ),
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(24.dp),
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label(context),
                                            tint = if (isSelected) {
                                                if (isDark) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                            },
                                            modifier = Modifier.size(if (isSelected) 22.dp else 20.dp),
                                        )
                                        Text(
                                            text = tab.label(context),
                                            style = if (tab == AlarmTab.AI && isTraditionalChineseUi) {
                                                MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp)
                                            } else {
                                                MaterialTheme.typography.labelSmall
                                            },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                if (isDark) {
                                                    Frost
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                .padding(innerPadding),
            ) {
                if (selectedTab == AlarmTab.History) {
                    HistoryTab(
                        historyManager = historyManager,
                        requestedFilterKey = requestedHistoryFilterKey,
                        requestedFilterVersion = requestedHistoryFilterVersion,
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (swipeTabs[page]) {
                            AlarmTab.Detect -> DetectTab(
                                uiState = uiState,
                                aiSettings = aiSettings,
                                aiState = aiState,
                                latestAlertDeliverySummary = latestAlertDeliverySummary,
                                onOpenAlertDeliverySummary = { selectedAlertResult = it },
                                hasLocationPermission = hasLocationPermission,
                                hasCallPermission = hasCallPermission,
                                hasSmsPermission = hasSmsPermission,
                                hasNotificationPermission = hasNotificationPermission,
                                isMonitoringPaused = isMonitoringPaused,
                                onRequestAllPermissions = { requestMissingPermissions() },
                                onRequestPermission = { requestPermissionOrOpenSettings(Manifest.permission.RECORD_AUDIO) },
                                onRequestLocationPermission = { requestPermissionOrOpenSettings(Manifest.permission.ACCESS_FINE_LOCATION) },
                                onRequestCallPermission = { requestPermissionOrOpenSettings(Manifest.permission.CALL_PHONE) },
                                onRequestSmsPermission = { requestPermissionOrOpenSettings(Manifest.permission.SEND_SMS) },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionOrOpenSettings(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        openAppSettings()
                                    }
                                },
                                onTriggerSilentSos = {
                                    context.startService(
                                        Intent(context, NoiseAlarmService::class.java)
                                            .setAction(NoiseAlarmService.ACTION_TRIGGER_SILENT_SOS),
                                    )
                                },
                                onTriggerLoudSos = {
                                    context.startService(
                                        Intent(context, NoiseAlarmService::class.java)
                                            .setAction(NoiseAlarmService.ACTION_TRIGGER_LOUD_SOS),
                                    )
                                },
                                onStopAlarm = {
                                    NoiseAlarmController.stopAlarm()
                                    context.startService(
                                        Intent(context, NoiseAlarmService::class.java)
                                            .setAction(NoiseAlarmService.ACTION_SILENCE),
                                    )
                                },
                                onToggleMonitoringPause = { shouldPause ->
                                    isMonitoringPaused = shouldPause
                                    preferences.setMonitoringPaused(shouldPause)
                                    if (shouldPause) {
                                        context.stopService(Intent(context, NoiseAlarmService::class.java))
                                    } else if (uiState.hasMicPermission) {
                                        startNoiseAlarmMonitoringService()
                                    }
                                },
                                onNavigateToSettings = { navigateToTab(AlarmTab.Settings) },
                            )
                            AlarmTab.AI -> AiTab(
                                aiSettings = aiSettings,
                                aiDeviceSupport = aiDeviceSupport,
                                aiState = aiState,
                                onAiSettingsChange = { updatedSettings ->
                                    val sanitizedSettings = clampAiSettingsForDevice(updatedSettings, aiDeviceSupport)
                                    updateAiSettings(sanitizedSettings, prepareEngine = true)
                                },
                                onDownloadModel = {
                                    scope.launch {
                                        runCatching {
                                            val shouldRetryEngine =
                                                aiState.modelStatus == AiModelStatus.Error &&
                                                    !aiState.statusMessage.startsWith("Model download failed") &&
                                                    !aiState.statusMessage.startsWith("Downloaded model is too small")
                                            val currentModel = GemmaModelType.fromName(aiSettings.modelType)
                                            if (shouldRetryEngine) {
                                                GemmaAudioAnalysisManager.retryInitialization(context, currentModel)
                                            } else {
                                                GemmaAudioAnalysisManager.downloadIfNeeded(context, currentModel)
                                            }
                                        }
                                    }
                                },
                                onDeleteModel = { modelToDelete ->
                                    scope.launch {
                                        runCatching {
                                            GemmaAudioAnalysisManager.deleteModel(context, modelToDelete)
                                        }
                                    }
                                },
                                onAudioTest = {
                                    scope.launch {
                                        val currentModel = GemmaModelType.fromName(aiSettings.modelType)
                                        runCatching {
                                            GemmaAudioAnalysisManager.downloadIfNeeded(context, currentModel)
                                        }.onFailure { return@launch }

                                        val wasMonitoring = uiState.isMonitoring
                                        if (wasMonitoring) {
                                            context.stopService(Intent(context, NoiseAlarmService::class.java))
                                            delay(400)
                                        }

                                        val pcmSamples = try {
                                            recordTestPcmSamples(durationSeconds = 5)
                                        } catch (e: Exception) {
                                            if (wasMonitoring && !isMonitoringPaused) {
                                                startNoiseAlarmMonitoringService()
                                            }
                                            return@launch
                                        }

                                        val analysisAudioBytes = AudioEncodingUtil.pcm16ToWavBytes(pcmSamples)
                                        val m4aBytes = AudioEncodingUtil.encodePcm16ToM4aBytes(
                                            context = context,
                                            samples = pcmSamples,
                                        )
                                        val testClipFile = File(context.cacheDir, "test_clip.m4a")
                                        val testClipWavFile = File(context.cacheDir, "test_clip.wav")

                                        val testClip = CrisisAudioClip(
                                            sessionId = -1L,
                                            triggerSource = EmergencyTriggerSource.SilentSosButton,
                                            emergencyMode = EmergencyMode.Silent,
                                            analysisAudioBytes = analysisAudioBytes,
                                            m4aBytes = m4aBytes,
                                            file = testClipFile,
                                            wavFile = testClipWavFile,
                                        )
                                        runCatching {
                                            testClip.file.writeBytes(m4aBytes)
                                            testClip.wavFile.writeBytes(analysisAudioBytes)
                                            val result = GemmaAudioAnalysisManager.analyzeClip(context, testClip, currentModel)
                                            val sosText = AlertMessageFormatter.format(
                                                contactSettings,
                                                uiState.currentDb,
                                                locationSnapshotProvider.getCurrentLocation(),
                                            )
                                            historyManager.saveRecord(
                                                isTest = true,
                                                sosText = sosText,
                                                sourceAudioFile = testClip.file,
                                                aiResultText = result ?: "No analysis provided",
                                            )
                                        }

                                        if (wasMonitoring && !isMonitoringPaused) {
                                            startNoiseAlarmMonitoringService()
                                        }
                                    }
                                },
                            )
                            AlarmTab.Settings -> SettingsTab(
                                alarmPreferences = preferences,
                                thresholdDb = uiState.thresholdDb,
                                currentDb = uiState.currentDb,
                                contactSettings = contactSettings,
                                aiSettings = aiSettings,
                                themeMode = themeMode,
                                appLanguage = appLanguage,
                                bindCode = bindCode,
                                movementSettings = movementSettings,
                                currentG = uiState.currentG,
                                telegramContacts = telegramContacts,
                                pushContacts = pushContacts,
                                contactsSyncMessage = contactsSyncMessage,
                                pushSyncMessage = pushSyncMessage,
                                contactMethodMessage = contactMethodMessage,
                                trustedPushBindCode = trustedPushBindCode,
                                hasCallPermission = hasCallPermission,
                                hasSmsPermission = hasSmsPermission,
                                hasMicPermission = uiState.hasMicPermission,
                                hasLocationPermission = hasLocationPermission,
                                hasNotificationPermission = hasNotificationPermission,
                                hasCompletedQuickSetup = hasCompletedQuickSetup,
                                scrollToTopToken = settingsScrollToTopToken,
                                requestedSectionKey = requestedSettingsSectionKey,
                                requestedSectionVersion = requestedSettingsSectionVersion,
                                onRequestPermission = { requestPermissionOrOpenSettings(Manifest.permission.RECORD_AUDIO) },
                                onRequestLocationPermission = { requestPermissionOrOpenSettings(Manifest.permission.ACCESS_FINE_LOCATION) },
                                onRequestCallPermission = { requestPermissionOrOpenSettings(Manifest.permission.CALL_PHONE) },
                                onRequestSmsPermission = { requestPermissionOrOpenSettings(Manifest.permission.SEND_SMS) },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionOrOpenSettings(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        openAppSettings()
                                    }
                                },
                                onThresholdPreviewChange = {
                                    updateThreshold(it, persist = false)
                                },
                                onThresholdChange = {
                                    updateThreshold(it)
                                },
                                onMovementSettingsPreviewChange = { updatedSettings ->
                                    updateMovementSettings(updatedSettings, persist = false)
                                },
                                onMovementSettingsChange = { updatedSettings ->
                                    updateMovementSettings(updatedSettings)
                                },
                                onAiSettingsChange = { updatedSettings ->
                                    updateAiSettings(updatedSettings, prepareEngine = true)
                                },
                                onContactSettingsChange = { updatedSettings ->
                                    updateContactSettings(updatedSettings)
                                },
                                onUseTelegramChange = { enabled ->
                                    updateContactSettings(contactSettings.copy(useTelegram = enabled))
                                },
                                onUsePushChange = { enabled ->
                                    updateContactSettings(contactSettings.copy(usePush = enabled))
                                },
                                onUseSmsChange = { enabled ->
                                    updateContactSettings(contactSettings.copy(useSms = enabled))
                                },
                                onUsePhoneCallChange = { enabled ->
                                    updateContactSettings(contactSettings.copy(usePhoneCall = enabled))
                                },
                                onThemeModeChange = {
                                    updateThemeMode(it)
                                },
                                onAppLanguageChange = {
                                    updateAppLanguage(it)
                                },
                                onReviewOnboarding = {
                                    reviewOnboardingFromSettings()
                                },
                                onResumeQuickSetup = {
                                    resumeQuickSetupFromSettings()
                                },
                                onSaveAlertProfile = {
                                    if (hasAnyContactMethodEnabled(contactSettings)) {
                                        preferences.saveAlertContactSettings(contactSettings)
                                        syncContacts()
                                        if (contactSettings.usePush) {
                                            syncPushContacts()
                                        }
                                    }
                                },
                                onRefreshContacts = { syncContacts() },
                                onDeleteTelegramContact = { deleteTelegramContact(it) },
                                onPrepareTelegramSetupLink = { onPrepared ->
                                    prepareTelegramSetupLink(onPrepared)
                                },
                                onRefreshPushContacts = { syncPushContacts() },
                                onDeletePushContact = { deletePushContact(it) },
                                onTrustedPushBindCodeChange = { trustedPushBindCode = it },
                                onBindCurrentDeviceToPushCode = { bindCurrentDeviceToTrustedContact() },
                            )
                            AlarmTab.History -> Unit
                        }
                    }
                }
            }
        }

        if (shouldShowOnboarding) {
            OnboardingFlowOverlay(
                onSkip = {
                    skipOnboardingOverlay()
                },
                onStartSetup = {
                    completeOnboardingAndOpenQuickSetup()
                },
            )
        }

        if (hasAcceptedTermsOfUse && !shouldShowOnboarding && isQuickSetupVisible && !hasCompletedQuickSetup) {
            QuickSetupWizardOverlay(
                progress = quickSetupProgress,
                contactStatuses = quickSetupContactStatuses,
                hasMicPermission = uiState.hasMicPermission,
                hasLocationPermission = hasLocationPermission,
                hasNotificationPermission = hasNotificationPermission,
                hasCallPermission = hasCallPermission,
                hasSmsPermission = hasSmsPermission,
                onBack = {
                    val previousStep = quickSetupStep.previous()
                    if (previousStep != null) {
                        updateQuickSetupStep(previousStep)
                    } else {
                        closeQuickSetupForThisSession()
                    }
                },
                onClose = {
                    closeQuickSetupForThisSession()
                },
                onNext = {
                    quickSetupStep.next()?.let { nextStep ->
                        updateQuickSetupStep(nextStep)
                    }
                },
                onFinish = {
                    preferences.setCompletedQuickSetup(true)
                    hasCompletedQuickSetup = true
                    closeQuickSetupForThisSession()
                },
                onRequestMicPermission = {
                    requestPermissionOrOpenSettings(Manifest.permission.RECORD_AUDIO)
                },
                onRequestLocationPermission = {
                    requestPermissionOrOpenSettings(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionOrOpenSettings(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppSettings()
                    }
                },
                onRequestCallPermission = {
                    requestPermissionOrOpenSettings(Manifest.permission.CALL_PHONE)
                },
                onRequestSmsPermission = {
                    requestPermissionOrOpenSettings(Manifest.permission.SEND_SMS)
                },
                onOpenContactSetup = { type ->
                    updateQuickSetupStep(QuickSetupStep.Contacts)
                    openContactSetupFromWizard(type)
                },
            )
        }

        PermissionRationaleOverlay(
            currentRationale = currentRationalePermission.value,
            onAccepted = { permission ->
                // CRITICAL: Dismiss the rationale dialog BEFORE launching the system request
                // so the system permission window can gain focus.
                currentRationalePermission.value = null
                launchSystemPermissionRequest(permission)
            },
            onDismissed = {
                currentRationalePermission.value?.let { preferences.markPermissionRequested(it) }
                currentRationalePermission.value = null
                processNextInQueue()
            }
        )

        if (!hasAcceptedTermsOfUse) {
            TermsOfUseConsentDialog(
                onViewTerms = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_OF_USE_URL)),
                        )
                    }.onFailure {
                        Toast.makeText(
                            context,
                            "Unable to open Terms of Use.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onAgree = {
                    preferences.setAcceptedTermsOfUse(true)
                    hasAcceptedTermsOfUse = true
                },
                onDecline = {
                    (context as? Activity)?.finishAffinity()
                },
            )
        }
    }
}
@Composable
private fun DetectTab(
    uiState: NoiseAlarmUiState,
    aiSettings: AiSettings,
    aiState: AiFeatureUiState,
    latestAlertDeliverySummary: AlertDeliverySummary?,
    onOpenAlertDeliverySummary: (AlertDeliverySummary) -> Unit,
    hasLocationPermission: Boolean,
    hasCallPermission: Boolean,
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    isMonitoringPaused: Boolean,
    onRequestAllPermissions: () -> Unit,
    onRequestPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onTriggerSilentSos: () -> Unit,
    onTriggerLoudSos: () -> Unit,
    onStopAlarm: () -> Unit,
    onToggleMonitoringPause: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val pendingPermissionCount = remember(
        uiState.hasMicPermission,
        hasLocationPermission,
        hasCallPermission,
        hasSmsPermission,
        hasNotificationPermission,
    ) {
        var count = 0
        if (!uiState.hasMicPermission) count++
        if (!hasLocationPermission) count++
        if (!hasCallPermission) count++
        if (!hasSmsPermission) count++
        if (!hasNotificationPermission) count++
        count
    }
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        // Calculate dynamic heights. If screen is very small, cap min heights.
        val nonFlexible = if (pendingPermissionCount > 0) 220.dp else 142.dp
        val availableForCards = (screenHeight - nonFlexible).coerceAtLeast(400.dp)
        
        // Distribute approximately 55% to HeroCard, 45% to SOS Actions
        val heroHeight = (availableForCards * 0.58f).coerceAtLeast(300.dp).coerceAtMost(420.dp)
        val sosHeight = (availableForCards * 0.28f).coerceAtLeast(170.dp).coerceAtMost(230.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Compact permission status banner only shows when setup is incomplete
            if (pendingPermissionCount > 0) {
                PermissionStatusBanner(
                    pendingCount = pendingPermissionCount,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }
            HeroCard(
                modifier = Modifier.height(heroHeight),
                uiState = uiState,
                aiSettings = aiSettings,
                aiState = aiState,
                isMonitoringPaused = isMonitoringPaused,
                onStopAlarm = onStopAlarm,
            )
            SosActionsCard(
                modifier = Modifier.height(sosHeight),
                onTriggerSilentSos = onTriggerSilentSos,
                onTriggerLoudSos = onTriggerLoudSos,
            )
            latestAlertDeliverySummary?.let { summary ->
                AlertDeliveryResultCard(
                    summary = summary,
                    onClick = { onOpenAlertDeliverySummary(summary) },
                )
            }
            MonitoringPauseCard(
                isPaused = isMonitoringPaused,
                onTogglePause = onToggleMonitoringPause,
            )
        }
    }
}

@Composable
private fun AlertDeliveryResultCard(
    summary: AlertDeliverySummary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        ComposeColor(0xFFFBF7F1),
                        ComposeColor(0xFFF1EAE2),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = ComposeColor(0xFFD9CEC4).copy(alpha = 0.64f),
                shape = RoundedCornerShape(28.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    text = stringResource(R.string.alert_delivery_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor(0xFF102A43),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    modifier = Modifier.widthIn(min = 76.dp),
                    text = alertDeliveryStatusLabel(summary.sosAlertStatus),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = alertDeliveryStatusColor(summary.sosAlertStatus),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
            AlertDeliverySteps(summary = summary)
        }
    }
}

@Composable
private fun AlertDeliveryResultDialog(
    summary: AlertDeliverySummary,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.alert_delivery_popup_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AlertDeliveryDetailRow(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    label = stringResource(R.string.alert_delivery_sos_alert),
                    status = summary.sosAlertStatus,
                )
                AlertDeliveryDetailRow(
                    icon = Icons.Outlined.Mic,
                    label = stringResource(R.string.alert_delivery_audio_evidence),
                    status = summary.audioEvidenceStatus,
                )
                AlertDeliveryDetailRow(
                    icon = Icons.Outlined.AutoAwesome,
                    label = stringResource(R.string.alert_delivery_ai_summary),
                    status = summary.aiSummaryAnalysisStatus,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.alert_delivery_done))
            }
        },
    )
}

@Composable
private fun AlertDeliverySteps(summary: AlertDeliverySummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AlertDeliveryStep(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Outlined.Send,
            label = stringResource(R.string.alert_delivery_sos_alert),
            status = summary.sosAlertStatus,
        )
        AlertDeliveryConnector()
        AlertDeliveryStep(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Mic,
            label = stringResource(R.string.alert_delivery_audio_evidence),
            status = summary.audioEvidenceStatus,
        )
        AlertDeliveryConnector()
        AlertDeliveryStep(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.AutoAwesome,
            label = stringResource(R.string.alert_delivery_ai_summary),
            status = summary.aiSummaryAnalysisStatus,
        )
    }
}

@Composable
private fun AlertDeliveryStep(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    status: TextAlertDeliveryStatus,
) {
    val statusColor = alertDeliveryStatusColor(status)
    val statusBackgroundColor = alertDeliveryStatusBackgroundColor(status)
    val statusBorderColor = alertDeliveryStatusBorderColor(status)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (status == TextAlertDeliveryStatus.Sent) {
                        Modifier.shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = statusColor.copy(alpha = 0.42f),
                            spotColor = statusColor.copy(alpha = 0.34f),
                        )
                    } else {
                        Modifier
                    },
                )
                .background(statusBackgroundColor, CircleShape)
                .border(1.25.dp, statusBorderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = ComposeColor(0xFF102A43),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = alertDeliveryStatusLabel(status),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun AlertDeliveryConnector() {
    Box(
        modifier = Modifier
            .padding(top = 22.dp)
            .width(8.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    )
}

@Composable
private fun AlertDeliveryDetailRow(
    icon: ImageVector,
    label: String,
    status: TextAlertDeliveryStatus,
) {
    val statusColor = alertDeliveryStatusColor(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(alertDeliveryStatusBackgroundColor(status), CircleShape)
                .border(1.dp, alertDeliveryStatusBorderColor(status), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.width(86.dp),
            text = alertDeliveryStatusLabel(status),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun alertDeliveryStatusColor(status: TextAlertDeliveryStatus): ComposeColor {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (status) {
        TextAlertDeliveryStatus.Sent -> if (isDark) ComposeColor(0xFF65F0DF) else ComposeColor(0xFF119A8D)
        TextAlertDeliveryStatus.Failed -> if (isDark) ComposeColor(0xFFFF8A84) else MaterialTheme.colorScheme.error
        TextAlertDeliveryStatus.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.86f else 0.78f)
    }
}

@Composable
private fun alertDeliveryStatusBackgroundColor(status: TextAlertDeliveryStatus): ComposeColor {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (status) {
        TextAlertDeliveryStatus.Sent -> if (isDark) ComposeColor(0xFF103F3B) else ComposeColor(0xFFD9F3EF)
        TextAlertDeliveryStatus.Failed -> if (isDark) ComposeColor(0xFF44201F) else ComposeColor(0xFFFFE1DE)
        TextAlertDeliveryStatus.Skipped -> if (isDark) ComposeColor(0xFF2B333A) else ComposeColor(0xFFE8E4DD)
    }
}

@Composable
private fun alertDeliveryStatusBorderColor(status: TextAlertDeliveryStatus): ComposeColor {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (status) {
        TextAlertDeliveryStatus.Sent -> if (isDark) ComposeColor(0xFF42D8C8) else ComposeColor(0xFF21AEA2)
        TextAlertDeliveryStatus.Failed -> if (isDark) ComposeColor(0xFFFF7E78) else MaterialTheme.colorScheme.error.copy(alpha = 0.78f)
        TextAlertDeliveryStatus.Skipped -> MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.55f else 0.45f)
    }
}

@Composable
private fun alertDeliveryStatusLabel(status: TextAlertDeliveryStatus): String {
    return when (status) {
        TextAlertDeliveryStatus.Sent -> stringResource(R.string.alert_delivery_status_sent)
        TextAlertDeliveryStatus.Failed -> stringResource(R.string.alert_delivery_status_failed)
        TextAlertDeliveryStatus.Skipped -> stringResource(R.string.alert_delivery_status_skipped)
    }
}

@Composable
private fun PermissionStatusBanner(
    pendingCount: Int,
    onNavigateToSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ComposeColor(0xFFFF8C42).copy(alpha = 0.18f),
                        ComposeColor(0xFFFFB347).copy(alpha = 0.10f),
                    )
                )
            )
            .border(
                width = 1.dp,
                color = ComposeColor(0xFFFF8C42).copy(alpha = 0.30f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { onNavigateToSettings() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ComposeColor(0xFFFF8C42).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = ComposeColor(0xFFFF8C42),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.pending_permissions_attention,
                        pendingCount,
                        pendingCount,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ComposeColor(0xFFFF8C42),
                )
                Text(
                    text = stringResource(R.string.pending_permissions_complete_setup),
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor(0xFFFF8C42).copy(alpha = 0.70f),
                )
            }
        }
    }
}

@Composable
private fun MonitoringPauseCard(
    isPaused: Boolean,
    onTogglePause: (Boolean) -> Unit,
) {
    var showPauseDialog by remember { mutableStateOf(false) }

    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = { showPauseDialog = false },
            title = {
                Text(stringResource(R.string.pause_protection_title))
            },
            text = {
                Text(stringResource(R.string.pause_protection_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showPauseDialog = false
                    onTogglePause(true)
                }) {
                    Text(stringResource(R.string.common_pause))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPauseDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                color = ComposeColor(0xFFFBF7F1),
            )
            .border(
                width = 1.dp,
                color = ComposeColor(0xFFD9CEC4).copy(alpha = 0.66f),
                shape = RoundedCornerShape(28.dp),
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(ComposeColor(0xFFF3EEE7), CircleShape)
                        .border(1.dp, ComposeColor(0xFFE4D9CF), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = if (isPaused) Ember else ComposeColor(0xFF789B88),
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = if (isPaused) {
                            stringResource(R.string.monitoring_paused_title)
                        } else {
                            stringResource(R.string.monitoring_active_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isPaused) {
                            stringResource(R.string.monitoring_paused_body)
                        } else {
                            stringResource(R.string.monitoring_active_body)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Compact toggle button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (isPaused) {
                            onTogglePause(false)
                        } else {
                            showPauseDialog = true
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (isPaused) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            ) {
                Text(
                    text = if (isPaused) stringResource(R.string.common_resume) else stringResource(R.string.common_pause),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPaused) ComposeColor(0xFF31577D) else EmberDeep,
                )
            }
        }
    }
}
@Composable
private fun SosActionsCard(
    modifier: Modifier = Modifier,
    onTriggerSilentSos: () -> Unit,
    onTriggerLoudSos: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.emergency_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                letterSpacing = 4.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f), // Dynamically occupy remaining height!
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ?? Silent SOS ??????????????????????????????????????????????????
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight() // Fill available height flexibly
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(GradientSilent1, GradientSilent2)
                            } else {
                                listOf(GradientSilentLight1, GradientSilentLight2)
                            },
                        )
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ComposeColor.White.copy(alpha = if (isDark) 0.18f else 0.34f),
                                ComposeColor(0xFF20384E).copy(alpha = if (isDark) 0.32f else 0.42f),
                            )
                        ),
                        shape = RoundedCornerShape(30.dp),
                    )
                    .clickable { onTriggerSilentSos() },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(ComposeColor.White.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, ComposeColor.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                            contentDescription = stringResource(R.string.silent_sos_content_description),
                            tint = ComposeColor.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Silent SOS",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 26.sp),
                        fontWeight = FontWeight.Black,
                        color = ComposeColor.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // ?? Loud SOS ????????????????????????????????????????????????????
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight() // Fill available height flexibly
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientSosRed1, GradientSosRed2),
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Ember.copy(alpha = 0.52f),
                        shape = RoundedCornerShape(30.dp),
                    )
                    .clickable { onTriggerLoudSos() },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(ComposeColor.White.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, ComposeColor.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = stringResource(R.string.loud_sos_content_description),
                            tint = ComposeColor.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.loud_sos_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 26.sp),
                        fontWeight = FontWeight.Black,
                        color = ComposeColor.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiTab(
    aiSettings: AiSettings,
    aiDeviceSupport: AiDeviceSupport,
    aiState: AiFeatureUiState,
    onAiSettingsChange: (AiSettings) -> Unit,
    onDownloadModel: () -> Unit,
    onDeleteModel: (GemmaModelType) -> Unit,
    onAudioTest: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AiModelStatusCard(
            aiSettings = aiSettings,
            aiDeviceSupport = aiDeviceSupport,
            onAiSettingsChange = onAiSettingsChange,
            aiState = aiState,
            onDownloadModel = onDownloadModel,
            onDeleteModel = onDeleteModel,
        )
        AiOptionsCard(
            aiSettings = aiSettings,
            aiDeviceSupport = aiDeviceSupport,
            aiState = aiState,
            onAiSettingsChange = onAiSettingsChange,
        )
        AiAudioTestCard(
            aiSettings = aiSettings,
            aiDeviceSupport = aiDeviceSupport,
            aiState = aiState,
            onAudioTest = onAudioTest,
        )
        AiTrainingAudioCard(
            aiSettings = aiSettings,
            onAiSettingsChange = onAiSettingsChange,
        )
        AiDisclaimerCard()
    }
}

@Composable
private fun AiDisclaimerCard() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                ComposeColor(0xFF6F8AA8).copy(alpha = 0.14f),
                ComposeColor(0xFF5F7A96).copy(alpha = 0.08f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Paper,
                ComposeColor(0xFFE7ECF0),
            ),
        )
    }
    val borderColor = if (isDark) {
        ComposeColor(0xFF9BB1C7).copy(alpha = 0.30f)
    } else {
        ComposeColor(0xFF6D8197).copy(alpha = 0.24f)
    }
    val iconBadgeColor = if (isDark) {
        ComposeColor(0xFF9BB1C7).copy(alpha = 0.16f)
    } else {
        ComposeColor(0xFFDDE6ED)
    }
    val accentColor = if (isDark) ComposeColor(0xFFB7C9DA) else ComposeColor(0xFF40566B)
    val bodyColor = if (isDark) {
        Frost.copy(alpha = 0.88f)
    } else {
        Graphite.copy(alpha = 0.96f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(brush = cardBrush)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBadgeColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.ai_disclaimer_title),
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }
            Text(
                text = stringResource(R.string.ai_disclaimer_body),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = bodyColor,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsTab(
    alarmPreferences: AlarmPreferences,
    thresholdDb: Float,
    currentDb: Float,
    contactSettings: AlertContactSettings,
    aiSettings: AiSettings,
    themeMode: AppThemeMode,
    appLanguage: AppLanguageOption,
    bindCode: String,
    movementSettings: MovementSettings,
    currentG: Float,
    telegramContacts: List<TelegramContact>,
    pushContacts: List<PushContact>,
    contactsSyncMessage: String,
    pushSyncMessage: String,
    contactMethodMessage: String,
    trustedPushBindCode: String,
    hasCallPermission: Boolean,
    hasSmsPermission: Boolean,
    // Permission states (moved from Dashboard)
    hasMicPermission: Boolean,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasCompletedQuickSetup: Boolean,
    scrollToTopToken: Long,
    requestedSectionKey: String,
    requestedSectionVersion: Int,
    onRequestPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onThresholdPreviewChange: (Float) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onMovementSettingsPreviewChange: (MovementSettings) -> Unit,
    onMovementSettingsChange: (MovementSettings) -> Unit,
    onAiSettingsChange: (AiSettings) -> Unit,
    onContactSettingsChange: (AlertContactSettings) -> Unit,
    onUseTelegramChange: (Boolean) -> Unit,
    onUsePushChange: (Boolean) -> Unit,
    onUseSmsChange: (Boolean) -> Unit,
    onUsePhoneCallChange: (Boolean) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onAppLanguageChange: (AppLanguageOption) -> Unit,
    onReviewOnboarding: () -> Unit,
    onResumeQuickSetup: () -> Unit,
    onSaveAlertProfile: () -> Unit,
    onRefreshContacts: () -> Unit,
    onDeleteTelegramContact: (TelegramContact) -> Unit,
    onPrepareTelegramSetupLink: (() -> Unit) -> Unit,
    onRefreshPushContacts: () -> Unit,
    onDeletePushContact: (PushContact) -> Unit,
    onTrustedPushBindCodeChange: (String) -> Unit,
    onBindCurrentDeviceToPushCode: () -> Unit,
) {
    val selectedSecurityMode = remember(thresholdDb, movementSettings, aiSettings) {
        currentSecurityModePreset(
            thresholdDb = thresholdDb,
            movementSettings = movementSettings,
            aiSettings = aiSettings,
        )
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopToken) {
        if (scrollToTopToken > 0L) {
            scrollState.scrollTo(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val versionName = BuildConfig.VERSION_NAME.ifBlank { "unknown" }
        // ?? Detection ?????????????????????????????????????????????????????????
        SettingsSectionHeader(stringResource(R.string.settings_section_security_mode))
        SecurityModePresetCard(
            selectedPreset = selectedSecurityMode,
            onPresetSelected = { preset ->
                if (preset == SecurityModePreset.Custom) return@SecurityModePresetCard
                onThresholdChange(preset.alarmThresholdDb)
                onMovementSettingsChange(
                    if (preset.shakeDetectionEnabled) {
                        movementSettings.copy(
                            useShakeDetection = true,
                            shakeThresholdG = preset.movementThresholdG ?: movementSettings.shakeThresholdG,
                        )
                    } else {
                        movementSettings.copy(useShakeDetection = false)
                    }
                )
                onAiSettingsChange(
                    aiSettings.copy(
                        preloadAiInBackground = preset.preloadAiEngine,
                        sosDispatchMode = preset.sosDispatchMode,
                    ),
                )
            },
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsSectionHeader(stringResource(R.string.settings_section_detection))
        ThresholdCard(
            thresholdDb = thresholdDb,
            currentDb = currentDb,
            onThresholdPreviewChange = onThresholdPreviewChange,
            onThresholdChange = onThresholdChange,
        )
        MovementSecurityCard(
            settings = movementSettings,
            currentG = currentG,
            onSettingsPreviewChange = onMovementSettingsPreviewChange,
            onSettingsChange = onMovementSettingsChange,
        )

        // ?? Permissions ???????????????????????????????????????????????????????
        Spacer(modifier = Modifier.height(4.dp))
        SettingsSectionHeader(stringResource(R.string.settings_section_permissions))
        PermissionsSection(
            hasMicPermission = hasMicPermission,
            hasLocationPermission = hasLocationPermission,
            hasCallPermission = hasCallPermission,
            hasSmsPermission = hasSmsPermission,
            hasNotificationPermission = hasNotificationPermission,
            onRequestMicPermission = onRequestPermission,
            onRequestLocationPermission = onRequestLocationPermission,
            onRequestCallPermission = onRequestCallPermission,
            onRequestSmsPermission = onRequestSmsPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
        )

        // ?? Contacts ??????????????????????????????????????????????????????????
        Spacer(modifier = Modifier.height(4.dp))
        SettingsSectionHeader(stringResource(R.string.settings_section_contacts_alerts))
        AlertProfileCard(
            alarmPreferences = alarmPreferences,
            contactSettings = contactSettings,
            onContactSettingsChange = onContactSettingsChange,
            onSaveSettings = onSaveAlertProfile,
        )
        ContactMethodsCard(
            contactSettings = contactSettings,
            bindCode = bindCode,
            telegramContacts = telegramContacts,
            pushContacts = pushContacts,
            contactsSyncMessage = contactsSyncMessage,
            pushSyncMessage = pushSyncMessage,
            hasCallPermission = hasCallPermission,
            hasSmsPermission = hasSmsPermission,
            hasNotificationPermission = hasNotificationPermission,
            isPushAvailable = FirebaseInitializer.isConfigured(),
            contactMethodMessage = contactMethodMessage,
            trustedPushBindCode = trustedPushBindCode,
            requestedSectionKey = requestedSectionKey,
            requestedSectionVersion = requestedSectionVersion,
            onUseTelegramChange = onUseTelegramChange,
            onUsePushChange = onUsePushChange,
            onUseSmsChange = onUseSmsChange,
            onUsePhoneCallChange = onUsePhoneCallChange,
            onPhoneNumberChange = {
                onContactSettingsChange(contactSettings.copy(emergencyPhoneNumber = it))
            },
            onSecondaryPhoneNumberChange = {
                onContactSettingsChange(contactSettings.copy(secondaryPhoneNumber = it))
            },
            onRefreshContacts = onRefreshContacts,
            onDeleteTelegramContact = onDeleteTelegramContact,
            onPrepareTelegramSetupLink = onPrepareTelegramSetupLink,
            onTrustedPushBindCodeChange = onTrustedPushBindCodeChange,
            onRefreshPushContacts = onRefreshPushContacts,
            onDeletePushContact = onDeletePushContact,
            onBindCurrentDeviceToPushCode = onBindCurrentDeviceToPushCode,
        )

        // ?? Appearance ????????????????????????????????????????????????????????
        Spacer(modifier = Modifier.height(4.dp))
        SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
        ThemeModeCard(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppLanguageCard(
            selectedLanguage = appLanguage,
            onLanguageChange = onAppLanguageChange,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GuidedSetupCard(
            hasCompletedQuickSetup = hasCompletedQuickSetup,
            onReviewOnboarding = onReviewOnboarding,
            onResumeQuickSetup = onResumeQuickSetup,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.version_label, versionName),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SecurityModePresetCard(
    selectedPreset: SecurityModePreset,
    onPresetSelected: (SecurityModePreset) -> Unit,
) {
    AuroraCard {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.preset_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SETTINGS_PRESET_ROWS.forEach { rowPresets ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowPresets.forEach { preset ->
                        SecurityModeOption(
                            preset = preset,
                            selected = selectedPreset == preset,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { onPresetSelected(preset) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityModeOption(
    preset: SecurityModePreset,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val selectedFill = if (isDark) ComposeColor(0xFF2D4558) else ComposeColor(0xFF31577D)
    val selectedFillEnd = if (isDark) ComposeColor(0xFF243C4F) else ComposeColor(0xFF496D95)
    val titleColor = if (selected) {
        Frost
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val descriptionColor = if (selected) {
        Frost.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val presetIcon = when (preset) {
        SecurityModePreset.Commute -> Icons.Outlined.DirectionsTransit
        SecurityModePreset.Travel -> Icons.Outlined.Luggage
        SecurityModePreset.Workout -> Icons.Outlined.FitnessCenter
        SecurityModePreset.NightWalk -> Icons.Outlined.Nightlight
        SecurityModePreset.Custom -> Icons.Outlined.Security
    }
    val containerBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(selectedFill, selectedFillEnd),
        )
    } else {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                )
            } else {
                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            },
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (selected) {
                        listOf(
                            Frost.copy(alpha = 0.42f),
                            Frost.copy(alpha = 0.16f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                        )
                    },
                ),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (selected) ComposeColor.White.copy(alpha = 0.13f) else ComposeColor(0xFF8DA394).copy(alpha = 0.26f),
                                CircleShape,
                            )
                            .border(1.dp, ComposeColor.White.copy(alpha = if (selected) 0.22f else 0f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = presetIcon,
                            contentDescription = null,
                            tint = titleColor,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Text(
                        text = preset.label(LocalContext.current),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                }
                Text(
                    text = preset.description(LocalContext.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = descriptionColor,
                )
            }
        }
    }
}

/**
 * A section header with aurora left-accent line for visual grouping.
 */
@Composable
private fun SettingsSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Aurora left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AuroraBlue, AuroraTeal),
                    ),
                    shape = RoundedCornerShape(2.dp),
                )
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            letterSpacing = 1.4.sp,
        )
    }
}

@Composable
private fun GuidedSetupCard(
    hasCompletedQuickSetup: Boolean,
    onReviewOnboarding: () -> Unit,
    onResumeQuickSetup: () -> Unit,
) {
    AuroraCard {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_guided_setup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (hasCompletedQuickSetup) {
                        R.string.settings_guided_setup_body_complete
                    } else {
                        R.string.settings_guided_setup_body_incomplete
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onReviewOnboarding,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(stringResource(R.string.settings_review_onboarding))
            }
            if (!hasCompletedQuickSetup) {
                Button(
                    onClick = onResumeQuickSetup,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(stringResource(R.string.settings_resume_setup))
                }
            }
        }
    }
}

@Composable
private fun AiOptionsCard(
    aiSettings: AiSettings,
    aiDeviceSupport: AiDeviceSupport,
    aiState: AiFeatureUiState,
    onAiSettingsChange: (AiSettings) -> Unit,
) {
    val isAiFeatureAvailable = aiDeviceSupport.supportsOnDeviceAi
    val isAiReady = isAiAnalysisReady(aiSettings, aiState)
    val isAiFirstAvailable = isAiReady && aiSettings.isAiEnabled
    val context = LocalContext.current
    val aiStepDescription = if (!isAiFeatureAvailable) {
        context.getString(R.string.ai_step_description_unavailable)
    } else if (aiSettings.isAiEnabled && isAiReady) {
        context.getString(R.string.ai_step_description_ready)
    } else if (aiSettings.isAiEnabled) {
        context.getString(R.string.ai_step_description_preparing)
    } else {
        context.getString(R.string.ai_step_description_disabled)
    }
    AuroraCard {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ComposeColor(0xFFE9F2ED), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = ComposeColor(0xFF789B88),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.sos_process_title),
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = ComposeColor(0xFF102A43),
                    )
                    Text(
                        text = stringResource(R.string.sos_process_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = ComposeColor(0xFF49627C),
                    )
                }
            }

            SosDispatchModeSelector(
                selectedMode = aiSettings.sosDispatchMode,
                aiFirstAvailable = isAiFirstAvailable && aiSettings.isAiEnabled,
                isAiEnabled = aiSettings.isAiEnabled,
                onModeSelected = { mode ->
                    if (aiSettings.isAiEnabled) {
                        onAiSettingsChange(aiSettings.copy(sosDispatchMode = mode))
                    }
                },
            )

            if (aiSettings.sosDispatchMode == SosDispatchMode.AiFirst) {
                AiProcessStep(
                    stepNumber = 1,
                    label = stringResource(R.string.ai_step_title),
                    description = aiStepDescription,
                    checked = aiSettings.isAiEnabled,
                    onCheckedChange = {},
                    enabled = false,
                    isUnavailable = !isAiFeatureAvailable,
                )
                AiProcessStep(
                    stepNumber = 2,
                    label = stringResource(R.string.sos_alert_title),
                    description = stringResource(R.string.sos_alert_description_verified),
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                )
                AiProcessStep(
                    stepNumber = 3,
                    label = stringResource(R.string.audio_evidence_title),
                    description = stringResource(R.string.audio_evidence_description),
                    checked = aiSettings.sendAudioToContacts,
                    onCheckedChange = { enabled ->
                        onAiSettingsChange(aiSettings.copy(sendAudioToContacts = enabled))
                    },
                    enabled = true,
                    isLast = true,
                )
            } else {
                AiProcessStep(
                    stepNumber = 1,
                    label = stringResource(R.string.sos_alert_title),
                    description = stringResource(R.string.sos_alert_description_instant),
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                )
                AiProcessStep(
                    stepNumber = 2,
                    label = stringResource(R.string.audio_evidence_title),
                    description = stringResource(R.string.audio_evidence_description),
                    checked = aiSettings.sendAudioToContacts,
                    onCheckedChange = { enabled ->
                        onAiSettingsChange(aiSettings.copy(sendAudioToContacts = enabled))
                    },
                    enabled = true,
                )
                AiProcessStep(
                    stepNumber = 3,
                    label = stringResource(R.string.ai_step_title),
                    description = aiStepDescription,
                    checked = aiSettings.isAiEnabled,
                    onCheckedChange = {},
                    enabled = false,
                    isUnavailable = !isAiFeatureAvailable,
                    isLast = true,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ComposeColor(0xFFE9EFE8))
                    .border(1.dp, ComposeColor(0xFFD5DDD2), RoundedCornerShape(22.dp))
                    .padding(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = ComposeColor(0xFF789B88),
                    )
                    Text(
                        text = stringResource(R.string.verified_sos_tradeoff),
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFF536B5F),
                    )
                }
            }
        }
    }
}

@Composable
private fun SosDispatchModeSelector(
    selectedMode: SosDispatchMode,
    aiFirstAvailable: Boolean,
    isAiEnabled: Boolean,
    onModeSelected: (SosDispatchMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(18.dp))
                .background(ComposeColor(0xFFF3EEE7))
                .border(1.dp, ComposeColor(0xFFE2D8CE), RoundedCornerShape(18.dp)),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            SosDispatchModePill(
                icon = Icons.Outlined.FlashOn,
                label = stringResource(R.string.instant_sos_title),
                description = stringResource(R.string.instant_sos_description),
                selected = selectedMode == SosDispatchMode.Immediate,
                enabled = true,
                onClick = { onModeSelected(SosDispatchMode.Immediate) },
            )
            SosDispatchModePill(
                icon = Icons.Outlined.VerifiedUser,
                label = stringResource(R.string.verified_sos_title),
                description = stringResource(R.string.verified_sos_description),
                selected = selectedMode == SosDispatchMode.AiFirst,
                enabled = aiFirstAvailable,
                onClick = { onModeSelected(SosDispatchMode.AiFirst) },
            )
        }
        if (!aiFirstAvailable) {
            Text(
                text = if (isAiEnabled) {
                    stringResource(R.string.verified_sos_preparing_message)
                } else {
                    stringResource(R.string.verified_sos_enable_ai_message)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.SosDispatchModePill(
    icon: ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val displayLabel = label.withBalancedSosPillBreaks()
    val displayDescription = description.withBalancedSosPillBreaks()
    val selectedFill = if (isDark) ComposeColor(0xFF2D4558) else ComposeColor(0xFFFBF7F1)
    val selectedFillEnd = if (isDark) ComposeColor(0xFF243C4F) else ComposeColor(0xFFF7F1EA)
    val titleColor = if (selected) {
        if (isDark) Frost else if (label.contains("Instant", ignoreCase = true)) ComposeColor(0xFFC44235) else ComposeColor(0xFF102A43)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.58f else 0.34f)
    }
    val descriptionColor = if (selected) {
        if (isDark) Frost.copy(alpha = 0.82f) else ComposeColor(0xFF49627C)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.52f else 0.32f)
    }
    val selectedBackground = Brush.linearGradient(
        colors = listOf(selectedFill, selectedFillEnd),
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    selectedBackground
                } else {
                    Brush.linearGradient(
                        colors = listOf(ComposeColor.Transparent, ComposeColor.Transparent),
                    )
                },
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Frost.copy(alpha = 0.42f),
                                Frost.copy(alpha = 0.16f),
                            ),
                        ),
                        shape = RoundedCornerShape(18.dp),
                    )
                } else {
                    Modifier
                },
            )
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(17.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = displayLabel,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall.copy(lineHeight = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = displayDescription,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium.copy(lineHeight = 16.sp),
                    color = descriptionColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun String.withBalancedSosPillBreaks(): String {
    return when (this) {
        "Instant SOS" -> "Instant\nSOS"
        "Quick action" -> "Quick\naction"
        "Verified SOS" -> "Verified\nSOS"
        "Higher accuracy" -> "Higher\naccuracy"
        else -> this
    }
}

@Composable
private fun AiProcessStep(
    stepNumber: Int,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    isUnavailable: Boolean = false,
    isLast: Boolean = false,
) {
    val rowAlpha = if (isUnavailable) 0.5f else 1f
    val bubbleColor = when {
        isUnavailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        checked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val bubbleTextColor = when {
        isUnavailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
        checked -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleColor = when {
        isUnavailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val descriptionColor = if (isUnavailable) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
    ) {
        // Title + Checkbox Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(46.dp)) // 32dp bubble + 14dp spacing
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(lineHeight = 20.sp),
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                modifier = Modifier.weight(1f),
            )
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }

        // Bubble/Line + Description Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Step number bubble + connector line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = bubbleColor,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = bubbleTextColor,
                    )
                }
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(1.dp),
                            ),
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = descriptionColor,
                modifier = Modifier.padding(bottom = if (isLast) 0.dp else 8.dp),
            )
        }
    }
}

@Composable
private fun AiTrainingAudioCard(
    aiSettings: AiSettings,
    onAiSettingsChange: (AiSettings) -> Unit,
) {
    AuroraCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.training_audio_title),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor(0xFF102A43),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(
                    checked = aiSettings.uploadTrainingTriggerAudio,
                    onCheckedChange = { enabled ->
                        onAiSettingsChange(aiSettings.copy(uploadTrainingTriggerAudio = enabled))
                    },
                )
            }

            Text(
                text = stringResource(R.string.training_audio_summary),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = ComposeColor(0xFF49627C),
            )
            Text(
                text = stringResource(R.string.training_audio_description),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = ComposeColor(0xFF49627C),
            )
            Text(
                text = stringResource(R.string.training_audio_privacy_note),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = ComposeColor(0xFF6A7E74),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiModelStatusCard(
    aiSettings: AiSettings,
    aiDeviceSupport: AiDeviceSupport,
    onAiSettingsChange: (AiSettings) -> Unit,
    aiState: AiFeatureUiState,
    onDownloadModel: () -> Unit,
    onDeleteModel: (GemmaModelType) -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<GemmaModelType?>(null) }
    val currentModel = remember(aiSettings.modelType) {
        GemmaModelType.fromName(aiSettings.modelType)
    }
    val isAiSupported = aiDeviceSupport.supportsOnDeviceAi
    val availableModels = remember(isAiSupported, aiDeviceSupport.defaultModelType) {
        if (isAiSupported) {
            GemmaModelType.entries.toList()
        } else {
            listOf(aiDeviceSupport.defaultModelType)
        }
    }
    val unsupportedMessage = stringResource(R.string.ai_step_description_unavailable)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF617A95)),
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ai_audio_detection_title),
                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 30.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(
                    checked = aiSettings.isAiEnabled,
                    onCheckedChange = { enabled ->
                        onAiSettingsChange(
                            aiSettings.copy(
                                isAiEnabled = enabled,
                                enableAiAnalysis = enabled,
                                sosDispatchMode = if (enabled) aiSettings.sosDispatchMode else SosDispatchMode.Immediate
                            )
                        )
                    }
                )
            }
            Text(
                text = stringResource(R.string.ai_audio_detection_setup),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = ComposeColor.White.copy(alpha = 0.90f),
                modifier = Modifier.alpha(if (aiSettings.isAiEnabled) 1f else 0.5f)
            )
            val isAiContentEnabled = aiSettings.isAiEnabled && isAiSupported
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (aiSettings.isAiEnabled) 1f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { shouldExpand ->
                        if (isAiContentEnabled) {
                            expanded = shouldExpand
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = currentModel.title(context),
                        onValueChange = {},
                        readOnly = true,
                        enabled = isAiContentEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableModels.forEachIndexed { index, model ->
                            val isDownloaded = GemmaAudioAnalysisManager.isModelDownloaded(context, model)
                            DropdownMenuItem(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                text = { 
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = model.title(context), fontWeight = FontWeight.Bold)
                                            Text(text = model.description(context), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = model.displaySize,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isAiSupported && isDownloaded) {
                                                IconButton(
                                                    onClick = { modelToDelete = model },
                                                    modifier = Modifier.size(32.dp),
                                                    enabled = isAiContentEnabled
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = stringResource(R.string.delete_model_content_description),
                                                        tint = if (isAiContentEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onAiSettingsChange(aiSettings.copy(modelType = model.name))
                                    expanded = false
                                }
                            )
                            if (index < availableModels.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            if (modelToDelete != null) {
                AlertDialog(
                    onDismissRequest = { modelToDelete = null },
                    title = { Text(stringResource(R.string.delete_model_title)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.delete_model_message,
                                modelToDelete?.title(context).orEmpty(),
                            ),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                modelToDelete?.let { onDeleteModel(it) }
                                modelToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.common_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { modelToDelete = null }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (aiSettings.isAiEnabled) 1f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isAiSupported) {
                        localizeAiStatusMessage(context, aiState.statusMessage)
                    } else {
                        unsupportedMessage
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = ComposeColor.White.copy(alpha = 0.86f),
                )
                Text(
                    text = stringResource(
                        R.string.accelerator_label,
                        if (isAiSupported) {
                            localizeAiAccelerator(context, aiState.accelerator)
                        } else {
                            stringResource(R.string.status_unavailable)
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = ComposeColor.White.copy(alpha = 0.86f),
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = ComposeColor.White.copy(alpha = 0.18f)
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Title + Checkbox Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(46.dp)) // 32dp bubble + 14dp spacing
                        Text(
                            text = stringResource(R.string.preload_ai_engine_title),
                            style = MaterialTheme.typography.titleSmall.copy(lineHeight = 20.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAiSupported) ComposeColor.White
                                    else ComposeColor.White.copy(alpha = 0.52f),
                            modifier = Modifier.weight(1f),
                        )
                        Checkbox(
                            checked = aiSettings.preloadAiInBackground,
                            onCheckedChange = { enabled ->
                                onAiSettingsChange(aiSettings.copy(preloadAiInBackground = enabled))
                            },
                            enabled = isAiSupported && aiSettings.isAiEnabled,
                        )
                    }

                    // Bubble + Description Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (isAiSupported && aiSettings.preloadAiInBackground && aiSettings.isAiEnabled)
                                        ComposeColor(0xFFE9F2ED)
                                    else ComposeColor.White.copy(alpha = 0.14f),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = if (isAiSupported && aiSettings.preloadAiInBackground && aiSettings.isAiEnabled)
                                    ComposeColor(0xFF466B60)
                                else ComposeColor.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.preload_ai_engine_description),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = if (isAiSupported) ComposeColor.White.copy(alpha = 0.86f)
                                    else ComposeColor.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                // Downloading: show determinate progress bar
                if (isAiSupported && aiState.modelStatus == AiModelStatus.Downloading) {
                    LinearProgressIndicator(
                        progress = { (aiState.downloadProgress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.download_progress_label, aiState.downloadProgress),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Initializing: show indeterminate progress with no Download button
                if (isAiSupported && aiState.modelStatus == AiModelStatus.Initializing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.loading_model_engine),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (
                    isAiSupported && (
                        aiState.modelStatus == AiModelStatus.NotDownloaded ||
                        aiState.modelStatus == AiModelStatus.Downloaded ||
                        aiState.modelStatus == AiModelStatus.Error
                    )
                ) {
                    val actionLabel = when {
                        aiState.modelStatus == AiModelStatus.Error &&
                            aiState.statusMessage.startsWith("Model download failed") -> stringResource(R.string.download_model)
                        aiState.modelStatus == AiModelStatus.Error &&
                            aiState.statusMessage.startsWith("Downloaded model is too small") -> stringResource(R.string.download_model)
                        aiState.modelStatus == AiModelStatus.Error -> stringResource(R.string.retry_ai_engine)
                        aiState.modelStatus == AiModelStatus.Downloaded -> stringResource(R.string.initialize_ai_engine)
                        else -> stringResource(R.string.download_model)
                    }
                    Button(
                        onClick = onDownloadModel,
                        enabled = aiSettings.isAiEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(text = actionLabel)
                    }
                }
            }
        }

    }
}
@Composable
private fun AiAudioTestCard(
    aiSettings: AiSettings,
    aiDeviceSupport: AiDeviceSupport,
    aiState: AiFeatureUiState,
    onAudioTest: () -> Unit,
) {
    AuroraCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .alpha(if (aiSettings.isAiEnabled) 1f else 0.5f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AudioTestSection(
                aiSettings = aiSettings,
                aiDeviceSupport = aiDeviceSupport,
                aiState = aiState,
                onAudioTest = onAudioTest,
            )
        }
    }
}

@Composable
private fun AudioTestSection(
    aiSettings: AiSettings,
    aiDeviceSupport: AiDeviceSupport,
    aiState: AiFeatureUiState,
    onAudioTest: () -> Unit,
) {
    val context = LocalContext.current
    var secondsLeft by remember { mutableStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isAiSupported = aiDeviceSupport.supportsOnDeviceAi
    val isBusy = !isAiSupported || aiState.modelStatus == AiModelStatus.Downloading ||
        aiState.modelStatus == AiModelStatus.Initializing
    val actionLabel = when {
        !isAiSupported -> stringResource(R.string.audio_test_action_unavailable)
        aiState.modelStatus == AiModelStatus.NotDownloaded -> stringResource(R.string.audio_test_action_download)
        aiState.modelStatus == AiModelStatus.Downloaded -> stringResource(R.string.audio_test_action_initialize)
        aiState.modelStatus == AiModelStatus.Downloading -> stringResource(R.string.audio_test_action_downloading, aiState.downloadProgress)
        aiState.modelStatus == AiModelStatus.Initializing -> stringResource(R.string.audio_test_action_initializing)
        else -> stringResource(R.string.audio_test_action_start)
    }
    val currentModelTitle = remember(aiState.currentModelType, context) {
        GemmaModelType.fromName(aiState.currentModelType.name).title(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.audio_test_title),
            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (isAiSupported) {
                stringResource(R.string.audio_test_description_supported)
            } else {
                stringResource(R.string.audio_test_description_unsupported)
            },
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            // Recording: show live countdown
            isRecording -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { ((5 - secondsLeft) / 5f).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${secondsLeft}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.recording_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // Analyzing: show spinner row
            aiState.isAnalyzing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(
                        R.string.analyzing_with_model,
                        GemmaModelType.fromName(aiState.currentModelType.name).title(context),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Idle or result: show button
            else -> {
                Button(
                    onClick = {
                        scope.launch {
                            isRecording = true
                            secondsLeft = 5
                            // Tick down while onAudioTest runs asynchronously
                            repeat(5) {
                                delay(1_000)
                                secondsLeft--
                            }
                            isRecording = false
                        }
                        onAudioTest()
                    },
                    enabled = !isBusy && aiSettings.isAiEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Result card shown after the latest analysis result appears
        aiState.lastAnalysisResult?.let { result ->
            val parsedResult = remember(result.content) { parseAnalysisResult(result.content) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.test_result_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = result.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    parsedResult.dangerLevel?.let { level ->
                        DangerLevelBadge(level = level)
                    }
                    if (parsedResult.bodyText.isNotBlank()) {
                        Text(
                            text = parsedResult.bodyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private data class ParsedAnalysisResult(
    val bodyText: String,
    val dangerLevel: String?,
)

private data class DangerLevelVisual(
    val label: String,
    val dotColor: ComposeColor,
    val textColor: ComposeColor,
    val backgroundColor: ComposeColor,
)

private fun parseAnalysisResult(content: String): ParsedAnalysisResult {
    var dangerLevel: String? = null
    val bodyLines = content.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { line ->
            val normalized = line.removePrefix("-").trim()
            if (normalized.startsWith("Danger Level:", ignoreCase = true)) {
                dangerLevel = normalized.substringAfter(":", "").trim().ifBlank { null }
                true
            } else {
                false
            }
        }
        .toList()

    return ParsedAnalysisResult(
        bodyText = bodyLines.joinToString("\n"),
        dangerLevel = dangerLevel,
    )
}

@Composable
private fun rememberDangerLevelVisual(level: String): DangerLevelVisual {
    val normalized = level.trim().lowercase()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current

    return when (normalized) {
        "danger" -> DangerLevelVisual(
            label = context.getString(R.string.danger_level_danger),
            dotColor = ComposeColor(0xFFE24A4A),
            textColor = if (isDark) ComposeColor(0xFFFFD8D8) else ComposeColor(0xFF5A1F24),
            backgroundColor = if (isDark) ComposeColor(0xFF5A1F24) else ComposeColor(0xFFFFD8D8),
        )
        "high" -> DangerLevelVisual(
            label = context.getString(R.string.danger_level_high),
            dotColor = ComposeColor(0xFFFF8A4C),
            textColor = if (isDark) ComposeColor(0xFFFFE0CF) else ComposeColor(0xFF5B3123),
            backgroundColor = if (isDark) ComposeColor(0xFF5B3123) else ComposeColor(0xFFFFE0CF),
        )
        "medium" -> DangerLevelVisual(
            label = context.getString(R.string.danger_level_medium),
            dotColor = ComposeColor(0xFFF2C14E),
            textColor = if (isDark) ComposeColor(0xFFFFF0C9) else ComposeColor(0xFF54461F),
            backgroundColor = if (isDark) ComposeColor(0xFF54461F) else ComposeColor(0xFFFFF0C9),
        )
        else -> DangerLevelVisual(
            label = context.getString(R.string.danger_level_low),
            dotColor = ComposeColor(0xFFFF6E6E),
            textColor = if (isDark) ComposeColor(0xFFFFDCDC) else ComposeColor(0xFF4B2630),
            backgroundColor = if (isDark) ComposeColor(0xFF4B2630) else ComposeColor(0xFFFFDCDC),
        )
    }
}

@Composable
private fun DangerLevelBadge(level: String) {
    val visual = rememberDangerLevelVisual(level)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.danger_level_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(visual.backgroundColor)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(visual.dotColor, CircleShape),
            )
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = visual.textColor,
            )
        }
    }
}

private suspend fun recordTestPcmSamples(durationSeconds: Int = 5): ShortArray =
    withContext(Dispatchers.IO) {
        val sampleRate = 16_000
        val frameSize = 512
        val totalSamples = sampleRate * durationSeconds
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBuffer, frameSize * 4)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        val pcmData = ShortArray(totalSamples)
        var samplesRead = 0
        val frame = ShortArray(frameSize)
        recorder.startRecording()
        try {
            while (samplesRead < totalSamples) {
                val toRead = minOf(frameSize, totalSamples - samplesRead)
                val read = recorder.read(frame, 0, toRead)
                if (read > 0) {
                    System.arraycopy(frame, 0, pcmData, samplesRead, read)
                    samplesRead += read
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
        pcmData
    }

@Composable
private fun ThemeModeCard(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
) {
    val darkModeBackgroundBrush = remember {
        Brush.linearGradient(
            listOf(
                ComposeColor(0xFF1E3348),
                ComposeColor(0xFF173042),
            ),
        )
    }
    val transparentBackgroundBrush = remember {
        Brush.linearGradient(listOf(ComposeColor.Transparent, ComposeColor.Transparent))
    }
    val darkModeBorderBrush = remember {
        Brush.linearGradient(listOf(AuroraBlue.copy(alpha = 0.52f), AuroraTeal.copy(alpha = 0.26f)))
    }
    AuroraCard {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.appearance_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Pill-toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (themeMode == AppThemeMode.Dark) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
                        },
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Light mode pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (themeMode == AppThemeMode.Light)
                                MaterialTheme.colorScheme.surface
                            else
                                ComposeColor.Transparent
                        )
                        .border(
                            width = if (themeMode == AppThemeMode.Light) 1.dp else 0.dp,
                            color = if (themeMode == AppThemeMode.Light) ComposeColor(0x140F1E2D) else ComposeColor.Transparent,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { onThemeModeChange(AppThemeMode.Light) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "\u2600", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(R.string.theme_light),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (themeMode == AppThemeMode.Light) FontWeight.Bold else FontWeight.Normal,
                            color = if (themeMode == AppThemeMode.Light)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Dark mode pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (themeMode == AppThemeMode.Dark)
                                darkModeBackgroundBrush
                            else
                                transparentBackgroundBrush
                        )
                        .border(
                            width = if (themeMode == AppThemeMode.Dark) 1.dp else 0.dp,
                            brush = darkModeBorderBrush,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onThemeModeChange(AppThemeMode.Dark) },
                    contentAlignment = Alignment.Center,
                ) {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                          Text(text = "\uD83C\uDF19", style = MaterialTheme.typography.bodyMedium)
                          Text(
                              text = stringResource(R.string.theme_aurora),
                              style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (themeMode == AppThemeMode.Dark) FontWeight.Bold else FontWeight.Normal,
                            color = if (themeMode == AppThemeMode.Dark) Frost
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLanguageCard(
    selectedLanguage: AppLanguageOption,
    onLanguageChange: (AppLanguageOption) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.language_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.language_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = stringResource(selectedLanguage.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.language_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    AppLanguageOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                expanded = false
                                if (option != selectedLanguage) {
                                    onLanguageChange(option)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactMethodsCard(
    contactSettings: AlertContactSettings,
    bindCode: String,
    telegramContacts: List<TelegramContact>,
    pushContacts: List<PushContact>,
    contactsSyncMessage: String,
    pushSyncMessage: String,
    hasCallPermission: Boolean,
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    isPushAvailable: Boolean,
    contactMethodMessage: String,
    trustedPushBindCode: String,
    requestedSectionKey: String,
    requestedSectionVersion: Int,
    onUseTelegramChange: (Boolean) -> Unit,
    onUsePushChange: (Boolean) -> Unit,
    onUseSmsChange: (Boolean) -> Unit,
    onUsePhoneCallChange: (Boolean) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onSecondaryPhoneNumberChange: (String) -> Unit,
    onRefreshContacts: () -> Unit,
    onDeleteTelegramContact: (TelegramContact) -> Unit,
    onPrepareTelegramSetupLink: (() -> Unit) -> Unit,
    onTrustedPushBindCodeChange: (String) -> Unit,
    onRefreshPushContacts: () -> Unit,
    onDeletePushContact: (PushContact) -> Unit,
    onBindCurrentDeviceToPushCode: () -> Unit,
) {
    var expandedSection by rememberSaveable { mutableStateOf<ContactMethodSection?>(null) }
    val layoutSpec = rememberContactMethodLayoutSpec()
    val contactMethodsCardRequester = remember { BringIntoViewRequester() }
    val pushBindSectionRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(requestedSectionKey, requestedSectionVersion) {
        if (requestedSectionVersion <= 0) return@LaunchedEffect
        when (requestedSectionKey) {
            SETTINGS_SECTION_CONTACTS -> {
                expandedSection = null
                contactMethodsCardRequester.bringIntoView()
            }

            SETTINGS_SECTION_CONTACTS_TELEGRAM -> {
                expandedSection = ContactMethodSection.Telegram
                contactMethodsCardRequester.bringIntoView()
            }

            SETTINGS_SECTION_CONTACTS_PUSH,
            SETTINGS_SECTION_PUSH_BIND -> {
                expandedSection = ContactMethodSection.Push
                contactMethodsCardRequester.bringIntoView()
                pushBindSectionRequester.bringIntoView()
            }

            SETTINGS_SECTION_CONTACTS_SMS -> {
                expandedSection = ContactMethodSection.Sms
                contactMethodsCardRequester.bringIntoView()
            }

            SETTINGS_SECTION_CONTACTS_PHONE -> {
                expandedSection = ContactMethodSection.PhoneCall
                contactMethodsCardRequester.bringIntoView()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(contactMethodsCardRequester),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .padding(layoutSpec.cardPadding)
                .widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(layoutSpec.cardSpacing),
        ) {
            val telegramBoundCount = telegramContacts.count { it.status == TelegramBindingStatus.Bound }
            val pushBoundCount = pushContacts.count { it.status == TelegramBindingStatus.Bound }
            val primaryContactReady = contactSettings.emergencyPhoneNumber.isNotBlank()
            val secondaryContactReady = contactSettings.secondaryPhoneNumber.isNotBlank()

            Text(
                text = stringResource(R.string.contact_methods_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = contactMethodMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ContactMethodToggleRow(
                icon = Icons.AutoMirrored.Outlined.Send,
                label = stringResource(R.string.contact_method_telegram),
                description = stringResource(R.string.contact_method_telegram_description),
                statusSummary = when {
                    !contactSettings.useTelegram -> stringResource(R.string.common_off)
                    telegramBoundCount > 0 -> pluralStringResource(
                        R.plurals.status_contacts_bound,
                        telegramBoundCount,
                        telegramBoundCount,
                    )
                    else -> stringResource(R.string.status_setup_required)
                },
                checked = contactSettings.useTelegram,
                expanded = expandedSection == ContactMethodSection.Telegram,
                onExpandedChange = {
                    expandedSection = if (expandedSection == ContactMethodSection.Telegram) null else ContactMethodSection.Telegram
                },
                onCheckedChange = { enabled ->
                    onUseTelegramChange(enabled)
                    expandedSection = when {
                        enabled -> ContactMethodSection.Telegram
                        expandedSection == ContactMethodSection.Telegram -> null
                        else -> expandedSection
                    }
                },
            )
            ContactMethodSectionContent(visible = expandedSection == ContactMethodSection.Telegram) {
                TelegramManagementContent(
                    contactSettings = contactSettings,
                    bindCode = bindCode,
                    contacts = telegramContacts,
                    contactsSyncMessage = contactsSyncMessage,
                    onRefreshContacts = onRefreshContacts,
                    onDeleteContact = onDeleteTelegramContact,
                    onPrepareSetupLink = onPrepareTelegramSetupLink,
                )
            }

            ContactMethodToggleRow(
                icon = Icons.Outlined.NotificationsActive,
                label = stringResource(R.string.contact_method_push),
                description = if (isPushAvailable) {
                    stringResource(R.string.contact_method_push_description_available)
                } else {
                    stringResource(R.string.contact_method_push_description_unavailable)
                },
                statusSummary = when {
                    !contactSettings.usePush -> stringResource(R.string.common_off)
                    !isPushAvailable -> stringResource(R.string.status_fcm_setup_required)
                    !hasNotificationPermission -> stringResource(R.string.status_permission_required)
                    pushBoundCount > 0 -> pluralStringResource(
                        R.plurals.status_contacts_bound,
                        pushBoundCount,
                        pushBoundCount,
                    )
                    else -> stringResource(R.string.status_setup_required)
                },
                checked = contactSettings.usePush,
                expanded = expandedSection == ContactMethodSection.Push,
                onExpandedChange = {
                    expandedSection = if (expandedSection == ContactMethodSection.Push) null else ContactMethodSection.Push
                },
                onCheckedChange = { enabled ->
                    onUsePushChange(enabled)
                    expandedSection = when {
                        enabled -> ContactMethodSection.Push
                        expandedSection == ContactMethodSection.Push -> null
                        else -> expandedSection
                    }
                },
                enabled = isPushAvailable,
            )
            ContactMethodSectionContent(
                visible = expandedSection == ContactMethodSection.Push,
                modifier = Modifier.bringIntoViewRequester(pushBindSectionRequester),
            ) {
                PushManagementContent(
                    bindCode = bindCode,
                    contacts = pushContacts,
                    pushSyncMessage = pushSyncMessage,
                    trustedPushBindCode = trustedPushBindCode,
                    hasNotificationPermission = hasNotificationPermission,
                    onTrustedPushBindCodeChange = onTrustedPushBindCodeChange,
                    onRefreshContacts = onRefreshPushContacts,
                    onDeleteContact = onDeletePushContact,
                    onBindCurrentDevice = onBindCurrentDeviceToPushCode,
                )
            }

            ContactMethodToggleRow(
                icon = Icons.Outlined.Sms,
                label = stringResource(R.string.contact_method_sms),
                description = stringResource(R.string.contact_method_sms_description),
                statusSummary = when {
                    !contactSettings.useSms -> stringResource(R.string.common_off)
                    !hasSmsPermission -> stringResource(R.string.status_permission_required)
                    !primaryContactReady -> stringResource(R.string.status_number_required)
                    else -> {
                        val smsCount = 1 + if (secondaryContactReady) 1 else 0
                        pluralStringResource(
                            R.plurals.status_numbers_ready,
                            smsCount,
                            smsCount,
                        )
                    }
                },
                checked = contactSettings.useSms,
                expanded = expandedSection == ContactMethodSection.Sms,
                onExpandedChange = {
                    expandedSection = if (expandedSection == ContactMethodSection.Sms) null else ContactMethodSection.Sms
                },
                onCheckedChange = { enabled ->
                    onUseSmsChange(enabled)
                    expandedSection = when {
                        enabled -> ContactMethodSection.Sms
                        expandedSection == ContactMethodSection.Sms -> null
                        else -> expandedSection
                    }
                },
                enabled = hasSmsPermission,
            )
            ContactMethodSectionContent(visible = expandedSection == ContactMethodSection.Sms) {
                PhoneNumberFieldsContent(
                    primaryNumber = contactSettings.emergencyPhoneNumber,
                    secondaryNumber = contactSettings.secondaryPhoneNumber,
                    showSecondary = true,
                    onPrimaryNumberChange = onPhoneNumberChange,
                    onSecondaryNumberChange = onSecondaryPhoneNumberChange,
                )
                Text(
                    text = if (hasSmsPermission) {
                        stringResource(R.string.sms_permission_ready)
                    } else {
                        stringResource(R.string.sms_permission_needed)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ContactMethodToggleRow(
                icon = Icons.Outlined.Phone,
                label = stringResource(R.string.contact_method_phone_call),
                description = stringResource(R.string.contact_method_phone_description),
                statusSummary = when {
                    !contactSettings.usePhoneCall -> stringResource(R.string.common_off)
                    !hasCallPermission -> stringResource(R.string.status_permission_required)
                    !primaryContactReady -> stringResource(R.string.status_number_required)
                    else -> stringResource(R.string.status_primary_number_ready)
                },
                checked = contactSettings.usePhoneCall,
                expanded = expandedSection == ContactMethodSection.PhoneCall,
                onExpandedChange = {
                    expandedSection = if (expandedSection == ContactMethodSection.PhoneCall) null else ContactMethodSection.PhoneCall
                },
                onCheckedChange = { enabled ->
                    onUsePhoneCallChange(enabled)
                    expandedSection = when {
                        enabled -> ContactMethodSection.PhoneCall
                        expandedSection == ContactMethodSection.PhoneCall -> null
                        else -> expandedSection
                    }
                },
                enabled = hasCallPermission,
            )
            ContactMethodSectionContent(visible = expandedSection == ContactMethodSection.PhoneCall) {
                PhoneNumberFieldsContent(
                    primaryNumber = contactSettings.emergencyPhoneNumber,
                    secondaryNumber = contactSettings.secondaryPhoneNumber,
                    showSecondary = false,
                    onPrimaryNumberChange = onPhoneNumberChange,
                    onSecondaryNumberChange = onSecondaryPhoneNumberChange,
                )
                Text(
                    text = if (hasCallPermission) {
                        stringResource(R.string.phone_contact_permission_ready)
                    } else {
                        stringResource(R.string.phone_contact_permission_needed)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class ContactMethodSection {
    Telegram,
    Push,
    Sms,
    PhoneCall,
}

private data class ContactMethodLayoutSpec(
    val cardPadding: Dp,
    val cardSpacing: Dp,
    val sectionPadding: Dp,
    val sectionSpacing: Dp,
    val sectionBottomGap: Dp,
)

@Composable
private fun rememberContactMethodLayoutSpec(): ContactMethodLayoutSpec {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidthDp >= 840 -> ContactMethodLayoutSpec(
            cardPadding = 28.dp,
            cardSpacing = 22.dp,
            sectionPadding = 20.dp,
            sectionSpacing = 18.dp,
            sectionBottomGap = 18.dp,
        )
        screenWidthDp >= 600 -> ContactMethodLayoutSpec(
            cardPadding = 26.dp,
            cardSpacing = 20.dp,
            sectionPadding = 19.dp,
            sectionSpacing = 17.dp,
            sectionBottomGap = 16.dp,
        )
        screenWidthDp <= 360 -> ContactMethodLayoutSpec(
            cardPadding = 20.dp,
            cardSpacing = 16.dp,
            sectionPadding = 16.dp,
            sectionSpacing = 14.dp,
            sectionBottomGap = 12.dp,
        )
        else -> ContactMethodLayoutSpec(
            cardPadding = 24.dp,
            cardSpacing = 18.dp,
            sectionPadding = 18.dp,
            sectionSpacing = 16.dp,
            sectionBottomGap = 14.dp,
        )
    }
}

@Composable
private fun ContactMethodToggleRow(
    icon: ImageVector,
    label: String,
    description: String,
    statusSummary: String = "",
    checked: Boolean,
    expanded: Boolean = false,
    onExpandedChange: () -> Unit = {},
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    dimTextWhenDisabled: Boolean = true,
    showExpandIcon: Boolean = true,
    alignTop: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled || !dimTextWhenDisabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled || !dimTextWhenDisabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled || !dimTextWhenDisabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = statusSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled || !dimTextWhenDisabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showExpandIcon) {
                IconButton(
                    onClick = onExpandedChange,
                    enabled = enabled && checked,
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) {
                            stringResource(R.string.collapse_settings_description, label)
                        } else {
                            stringResource(R.string.expand_settings_description, label)
                        },
                        tint = if (enabled && checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun ContactMethodSectionContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return

    val layoutSpec = rememberContactMethodLayoutSpec()
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val sectionContainerColor = if (isLightSurface) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    } else {
        AuroraBlue.copy(alpha = 0.14f)
    }
    val sectionBorderColor = if (isLightSurface) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
    } else {
        AuroraBlue.copy(alpha = 0.24f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = layoutSpec.sectionBottomGap)
            .background(
                color = sectionContainerColor,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                color = sectionBorderColor,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(layoutSpec.sectionPadding),
        verticalArrangement = Arrangement.spacedBy(layoutSpec.sectionSpacing),
        content = content,
    )
}

@Composable
private fun PhoneNumberFieldsContent(
    primaryNumber: String,
    secondaryNumber: String,
    showSecondary: Boolean,
    onPrimaryNumberChange: (String) -> Unit,
    onSecondaryNumberChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = primaryNumber,
        onValueChange = { input ->
            val digitsOnly = input.filter { it.isDigit() }
            onPrimaryNumberChange(digitsOnly)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.primary_emergency_contact)) },
        placeholder = { Text(stringResource(R.string.primary_emergency_contact_placeholder)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = {
            Text(stringResource(R.string.primary_emergency_contact_help))
        },
    )

    if (showSecondary) {
        OutlinedTextField(
            value = secondaryNumber,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                onSecondaryNumberChange(digitsOnly)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.secondary_emergency_contact)) },
            placeholder = { Text(stringResource(R.string.secondary_emergency_contact_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                Text(stringResource(R.string.secondary_emergency_contact_help))
            },
        )
    }
}
@Composable
private fun CopyableRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.copy_label_description, label),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AlertProfileSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private data class PermissionOverviewItem(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    @param:StringRes val summaryRes: Int,
    val isGranted: Boolean,
)

@Composable
private fun PermissionsSection(
    hasMicPermission: Boolean,
    hasLocationPermission: Boolean,
    hasCallPermission: Boolean,
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    val layoutSpec = rememberContactMethodLayoutSpec()
    val permissionItems = listOf(
        PermissionOverviewItem(R.string.permission_overview_microphone_title, Icons.Outlined.Mic, R.string.permission_overview_microphone_summary, hasMicPermission),
        PermissionOverviewItem(R.string.permission_overview_location_title, Icons.Outlined.LocationOn, R.string.permission_overview_location_summary, hasLocationPermission),
        PermissionOverviewItem(R.string.permission_overview_phone_title, Icons.Outlined.Phone, R.string.permission_overview_phone_summary, hasCallPermission),
        PermissionOverviewItem(R.string.permission_overview_sms_title, Icons.Outlined.Sms, R.string.permission_overview_sms_summary, hasSmsPermission),
        PermissionOverviewItem(R.string.permission_overview_notifications_title, Icons.Outlined.NotificationsActive, R.string.permission_overview_notifications_summary, hasNotificationPermission),
    )
    val grantedItems = permissionItems.filter { it.isGranted }
    val missingItems = permissionItems.filterNot { it.isGranted }
    val allGranted = missingItems.isEmpty()
    var isExpanded by rememberSaveable(allGranted) { mutableStateOf(!allGranted) }
    var showGrantedDetails by rememberSaveable(grantedItems.size, missingItems.size) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(layoutSpec.cardPadding),
            verticalArrangement = Arrangement.spacedBy(layoutSpec.cardSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.permissions_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (allGranted) {
                            stringResource(R.string.permissions_all_granted)
                        } else {
                            pluralStringResource(
                                R.plurals.permissions_attention_needed,
                                missingItems.size,
                                missingItems.size,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isExpanded) {
                            stringResource(R.string.collapse_permissions_description)
                        } else {
                            stringResource(R.string.expand_permissions_description)
                        },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (isExpanded) {
                if (!hasMicPermission) {
                    PermissionCard(
                        hasMicPermission = hasMicPermission,
                        isMonitoring = hasMicPermission,
                        onRequestPermission = onRequestMicPermission,
                    )
                }
                if (!hasLocationPermission) {
                    LocationPermissionCard(
                        hasLocationPermission = hasLocationPermission,
                        onRequestPermission = onRequestLocationPermission,
                    )
                }
                if (!hasCallPermission) {
                    PhoneCallPermissionCard(
                        hasCallPermission = hasCallPermission,
                        onRequestPermission = onRequestCallPermission,
                    )
                }
                if (!hasSmsPermission) {
                    SmsPermissionCard(
                        hasSmsPermission = hasSmsPermission,
                        onRequestPermission = onRequestSmsPermission,
                    )
                }
                if (!hasNotificationPermission) {
                    NotificationPermissionCard(
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestPermission = onRequestNotificationPermission,
                    )
                }

                if (grantedItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGrantedDetails = !showGrantedDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.granted_permissions_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.permissions_ready_count,
                                        grantedItems.size,
                                        grantedItems.size,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    imageVector = if (showGrantedDetails) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        if (showGrantedDetails) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                grantedItems.forEach { item ->
                                    GrantedPermissionRow(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrantedPermissionRow(
    item: PermissionOverviewItem,
) {
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isLightSurface) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
                } else {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = if (isLightSurface) 0.16f else 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.permission_granted),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(item.summaryRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun TelegramManagementContent(
    contactSettings: AlertContactSettings,
    bindCode: String,
    contacts: List<TelegramContact>,
    contactsSyncMessage: String,
    onRefreshContacts: () -> Unit,
    onDeleteContact: (TelegramContact) -> Unit,
    onPrepareSetupLink: (() -> Unit) -> Unit,
) {
    val layoutSpec = rememberContactMethodLayoutSpec()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val configuredBotName = contactSettings.botName
        .removePrefix("@")
        .trim()
        .ifBlank { "aurora_security_alarm_bot" }
    val inviteLink = "https://t.me/$configuredBotName?start=$bindCode"

    Column(
        verticalArrangement = Arrangement.spacedBy(layoutSpec.sectionSpacing),
    ) {
        Text(
            text = stringResource(R.string.share_setup_link_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CopyableRow(
            label = stringResource(R.string.invite_link_label),
            value = inviteLink,
            onCopy = {
                onPrepareSetupLink {
                    clipboardManager.setText(AnnotatedString(inviteLink))
                    Toast.makeText(context, context.getString(R.string.toast_invite_link_copied), Toast.LENGTH_SHORT).show()
                }
            }
        )

        Button(
            onClick = {
                onPrepareSetupLink {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            context.getString(R.string.telegram_share_setup_link_body, inviteLink),
                        )
                    }
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.share_setup_link_chooser_title),
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResource(R.string.share_setup_link_button))
        }

        OutlinedButton(
            onClick = onRefreshContacts,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.refresh_contact_list_button))
        }

        if (contactsSyncMessage.isNotBlank()) {
            Text(
                text = contactsSyncMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (contacts.isEmpty()) {
            Text(
                text = stringResource(R.string.no_telegram_contacts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            contacts.forEach { contact ->
                TelegramContactRow(
                    contact = contact,
                    onDeleteContact = onDeleteContact,
                )
            }
        }
    }
}

@Composable
private fun AlertProfileCard(
    alarmPreferences: AlarmPreferences,
    contactSettings: AlertContactSettings,
    onContactSettingsChange: (AlertContactSettings) -> Unit,
    onSaveSettings: () -> Unit,
) {
    val layoutSpec = rememberContactMethodLayoutSpec()
    var showTemplateDialog by remember { mutableStateOf(false) }
    var tempTemplate by remember { mutableStateOf("") }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val usesDefaultTemplate = remember(alarmPreferences, contactSettings.alertMessageTemplate) {
        alarmPreferences.isAlertMessageTemplateDefault(contactSettings.alertMessageTemplate)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(layoutSpec.cardPadding),
            verticalArrangement = Arrangement.spacedBy(layoutSpec.cardSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.alert_profile_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.alert_profile_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isExpanded) {
                            stringResource(R.string.collapse_alert_profile_description)
                        } else {
                            stringResource(R.string.expand_alert_profile_description)
                        },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                        } else {
                            AuroraBlue.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(20.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                        } else {
                            AuroraBlue.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AlertProfileSummaryRow(
                    stringResource(R.string.alert_profile_summary_name),
                    contactSettings.userName.ifBlank { stringResource(R.string.alert_profile_not_set) },
                )
                AlertProfileSummaryRow(
                    stringResource(R.string.alert_profile_summary_number),
                    contactSettings.mobileNumber.ifBlank { stringResource(R.string.alert_profile_not_set) },
                )
                AlertProfileSummaryRow(
                    stringResource(R.string.alert_profile_summary_device),
                    contactSettings.deviceName.ifBlank { stringResource(R.string.alert_profile_not_set) },
                )
                AlertProfileSummaryRow(
                    stringResource(R.string.alert_profile_summary_template),
                    if (usesDefaultTemplate) {
                        stringResource(R.string.alert_profile_template_default)
                    } else {
                        stringResource(R.string.alert_profile_template_customized)
                    },
                )
            }

            if (isExpanded) {
                OutlinedTextField(
                    value = contactSettings.userName,
                    onValueChange = { onContactSettingsChange(contactSettings.copy(userName = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.my_name_label)) },
                    placeholder = { Text(stringResource(R.string.my_name_label)) },
                    singleLine = true,
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = contactSettings.mobileNumber,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            onContactSettingsChange(contactSettings.copy(mobileNumber = digitsOnly))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text(stringResource(R.string.my_mobile_number_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = {
                            Text(stringResource(R.string.my_number_help))
                        },
                    )
                    Text(
                        text = stringResource(R.string.my_number_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 4.dp)
                            .align(Alignment.TopStart),
                    )
                }
                OutlinedTextField(
                    value = contactSettings.deviceName,
                    onValueChange = { onContactSettingsChange(contactSettings.copy(deviceName = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.device_name_label)) },
                    placeholder = { Text(stringResource(R.string.device_name_placeholder)) },
                    singleLine = true,
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = contactSettings.alertMessageTemplate,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.alert_message_template_label)) },
                        readOnly = true,
                        maxLines = 3,
                        supportingText = {
                            Text(stringResource(R.string.alert_message_template_help))
                        },
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                tempTemplate = contactSettings.alertMessageTemplate
                                showTemplateDialog = true
                            }
                    )
                }
            }

            if (showTemplateDialog) {
                AlertDialog(
                    onDismissRequest = { showTemplateDialog = false },
                    title = { Text(stringResource(R.string.edit_alert_message_template_title)) },
                    text = {
                        OutlinedTextField(
                            value = tempTemplate,
                            onValueChange = { tempTemplate = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.6f),
                            minLines = 6,
                            label = { Text(stringResource(R.string.template_content_label)) }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onContactSettingsChange(contactSettings.copy(alertMessageTemplate = tempTemplate))
                                showTemplateDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.common_save))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showTemplateDialog = false }
                        ) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

            if (isExpanded) {
                Button(
                    onClick = onSaveSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(stringResource(R.string.save_alert_profile_button))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PushManagementContent(
    bindCode: String,
    contacts: List<PushContact>,
    pushSyncMessage: String,
    trustedPushBindCode: String,
    hasNotificationPermission: Boolean,
    onTrustedPushBindCodeChange: (String) -> Unit,
    onRefreshContacts: () -> Unit,
    onDeleteContact: (PushContact) -> Unit,
    onBindCurrentDevice: () -> Unit,
) {
    val layoutSpec = rememberContactMethodLayoutSpec()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val pushSetupLink = remember(bindCode) { buildPushSetupLink(bindCode) }

    Column(
        verticalArrangement = Arrangement.spacedBy(layoutSpec.sectionSpacing),
    ) {
        Text(
            text = stringResource(R.string.share_push_setup_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CopyableRow(
            label = stringResource(R.string.push_setup_code_label),
            value = bindCode,
            onCopy = {
                clipboardManager.setText(AnnotatedString(bindCode))
                Toast.makeText(context, context.getString(R.string.toast_push_setup_code_copied), Toast.LENGTH_SHORT).show()
            },
        )

        Button(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        context.getString(
                            R.string.push_share_setup_link_body,
                            pushSetupLink,
                            bindCode,
                        ),
                    )
                }
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        context.getString(R.string.share_push_setup_link_chooser_title),
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResource(R.string.share_push_setup_link_button))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = stringResource(R.string.receive_push_setup_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = trustedPushBindCode,
            onValueChange = { input ->
                val sanitized = input.uppercase().filter { it.isLetterOrDigit() }
                onTrustedPushBindCodeChange(sanitized)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.trusted_push_code_label)) },
            placeholder = { Text(stringResource(R.string.trusted_push_code_placeholder)) },
            singleLine = true,
            supportingText = {
                Text(
                    if (hasNotificationPermission) {
                        stringResource(R.string.push_bind_supporting_ready)
                    } else {
                        stringResource(R.string.push_bind_notifications_required)
                    }
                )
            },
        )

        Button(
            onClick = onBindCurrentDevice,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResource(R.string.bind_this_device_button))
        }

        OutlinedButton(
            onClick = onRefreshContacts,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.refresh_push_contact_list_button))
        }

        if (pushSyncMessage.isNotBlank()) {
            Text(
                text = pushSyncMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (contacts.isEmpty()) {
            Text(
                text = stringResource(R.string.no_push_contacts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            contacts.forEach { contact ->
                PushContactRow(
                    contact = contact,
                    onDeleteContact = onDeleteContact,
                )
            }
        }
    }
}

@Composable
private fun TelegramContactRow(
    contact: TelegramContact,
    onDeleteContact: (TelegramContact) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = contact.telegramHandle.isNotBlank()) {
                val handle = contact.telegramHandle.removePrefix("@")
                if (handle.isNotBlank()) {
                    uriHandler.openUri("https://t.me/$handle")
                }
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = contact.telegramHandle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = contact.status.label(LocalContext.current),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (contact.status == TelegramBindingStatus.Bound) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.remove_telegram_contact_content_description),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.remove_contact_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.remove_telegram_contact_confirmation,
                        contact.name.ifBlank { stringResource(R.string.contact_fallback_name) },
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteContact(contact)
                    },
                ) {
                    Text(stringResource(R.string.common_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun LocationPermissionCard(
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    PermissionStateCard(
        title = stringResource(R.string.permission_location_card_title),
        description = if (hasLocationPermission) {
            stringResource(R.string.permission_location_card_description_enabled)
        } else {
            stringResource(R.string.permission_location_card_description_disabled)
        },
        isGranted = hasLocationPermission,
        activeLabel = stringResource(R.string.state_enabled),
        inactiveLabel = stringResource(R.string.state_action_needed),
        grantedIcon = Icons.Outlined.LocationOn,
        actionLabel = stringResource(R.string.action_allow_location),
        onAction = onRequestPermission,
    )
}

@Composable
private fun PhoneCallPermissionCard(
    hasCallPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    PermissionStateCard(
        title = stringResource(R.string.permission_phone_card_title),
        description = if (hasCallPermission) {
            stringResource(R.string.permission_phone_card_description_enabled)
        } else {
            stringResource(R.string.permission_phone_card_description_disabled)
        },
        isGranted = hasCallPermission,
        activeLabel = stringResource(R.string.state_active),
        inactiveLabel = stringResource(R.string.state_action_needed),
        grantedIcon = Icons.Outlined.Phone,
        actionLabel = stringResource(R.string.action_allow_direct_calling),
        onAction = onRequestPermission,
    )
}

@Composable
private fun SmsPermissionCard(
    hasSmsPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    PermissionStateCard(
        title = stringResource(R.string.permission_sms_card_title),
        description = if (hasSmsPermission) {
            stringResource(R.string.permission_sms_card_description_enabled)
        } else {
            stringResource(R.string.permission_sms_card_description_disabled)
        },
        isGranted = hasSmsPermission,
        activeLabel = stringResource(R.string.state_enabled),
        inactiveLabel = stringResource(R.string.state_action_needed),
        grantedIcon = Icons.Outlined.Sms,
        actionLabel = stringResource(R.string.action_allow_sms),
        onAction = onRequestPermission,
    )
}

@Composable
private fun NotificationPermissionCard(
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    PermissionStateCard(
        title = stringResource(R.string.permission_notifications_card_title),
        description = if (hasNotificationPermission) {
            stringResource(R.string.permission_notifications_card_description_enabled)
        } else {
            stringResource(R.string.permission_notifications_card_description_disabled)
        },
        isGranted = hasNotificationPermission,
        activeLabel = stringResource(R.string.state_enabled),
        inactiveLabel = stringResource(R.string.state_action_needed),
        grantedIcon = Icons.Outlined.NotificationsActive,
        actionLabel = stringResource(R.string.action_allow_notifications),
        onAction = onRequestPermission,
    )
}

@Composable
private fun PermissionStateCard(
    title: String,
    description: String,
    isGranted: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    grantedIcon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val containerColor = if (isGranted) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val iconContainerColor = if (isGranted) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    }
    val iconTint = if (isGranted) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    val badgeContainerColor = if (isGranted) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    val statusIcon = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber
    val leadingIcon = if (isGranted) grantedIcon else statusIcon

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconContainerColor,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = title,
                                tint = iconTint,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isGranted) "Permission granted" else "Permission required",
                            style = MaterialTheme.typography.labelLarge,
                            color = iconTint,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = badgeContainerColor,
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = if (isGranted) activeLabel else inactiveLabel,
                            tint = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!isGranted) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    modifier: Modifier = Modifier,
    uiState: NoiseAlarmUiState,
    aiSettings: AiSettings,
    aiState: AiFeatureUiState,
    isMonitoringPaused: Boolean,
    onStopAlarm: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isEmergencyActive =
        uiState.alarmTriggered ||
            uiState.activeEmergencyMode != null ||
            uiState.pendingEmergencyMode != null
    val isStartingMonitor = uiState.hasMicPermission && !uiState.isMonitoring && !isEmergencyActive && !isMonitoringPaused
    val isPulseActive = uiState.isMonitoring && !isMonitoringPaused

    // Aurora pulse animation, with rings expanding outward while monitoring
    val infiniteTransition = rememberInfiniteTransition(label = "aurora-pulse")
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
        ), label = "ring1"
    )
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.60f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
        ), label = "ring1s"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, 650, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
        ), label = "ring2"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.60f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, 650, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
        ), label = "ring2s"
    )

    val isCalmProtectionState = !isEmergencyActive && !isMonitoringPaused
    val heroContentColor = if (isDark) ComposeColor(0xFFF6F1EA) else ComposeColor(0xFFFBF7F1)
    val heroSecondaryTextColor = heroContentColor.copy(alpha = 0.90f)

    val context = LocalContext.current
    val aiStatusLabel = heroAiStatusLabel(context, aiSettings, aiState)
    val aiBadgeColors = heroAiBadgeColors(isDark)
    val aiReadinessMessage = when {
        !aiSettings.enableAiAnalysis -> null
        !aiSettings.preloadAiInBackground -> ""
        aiState.modelStatus == AiModelStatus.Downloading || aiState.modelStatus == AiModelStatus.Initializing ->
            stringResource(R.string.ai_readiness_preparing)
        aiState.modelStatus == AiModelStatus.Downloaded ->
            stringResource(R.string.ai_readiness_initialize_first)
        aiState.modelStatus == AiModelStatus.Ready -> ""
        aiState.modelStatus == AiModelStatus.Error -> stringResource(R.string.ai_readiness_unavailable)
        else -> stringResource(R.string.ai_readiness_warmup)
    }
    val statusText = when {
        !uiState.hasMicPermission -> stringResource(R.string.hero_status_permission_required)
        isMonitoringPaused -> stringResource(R.string.hero_status_paused)
        !uiState.isMonitoring -> stringResource(R.string.hero_status_starting)
        uiState.pendingEmergencyMode != null -> stringResource(
            R.string.hero_status_countdown,
            uiState.countdownSecondsRemaining,
        )
        uiState.activeEmergencyMode == EmergencyMode.Silent -> stringResource(R.string.hero_status_silent_active)
        uiState.activeEmergencyMode == EmergencyMode.Loud -> stringResource(R.string.hero_status_loud_active)
        uiState.alarmTriggered -> stringResource(R.string.hero_status_alarm_active)
        uiState.isSilenced && uiState.currentDb >= uiState.thresholdDb -> stringResource(R.string.hero_status_alert_silenced)
        else -> stringResource(R.string.hero_status_protection_active)
    }
    val protectionDotColor = if (isDark) ComposeColor(0xFF7FD0B4) else ComposeColor(0xFF78B7A2)
    val dotColor = when {
        !uiState.hasMicPermission -> ComposeColor(0xFFFF8C42)
        isMonitoringPaused -> ComposeColor(0xFFD3D0CA)
        uiState.pendingEmergencyMode != null || uiState.alarmTriggered || uiState.activeEmergencyMode != null -> Ember
        else -> protectionDotColor
    }
    val isCountdownActive = uiState.pendingEmergencyMode != null
    val isEmergencyLayout = isEmergencyActive
    val emergencyHeadline = when {
        uiState.pendingEmergencyMode == EmergencyMode.Silent -> stringResource(R.string.hero_emergency_silent_countdown)
        uiState.pendingEmergencyMode == EmergencyMode.Loud -> stringResource(R.string.hero_emergency_loud_countdown)
        uiState.activeEmergencyMode == EmergencyMode.Silent -> stringResource(R.string.hero_status_silent_active)
        uiState.activeEmergencyMode == EmergencyMode.Loud -> stringResource(R.string.hero_status_loud_active)
        uiState.alarmTriggered -> stringResource(R.string.hero_status_alarm_active)
        else -> stringResource(R.string.hero_emergency_active)
    }
    val emergencySupportingText = when {
        uiState.pendingEmergencyMode != null -> stringResource(R.string.hero_emergency_cancel_hint)
        uiState.activeEmergencyMode == EmergencyMode.Silent -> stringResource(R.string.hero_emergency_sending_discreetly)
        uiState.alarmTriggered -> stringResource(R.string.hero_emergency_stop_alarm_hint)
        else -> stringResource(R.string.hero_emergency_in_progress)
    }
    val liveDbText = if (isStartingMonitor || isMonitoringPaused) "--" else uiState.currentDb.roundToInt().toString()
    val thresholdDbText = uiState.thresholdDb.roundToInt().toString()

    // Emergency gradient vs paused vs normal aurora gradient
    val heroBrushColors = when {
        isEmergencyActive -> listOf(GradientSosRed1, GradientSosRed2)
        isMonitoringPaused -> listOf(ComposeColor(0xFF4B5258), ComposeColor(0xFF363C42))
        isDark -> listOf(GradientSafeStart, GradientSafeMid, GradientSafeEnd)
        else -> listOf(
            ComposeColor(0xFF8298AE),
            ComposeColor(0xFF6A839E),
            ComposeColor(0xFF55728D),
        )
    }
    val heroBackgroundBrush = remember(heroBrushColors) { Brush.linearGradient(colors = heroBrushColors) }
    val heroBorderBrush = remember(isEmergencyActive) {
        Brush.linearGradient(
            colors = listOf(
                BorderLight.copy(alpha = if (isEmergencyActive) 0.20f else 0.14f),
                BorderLight.copy(alpha = 0.05f),
            ),
        )
    }
    val shieldBrush = remember(isDark, isCalmProtectionState) {
        Brush.linearGradient(
            colors = listOf(
                if (isCalmProtectionState && !isDark) ComposeColor(0xFFF2F5FA).copy(alpha = 0.78f)
                else if (isDark) AuroraBlue.copy(alpha = 0.34f)
                else ComposeColor(0xFFF5F8FB).copy(alpha = 0.28f),
                if (isCalmProtectionState && !isDark) ComposeColor(0xFFD9E0EA).copy(alpha = 0.84f)
                else if (isDark) AuroraTeal.copy(alpha = 0.22f)
                else ComposeColor(0xFFD7E0D8).copy(alpha = 0.22f),
            ),
        )
    }
    val shieldBorderBrush = remember(heroContentColor) {
        Brush.linearGradient(
            colors = listOf(
                heroContentColor.copy(alpha = 0.30f),
                heroContentColor.copy(alpha = 0.10f),
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(brush = heroBackgroundBrush)
            .border(
                width = 1.dp,
                brush = heroBorderBrush,
                shape = RoundedCornerShape(34.dp),
            )
    ) {
        // Ambient aurora glow orbs in background
        Canvas(modifier = Modifier.matchParentSize()) {
                val cx = size.width * 0.78f
                val cy = size.height * 0.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isEmergencyActive) Ember.copy(alpha = 0.24f) else protectionDotColor.copy(alpha = 0.24f),
                        ComposeColor.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = size.width * 0.45f,
                ),
                radius = size.width * 0.45f,
                center = Offset(cx, cy),
            )
            val cx2 = size.width * 0.18f
            val cy2 = size.height * 0.70f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isEmergencyActive) EmberDeep.copy(alpha = 0.18f) else protectionDotColor.copy(alpha = 0.18f),
                        ComposeColor.Transparent,
                    ),
                    center = Offset(cx2, cy2),
                    radius = size.width * 0.38f,
                ),
                radius = size.width * 0.38f,
                center = Offset(cx2, cy2),
            )
        }

        if (isEmergencyLayout) {
            // ?? Emergency Layout ????????????????????????????????????????
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = emergencyHeadline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = heroContentColor,
                )
                if (isCountdownActive) {
                    CountdownOrbitIndicator(
                        secondsRemaining = uiState.countdownSecondsRemaining,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = emergencySupportingText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = heroContentColor.copy(alpha = 0.94f),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(heroContentColor)
                            .clickable { onStopAlarm() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when {
                                uiState.pendingEmergencyMode != null -> stringResource(R.string.hero_action_cancel_alert)
                                uiState.alarmTriggered -> stringResource(R.string.hero_action_stop_alarm)
                                uiState.activeEmergencyMode == EmergencyMode.Silent -> stringResource(R.string.hero_action_stop_sos)
                                else -> stringResource(R.string.hero_action_cancel_alert)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmberDeep,
                        )
                    }
                }
            }
        } else {
            // ?? Normal Layout ???????????????????????????????????????????
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isMonitoringPaused) 0.45f else 1f }
                    .padding(horizontal = 26.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                HeroStatusRow(
                    statusText = statusText,
                    dotColor = dotColor,
                    heroContentColor = heroContentColor,
                    aiStatusLabel = aiStatusLabel,
                    aiBadgeColors = aiBadgeColors,
                )

                HeroPulseCenter(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    isPulseActive = isPulseActive,
                    ring1Alpha = ring1Alpha,
                    ring1Scale = ring1Scale,
                    ring2Alpha = ring2Alpha,
                    ring2Scale = ring2Scale,
                    shieldBrush = shieldBrush,
                    shieldBorderBrush = shieldBorderBrush,
                )

                HeroDbSummary(
                    liveDbText = liveDbText,
                    thresholdDbText = thresholdDbText,
                    heroContentColor = heroContentColor,
                    heroSecondaryTextColor = heroSecondaryTextColor,
                    aiReadinessMessage = aiReadinessMessage,
                    isStartingMonitor = isStartingMonitor,
                )

                // AI readiness message, if any
            }
        }
    }
}

@Composable
private fun HeroStatusRow(
    statusText: String,
    dotColor: ComposeColor,
    heroContentColor: ComposeColor,
    aiStatusLabel: String?,
    aiBadgeColors: HeroAiBadgeColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                drawCircle(
                    color = dotColor.copy(alpha = 0.28f),
                    radius = size.minDimension * 0.50f,
                    center = center,
                )
                drawCircle(
                    color = dotColor,
                    radius = size.minDimension * 0.34f,
                    center = center,
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = statusText,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 23.sp),
                color = heroContentColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            aiStatusLabel?.let { label ->
                Surface(
                    modifier = Modifier.widthIn(min = 94.dp, max = 132.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = aiBadgeColors.container,
                    shadowElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = aiBadgeColors.content,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 14.sp),
                            color = aiBadgeColors.content,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPulseCenter(
    modifier: Modifier = Modifier,
    isPulseActive: Boolean,
    ring1Alpha: Float,
    ring1Scale: Float,
    ring2Alpha: Float,
    ring2Scale: Float,
    shieldBrush: Brush,
    shieldBorderBrush: Brush,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isPulseActive) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseR = 76.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ComposeColor.White.copy(alpha = 0.22f),
                            AuroraBlue.copy(alpha = 0.12f),
                            ComposeColor.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = 118.dp.toPx(),
                    ),
                    radius = 118.dp.toPx(),
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = ComposeColor.White.copy(alpha = ring1Alpha * 0.42f),
                    radius = baseR * (0.86f + ring1Scale * 0.82f),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawCircle(
                    color = ComposeColor.White.copy(alpha = ring2Alpha * 0.30f),
                    radius = baseR * (0.72f + ring2Scale * 0.72f),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    ambientColor = AuroraBlue.copy(alpha = 0.50f),
                    spotColor = AuroraBlue.copy(alpha = 0.35f),
                )
                .background(
                    brush = shieldBrush,
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    brush = shieldBorderBrush,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = ComposeColor(0xFF4B6884).copy(alpha = 0.88f),
                modifier = Modifier.size(58.dp),
            )
        }
    }
}

@Composable
private fun HeroDbSummary(
    liveDbText: String,
    thresholdDbText: String,
    heroContentColor: ComposeColor,
    heroSecondaryTextColor: ComposeColor,
    aiReadinessMessage: String?,
    isStartingMonitor: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = liveDbText,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp, lineHeight = 46.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = heroContentColor,
                )
                Text(
                    text = stringResource(R.string.hero_live_db_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = heroSecondaryTextColor,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(heroContentColor.copy(alpha = 0.22f)),
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = thresholdDbText,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp, lineHeight = 46.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = heroContentColor.copy(alpha = 0.90f),
                )
                Text(
                    text = stringResource(R.string.hero_threshold_db_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = heroSecondaryTextColor,
                )
            }
        }

        if (aiReadinessMessage?.isNotEmpty() == true) {
            Text(
                text = aiReadinessMessage,
                style = MaterialTheme.typography.bodySmall,
                color = heroSecondaryTextColor,
            )
        }
        if (isStartingMonitor) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = heroContentColor.copy(alpha = 0.80f),
                trackColor = heroContentColor.copy(alpha = 0.24f),
            )
        }
    }
}

@Composable
private fun CountdownOrbitIndicator(
    secondsRemaining: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "countdown-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    
    val displayNumber = secondsRemaining.coerceIn(1, 4).toString()
    val contentColor = ComposeColor.White

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Calculate responsive font size based on the available space
        // We use 60% of the smaller dimension as a base for premium visual impact
        val size = if (maxHeight < maxWidth) maxHeight else maxWidth
        val responsiveFontSize = (size.value * 0.65f).sp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(pulseScale)
        ) {
            Text(
                text = displayNumber,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = responsiveFontSize,
                    lineHeight = responsiveFontSize,
                ),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    hasMicPermission: Boolean,
    isMonitoring: Boolean,
    onRequestPermission: () -> Unit,
) {
    PermissionStateCard(
        title = stringResource(R.string.permission_microphone_card_title),
        description = when {
            !hasMicPermission -> stringResource(R.string.permission_microphone_card_description_disabled)
            isMonitoring -> stringResource(R.string.permission_microphone_card_description_monitoring)
            else -> stringResource(R.string.permission_microphone_card_description_starting)
        },
        isGranted = hasMicPermission,
        activeLabel = stringResource(R.string.state_enabled),
        inactiveLabel = stringResource(R.string.state_action_needed),
        grantedIcon = Icons.Outlined.Mic,
        actionLabel = stringResource(R.string.action_allow_microphone),
        onAction = onRequestPermission,
    )
}

@Composable
private fun ThresholdCard(
    thresholdDb: Float,
    currentDb: Float,
    onThresholdPreviewChange: (Float) -> Unit,
    onThresholdChange: (Float) -> Unit,
) {
    var sliderValue by remember { mutableStateOf(thresholdDb) }

    LaunchedEffect(thresholdDb) {
        sliderValue = thresholdDb
    }

    AuroraCard {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.sound_sensitivity_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ComposeColor(0xFF102A43),
                    )
                    Text(
                        text = stringResource(R.string.sound_sensitivity_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ComposeColor(0xFF49627C),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(ComposeColor(0xFFF3EEE7), CircleShape)
                        .border(1.dp, ComposeColor(0xFFE3D8CE), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = ComposeColor(0xFF31577D),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${sliderValue.roundToInt()} dB",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor(0xFF102A43),
                )
                Text(
                    text = stringResource(R.string.live_db_label, currentDb.roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (currentDb >= sliderValue) MaterialTheme.colorScheme.error else ComposeColor(0xFF49627C),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onThresholdPreviewChange(it)
                },
                onValueChangeFinished = {
                    onThresholdChange(sliderValue)
                },
                valueRange = 40f..110f,
            )
        }
    }
}

@Composable
private fun MovementSecurityCard(
    settings: MovementSettings,
    currentG: Float,
    onSettingsPreviewChange: (MovementSettings) -> Unit,
    onSettingsChange: (MovementSettings) -> Unit,
) {
    var sliderValue by remember { mutableStateOf(settings.shakeThresholdG) }

    LaunchedEffect(settings.shakeThresholdG) {
        sliderValue = settings.shakeThresholdG
    }

    AuroraCard {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.motion_sensitivity_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor(0xFF102A43),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(
                    checked = settings.useShakeDetection,
                    onCheckedChange = { onSettingsChange(settings.copy(useShakeDetection = it)) }
                )
            }
            Text(
                text = stringResource(R.string.motion_sensitivity_description),
                style = MaterialTheme.typography.bodyMedium,
                color = ComposeColor(0xFF49627C),
            )

            if (settings.useShakeDetection) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = String.format("%.1f G", sliderValue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ComposeColor(0xFF102A43),
                    )
                    Text(
                        text = stringResource(R.string.live_g_label, currentG),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (currentG >= sliderValue) MaterialTheme.colorScheme.error else ComposeColor(0xFF49627C),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onSettingsPreviewChange(settings.copy(shakeThresholdG = it))
                    },
                    onValueChangeFinished = {
                        onSettingsChange(settings.copy(shakeThresholdG = sliderValue))
                    },
                    valueRange = 1.2f..5.0f,
                )
                
                Text(
                    text = stringResource(R.string.motion_sensitivity_tip),
                    style = MaterialTheme.typography.labelSmall,
                    color = ComposeColor(0xFF6A7E74)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmAppPreview() {
    val context = LocalContext.current
    AlarmTheme {
        SettingsTab(
            alarmPreferences = AlarmPreferences(LocalContext.current),
            thresholdDb = 75f,
            currentDb = 60f,
            contactSettings = AlertContactSettings(
                apiUrl = "https://example.com/api",
                apiToken = "secret",
                botName = "alarm_helper_bot",
                deviceName = "Phone",
                userName = "Alex Chen",
                mobileNumber = "+886 912 345 678",
                alertMessageTemplate = context.getString(R.string.default_alert_message_template),
                useTelegram = true,
                usePush = true,
                useSms = false,
                usePhoneCall = false,
                emergencyPhoneNumber = "",
                secondaryPhoneNumber = "",
            ),
            aiSettings = AiSettings(
                sendAudioToContacts = false,
                enableAiAnalysis = true,
                preloadAiInBackground = false,
            ),
            themeMode = AppThemeMode.Light,
            appLanguage = AppLanguageOption.English,
            bindCode = "AB12CD34EF56",
            movementSettings = MovementSettings(),
            currentG = 1.0f,
            telegramContacts = listOf(
                TelegramContact("1", "Alice", "@alice", TelegramBindingStatus.Bound),
                TelegramContact("2", "Bob", "@bob", TelegramBindingStatus.Pending),
            ),
            pushContacts = listOf(
                PushContact("device-1", "Mia", "Pixel 8", TelegramBindingStatus.Bound),
            ),
            contactsSyncMessage = "Last sync completed successfully.",
            pushSyncMessage = "Push sync complete.",
            contactMethodMessage = "Choose how the app should contact people when an alarm is triggered.",
            trustedPushBindCode = "ZX12AB34CD56",
            hasCallPermission = false,
            hasSmsPermission = false,
            hasMicPermission = true,
            hasLocationPermission = true,
            hasNotificationPermission = true,
            hasCompletedQuickSetup = false,
            scrollToTopToken = 0L,
            requestedSectionKey = "",
            requestedSectionVersion = 0,
            onRequestPermission = {},
            onRequestLocationPermission = {},
            onRequestCallPermission = {},
            onRequestSmsPermission = {},
            onRequestNotificationPermission = {},
            onThresholdPreviewChange = {},
            onThresholdChange = {},
            onMovementSettingsPreviewChange = {},
            onMovementSettingsChange = {},
            onAiSettingsChange = {},
            onContactSettingsChange = {},
            onUseTelegramChange = {},
            onUsePushChange = {},
            onUseSmsChange = {},
            onUsePhoneCallChange = {},
            onThemeModeChange = {},
            onAppLanguageChange = {},
            onReviewOnboarding = {},
            onResumeQuickSetup = {},
            onSaveAlertProfile = {},
            onRefreshContacts = {},
            onDeleteTelegramContact = {},
            onPrepareTelegramSetupLink = { onPrepared -> onPrepared() },
            onRefreshPushContacts = {},
            onDeletePushContact = {},
            onTrustedPushBindCodeChange = {},
            onBindCurrentDeviceToPushCode = {},
        )
    }
}
/**
 * A glassmorphism rationale dialog that explains WHY a permission is needed.
 */
@Composable
private fun PermissionRationaleDialog(
    rationale: PermissionRationale,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AuroraCard(
            modifier = Modifier
                .padding(28.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Branded icon with gradient
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(AuroraBlue.copy(alpha = 0.2f), AuroraTeal.copy(alpha = 0.1f))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = rationale.icon,
                        contentDescription = null,
                        tint = AuroraBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = stringResource(rationale.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(rationale.descriptionRes),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.common_got_it),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Overlay component to manage the display of permission rationales.
 */
@Composable
private fun PermissionRationaleOverlay(
    currentRationale: String?,
    onAccepted: (String) -> Unit,
    onDismissed: () -> Unit,
) {
    if (currentRationale != null) {
        val rationale = PERMISSION_RATIONALES.find { it.permission == currentRationale }
        if (rationale != null) {
            PermissionRationaleDialog(
                rationale = rationale,
                onConfirm = { onAccepted(currentRationale) },
                onDismiss = onDismissed
            )
        }
    }
}

@Composable
private fun TermsOfUseConsentDialog(
    onViewTerms: () -> Unit,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        AuroraCard(
            modifier = Modifier
                .padding(24.dp)
                .wrapContentHeight(),
            accentColors = listOf(
                AuroraBlue.copy(alpha = 0.38f),
                AuroraTeal.copy(alpha = 0.22f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.terms_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                                ComposeColor(0xFF0B1723)
                            } else {
                                ComposeColor(0xFFF8F1E8)
                            },
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(20.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.terms_dialog_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                            lineHeight = 22.sp,
                        )
                    }

                    if (scrollState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ComposeColor.Transparent,
                                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                                                ComposeColor(0xFF0B1723)
                                            } else {
                                                ComposeColor(0xFFF8F1E8)
                                            },
                                        ),
                                    ),
                                )
                                .height(40.dp),
                        )
                    }
                }

                TextButton(
                    onClick = onViewTerms,
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.terms_view_full),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.terms_decline))
                    }

                    Button(
                        onClick = onAgree,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.terms_agree),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PushContactRow(
    contact: PushContact,
    onDeleteContact: (PushContact) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = contact.name.ifBlank { "Aurora contact" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (contact.deviceName.isNotBlank()) {
                    Text(
                        text = contact.deviceName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = contact.status.label(LocalContext.current),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.remove_push_contact_content_description),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.remove_contact_title)) },
            text = {
                Text(stringResource(R.string.remove_push_contact_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteContact(contact)
                    },
                ) {
                    Text(stringResource(R.string.common_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
