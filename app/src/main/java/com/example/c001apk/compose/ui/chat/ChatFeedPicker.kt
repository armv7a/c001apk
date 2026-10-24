package com.example.c001apk.compose.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.repository.HistoryFavoriteRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.*
import com.example.c001apk.compose.util.CookieUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*

object ChatFeedLink {
    fun parse(input: String): String? {
        val value = input.trim()
        if (Regex("^[0-9]+$").matches(value)) return value
        val parts =
            com.example.c001apk.compose.util.CoolapkLink.parse(value)?.segments ?: return null
        return parts.getOrNull(1)?.takeIf {
            parts.size == 2 && parts[0] == "feed" && Regex("[0-9]+").matches(it)
        }
    }
}

@HiltViewModel
class ChatFeedPickerViewModel
@Inject
constructor(private val network: NetworkRepo, favorites: HistoryFavoriteRepo) : ViewModel() {
    val favorites = favorites.loadAllFavoriteListFlow()
    var feeds by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    var ended by mutableStateOf(false)
        private set

    private var page = 1
    private var last: String? = null
    private val session = CookieUtil.accountSession

    init {
        loadMore()
    }

    fun loadMore() {
        if (loading || ended || session.revision != CookieUtil.accountSession.revision) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                when (
                    val result =
                        network.getUserFeed(session.uid, page, last, includeTop = false).first()
                ) {
                    is LoadingState.Success -> {
                        val rows =
                            result.response.filter {
                                it.entityType == "feed" && !it.id.isNullOrBlank()
                            }
                        feeds =
                            (feeds +
                                    rows.map {
                                        it.id!! to
                                            (it.title?.takeIf(String::isNotBlank)
                                                ?: it.message.orEmpty())
                                    })
                                .distinctBy { it.first }
                        last = rows.lastOrNull()?.id ?: last
                        page++
                        ended = result.response.any { it.entityTemplate == "noMoreDataCard" }
                    }
                    is LoadingState.Error -> error = result.errMsg
                    LoadingState.Empty -> ended = true
                    else -> Unit
                }
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun ChatFeedPicker(onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    val model = hiltViewModel<ChatFeedPickerViewModel>()
    val favorites by model.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    var tab by remember { mutableIntStateOf(0) }
    var link by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<String?>(null) }
    MiuixAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发送动态") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("我的动态", "本地收藏", "链接").forEachIndexed { index, label ->
                        MiuixActionButton(onClick = { tab = index }, enabled = tab != index) {
                            Text(label)
                        }
                    }
                }
                if (tab == 2) {
                    TextField(
                        value = link,
                        onValueChange = {
                            link = it
                            invalid = false
                        },
                        label = "动态链接或编号",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (invalid) Text("请输入有效的酷安动态链接或编号")
                    MiuixActionButton(
                        onClick = {
                            chosen = ChatFeedLink.parse(link)
                            invalid = chosen == null
                        },
                    ) {
                        Text("下一步")
                    }
                } else
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        val rows =
                            if (tab == 0) model.feeds
                            else favorites.reversed().map { it.id to it.message }
                        items(rows, key = { it.first }) { (id, title) ->
                            BasicComponent(
                                title =
                                    org.jsoup.Jsoup.parse(title).text().take(100).ifBlank {
                                        "动态 $id"
                                    },
                                summary = "ID $id",
                                onClick = { chosen = id },
                            )
                        }
                        item {
                            if (rows.isEmpty()) Text("暂无动态，可切换到链接输入。", Modifier.padding(16.dp))
                            if (tab == 0 && !model.ended)
                                MiuixActionButton(
                                    onClick = model::loadMore,
                                    enabled = !model.loading,
                                ) {
                                    Text(
                                        if (model.loading) "加载中…"
                                        else model.error?.let { "加载失败，重试" } ?: "加载更多",
                                    )
                                }
                        }
                    }
            }
        },
        confirmButton = { MiuixDialogButton(onClick = onDismiss) { Text("取消") } },
    )
    chosen?.let { id ->
        MiuixAlertDialog(
            onDismissRequest = { chosen = null },
            title = { Text("发送这条动态？") },
            text = {
                Text(
                    com.example.c001apk.compose.util.ShareContent.link(
                        com.example.c001apk.compose.util.ShareType.FEED,
                        id,
                    ),
                )
            },
            confirmButton = {
                MiuixDialogButton(
                    onClick = {
                        chosen = null
                        onSelected(id)
                    },
                ) {
                    Text("发送")
                }
            },
            dismissButton = { MiuixDialogButton(onClick = { chosen = null }) { Text("返回") } },
        )
    }
}
