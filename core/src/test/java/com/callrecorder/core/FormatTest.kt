package com.callrecorder.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormatTest {
    @Test fun duree() {
        assertEquals("0:00", Format.duration(0))
        assertEquals("0:42", Format.duration(42_900))
        assertEquals("12:05", Format.duration(725_000))
        assertEquals("1:02:33", Format.duration(3_753_000))
        assertEquals("0:00", Format.duration(-5))
    }

    @Test fun taille() {
        val units = listOf("o", "Ko", "Mo", "Go")
        assertEquals("512 o", Format.size(512, Locale.FRENCH, units))
        assertEquals("1,5 Ko", Format.size(1536, Locale.FRENCH, units))
        assertEquals("1.5 KB", Format.size(1536, Locale.ENGLISH, listOf("B", "KB", "MB", "GB")))
        assertEquals("200 Mo", Format.size(200L * 1024 * 1024, Locale.FRENCH, units))
    }

    @Test fun nomDeFichierSansCaracteresInterdits() {
        val name = Format.fileName("Appel", 0, "Marie / Dupont: \"pro\"")
        assertTrue(name, name.startsWith("Appel_") && name.endsWith("_Marie_Dupont_pro.m4a"))
        assertFalse(name.contains('/') || name.contains(':') || name.contains('"'))
        assertTrue(Format.fileName("Appel", 0, null).matches(Regex("Appel_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.m4a")))
    }

    @Test fun numerosEquivalents() {
        assertTrue(PhoneNumbers.same("06 12 34 56 78", "+33612345678"))
        assertTrue(PhoneNumbers.same("0033 6 12 34 56 78", "06.12.34.56.78"))
        assertFalse(PhoneNumbers.same("0612345678", "0612345679"))
        assertFalse(PhoneNumbers.same("", "0612345678"))
        assertFalse(PhoneNumbers.same("112", "+33612345112"))
    }

    @Test fun numerosMasques() {
        assertTrue(PhoneNumbers.isHidden(null))
        assertTrue(PhoneNumbers.isHidden(""))
        assertTrue(PhoneNumbers.isHidden("-1"))
        assertTrue(PhoneNumbers.isHidden("-2"))
        assertFalse(PhoneNumbers.isHidden("+33612345678"))
    }

    @Test fun filtreDesAppels() {
        assertTrue(RecordFilter.ALL.keeps(false, hiddenNumber = true))
        assertTrue(RecordFilter.CONTACTS.keeps(true, hiddenNumber = false))
        assertFalse(RecordFilter.CONTACTS.keeps(false, hiddenNumber = false))
        assertFalse(RecordFilter.CONTACTS.keeps(null, hiddenNumber = true))
        // Permission Contacts refusée : dans le doute, on garde.
        assertTrue(RecordFilter.CONTACTS.keeps(null, hiddenNumber = false))
        assertTrue(RecordFilter.UNKNOWN.keeps(false, hiddenNumber = false))
        assertFalse(RecordFilter.UNKNOWN.keeps(true, hiddenNumber = false))
        assertTrue(RecordFilter.UNKNOWN.keeps(null, hiddenNumber = true))
    }
}
