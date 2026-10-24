package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.ImageView
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.Utils.richToString
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.noRippleClickable
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun UserInfoCard(
    data: HomeFeedResponse.Data,
    onFollow: (String, Int) -> Unit,
    onLocalFollow: () -> Unit,
    onPMUser: (String, String) -> Unit,
    onViewFFFList: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current
    val locallyFollowed = prefs.localFollows.any { it.uid == data.uid }
    val ownProfile = prefs.isLogin && prefs.uid == data.uid
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Card(cornerRadius = 24.dp) {
            Box(Modifier.fillMaxWidth().height(212.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .height(180.dp)
                        .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 0.dp),
                ) {
                    if (!data.cover.isNullOrBlank())
                        ImageView(Modifier.fillMaxSize(), url = data.cover, isCover = true)
                }
                Box(
                    Modifier.padding(start = 20.dp)
                        .align(Alignment.BottomStart)
                        .size(76.dp)
                        .squircleBackground(MiuixTheme.colorScheme.surfaceContainer, 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(data.username.orEmpty().take(1), style = MiuixTheme.textStyles.title1)
                    ImageView(
                        Modifier.fillMaxSize(),
                        url = data.userAvatar.orEmpty(),
                        isRound = true,
                        borderWidth = 4f,
                        borderColor = MiuixTheme.colorScheme.surfaceContainer.toArgb(),
                    )
                }
                if (data.level != null)
                    Text(
                        "Lv.${data.level}",
                        modifier =
                            Modifier.align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 8.dp)
                                .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 8.dp)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSecondaryContainer,
                    )
            }
            Column(
                Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    data.username.orEmpty(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier =
                        Modifier.noRippleClickable { context.copyText(data.username.orEmpty()) },
                )
                Text(
                    "UID ${data.uid.orEmpty()}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.noRippleClickable { context.copyText(data.uid.orEmpty()) },
                )
                if (!data.bio.isNullOrBlank())
                    Text(
                        data.bio.richToString(),
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.noRippleClickable { context.copyText(data.bio) },
                    )
                if ((data.logintime ?: 0) > 0)
                    Text(
                        "${fromToday(data.logintime ?: 0)}活跃",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                if (!ownProfile) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (prefs.isLogin) onFollow(data.uid.orEmpty(), data.isFollow ?: 0)
                                else onLocalFollow()
                            },
                            colors =
                                if (
                                    (!prefs.isLogin && locallyFollowed) ||
                                        (prefs.isLogin && data.isFollow == 1)
                                )
                                    ButtonDefaults.buttonColors()
                                else ButtonDefaults.buttonColorsPrimary(),
                        ) {
                            Text(
                                if (!prefs.isLogin) {
                                    if (locallyFollowed) "已本地关注" else "本地关注"
                                } else if (data.isFollow == 1) "已关注" else "关注",
                            )
                        }
                        Button(
                            onClick = { onPMUser(data.uid.orEmpty(), data.username.orEmpty()) },
                            enabled = prefs.isLogin,
                        ) {
                            Icon(MiuixIcons.Email, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("私信")
                        }
                    }
                    if (prefs.isLogin && prefs.quietFollow)
                        TextButton(
                            text = if (locallyFollowed) "已悄悄关注" else "悄悄关注",
                            onClick = onLocalFollow,
                            modifier = Modifier.fillMaxWidth(),
                        )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            cornerRadius = 20.dp,
            insideMargin = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                UserStatistic("动态", (data.feed?.id ?: 0).toString(), Modifier.weight(1f))
                UserStatistic("获赞", (data.beLikeNum ?: 0).toString(), Modifier.weight(1f))
                UserStatistic("关注", (data.follow ?: 0).toString(), Modifier.weight(1f)) {
                    onViewFFFList(data.uid.orEmpty(), FFFListType.USER_FOLLOW.name)
                }
                UserStatistic("粉丝", (data.fans ?: 0).toString(), Modifier.weight(1f)) {
                    onViewFFFList(data.uid.orEmpty(), FFFListType.FAN.name)
                }
            }
        }
    }
}

@Composable
private fun UserStatistic(
    label: String,
    value: String,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .then(if (onClick != null) Modifier.noRippleClickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            value,
            style = MiuixTheme.textStyles.subtitle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
