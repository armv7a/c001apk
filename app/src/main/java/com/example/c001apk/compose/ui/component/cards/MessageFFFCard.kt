package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.ui.history.HistoryType
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback

/** Created by bggRGjQaUbCoE on 2024/6/10 */
data class FFFCardItem(
    val title: String,
    val value: String?,
    val imageVector: ImageVector?,
    val type: String,
)

@Composable
fun MessageFFFCard(
    modifier: Modifier = Modifier,
    fffList: List<String>,
    onViewFFFList: (String, String) -> Unit,
) {
    FFFCardRow(
        modifier = modifier.squircleSurface(cardBg(), 20.dp),
        dataList =
            listOf(
                FFFCardItem(
                    title = "动态",
                    value = fffList.getOrNull(0),
                    imageVector = null,
                    FFFListType.FEED.name,
                ),
                FFFCardItem(
                    title = "关注",
                    value = fffList.getOrNull(1),
                    imageVector = null,
                    FFFListType.FOLLOW.name,
                ),
                FFFCardItem(
                    title = "粉丝",
                    value = fffList.getOrNull(2),
                    imageVector = null,
                    FFFListType.FAN.name,
                ),
            ),
        onViewFFFList = { type ->
            onViewFFFList(CookieUtil.uid, type)
        },
        onViewHistory = {},
    )
}

@Composable
fun FFFCardRow(
    modifier: Modifier = Modifier,
    dataList: List<FFFCardItem>,
    onViewFFFList: (String) -> Unit,
    onViewHistory: (String) -> Unit,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min).fillMaxWidth(),
    ) {
        dataList.forEach { item ->
            FFFCardItem(
                modifier = Modifier.weight(1f),
                onViewFFFList = {
                    when (item.type) {
                        FFFListType.FAV.name -> {
                            onViewHistory(HistoryType.FAV.name)
                        }

                        FFFListType.HISTORY.name -> {
                            onViewHistory(HistoryType.HISTORY.name)
                        }

                        else -> {
                            if (CookieUtil.isLogin) {
                                onViewFFFList(item.type)
                            }
                        }
                    }
                },
                title = item.title,
                value = item.value,
                imageVector = item.imageVector,
            )
        }
    }
}

@Composable
fun FFFCardItem(
    modifier: Modifier = Modifier,
    onViewFFFList: () -> Unit,
    title: String,
    value: String?,
    imageVector: ImageVector?,
) {
    Column(
        modifier =
            modifier
                .clickable(interactionSource = null, indication = SinkFeedback()) {
                    onViewFFFList()
                }
                .heightIn(min = 80.dp)
                .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (imageVector == null) {
            Text(
                text = value ?: "—",
                style =
                    MiuixTheme.textStyles.subtitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Image(
                imageVector = imageVector,
                modifier = Modifier.size(24.dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantSummary),
            )
        }

        Text(
            text = title,
            style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
