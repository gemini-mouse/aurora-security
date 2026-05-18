package app.aurorasecurity.security

import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager

class AlarmPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var originalAlarmVolume: Int? = null

    fun play() {
        if (mediaPlayer?.isPlaying == true) return

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val alarmStream = AudioManager.STREAM_ALARM
        val maxVolume = audioManager.getStreamMaxVolume(alarmStream)
        originalAlarmVolume = audioManager.getStreamVolume(alarmStream)
        if (originalAlarmVolume != maxVolume) {
            runCatching {
                audioManager.setStreamVolume(
                    alarmStream,
                    maxVolume,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE,
                )
            }
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    fun stop() {
        restoreVolume()
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        stop()
    }

    private fun restoreVolume() {
        val volumeToRestore = originalAlarmVolume ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                volumeToRestore,
                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE,
            )
        }
        originalAlarmVolume = null
    }
}
