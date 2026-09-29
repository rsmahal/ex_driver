package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundSynthesizer {
    private val sampleRate = 22050
    private var isMuted = false
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun playCountdownBeep(isFinalGo: Boolean) {
        if (isMuted) return
        scope.launch {
            val freq = if (isFinalGo) 880f else 440f
            val durationMs = if (isFinalGo) 350 else 180
            playTone(freq, durationMs, 0.45f)
        }
    }

    fun playBumpSound() {
        if (isMuted) return
        scope.launch {
            val numSamples = (sampleRate * 0.15f).toInt()
            val buffer = ShortArray(numSamples)
            var currentFreq = 160f
            for (i in 0 until numSamples) {
                val decay = 1.0f - (i.toFloat() / numSamples)
                val noise = (Math.random() * 2 - 1).toFloat() * 0.2f
                val tone = sin(2.0 * Math.PI * currentFreq * i / sampleRate).toFloat()
                buffer[i] = (((tone * 0.8f + noise) * decay * 0.6f) * Short.MAX_VALUE).toInt().toShort()
                currentFreq = (currentFreq * 0.999f).coerceAtLeast(40f)
            }
            playPcm(buffer)
        }
    }

    fun playCoinSound() {
        if (isMuted) return
        scope.launch {
            val numSamples = (sampleRate * 0.18f).toInt()
            val buffer = ShortArray(numSamples)
            val half = numSamples / 2
            for (i in 0 until numSamples) {
                val freq = if (i < half) 987.77f else 1318.51f // B5 -> E6
                val env = 1f - ((i % half).toFloat() / half)
                val tone = sin(2.0 * Math.PI * freq * i / sampleRate).toFloat()
                buffer[i] = (tone * env * 0.4f * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playNitroSound() {
        if (isMuted) return
        scope.launch {
            val numSamples = (sampleRate * 0.4f).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val freq = 200f + (progress * 400f)
                val noise = (Math.random() * 2.0 - 1.0).toFloat() * 0.35f
                val tone = sin(2.0 * Math.PI * freq * i / sampleRate).toFloat() * 0.4f
                val envelope = sin(progress * Math.PI).toFloat()
                buffer[i] = ((tone + noise) * envelope * 0.5f * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playBoostPadSound() {
        if (isMuted) return
        scope.launch {
            val numSamples = (sampleRate * 0.25f).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val freq = 523.25f + (progress * 800f) // C5 sweep
                val tone = sin(2.0 * Math.PI * freq * i / sampleRate).toFloat()
                val env = 1f - progress
                buffer[i] = (tone * env * 0.45f * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playVictoryFanfare() {
        if (isMuted) return
        scope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
            for (freq in notes) {
                playTone(freq, 120, 0.45f)
                delay(125)
            }
            playTone(1046.50f, 400, 0.55f)
        }
    }

    private fun playTone(frequency: Float, durationMs: Int, volume: Float) {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        if (numSamples <= 0) return
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val envelope = 1f - (i.toFloat() / numSamples)
            val sample = sin(2.0 * Math.PI * frequency * i / sampleRate).toFloat()
            buffer[i] = (sample * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(buffer)
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Release after playback finished in background
            scope.launch {
                val delayTime = (buffer.size * 1000L / sampleRate) + 100L
                delay(delayTime)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
