package com.ppam.eyemovementbellapp.sound

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.ppam.eyemovementbellapp.R

object SoundPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun startLoopingBell(context: Context) {
        stop()

        mediaPlayer = MediaPlayer.create(context, R.raw.templebells).apply {
            isLooping = true
            start()
        }

        Log.d("SoundPlayer", "Bell sound started looping")
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
            }
        }
        mediaPlayer = null
        Log.d("SoundPlayer", "Bell sound stopped")
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
}