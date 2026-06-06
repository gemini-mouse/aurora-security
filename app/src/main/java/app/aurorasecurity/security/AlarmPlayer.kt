package app.aurorasecurity.security

import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.annotation.RawRes

class AlarmPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var originalAlarmVolume: Int? = null
    private val alarmPreferences = AlarmPreferences(context)

    fun play() {
        if (mediaPlayer?.isPlaying == true) return

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

        mediaPlayer = when (alarmPreferences.getLoudSosSound()) {
            LoudSosSound.Siren -> createAlarmMediaPlayer(R.raw.loud_sos_alarm)
                ?: createSystemSoundMediaPlayer(RingtoneManager.TYPE_ALARM)
            LoudSosSound.Ringtone -> createSystemSoundMediaPlayer(RingtoneManager.TYPE_RINGTONE)
                ?: createSystemSoundMediaPlayer(RingtoneManager.TYPE_ALARM)
        } ?: createSystemSoundMediaPlayer(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer?.start()
    }

    private fun createAlarmMediaPlayer(@RawRes soundRes: Int): MediaPlayer? {
        return runCatching {
            context.resources.openRawResourceFd(soundRes)?.use { descriptor ->
                MediaPlayer().apply {
                    configureAlarmPlayback()
                    setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length,
                    )
                    isLooping = true
                    prepare()
                }
            }
        }.getOrNull()
    }

    private fun createSystemSoundMediaPlayer(type: Int): MediaPlayer? {
        val uri = RingtoneManager.getDefaultUri(type)
            ?: return null

        return runCatching {
            MediaPlayer().apply {
                configureAlarmPlayback()
                setDataSource(context, uri)
                isLooping = true
                prepare()
            }
        }.getOrNull()
    }

    private fun MediaPlayer.configureAlarmPlayback() {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
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
