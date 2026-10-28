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
class ContentFilterViewModel @Inject constructor(private val store: ContentFilterStore) : ViewModel() {
    var error by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

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
            catch (e: Exception) { error = e.message ?: "保存失败，请重试" }
            finally { saving = false }
        }
    }
}
