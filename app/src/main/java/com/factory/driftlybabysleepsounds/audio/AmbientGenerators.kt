package com.factory.driftlybabysleepsounds.audio

import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = (2.0 * Math.PI).toFloat()

/** Steady brown-noise bed with a faint low-frequency hum and a slow blade-rotation wobble. */
class FanGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private val bed = BrownNoiseGenerator(random)
    private var humPhase = 0f
    private var lfoPhase = 0f
    private val scratch = FloatArray(4096)

    override fun fill(buffer: FloatArray, length: Int) {
        val bedBuf = if (scratch.size >= length) scratch else FloatArray(length)
        bed.fill(bedBuf, length)
        val humStep = TWO_PI * 120f / SAMPLE_RATE
        val lfoStep = TWO_PI * 3.2f / SAMPLE_RATE
        for (i in 0 until length) {
            val hum = sin(humPhase) * 0.04f
            val wobble = 0.92f + 0.08f * sin(lfoPhase)
            humPhase += humStep
            lfoPhase += lfoStep
            buffer[i] = clamp((bedBuf[i] * 0.9f + hum) * wobble)
        }
        if (humPhase > TWO_PI) humPhase %= TWO_PI
        if (lfoPhase > TWO_PI) lfoPhase %= TWO_PI
    }
}

/** Pink-noise bed with randomly timed decaying droplet "ticks" layered on top. */
class RainGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private val bed = PinkNoiseGenerator(random)
    private val scratch = FloatArray(4096)
    private var samplesUntilNextDrop = nextDropDelay()
    private var dropletEnvelope = 0f
    private var dropletLpState = 0f

    private fun nextDropDelay(): Int = (SAMPLE_RATE * (0.01 + random.nextFloat() * 0.05)).toInt()

    override fun fill(buffer: FloatArray, length: Int) {
        val bedBuf = if (scratch.size >= length) scratch else FloatArray(length)
        bed.fill(bedBuf, length)
        for (i in 0 until length) {
            if (samplesUntilNextDrop <= 0) {
                dropletEnvelope = 0.4f + random.nextFloat() * 0.6f
                samplesUntilNextDrop = nextDropDelay()
            } else {
                samplesUntilNextDrop--
            }
            val dropletNoise = nextWhite(random) * dropletEnvelope
            dropletLpState += (dropletNoise - dropletLpState) * 0.5f
            dropletEnvelope *= 0.985f
            buffer[i] = clamp(bedBuf[i] * 0.55f + dropletLpState * 0.7f)
        }
    }
}

/** Brown-noise bed shaped by a slow swell envelope, with a foamy wash of extra noise at each crest. */
class OceanWaveGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private val bed = BrownNoiseGenerator(random)
    private val scratch = FloatArray(4096)
    private var swellPhase = 0f
    private var swellPhase2 = 1.7f

    override fun fill(buffer: FloatArray, length: Int) {
        val bedBuf = if (scratch.size >= length) scratch else FloatArray(length)
        bed.fill(bedBuf, length)
        val swellStep = TWO_PI * (1f / 7.0f) / SAMPLE_RATE
        val swellStep2 = TWO_PI * (1f / 11.0f) / SAMPLE_RATE
        for (i in 0 until length) {
            val swell = (sin(swellPhase) + sin(swellPhase2) * 0.5f) / 1.5f
            val envelope = 0.35f + 0.65f * ((swell + 1f) / 2f)
            swellPhase += swellStep
            swellPhase2 += swellStep2
            val foam = nextWhite(random) * (envelope * envelope) * 0.25f
            buffer[i] = clamp(bedBuf[i] * envelope + foam)
        }
        if (swellPhase > TWO_PI) swellPhase %= TWO_PI
        if (swellPhase2 > TWO_PI) swellPhase2 %= TWO_PI
    }
}

/** Two brown-noise beds with different smoothing, cross-faded by a slow LFO to suggest gusts. */
class WindGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private var lastSlow = 0f
    private var lastFast = 0f
    private var gustPhase = 0f
    private var amplitudePhase = 2.1f

    override fun fill(buffer: FloatArray, length: Int) {
        val gustStep = TWO_PI * (1f / 9.0f) / SAMPLE_RATE
        val ampStep = TWO_PI * (1f / 5.5f) / SAMPLE_RATE
        for (i in 0 until length) {
            val white = nextWhite(random)
            lastSlow = (lastSlow + 0.008f * white) / 1.008f
            lastFast = (lastFast + 0.05f * white) / 1.05f
            val gustMix = (sin(gustPhase) + 1f) / 2f
            gustPhase += gustStep
            val amplitude = 0.55f + 0.45f * ((sin(amplitudePhase) + 1f) / 2f)
            amplitudePhase += ampStep
            val mixed = lastSlow * (1f - gustMix) * 5f + lastFast * gustMix * 3.2f
            buffer[i] = clamp(mixed * amplitude)
        }
        if (gustPhase > TWO_PI) gustPhase %= TWO_PI
        if (amplitudePhase > TWO_PI) amplitudePhase %= TWO_PI
    }
}
