package com.example.c001apk.compose.ui.component.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BasicListItem(
    modifier: Modifier = Modifier,
    headlineText: String? = null,
    supportingText: String? = null,
    leadingImageVector: ImageVector? = null,
    leadingPainter: Painter? = null,
    leadingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    if (leadingText != null && headlineText == null) {
        SmallTitle(text = leadingText, modifier = modifier)
        return
    }
    PreferenceCard(modifier) {
        BasicComponent(
            title = headlineText,
            summary = supportingText,
            enabled = enabled,
            holdDownState = holdDownState,
            onClick = onClick,
            startAction =
                if (leadingImageVector != null || leadingPainter != null) {
                    {
                        if (leadingImageVector != null)
                            Icon(
                                leadingImageVector,
                                null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.size(24.dp),
                            )
                        else if (leadingPainter != null)
                            Image(
                                leadingPainter,
                                null,
                                modifier = Modifier.size(24.dp).squircleClip(6.dp),
                            )
                    }
                } else null,
            endActions = {
                if (trailingContent != null) trailingContent()
                else if (onClick != null)
                    Icon(
                        MiuixIcons.ChevronForward,
                        null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
            },
        )
    }
}
