/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.console

import android.app.Activity
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ScreenLockRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.twotone.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.zs.audiofy.R
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.WindowStyle
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.LottieAnimatedIcon
import com.zs.audiofy.common.compose.VideoSurface
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.collectAsState
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.compose.resize
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.common.compose.timer
import com.zs.audiofy.console.components.TimeBar
import com.zs.audiofy.effects.RouteAudioFx
import com.zs.audiofy.settings.AppConfig
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.WindowSize
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.SinglePaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.VerticalTwoPaneStrategy
import com.zs.compose.theme.menu.DropDownMenu
import com.zs.compose.theme.menu.DropDownMenuItem
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import kotlinx.coroutines.delay
import com.zs.audiofy.common.compose.ContentPadding as CP
import com.zs.audiofy.common.compose.rememberAnimatedVectorPainter as AnimVectorPainter
import com.zs.audiofy.common.icons.MoreHort
import com.zs.compose.foundation.thenIf

object RouteConsole : Route {
    // Component IDs
    const val ID_PLAYING_INDICATOR = "_playing_indicator"
    const val ID_BTN_COLLAPSE = "_btn_collapse"
    const val ID_ARTWORK = "_artwork"
    const val ID_TITLE = "_title"
    const val ID_SUBTITLE = "_subtitle"
    const val ID_EXTRA_INFO = "_extra_info"
    const val ID_SHUFFLE = "_shuffle"
    const val ID_BTN_REPEAT_MODE = "_btn_repeat_mode"
    const val ID_BTN_SKIP_PREVIOUS = "_btn_skip_previous"
    const val ID_BTN_PLAY_PAUSE = "_play_pause"
    const val ID_BTN_SKIP_TO_NEXT = "_skip_next"
    const val ID_SEEK_BAR = "_seek_bar"
    const val ID_VIDEO_SURFACE = "_video_surface"
    const val ID_BACKGROUND = "_background"
    const val ID_SCRIM = "_scrim"
    const val ID_BTN_RESIZE_MODE = "_resize_mode"
    const val ID_BTN_ROTATION_LOCK = "_rotation_lock"
    const val ID_BTN_QUEUE = "_queue"
    const val ID_BTN_SLEEP_TIMER = "_sleep_timer"
    const val ID_BTN_PLAYBACK_SPEED = "_playback_speed"
    const val ID_BTN_EQUALIZER = "_equalizer"
    const val ID_BTN_MEDIA_INFO = "_media_info"
    const val ID_BTN_LIKED = "_liked"
    const val ID_BTN_MORE = "_more"
    const val ID_BTN_LOCK = "_lock"
    const val ID_CAPTIONS = "_captions"
    const val ID_BANNER_AD = "_banner_ad"
    const val ID_CUES = "_cues"

    const val VISIBILITY_AUTO_HIDE_DELAY = 5_000L

    // Controller Visibility States
    const val VISIBLE_NONE = 0         // Fully hidden
    const val VISIBLE_NONE_LOCKED = 1  // Hidden + locked
    const val VISIBLE_LOCKED_SEEK = 3  // Locked, seek bar only (⚠️ duplicate of VISIBLE_SEEK)
    const val VISIBLE_LOCKED_LOCK = 4  // Locked, only lock icon shown
    const val VISIBLE_SEEK = 5         // Auto-hides, seek bar stays
    const val VISIBLE = 6              // Visible, auto-hides after timeout
    const val VISIBLE_ALWAYS = 7       // Always visible (default for audio)


    // Represents different dialogs to be shown
    private const val SHOW_NONE = 0
    private const val SHOW_TIMER = 1
    private const val SHOW_SPEED = 2
    private const val SHOW_MEDIA_INFO = 3
    private const val SHOW_MORE = 4
    private const val SHOW_MEDIA_CONFIG = 5


    private const val TAG = "Console"

    /** Represents [NowPlaying] default state in [Console] */
    private val NonePlaying = NowPlaying(null, null)
    private val COLOR_BACKGROUND = Color(0xFF0E0E0F)

