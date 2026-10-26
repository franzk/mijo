package com.franzk.mijo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franzk.mijo.data.RecipeBook
import com.franzk.mijo.model.Recipe
import com.franzk.mijo.model.formatDuration
import com.franzk.mijo.model.matches

enum class SortOrder(val label: String) { Title("Nom"), Rating("Note"), Duration("Durée") }

/** État de la recherche, conservé quand on revient d'une fiche. */
class CatalogState {
    var query by mutableStateOf("")
    var favoritesOnly by mutableStateOf(false)
    var tags by mutableStateOf(setOf<String>())
    var sort by mutableStateOf(SortOrder.Title)

    val isFiltering: Boolean get() = query.isNotBlank() || favoritesOnly || tags.isNotEmpty()
}

@Composable
fun CatalogScreen(book: RecipeBook, state: CatalogState, onOpen: (String) -> Unit) {
    val all = book.recipes
    val results = all
        .filter { it.matches(state.query) }
        .filter { !state.favoritesOnly || it.favorite }
        .filter { r -> state.tags.all { it in r.tags } }
        .let { list ->
            when (state.sort) {
                SortOrder.Title -> list.sortedBy { it.title.lowercase() }
                SortOrder.Rating -> list.sortedWith(compareByDescending<Recipe> { it.rating ?: 0 }.thenBy { it.title.lowercase() })
                SortOrder.Duration -> list.sortedBy { it.totalMinutes }
            }
        }
    val inProgress = all.filter { book.progressOf(it.id) != null }

    LazyColumn(
        Modifier.fillMaxSize().background(MijoColors.Cream),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Text("Mon carnet", style = MaterialTheme.typography.displaySmall, color = MijoColors.Ink)
                Text(
                    "${all.size} recettes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MijoColors.Muted,
                )
                Spacer(Modifier.height(16.dp))
                SearchRow(state)
            }
        }
        item { TagTabs(book.allTags, state) }

        if (!state.isFiltering) {
            if (inProgress.isNotEmpty()) {
                item { SectionTitle("En cours") }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        inProgress.forEach { r ->
                            InProgressCard(r, (book.progressOf(r.id) ?: 0) + 1) { onOpen(r.id) }
                        }
                    }
                }
            }
            item { SectionTitle("À la une") }
            item { FeaturedCarousel(results, onOpen) }
            item { SectionTitle("Toutes les recettes") }
        } else {
            item { Spacer(Modifier.height(8.dp)) }
        }

        if (results.isEmpty()) {
            item { EmptyResults() }
        }
        items(results, key = { it.id }) { r ->
            RecipeRow(r, Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { onOpen(r.id) }
        }
    }
}

@Composable
private fun SearchRow(state: CatalogState) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(MijoColors.Paper)
                .border(1.dp, MijoColors.CreamDeep, RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text("Recette ou ingrédient…", color = MijoColors.Muted, style = MaterialTheme.typography.bodyLarge)
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = { state.query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MijoColors.Ink),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.query.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close, "Effacer",
                    tint = MijoColors.InkSoft,
                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(50)).clickable { state.query = "" },
                )
            } else {
                Icon(Icons.Filled.Search, null, tint = MijoColors.Ink, modifier = Modifier.size(22.dp))
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.SwapVert, "Trier", tint = MijoColors.Ink)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                SortOrder.entries.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Trier par ${s.label.lowercase()}",
                                fontWeight = if (s == state.sort) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = { state.sort = s; menu = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun TagTabs(tags: List<String>, state: CatalogState) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab("♥ Favoris", state.favoritesOnly) { state.favoritesOnly = !state.favoritesOnly }
        tags.forEach { tag ->
            Tab(tag, tag in state.tags) {
                state.tags = if (tag in state.tags) state.tags - tag else state.tags + tag
            }
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MijoColors.Ink else MijoColors.Muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .height(2.dp)
                .width(28.dp)
                .background(if (selected) MijoColors.Ink else Color.Transparent, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MijoColors.Ink,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
    )
}

@Composable
private fun FeaturedCarousel(recipes: List<Recipe>, onOpen: (String) -> Unit) {
    if (recipes.isEmpty()) return
    val pager = rememberPagerState { recipes.size }
    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(horizontal = 56.dp),
        pageSpacing = 14.dp,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        val r = recipes[page]
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable { onOpen(r.id) },
        ) {
            RecipeArt(r.artwork, Modifier.fillMaxSize(), emojiSize = 96.sp)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.45f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.35f),
                    )
                )
            )
            Column(Modifier.padding(18.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    r.ingredients.take(4).joinToString(", ") { it.name.replaceFirstChar { c -> c.uppercaseChar() } } + ".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill("⏱ ${formatDuration(r.totalMinutes)}", Color.Black.copy(alpha = 0.35f), Color.White)
                if (r.favorite) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Favorite, "Favori", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun InProgressCard(r: Recipe, step: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MijoColors.ButterSoft)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeArt(r.artwork, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), emojiSize = 26.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(r.title, style = MaterialTheme.typography.titleMedium, color = MijoColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Étape $step sur ${r.steps.size}", style = MaterialTheme.typography.bodyMedium, color = MijoColors.InkSoft)
        }
    }
}

@Composable
fun RecipeRow(r: Recipe, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MijoColors.Paper)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeArt(r.artwork, Modifier.width(104.dp).fillMaxHeight(), emojiSize = 40.sp)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleMedium, color = MijoColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = MijoColors.Muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatDuration(r.totalMinutes), style = MaterialTheme.typography.bodyMedium, color = MijoColors.InkSoft)
                if (r.rating != null) {
                    Spacer(Modifier.width(12.dp))
                    RatingStars(r.rating, tint = MijoColors.Ink, size = 14.dp)
                }
            }
        }
        if (r.favorite) {
            Icon(
                Icons.Filled.Favorite, "Favori",
                tint = MijoColors.Danger,
                modifier = Modifier.padding(end = 18.dp).size(22.dp),
            )
        }
    }
}

@Composable
private fun EmptyResults() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🥄", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Aucune recette ne correspond", style = MaterialTheme.typography.titleMedium, color = MijoColors.Ink)
        Text(
            "Essayez un autre mot ou retirez un filtre.",
            style = MaterialTheme.typography.bodyMedium,
            color = MijoColors.Muted,
        )
    }
}
