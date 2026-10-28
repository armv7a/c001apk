package com.example.c001apk.compose.logic.filter

import androidx.datastore.core.DataStore
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class ContentFilterStore @Inject constructor(private val preferences: DataStore<UserPreferencesCompat>) {
    val data = preferences.data.map { ContentFilterPreferences.decode(it.contentFilterJson) }.distinctUntilChanged()

    suspend fun update(change: (ContentFilterPreferences) -> ContentFilterPreferences) {
        preferences.updateData { current ->
            current.copy(contentFilterJson = change(ContentFilterPreferences.decode(current.contentFilterJson)).encode())
        }
    }

    suspend fun save(rule: ContentRule) = update { current ->
        rule.validate()
        current.copy(rules = current.rules.filterNot { it.id == rule.id } + rule)
    }

    suspend fun remove(id: String) = update { it.copy(rules = it.rules.filterNot { rule -> rule.id == id }) }

    suspend fun disable(id: String) = update {
        it.copy(rules = it.rules.map { rule -> if (rule.id == id) rule.copy(enabled = false) else rule })
    }
}
