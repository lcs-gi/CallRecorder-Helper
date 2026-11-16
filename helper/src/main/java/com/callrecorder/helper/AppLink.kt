package com.callrecorder.helper

import androidx.core.content.edit
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.callrecorder.core.AppSettings
import com.callrecorder.core.CalibrationResult
import com.callrecorder.core.Contract
import com.callrecorder.core.LiveCall
import com.callrecorder.core.Recording
import com.callrecorder.core.Signatures
import org.json.JSONObject
import java.io.File

/**
 * Liaison avec l'application CallRecorder (voir [Contract]).
 *
 * Chaque enregistrement est d'abord rangé dans la file d'attente du Helper ([pendingDir]),
 * puis remis à l'application. Si elle est absente ou injoignable, il attend la prochaine occasion
 * (appel suivant, ouverture du Helper) : rien n'est perdu.
 */
object AppLink {
    private const val TAG = "AppLink"
    private const val PREFS = "link"

    fun isInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(Contract.APP_PACKAGE, 0); true }.getOrDefault(false)

    /**
     * Application installée, signée par nous, et propriétaire du point d'échange : on ne confie pas
     * un enregistrement à une imitation (même nom de paquet ou même autorité de ContentProvider).
     */
    fun isTrusted(context: Context): Boolean =
        isInstalled(context) &&
            providerOwner(context) == Contract.APP_PACKAGE &&
            Signatures.isTrusted(context, Contract.APP_PACKAGE, Signatures.parseList(BuildConfig.TRUSTED_APP_CERTS))

    @Suppress("DEPRECATION")
    private fun providerOwner(context: Context): String? =
        runCatching { context.packageManager.resolveContentProvider(Contract.AUTHORITY, 0)?.packageName }.getOrNull()

    /** Réglages de l'application ; les derniers connus si elle est injoignable, sinon ceux par défaut. */
    fun settings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fresh = if (isTrusted(context)) runCatching {
            context.contentResolver.query(Contract.SETTINGS, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(Contract.COLUMN_JSON)) else null
            }
        }.onFailure { Log.w(TAG, "Réglages illisibles", it) }.getOrNull() else null
        if (fresh != null) prefs.edit { putString("settings", fresh) }
        return AppSettings.fromJson(fresh ?: prefs.getString("settings", null))
    }

    fun liveStart(context: Context, call: LiveCall) = notify(
        context, Contract.METHOD_LIVE_START,
        Bundle().apply {
            putLong(Contract.EXTRA_STARTED_AT, call.startedAt)
            putString(Contract.EXTRA_DIRECTION, call.direction.name)
        },
    )

    fun liveStop(context: Context) = notify(context, Contract.METHOD_LIVE_STOP, Bundle())

    /** Résultat de l'assistant de réglage du son : l'application applique le meilleur réglage. */
    fun calibration(context: Context, result: CalibrationResult) = notify(
        context, Contract.METHOD_CALIBRATION, Bundle().apply { putString(Contract.EXTRA_JSON, result.toJson()) },
    )

    private fun notify(context: Context, method: String, extras: Bundle) {
        if (!isTrusted(context)) return
        extras.putInt(Contract.EXTRA_VERSION, Contract.VERSION)
        runCatching { context.contentResolver.call(Contract.BASE, method, null, extras) }
            .onFailure { Log.w(TAG, "$method refusé", it) }
    }

    fun pendingDir(context: Context) = File(context.filesDir, "pending").apply { mkdirs() }

    /**
     * Range un enregistrement terminé dans la file d'attente. Le JSON est écrit en dernier :
     * sa présence signifie que l'audio et la forme d'onde sont complets.
     */
    fun enqueue(context: Context, rec: Recording, audio: File, wave: ByteArray) {
        val dir = pendingDir(context)
        val dest = File(dir, Contract.audioName(rec.id))
        if (!audio.renameTo(dest)) { audio.copyTo(dest, overwrite = true); audio.delete() }
        runCatching { File(dir, Contract.waveName(rec.id)).writeBytes(wave) }
        File(dir, "${rec.id}.json").writeText(rec.toJson().toString())
    }

    fun pendingCount(context: Context): Int =
        pendingDir(context).listFiles { f -> f.name.endsWith(".json") }?.size ?: 0

    /** Remet à l'application tout ce qui attend. Renvoie le nombre d'enregistrements remis. */
    @Synchronized
    fun deliver(context: Context): Int {
        if (!isTrusted(context)) return 0
        val dir = pendingDir(context)
        val queue = dir.listFiles { f -> f.name.endsWith(".json") }?.sortedBy { it.lastModified() } ?: return 0
        var delivered = 0
        for (json in queue) {
            val rec = runCatching { Recording.fromJson(JSONObject(json.readText())) }.getOrNull()
            val audio = rec?.let { File(dir, Contract.audioName(it.id)) }
            if (rec == null || audio == null || !audio.exists() || !Contract.isSafeId(rec.id)) {
                Log.w(TAG, "Entrée abîmée ignorée : ${json.name}")
                json.delete()
                continue
            }
            val wave = File(dir, Contract.waveName(rec.id))
            try {
                send(context, audio, Contract.audioName(rec.id))
                if (wave.exists()) send(context, wave, Contract.waveName(rec.id))
                context.contentResolver.insert(
                    Contract.RECORDINGS,
                    ContentValues().apply { put(Contract.COLUMN_JSON, rec.toJson().toString()) },
                ) ?: error("refusé par l'application")
            } catch (e: Exception) {
                Log.w(TAG, "Remise impossible, nouvel essai plus tard", e)
                break
            }
            audio.delete()
            wave.delete()
            json.delete()
            delivered++
        }
        return delivered
    }

    private fun send(context: Context, file: File, name: String) {
        val out = context.contentResolver.openOutputStream(Contract.incoming(name), "wt") ?: error("fichier refusé")
        out.use { o -> file.inputStream().use { it.copyTo(o) } }
    }

    /** Enregistrements interrompus (téléphone éteint pendant un appel…). */
    fun cleanup(context: Context) {
        val limit = System.currentTimeMillis() - 6 * 3600_000L
        pendingDir(context).listFiles { f -> f.name.endsWith(".part") && f.lastModified() < limit }?.forEach { it.delete() }
    }
}
