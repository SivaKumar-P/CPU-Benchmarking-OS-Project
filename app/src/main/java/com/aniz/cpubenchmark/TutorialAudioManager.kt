package com.aniz.cpubenchmark

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import kotlin.math.sin

/**
 * TutorialAudioManager - Delivers spoken audio narration and sound cues
 * for the Animated Video & Audio Tutorial.
 *
 * Uses Android's built-in TextToSpeech engine so narration is clear,
 * 100% offline, requires no bulky external video/audio downloads,
 * and runs natively on every Android device.
 */
class TutorialAudioManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isMuted = false
    private var speechRate = 1.0f

    var onSpeechCompleted: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(speechRate)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onSpeechCompleted?.invoke()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })
            isTtsReady = true
        } else {
            Log.e("TutorialAudio", "TextToSpeech initialization failed with status $status")
        }
    }

    fun speakNarration(text: String, utteranceId: String = "narration") {
        if (isMuted || !isTtsReady) return
        stopNarration()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopNarration() {
        if (isTtsReady) {
            tts?.stop()
        }
    }

    fun setMuted(muted: Boolean) {
        this.isMuted = muted
        if (muted) stopNarration()
    }

    fun isMuted(): Boolean = isMuted

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    /**
     * Synthesizes a subtle tone pulse when scenes change or events occur.
     */
    fun playToneCue(freqHz: Float = 587.33f, durationMs: Int = 120) {
        if (isMuted) return
        Thread {
            try {
                val sampleRate = 44100
                val numSamples = (durationMs * sampleRate) / 1000
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i * freqHz / sampleRate
                    val envelope = (1.0 - (i.toDouble() / numSamples)) // soft decay
                    samples[i] = (sin(angle) * 32767 * 0.18 * envelope).toInt().toShort()
                }
                val bufferSize = numSamples * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, numSamples)
                track.play()
                Thread.sleep(durationMs.toLong() + 50)
                track.release()
            } catch (ignored: Exception) {}
        }.start()
    }

    fun release() {
        stopNarration()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }
}
