package com.example.c001apk.compose.ui.component.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Small, attributed AOSP vector assets for community actions without a miuix equivalent.
internal inline fun communityIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> Unit,
): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = autoMirror).apply(block).build()

internal inline fun ImageVector.Builder.communityPath(block: PathBuilder.() -> Unit) =
    path(fill = SolidColor(Color.Black), pathBuilder = block)
