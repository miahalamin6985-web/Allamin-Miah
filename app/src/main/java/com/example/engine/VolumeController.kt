package com.example.engine

import android.content.Context
import android.media.AudioManager

class VolumeController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun increaseVolume(): String {
        return try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            "Volume increased to $current of $max"
        } catch (e: Exception) {
            "Unable to adjust volume: ${e.message}"
        }
    }

    fun decreaseVolume(): String {
        return try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            "Volume decreased to $current of $max"
        } catch (e: Exception) {
            "Unable to adjust volume: ${e.message}"
        }
    }

    fun muteVolume(): String {
        return try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            "Media volume muted"
        } catch (e: Exception) {
            "Unable to mute volume: ${e.message}"
        }
    }

    fun maxVolume(): String {
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
            "Volume set to maximum"
        } catch (e: Exception) {
            "Unable to set maximum volume: ${e.message}"
        }
    }

    fun getCurrentVolumePercentage(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) (current * 100) / max else 0
    }
}
