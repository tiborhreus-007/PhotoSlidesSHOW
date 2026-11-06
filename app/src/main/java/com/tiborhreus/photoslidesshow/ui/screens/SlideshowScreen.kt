package com.tiborhreus.photoslidesshow.ui.screens

import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.tiborhreus.photoslidesshow.data.BackgroundStyle
import com.tiborhreus.photoslidesshow.data.MusicConfig
import com.tiborhreus.photoslidesshow.data.MusicMode
import com.tiborhreus.photoslidesshow.data.PhotoItem
import com.tiborhreus.photoslidesshow.data.PlaylistBuilder
import com.tiborhreus.photoslidesshow.data.PlaylistState
import com.tiborhreus.photoslidesshow.data.ScaleMode
import com.tiborhreus.photoslidesshow.data.ScreenOrientation
import com.tiborhreus.photoslidesshow.data.SleepTimer
import com.tiborhreus.photoslidesshow.data.Slide
import com.tiborhreus.photoslidesshow.data.SlideshowSettings
import com.tiborhreus.photoslidesshow.data.SortOrder
import com.tiborhreus.photoslidesshow.data.TransitionType
import com.tiborhreus.photoslidesshow.ui.components.BorderedButton
import com.tiborhreus.photoslidesshow.ui.components.BorderedIconButton
import com.tiborhreus.photoslidesshow.ui.components.findActivity
import com.tiborhreus.photoslidesshow.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun SlideshowScreen(
    playlistState: PlaylistState,
    settings: SlideshowSettings,
    music: MusicConfig,
    onReshuffle: () -> Unit,
    onExit: () -> Unit,
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val view = LocalView.current

    // Celá obrazovka + zákaz zhasnutia displeja + nastavenie orientácie obrazovky
    DisposableEffect(settings.screenOrientation) {
        val activity = context.findActivity()
        val window = activity?.window
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        activity?.requestedOrientation = when (settings.screenOrientation) {
            ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientation.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = prevOrientation
        }
    }

    // Prehrávač hudby (Media3 ExoPlayer) pre lokálne súbory a internetové rádiá
    var isMuted by remember { mutableStateOf(false) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(music) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        when (music.mode) {
            MusicMode.LOCAL -> {
                val list = if (music.shuffle) music.tracks.shuffled() else music.tracks
                if (list.isNotEmpty()) {
                    list.forEach { track ->
                        exoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse(track.uri)))
                    }
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                    exoPlayer.volume = if (music.fadeIn) 0.02f else music.volume
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            }
            MusicMode.RADIO -> {
                if (music.radioUrl.isNotBlank()) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(music.radioUrl)))
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    exoPlayer.volume = if (music.fadeIn) 0.02f else music.volume
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            }
            MusicMode.NONE, MusicMode.EXTERNAL -> Unit
        }
        onDispose {
            exoPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Plynulý nábeh hlasitosti (Fade-In)
    LaunchedEffect(music, isMuted) {
        if (isMuted) {
            exoPlayer.volume = 0f
            return@LaunchedEffect
        }
        if (music.mode == MusicMode.LOCAL || music.mode == MusicMode.RADIO) {
            if (music.fadeIn) {
                val steps = 20
                for (i in 1..steps) {
                    exoPlayer.volume = music.volume * (i / steps.toFloat())
                    delay(120L)
                }
            } else {
                exoPlayer.volume = music.volume
            }
        }
    }

    // Časovač automatického ukončenia (Sleep Timer)
    var remainingSeconds by remember(settings.sleepTimer) {
        mutableLongStateOf(settings.sleepTimer.minutes * 60L)
    }
    LaunchedEffect(settings.sleepTimer) {
        if (settings.sleepTimer != SleepTimer.OFF) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            onExit()
        }
    }

    // Digitálne hodiny
    var clockText by remember { mutableStateOf("") }
    LaunchedEffect(settings.showClock) {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (settings.showClock) {
            clockText = fmt.format(Date())
            delay(10_000L)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val isScreenLandscape = maxWidth >= maxHeight

        when (playlistState) {
            PlaylistState.Idle, PlaylistState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppColors.Accent)
                        Spacer(Modifier.height(16.dp))
                        Text("Pripravujem fotky a analyzujem orientáciu…", color = AppColors.Text)
                    }
                }
            }

            is PlaylistState.Error -> {
                CenterMessage(
                    title = "Chyba pri načítaní fotiek",
                    subtitle = playlistState.message,
                    onExit = onExit,
                )
            }

            is PlaylistState.Empty -> {
                val detail = if (playlistState.totalBeforeFilter > 0) {
                    "Zo zdrojov sa načítalo ${playlistState.totalBeforeFilter} fotiek, ale žiadna nevyhovela zvolenému filtru orientácie alebo obdobia."
                } else {
                    "Vo vybraných zdrojoch sa nenašli žiadne podporované obrázky."
                }
                CenterMessage(
                    title = "Žiadne fotky na zobrazenie",
                    subtitle = detail,
                    onExit = onExit,
                )
            }

            is PlaylistState.Ready -> {
                val slides = remember(
                    playlistState.generation,
                    settings.pairPortraits,
                    isScreenLandscape,
                ) {
                    PlaylistBuilder.buildSlides(
                        photos = playlistState.photos,
                        pairPortraits = settings.pairPortraits && isScreenLandscape,
                    )
                }

                if (slides.isEmpty()) {
                    CenterMessage("Žiadne snímky", "Zoznam snímok je prázdny.", onExit)
                } else {
                    ActiveSlideshowContent(
                        slides = slides,
                        settings = settings,
                        music = music,
                        isMuted = isMuted,
                        onToggleMute = { isMuted = !isMuted },
                        clockText = clockText,
                        remainingSeconds = remainingSeconds,
                        onReshuffle = onReshuffle,
                        onExit = onExit,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSlideshowContent(
    slides: List<Slide>,
    settings: SlideshowSettings,
    music: MusicConfig,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    clockText: String,
    remainingSeconds: Long,
    onReshuffle: () -> Unit,
    onExit: () -> Unit,
) {
    var index by remember(slides) { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    val safeIndex = index.coerceIn(0, (slides.size - 1).coerceAtLeast(0))
    val currentSlide = slides[safeIndex]

    fun nextSlide() {
        if (safeIndex + 1 < slides.size) {
            index = safeIndex + 1
        } else if (settings.loop) {
            if (settings.sortOrder == SortOrder.RANDOM) {
                onReshuffle()
            }
            index = 0
        } else {
            onExit()
        }
    }

    fun prevSlide() {
        index = if (safeIndex - 1 >= 0) safeIndex - 1 else (slides.size - 1).coerceAtLeast(0)
    }

    // Automatické schovanie ovládacieho panelu po 3.5 s nečinnosti
    LaunchedEffect(controlsVisible, isPaused) {
        if (controlsVisible && !isPaused) {
            delay(3500L)
            controlsVisible = false
        }
    }

    // Časovač zobrazenia snímky
    LaunchedEffect(safeIndex, isPaused, settings.displaySeconds, slides) {
        progress = 0f
        if (isPaused) return@LaunchedEffect
        val totalMs = (settings.displaySeconds * 1000L).coerceAtLeast(1000L)
        val stepMs = 40L
        var elapsed = 0L
        while (elapsed < totalMs) {
            delay(stepMs)
            elapsed += stepMs
            progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
        }
        nextSlide()
    }

    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(slides) {
                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
            }
            .pointerInput(slides) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, amount -> dragAccum += amount },
                    onDragEnd = {
                        if (dragAccum < -90f) nextSlide()
                        else if (dragAccum > 90f) prevSlide()
                    },
                )
            },
    ) {
        // Prechod medzi snímkami
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = {
                buildTransition(settings.transition, settings.transitionMs)
            },
            contentKey = { it.key },
            label = "slideTransition",
        ) { slide ->
            SlideViewport(slide = slide, settings = settings)
        }

        // Indikátor priebehu hore
        if (settings.showProgress) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = AppColors.Accent,
                trackColor = Color.White.copy(alpha = 0.16f),
            )
        }

        // Digitálne hodiny a časovač vypnutia v pravom hornom rohu
        if (settings.showClock || settings.sleepTimer != SleepTimer.OFF) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(1.5.dp, AppColors.Border.copy(alpha = 0.7f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (settings.showClock && clockText.isNotEmpty()) {
                        Text(
                            clockText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Text,
                        )
                    }
                    if (settings.sleepTimer != SleepTimer.OFF && remainingSeconds > 0) {
                        val mins = (remainingSeconds / 60).coerceAtLeast(1)
                        Text(
                            "⏱ ${mins} min",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppColors.Accent,
                        )
                    }
                }
            }
        }

        // Informácie o aktuálnej fotke v ľavom dolnom rohu
        if (settings.showInfo) {
            PhotoInfoBadge(
                slide = currentSlide,
                slideNumber = safeIndex + 1,
                totalSlides = slides.size,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .safeDrawingPadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (controlsVisible) 96.dp else 16.dp,
                    ),
            )
        }

        // Ovládací panel s kontrastne ohraničenými tlačidlami
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppColors.Background.copy(alpha = 0.85f),
                border = BorderStroke(2.dp, AppColors.Border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BorderedIconButton(
                        icon = Icons.Filled.SkipPrevious,
                        contentDescription = "Predchádzajúca fotka",
                        translucent = true,
                        onClick = ::prevSlide,
                    )
                    BorderedIconButton(
                        icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Pokračovať" else "Pozastaviť",
                        primary = true,
                        onClick = { isPaused = !isPaused },
                    )
                    BorderedIconButton(
                        icon = Icons.Filled.SkipNext,
                        contentDescription = "Ďalšia fotka",
                        translucent = true,
                        onClick = ::nextSlide,
                    )
                    BorderedIconButton(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = "Zamiešať poradie",
                        translucent = true,
                        onClick = onReshuffle,
                    )
                    if (music.mode == MusicMode.LOCAL || music.mode == MusicMode.RADIO) {
                        BorderedIconButton(
                            icon = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = "Stlmiť hudbu",
                            translucent = true,
                            onClick = onToggleMute,
                        )
                    }
                    BorderedIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Ukončiť prezentáciu",
                        danger = true,
                        translucent = true,
                        onClick = onExit,
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideViewport(slide: Slide, settings: SlideshowSettings) {
    if (slide.photos.size == 2) {
        // Dve fotky na výšku vedľa seba s jemným oddeľovačom
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PhotoFrame(
                photo = slide.photos[0],
                settings = settings,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            PhotoFrame(
                photo = slide.photos[1],
                settings = settings,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    } else {
        PhotoFrame(
            photo = slide.photos.first(),
            settings = settings,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PhotoFrame(
    photo: PhotoItem,
    settings: SlideshowSettings,
    modifier: Modifier = Modifier,
) {
    // Ken Burns plynulý zoom počas zobrazenia
    val zoom = remember(photo.uri) { Animatable(1f) }
    val dirX = remember(photo.uri) { if (photo.uri.hashCode() % 2 == 0) 1f else -1f }
    val dirY = remember(photo.uri) { if (photo.uri.hashCode() % 3 == 0) 1f else -1f }

    LaunchedEffect(photo.uri, settings.kenBurns, settings.displaySeconds) {
        zoom.snapTo(1f)
        if (settings.kenBurns) {
            zoom.animateTo(
                targetValue = 1.09f,
                animationSpec = tween(
                    durationMillis = (settings.displaySeconds * 1000).coerceAtLeast(1500),
                    easing = LinearEasing,
                ),
            )
        }
    }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val frameAspect = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
        val shouldFill = when (settings.scaleMode) {
            ScaleMode.FILL -> true
            ScaleMode.FIT -> false
            ScaleMode.SMART -> {
                if (!photo.hasSize) false
                else {
                    val diff = abs(photo.aspect - frameAspect) / frameAspect.coerceAtLeast(0.1f)
                    diff <= 0.20f
                }
            }
        }

        // Rozmazané pozadie pri zachovaní celej fotky s iným pomerom strán
        if (!shouldFill && settings.background == BackgroundStyle.BLUR) {
            AsyncImage(
                model = photo.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
                    .alpha(0.55f),
            )
        }

        val progressFraction = ((zoom.value - 1f) / 0.09f).coerceIn(0f, 1f)
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.name,
            contentScale = if (shouldFill) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.value
                    scaleY = zoom.value
                    translationX = dirX * progressFraction * 18f
                    translationY = dirY * progressFraction * 12f
                },
        )
    }
}

@Composable
private fun PhotoInfoBadge(
    slide: Slide,
    slideNumber: Int,
    totalSlides: Int,
    modifier: Modifier = Modifier,
) {
    val first = slide.photos.first()
    val dateStr = remember(first.dateTaken) {
        if (first.dateTaken > 0L) {
            SimpleDateFormat("d. M. yyyy", Locale.getDefault()).format(Date(first.dateTaken))
        } else ""
    }
    val orientBadge = when {
        slide.photos.size == 2 -> "2× Na výšku (spojené)"
        first.isPortrait -> "Na výšku (${first.width}×${first.height})"
        first.isLandscape -> "Na šírku (${first.width}×${first.height})"
        else -> ""
    }
    val title = if (slide.photos.size == 2) {
        "${slide.photos[0].name}  •  ${slide.photos[1].name}"
    } else {
        first.name
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.60f),
        border = BorderStroke(1.5.dp, AppColors.Border.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                "$slideNumber / $totalSlides",
                dateStr.takeIf { it.isNotEmpty() },
                orientBadge.takeIf { it.isNotEmpty() },
            ).joinToString("  •  ")
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Accent,
            )
        }
    }
}

