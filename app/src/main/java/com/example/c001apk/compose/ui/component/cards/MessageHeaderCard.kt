package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.icons.FlutterProfileIcons
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.decodeCookieValue
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MessageHeaderCard(
    modifier: Modifier = Modifier,
    isLogin: Boolean,
    userAvatar: String,
    userName: String,
    level: String,
    experience: String,
    nextLevelExperience: String,
    onLogin: () -> Unit,
    onViewUser: (String) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(20.dp),
        onClick = { if (isLogin) onViewUser(CookieUtil.uid) else onLogin() },
        showIndication = true,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(64.dp)
                    .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    FlutterProfileIcons.Account,
                    null,
                    Modifier.size(32.dp),
                    tint = MiuixTheme.colorScheme.onSecondaryContainer,
                )
                if (isLogin)
                    CoilLoader(
                        url = userAvatar,
                        modifier = Modifier.fillMaxSize().squircleClip(22.dp),
                    )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (isLogin) decodeCookieValue(userName) else "登录酷安",
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isLogin) "Lv.${level.ifBlank { "0" }} · 查看个人主页" else "同步关注、收藏与消息",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                FlutterProfileIcons.ChevronRight,
                null,
                Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
        if (isLogin) {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "等级进度",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    "${experience.ifBlank { "0" }}/${nextLevelExperience.ifBlank { "0" }}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(8.dp))
            val total = (nextLevelExperience.toFloatOrNull() ?: 0f).coerceAtLeast(1f)
            LinearProgressIndicator(
                progress = ((experience.toFloatOrNull() ?: 0f) / total).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                height = 4.dp,
            )
        }
    }
}
