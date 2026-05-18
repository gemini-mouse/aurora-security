package app.aurorasecurity.security.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.aurorasecurity.security.AuroraCard
import app.aurorasecurity.security.R
import app.aurorasecurity.security.ui.theme.AuroraBlue
import app.aurorasecurity.security.ui.theme.AuroraTeal
import kotlinx.coroutines.launch

private data class OnboardingLayoutSpec(
    val compact: Boolean,
    val outerPadding: Dp,
    val pageTopPadding: Dp,
    val cardPadding: Dp,
    val sectionSpacing: Dp,
    val itemSpacing: Dp,
    val heroSize: Dp,
    val heroInnerSize: Dp,
    val heroIconSize: Dp,
    val indicatorSize: Dp,
    val footerBottomPadding: Dp,
    val footerReservedHeight: Dp,
)

@Composable
private fun rememberOnboardingLayoutSpec(): OnboardingLayoutSpec {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val compact = configuration.screenWidthDp < 380 || configuration.screenHeightDp < 760
        val shortHeight = configuration.screenHeightDp < 700
        if (compact) {
            OnboardingLayoutSpec(
                compact = true,
                outerPadding = 12.dp,
                pageTopPadding = 6.dp,
                cardPadding = 16.dp,
                sectionSpacing = 10.dp,
                itemSpacing = 8.dp,
                heroSize = 80.dp,
                heroInnerSize = 42.dp,
                heroIconSize = 20.dp,
                indicatorSize = 8.dp,
                footerBottomPadding = if (shortHeight) 40.dp else 48.dp,
                footerReservedHeight = if (shortHeight) 104.dp else 116.dp,
            )
        } else {
            OnboardingLayoutSpec(
                compact = false,
                outerPadding = 20.dp,
                pageTopPadding = 12.dp,
                cardPadding = 24.dp,
                sectionSpacing = 16.dp,
                itemSpacing = 10.dp,
                heroSize = 112.dp,
                heroInnerSize = 58.dp,
                heroIconSize = 26.dp,
                indicatorSize = 10.dp,
                footerBottomPadding = 56.dp,
                footerReservedHeight = 132.dp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OnboardingFlowOverlay(
    onSkip: () -> Unit,
    onStartSetup: () -> Unit,
) {
    val pages = OnboardingPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val layout = rememberOnboardingLayoutSpec()

    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = layout.outerPadding, vertical = layout.outerPadding),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(onClick = onSkip) {
                            Text(stringResource(R.string.onboarding_action_skip))
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = layout.footerReservedHeight),
                    ) { pageIndex ->
                        val page = pages[pageIndex]
                        OnboardingPageContent(page = page, layout = layout)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(pages.size) { index ->
                                val isActive = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .size(
                                            if (isActive) {
                                                layout.indicatorSize + 2.dp
                                            } else {
                                                layout.indicatorSize
                                            },
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                            },
                                        ),
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val nextPage = pagerState.currentPage + 1
                                if (nextPage >= pages.size) {
                                    onStartSetup()
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(nextPage) }
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(
                                text = if (pagerState.currentPage == pages.lastIndex) {
                                    stringResource(R.string.onboarding_action_start_setup)
                                } else {
                                    stringResource(R.string.onboarding_action_next)
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(layout.footerBottomPadding))
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    layout: OnboardingLayoutSpec,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = layout.pageTopPadding),
    ) {
        AuroraCard(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(layout.cardPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
                    ) {
                        OnboardingHeroIllustration(page = page, layout = layout)
                        Text(
                            text = stringResource(page.titleRes),
                            style = if (layout.compact) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.headlineSmall
                            },
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(page.bodyRes),
                            style = if (layout.compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(layout.sectionSpacing))

                    when (page) {
                        OnboardingPage.Welcome -> OnboardingWelcomeHighlights(layout = layout)
                        OnboardingPage.AutoTrigger -> OnboardingAutoTriggerHighlights(layout = layout)
                        OnboardingPage.SosModes -> OnboardingSosHighlights(layout = layout)
                        OnboardingPage.AiDetection -> OnboardingAiHighlights(layout = layout)
                        OnboardingPage.SetupChecklist -> OnboardingChecklistHighlights(layout = layout)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeroIllustration(
    page: OnboardingPage,
    layout: OnboardingLayoutSpec,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val icon = when (page) {
        OnboardingPage.Welcome -> Icons.Outlined.Security
        OnboardingPage.AutoTrigger -> Icons.Outlined.GraphicEq
        OnboardingPage.SosModes -> Icons.Outlined.NotificationsActive
        OnboardingPage.AiDetection -> Icons.Outlined.AutoAwesome
        OnboardingPage.SetupChecklist -> Icons.Outlined.CheckCircle
    }
    val gradient = if (isDark) {
        Brush.linearGradient(listOf(ComposeColor(0xFF20384E), ComposeColor(0xFF143146)))
    } else {
        Brush.linearGradient(listOf(AuroraBlue.copy(alpha = 0.20f), AuroraTeal.copy(alpha = 0.12f)))
    }

    Box(
        modifier = Modifier
            .size(layout.heroSize)
            .clip(RoundedCornerShape(if (layout.compact) 28.dp else 32.dp))
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(layout.heroInnerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(layout.heroIconSize),
            )
        }
    }
}

@Composable
private fun OnboardingWelcomeHighlights(layout: OnboardingLayoutSpec) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
    ) {
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_welcome_chip_detection))
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_welcome_chip_sos))
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_welcome_chip_contacts))
    }
}

@Composable
private fun OnboardingSosHighlights(layout: OnboardingLayoutSpec) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
    ) {
        OnboardingInfoCard(
            title = stringResource(R.string.onboarding_silent_sos_title),
            body = stringResource(R.string.onboarding_silent_sos_body),
            compact = layout.compact,
        )
        OnboardingInfoCard(
            title = stringResource(R.string.onboarding_loud_sos_title),
            body = stringResource(R.string.onboarding_loud_sos_body),
            compact = layout.compact,
        )
    }
}

@Composable
private fun OnboardingAutoTriggerHighlights(layout: OnboardingLayoutSpec) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
    ) {
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_auto_trigger_item_listen))
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_auto_trigger_item_threshold))
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_auto_trigger_item_hands_free))
        Text(
            text = stringResource(R.string.onboarding_auto_trigger_footer),
            style = if (layout.compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingAiHighlights(layout: OnboardingLayoutSpec) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
    ) {
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_ai_benefit_detection))
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_ai_benefit_context))
        OnboardingFeatureRow(text = stringResource(R.string.onboarding_ai_benefit_false_alarm))
        Text(
            text = stringResource(R.string.onboarding_ai_footer),
            style = if (layout.compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingChecklistHighlights(layout: OnboardingLayoutSpec) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
    ) {
        OnboardingChecklistRow(text = stringResource(R.string.onboarding_checklist_item_permissions))
        OnboardingChecklistRow(text = stringResource(R.string.onboarding_checklist_item_contacts))
        OnboardingChecklistRow(text = stringResource(R.string.onboarding_checklist_item_test))
    }
}

@Composable
private fun OnboardingFeatureRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OnboardingInfoCard(
    title: String,
    body: String,
    compact: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OnboardingChecklistRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
