package com.callrecorder.helper

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.callrecorder.core.AudioQuality
import com.callrecorder.core.AudioSourceOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.sqrt

/**
 * Enregistrement AAC (.m4a) avec MediaRecorder.
 * Si la source choisie est refusée par le téléphone, les suivantes sont essayées dans l'ordre.
 * Le niveau sonore est relevé toutes les 200 ms pour dessiner la forme d'onde et détecter un enregistrement muet.
 */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var sampler: Job? = null
    private val wave = ByteArrayOutputStream()
    private var peak = 0

    var usedSource: AudioSourceOption? = null
        private set

    fun start(file: File, preferred: AudioSourceOption, quality: AudioQuality, scope: CoroutineScope): Boolean {
        val order = listOf(preferred) + FALLBACK.filter { it != preferred }
        for (source in order) {
            val r = newRecorder()
            try {
                r.setAudioSource(source.source)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioChannels(1)
                r.setAudioSamplingRate(quality.sampleRate)
                r.setAudioEncodingBitRate(quality.bitRate)
                r.setOutputFile(file.absolutePath)
                r.prepare()
                r.start()
                recorder = r
                usedSource = source
                startSampling(scope)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Source ${source.name} refusée", e)
                runCatching { r.reset() }
                runCatching { r.release() }
                file.delete()
            }
        }
        return false
    }

    /** Renvoie la forme d'onde (un octet 0–255 par 200 ms) et si l'enregistrement est muet. */
    fun stop(): Pair<ByteArray, Boolean> {
        sampler?.cancel()
        sampler = null
        recorder?.let { r ->
            // stop() échoue si rien n'a été encodé (appel très court) : le fichier est alors inutilisable.
            runCatching { r.stop() }.onFailure { Log.w(TAG, "stop", it) }
            runCatching { r.release() }
        }
        recorder = null
        val silent = peak == 0
        return wave.toByteArray() to silent
    }

    private fun startSampling(scope: CoroutineScope) {
        wave.reset()
        peak = 0
        sampler = scope.launch {
            while (isActive) {
                delay(SAMPLE_MS)
                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                if (amp > peak) peak = amp
                // Racine carrée : les voix faibles restent visibles à côté des voix fortes.
                wave.write((sqrt(amp / 32767.0).coerceIn(0.0, 1.0) * 255).toInt())
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_MS = 200L
        private val FALLBACK = listOf(
            AudioSourceOption.VOICE_RECOGNITION,
            AudioSourceOption.MIC,
            AudioSourceOption.VOICE_COMMUNICATION,
        )
    }
}
