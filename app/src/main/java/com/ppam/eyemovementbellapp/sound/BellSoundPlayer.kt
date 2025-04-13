package com.ppam.eyemovementbellapp.sound


import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
/*
object BellSoundPlayer {
    private var soundPool: SoundPool? = null
    private var bellSoundId: Int = 0
    private var isPlaying = false
    private var isLoaded = false

    fun init(context: Context) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(1)
            .build()

        bellSoundId = soundPool!!.load(context, R.raw.templebells, 1)
        soundPool!!.setOnLoadCompleteListener { _, _, status ->
            isLoaded = status == 0
        }
    }

    fun toggleBell() {
        if (!isLoaded) {
            Log.d("BellSoundPlayer", "Sound not loaded yet")
            return
        }

        if (isPlaying) {
            soundPool?.stop(bellSoundId)
            isPlaying = false
            Log.d("BellSoundPlayer", "Bell stopped")
        } else {
            soundPool?.play(bellSoundId, 1f, 1f, 1, -1, 1f)
            isPlaying = true
            Log.d("BellSoundPlayer", "Bell started")
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded = false
        isPlaying = false
    }
}
*/