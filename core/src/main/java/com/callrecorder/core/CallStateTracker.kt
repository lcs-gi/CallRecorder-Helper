package com.callrecorder.core

import com.callrecorder.core.Direction

/**
 * Transforme les états de téléphonie (repos / sonnerie / décroché) en début et fin d'enregistrement.
 * Sonnerie puis décroché = appel entrant ; décroché directement = appel sortant.
 * Un double appel (mise en attente) reste dans le même enregistrement.
 */
class CallStateTracker {
    sealed interface Action {
        data class Start(val direction: Direction) : Action
        data object Stop : Action
    }

    private var last = IDLE
    private var ringing = false
    var inCall = false
        private set

    fun onState(state: Int): Action? {
        if (state == last) return null
        val previous = last
        last = state
        return when (state) {
            RINGING -> { if (!inCall) ringing = true; null }
            OFFHOOK -> if (!inCall) {
                inCall = true
                Action.Start(if (ringing || previous == RINGING) Direction.INCOMING else Direction.OUTGOING)
            } else null
            IDLE -> {
                ringing = false
                if (inCall) { inCall = false; Action.Stop } else null // appel manqué ou refusé : rien
            }
            else -> null
        }
    }

    companion object {
        // Valeurs de TelephonyManager.CALL_STATE_*.
        const val IDLE = 0
        const val RINGING = 1
        const val OFFHOOK = 2
    }
}
