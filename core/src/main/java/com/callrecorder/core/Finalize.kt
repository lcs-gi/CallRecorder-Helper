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

    /** Ligne du journal d'appels : numéro, type ([CallLog.Calls.TYPE]) et heure de début. */
    data class LogEntry(val number: String?, val type: Int, val date: Long)

    /** Un appel du journal commence au plus tôt 2 minutes avant le décroché (sonnerie, numérotation). */
    const val LOG_BEFORE_MS = 120_000L

    /** Et au plus tard un peu après (horloges et délais d'écriture du téléphone). */
    const val LOG_AFTER_MS = 15_000L

    /**
     * Ligne du journal correspondant à l'appel enregistré, commencé à [callStart].
     * Certains téléphones (Samsung) écrivent la ligne d'un appel dès la numérotation : si un nouvel appel
     * est composé juste après avoir raccroché, sa ligne existe déjà. On prend donc la ligne la plus proche
     * du début de l'appel, jamais une ligne nettement postérieure.
     */
    fun pickLogEntry(entries: List<LogEntry>, callStart: Long): LogEntry? =
        entries.filter { it.date in (callStart - LOG_BEFORE_MS)..(callStart + LOG_AFTER_MS) }
            .minByOrNull { kotlin.math.abs(it.date - callStart) }

    /** Sens d'après le journal d'appels ; null si le journal ne permet pas de conclure. */
    fun direction(callLogType: Int?): Direction? = when (callLogType) {
        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> Direction.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> Direction.OUTGOING
        else -> null
    }
}
