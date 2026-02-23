/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zs.audiofy.R
import com.zs.audiofy.common.AccentColorPolicy
import com.zs.audiofy.common.NightMode
import com.zs.audiofy.common.Res
import com.zs.audiofy.common.compose.preference
import com.zs.audiofy.common.vectorResource
import com.zs.compose.foundation.textArrayResource
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.DropDownPreference
import com.zs.compose.theme.SliderPreference
import com.zs.compose.theme.SwitchPreference
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import kotlin.math.roundToInt
import com.zs.audiofy.settings.RouteSettings as RS

private const val CONTENT_TYPE_PREF = "preference"
private const val TAG = "Preferences"

context(_: RS)
fun LazyListScope.preferences(viewState: SettingsViewState) {
    // General
    item(contentType = RS.CONTENT_TYPE_HEADER) {
        Header(
            textResource(Res.string.general),
            style = AppTheme.typography.title3,
            color = AppTheme.colors.accent,
            contentPadding = RS.HeaderPadding
        )
    }
    // Recycle Bin
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(Res.string.pref_enable_trash_can),
            checked = viewState.trashCanEnabled,
            onCheckedChange = { viewState.trashCanEnabled = it },
            icon = vectorResource(Res.drawable.ic_recycling),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.TopTileShape),
        )
    }
    // Legacy Artwork Method
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(Res.string.pref_fetch_artwork_from_media_store),
            checked = viewState.preferCachedThumbnails,
            onCheckedChange = { viewState.preferCachedThumbnails = it },
            icon = vectorResource(Res.drawable.ic_art_track),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape),
        )
    }
    // Exclude Track Duration
    // The duration from which below tracks are excluded from the library.
    item(contentType = CONTENT_TYPE_PREF) {
        SliderPreference(
            text = textResource(Res.string.pref_minimum_track_length),
            value = viewState.minTrackLengthSecs.toFloat(),
            onRequestChange = { viewState.minTrackLengthSecs = it.toInt() },
            valueRange = 0f..100f,
            steps = 9,
            icon = vectorResource(Res.drawable.ic_straighten),
            preview = {
                Label(
                    text = textResource(Res.string.postfix_s_d, it.roundToInt()),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .wrapContentSize(Alignment.Center)
                )
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape),
        )
    }

    // Labs Mode
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(Res.string.pref_is_labs_mode_on_s, "Long press seek"),
            checked = viewState.isLabsModeOn,
            onCheckedChange = { viewState.isLabsModeOn = it },
            icon = vectorResource(Res.drawable.ic_science),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.BottomTileShape),
        )
    }

    // File grouping
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(Res.string.pref_enable_file_grouping),
            checked = viewState.isFileGroupingEnabled,
            onCheckedChange = { viewState.isFileGroupingEnabled = it },
            icon = vectorResource(Res.drawable.ic_sort),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.BottomTileShape),
        )
    }


    // Appearance
    item(contentType = RS.CONTENT_TYPE_HEADER) {
        Header(
            textResource(Res.string.appearance),
            style = AppTheme.typography.title3,
            color = AppTheme.colors.accent,
            contentPadding = RS.HeaderPadding
        )
    }
    // Night Mode Strategy
    // The strategy to use for night mode.
    item(contentType = CONTENT_TYPE_PREF) {
        val strategy by preference(Settings.NIGHT_MODE)
        val entries = textArrayResource(Res.array.pref_night_mode_entries)
        DropDownPreference(
            text = textResource(Res.string.pref_app_theme_s, entries[strategy.ordinal]),
            value = strategy,
            icon = vectorResource(Res.drawable.ic_light_mode),
            entries = entries,
            onRequestChange = { viewState.set(Settings.NIGHT_MODE, it) },
            values = NightMode.values(),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.TopTileShape)
        )
    }
    // Splash Anim
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            checked = viewState.isSplashAnimWaitEnabled,
            text = textResource(Res.string.pref_enable_splash_anim_wait),
            onCheckedChange = { should: Boolean ->
                viewState.isSplashAnimWaitEnabled = should
            },
            icon = vectorResource(Res.drawable.ic_animation),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Widget <=> Console Transition
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            checked = viewState.isWidgetToConsoleTransitionEnabled,
            text = textResource(Res.string.pref_is_widget_to_console_transition_enabled),
            onCheckedChange = { should: Boolean ->
                viewState.isWidgetToConsoleTransitionEnabled = should
            },
            icon = vectorResource(Res.drawable.ic_auto_awesome_motion),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Acrylic effect
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            checked = viewState.enabledBackgroundBlur,
            text = textResource(Res.string.pref_acrylic_effect),
            onCheckedChange = { should: Boolean ->
                viewState.enabledBackgroundBlur = should
            },
            icon = vectorResource(Res.drawable.ic_blur_on),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    item(contentType = CONTENT_TYPE_PREF) {
        val use by preference(Settings.USE_ACCENT_IN_NAV_BAR)
        SwitchPreference(
            checked = use,
            text = textResource(Res.string.pref_accent_nav),
            onCheckedChange = { should: Boolean ->
                viewState.set(Settings.USE_ACCENT_IN_NAV_BAR, should)
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Colorization Strategy
    item(contentType = CONTENT_TYPE_PREF) {
        val colorizationStrategy by preference(Settings.COLORIZATION_STRATEGY)
        SwitchPreference(
            checked = colorizationStrategy == AccentColorPolicy.WALLPAPER,
            text = textResource(Res.string.pref_colorization_strategy),
            onCheckedChange = { should: Boolean ->
                val strategy =
                    if (should) AccentColorPolicy.WALLPAPER else AccentColorPolicy.DEFAULT
                viewState.set(Settings.COLORIZATION_STRATEGY, strategy)
            },
            icon = vectorResource(Res.drawable.ic_imagesearch_roller),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }
    // App font scale
    // The font scale to use for the app if -1 is used, the system font scale is used.
    item(contentType = CONTENT_TYPE_PREF) {
        SliderPreference(
            value = viewState.fontScale,
            text = textResource(Res.string.pref_font_scale),
            valueRange = 0.75f..1.5f,
            steps = 14,   // steps=(max−min)stepSize−1/ (2.0 - 0.7) / 0.1 - 1 =  12 steps
            icon = vectorResource(Res.drawable.ic_format_size),
            preview = {
                Label(
                    text = if (it <= 0.75f)
                        textResource(Res.string.system)
                    else
                        textResource(Res.string.postfix_x_f, it),
                    fontWeight = FontWeight.Bold
                )
            },
            onRequestChange = { value: Float ->
                val newValue = if (value <= 0.75f) -1f else value
                viewState.fontScale = newValue
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Grid Item Multiplier
    // The multiplier increases/decreases the size of the grid item from 0.6 to 2f
    item(contentType = CONTENT_TYPE_PREF) {
        SliderPreference(
            value = viewState.gridItemSizeMultiplier,
            text = textResource(Res.string.pref_grid_item_size_multiplier),
            valueRange = 0.8f..1.6f,
            steps = 15, // (2.0 - 0.7) / 0.1 = 13 steps
            icon = vectorResource(Res.drawable.ic_grid_view),
            preview = {
                Label(
                    text = textResource(Res.string.postfix_x_f, it),
                    fontWeight = FontWeight.Bold
                )
            },
            onRequestChange = { value: Float ->
                viewState.gridItemSizeMultiplier = value
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }
    // Translucent System Bars
    // Whether System Bars are rendered as translucent or Transparent.
    item(contentType = CONTENT_TYPE_PREF) {
        val translucentSystemBars by preference(Settings.TRANSPARENT_SYSTEM_BARS)
        SwitchPreference(
            checked = translucentSystemBars,
            text = textResource(Res.string.pref_translucent_system_bars),
            onCheckedChange = { should: Boolean ->
                viewState.set(Settings.TRANSPARENT_SYSTEM_BARS, should)
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.BottomTileShape)
        )
    }

    // Playback
    item(contentType = RS.CONTENT_TYPE_HEADER) {
        Header(
            textResource(Res.string.playback),
            style = AppTheme.typography.title3,
            color = AppTheme.colors.accent,
            contentPadding = RS.HeaderPadding
        )
    }
    // Use Inbuilt Audio FX
    // Whether to use inbuilt audio effects or inApp.
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(Res.string.pref_use_inbuilt_audio_effects),
            checked = viewState.inAppAudioEffectsEnabled,
            onCheckedChange = { viewState.inAppAudioEffectsEnabled = it},
            icon = vectorResource(Res.drawable.ic_tune),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.TopTileShape)
        )
    }
    // FAB player long press behaviour
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(Res.string.pref_fab_player_tap_behaviour),
            checked = !viewState.fabLongPressLaunchConsole,
            onCheckedChange = { viewState.fabLongPressLaunchConsole = !it},
            icon = vectorResource(Res.drawable.ic_touch_app),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Texture View/Surface view
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(Res.string.pref_use_surface_view_video_rendering),
            checked = viewState.isSurfaceViewVideoRenderingPreferred,
            onCheckedChange = { viewState.isSurfaceViewVideoRenderingPreferred = it},
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.BottomTileShape)
        )
    }
}