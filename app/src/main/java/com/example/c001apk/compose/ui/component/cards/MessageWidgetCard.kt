package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.icons.FlutterProfileIcons
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
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
                        imageVector = FlutterProfileIcons.Archive,
                        FFFListType.FAV.name,
                    ),
                    FFFCardItem(
                        title = "浏览历史",
                        value = null,
                        imageVector = FlutterProfileIcons.History,
                        FFFListType.HISTORY.name,
                    ),
                    FFFCardItem(
                        title = "我的常去",
                        value = null,
                        imageVector = FlutterProfileIcons.MyLocation,
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
                        imageVector = FlutterProfileIcons.Star,
                        FFFListType.COLLECTION.name,
                    ),
                    FFFCardItem(
                        title = "我的赞",
                        value = null,
                        imageVector = FlutterProfileIcons.Favorite,
                        FFFListType.LIKE.name,
                    ),
                    FFFCardItem(
                        title = "我的回复",
                        value = null,
                        imageVector = FlutterProfileIcons.Reply,
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
