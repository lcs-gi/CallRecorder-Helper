package com.callrecorder.core

import org.json.JSONArray
import org.json.JSONObject

enum class Direction { INCOMING, OUTGOING, UNKNOWN }

/** Un appel enregistré. Le fichier audio est dans le dossier des enregistrements, sous [fileName]. */
data class Recording(
    val id: String,
    val fileName: String,
    val startedAt: Long,
    val durationMs: Long,
    val direction: Direction,
    val number: String? = null,
    val contactName: String? = null,
    val favorite: Boolean = false,
    val note: String = "",
    val sizeBytes: Long = 0,
    /** Aucun son capté : le téléphone bloque probablement la source audio choisie. */
    val silent: Boolean = false,
    val audioSource: AudioSourceOption = AudioSourceOption.VOICE_RECOGNITION,
) {
    /** Nom affiché : contact, sinon numéro, sinon null (numéro masqué). */
    val title: String? get() = contactName?.takeIf { it.isNotBlank() } ?: number?.takeIf { it.isNotBlank() }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("file", fileName)
        put("start", startedAt)
        put("duration", durationMs)
        put("direction", direction.name)
        number?.let { put("number", it) }
        contactName?.let { put("name", it) }
        put("favorite", favorite)
        put("note", note)
        put("size", sizeBytes)
        put("silent", silent)
        put("source", audioSource.name)
    }

    companion object {
        fun fromJson(o: JSONObject) = Recording(
            id = o.getString("id"),
            fileName = o.getString("file"),
            startedAt = o.optLong("start"),
            durationMs = o.optLong("duration"),
            direction = enumOr(o.optString("direction"), Direction.UNKNOWN),
            number = o.optString("number").takeIf { o.has("number") && it.isNotEmpty() },
            contactName = o.optString("name").takeIf { o.has("name") && it.isNotEmpty() },
            favorite = o.optBoolean("favorite"),
            note = o.optString("note"),
            sizeBytes = o.optLong("size"),
            silent = o.optBoolean("silent"),
            audioSource = enumOr(o.optString("source"), AudioSourceOption.VOICE_RECOGNITION),
        )

        fun listToJson(list: List<Recording>): String = JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        /** Les entrées illisibles sont ignorées plutôt que de perdre toute la liste. */
        fun listFromJson(text: String?): List<Recording> {
            if (text.isNullOrBlank()) return emptyList()
            val arr = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i -> runCatching { fromJson(arr.getJSONObject(i)) }.getOrNull() }
        }
    }
}

inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
