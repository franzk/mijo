package com.franzk.mijo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franzk.mijo.data.RecipeBook
import com.franzk.mijo.model.Recipe
import com.franzk.mijo.model.display
import com.franzk.mijo.model.formatDuration

private val OnGlass = Color(0xFFFFFDF2)
private val OnGlassSoft = Color(0xCCFFFDF2)

@Composable
fun RecipeScreen(
    recipe: Recipe,
    book: RecipeBook,
    onClose: () -> Unit,
    onCook: (startIndex: Int) -> Unit,
) {
    var servings by remember(recipe.id) { mutableStateOf(recipe.servings) }
    val progress = book.progressOf(recipe.id)

    Box(Modifier.fillMaxSize().background(MijoColors.Ink)) {
        RecipeArt(recipe.artwork, Modifier.fillMaxWidth().height(420.dp), emojiSize = 120.sp)
        Box(
            Modifier.fillMaxWidth().height(420.dp).background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 0.3f to Color.Transparent)
            )
        )

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(300.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(MijoColors.Glass)
                    .padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
                RecipeHeader(recipe, book)
                Spacer(Modifier.height(18.dp))
                Durations(recipe)
                if (!recipe.description.isNullOrBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(recipe.description, style = MaterialTheme.typography.bodyLarge, color = OnGlassSoft)
                }
                Spacer(Modifier.height(26.dp))
                IngredientsSection(recipe, servings) { servings = it }
                Spacer(Modifier.height(26.dp))
                StepsSection(recipe)
                if (!recipe.tips.isNullOrBlank()) {
                    Spacer(Modifier.height(26.dp))
                    Text("Astuces", style = MaterialTheme.typography.titleLarge, color = OnGlass)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        recipe.tips,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnGlassSoft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MijoColors.GlassChip)
                            .padding(16.dp),
                    )
                }
                // Laisse la place aux boutons fixés en bas
                Spacer(Modifier.height(if (progress != null) 170.dp else 110.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }

        RoundIcon(
            Icons.Filled.Close, "Fermer",
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
            onClick = onClose,
        )

        CookActions(
            stepIndex = progress,
            total = recipe.steps.size,
            modifier = Modifier.align(Alignment.BottomCenter),
            onStart = { onCook(0) },
            onResume = { onCook(progress ?: 0) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeHeader(recipe: Recipe, book: RecipeBook) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            recipe.title,
            style = MaterialTheme.typography.headlineSmall,
            color = OnGlass,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (recipe.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (recipe.favorite) "Retirer des favoris" else "Ajouter aux favoris",
            tint = if (recipe.favorite) MijoColors.Butter else OnGlass,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { book.toggleFavorite(recipe.id) }
                .padding(9.dp),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RatingStars(recipe.rating, tint = MijoColors.Butter, size = 18.dp) { book.setRating(recipe.id, it) }
        Spacer(Modifier.width(6.dp))
        Text(
            if (recipe.rating == null) "Pas encore notée" else "Ma note",
            style = MaterialTheme.typography.bodyMedium,
            color = OnGlassSoft,
        )
    }
    if (recipe.tags.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recipe.tags.forEach { Pill(it, MijoColors.GlassChip, OnGlass) }
        }
    }
}

@Composable
private fun Durations(recipe: Recipe) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DurationBox("Préparation", recipe.prepMinutes, Modifier.weight(1f))
        DurationBox("Cuisson", recipe.cookMinutes, Modifier.weight(1f))
        DurationBox("Repos", recipe.restMinutes, Modifier.weight(1f))
    }
}

@Composable
private fun DurationBox(label: String, minutes: Int, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(formatDuration(minutes), style = MaterialTheme.typography.titleMedium, color = OnGlass)
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnGlassSoft)
    }
}

@Composable
private fun IngredientsSection(recipe: Recipe, servings: Int, onServings: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Ingrédients", style = MaterialTheme.typography.titleLarge, color = OnGlass, modifier = Modifier.weight(1f))
        RoundIcon(Icons.Filled.Remove, "Moins de portions", small = true, enabled = servings > 1) { onServings(servings - 1) }
        Text(
            "$servings pers.",
            style = MaterialTheme.typography.titleMedium,
            color = OnGlass,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
        RoundIcon(Icons.Filled.Add, "Plus de portions", small = true, enabled = servings < 99) { onServings(servings + 1) }
    }
    if (servings != recipe.servings) {
        Text(
            "Quantités adaptées · recette d'origine pour ${recipe.servings}",
            style = MaterialTheme.typography.bodyMedium,
            color = MijoColors.Butter,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    recipe.ingredients.forEach { ing ->
        val (amount, label) = ing.display(recipe.servings, servings)
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OnGlass,
                modifier = Modifier.weight(1f),
            )
            Text(amount, style = MaterialTheme.typography.bodyLarge, color = OnGlassSoft)
        }
    }
}

@Composable
private fun StepsSection(recipe: Recipe) {
    Text("Préparation", style = MaterialTheme.typography.titleLarge, color = OnGlass)
    Spacer(Modifier.height(12.dp))
    recipe.steps.forEachIndexed { i, step ->
        Row(Modifier.padding(vertical = 8.dp)) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(MijoColors.Butter),
                contentAlignment = Alignment.Center,
            ) {
                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MijoColors.Ink)
            }
            Spacer(Modifier.width(14.dp))
            Text(step, style = MaterialTheme.typography.bodyLarge, color = OnGlass, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CookActions(
    stepIndex: Int?,
    total: Int,
    modifier: Modifier,
    onStart: () -> Unit,
    onResume: () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.25f to MijoColors.Ink))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (stepIndex == null) {
            BigButton("Commencer la recette", Icons.Filled.PlayArrow, filled = true, onClick = onStart)
        } else {
            BigButton("Reprendre · étape ${stepIndex + 1} sur $total", Icons.Filled.PlayArrow, filled = true, onClick = onResume)
            BigButton("Recommencer depuis le début", null, filled = false, onClick = onStart)
        }
    }
}

@Composable
fun BigButton(label: String, icon: ImageVector?, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (filled) Modifier.background(MijoColors.Butter)
                else Modifier.border(1.5.dp, OnGlassSoft, RoundedCornerShape(18.dp))
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (filled) MijoColors.Ink else OnGlass)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (filled) MijoColors.Ink else OnGlass)
    }
}

@Composable
fun RoundIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(if (small) 36.dp else 46.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (enabled) 0.35f else 0.15f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = OnGlass.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(if (small) 20.dp else 24.dp))
    }
}