    /** A short-hand   */
    private fun Modifier.key(value: String) = layoutId(value).thenIf(AppConfig.isWidgetToConsoleTransitionEnabled){
        sharedElement(value)
    }
    private val DefaultAnimSpecs = tween<Float>()

    // Represents the shadow around the tef Cue.
    private val SCRIM_STYLE = Brush.verticalGradient(
        0f to Color.Black,                     // Top: solid black
        0.1f to Color.Black.copy(alpha = 0.5f),// Slightly lower: semi-transparent black
        0.2f to Color.Transparent,             // Transition to fully transparent
        0.8f to Color.Transparent,             // Stays transparent until near the bottom
        0.9f to Color.Black.copy(alpha = 0.5f),// Near bottom: semi-transparent black again
        1f to Color.Black                      // Bottom: solid black
    )

    // Represents the shapes of content/details in different configurations.
    private val PrimaryHorizontal = RoundedCornerShape(topEndPercent = 8, bottomEndPercent = 8)
    private val PrimaryVertical = RoundedCornerShape(bottomStartPercent = 8, bottomEndPercent = 8)
    private val SecondaryVertical = RoundedCornerShape(topStartPercent = 8, topEndPercent = 8)
    private val SecondaryHorizontal =
        RoundedCornerShape(topStartPercent = 8, bottomStartPercent = 8)
    private val ArtworkShape = RoundedCornerShape(12)

    // background styles
    const val BG_STYLE_AUTO = 0
    const val BG_STYLE_DARK = 1
    const val BG_STYLE_AUTO_ACRYLIC = 2
    const val BG_STYLE_DARK_ACRYLIC = 3

    // playbutton styles
    const val PLAY_BTN_STYLE_SIMPLE = 0
    const val PLAY_BTN_STYLE_OUTLINED = 1

