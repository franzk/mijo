package com.franzk.mijo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franzk.mijo.model.Artwork

/** Visuel d'une recette sans photo : dégradé chaleureux, motif discret et grand emoji. */
@Composable
fun RecipeArt(artwork: Artwork, modifier: Modifier = Modifier, emojiSize: TextUnit = 64.sp) {
    val from = Color(artwork.from)
    val to = Color(artwork.to)
    Box(modifier.background(Brush.linearGradient(listOf(from, to))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension
            drawCircle(Color.White.copy(alpha = 0.18f), radius = r * 0.55f, center = Offset(size.width * 0.85f, size.height * 0.15f))
            drawCircle(Color.White.copy(alpha = 0.12f), radius = r * 0.35f, center = Offset(size.width * 0.1f, size.height * 0.9f))
            drawCircle(Color.Black.copy(alpha = 0.05f), radius = r * 0.25f, center = Offset(size.width * 0.2f, size.height * 0.25f))
        }
        Text(artwork.emoji, fontSize = emojiSize)
    }
}

@Composable
fun RatingStars(
    rating: Int?,
    tint: Color,
    size: Dp = 18.dp,
    onRate: ((Int?) -> Unit)? = null,
) {
    Row {
        for (i in 1..5) {
            val filled = (rating ?: 0) >= i
            Icon(
                if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = if (onRate != null) "Noter $i sur 5" else null,
                tint = if (filled) tint else tint.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(if (onRate != null) size + 14.dp else size)
                    .let { m ->
                        if (onRate == null) m
                        else m.clip(RoundedCornerShape(50)).clickable { onRate(if (rating == i) null else i) }.padding(7.dp)
                    },
            )
        }
    }
}

@Composable
fun Pill(text: String, background: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
