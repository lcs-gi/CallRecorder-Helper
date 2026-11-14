package com.callrecorder.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Échanges entre l'application et le Helper : noms de fichiers, réglages, empreintes de certificat. */
class ContractTest {
    private val id = "3f2b8c1e-7a4d-4e0b-9c6a-1d2e3f4a5b6c"

    @Test
    fun nomsDeFichiersAcceptes() {
        assertTrue(Contract.isSafeIncomingName(Contract.audioName(id)))
        assertTrue(Contract.isSafeIncomingName(Contract.waveName(id)))
    }

    @Test
    fun nomsDeFichiersDangereuxRefuses() {
        listOf(null, "", "../recordings.json", "a/b.m4a", "..m4a", "$id.json", "$id.m4a.part", "x.M4A", "a".repeat(65) + ".m4a")
            .forEach { assertFalse("accepté à tort : $it", Contract.isSafeIncomingName(it)) }
        assertFalse(Contract.isSafeId("../x"))
        assertFalse(Contract.isSafeId(""))
        assertTrue(Contract.isSafeId(id))
    }

    @Test
    fun reglagesAllerRetour() {
        val s = AppSettings(
            autoRecord = false, filter = RecordFilter.CONTACTS, source = AudioSourceOption.MIC,
            quality = AudioQuality.HIGH, autoDeleteDays = 30, notifyAfterCall = false,
            autoSpeaker = true, boostVolume = true,
        )
        assertEquals(s, AppSettings.fromJson(s.toJson()))
    }

    @Test
    fun reglagesIllisiblesOuIncomplets() {
        assertEquals(AppSettings(), AppSettings.fromJson(null))
        assertEquals(AppSettings(), AppSettings.fromJson("pas du json"))
        // Application plus récente que le Helper : valeur inconnue → défaut, le reste est conservé.
        val s = AppSettings.fromJson("""{"auto":false,"source":"FUTURE_SOURCE","quality":"LOW"}""")
        assertFalse(s.autoRecord)
        assertEquals(AudioSourceOption.VOICE_RECOGNITION, s.source)
        assertEquals(AudioQuality.LOW, s.quality)
        assertTrue(s.notifyAfterCall)
    }

    @Test
    fun empreintesNormalisees() {
        val hex = "AB".repeat(32)
        val colons = hex.chunked(2).joinToString(":").lowercase()
        assertEquals(hex, Signatures.normalize(colons))
        assertEquals(setOf(hex, "CD".repeat(32)), Signatures.parseList("$colons, ${"cd".repeat(32)}"))
        // Vide ou tronqué : rien n'est accepté.
        assertTrue(Signatures.parseList("").isEmpty())
        assertTrue(Signatures.parseList("AB:CD").isEmpty())
    }

    @Test
    fun enregistrementTransmisSansPerte() {
        val r = Recording(
            id = id, fileName = "Appel_2026-11-14_15-30-12_Marie.m4a", startedAt = 1_700_000_000_000, durationMs = 61_000,
            direction = Direction.OUTGOING, number = "+33612345678", contactName = "Marie", sizeBytes = 123_456,
            silent = true, audioSource = AudioSourceOption.VOICE_COMMUNICATION,
        )
        assertEquals(r, Recording.fromJson(org.json.JSONObject(r.toJson().toString())))
    }
}
