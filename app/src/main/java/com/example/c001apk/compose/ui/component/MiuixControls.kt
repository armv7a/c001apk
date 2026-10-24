package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun MiuixActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 35.dp,
        minWidth = 35.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors =
            if (LocalDialogPrimaryAction.current) ButtonDefaults.buttonColorsPrimary()
            else ButtonDefaults.buttonColors(),
        content = content,
    )
}

/** Header slot for screens whose title contains a search input or an animated profile. */
@Composable
fun MiuixHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
    color: Color = LocalBarColor.current,
) {
    top.yukonga.miuix.kmp.basic.Surface(color = color) {
        Row(
            modifier
                .windowInsetsPadding(windowInsets)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon()
            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                MiuixTheme(
                    textStyles = MiuixTheme.textStyles.copy(main = MiuixTheme.textStyles.title3),
                ) {
                    title()
                }
            }
            actions()
        }
    }
}

@Composable
fun MiuixMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    WindowListPopup(
        show = expanded,
        onDismissRequest = onDismissRequest,
        popupModifier = modifier,
        alignment = alignment,
        onDismissFinished = onDismissFinished,
    ) {
        ListPopupColumn(content)
    }
}

/** Transparent overflow button with miuix press feedback and an anchored native popup. */
@Composable
fun MiuixMoreMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        // The popup reads this parent's bounds, so its animation starts at the actual button.
        Box(Modifier.padding(4.dp)) {
            IconButton(
                onClick = { onExpandedChange(true) },
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = Color.Transparent,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = "更多操作",
                    modifier = Modifier.size(17.5.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            MiuixMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                alignment = PopupPositionProvider.Align.End,
                content = content,
            )
        }
    }
}

@Composable
fun MiuixMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    CompositionLocalProvider(
        LocalContentColor provides
            if (enabled) MiuixTheme.colorScheme.onSurface
            else MiuixTheme.colorScheme.disabledOnSurface,
    ) {
        BasicComponent(
            modifier =
                modifier.then(
                    if (onLongClick != null)
                        Modifier.combinedClickable(
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick,
                            onLongClick = onLongClick,
                            onLongClickLabel = onLongClickLabel,
                        )
                    else Modifier,
                ),
            onClick = if (onLongClick == null) onClick else null,
            enabled = enabled,
            role = Role.Button,
            startAction = leadingIcon,
            endActions = trailingIcon?.let { icon -> { icon() } },
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            text()
        }
    }
}

@Composable
fun MiuixBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    WindowBottomSheet(
        show = true,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        insideMargin = DpSize(0.dp, 12.dp),
    ) {
        Box(
            Modifier.windowInsetsPadding(
                WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom),
            ),
        ) {
            content()
        }
    }
}

object MiuixScaffoldDefaults {
    val contentWindowInsets: WindowInsets
        @Composable get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)
}
