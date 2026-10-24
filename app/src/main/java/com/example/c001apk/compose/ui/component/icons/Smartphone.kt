package com.example.c001apk.compose.ui.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons

/** A modern handset silhouette with a camera cutout and gesture bar. */
val MiuixIcons.Smartphone: ImageVector
    get() = smartphone

private val smartphone by lazy {
    ImageVector.Builder("Smartphone", 24.dp, 24.dp, 24f, 24f)
        .apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 2f)
                horizontalLineTo(15f)
                curveTo(16.66f, 2f, 17.5f, 2.84f, 17.5f, 4.5f)
                verticalLineTo(19.5f)
                curveTo(17.5f, 21.16f, 16.66f, 22f, 15f, 22f)
                horizontalLineTo(9f)
                curveTo(7.34f, 22f, 6.5f, 21.16f, 6.5f, 19.5f)
                verticalLineTo(4.5f)
                curveTo(6.5f, 2.84f, 7.34f, 2f, 9f, 2f)
                close()
                moveTo(10.5f, 4.6f)
                horizontalLineTo(13.5f)
                moveTo(10.5f, 19.5f)
                horizontalLineTo(13.5f)
            }
        }
        .build()
}
