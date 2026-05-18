package app.aurorasecurity.security.onboarding

import androidx.annotation.StringRes
import app.aurorasecurity.security.R

internal enum class OnboardingPage(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
) {
    Welcome(
        titleRes = R.string.onboarding_welcome_title,
        bodyRes = R.string.onboarding_welcome_body,
    ),
    SosModes(
        titleRes = R.string.onboarding_sos_title,
        bodyRes = R.string.onboarding_sos_body,
    ),
    AutoTrigger(
        titleRes = R.string.onboarding_auto_trigger_title,
        bodyRes = R.string.onboarding_auto_trigger_body,
    ),
    AiDetection(
        titleRes = R.string.onboarding_ai_title,
        bodyRes = R.string.onboarding_ai_body,
    ),
    SetupChecklist(
        titleRes = R.string.onboarding_checklist_title,
        bodyRes = R.string.onboarding_checklist_body,
    ),
}

internal enum class QuickSetupStep(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
) {
    Permissions(
        titleRes = R.string.quick_setup_permissions_title,
        bodyRes = R.string.quick_setup_permissions_body,
    ),
    Contacts(
        titleRes = R.string.quick_setup_contacts_title,
        bodyRes = R.string.quick_setup_contacts_body,
    ),
    Test(
        titleRes = R.string.quick_setup_test_title,
        bodyRes = R.string.quick_setup_test_body,
    );

    fun next(): QuickSetupStep? {
        val nextIndex = ordinal + 1
        return entries.getOrNull(nextIndex)
    }

    fun previous(): QuickSetupStep? {
        val previousIndex = ordinal - 1
        return entries.getOrNull(previousIndex)
    }
}

internal enum class ContactSetupType(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
) {
    Push(
        titleRes = R.string.quick_setup_contact_push_title,
        bodyRes = R.string.quick_setup_contact_push_body,
    ),
    Telegram(
        titleRes = R.string.quick_setup_contact_telegram_title,
        bodyRes = R.string.quick_setup_contact_telegram_body,
    ),
    Sms(
        titleRes = R.string.quick_setup_contact_sms_title,
        bodyRes = R.string.quick_setup_contact_sms_body,
    ),
    PhoneCall(
        titleRes = R.string.quick_setup_contact_phone_title,
        bodyRes = R.string.quick_setup_contact_phone_body,
    ),
}

internal data class ContactSetupStatus(
    val type: ContactSetupType,
    val isRecommended: Boolean,
    val isReady: Boolean,
    val summary: String,
)

internal data class QuickSetupProgress(
    val currentStep: QuickSetupStep,
    val permissionsReady: Boolean,
    val contactsReady: Boolean,
    val testComplete: Boolean,
) {
    fun isCurrentStepReady(): Boolean {
        return when (currentStep) {
            QuickSetupStep.Permissions -> permissionsReady
            QuickSetupStep.Contacts -> contactsReady
            QuickSetupStep.Test -> true
        }
    }

    fun canFinishCoreSetup(): Boolean {
        return permissionsReady && contactsReady
    }

    fun completedStepCount(): Int {
        var count = 0
        if (permissionsReady) count += 1
        if (contactsReady) count += 1
        if (testComplete) count += 1
        return count
    }
}