    @Composable
    operator fun invoke(viewState: ConsoleViewState) {
        // Describe content of primary pane.
        val facade = LocalSystemFacade.current
        val navController = LocalNavController.current

        var showViewOf by remember { mutableIntStateOf(SHOW_NONE) }
        var showQueue by remember { mutableStateOf(false) }
        val visibility = viewState.visibility

        // BackHandler
        val onNavigateBack = onBack@{
            if (showViewOf != SHOW_NONE || showQueue) {
                showViewOf = SHOW_NONE
                showQueue = false
                return@onBack
            }
            // Consume request if locked.
            if (viewState.visibility == VISIBLE_NONE_LOCKED) {
                viewState.emit(VISIBLE_LOCKED_LOCK, true)
                return@onBack
            }

            // Check if the activity has orientation lock enabled - unlock it.
            if ((facade as Activity).isOrientationLocked) {
                facade.toggleRotationLock()
                return@onBack
            }
            // restore system bars
            // When the composable is disposed (e.g., navigating away),
            // revert the system bar appearance and visibility to their default automatic states.
            facade.style =
                facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_AUTO + WindowStyle.FLAG_SYSTEM_BARS_VISIBILITY_AUTO
            navController.navigateUp()
        }
        BackHandler(onBack = onNavigateBack)

        // Primary
        val state by viewState.state.collectAsState(default = NonePlaying)
        var titleTextSize by remember { mutableIntStateOf(16) }
        // TODO - Maybe allow users to set background using pref.
        val background = if (state.isVideo) BG_STYLE_DARK else BG_STYLE_AUTO_ACRYLIC
        val content: @Composable () -> Unit = {
            // Background
            val isVideo = state.isVideo
            Background(
                artwork = state.artwork,
                style = background,
                modifier = Modifier.layoutId(ID_BACKGROUND),
            )

            // Video
            val enabled = visibility >= VISIBLE
            val onColor = LocalContentColor.current
            if (isVideo) {
                var scale by remember { mutableStateOf(ContentScale.Fit) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .key(ID_VIDEO_SURFACE)
                        .handlePlayerGestures(viewState),
                    content = {
                        VideoSurface(
                            provider = viewState.getVideoProvider(),
                            keepScreenOn = state.playWhenReady,
                            modifier = Modifier
                                .resize(scale, state.videoSize),
                            typeSurfaceView = AppConfig.isSurfaceViewVideoRenderingPreferred
                        )
                    }
                )

                // Cues
                val cues by viewState.cues.collectAsState(null)
                /*val cManager = remember {
                    facade.getDeviceService<CaptioningManager>(Context.CAPTIONING_SERVICE)
                }*/
                Cue(
                    provider = { cues ?: "" },
                    modifier = Modifier.layoutId(ID_CUES),
                    color = Color.White,
                )

                // Scrim
                Spacer(
                    modifier = Modifier
                        .layoutId(ID_SCRIM)
                        .background(SCRIM_STYLE)
                )
                // Resize Mode
                IconButton(
                    icon = if (scale == ContentScale.Fit) Icons.Outlined.Fullscreen else Icons.Outlined.FitScreen,
                    contentDescription = null,
                    onClick = {
                        scale =
                            if (scale == ContentScale.Fit) ContentScale.Crop else ContentScale.Fit
                    },
                    modifier = Modifier.layoutId(ID_BTN_RESIZE_MODE),
                    enabled = enabled
                )
                // Lock
                IconButton(
                    icon = Icons.Outlined.Lock,
                    contentDescription = null,
                    onClick = { viewState.emit(if (visibility == VISIBLE_LOCKED_LOCK) VISIBLE else VISIBLE_LOCKED_LOCK) },
                    modifier = Modifier.layoutId(ID_BTN_LOCK),
                    enabled = visibility >= VISIBLE_LOCKED_LOCK
                )
            }

            // Collapse
            val accent = if (isVideo) onColor else AppTheme.colors.accent
            TonalIconButton(
                icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                onClick = onNavigateBack,
                enabled = enabled,
                border = AppTheme.colors.shine,
                contentDescription = null,
                modifier = Modifier.key(ID_BTN_COLLAPSE)
            )

            // Playing bars.
            LottieAnimatedIcon(
                R.raw.playback_indicator,
                isPlaying = state.playing,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = CP.xSmall)
                    .key(ID_PLAYING_INDICATOR),
                tint = accent
            )

            // Title
            Box(
                modifier = Modifier
                    .key(ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        text = state.title ?: stringResource(id = R.string.unknown),
                        fontSize = titleTextSize.sp,// Maybe Animate
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.marque(Int.MAX_VALUE)
                    )
                }
            )

            // Subtitle
            Label(
                text = state.subtitle ?: "",
                style = AppTheme.typography.label3,
                modifier = Modifier.key(ID_SUBTITLE),
                color = onColor.copy(ContentAlpha.medium)
            )

            // Extra-info
            val chronometer = state.chronometer
            ExtraInfo(
                color = onColor.copy(ContentAlpha.medium),
                modifier = Modifier.key(ID_EXTRA_INFO),
                provider = {
                    buildString {
                        val elapsed = chronometer.elapsed
                        val fPos =
                            if (elapsed == Long.MIN_VALUE) "N/A" else DateUtils.formatElapsedTime(
                                elapsed / 1000
                            )
                        val duration = state.duration
                        val fDuration =
                            if (duration == Remote.TIME_UNSET) "N/A" else DateUtils.formatElapsedTime(
                                duration / 1000
                            )
                        append(
                            "$fPos / $fDuration (${state.speed}x)"
                        )
                    }
                }
            )

            // Slider
            val bufferedProgress by produceState(-1f) {
                while (true) {
                    value = viewState.getBufferedPct()
                    delay(1000)
                }
            }
            TimeBar(
                progress = chronometer.progress(state.duration),
                secondary = bufferedProgress,
                onValueChange = {
                    if (isVideo) viewState.emit(VISIBLE_SEEK)
                    val mills = (it * state.duration).toLong()
                    chronometer.raw = mills
                },
                onValueChangeFinished = {
                    if (isVideo) viewState.emit(VISIBLE_NONE)
                    val progress = chronometer.elapsed / state.duration.toFloat()
                    viewState.seekTo(progress)
                },
                modifier = Modifier.key(ID_SEEK_BAR),
                enabled = state.duration > 0 && visibility >= VISIBLE_SEEK && state.state != Remote.PLAYER_STATE_IDLE,
                buffering = state.state == Remote.PLAYER_STATE_BUFFERING,
                color = accent
            )

