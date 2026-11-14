package com.callrecorder.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Les deux langues restent synchronisées : mêmes clés, mêmes paramètres, aucun texte vide. */
class TranslationsTest {
    private fun load(folder: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$folder/strings.xml"))
        val out = mutableMapOf<String, String>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as org.w3c.dom.Element
            if (e.getAttribute("translatable") != "false") out[e.getAttribute("name")] = e.textContent
        }
        val plurals = doc.getElementsByTagName("item")
        for (i in 0 until plurals.length) {
            val e = plurals.item(i) as org.w3c.dom.Element
            val parent = (e.parentNode as org.w3c.dom.Element).getAttribute("name")
            out["$parent/${e.getAttribute("quantity")}"] = e.textContent
        }
        return out
    }

    private val args = Regex("%\\d\\$[sd]")

    @Test
    fun memesClesEtMemesParametres() {
        val en = load("values")
        val fr = load("values-fr")
        assertTrue("trop peu de textes : ${en.size}", en.size > 30)
        assertEquals("clés manquantes ou en trop", en.keys, fr.keys)
        en.forEach { (k, v) ->
            assertTrue("texte vide : $k", v.isNotBlank() && fr.getValue(k).isNotBlank())
            assertEquals("paramètres différents pour $k", args.findAll(v).map { it.value }.sorted().toList(),
                args.findAll(fr.getValue(k)).map { it.value }.sorted().toList())
        }
    }
}
