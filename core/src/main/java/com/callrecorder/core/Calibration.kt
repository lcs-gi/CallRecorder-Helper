package com.callrecorder.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Essai de l'assistant de réglage du son : une source audio, avec ou sans haut-parleur / volume renforcé,
 * et le niveau mesuré de la voix du correspondant (messagerie vocale qui parle, utilisateur silencieux).
 */
data class CalibrationTrial(
    val source: AudioSourceOption,
    val speaker: Boolean,
    val boost: Boolean,
    /** Niveau de la voix (80e centile de l'amplitude, 0–32767) ; 0 si la source est refusée. */
    val level: Int,
    val available: Boolean,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("source", source.name).put("speaker", speaker).put("boost", boost).put("level", level).put("available", available)

    companion object {
        fun fromJson(o: JSONObject): CalibrationTrial? {
            val source = AudioSourceOption.entries.firstOrNull { it.name == o.optString("source") } ?: return null
            return CalibrationTrial(source, o.optBoolean("speaker"), o.optBoolean("boost"), o.optInt("level"), o.optBoolean("available"))
        }
    }
}

data class CalibrationResult(val trials: List<CalibrationTrial>, val finishedAt: Long, val complete: Boolean) {
    fun toJson(): String = JSONObject()
        .put("finished", finishedAt).put("complete", complete)
        .put("trials", JSONArray().apply { trials.forEach { put(it.toJson()) } })
        .toString()

    companion object {
        fun fromJson(text: String?): CalibrationResult? {
            val o = text?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
            val a = o.optJSONArray("trials") ?: return null
            val trials = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(CalibrationTrial::fromJson) }
            return CalibrationResult(trials, o.optLong("finished"), o.optBoolean("complete"))
        }
    }
}

/** Déroulé et choix de l'assistant de réglage du son, séparés du code Android pour être testés. */
object Calibration {
    /** Durée de chaque essai. */
    const val TRIAL_MS = 4_000L

    /** Après avoir lancé l'assistant, l'appel de test doit être passé dans ce délai ; ensuite, les appels sont normaux. */
    const val WINDOW_MS = 10 * 60_000L

    /** Sources essayées, sans haut-parleur. Les sources refusées par le téléphone sont notées indisponibles. */
    val SOURCES = listOf(
        AudioSourceOption.VOICE_RECOGNITION,
        AudioSourceOption.MIC,
        AudioSourceOption.UNPROCESSED,
        AudioSourceOption.CAMCORDER,
        AudioSourceOption.VOICE_COMMUNICATION,
        AudioSourceOption.VOICE_CALL,
    )

    /** Niveau à partir duquel la voix est jugée bien audible (≈ -18 dB sous le maximum). */
    const val GOOD_LEVEL = 4_000

    /**
     * Réglage recommandé. Le haut-parleur gêne pendant l'appel : il n'est choisi que si aucun réglage sans
     * haut-parleur n'atteint [GOOD_LEVEL], ou s'il fait au moins deux fois mieux. Idem pour le volume renforcé.
     */
    fun recommend(trials: List<CalibrationTrial>): CalibrationTrial? {
        val ok = trials.filter { it.available && it.level > 0 }
        val plain = ok.filter { !it.speaker && !it.boost }.maxByOrNull { it.level }
        val boosted = ok.filter { !it.speaker && it.boost }.maxByOrNull { it.level }
        val speaker = ok.filter { it.speaker }.maxByOrNull { it.level }
        var best = plain
        if (boosted != null && (best == null || (best.level < GOOD_LEVEL && boosted.level > best.level * 1.5))) best = boosted
        if (speaker != null && (best == null || best.level < GOOD_LEVEL || speaker.level >= best.level * 2)) {
            if (best == null || speaker.level > best.level) best = speaker
        }
        return best
    }

    /** Niveau d'un essai : 80e centile des amplitudes relevées (les crêtes de la voix, pas le bruit). */
    fun level(amplitudes: List<Int>): Int {
        if (amplitudes.isEmpty()) return 0
        val sorted = amplitudes.sorted()
        return sorted[((sorted.size - 1) * 0.8).toInt()]
    }

    /** L'appel qui commence doit-il servir de test ? */
    fun active(settings: AppSettings, now: Long): Boolean = now < settings.calibrateUntil
}
