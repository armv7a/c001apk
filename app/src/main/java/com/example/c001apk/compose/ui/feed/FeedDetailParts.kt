package com.example.c001apk.compose.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.icons.Smartphone
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.Utils.richToString
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback

@Composable
fun FeedDetailAuthor(data: HomeFeedResponse.Data, onViewUser: (String) -> Unit) {
    ReadingSurface(first = true) {
        Column(
            Modifier.fillMaxWidth()
                .clickable(interactionSource = null, indication = SinkFeedback()) {
                    onViewUser(data.uid.orEmpty())
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(48.dp)
                        .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 16.dp),
                ) {
                    Text(
                        data.username.orEmpty().take(1),
                        modifier = Modifier.align(Alignment.Center),
                        style = MiuixTheme.textStyles.title3,
                    )
                    CoilLoader(
                        url = data.userAvatar,
                        modifier = Modifier.fillMaxSize().squircleClip(16.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        data.username.orEmpty(),
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                                fromToday(data.dateline ?: 0),
                                data.ipLocation?.takeIf(String::isNotBlank),
                            )
                            .joinToString(" · "),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Icon(
                    MiuixIcons.ChevronForward,
                    contentDescription = "查看用户主页",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.size(18.dp),
                )
            }
            data.deviceTitle?.richToString()?.takeIf(String::isNotBlank)?.let { device ->
                Row(
                    Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        MiuixIcons.Smartphone,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        device,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Article blocks stay lazy while the outer edges form one continuous reading card. */
@Composable
fun ReadingSurface(first: Boolean = false, last: Boolean = false, content: @Composable () -> Unit) {
    val top = if (first) 20.dp else 0.dp
    val bottom = if (last) 20.dp else 0.dp
    Column(
        Modifier.fillMaxWidth()
            .squircleSurface(MiuixTheme.colorScheme.surfaceContainer, top, top, bottom, bottom)
            .padding(top = if (first) 6.dp else 0.dp, bottom = if (last) 8.dp else 0.dp),
    ) {
        content()
    }
}