@Composable
private fun CenterMessage(
    title: String,
    subtitle: String,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppColors.Surface,
            border = BorderStroke(2.dp, AppColors.Border),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                BorderedButton(text = "Späť na nastavenia", primary = true, onClick = onExit)
            }
        }
    }
}

private fun buildTransition(type: TransitionType, durationMs: Int): ContentTransform {
    val actual = if (type == TransitionType.RANDOM) {
        TransitionType.concrete[Random.nextInt(TransitionType.concrete.size)]
    } else {
        type
    }
    val d = durationMs.coerceIn(150, 3000)

    return when (actual) {
        TransitionType.NONE -> fadeIn(tween(1)) togetherWith fadeOut(tween(1))
        TransitionType.FADE -> fadeIn(tween(d)) togetherWith fadeOut(tween(d))
        TransitionType.SLIDE_H -> (
            slideInHorizontally(tween(d, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(d))
        ) togetherWith (
            slideOutHorizontally(tween(d, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(d))
        )
        TransitionType.SLIDE_V -> (
            slideInVertically(tween(d, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(d))
        ) togetherWith (
            slideOutVertically(tween(d, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(d))
        )
        TransitionType.ZOOM_IN -> (
            scaleIn(tween(d, easing = FastOutSlowInEasing), initialScale = 0.84f) + fadeIn(tween(d))
        ) togetherWith (
            scaleOut(tween(d, easing = FastOutSlowInEasing), targetScale = 1.12f) + fadeOut(tween(d))
        )
        TransitionType.ZOOM_OUT -> (
            scaleIn(tween(d, easing = FastOutSlowInEasing), initialScale = 1.18f) + fadeIn(tween(d))
        ) togetherWith (
            scaleOut(tween(d, easing = FastOutSlowInEasing), targetScale = 0.88f) + fadeOut(tween(d))
        )
        TransitionType.PUSH_FADE -> (
            slideInHorizontally(tween(d, easing = FastOutSlowInEasing)) { it / 3 } +
                scaleIn(tween(d), initialScale = 0.94f) +
                fadeIn(tween(d))
        ) togetherWith (
            slideOutHorizontally(tween(d, easing = FastOutSlowInEasing)) { -it / 3 } +
                scaleOut(tween(d), targetScale = 0.94f) +
                fadeOut(tween(d))
        )
        TransitionType.RANDOM -> fadeIn(tween(d)) togetherWith fadeOut(tween(d))
    }
}
