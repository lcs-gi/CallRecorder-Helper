package com.callrecorder.core

import android.provider.CallLog
import com.callrecorder.core.Direction
import com.callrecorder.core.RecordFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Ce qui est décidé au raccroché : garder ou jeter, nom du fichier, sens de l'appel. */
class FinalizeTest {
    @get:Rule val tmp = TemporaryFolder()

    private val contact = CallInfo("+33612345678", Direction.INCOMING, "Marie", isContact = true)
    private val stranger = CallInfo("+33799999999", Direction.INCOMING, null, isContact = false)
    private val hidden = CallInfo(null, null, null, isContact = false)
    private val noContactsPermission = CallInfo("+33612345678", null, null, isContact = null)

    @Test fun appelNormalConserve() = assertTrue(Finalize.shouldKeep(50_000, 60_000, RecordFilter.ALL, contact))

    @Test fun fichierVideOuAppelTropCourtJete() {
        // MediaRecorder n'a rien écrit (source refusée en cours de route, ou stop() en échec).
        assertFalse(Finalize.shouldKeep(0, 60_000, RecordFilter.ALL, contact))
        // Raccroché aussitôt décroché.
        assertFalse(Finalize.shouldKeep(2_000, 999, RecordFilter.ALL, contact))
        assertTrue(Finalize.shouldKeep(2_000, Finalize.MIN_DURATION_MS, RecordFilter.ALL, contact))
    }

    @Test fun filtreContactsUniquement() {
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.CONTACTS, contact))
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.CONTACTS, stranger))
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.CONTACTS, hidden))
        // Permission Contacts refusée : on ne jette pas un appel qui est peut-être celui d'un contact.
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.CONTACTS, noContactsPermission))
    }

    @Test fun filtreNumerosInconnusUniquement() {
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.UNKNOWN, contact))
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.UNKNOWN, stranger))
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.UNKNOWN, hidden))
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.UNKNOWN, noContactsPermission))
    }

    @Test fun nomDejaPrisNEcrasePasLAncienFichier() {
        val dir = tmp.root
        val name = "Appel_2026-11-14_15-30-12_Marie.m4a"
        assertEquals(dir.resolve(name), Finalize.destination(dir, name, "abcdef123"))
        dir.resolve(name).writeText("ancien")
        val second = Finalize.destination(dir, name, "abcdef123")
        assertEquals("abcdef_$name", second.name)
        assertFalse(second.exists())
    }

    @Test fun sensDepuisLeJournalDAppels() {
        assertEquals(Direction.INCOMING, Finalize.direction(CallLog.Calls.INCOMING_TYPE))
        assertEquals(Direction.INCOMING, Finalize.direction(CallLog.Calls.ANSWERED_EXTERNALLY_TYPE))
        assertEquals(Direction.OUTGOING, Finalize.direction(CallLog.Calls.OUTGOING_TYPE))
        // Manqué, refusé, bloqué, messagerie ou journal illisible : on garde le sens détecté pendant l'appel.
        assertNull(Finalize.direction(CallLog.Calls.MISSED_TYPE))
        assertNull(Finalize.direction(CallLog.Calls.REJECTED_TYPE))
        assertNull(Finalize.direction(CallLog.Calls.VOICEMAIL_TYPE))
        assertNull(Finalize.direction(null))
    }

    // Liste choisie : Marie y figure, enregistrée avec un autre format de numéro.
    private val people = listOf(Person("Marie", "06 12 34 56 78"))

    @Test fun seulementCesPersonnes() {
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.ONLY_LIST, contact, people))
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.ONLY_LIST, stranger, people))
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.ONLY_LIST, hidden, people))
        // Liste vide : rien n'est conservé.
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.ONLY_LIST, contact))
    }

    @Test fun tousSaufCesPersonnes() {
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.EXCEPT_LIST, contact, people))
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.EXCEPT_LIST, stranger, people))
        assertTrue(Finalize.shouldKeep(1, 5_000, RecordFilter.EXCEPT_LIST, hidden, people))
        // Fonctionne même sans la permission Contacts du Helper : seule la comparaison des numéros compte.
        assertFalse(Finalize.shouldKeep(1, 5_000, RecordFilter.EXCEPT_LIST, noContactsPermission, people))
    }
}
