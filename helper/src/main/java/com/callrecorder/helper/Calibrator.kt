package com.callrecorder.helper

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.callrecorder.core.AudioSourceOption
import com.callrecorder.core.Calibration
import com.callrecorder.core.CalibrationResult
import com.callrecorder.core.CalibrationTrial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Assistant de réglage du son, pendant l'appel de test (messagerie vocale qui parle, utilisateur silencieux) :
 * chaque source audio est essayée quelques secondes, puis le volume renforcé et le haut-parleur avec les
 * meilleures. Le niveau de la voix est mesuré ; rien n'est gardé (fichiers d'essai effacés).
 */
class Calibrator(private val context: Context) {
    private val thread = Dispatchers.IO.limitedParallelism(1)

    suspend fun run(isActive: () -> Boolean, audio: CallAudio): CalibrationResult = withContext(thread) {
        val trials = mutableListOf<CalibrationTrial>()
        val file = File(context.cacheDir, "calibration.m4a")
        suspend fun trial(source: AudioSourceOption, speaker: Boolean, boost: Boolean) {
            if (!isActive()) return
            audio.restore()
            audio.apply(speaker, boost)
            if (speaker) audio.reapplySpeaker()
            delay(SETTLE_MS)
            trials += measure(source, speaker, boost, file, isActive)
        }
        Calibration.SOURCES.forEach { trial(it, speaker = false, boost = false) }
        val plain = trials.filter { it.available }.sortedByDescending { it.level }
        plain.firstOrNull()?.let { trial(it.source, speaker = false, boost = true) }
        plain.take(2).forEach { trial(it.source, speaker = true, boost = false) }
        audio.restore()
        file.delete()
        CalibrationResult(trials, System.currentTimeMillis(), complete = isActive())
    }

    private suspend fun measure(
        source: AudioSourceOption, speaker: Boolean, boost: Boolean, file: File, isActive: () -> Boolean,
    ): CalibrationTrial {
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(source.source)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            Log.w(TAG, "Source ${source.name} refusée", e)
            runCatching { r.release() }
            return CalibrationTrial(source, speaker, boost, level = 0, available = false)
        }
        val amplitudes = mutableListOf<Int>()
        runCatching { r.maxAmplitude } // Le premier relevé remet le compteur à zéro.
        var elapsed = 0L
        while (elapsed < Calibration.TRIAL_MS && isActive()) {
            delay(SAMPLE_MS)
            elapsed += SAMPLE_MS
            amplitudes += runCatching { r.maxAmplitude }.getOrDefault(0)
        }
        runCatching { r.stop() }
        runCatching { r.release() }
        val level = Calibration.level(amplitudes)
        Log.i(TAG, "${source.name} haut-parleur=$speaker volume=$boost : $level")
        return CalibrationTrial(source, speaker, boost, level, available = true)
    }

    private companion object {
        const val TAG = "Calibrator"
        const val SAMPLE_MS = 100L
        /** Le temps que le haut-parleur ou le volume change réellement. */
        const val SETTLE_MS = 600L
    }
}
