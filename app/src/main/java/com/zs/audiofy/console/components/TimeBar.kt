/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-11-2025.
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

package com.zs.audiofy.console.components

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.lerp
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.minimumInteractiveComponentSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

private const val TAG = "TimeBar"

// Indeterminate linear indicator transition specs
// Total duration for one cycle
private val SLIDER_HEIGHT = 6.dp
private val DEFAULT_PROGRESS_RANGE = 0f..1.0f
private val INDETERMINATE_ANIM_SPEC = infiniteRepeatable(
    animation = tween<Float>(1800, easing = FastOutSlowInEasing),
    repeatMode = RepeatMode.Restart  // loop continuously
)

// Helpers
private fun DrawScope.drawLinearIndicator(
    strokeWidth: Float,
    color: Color,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val width = size.width
    val height = size.height
    // Start drawing from the vertical center of the stroke
    val yOffset = height / 2

    val isLtr = layoutDirection == LayoutDirection.Ltr
    val barStart = (if (isLtr) startFraction else 1f - endFraction) * width
    val barEnd = (if (isLtr) endFraction else 1f - startFraction) * width

    // if there isn't enough space to draw the stroke caps, fall back to StrokeCap.Butt
    if (strokeCap == StrokeCap.Butt || height > width) {
        // Progress line
        drawLine(color, Offset(barStart, yOffset), Offset(barEnd, yOffset), strokeWidth)
    } else {
        // need to adjust barStart and barEnd for the stroke caps
        val strokeCapOffset = strokeWidth / 2
        val coerceRange = strokeCapOffset..(width - strokeCapOffset)
        val adjustedBarStart = barStart.coerceIn(coerceRange)
        val adjustedBarEnd = barEnd.coerceIn(coerceRange)

        if (abs(endFraction - startFraction) > 0) {
            // Progress line
            drawLine(
                color,
                Offset(adjustedBarStart, yOffset),
                Offset(adjustedBarEnd, yOffset),
                strokeWidth,
                strokeCap,
            )
        }
    }
}

/**
 * Draws a series of horizontally spaced dots along a line, with optional animation
 * controlled by [progress]. Dot count is reduced automatically if available width
 * is too small, and dots are not drawn if only one dot would fit.
 *
 * @param radius Radius of each dot in pixels.
 * @param color Color of the dots.
 * @param rangeX Horizontal range (startX..endX) for drawing dots.
 * @param progress Animation progress (0f..1f) controlling dot positions.
 */
private fun DrawScope.drawLoadingDots(
    radius: Float,          // Radius of each dot
    color: Color,           // Color of the dots
    rangeX: Offset,         // Horizontal range (startX..endX) where dots are allowed
    progress: Float         // Animation progress 0→1 controlling dot movement
) {
    // Initial maximum number of dots to draw
    var dotsToDraw = 4

    // Destructure horizontal range into start and end
    val (startX, endX) = rangeX

    // Available horizontal width for the dots
    val availableWidth = endX - startX

    // Diameter of each dot in pixels
    val dotDiameterPx = radius * 2

    // Reduce dot count if there is not enough available space
    while (true) {
        // Stop drawing if only one or fewer dots remain
        if (dotsToDraw <= 1) return

        // Approximate spacing between dots (can be animated later)
        val dotSpacing = dotDiameterPx

        // Total width required for current number of dots including spacing
        val requiredWidth = dotsToDraw * dotDiameterPx + (dotsToDraw - 1) * dotSpacing

        // Rule: only draw if available width is more than *twice* the required width
        // If not enough space, reduce dot count
        if (availableWidth <= 2 * requiredWidth)
            dotsToDraw--
        else
            break
    }

    // Draw each dot at progress on range.
    repeat(dotsToDraw) { index ->
        // 1) Compute a triangle-wave phase to animate spacing:
        // progress goes 0→1, spacing pulse goes 0→1→0
        val spacingPhase = 1f - abs(progress - 0.5f) * 2f

        // 2) Interpolate spacing based on phase:
        // spacing varies between one dot diameter (ends) → 4×diameter (center)
        val dynamicSpacing = lerp(
            startValue = dotDiameterPx,         // minimum spacing at ends
            endValue = 4 * dotDiameterPx,       // maximum spacing at center
            fraction = spacingPhase
        )

        // 3) Compute horizontal offset for this dot
        val dotOffset = dynamicSpacing * index

        // 4) Normalize offset to 0..1 range relative to total available width
        val dotPhaseOffset = dotOffset / availableWidth

        // 5) Combine animation progress with dot offset to get final animation phase
        val finalPhase = (progress + dotPhaseOffset) % 1f

        // 6) Convert phase into actual x-coordinate for drawing
        val xPos = size.width - finalPhase * availableWidth

        // 7) Draw the dot at computed position, vertically centered
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(xPos, size.height / 2)
        )
    }
}


