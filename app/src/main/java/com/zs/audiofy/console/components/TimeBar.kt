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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.LinearProgressIndicator
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults

/**
 * Represents the Slider for Console's PlayerView.
 */
@Composable
fun TimeBar(
    progress: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = AppTheme.colors.accent
) {
    if (progress == -1f)
        return LinearProgressIndicator(
            color = accent,
            backgroundColor = accent.copy(ContentAlpha.indication),
            modifier = modifier
        )
    // FIXME: This is a temporary workaround.
    //  Problem:
    //  The Slider composable uses BoxWithConstraints internally. When used within a ConstraintLayout
    //  with width Dimension.fillToConstraints, it behaves unexpectedly. This workaround addresses the issue.
    //  Remove this workaround once the underlying issue is resolved.
    var width by remember { mutableIntStateOf(0) }
    Box(modifier.onSizeChanged() {
        width = it.width
    }) {
        Slider(
            progress,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.width(with(LocalDensity.current) { width.toDp() }),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                disabledThumbColor = accent,
                disabledActiveTrackColor = accent
            )
        )
    }
}