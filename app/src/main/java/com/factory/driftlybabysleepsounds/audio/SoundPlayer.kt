package com.factory.driftlybabysleepsounds.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.max

private const val CHUNK_SAMPLES = 2048

/**
 * Drives a single [NoiseGenerator] into its own [AudioTrack]. Multiple [SoundPlayer]s can run
 * at once — Android's audio mixer sums their output, which is how the app layers several
 * ambient sounds together without any extra mixing code.
 */
class SoundPlayer(private val generator: NoiseGenerator) {
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    @Volatile private var running = false
    @Volatile private var currentVolume = 1f

    val isPlaying: Boolean get() = running

    fun start(initialVolume: Float) {
        if (running) return
        currentVolume = initialVolume
        val minBufferBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferBytes = max(minBufferBytes, CHUNK_SAMPLES * 2) * 2

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(0f)
        track.play()
        audioTrack = track
        running = true

        playbackThread = thread(name = "SoundPlayer-${generator.javaClass.simpleName}") {
            val floatBuffer = FloatArray(CHUNK_SAMPLES)
            val shortBuffer = ShortArray(CHUNK_SAMPLES)
            while (running) {
                generator.fill(floatBuffer, CHUNK_SAMPLES)
                for (i in 0 until CHUNK_SAMPLES) {
                    val sample = (floatBuffer[i] * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    shortBuffer[i] = sample.toShort()
                }
                track.write(shortBuffer, 0, CHUNK_SAMPLES)
            }
        }

        fade(from = 0f, to = currentVolume, durationMs = 300)
    }

    fun setVolume(volume: Float) {
        currentVolume = volume
        audioTrack?.setVolume(volume)
    }

    fun stop(onStopped: (() -> Unit)? = null) {
        if (!running) {
            onStopped?.invoke()
            return
        }
        fade(from = currentVolume, to = 0f, durationMs = 200) {
            running = false
            playbackThread?.join(500)
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            onStopped?.invoke()
        }
    }

    private fun fade(from: Float, to: Float, durationMs: Int, onDone: (() -> Unit)? = null) {
        thread(name = "SoundPlayer-Fade") {
            val steps = 10
            val stepDelay = max(1, durationMs / steps).toLong()
            for (step in 0..steps) {
                val v = from + (to - from) * (step / steps.toFloat())
                audioTrack?.setVolume(v.coerceIn(0f, 1f))
                Thread.sleep(stepDelay)
            }
            onDone?.invoke()
        }
    }
}
