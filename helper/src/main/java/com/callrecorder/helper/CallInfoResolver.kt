package com.callrecorder.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.callrecorder.core.Direction
import com.callrecorder.core.CallInfo
import com.callrecorder.core.Finalize
import com.callrecorder.core.PhoneNumbers
import kotlinx.coroutines.delay

/**
 * Le numéro n'est pas fourni pendant l'appel aux applications ordinaires :
 * on le lit dans le journal d'appels, où le téléphone l'écrit juste après la fin de l'appel.
 */
class CallInfoResolver(private val context: Context) {

    private fun has(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    suspend fun resolve(callStart: Long): CallInfo {
        var entry: Finalize.LogEntry? = null
        if (has(Manifest.permission.READ_CALL_LOG)) {
            // Le journal est parfois écrit avec un peu de retard.
            for (attempt in 0 until 5) {
                delay(800)
                entry = Finalize.pickLogEntry(callsAround(callStart), callStart)
                if (entry != null) break
            }
        }
        val number = entry?.number?.takeIf { !PhoneNumbers.isHidden(it) }
        val direction = Finalize.direction(entry?.type)
        val name = number?.let { contactName(it) }
        val isContact = when {
            name != null -> true
            !has(Manifest.permission.READ_CONTACTS) -> null
            else -> false
        }
        return CallInfo(number, direction, name, isContact)
    }

    /** Lignes du journal autour du début de l'appel ; le choix est fait par [Finalize.pickLogEntry]. */
    private fun callsAround(callStart: Long): List<Finalize.LogEntry> = runCatching {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} <= ?",
            arrayOf((callStart - Finalize.LOG_BEFORE_MS).toString(), (callStart + Finalize.LOG_AFTER_MS).toString()),
            "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            buildList { while (c.moveToNext()) add(Finalize.LogEntry(c.getString(0), c.getInt(1), c.getLong(2))) }
        }
    }.getOrNull().orEmpty()

    fun contactName(number: String): String? {
        if (!has(Manifest.permission.READ_CONTACTS)) return null
        return runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
    }
}
