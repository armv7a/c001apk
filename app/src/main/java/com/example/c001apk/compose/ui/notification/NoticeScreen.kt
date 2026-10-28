package com.example.c001apk.compose.ui.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.CommonScreen
import com.example.c001apk.compose.ui.component.MiuixHeader as TopAppBar
import com.example.c001apk.compose.ui.component.MiuixScaffold as Scaffold
import com.example.c001apk.compose.ui.ffflist.FFFContentViewModel
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast
import top.yukonga.miuix.kmp.basic.Card as ElevatedCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Created by bggRGjQaUbCoE on 2024/6/13 */
enum class NoticeType {
    AT,
    COMMENT,
    LIKE,
    FOLLOW,
    MESSAGE,
    NOTIFICATION,
}

@Composable
fun NoticeScreen(
    onBackClick: () -> Unit,
    type: String,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewChat: (String, String, String) -> Unit,
) {

    val viewModel =
        hiltViewModel<NoticeViewModel, NoticeViewModel.ViewModelFactory>(key = type) { factory ->
            factory.create(
                url =
                    when (type) {
                        NoticeType.AT.name -> "/v6/notification/atMeList"
                        NoticeType.COMMENT.name -> "/v6/notification/atCommentMeList"
                        NoticeType.LIKE.name -> "/v6/notification/feedLikeList"
                        NoticeType.FOLLOW.name -> "/v6/notification/contactsFollowList"
                        NoticeType.MESSAGE.name -> "/v6/message/list"
                        NoticeType.NOTIFICATION.name -> "/v6/notification/list"
                        else -> throw IllegalArgumentException("invalid type: $type")
                    },
            )
        }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        if (viewModel.loadingState !is com.example.c001apk.compose.logic.state.LoadingState.Loading)
            viewModel.refresh()
    }
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }
    var showMessageDialog by remember { mutableStateOf(false) }
    var deleteNoticeId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets =
                    WindowInsets.systemBars.only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = {
                    Text(
                        text =
                            when (type) {
                                NoticeType.AT.name -> "@我的动态"
                                NoticeType.COMMENT.name -> "@我的评论"
                                NoticeType.LIKE.name -> "我收到的赞"
                                NoticeType.FOLLOW.name -> "好友关注"
                                NoticeType.MESSAGE.name -> "私信"
                                NoticeType.NOTIFICATION.name -> "信息"
                                else -> throw IllegalArgumentException("invalid type: $type")
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
    ) { paddingValues ->
        CommonScreen(
            viewModel = viewModel,
            isReceivedLikeList = type == NoticeType.LIKE.name,
            refreshState = null,
            resetRefreshState = {},
            paddingValues =
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding(),
                ),
            needTopPadding = true,
            onViewUser = onViewUser,
            onViewFeed = onViewFeed,
            onOpenLink = onOpenLink,
            onCopyText = onCopyText,
            onReport = onReport,
            onHandleMessage = { ukey, isTop ->
                viewModel.ukey = ukey
                viewModel.isTop = isTop
                showMessageDialog = true
            },
            onDeleteNotice = { deleteNoticeId = it },
            onViewChat = { ukey, uid, username ->
                viewModel.resetUnRead(ukey)
                onViewChat(ukey, uid, username)
            },
        )
    }

    deleteNoticeId?.let { id ->
        com.example.c001apk.compose.ui.component.MiuixAlertDialog(
            title = { Text("删除这条通知？") },
            onDismissRequest = { deleteNoticeId = null },
            confirmButton = {
                com.example.c001apk.compose.ui.component.MiuixDialogButton(
                    onClick = {
                        deleteNoticeId = null
                        viewModel.deleteNotice(id)
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                com.example.c001apk.compose.ui.component.MiuixDialogButton(
                    onClick = {
                        deleteNoticeId = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }

    when {
        showMessageDialog -> {
            Dialog(onDismissRequest = { showMessageDialog = false }) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    colors =
                        CardDefaults.defaultColors()
                            .copy(color = MiuixTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    ) {
                        Text(
                            text = if (viewModel.isTop == 1) "移除置顶" else "置顶",
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        showMessageDialog = false
                                        viewModel.onHandleMessage(
                                            FFFContentViewModel.ActionType.TOP,
                                        )
                                    }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MiuixTheme.textStyles.headline2,
                        )
                        Text(
                            text = "删除此对话",
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        showMessageDialog = false
                                        viewModel.onHandleMessage(
                                            FFFContentViewModel.ActionType.DELETE,
                                        )
                                    }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MiuixTheme.textStyles.headline2,
                        )
                    }
                }
            }
        }
    }
}
