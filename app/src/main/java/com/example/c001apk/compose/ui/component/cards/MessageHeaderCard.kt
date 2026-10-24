package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.decode
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Fixed profile strip, matching the Flutter page's information hierarchy. */
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
    Box(
        modifier.fillMaxWidth().heightIn(min = 88.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isLogin) {
            Button(onClick = onLogin) { Text("点击登录") }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CoilLoader(
                    url = userAvatar,
                    modifier =
                        Modifier.size(48.dp).clip(CircleShape).clickable {
                            onViewUser(CookieUtil.uid)
                        },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        userName.decode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Lv.${level.ifBlank { "0" }}",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            "${experience.ifBlank { "0" }}/${nextLevelExperience.ifBlank { "0" }}",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    val total = (nextLevelExperience.toFloatOrNull() ?: 0f).coerceAtLeast(1f)
                    LinearProgressIndicator(
                        progress = ((experience.toFloatOrNull() ?: 0f) / total).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        height = 4.dp,
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
