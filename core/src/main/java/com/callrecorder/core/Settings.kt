package com.callrecorder.core

import android.media.MediaRecorder
import org.json.JSONObject

/** Appels à conserver. Tous les appels sont enregistrés ; le tri se fait à la fin, une fois le numéro connu. */
enum class RecordFilter {
    ALL, CONTACTS, UNKNOWN;

    /** [isContact] : null si on ne sait pas (permission Contacts refusée) → on garde. */
    fun keeps(isContact: Boolean?, hiddenNumber: Boolean): Boolean = when (this) {
        ALL -> true
        CONTACTS -> isContact != false && !hiddenNumber
        UNKNOWN -> isContact != true
    }
}

/**
 * Source audio de MediaRecorder. Sans privilèges système, aucune ne donne le son de l'appel lui-même :
 * le correspondant n'est capté que par le micro, depuis l'écouteur ou le haut-parleur.
 * Selon le téléphone, une source le capte mieux que les autres (traitements du son différents).
 */
enum class AudioSourceOption(val source: Int) {
    VOICE_RECOGNITION(MediaRecorder.AudioSource.VOICE_RECOGNITION),
    MIC(MediaRecorder.AudioSource.MIC),
    /** Micro sans aucun traitement (ni réduction de bruit, ni annulation d'écho). */
    UNPROCESSED(MediaRecorder.AudioSource.UNPROCESSED),
    /** Souvent le micro du haut, près de l'écouteur. */
    CAMCORDER(MediaRecorder.AudioSource.CAMCORDER),
    VOICE_COMMUNICATION(MediaRecorder.AudioSource.VOICE_COMMUNICATION),
    VOICE_CALL(MediaRecorder.AudioSource.VOICE_CALL),
}

enum class AudioQuality(val bitRate: Int, val sampleRate: Int) {
    LOW(32_000, 16_000),
    NORMAL(64_000, 44_100),
    HIGH(128_000, 48_000),
}

/**
 * Réglages choisis dans l'application. Le Helper les lit au début de chaque appel
 * ([Contract.SETTINGS]) : ils voyagent en JSON entre les deux applications.
 */
data class AppSettings(
    val autoRecord: Boolean = true,
    val filter: RecordFilter = RecordFilter.ALL,
    val source: AudioSourceOption = AudioSourceOption.VOICE_RECOGNITION,
    val quality: AudioQuality = AudioQuality.NORMAL,
    /** 0 = jamais. Les favoris ne sont jamais supprimés automatiquement. */
    val autoDeleteDays: Int = 0,
    val notifyAfterCall: Boolean = true,
    /** Haut-parleur pendant les appels enregistrés (sauf casque) : le correspondant est alors bien capté. */
    val autoSpeaker: Boolean = false,
    /** Volume de l'écouteur au maximum pendant l'appel, rétabli ensuite. */
    val boostVolume: Boolean = false,
) {
    fun toJson(): String = JSONObject().apply {
        put("auto", autoRecord)
        put("filter", filter.name)
        put("source", source.name)
        put("quality", quality.name)
        put("autoDelete", autoDeleteDays)
        put("notify", notifyAfterCall)
        put("speaker", autoSpeaker)
        put("boost", boostVolume)
    }.toString()

    companion object {
        val AUTO_DELETE_CHOICES = listOf(0, 7, 30, 90, 365)

        /** Valeurs par défaut pour ce qui manque ou est illisible (versions différentes des deux applications). */
        fun fromJson(text: String?): AppSettings {
            val o = text?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return AppSettings()
            val d = AppSettings()
            return AppSettings(
                autoRecord = o.optBoolean("auto", d.autoRecord),
                filter = enumOr(o.optString("filter"), d.filter),
                source = enumOr(o.optString("source"), d.source),
                quality = enumOr(o.optString("quality"), d.quality),
                autoDeleteDays = o.optInt("autoDelete", d.autoDeleteDays),
                notifyAfterCall = o.optBoolean("notify", d.notifyAfterCall),
                autoSpeaker = o.optBoolean("speaker", d.autoSpeaker),
                boostVolume = o.optBoolean("boost", d.boostVolume),
            )
        }
    }
}
