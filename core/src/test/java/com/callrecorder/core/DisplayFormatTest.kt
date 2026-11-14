package com.callrecorder.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DisplayFormatTest {
    @Test fun initiales() {
        assertEquals("MD", Format.initials("Marie Dupont"))
        assertEquals("JP", Format.initials("Jean-Pierre"))
        assertEquals("JP", Format.initials("jean pierre martin")) // deux lettres au maximum
        assertEquals("É", Format.initials("Élodie"))
        assertEquals("MD", Format.initials("  Marie   Dupont  "))
        assertEquals("P", Format.initials("Plombier 24h/24")) // « 24h » commence par un chiffre : ignoré
        assertEquals("D", Format.initials("Dr. "))
    }

    @Test fun pasDInitialesPourUnNumeroOuDesSymboles() {
        assertNull(Format.initials(null))
        assertNull(Format.initials(""))
        assertNull(Format.initials("+33 6 12 34 56 78"))
        assertNull(Format.initials("☎ ★"))
    }

    @Test fun vitesseDeLecture() {
        assertEquals("1×", Format.speed(1f, Locale.FRENCH))
        assertEquals("2×", Format.speed(2f, Locale.ENGLISH))
        assertEquals("1,25×", Format.speed(1.25f, Locale.FRENCH))
        assertEquals("1.5×", Format.speed(1.5f, Locale.ENGLISH))
        assertEquals("0,75×", Format.speed(0.75f, Locale.FRENCH))
    }

    @Test fun nomDeFichierLimite() {
        val name = Format.fileName("Appel", 0, "x".repeat(200))
        assertTrue(name.length < 80)
        assertTrue(Format.fileName("", 0, null).startsWith("Call_"))
        // Retours à la ligne et tabulations (nom de contact mal saisi) supprimés.
        assertTrue(!Format.fileName("Appel", 0, "Marie\nDupont\t").contains('\n'))
    }

    @Test fun titreDUnEnregistrement() {
        val r = Recording("a", "a.m4a", 0, 0, Direction.INCOMING)
        assertNull(r.title)
        assertEquals("0612", r.copy(number = "0612").title)
        assertEquals("Marie", r.copy(number = "0612", contactName = "Marie").title)
        assertEquals("0612", r.copy(number = "0612", contactName = " ").title)
    }
}