/*
Source - https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Slider.kt;l=1?q=slider&sq=&ss=androidx%2Fplatform%2Fframeworks%2Fsupport
*/
private fun Modifier.slideOnKeyEvents(
    enabled: Boolean,
    steps: Int,
    valueRange: ClosedFloatingPointRange<Float> = DEFAULT_PROGRESS_RANGE,
    value: Float,
    onValueChangeState: (Float) -> Unit,
    onValueChangeFinishedState: (() -> Unit)?,
    isRtl: Boolean,
): Modifier {
    require(steps >= 0) { "steps should be >= 0" }
    return this.onKeyEvent {
        if (!enabled) return@onKeyEvent false
        when (it.type) {
            KeyEventType.KeyDown -> {
                val rangeLength = abs(valueRange.endInclusive - valueRange.start)
                // When steps == 0, it means that a user is not limited by a step length (delta)
                // when using touch or mouse. But it is not possible to adjust the value
                // continuously when using keyboard buttons - the delta has to be discrete.
                // In this case, 1% of the valueRange seems to make sense.
                val actualSteps = if (steps > 0) steps + 1 else 100
                val delta = rangeLength / actualSteps
                val sign = if (isRtl) -1 else 1

                if (it.key == Key.MoveHome) {
                    onValueChangeState(valueRange.start)
                    return@onKeyEvent true
                } else if (it.key == Key.MoveEnd) {
                    onValueChangeState(valueRange.endInclusive)
                    return@onKeyEvent true
                }
                when (it.key) {
                    Key.DirectionRight -> {
                        onValueChangeState((value + sign * delta).coerceIn(valueRange))
                        return@onKeyEvent true
                    }

                    Key.DirectionLeft -> {
                        onValueChangeState((value - sign * delta).coerceIn(valueRange))
                        return@onKeyEvent true
                    }

                    Key.PageUp -> {
                        val page = (actualSteps / 10).coerceIn(1, 10)
                        onValueChangeState((value + page * delta).coerceIn(valueRange))
                        return@onKeyEvent true
                    }

                    Key.PageDown -> {
                        val page = (actualSteps / 10).coerceIn(1, 10)
                        onValueChangeState((value - page * delta).coerceIn(valueRange))
                        return@onKeyEvent true
                    }

                    else -> return@onKeyEvent false
                }
            }

            KeyEventType.KeyUp -> {
                when (it.key) {
                    Key.DirectionRight,
                    Key.DirectionLeft,
                    Key.MoveHome,
                    Key.MoveEnd,
                    Key.PageUp,
                    Key.PageDown -> {
                        onValueChangeFinishedState?.invoke()
                        return@onKeyEvent true
                    }

                    else -> return@onKeyEvent false
                }
            }

            else -> return@onKeyEvent false
        }
    }
}

private fun Modifier.sliderSemantics(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = DEFAULT_PROGRESS_RANGE,
    steps: Int = 0,
): Modifier {
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    return semantics {
        if (!enabled) disabled()
        setProgress(
            action = { targetValue ->
                var newValue = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                val originalVal = newValue
                val resolvedValue =
                    if (steps > 0) {
                        var distance: Float = newValue
                        for (i in 0..steps + 1) {
                            val stepValue =
                                lerp(
                                    valueRange.start,
                                    valueRange.endInclusive,
                                    i.toFloat() / (steps + 1),
                                )
                            if (abs(stepValue - originalVal) <= distance) {
                                distance = abs(stepValue - originalVal)
                                newValue = stepValue
                            }
                        }
                        newValue
                    } else {
                        newValue
                    }
                // This is to keep it consistent with AbsSeekbar.java: return false if no
                // change from current.
                if (resolvedValue == coerced) {
                    false
                } else {
                    onValueChange(resolvedValue)
                    onValueChangeFinished?.invoke()
                    true
                }
            }
        )
    }
        .progressSemantics(value, valueRange, steps)
}

// handles press and drag and emits different interaction state.
private fun Modifier.slidePressDragGesture(
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    interactionSource: MutableInteractionSource,
    onValueChangeFinished: (() -> Unit),
): Modifier {
    if (!enabled) return this then Modifier
    return pointerInput(Unit) {

        awaitPointerEventScope {
            while (true) {
                val (width, _) = size
                val down = awaitFirstDown()

                // Emit press interaction
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)

                // Press → jump immediately
                val progress =
                    down.position.x.coerceIn(0f, width.toFloat()) / width.toFloat()
                onValueChange(
                    (progress)
                )
                Log.d(TAG, "Down: width:$width| progress: $progress")
                // Drag
                // Emit drag start
                val dragStart = DragInteraction.Start()
                interactionSource.tryEmit(dragStart)
                val id = down.id
                drag(id) { change ->
                    val progress = change.position.x.coerceIn(
                        0f,
                        width.toFloat()
                    ) / width.toFloat()
                    Log.d(TAG, "Drag: $progress")
                    onValueChange(progress)
                    change.consume()
                }
                // Emit drag stop
                interactionSource.tryEmit(DragInteraction.Stop(dragStart))
                Log.d(TAG, "Up: $progress")
                // Emit release interaction
                interactionSource.tryEmit(PressInteraction.Release(press))
                // Finger lifted
                onValueChangeFinished()
            }
        }
    }
}

