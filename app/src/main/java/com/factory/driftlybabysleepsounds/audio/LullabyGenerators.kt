package com.factory.driftlybabysleepsounds.audio

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = (2.0 * Math.PI).toFloat()

/** A single melody event; a [frequencyHz] of 0 represents a rest. */
data class NoteSpec(val frequencyHz: Float, val durationSeconds: Float)

private class ToneVoice {
    var active = false
    var frequency = 0f
    var phase = 0f
    var ageSeconds = 0f
}

/**
 * A small polyphonic pool of decaying sine-harmonic voices. Notes are allowed to ring past
 * their rhythmic slot (like a music box or a sustained piano note) by simply letting the
 * envelope decay independently of when the next note starts.
 */
private class ToneSynth(
    private val harmonics: List<Pair<Int, Float>>,
    private val attackSeconds: Float,
    private val decayRate: Float,
    voiceCount: Int = 6
) {
    private val voices = Array(voiceCount) { ToneVoice() }
    private val dt = 1f / SAMPLE_RATE

    fun noteOn(frequencyHz: Float) {
        val voice = voices.firstOrNull { !it.active } ?: voices.minByOrNull { it.ageSeconds }!!
        voice.active = true
        voice.frequency = frequencyHz
        voice.phase = 0f
        voice.ageSeconds = 0f
    }

    fun renderSample(): Float {
        var sum = 0f
        for (voice in voices) {
            if (!voice.active) continue
            val attackGain = if (voice.ageSeconds < attackSeconds) voice.ageSeconds / attackSeconds else 1f
            val decayGain = exp(-decayRate * max(0f, voice.ageSeconds - attackSeconds))
            val envelope = attackGain * decayGain
            var voiceSample = 0f
            for ((harmonicNumber, harmonicAmplitude) in harmonics) {
                voiceSample += sin(voice.phase * harmonicNumber) * harmonicAmplitude
            }
            sum += voiceSample * envelope
            voice.phase += TWO_PI * voice.frequency * dt
            if (voice.phase > TWO_PI) voice.phase %= TWO_PI
            voice.ageSeconds += dt
            if (envelope < 0.001f && voice.ageSeconds > attackSeconds) voice.active = false
        }
        return sum
    }
}

private class MelodySequencer(private val melody: List<NoteSpec>, private val onNoteOn: (Float) -> Unit) {
    private var noteIndex = -1
    private var samplesRemainingInNote = 0

    fun advanceSample() {
        if (samplesRemainingInNote <= 0) {
            noteIndex = (noteIndex + 1) % melody.size
            val note = melody[noteIndex]
            samplesRemainingInNote = max(1, (note.durationSeconds * SAMPLE_RATE).toInt())
            if (note.frequencyHz > 0f) onNoteOn(note.frequencyHz)
        }
        samplesRemainingInNote--
    }
}

private val MUSIC_BOX_MELODY = listOf(
    NoteSpec(659.25f, 0.28f), // E5
    NoteSpec(783.99f, 0.28f), // G5
    NoteSpec(880.00f, 0.28f), // A5
    NoteSpec(783.99f, 0.28f), // G5
    NoteSpec(659.25f, 0.28f), // E5
    NoteSpec(587.33f, 0.28f), // D5
    NoteSpec(523.25f, 0.28f), // C5
    NoteSpec(587.33f, 0.28f), // D5
    NoteSpec(0f, 0.4f)
)

/** Bright, bell-like harmonic stack with a near-instant attack and long ring, like a wind-up music box. */
class MusicBoxLullabyGenerator(random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private val synth = ToneSynth(
        harmonics = listOf(1 to 1.0f, 2 to 0.5f, 3 to 0.25f, 4 to 0.12f),
        attackSeconds = 0.001f,
        decayRate = 2.2f
    )
    private val sequencer = MelodySequencer(MUSIC_BOX_MELODY) { synth.noteOn(it) }
    private val noiseFloor = PinkNoiseGenerator(random)
    private val noiseScratch = FloatArray(4096)

    override fun fill(buffer: FloatArray, length: Int) {
        val floorBuf = if (noiseScratch.size >= length) noiseScratch else FloatArray(length)
        noiseFloor.fill(floorBuf, length)
        for (i in 0 until length) {
            sequencer.advanceSample()
            val tone = synth.renderSample()
            buffer[i] = clamp(tone * 0.5f + floorBuf[i] * 0.02f)
        }
    }
}

private val PIANO_LULLABY_MELODY = listOf(
    NoteSpec(440.00f, 0.9f),  // A4
    NoteSpec(523.25f, 0.9f),  // C5
    NoteSpec(587.33f, 0.9f),  // D5
    NoteSpec(659.25f, 1.8f),  // E5
    NoteSpec(587.33f, 0.9f),  // D5
    NoteSpec(523.25f, 0.9f),  // C5
    NoteSpec(440.00f, 1.8f),  // A4
    NoteSpec(392.00f, 1.8f),  // G4
    NoteSpec(0f, 0.6f)
)

/** Softer, slower harmonic stack with a gentle attack and a short feedback echo for warmth. */
class PianoLullabyGenerator(random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private val synth = ToneSynth(
        harmonics = listOf(1 to 1.0f, 2 to 0.35f, 3 to 0.1f),
        attackSeconds = 0.03f,
        decayRate = 0.9f
    )
    private val sequencer = MelodySequencer(PIANO_LULLABY_MELODY) { synth.noteOn(it) }
    private val noiseFloor = PinkNoiseGenerator(random)
    private val noiseScratch = FloatArray(4096)

    private val delayBuffer = FloatArray((SAMPLE_RATE * 0.24f).toInt())
    private var delayIndex = 0

    override fun fill(buffer: FloatArray, length: Int) {
        val floorBuf = if (noiseScratch.size >= length) noiseScratch else FloatArray(length)
        noiseFloor.fill(floorBuf, length)
        for (i in 0 until length) {
            sequencer.advanceSample()
            val dry = synth.renderSample()
            val delayed = delayBuffer[delayIndex]
            val withEcho = dry + delayed * 0.35f
            delayBuffer[delayIndex] = withEcho * 0.5f
            delayIndex = (delayIndex + 1) % delayBuffer.size
            buffer[i] = clamp(withEcho * 0.45f + floorBuf[i] * 0.025f)
        }
    }
}
