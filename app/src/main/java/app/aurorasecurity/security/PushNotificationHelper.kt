package app.aurorasecurity.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

object PushNotificationHelper {
    const val CHANNEL_ID = "aurora_emergency_alerts_v2"
    const val EXTRA_OPEN_HISTORY = "app.aurorasecurity.security.extra.OPEN_HISTORY"
    const val EXTRA_HISTORY_FILTER = "app.aurorasecurity.security.extra.HISTORY_FILTER"
    const val HISTORY_FILTER_INCOMING = "incoming"
    private val vibrationPattern = longArrayOf(0L, 300L, 180L, 420L)
    private val nextNotificationId = AtomicInteger(
        ((System.currentTimeMillis() and 0x0FFFFFFF).toInt()).coerceAtLeast(3001),
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.push_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.push_notification_channel_description)
            enableVibration(true)
            setVibrationPattern(vibrationPattern)
            setSound(
                defaultSoundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    fun showEmergencyAlert(
        context: Context,
        title: String,
        body: String,
    ) {
        createNotificationChannel(context)
        val notificationId = nextNotificationId.getAndIncrement()

        val openIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_HISTORY, true)
                putExtra(EXTRA_HISTORY_FILTER, HISTORY_FILTER_INCOMING)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(Notification.DEFAULT_ALL)
            .setVibrate(vibrationPattern)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
