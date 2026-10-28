/*
 * Material Icons notifications_outlined, Copyright Google LLC.
 * Licensed under the Apache License, Version 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 * Converted from SVG to a Compose vector; see docs/THIRD_PARTY_ICONS.md.
 */
package com.example.c001apk.compose.ui.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal val NotificationIcon: ImageVector by lazy {
    ImageVector.Builder("NotificationsOutlined", 24.dp, 24.dp, 24f, 24f)
        .apply {
            addPath(
                PathParser().parsePathString(
                    "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z",
                ).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
}
