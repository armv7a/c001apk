package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.squircle.squircleSurface

/** Created by bggRGjQaUbCoE on 2024/6/10 */
@Composable
fun MessageWidgetCard(
    modifier: Modifier = Modifier,
    onViewFFFList: (String?, String) -> Unit,
    onViewHistory: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().squircleSurface(cardBg(), 16.dp),
    ) {
        FFFCardRow(
            dataList =
                listOf(
                    FFFCardItem(
                        title = "本地收藏",
                        value = null,
                        imageVector = MiuixIcons.Backup,
                        FFFListType.FAV.name,
                    ),
                    FFFCardItem(
                        title = "浏览历史",
                        value = null,
                        imageVector = MiuixIcons.Recent,
                        FFFListType.HISTORY.name,
                    ),
                    FFFCardItem(
                        title = "我的常去",
                        value = null,
                        imageVector = MiuixIcons.Location,
                        FFFListType.RECENT.name,
                    ),
                ),
            onViewFFFList = { type ->
                onViewFFFList(CookieUtil.uid, type)
            },
            onViewHistory = onViewHistory,
        )
        FFFCardRow(
            dataList =
                listOf(
                    FFFCardItem(
                        title = "我的收藏",
                        value = null,
                        imageVector = MiuixIcons.Favorites,
                        FFFListType.COLLECTION.name,
                    ),
                    FFFCardItem(
                        title = "我的赞",
                        value = null,
                        imageVector = MiuixIcons.Favorites,
                        FFFListType.LIKE.name,
                    ),
                    FFFCardItem(
                        title = "我的回复",
                        value = null,
                        imageVector = MiuixIcons.Messages,
                        FFFListType.REPLY.name,
                    ),
                ),
            onViewFFFList = { type ->
                onViewFFFList(
                    if (type == FFFListType.COLLECTION.name) null else CookieUtil.uid,
                    type,
                )
            },
            onViewHistory = {},
        )
    }
}
