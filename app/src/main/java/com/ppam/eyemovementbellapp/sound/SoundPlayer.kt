package com.ppam.eyemovementbellapp.sound

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.ppam.eyemovementbellapp.R

object SoundPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun playBellSound(context: Context) {
        stop()

        mediaPlayer = MediaPlayer.create(context, R.raw.templebells).apply {
            isLooping = false
            setOnCompletionListener {
                stop()
            }
            start()
        }

        Log.d("SoundPlayer", "Bell sound played")
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
            }
        }
        mediaPlayer = null
    }
}