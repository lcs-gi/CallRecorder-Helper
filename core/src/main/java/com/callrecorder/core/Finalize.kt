package com.callrecorder.core

import android.provider.CallLog
import java.io.File

/** Ce que l'on sait du correspondant une fois l'appel terminé. */
data class CallInfo(
    val number: String?,
    val direction: Direction?,
    val contactName: String?,
    /** null : impossible à savoir (permission Contacts refusée). */
    val isContact: Boolean?,
)

/** Décisions prises à la fin d'un appel, séparées du code Android pour être testées. */
object Finalize {
    /** En dessous, l'appel a été raccroché aussitôt décroché : rien d'utile n'a été enregistré. */
    const val MIN_DURATION_MS = 1000L

    fun shouldKeep(fileBytes: Long, durationMs: Long, filter: RecordFilter, info: CallInfo, people: List<Person> = emptyList()): Boolean =
        fileBytes > 0 && durationMs >= MIN_DURATION_MS &&
            filter.keeps(info.isContact, hiddenNumber = info.number == null, number = info.number, people = people)

    /** Fichier de destination ; si le nom est déjà pris (deux appels dans la même seconde), préfixé par l'identifiant. */
    fun destination(dir: File, name: String, id: String): File =
        File(dir, name).let { if (it.exists()) File(dir, "${id.take(6)}_$name") else it }

    /** Sens d'après le journal d'appels ; null si le journal ne permet pas de conclure. */
    fun direction(callLogType: Int?): Direction? = when (callLogType) {
        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> Direction.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> Direction.OUTGOING
        else -> null
    }
}
