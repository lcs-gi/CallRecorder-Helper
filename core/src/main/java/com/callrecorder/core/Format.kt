package com.callrecorder.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    /** « 0:42 », « 12:05 », « 1:02:33 ». */
    fun duration(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, s) else "%d:%02d".format(Locale.ROOT, m, s)
    }

    /** « 1,2 Mo » / « 1.2 MB » selon [locale]. */
    fun size(bytes: Long, locale: Locale, units: List<String>): String {
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        val num = if (i == 0 || v >= 100) "%.0f".format(locale, v) else "%.1f".format(locale, v)
        return "$num ${units[i]}"
    }

    /** Nom de fichier lisible et sûr : « Appel_2026-11-14_15-30_Marie_Dupont.m4a ». */
    fun fileName(prefix: String, startedAt: Long, who: String?): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date(startedAt))
        val safe = who?.let { sanitize(it) }?.takeIf { it.isNotEmpty() }
        return listOfNotNull(sanitize(prefix).ifEmpty { "Call" }, date, safe).joinToString("_") + ".m4a"
    }

    /** Vitesse de lecture : « 1× », « 1,25× » (fr), « 1.25× » (en). */
    fun speed(s: Float, locale: Locale): String =
        java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(locale)).format(s.toDouble()) + "×"

    /**
     * Initiales d'un contact pour l'avatar : « Marie Dupont » → « MD », « Jean-Pierre » → « JP ».
     * null s'il n'y a pas de nom exploitable (numéro, symboles) : l'avatar affiche alors une icône.
     */
    fun initials(name: String?): String? =
        name?.split(' ', '-', '_', '.')
            ?.mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() }?.takeIf { it.isLetter() } }
            ?.take(2)
            ?.joinToString("") { it.uppercase() }
            ?.takeIf { it.isNotEmpty() }

    fun sanitize(s: String): String =
        s.trim()
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "")
            .replace(Regex("\\s+"), "_")
            .take(40)
}

object PhoneNumbers {
    /** Chiffres seulement (et « + » en tête), pour comparer des numéros saisis différemment. */
    fun normalize(n: String?): String {
        if (n.isNullOrBlank()) return ""
        val t = n.trim()
        val digits = t.filter { it.isDigit() }
        return if (t.startsWith("+")) "+$digits" else digits
    }

    /** Même numéro, avec ou sans indicatif (06… et +336… ; 0033… et +33…). Comparaison sur les 9 derniers chiffres. */
    fun same(a: String?, b: String?): Boolean {
        val x = normalize(a).trimStart('+')
        val y = normalize(b).trimStart('+')
        if (x.isEmpty() || y.isEmpty()) return false
        if (x == y) return true
        val n = 9
        return x.length >= n && y.length >= n && x.takeLast(n) == y.takeLast(n)
    }

    /** Numéro masqué ou inconnu (journal d'appels : vide, « -1 », « -2 »…). */
    fun isHidden(n: String?): Boolean = normalize(n).trimStart('+').let { it.isEmpty() || n?.trim()?.startsWith("-") == true }
}
