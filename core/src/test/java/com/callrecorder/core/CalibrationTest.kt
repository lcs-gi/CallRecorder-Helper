package com.callrecorder.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {
    private fun t(source: AudioSourceOption, level: Int, speaker: Boolean = false, boost: Boolean = false, available: Boolean = true) =
        CalibrationTrial(source, speaker, boost, level, available)

    @Test fun meilleureSourceSansHautParleur() {
        val trials = listOf(
            t(AudioSourceOption.VOICE_RECOGNITION, 1_200), t(AudioSourceOption.CAMCORDER, 6_500),
            t(AudioSourceOption.VOICE_CALL, 0, available = false), t(AudioSourceOption.CAMCORDER, 9_000, speaker = true),
        )
        // Bien audible sans haut-parleur : le haut-parleur (moins de deux fois mieux) n'est pas imposé.
        assertEquals(trials[1], Calibration.recommend(trials))
    }

    @Test fun hautParleurSiRienNestAudible() {
        val trials = listOf(t(AudioSourceOption.MIC, 900), t(AudioSourceOption.MIC, 2_000, boost = true), t(AudioSourceOption.MIC, 7_000, speaker = true))
        assertEquals(trials[2], Calibration.recommend(trials))
    }

    @Test fun volumeRenforceSiNettementMieux() {
        val trials = listOf(t(AudioSourceOption.MIC, 1_000), t(AudioSourceOption.MIC, 2_500, boost = true))
        assertEquals(trials[1], Calibration.recommend(trials))
        // Gain trop faible : on garde le réglage simple.
        assertEquals(t(AudioSourceOption.MIC, 1_000), Calibration.recommend(listOf(t(AudioSourceOption.MIC, 1_000), t(AudioSourceOption.MIC, 1_200, boost = true))))
    }

    @Test fun hautParleurDeuxFoisMieux() {
        val trials = listOf(t(AudioSourceOption.MIC, 5_000), t(AudioSourceOption.MIC, 11_000, speaker = true))
        assertEquals(trials[1], Calibration.recommend(trials))
    }

    @Test fun aucunEssaiExploitable() {
        assertNull(Calibration.recommend(emptyList()))
        assertNull(Calibration.recommend(listOf(t(AudioSourceOption.VOICE_CALL, 0, available = false), t(AudioSourceOption.MIC, 0))))
    }

    @Test fun niveauCrêtesPasBruit() {
        // Silence entre les mots (100) et voix (5000) : le niveau suit la voix.
        assertEquals(5_000, Calibration.level(List(14) { 100 } + List(26) { 5_000 }))
        assertEquals(0, Calibration.level(emptyList()))
    }

    @Test fun allerRetourJson() {
        val r = CalibrationResult(listOf(t(AudioSourceOption.CAMCORDER, 6_500, speaker = true, boost = true), t(AudioSourceOption.VOICE_CALL, 0, available = false)), 123L, true)
        assertEquals(r, CalibrationResult.fromJson(r.toJson()))
        assertNull(CalibrationResult.fromJson("pas du json"))
    }

    @Test fun fenetreDeTest() {
        assertTrue(Calibration.active(AppSettings(calibrateUntil = 1_000), now = 999))
        assertFalse(Calibration.active(AppSettings(calibrateUntil = 1_000), now = 1_000))
        assertFalse(Calibration.active(AppSettings(), now = 0))
    }
}
