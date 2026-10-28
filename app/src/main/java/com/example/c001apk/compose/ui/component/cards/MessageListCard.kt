package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.icons.FlutterProfileIcons
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MessageListCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    count: Int?,
    first: Boolean,
    last: Boolean,
    onViewNotice: () -> Unit,
) {
    val top = if (first) 20.dp else 0.dp
    val bottom = if (last) 20.dp else 0.dp
    BasicComponent(
        title = title,
        modifier =
            modifier
                .heightIn(min = 60.dp)
                .squircleSurface(MiuixTheme.colorScheme.surfaceContainer, top, top, bottom, bottom),
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        role = Role.Button,
        onClick = onViewNotice,
        startAction = {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
        },
        endActions = {
            if (count != null && count > 0) {
                Badge { Text(if (count > 99) "99+" else count.toString()) }
                Spacer(Modifier.width(12.dp))
            }
            Icon(
                FlutterProfileIcons.ChevronRight,
                null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
    )
}
