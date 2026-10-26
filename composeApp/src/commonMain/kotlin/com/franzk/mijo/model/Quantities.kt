package com.franzk.mijo.model

import kotlin.math.abs
import kotlin.math.roundToLong

/** Recalcule une quantité pour un nouveau nombre de portions, sans toucher à la recette d'origine. */
fun scaleQuantity(quantity: Double, baseServings: Int, targetServings: Int): Double =
    if (baseServings <= 0) quantity else quantity * targetServings / baseServings

/**
 * Affichage lisible, sans décimales excessives :
 * 3 → « 3 », 1.5 → « 1,5 », 0.333 → « 0,33 », 333.33 → « 333 ».
 */
fun formatQuantity(value: Double): String {
    val rounded = when {
        value >= 100 -> value.roundToLong().toDouble()
        value >= 10 -> (value * 10).roundToLong() / 10.0
        else -> (value * 100).roundToLong() / 100.0
    }
    if (abs(rounded - rounded.roundToLong()) < 1e-9) return rounded.roundToLong().toString()
    var text = rounded.toString()
    if (text.contains('.')) text = text.trimEnd('0').trimEnd('.')
    return text.replace('.', ',')
}

/** « 300 g de farine », « 3 œufs », « Poivre, à volonté » */
fun Ingredient.display(baseServings: Int, targetServings: Int): Pair<String, String> {
    val amount = quantity?.let { q ->
        val value = formatQuantity(scaleQuantity(q, baseServings, targetServings))
        if (unit.isNullOrBlank()) value else "$value $unit"
    }
    val label = if (note.isNullOrBlank()) name else "$name, $note"
    return (amount ?: "") to label
}
