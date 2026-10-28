package com.example.c001apk.compose.ui.message

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.c001apk.compose.ui.component.cards.MessageListCard
import com.example.c001apk.compose.ui.component.icons.FlutterProfileIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import com.example.c001apk.compose.ui.notification.NoticeType

@Composable
internal fun MessageSection(
    counts: Map<NoticeType, Int?>,
    onViewNotice: (NoticeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        MessageEntry.entries.forEachIndexed { index, entry ->
            MessageListCard(
                imageVector = when (entry) {
                    MessageEntry.NOTICES -> MiuixIcons.Info
                    MessageEntry.LIKES -> FlutterProfileIcons.ThumbUp
                    MessageEntry.FOLLOWS -> FlutterProfileIcons.PersonAdd
                    MessageEntry.MESSAGES -> FlutterProfileIcons.Mail
                },
                title = entry.title,
                count = entry.unreadCount(counts),
                first = index == 0,
                last = index == MessageEntry.entries.lastIndex,
                onViewNotice = { onViewNotice(entry.destination) },
            )
        }
    }
}
