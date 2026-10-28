package com.example.c001apk.compose.ui.contentfilter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.filter.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class ContentFilterViewModel @Inject constructor(
    private val store: ContentFilterStore,
    private val twilight: TwilightRulesClient,
) : ViewModel() {
    var error by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var importMessage by mutableStateOf<String?>(null)
        private set

    fun importTwilight() = save {
        importMessage = null
        val candidate = twilight.fetch()
        store.importTwilight(candidate)
        importMessage = "已导入版本 ${candidate.revision}，共 ${candidate.rules.size} 条；过滤开关保持原设置"
    }

    fun removeTwilight() = save {
        store.update { it.copy(twilight = null, twilightStrict = false, disabledTwilightRules = emptySet()) }
        importMessage = null
    }

    fun update(change: (ContentFilterPreferences) -> ContentFilterPreferences) = save { store.update(change) }
    fun saveRule(rule: ContentRule, done: () -> Unit) = save(done) { store.save(rule) }
    fun removeRule(id: String, done: () -> Unit) = save(done) { store.remove(id) }
    fun disableRule(id: String) = save { store.disable(id) }
    private fun save(done: () -> Unit = {}, operation: suspend () -> Unit) {
        if (saving) return
        saving = true
        error = null
        viewModelScope.launch {
            try { operation(); done() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "操作失败，已有规则已保留：${e.message ?: "请重试"}" }
            finally { saving = false }
        }
    }
}
