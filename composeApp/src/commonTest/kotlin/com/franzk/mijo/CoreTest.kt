package com.franzk.mijo

import com.franzk.mijo.cooking.CookingSession
import com.franzk.mijo.cooking.VoiceCommand
import com.franzk.mijo.cooking.parseVoiceCommand
import com.franzk.mijo.model.Ingredient
import com.franzk.mijo.model.Recipe
import com.franzk.mijo.model.display
import com.franzk.mijo.model.formatQuantity
import com.franzk.mijo.model.matches
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTest {

    @Test
    fun portionsSuiventLExempleDuCahierDesCharges() {
        val ings = listOf(Ingredient("farine", 200.0, "g"), Ingredient("œufs", 2.0), Ingredient("lait", 100.0, "ml"))
        val scaled = ings.map { it.display(4, 6).first }
        assertEquals(listOf("300 g", "3", "150 ml"), scaled)
    }

    @Test
    fun ingredientSansQuantiteInchange() {
        assertEquals("" to "poivre, à volonté", Ingredient("poivre", note = "à volonté").display(4, 6))
    }

    @Test
    fun quantitesLisibles() {
        assertEquals("1,5", formatQuantity(1.5))
        assertEquals("0,33", formatQuantity(1.0 / 3))
        assertEquals("333", formatQuantity(1000.0 / 3))
        assertEquals("12,5", formatQuantity(12.5))
        assertEquals("4", formatQuantity(3.999999))
    }

    @Test
    fun navigationBorneeEtFin() {
        val s = CookingSession(listOf("a", "b", "c"))
        assertFalse(s.handle(VoiceCommand.Previous).moved)
        assertEquals(0, s.index)
        assertTrue(s.handle(VoiceCommand.Next).moved)
        assertFalse(s.handle(VoiceCommand.Repeat).moved)
        assertEquals(1, s.index)
        s.handle(VoiceCommand.Next)
        assertEquals(2, s.index)
        assertTrue(s.handle(VoiceCommand.Next).moved)
        assertTrue(s.finished)
        assertFalse(s.handle(VoiceCommand.Next).moved)
        s.handle(VoiceCommand.Previous)
        assertFalse(s.finished)
        assertEquals(2, s.index)
    }

    @Test
    fun repriseAUneEtape() {
        val s = CookingSession(listOf("a", "b", "c"), startIndex = 2)
        assertEquals("Étape 3. c", s.currentSpeech())
        assertEquals(2, CookingSession(listOf("a", "b", "c"), startIndex = 9).index)
    }

    @Test
    fun commandesVocales() {
        assertEquals(VoiceCommand.Next, parseVoiceCommand("suivant"))
        assertEquals(VoiceCommand.Next, parseVoiceCommand("[unk] suivante"))
        assertEquals(VoiceCommand.Previous, parseVoiceCommand("Précédent"))
        assertEquals(VoiceCommand.Repeat, parseVoiceCommand("répète"))
        assertEquals(VoiceCommand.Repeat, parseVoiceCommand("répéter"))
        assertNull(parseVoiceCommand("[unk]"))
        assertNull(parseVoiceCommand(""))
        assertNull(parseVoiceCommand("tu peux me passer le plat suivant s'il te plaît"))
    }

    @Test
    fun rechercheTitreEtIngredientsSansAccents() {
        val r = Recipe("1", "Crêpes", ingredients = listOf(Ingredient("œufs"), Ingredient("farine")))
        assertTrue(r.matches("crepe"))
        assertTrue(r.matches("oeufs"))
        assertTrue(r.matches("Farine crêpes"))
        assertFalse(r.matches("tomate"))
    }
}
