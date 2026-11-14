package com.callrecorder.core

import com.callrecorder.core.CallStateTracker.Action
import com.callrecorder.core.CallStateTracker.Companion.IDLE
import com.callrecorder.core.CallStateTracker.Companion.OFFHOOK
import com.callrecorder.core.CallStateTracker.Companion.RINGING
import com.callrecorder.core.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallStateTrackerTest {
    private fun run(vararg states: Int): List<Action> {
        val t = CallStateTracker()
        return states.toList().mapNotNull { t.onState(it) }
    }

    @Test fun appelEntrant() =
        assertEquals(listOf(Action.Start(Direction.INCOMING), Action.Stop), run(IDLE, RINGING, OFFHOOK, IDLE))

    @Test fun appelSortant() =
        assertEquals(listOf(Action.Start(Direction.OUTGOING), Action.Stop), run(IDLE, OFFHOOK, IDLE))

    @Test fun appelManqueOuRefuse() = assertEquals(emptyList<Action>(), run(IDLE, RINGING, IDLE))

    @Test fun doubleAppelResteDansLeMemeEnregistrement() =
        // Un second appel sonne pendant le premier, puis est pris : un seul enregistrement.
        assertEquals(listOf(Action.Start(Direction.OUTGOING), Action.Stop), run(IDLE, OFFHOOK, RINGING, OFFHOOK, IDLE))

    @Test fun etatInitialRepete() {
        val t = CallStateTracker()
        assertNull(t.onState(IDLE))
        assertNull(t.onState(IDLE))
    }

    @Test fun appelSortantApresUnAppelManque() =
        assertEquals(listOf(Action.Start(Direction.OUTGOING), Action.Stop), run(RINGING, IDLE, OFFHOOK, IDLE))
}

class CallStateTrackerEdgeTest {
    @Test fun serviceDemarreEnPleinAppel() {
        // Service activé (ou redémarré) pendant un appel : l'enregistrement démarre quand même.
        val t = CallStateTracker()
        assertEquals(Action.Start(Direction.OUTGOING), t.onState(OFFHOOK))
        assertEquals(Action.Stop, t.onState(IDLE))
    }

    @Test fun deuxAppelsSuccessifs() {
        val t = CallStateTracker()
        val actions = listOf(IDLE, RINGING, OFFHOOK, IDLE, OFFHOOK, IDLE).mapNotNull { t.onState(it) }
        assertEquals(
            listOf(Action.Start(Direction.INCOMING), Action.Stop, Action.Start(Direction.OUTGOING), Action.Stop),
            actions,
        )
    }

    @Test fun etatInconnuIgnore() {
        val t = CallStateTracker()
        assertNull(t.onState(42))
        assertEquals(Action.Start(Direction.OUTGOING), t.onState(OFFHOOK))
    }

    @Test fun inCallSuitLAppel() {
        val t = CallStateTracker()
        t.onState(RINGING); assertEquals(false, t.inCall)
        t.onState(OFFHOOK); assertEquals(true, t.inCall)
        t.onState(IDLE); assertEquals(false, t.inCall)
    }
}
