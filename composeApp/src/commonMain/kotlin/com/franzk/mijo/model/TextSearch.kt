package com.franzk.mijo.model

private val accents = mapOf(
    'à' to 'a', 'â' to 'a', 'ä' to 'a', 'á' to 'a',
    'ç' to 'c',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'î' to 'i', 'ï' to 'i', 'í' to 'i',
    'ô' to 'o', 'ö' to 'o', 'ó' to 'o',
    'ù' to 'u', 'û' to 'u', 'ü' to 'u', 'ú' to 'u',
    'ÿ' to 'y', 'ñ' to 'n',
)

/** Minuscules, sans accents, « œ » → « oe » : pour comparer sans se soucier de la saisie. */
fun normalize(text: String): String = buildString {
    for (c in text.lowercase()) {
        when (c) {
            'œ' -> append("oe")
            'æ' -> append("ae")
            else -> append(accents[c] ?: c)
        }
    }
}

/** Recherche par titre et par ingrédients ; tous les mots saisis doivent être trouvés. */
fun Recipe.matches(query: String): Boolean {
    val words = normalize(query).split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return true
    val haystack = normalize(title + " " + ingredients.joinToString(" ") { it.name })
    return words.all { haystack.contains(it) }
}
