package com.example.c001apk.compose.ui.message

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.MiuixAlertDialog as AlertDialog
import com.example.c001apk.compose.ui.component.MiuixDialogButton as TextButton
import com.example.c001apk.compose.ui.component.MiuixScaffold as Scaffold
import com.example.c001apk.compose.ui.component.cards.MessageFFFCard
import com.example.c001apk.compose.ui.component.cards.MessageHeaderCard
import com.example.c001apk.compose.ui.component.cards.MessageListCard
import com.example.c001apk.compose.ui.component.cards.MessageWidgetCard
import com.example.c001apk.compose.ui.component.cards.iconList
import com.example.c001apk.compose.ui.component.cards.titleList
import com.example.c001apk.compose.ui.component.icons.FlutterProfileIcons
import com.example.c001apk.compose.ui.notification.NoticeType
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text

/** Created by bggRGjQaUbCoE on 2024/6/2 */
@Composable
fun MessageScreen(
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onLogin: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onViewFFFList: (String?, String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewNotice: (String) -> Unit,
    onViewHistory: (String) -> Unit,
) {

    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val prefs = LocalUserPreferences.current
    val viewModel =
        hiltViewModel<MessageViewModel, MessageViewModel.ViewModelFactory> { factory ->
            factory.create(url = "/v6/notification/list")
        }
    var showInboxDialog by remember { mutableStateOf(false) }

    LaunchedEffect(prefs.isLogin) {
        if (prefs.isLogin && viewModel.fffList.isEmpty()) {
            viewModel.refresh()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            com.example.c001apk.compose.ui.component.MiuixTopBar(
                title = "我的",
                actions = {
                    top.yukonga.miuix.kmp.basic.IconButton(
                        onClick = onAccountsClick,
                    ) {
                        top.yukonga.miuix.kmp.basic.Icon(
                            FlutterProfileIcons.Account,
                            contentDescription = "账号",
                        )
                    }
                    top.yukonga.miuix.kmp.basic.IconButton(onClick = onSettingsClick) {
                        top.yukonga.miuix.kmp.basic.Icon(
                            FlutterProfileIcons.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
            )
        },
        contentWindowInsets =
            com.example.c001apk.compose.ui.component.MiuixScaffoldDefaults.contentWindowInsets
                .exclude(WindowInsets.navigationBars),
    ) { paddingValues ->
        Column(
            Modifier.fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = bottomPadding + paddingValues.calculateBottomPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection),
                    end = paddingValues.calculateRightPadding(layoutDirection),
                ),
        ) {
            MessageHeaderCard(
                isLogin = prefs.isLogin,
                userAvatar = prefs.userAvatar,
                userName = prefs.username,
                level = prefs.level,
                experience = prefs.experience,
                nextLevelExperience = prefs.nextLevelExperience,
                onLogin = onLogin,
                onViewUser = onViewUser,
            )
            HorizontalDivider()
            top.yukonga.miuix.kmp.basic.PullToRefresh(
                modifier = Modifier.weight(1f),
                isRefreshing = viewModel.isRefreshing,
                onRefresh = viewModel::refresh,
                contentPadding = PaddingValues(top = 12.dp),
                refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新", "刷新完成"),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                ) {
                    if (prefs.isLogin) {
                        item(key = "fff") {
                            MessageFFFCard(
                                modifier =
                                    Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                                fffList = viewModel.fffList,
                                onViewFFFList = onViewFFFList,
                            )
                        }
                    }
                    item(key = "widget") {
                        MessageWidgetCard(
                            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                            onViewFFFList = onViewFFFList,
                            onViewHistory = onViewHistory,
                        )
                    }
                    items(5) { index ->
                        MessageListCard(
                            modifier =
                                Modifier.padding(horizontal = 12.dp)
                                    .padding(bottom = if (index == 4) 12.dp else 2.dp),
                            imageVector = iconList[index],
                            title = titleList[index],
                            count = viewModel.badgeList.getOrNull(index),
                            first = index == 0,
                            last = index == 4,
                            onViewNotice = {
                                if (!prefs.isLogin) onLogin()
                                else if (index == 4) showInboxDialog = true
                                else {
                                    viewModel.clearBadge(index)
                                    onViewNotice(NoticeType.entries[index].name)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    when {
        showInboxDialog -> {
            AlertDialog(
                onDismissRequest = { showInboxDialog = false },
                title = { Text("私信与信息") },
                text = {
                    Column {
                        BasicComponent(
                            title = "私信",
                            onClick = {
                                showInboxDialog = false
                                viewModel.clearBadge(4)
                                onViewNotice(NoticeType.MESSAGE.name)
                            },
                        )
                        BasicComponent(
                            title = "通知",
                            onClick = {
                                showInboxDialog = false
                                onViewNotice(NoticeType.NOTIFICATION.name)
                            },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInboxDialog = false }) { Text("取消") }
                },
            )
        }
    }

    viewModel.toastText?.let {
        context.makeToast(it)
        viewModel.resetToastText()
    }
}
