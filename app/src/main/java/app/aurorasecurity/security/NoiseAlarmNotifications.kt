package app.aurorasecurity.security

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

internal const val MONITOR_CHANNEL_ID = "noise_alarm_monitor"
internal const val CALL_CHANNEL_ID = "noise_alarm_call_backup_v2"
internal const val MONITOR_NOTIFICATION_ID = 1001
internal const val CALL_FALLBACK_NOTIFICATION_ID = 1002

internal enum class NoiseAlarmNotificationState {
    Starting,
    Listening,
    Pending,
    SilentSos,
    Alarm,
}

internal fun NoiseAlarmUiState.toNoiseAlarmNotificationState(): NoiseAlarmNotificationState {
    return when {
        pendingEmergencyMode != null -> NoiseAlarmNotificationState.Pending
        activeEmergencyMode == EmergencyMode.Silent -> NoiseAlarmNotificationState.SilentSos
        alarmTriggered -> NoiseAlarmNotificationState.Alarm
        isMonitoring -> NoiseAlarmNotificationState.Listening
        else -> NoiseAlarmNotificationState.Starting
    }
}

internal fun buildNoiseAlarmNotificationKey(state: NoiseAlarmUiState): String {
    val notificationState = state.toNoiseAlarmNotificationState()
    return when (notificationState) {
        NoiseAlarmNotificationState.Pending -> "Pending-${state.countdownSecondsRemaining}"
        else -> notificationState.name
    }
}

internal class NoiseAlarmNotificationFactory(
    private val context: Context,
) {
    fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val monitorChannel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            "Noise Alarm Monitor",
            NotificationManager.IMPORTANCE_LOW,
        )
        val callChannel = NotificationChannel(
            CALL_CHANNEL_ID,
            "Emergency Call Backup",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Urgent notification that provides a one-tap emergency call action."
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = CALL_VIBRATION_PATTERN
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        notificationManager.createNotificationChannel(monitorChannel)
        notificationManager.createNotificationChannel(callChannel)
    }

    fun buildServiceNotification(state: NoiseAlarmUiState): Notification {
        val contentText = when (state.toNoiseAlarmNotificationState()) {
            NoiseAlarmNotificationState.Alarm ->
                context.getString(R.string.notification_state_alarm)
            NoiseAlarmNotificationState.Listening ->
                context.getString(R.string.notification_state_listening)
            NoiseAlarmNotificationState.Pending ->
                context.getString(
                    R.string.notification_state_pending,
                    state.countdownSecondsRemaining.coerceAtLeast(1),
                )
            NoiseAlarmNotificationState.SilentSos ->
                context.getString(R.string.notification_state_silent_sos)
            NoiseAlarmNotificationState.Starting ->
                context.getString(R.string.notification_state_starting)
        }

        return NotificationCompat.Builder(context, MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_service_title))
            .setContentText(contentText)
            .setContentIntent(createOpenAppPendingIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildCallFallbackNotification(phoneNumber: String): Notification {
        val fullScreenCallPendingIntent = PendingIntent.getActivity(
            context,
            0,
            createCallIntent(phoneNumber),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openAppIntent = createOpenAppPendingIntent()
        val dismissIntent = PendingIntent.getService(
            context,
            2,
            Intent(context, NoiseAlarmService::class.java).setAction(NoiseAlarmService.ACTION_SILENCE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
        val callPendingIntent = PendingIntent.getActivity(
            context,
            3,
            createCallIntent(phoneNumber),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val caller = Person.Builder()
                .setName(context.getString(R.string.notification_emergency_contact))
                .build()

            return Notification.Builder(context, CALL_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(context.getString(R.string.notification_call_ready_title))
                .setContentText(context.getString(R.string.notification_call_ready_body))
                .setCategory(Notification.CATEGORY_CALL)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenCallPendingIntent, true)
                .setContentIntent(openAppIntent)
                .setStyle(
                    Notification.CallStyle.forIncomingCall(
                        caller,
                        dismissIntent,
                        callPendingIntent,
                    ),
                )
                .build()
        }

        return NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(context.getString(R.string.notification_call_ready_title))
            .setContentText(
                if (hasCallPermission) {
                    context.getString(R.string.notification_call_ready_body_direct)
                } else {
                    context.getString(R.string.notification_call_ready_body_dialer)
                },
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_call_ready_big_text)),
            )
            .setContentIntent(openAppIntent)
            .setFullScreenIntent(fullScreenCallPendingIntent, true)
            .addAction(0, context.getString(R.string.notification_call_now), callPendingIntent)
            .addAction(0, context.getString(R.string.notification_dismiss), dismissIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setVibrate(CALL_VIBRATION_PATTERN)
            .build()
    }

    fun resolveForegroundServiceType(hasLocationPermission: Boolean): Int {
        return if (hasLocationPermission) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
    }

    private fun createCallIntent(phoneNumber: String): Intent {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
        return Intent(
            if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.parse("tel:${Uri.encode(phoneNumber)}"),
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        val CALL_VIBRATION_PATTERN = longArrayOf(0, 900, 250, 900, 250, 1200)
    }
}
