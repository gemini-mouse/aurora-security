package app.aurorasecurity.security.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.aurorasecurity.security.AuroraCard
import app.aurorasecurity.security.R
import app.aurorasecurity.security.ui.theme.AuroraBlue
import app.aurorasecurity.security.ui.theme.AuroraTeal

private data class QuickSetupLayoutSpec(
    val compact: Boolean,
    val outerPadding: Dp,
    val sectionSpacing: Dp,
    val cardPadding: Dp,
    val rowPadding: Dp,
    val rowSpacing: Dp,
    val progressHeight: Dp,
    val footerBottomPadding: Dp,
)

@Composable
private fun rememberQuickSetupLayoutSpec(): QuickSetupLayoutSpec {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val compact = configuration.screenWidthDp < 380 || configuration.screenHeightDp < 780
        val shortHeight = configuration.screenHeightDp < 700
        if (compact) {
            QuickSetupLayoutSpec(
                compact = true,
                outerPadding = 12.dp,
                sectionSpacing = 10.dp,
                cardPadding = 16.dp,
                rowPadding = 10.dp,
                rowSpacing = 8.dp,
                progressHeight = 6.dp,
                footerBottomPadding = if (shortHeight) 40.dp else 48.dp,
            )
        } else {
            QuickSetupLayoutSpec(
                compact = false,
                outerPadding = 20.dp,
                sectionSpacing = 16.dp,
                cardPadding = 22.dp,
                rowPadding = 14.dp,
                rowSpacing = 12.dp,
                progressHeight = 8.dp,
                footerBottomPadding = 56.dp,
            )
        }
    }
}

@Composable
internal fun QuickSetupWizardOverlay(
    progress: QuickSetupProgress,
    contactStatuses: List<ContactSetupStatus>,
    hasMicPermission: Boolean,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasCallPermission: Boolean,
    hasSmsPermission: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onOpenContactSetup: (ContactSetupType) -> Unit,
) {
    val layout = rememberQuickSetupLayoutSpec()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val currentStep = progress.currentStep
            val currentStepNumber = currentStep.ordinal + 1
            val progressFraction = currentStepNumber / QuickSetupStep.entries.size.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = layout.outerPadding, vertical = layout.outerPadding),
                verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.quick_setup_action_back),
                        )
                    }
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.quick_setup_action_later))
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.quick_setup_title),
                        style = if (layout.compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.quick_setup_step_label,
                            currentStepNumber,
                            QuickSetupStep.entries.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(layout.progressHeight)
                            .clip(RoundedCornerShape(999.dp)),
                    )
                }

                AuroraCard(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(layout.cardPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        val contentScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 460.dp),
                            verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
                        ) {
                            Column(
                                modifier = Modifier.verticalScroll(contentScrollState),
                                verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
                            ) {
                                Text(
                                    text = stringResource(currentStep.titleRes),
                                    style = if (layout.compact) {
                                        MaterialTheme.typography.headlineSmall
                                    } else {
                                        MaterialTheme.typography.headlineMedium
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(currentStep.bodyRes),
                                    style = if (layout.compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                when (currentStep) {
                                    QuickSetupStep.Permissions -> QuickSetupPermissionsStep(
                                        layout = layout,
                                        hasMicPermission = hasMicPermission,
                                        hasLocationPermission = hasLocationPermission,
                                        hasNotificationPermission = hasNotificationPermission,
                                        hasCallPermission = hasCallPermission,
                                        hasSmsPermission = hasSmsPermission,
                                        onRequestMicPermission = onRequestMicPermission,
                                        onRequestLocationPermission = onRequestLocationPermission,
                                        onRequestNotificationPermission = onRequestNotificationPermission,
                                        onRequestCallPermission = onRequestCallPermission,
                                        onRequestSmsPermission = onRequestSmsPermission,
                                    )

                                    QuickSetupStep.Contacts -> QuickSetupContactsStep(
                                        layout = layout,
                                        contactStatuses = contactStatuses,
                                        onOpenContactSetup = onOpenContactSetup,
                                    )

                                    QuickSetupStep.Test -> QuickSetupTestStep(
                                        layout = layout,
                                    )
                                }
                            }
                        }
                    }
                }

                QuickSetupFooter(
                    modifier = Modifier.fillMaxWidth(),
                    currentStep = currentStep,
                    progress = progress,
                    layout = layout,
                    onNext = onNext,
                    onFinish = onFinish,
                )
                Spacer(modifier = Modifier.height(layout.footerBottomPadding))
            }
        }
    }
}

@Composable
private fun QuickSetupPermissionsStep(
    layout: QuickSetupLayoutSpec,
    hasMicPermission: Boolean,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasCallPermission: Boolean,
    hasSmsPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    onRequestSmsPermission: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.rowSpacing)) {
        PermissionSetupRow(
            layout = layout,
            icon = Icons.Outlined.Mic,
            title = stringResource(R.string.permission_overview_microphone_title),
            isReady = hasMicPermission,
            onAction = onRequestMicPermission,
        )
        PermissionSetupRow(
            layout = layout,
            icon = Icons.Outlined.LocationOn,
            title = stringResource(R.string.permission_overview_location_title),
            isReady = hasLocationPermission,
            onAction = onRequestLocationPermission,
        )
        PermissionSetupRow(
            layout = layout,
            icon = Icons.Outlined.NotificationsActive,
            title = stringResource(R.string.permission_overview_notifications_title),
            isReady = hasNotificationPermission,
            onAction = onRequestNotificationPermission,
        )
        PermissionSetupRow(
            layout = layout,
            icon = Icons.Outlined.Phone,
            title = stringResource(R.string.permission_overview_phone_title),
            isReady = hasCallPermission,
            onAction = onRequestCallPermission,
        )
        PermissionSetupRow(
            layout = layout,
            icon = Icons.Outlined.Sms,
            title = stringResource(R.string.permission_overview_sms_title),
            isReady = hasSmsPermission,
            onAction = onRequestSmsPermission,
        )
    }
}

