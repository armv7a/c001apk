package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
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
    val followed = if (prefs.isLogin) data.isFollow == 1 else locallyFollowed
    val coverHeight = if (data.cover.isNullOrBlank()) 96.dp else 180.dp

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = 26.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                Box(
                    Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)
                        .fillMaxWidth()
                        .height(coverHeight)
                        .squircleClip(20.dp)
                        .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 20.dp),
                ) {
                    // No text overlays the cover, so preserve its original color and brightness.
                    if (!data.cover.isNullOrBlank())
                        ImageView(Modifier.fillMaxSize(), url = data.cover)
                }
                Column(
                    Modifier.fillMaxWidth()
                        .padding(start = 108.dp, end = 20.dp, top = 10.dp)
                        .heightIn(min = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            data.username.orEmpty(),
                            modifier =
                                Modifier.weight(1f, fill = false).noRippleClickable {
                                    context.copyText(data.username.orEmpty())
                                },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        data.level?.let { level ->
                            Text(
                                "Lv.$level",
                                modifier =
                                    Modifier.squircleBackground(
                                            MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            6.dp,
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        "UID ${data.uid.orEmpty()}",
                        modifier =
                            Modifier.noRippleClickable { context.copyText(data.uid.orEmpty()) },
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Box(
                Modifier.padding(start = 20.dp, top = coverHeight - 14.dp)
                    .size(76.dp)
                    .squircleBackground(MiuixTheme.colorScheme.surfaceContainer, 26.dp)
                    .padding(4.dp)
                    .squircleClip(22.dp)
                    .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(data.username.orEmpty().take(1), style = MiuixTheme.textStyles.title1)
                if (!data.userAvatar.isNullOrBlank())
                    ImageView(Modifier.fillMaxSize(), url = data.userAvatar)
            }
        }

        Column(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 16.dp)) {
            if (!data.bio.isNullOrBlank()) {
                Text(
                    data.bio.richToString(),
                    modifier = Modifier.noRippleClickable { context.copyText(data.bio) },
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(6.dp))
            }
            if ((data.logintime ?: 0) > 0)
                Text(
                    "${fromToday(data.logintime ?: 0)}活跃",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )

            Row(
                Modifier.fillMaxWidth()
                    .padding(top = 16.dp, bottom = if (ownProfile) 0.dp else 16.dp),
            ) {
                UserStatistic("动态", (data.feed?.id ?: 0).toString(), Modifier.weight(1f))
                UserStatistic("获赞", (data.beLikeNum ?: 0).toString(), Modifier.weight(1f))
                UserStatistic("关注", (data.follow ?: 0).toString(), Modifier.weight(1f)) {
                    onViewFFFList(data.uid.orEmpty(), FFFListType.USER_FOLLOW.name)
                }
                UserStatistic("粉丝", (data.fans ?: 0).toString(), Modifier.weight(1f)) {
                    onViewFFFList(data.uid.orEmpty(), FFFListType.FAN.name)
                }
            }

            if (!ownProfile) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (prefs.isLogin) onFollow(data.uid.orEmpty(), data.isFollow ?: 0)
                            else onLocalFollow()
                        },
                        minHeight = 48.dp,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors =
                            if (followed) ButtonDefaults.buttonColors()
                            else ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        if (!followed) {
                            Icon(MiuixIcons.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            if (!prefs.isLogin) {
                                if (locallyFollowed) "已本地关注" else "本地关注"
                            } else if (data.isFollow == 1) "已关注" else "关注",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPMUser(data.uid.orEmpty(), data.username.orEmpty()) },
                        enabled = prefs.isLogin,
                        minHeight = 48.dp,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(MiuixIcons.Email, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("私信", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (prefs.isLogin && prefs.quietFollow) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        text = if (locallyFollowed) "已悄悄关注" else "悄悄关注",
                        onClick = onLocalFollow,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.noRippleClickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Text(
            value,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
