package com.callrecorder.core

import androidx.core.net.toUri
import android.net.Uri

/** Appel en cours d'enregistrement (bandeau dans l'application). */
data class LiveCall(val startedAt: Long, val direction: Direction)

/**
 * Échanges entre l'application (Play Store) et le Helper (installé à part).
 *
 * L'application expose un ContentProvider ([AUTHORITY]) que seul le Helper peut appeler
 * (nom de paquet et certificat vérifiés, voir [Signatures]) :
 * - [SETTINGS] : `query` → une ligne, colonne [COLUMN_JSON] = [AppSettings] en JSON ;
 * - [incoming] : `openFile("w")` → le Helper y écrit l'audio (`<id>.m4a`) et la forme d'onde (`<id>.wave`) ;
 * - [RECORDINGS] : `insert` avec [COLUMN_JSON] = [Recording] en JSON, une fois les fichiers écrits ;
 * - `call` [METHOD_LIVE_START] / [METHOD_LIVE_STOP] : début et fin d'un enregistrement.
 *
 * Tout reste sur le téléphone : il n'y a aucun échange réseau.
 */
object Contract {
    const val APP_PACKAGE = "com.lcsgi.callrecorder"
    const val HELPER_PACKAGE = "com.lcsgi.callrecorder.helper"
    const val HELPER_ACCESSIBILITY_SERVICE = "com.callrecorder.helper.CallAccessibilityService"

    /** Version des échanges, pour les évolutions futures (le Helper et l'application se mettent à jour séparément). */
    const val VERSION = 1

    const val AUTHORITY = "com.lcsgi.callrecorder.recordings"
    const val COLUMN_JSON = "json"
    const val PATH_SETTINGS = "settings"
    const val PATH_INCOMING = "incoming"
    const val PATH_RECORDINGS = "recordings"
    const val METHOD_LIVE_START = "live_start"
    const val METHOD_LIVE_STOP = "live_stop"
    /** Résultat de l'assistant de réglage du son ([CalibrationResult] en JSON dans [EXTRA_JSON]). */
    const val METHOD_CALIBRATION = "calibration"
    const val EXTRA_JSON = "json"
    const val EXTRA_STARTED_AT = "started_at"
    const val EXTRA_DIRECTION = "direction"
    const val EXTRA_VERSION = "version"

    /** Ouvre un enregistrement dans l'application (notification). */
    const val EXTRA_RECORDING = "recording_id"

    /** Permissions du Helper, vérifiées aussi par l'application pour l'écran Configuration. */
    val HELPER_ESSENTIAL_PERMISSIONS = listOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.READ_PHONE_STATE,
    )
    val HELPER_OPTIONAL_PERMISSIONS = listOf(
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.READ_CONTACTS,
    )

    val BASE: Uri get() = "content://$AUTHORITY".toUri()
    val SETTINGS: Uri get() = Uri.withAppendedPath(BASE, PATH_SETTINGS)
    val RECORDINGS: Uri get() = Uri.withAppendedPath(BASE, PATH_RECORDINGS)
    fun incoming(name: String): Uri = BASE.buildUpon().appendPath(PATH_INCOMING).appendPath(name).build()

    fun audioName(id: String) = "$id.m4a"
    fun waveName(id: String) = "$id.wave"

    /**
     * Nom de fichier accepté dans le dossier d'arrivée : identifiant + extension connue, sans chemin.
     * Empêche d'écrire ailleurs (« ../ ») même pour un appelant autorisé.
     */
    fun isSafeIncomingName(name: String?): Boolean =
        name != null && name.length <= 80 && Regex("[A-Za-z0-9-]{1,64}\\.(m4a|wave)").matches(name)

    /** Identifiant d'enregistrement acceptable (UUID ou équivalent). */
    fun isSafeId(id: String?): Boolean = id != null && Regex("[A-Za-z0-9-]{1,64}").matches(id)
}
