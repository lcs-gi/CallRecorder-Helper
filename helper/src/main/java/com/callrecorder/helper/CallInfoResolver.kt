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
        var entry: Pair<String?, Int>? = null
        if (has(Manifest.permission.READ_CALL_LOG)) {
            // Le journal est parfois écrit avec un peu de retard.
            for (attempt in 0 until 5) {
                delay(800)
                entry = lastCallSince(callStart - 120_000)
                if (entry != null) break
            }
        }
        val number = entry?.first?.takeIf { !PhoneNumbers.isHidden(it) }
        val direction = Finalize.direction(entry?.second)
        val name = number?.let { contactName(it) }
        val isContact = when {
            name != null -> true
            !has(Manifest.permission.READ_CONTACTS) -> null
            else -> false
        }
        return CallInfo(number, direction, name, isContact)
    }

    private fun lastCallSince(since: Long): Pair<String?, Int>? = runCatching {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            "${CallLog.Calls.DATE} >= ?",
            arrayOf(since.toString()),
            "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            if (c.moveToFirst()) c.getString(0) to c.getInt(1) else null
        }
    }.getOrNull()

    fun contactName(number: String): String? {
        if (!has(Manifest.permission.READ_CONTACTS)) return null
        return runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
    }
}
