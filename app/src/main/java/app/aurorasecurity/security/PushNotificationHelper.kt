package app.aurorasecurity.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

object PushNotificationHelper {
    const val STANDARD_CHANNEL_ID = "aurora_trusted_contact_sos_alerts"
    const val SOS_RINGTONE_CHANNEL_ID = "aurora_trusted_contact_sos_ringtone_alerts"
    private const val GROUP_KEY_INCOMING_PUSH = "app.aurorasecurity.security.group.INCOMING_PUSH"
    private const val GROUP_SUMMARY_NOTIFICATION_ID = 3000
    const val EXTRA_OPEN_HISTORY = "app.aurorasecurity.security.extra.OPEN_HISTORY"
    const val EXTRA_HISTORY_FILTER = "app.aurorasecurity.security.extra.HISTORY_FILTER"
    const val HISTORY_FILTER_INCOMING = "incoming"
    const val FCM_DATA_CHANNEL_KEY = "channel"
    const val FCM_DATA_CHANNEL_AURORA_PUSH = "aurora_push"
    private const val ACTION_OPEN_INCOMING_HISTORY =
        "app.aurorasecurity.security.action.OPEN_INCOMING_HISTORY"
    private val vibrationPattern = longArrayOf(0L, 450L, 120L, 450L, 120L, 650L)
    private val nextNotificationId = AtomicInteger(
        ((System.currentTimeMillis() and 0x0FFFFFFF).toInt()).coerceAtLeast(3001),
    )

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val standardChannel = NotificationChannel(
            STANDARD_CHANNEL_ID,
            context.getString(R.string.push_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.push_notification_channel_description)
            enableVibration(true)
            setVibrationPattern(vibrationPattern)
            enableLights(true)
            lightColor = Color.RED
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationSoundUri()?.let { soundUri ->
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }

        val sosRingtoneChannel = NotificationChannel(
            SOS_RINGTONE_CHANNEL_ID,
            context.getString(R.string.push_sos_ringtone_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.push_sos_ringtone_channel_description)
            enableVibration(true)
            setVibrationPattern(vibrationPattern)
            enableLights(true)
            lightColor = Color.RED
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            ringtoneSoundUri()?.let { soundUri ->
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }

        manager.createNotificationChannel(standardChannel)
        manager.createNotificationChannel(sosRingtoneChannel)
    }

    fun showEmergencyAlert(
        context: Context,
        title: String,
        body: String,
        eventId: String,
        messageType: String,
    ) {
        createNotificationChannels(context)
        val notificationId = nextNotificationId.getAndIncrement()
        val isSosAlert = messageType.equals("sos", ignoreCase = true)
        val channelId = if (isSosAlert) SOS_RINGTONE_CHANNEL_ID else STANDARD_CHANNEL_ID

        val openIntent = PendingIntent.getActivity(
            context,
            notificationId,
            createOpenIncomingHistoryIntent(
                context = context,
                notificationId = notificationId,
                eventId = eventId,
                messageType = messageType,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(GROUP_KEY_INCOMING_PUSH)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setLights(Color.RED, 800, 400)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .apply {
                if (isSosAlert) {
                    setSound(ringtoneSoundUri())
                    setVibrate(vibrationPattern)
                } else {
                    setSilent(true)
                    setOnlyAlertOnce(true)
                }
            }
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notification)
        notificationManager.notify(
            GROUP_SUMMARY_NOTIFICATION_ID,
            buildIncomingPushGroupSummaryNotification(context),
        )
    }

    private fun buildIncomingPushGroupSummaryNotification(context: Context): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            GROUP_SUMMARY_NOTIFICATION_ID,
            createOpenIncomingHistoryIntent(
                context = context,
                notificationId = GROUP_SUMMARY_NOTIFICATION_ID,
                eventId = "incoming-push-group",
                messageType = "summary",
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, STANDARD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.push_notification_group_summary_title))
            .setContentText(context.getString(R.string.push_notification_group_summary_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(GROUP_KEY_INCOMING_PUSH)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createOpenIncomingHistoryIntent(
        context: Context,
        notificationId: Int,
        eventId: String,
        messageType: String,
    ): Intent {
        val tapToken = Uri.encode(
            listOf(notificationId.toString(), eventId, messageType, System.currentTimeMillis().toString())
                .joinToString(separator = "-"),
        )
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_INCOMING_HISTORY
            data = Uri.parse("aurora://notification-tap/$tapToken")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_HISTORY, true)
            putExtra(EXTRA_HISTORY_FILTER, HISTORY_FILTER_INCOMING)
            putExtra(FCM_DATA_CHANNEL_KEY, FCM_DATA_CHANNEL_AURORA_PUSH)
        }
    }

    fun shouldOpenIncomingHistory(intent: Intent?): Boolean {
        if (intent == null) return false
        if (intent.action == ACTION_OPEN_INCOMING_HISTORY) return true
        if (intent.getBooleanExtra(EXTRA_OPEN_HISTORY, false)) return true
        return intent.getStringExtra(FCM_DATA_CHANNEL_KEY) == FCM_DATA_CHANNEL_AURORA_PUSH
    }

    fun historyFilterFromIntent(intent: Intent?): String {
        return intent?.getStringExtra(EXTRA_HISTORY_FILTER)
            ?.takeIf { it.isNotBlank() }
            ?: HISTORY_FILTER_INCOMING
    }

    private fun notificationSoundUri(): Uri? {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun ringtoneSoundUri(): Uri? {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }
}