@Composable
private fun PermissionSetupRow(
    layout: QuickSetupLayoutSpec,
    icon: ImageVector,
    title: String,
    isReady: Boolean,
    onAction: () -> Unit,
) {
    val rowCorner = if (layout.compact) 14.dp else 16.dp
    val rowHorizontalPadding = if (layout.compact) 10.dp else 12.dp
    val rowVerticalPadding = if (layout.compact) 8.dp else 10.dp
    val iconContainerSize = if (layout.compact) 30.dp else 34.dp
    val iconSize = if (layout.compact) 16.dp else 18.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(rowCorner))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        AuroraBlue.copy(alpha = 0.18f),
                        AuroraTeal.copy(alpha = 0.10f),
                    ),
                ),
                shape = RoundedCornerShape(rowCorner),
            )
            .padding(horizontal = rowHorizontalPadding, vertical = rowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(iconContainerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isReady) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.quick_setup_status_ready),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                Text(stringResource(R.string.quick_setup_action_allow))
            }
        }
    }
}

@Composable
private fun QuickSetupContactsStep(
    layout: QuickSetupLayoutSpec,
    contactStatuses: List<ContactSetupStatus>,
    onOpenContactSetup: (ContactSetupType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.rowSpacing)) {
        contactStatuses.forEach { status ->
            val icon = when (status.type) {
                ContactSetupType.Line -> Icons.AutoMirrored.Outlined.Chat
                ContactSetupType.Push -> Icons.Outlined.NotificationsActive
                ContactSetupType.Telegram -> Icons.AutoMirrored.Outlined.Send
                ContactSetupType.Sms -> Icons.Outlined.Sms
                ContactSetupType.PhoneCall -> Icons.Outlined.Phone
            }
            SetupStatusRow(
                layout = layout,
                icon = icon,
                title = stringResource(status.type.titleRes),
                subtitle = stringResource(status.type.bodyRes),
                status = status.summary,
                isReady = status.isReady,
                actionLabel = stringResource(R.string.quick_setup_action_open_settings),
                onAction = { onOpenContactSetup(status.type) },
            )
        }
    }
}

@Composable
private fun QuickSetupTestStep(
    layout: QuickSetupLayoutSpec,
) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.rowSpacing)) {
        QuickSetupInstructionRow(
            number = 1,
            text = stringResource(R.string.quick_setup_test_tip_prepare),
        )
        QuickSetupInstructionRow(
            number = 2,
            text = stringResource(R.string.quick_setup_test_tip_trigger),
        )
        QuickSetupInstructionRow(
            number = 3,
            text = stringResource(R.string.quick_setup_test_tip_confirm),
        )
    }
}

@Composable
private fun QuickSetupFooter(
    modifier: Modifier = Modifier,
    currentStep: QuickSetupStep,
    progress: QuickSetupProgress,
    layout: QuickSetupLayoutSpec,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val canAdvance = progress.isCurrentStepReady()
    val canFinish = progress.canFinishCoreSetup()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (currentStep == QuickSetupStep.Contacts) {
                Text(
                    text = stringResource(R.string.quick_setup_contacts_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = if (currentStep == QuickSetupStep.Test) onFinish else onNext,
                enabled = if (currentStep == QuickSetupStep.Test) canFinish else canAdvance,
                shape = RoundedCornerShape(if (layout.compact) 16.dp else 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    if (currentStep == QuickSetupStep.Test) {
                        stringResource(R.string.quick_setup_action_finish)
                    } else {
                        stringResource(R.string.quick_setup_action_next)
                    },
                )
            }
        }
    }
}

@Composable
private fun SetupStatusRow(
    layout: QuickSetupLayoutSpec,
    icon: ImageVector,
    title: String,
    status: String,
    isReady: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    subtitle: String? = null,
) {
    val useStackedTrailingAction = layout.compact
    val badgeContainer = if (isReady) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        ComposeColor(0xFFFF8C42).copy(alpha = 0.14f)
    }
    val badgeContent = if (isReady) {
        MaterialTheme.colorScheme.primary
    } else {
        ComposeColor(0xFFFF8C42)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        AuroraBlue.copy(alpha = 0.20f),
                        AuroraTeal.copy(alpha = 0.10f),
                    ),
                ),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(layout.rowPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (useStackedTrailingAction) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!useStackedTrailingAction) {
                    if (isReady) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(badgeContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeContent,
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onAction,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(actionLabel)
                        }
                    }
                }
            }
            if (useStackedTrailingAction) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (isReady) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(badgeContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeContent,
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onAction,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(actionLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSetupInstructionRow(
    number: Int,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