            // Shuffle
            LottieAnimatedButton(
                id = R.raw.lt_shuffle_on_off,
                onClick = { viewState.shuffle(!state.shuffle) },
                atEnd = state.shuffle,
                progressRange = 0f..0.8f,
                scale = 1.5f,
                contentDescription = null,
                tint = if (state.shuffle) accent else onColor.copy(ContentAlpha.disabled),
                modifier = Modifier.key(ID_SHUFFLE),
                enabled = enabled
            )

            // Skip to next
            IconButton(
                onClick = viewState::skipToNext,
                icon = Icons.Outlined.KeyboardDoubleArrowRight,
                contentDescription = null,
                enabled = enabled && state.isNextAvailable, // add- logic
                modifier = Modifier.key(ID_BTN_SKIP_TO_NEXT)
            )

            // Skip to Prev
            IconButton(
                onClick = viewState::skipToPrev,
                icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                contentDescription = null,
                enabled = enabled && state.isPrevAvailable,
                modifier = Modifier.key(ID_BTN_SKIP_PREVIOUS)
            )

            // Repeat Mode
            IconButton(
                onClick = viewState::cycleRepeatMode,
                content = {
                    val mode = state.repeatMode
                    Icon(
                        painter = AnimVectorPainter(
                            R.drawable.avd_repeat_more_one_all,
                            mode == Remote.REPEAT_MODE_ALL
                        ),
                        contentDescription = null,
                        tint = onColor.copy(if (mode == Remote.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
                    )
                },
                modifier = Modifier.key(ID_BTN_REPEAT_MODE),
                enabled = enabled
            )

            // Play Button
            PlayButton(
                onClick = viewState::togglePlay,
                isPlaying = state.playing,
                style = if (isVideo) PLAY_BTN_STYLE_SIMPLE else PLAY_BTN_STYLE_OUTLINED,
                enabled = enabled,
                modifier = Modifier.key(ID_BTN_PLAY_PAUSE)
            )

            // Rotation
            IconButton(
                icon = Icons.Outlined.ScreenLockRotation,
                contentDescription = null,
                onClick = { (facade as Activity).toggleRotationLock() },
                enabled = enabled,
                modifier = Modifier.layoutId(ID_BTN_ROTATION_LOCK)
            )

            // Queue
            IconButton(
                icon = Icons.Outlined.Queue,
                contentDescription = null,
                onClick = { showQueue = !showQueue },
                enabled = enabled,
                modifier = Modifier.layoutId(ID_BTN_QUEUE)
            )

            // Favourite
            LottieAnimatedButton(
                R.raw.lt_twitter_heart_filled_unfilled,
                onClick = viewState::toggleLike,
                animationSpec = tween(800),
                atEnd = state.favourite, // if fav
                contentDescription = null,
                progressRange = 0.13f..1.0f,
                scale = 3.5f,
                tint = accent,
                enabled = enabled,
                modifier = Modifier.layoutId(ID_BTN_LIKED)
            )

            // Speed
            IconButton(
                icon = Icons.Outlined.Speed,
                contentDescription = null,
                onClick = { showViewOf = SHOW_SPEED },
                enabled = enabled,
                modifier = Modifier.layoutId(ID_BTN_PLAYBACK_SPEED)
            )

            // Timer
            IconButton(
                onClick = {
                    if (state.sleepAt != Remote.TIME_UNSET)
                        viewState.sleepAt(Remote.TIME_UNSET)
                    else showViewOf = SHOW_TIMER
                },
                modifier = Modifier.layoutId(ID_BTN_SLEEP_TIMER),
                content = {
                    if (state.sleepAt == Remote.TIME_UNSET)
                        return@IconButton Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null
                        )
                    val remaining by timer(state.sleepAt)
                    Label(
                        text = DateUtils.formatElapsedTime(remaining / 1000L),
                        style = AppTheme.typography.label3,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.accent
                    )
                }
            )

            // Equalizer
            IconButton(
                icon = Icons.Outlined.Tune,
                contentDescription = null,
                enabled = enabled,
                modifier = Modifier.layoutId(ID_BTN_EQUALIZER),
                onClick = {
                    if (!AppConfig.inAppAudioEffectsEnabled)
                        facade.launchEqualizer(0)
                    else
                        navController.navigate(RouteAudioFx())
                },
            )

            // Info
            IconButton(
                icon = Icons.TwoTone.Info,
                contentDescription = null,
                onClick = {},
                enabled = enabled,
                modifier = Modifier.layoutId(ID_BTN_MEDIA_INFO)
            )

            // More
            IconButton(
                onClick = { showViewOf = SHOW_MORE },
                modifier = Modifier.layoutId(ID_BTN_MORE),
                content = {
                    //
                    Icon(imageVector = Icons.Outlined.MoreHort, contentDescription = null)

                    // Menu
                    if (showViewOf == SHOW_MEDIA_CONFIG)
                        MediaConfigDialog(viewState) {
                            showViewOf = SHOW_NONE
                        }

                    DropDownMenu(
                        expanded = showViewOf == SHOW_MORE,
                        onDismissRequest = { showViewOf = SHOW_NONE },
                        content = {
                            // Remove
                            DropDownMenuItem(
                                stringResource(id = R.string.remove),
                                icon = Icons.Outlined.Remove,
                                onClick = {
                                    val key = state.data
                                    if (key != null)
                                        viewState.remove(key)
                                    showViewOf = SHOW_NONE
                                }
                            )

                            // Delete
                            DropDownMenuItem(
                                stringResource(id = R.string.delete),
                                icon = Icons.Outlined.Delete,
                                onClick = {
                                    val key = state.data
                                    if (key != null)
                                        viewState.delete(key, facade as Activity)
                                    showViewOf = SHOW_NONE
                                }
                            )

                            // Media Config


                            DropDownMenuItem(
                                title = "Media Config.",
                                icon = Icons.Default.Tune,
                                onClick = {
                                    showViewOf = SHOW_MEDIA_CONFIG
                                }
                            )
                        }
                    )
                }
            )

            if (!state.isVideo)
                Artwork(
                    uri = state.artwork,
                    modifier = Modifier.key(ID_ARTWORK),
                    border = 0.5.dp,
                    shape = ArtworkShape,
                    shadow = 12.dp
                )
        }

