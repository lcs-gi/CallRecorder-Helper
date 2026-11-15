package com.callrecorder.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Liste « appels à conserver » : elle voyage en JSON de l'application vers le Helper. */
class PeopleListTest {
    private val people = listOf(Person("Marie", "+33612345678"), Person(null, "0799999999"))

    @Test fun allerRetourJson() = assertEquals(people, Person.listFromJson(Person.listToJson(people)))

    @Test fun reglagesAvecListe() {
        val s = AppSettings(filter = RecordFilter.EXCEPT_LIST, people = people)
        assertEquals(s, AppSettings.fromJson(s.toJson()))
    }

    @Test fun texteIllisibleOuIncomplet() {
        assertEquals(emptyList<Person>(), Person.listFromJson(null))
        assertEquals(emptyList<Person>(), Person.listFromJson("pas du json"))
        assertEquals(listOf(Person(null, "0612")), Person.listFromJson("""[{"number":"0612"},{"name":"Sans numéro"},42]"""))
    }

    @Test fun ancienneVersionSansListe() {
        // Réglages envoyés par une application plus ancienne : liste vide, mode inchangé.
        val s = AppSettings.fromJson("""{"filter":"CONTACTS"}""")
        assertEquals(RecordFilter.CONTACTS, s.filter)
        assertEquals(emptyList<Person>(), s.people)
    }
}