private val RequiredSizeModifier = Modifier.requiredSizeIn(
minWidth = 20.dp,
minHeight = 8.dp,
)

/**
 * Represents the Slider for Console's PlayerView.
 */
@Composable
fun TimeBar(
    progress: Float,
    secondary: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buffering: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    color: Color = AppTheme.colors.accent
) {
    // val buffering = true
    val interactionSource = interactionSource ?: remember(::MutableInteractionSource)
    // Animates a "focus progress" value in response to interactions.
    // 0f is unfocused, 1f is focused (pressed, hovered, etc.).
    val focusProgress by produceState(initialValue = 0f, key1 = interactionSource) {
        // Animatable is internal to this produceState.
        // Its value will be copied into this@produceState.value during animation.
        val anim = Animatable(value)

        // Reference to the currently running animation coroutine.
        // We cancel it before starting a new one so animations don't overlap.
        var animJob: Job? = null

        // Collect interaction events from the InteractionSource.
        // This runs as long as the composable is in composition.
        interactionSource.interactions.collect { interaction ->

            // Cancel any ongoing animation (e.g., the user hovered then pressed).
            animJob?.cancel()

            // Determine whether the interaction means "focused" or "unfocused".
            val target = when (interaction) {
                is PressInteraction.Press,
                is HoverInteraction.Enter,
                is FocusInteraction.Focus,
                is DragInteraction.Start -> 1f   // focused
                else -> 0f                       // unfocused (Release, Exit, Cancel, Blur)
            }

            Log.d(TAG, "interaction: $target")

            // Launch animation in a new coroutine so we don't block the flow collector.
            animJob = launch {
                anim.animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = LinearEasing
                    )
                ) {
                    // Each animation frame updates produceState's value.
                    // This makes focusProgress update over time.
                    this@produceState.value = anim.value
                }
            }
        }

    }
    // Indeterminate animation for loading dots.
    // Produces a looping value from 0f → 1f that represents the animation phase.
    val indeterminateInfiniteTransition = rememberInfiniteTransition()
    val iProgress by indeterminateInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = INDETERMINATE_ANIM_SPEC
    )
    //
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Animate the secondary progress.
    val secondary by animateFloatAsState(secondary)
    Canvas(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .sliderSemantics(
                progress,
                enabled,
                onValueChange,
                onValueChangeFinished,
                0f..1.0f,
                0,
            )
            .focusable(enabled, interactionSource)
            .slideOnKeyEvents(
                enabled,
                0,
                DEFAULT_PROGRESS_RANGE,
                progress,
                { updatedValue ->
                    onValueChange(updatedValue)
                },
                onValueChangeFinished,
                isRtl
            )
            .slidePressDragGesture(
                enabled,
                onValueChange = onValueChange,
                interactionSource = interactionSource,
                onValueChangeFinished = onValueChangeFinished,
            ),
        onDraw = {
            val (width, height) = size

            // FIXME - Something is causing crash if width is less than 30.dp hence this.
            if (width <= 30.dp.toPx())
                return@Canvas
            val scale = lerp(1f, 1.25f, focusProgress)
            val trackHeightPx = SLIDER_HEIGHT.toPx() * scale
            drawLinearIndicator(trackHeightPx,  color.copy(ContentAlpha.indication))
            // draw secondary progress
            drawLinearIndicator(trackHeightPx, color.copy(0.3f), 0f, secondary)
            // draw the primary progress; since progress is always between 0 and 1 there should be no issue.
            drawLinearIndicator(trackHeightPx, color, 0f, progress)
            // draw the thumb. it is smaller initially than track height by 1dp
            val thumbRadiusPx = (trackHeightPx - 1.dp.toPx()) / 2f
            val thumbCentre = Offset(
                x = (progress * width).coerceIn(thumbRadiusPx, width - thumbRadiusPx),
                y = height / 2f
            )
            val thumbRadius = if (!buffering)
                lerp(thumbRadiusPx, thumbRadiusPx * 2f, focusProgress)
            else {
                /// Double beat pulse driven by iProgress
                val pulse = abs(sin(iProgress * 2 * Math.PI * 1.3f)).toFloat()
                lerp(thumbRadiusPx * 0.8f, thumbRadiusPx * 1.3f, pulse)
            }
            drawCircle(
                androidx.compose.ui.graphics.lerp(if (color.luminance() > 0.5) Color.Black else Color.White, color, fraction = focusProgress),
                radius = thumbRadius,
                center = thumbCentre
            )

            // draw thumb ring if focused.
            if (focusProgress > 0f)
                drawCircle(
                    color.copy(focusProgress),
                    radius = lerp(1f, 3f, focusProgress) * thumbRadiusPx,
                    center = thumbCentre,
                    style = Stroke(1.5.dp.toPx())
                )
            if (buffering)
                drawLoadingDots(
                    thumbRadiusPx,
                    color.copy(0.5f),
                    Offset(
                        secondary * size.width + 2 * thumbRadiusPx,
                        size.width
                    ),
                    iProgress
                )
        }
    )
}