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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zs.audiofy.common.AppConfig
import com.zs.audiofy.common.Res
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.vectorResource
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Chip
import com.zs.compose.theme.ChipDefaults
import com.zs.compose.theme.Icon
import com.zs.compose.theme.Preference
import com.zs.compose.theme.TextButton
import com.zs.compose.theme.text.Label
import com.zs.audiofy.common.compose.ContentPadding as CP

context(_: RouteSettings, scope: ColumnScope)
@Composable
fun AboutUs() {
    with(scope){
        // The app version and check for updates.
        val facade = LocalSystemFacade.current
        BaseListItem(
            heading = { Label(textResource(Res.string.version), fontWeight = FontWeight.Bold) },
            subheading = {
                Label(
                    textResource(Res.string.version_info_s, AppConfig.VERSION_NAME)
                )
            },
            footer = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CP.small),
                    content = {
                        TextButton(
                            textResource(Res.string.update_audiofy),
                            onClick = { facade.initiateUpdateFlow(true) })
                        TextButton(
                            textResource(Res.string.join_the_beta),
                            onClick = { facade.launch(Settings.JoinBetaIntent) },
                            enabled = false
                        )
                    }
                )
            },
            leading = {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_release_alert),
                    contentDescription = null
                )
            },
        )

        // Privacy Policy
        Preference(
            text = textResource(Res.string.pref_privacy_policy),
            icon = vectorResource(Res.drawable.ic_privacy_tip),
            modifier = Modifier
                .clip(AppTheme.shapes.medium)
                .clickable { facade.launch(Settings.PrivacyPolicyIntent) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(CP.small)
        ) {
            val colors = ChipDefaults.chipColors(
                backgroundColor = AppTheme.colors.background(1.dp),
                contentColor = AppTheme.colors.accent
            )
            Chip(
                content = { Label(textResource(Res.string.rate_us)) },
                leadingIcon = { Icon(vectorResource(Res.drawable.ic_rate_review_outline), null) },
                onClick = facade::launchAppStore,
                colors = colors,
                shape = AppTheme.shapes.xSmall
            )

            Chip(
                content = { Label(textResource(Res.string.share_app_label)) },
                leadingIcon = { Icon(vectorResource(Res.drawable.ic_share), null) },
                onClick = { facade.launch(Settings.ShareAppIntent) },
                colors = colors,
                shape = AppTheme.shapes.xSmall
            )
        }
    }
}