        // Layout
        // Compute Two pane strategy
        val clazz = LocalWindowSize.current
        val insets = WindowInsets.systemBars
        val strategy = when {
            !showQueue || clazz.height < Category.Medium && clazz.width <= Category.Medium -> SinglePaneStrategy
            clazz.width < clazz.height -> VerticalTwoPaneStrategy(0.35f)
            else -> HorizontalTwoPaneStrategy(0.55f)
        }

        // Layout
        TwoPane(
            modifier = Modifier.thenIf(AppConfig.isWidgetToConsoleTransitionEnabled){ sharedBounds(ID_BACKGROUND)},
            strategy = strategy,
            containerColor = COLOR_BACKGROUND,
            spacing = CP.normal,
            secondary = {
                val insets = when {
                    strategy is VerticalTwoPaneStrategy -> insets.only(WindowInsetsSides.Bottom)
                    else -> insets.only(WindowInsetsSides.End + WindowInsetsSides.Top)
                }
                val shape = when (strategy) {
                    SinglePaneStrategy -> RectangleShape
                    is HorizontalTwoPaneStrategy -> SecondaryHorizontal
                    else -> SecondaryVertical
                }
                Queue(viewState, shape, insets)
            },
            primary = {
                BoxWithConstraints {
                    val insets = when {
                        strategy is VerticalTwoPaneStrategy -> insets.only(WindowInsetsSides.Top).toDpRect
                        else -> insets.toDpRect
                    }
                    // Compute constraints
                    val isVideo = state.isVideo
                    // compute key
                    var key = maxWidth.hashCode()
                    key = 31 * key + maxHeight.hashCode()
                    key = 31 * key + isVideo.hashCode()
                    key = 31 * key + state.state.hashCode()
                    key = 31 * key + insets.hashCode()
                    key = 31 * key + visibility.hashCode()
                    val constraints = remember(key) {
                        // Update system bar style (auto vs hidden) depending on controller visibility
                        facade.style += when (visibility) {
                            VISIBLE, VISIBLE_ALWAYS -> WindowStyle.FLAG_SYSTEM_BARS_VISIBILITY_AUTO
                            else -> WindowStyle.FLAG_SYSTEM_BARS_HIDDEN
                        }

                        // emit new value for visibilty.
                        when {
                            !isVideo && visibility != VISIBLE_ALWAYS -> viewState.emit(VISIBLE_ALWAYS)
                            isVideo && !state.playing && visibility != VISIBLE_ALWAYS -> viewState.emit(VISIBLE_ALWAYS, true)
                            visibility == VISIBLE_LOCKED_LOCK -> viewState.emit(VISIBLE_NONE_LOCKED, true)
                            visibility == VISIBLE_LOCKED_SEEK && state.state != Remote.PLAYER_STATE_BUFFERING -> viewState.emit(VISIBLE_LOCKED_LOCK, true)
                            visibility == VISIBLE -> viewState.emit(VISIBLE_NONE, true)
                        }

                        // Determine which controls to hide
                        val excluded = when (visibility) {
                            VISIBLE_ALWAYS, VISIBLE -> if (!isVideo) null else null // No exclusions when visible
                            VISIBLE_NONE, VISIBLE_NONE_LOCKED -> emptyArray() // Hide everything
                            VISIBLE_LOCKED_LOCK -> arrayOf(ID_BTN_LOCK, ID_SCRIM) // Show only lock button
                            VISIBLE_SEEK, VISIBLE_LOCKED_SEEK -> arrayOf(
                                ID_SEEK_BAR,
                                ID_EXTRA_INFO,
                                ID_SCRIM
                            ) // Show seek + info
                            else -> error("Invalid visibility $visibility provided.") // Defensive check
                        }
                        val clazz = WindowSize(maxWidth, maxHeight)
                        // Compute the new ConstraintSet for layout adjustments
                        val c = calculateConstraintSet(clazz, insets, isVideo, excluded)
                        titleTextSize = c.titleTextSize
                        return@remember c
                    }
                    val onColor = when (background) {
                        BG_STYLE_DARK -> Color.SignalWhite
                        else -> AppTheme.colors.onBackground
                    }
                    CompositionLocalProvider(LocalContentColor provides onColor) {
                        ConstraintLayout(
                            constraints.constraints,
                            animateChangesSpec = DefaultAnimSpecs,
                            content = content,
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize()
                                .then(
                                    when (strategy) {
                                        SinglePaneStrategy -> Modifier
                                        is HorizontalTwoPaneStrategy -> Modifier.clip(
                                            PrimaryHorizontal
                                        )

                                        else -> Modifier.clip(PrimaryVertical)
                                    }
                                ),
                        )
                    }
                }
            }
        )

        // Update system bars style.
        SideEffect {
            facade.style = when (background) {
                BG_STYLE_DARK -> facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_DARK
                else -> facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_AUTO
            }
        }
        //
        when (showViewOf) {
            SHOW_SPEED -> PlaybackSpeed(true, viewState.playbackSpeed) { newValue ->
                if (newValue == -1f) {
                    showViewOf = SHOW_NONE
                    return@PlaybackSpeed
                }
                val fValue = (facade as Activity).getString(R.string.msg_playback_speed_updated_f, newValue)
                facade.showToast(fValue)
                viewState.playbackSpeed = newValue
            }
            SHOW_TIMER -> SleepTimer(true) { mills ->
                if (mills == -1L) {
                    showViewOf = SHOW_NONE
                    return@SleepTimer
                }
                viewState.sleepAt(mills)
                showViewOf = SHOW_NONE
            }
        }
    }
}