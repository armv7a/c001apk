package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.c001apk.compose.ui.component.icons.ThumbUpOffAlt
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Created by bggRGjQaUbCoE on 2024/6/10 */
val backgroundList = listOf("#2196f3", "#00bcd4", "#4caf50", "#f44336", "#ff9800")
val iconList =
    listOf(
        MiuixIcons.Email,
        MiuixIcons.Messages,
        MiuixIcons.ThumbUpOffAlt,
        MiuixIcons.Contacts,
        MiuixIcons.Email,
    )
val titleList = listOf("@我的动态", "@我的评论", "我收到的赞", "好友关注", "私信")

@Composable
fun MessageListCard(
    modifier: Modifier = Modifier,
    background: String,
    imageVector: ImageVector,
    title: String,
    count: Int?,
    onViewNotice: () -> Unit,
) {

    ConstraintLayout(
        modifier =
            modifier
                .fillMaxWidth()
                .squircleSurface(cardBg(), 16.dp)
                .clickable {
                    if (CookieUtil.isLogin) {
                        onViewNotice()
                    }
                }
                .padding(10.dp),
    ) {
        val (icon, name, badge, arrow) = createRefs()

        MessageCardLogo(
            background = background,
            imageVector = imageVector,
            modifier =
                Modifier.constrainAs(icon) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )

        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle.copy(fontSize = 14.sp),
            modifier =
                Modifier.padding(start = 10.dp).constrainAs(name) {
                    start.linkTo(icon.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )

        count?.let {
            if (it > 0) {
                Badge(
                    modifier =
                        Modifier.constrainAs(badge) {
                            start.linkTo(name.start)
                            end.linkTo(arrow.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        },
                ) {
                    Text(text = it.toString())
                }
            }
        }

        Image(
            imageVector = MiuixIcons.ChevronForward,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantSummary),
            modifier =
                Modifier.constrainAs(arrow) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )
    }
}

@Composable
private fun MessageCardLogo(
    modifier: Modifier = Modifier,
    background: String,
    imageVector: ImageVector,
) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(background))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = imageVector,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}
