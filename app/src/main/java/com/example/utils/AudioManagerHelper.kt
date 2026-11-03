package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File

object AudioManagerHelper {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    fun getSoundFile(context: Context, slot: Int): File {
        return File(context.filesDir, "guardian_sound_slot_$slot.aac")
    }

    fun isSlotRecorded(context: Context, slot: Int): Boolean {
        return getSoundFile(context, slot).exists()
    }

    fun deleteSlot(context: Context, slot: Int): Boolean {
        val file = getSoundFile(context, slot)
        return if (file.exists()) {
            file.delete()
        } else false
    }

    fun startRecording(context: Context, slot: Int) {
        stopRecording()
        val file = getSoundFile(context, slot)
        
        @Suppress("DEPRECATION")
        val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(file.absolutePath)
        recorder.setMaxDuration(30000) // 30 seconds count

        recorder.prepare()
        recorder.start()
        mediaRecorder = recorder
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Safe ignore
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }

    fun startPlayback(context: Context, slot: Int, onComplete: () -> Unit = {}) {
        stopPlayback()
        val file = getSoundFile(context, slot)
        if (!file.exists()) {
            onComplete()
            return
        }

        // Boost to max volume for STREAM_ALARM (antitheft requirement)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            it.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        }

        val player = MediaPlayer()
        player.setDataSource(file.absolutePath)
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        player.prepare()
        player.setOnCompletionListener {
            onComplete()
            stopPlayback()
        }
        player.start()
        mediaPlayer = player
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun playSystemAlarm(context: Context) {
        stopPlayback()
        
        // Anti-silent sound system: MAX volume on ALARM stream
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            it.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        }

        val player = MediaPlayer()
        val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        try {
            player.setDataSource(context, alarmUri)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_ALARM)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = true
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            // fallback if alarm is empty
        }
    }
}
