package com.franzk.mijo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franzk.mijo.cooking.CookingSession
import com.franzk.mijo.cooking.VoiceCommand
import com.franzk.mijo.data.RecipeBook
import com.franzk.mijo.model.Recipe
import com.franzk.mijo.platform.Host
import com.franzk.mijo.platform.VoiceEngine
import com.franzk.mijo.platform.VoiceStatus

@Composable
fun CookingScreen(
    recipe: Recipe,
    startIndex: Int,
    book: RecipeBook,
    voice: VoiceEngine,
    host: Host,
    onExit: () -> Unit,
) {
    val session = remember(recipe.id) { CookingSession(recipe.steps, startIndex) }
    // Compteur de rafraîchissement : la session n'est pas observable par Compose.
    var revision by remember { mutableIntStateOf(0) }
    val status by voice.status.collectAsState()

    fun apply(command: VoiceCommand) {
        val outcome = session.handle(command)
        if (outcome.moved) {
            if (session.finished) book.clearProgress(recipe.id)
            else book.saveProgress(recipe.id, session.index)
            revision++
        }
        voice.speak(outcome.speech)
    }

    DisposableEffect(recipe.id) {
        var disposed = false
        host.keepScreenOn(true)
        book.saveProgress(recipe.id, session.index)
        if (host.hasMicPermission()) {
            voice.startSession()
            voice.speak(session.currentSpeech())
        } else {
            // La fenêtre de permission interromprait la lecture : on lit après la réponse.
            host.requestMicPermission {
                if (disposed) return@requestMicPermission
                voice.startSession()
                voice.speak(session.currentSpeech())
            }
        }
        onDispose {
            disposed = true
            voice.endSession()
            host.keepScreenOn(false)
        }
    }

    LaunchedEffect(recipe.id) {
        voice.commands.collect { apply(it) }
    }

    // Lire `revision` abonne l'écran aux changements de la session.
    val index = revision.let { session.index }
    val finished = revision.let { session.finished }

    Column(
        Modifier
            .fillMaxSize()
            .background(MijoColors.Cream)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        // En-tête : titre, position, fermeture
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MijoColors.InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (finished) "Terminé" else "Étape ${index + 1} sur ${session.total}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MijoColors.Ink,
                )
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MijoColors.CreamDeep).clickable(onClick = onExit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "Quitter le mode cuisine", tint = MijoColors.Ink)
            }
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { if (finished) 1f else (index + 1f) / session.total },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = MijoColors.Butter,
            trackColor = MijoColors.CreamDeep,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )

        // Étape affichée
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 20.dp)) {
            AnimatedContent(
                targetState = if (finished) -1 else index,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "step",
            ) { idx ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MijoColors.Paper)
                        .verticalScroll(rememberScrollState())
                        .padding(26.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (idx < 0) {
                        Text("🎉", fontSize = 56.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Recette terminée.\nBon appétit !",
                            style = MaterialTheme.typography.displaySmall,
                            color = MijoColors.Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(20.dp))
                        BigButton("Retour à la recette", null, filled = true, onClick = onExit)
                    } else {
                        Text(
                            recipe.steps[idx],
                            fontSize = 28.sp,
                            lineHeight = 38.sp,
                            color = MijoColors.Ink,
                        )
                    }
                }
            }
        }

        VoiceIndicator(status, finished = finished)
        Spacer(Modifier.height(14.dp))

        // Contrôles tactiles équivalents aux commandes vocales
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ControlButton(Icons.Filled.SkipPrevious, "Précédent", enabled = finished || index > 0, modifier = Modifier.weight(1f)) {
                apply(VoiceCommand.Previous)
            }
            ControlButton(Icons.Filled.Replay, "Répète", modifier = Modifier.weight(1f)) {
                apply(VoiceCommand.Repeat)
            }
            ControlButton(Icons.Filled.SkipNext, "Suivant", primary = true, enabled = !finished, modifier = Modifier.weight(1.3f)) {
                apply(VoiceCommand.Next)
            }
        }
    }
}

@Composable
private fun VoiceIndicator(status: VoiceStatus, finished: Boolean) {
    val (icon, text, live) = when (status) {
        VoiceStatus.Listening -> Triple(
            Icons.Filled.Mic,
            if (finished) "J'écoute · « Précédent » pour revenir" else "J'écoute · « Suivant », « Précédent », « Répète »",
            true,
        )
        VoiceStatus.Speaking -> Triple(Icons.Filled.VolumeUp, "Lecture de l'étape…", false)
        VoiceStatus.Preparing -> Triple(Icons.Filled.Mic, "Préparation de l'écoute…", false)
        VoiceStatus.Idle -> Triple(Icons.Filled.MicOff, "Écoute en pause", false)
        VoiceStatus.NeedsPermission -> Triple(Icons.Filled.MicOff, "Micro non autorisé · utilisez les boutons", false)
        is VoiceStatus.Unavailable -> Triple(Icons.Filled.MicOff, status.message, false)
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (live) 0.35f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (live) MijoColors.ButterSoft else MijoColors.CreamDeep)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (live) MijoColors.Butter else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MijoColors.Ink, modifier = Modifier.size(20.dp).alpha(if (live) alpha else 1f))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MijoColors.Ink)
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(84.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (primary) Modifier.background(MijoColors.Ink)
                else Modifier.background(MijoColors.Paper).border(1.dp, MijoColors.CreamDeep, RoundedCornerShape(22.dp))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (primary) MijoColors.Butter else MijoColors.Ink, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (primary) MijoColors.Paper else MijoColors.Ink)
    }
}
