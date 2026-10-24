package com.example.c001apk.compose.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.c001apk.compose.logic.model.AccountCollection
import com.example.c001apk.compose.logic.network.ApiFailures
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.util.CookieUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Account collection selection is separate from the local favorites database. */
class FeedCollections(
    private val scope: CoroutineScope,
    private val repository: NetworkRepo,
    private val onSaved: (String, Boolean) -> Unit,
    private val notify: (String) -> Unit,
) {
    var visible by mutableStateOf(false)
        private set

    var items by mutableStateOf<List<AccountCollection>>(emptyList())
        private set

    var selected by mutableStateOf<Set<String>>(emptySet())
        private set

    var loading by mutableStateOf(false)
        private set

    var saving by mutableStateOf(false)
        private set

    var creating by mutableStateOf(false)
        private set

    var hasMore by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var createError by mutableStateOf<String?>(null)
        private set

    private var original = emptySet<String>()
    private var feedId = ""
    private var revision = -1L
    private var page = 1
    private var lastItem: String? = null
    private var job: Job? = null
    private var generation = 0
    val busy
        get() = loading || saving || creating

    fun open(id: String) {
        if (!CookieUtil.isLogin) {
            notify("请先登录后使用账号收藏；长按收藏可保存到本地")
            return
        }
        reset()
        feedId = id
        revision = CookieUtil.accountSession.revision
        visible = true
        loadMore()
    }

    private fun currentAccount(): Boolean {
        if (CookieUtil.isLogin && revision == CookieUtil.accountSession.revision) return true
        visible = false
        notify("账号已切换，请重新打开收藏夹")
        return false
    }

    fun toggle(id: String, checked: Boolean) {
        if (busy) return
        selected = if (checked) selected + id else selected - id
    }

    fun loadMore() {
        if (busy || !hasMore || !currentAccount()) return
        loading = true
        error = null
        val requestGeneration = generation
        job = scope.launch {
            try {
                val response = repository.getFeedCollections(feedId, page, lastItem)
                if (requestGeneration != generation || !currentAccount()) return@launch
                val added =
                    response
                        .filter { row ->
                            !row.id.isNullOrBlank() && items.none { it.id == row.id }
                        }
                        .distinctBy { it.id }
                val checked = added.filter { it.isBeCollected == 1 }.mapNotNull { it.id }.toSet()
                original = original + checked
                selected = selected + checked
                items = items + added
                hasMore = added.isNotEmpty()
                lastItem = response.lastOrNull()?.let { it.entityId ?: it.id }
                page++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestGeneration == generation) error = ApiFailures.safeMessage(e)
            } finally {
                if (requestGeneration == generation) loading = false
            }
        }
    }

    fun save() {
        if (busy || error != null || !currentAccount()) return
        if (selected == original) {
            visible = false
            return
        }
        saving = true
        val requestGeneration = generation
        job = scope.launch {
            try {
                val result = repository.saveFeedCollections(feedId, selected, original - selected)
                check(result.collect == 0 || result.collect == 1) { "未收到收藏状态，请重试" }
                if (requestGeneration != generation || !currentAccount()) return@launch
                onSaved(feedId, result.collect == 1)
                notify(if (result.collect == 1) "已保存到账号收藏夹" else "已取消账号收藏")
                visible = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestGeneration == generation) notify(ApiFailures.safeMessage(e))
            } finally {
                if (requestGeneration == generation) saving = false
            }
        }
    }

    fun create(title: String, isOpen: Boolean, onCreated: () -> Unit) {
        if (busy || title.isBlank() || !currentAccount()) return
        creating = true
        createError = null
        val requestGeneration = generation
        job = scope.launch {
            try {
                val collection = repository.createCollection(title.trim(), isOpen)
                val id = checkNotNull(collection.id?.takeIf { it.isNotBlank() }) { "未收到收藏夹信息" }
                if (requestGeneration != generation || !currentAccount()) return@launch
                items = listOf(collection) + items.filterNot { it.id == id }
                selected = selected + id
                onCreated()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestGeneration == generation) createError = ApiFailures.safeMessage(e)
            } finally {
                if (requestGeneration == generation) creating = false
            }
        }
    }

    fun dismiss() {
        if (!saving && !creating) reset()
    }

    fun reset() {
        generation++
        job?.cancel()
        job = null
        visible = false
        items = emptyList()
        selected = emptySet()
        original = emptySet()
        loading = false
        saving = false
        creating = false
        hasMore = true
        error = null
        createError = null
        page = 1
        lastItem = null
    }
}
