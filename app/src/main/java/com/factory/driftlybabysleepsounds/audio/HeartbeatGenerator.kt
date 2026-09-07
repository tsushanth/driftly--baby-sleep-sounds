package com.factory.driftlybabysleepsounds.audio

import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = (2.0 * Math.PI).toFloat()
private const val BEATS_PER_MINUTE = 68
private const val CYCLE_SECONDS = 60f / BEATS_PER_MINUTE

/**
 * A soft brown-noise "womb" bed under a two-lobe "lub-dub" heartbeat thump, synthesized as
 * short sine bursts with a fast-attack/exponential-decay amplitude envelope.
 */
class HeartbeatGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private val bed = BrownNoiseGenerator(random)
    private val scratch = FloatArray(4096)
    private var cyclePosition = 0f
    private val cycleLengthSamples = (SAMPLE_RATE * CYCLE_SECONDS)

    private fun thumpEnvelope(tSeconds: Float, onsetSeconds: Float, decayRate: Float): Float {
        val dt = tSeconds - onsetSeconds
        if (dt < 0f) return 0f
        return exp(-decayRate * dt)
    }

    override fun fill(buffer: FloatArray, length: Int) {
        val bedBuf = if (scratch.size >= length) scratch else FloatArray(length)
        bed.fill(bedBuf, length)
        for (i in 0 until length) {
            val tSeconds = cyclePosition / SAMPLE_RATE
            val lub = thumpEnvelope(tSeconds, 0f, 24f) * sin(TWO_PI * 58f * tSeconds)
            val dubOnset = CYCLE_SECONDS * 0.32f
            val dub = thumpEnvelope(tSeconds, dubOnset, 28f) * sin(TWO_PI * 48f * (tSeconds - dubOnset))
            val thump = (lub * 0.9f + dub * 0.6f)
            cyclePosition += 1f
            if (cyclePosition >= cycleLengthSamples) cyclePosition -= cycleLengthSamples
            buffer[i] = clamp(bedBuf[i] * 0.18f + thump * 0.8f)
        }
    }
}
