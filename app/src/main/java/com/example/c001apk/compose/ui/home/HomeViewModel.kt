package com.example.c001apk.compose.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.HomeMenu
import com.example.c001apk.compose.logic.repository.HomeMenuRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val homeMenuRepo: HomeMenuRepo,
) : ViewModel() {

    val homeMenus: Flow<List<HomeMenu>> = homeMenuRepo.loadAllListFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            homeMenuRepo.upsertList(normalizeMenus(homeMenuRepo.loadAllList()))
        }
    }

    fun setEnabledTabs(enabledTabs: Set<TabType>, onSaved: () -> Unit) {
        if (enabledTabs.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val enabledNames = enabledTabs.mapTo(mutableSetOf()) { it.name }
                val menus =
                    normalizeMenus(homeMenuRepo.loadAllList()).map { menu ->
                        menu.copy(isEnable = menu.title in enabledNames)
                    }
                homeMenuRepo.upsertList(menus)
            }
            // Pop the editor only after its ViewModel has finished persisting the changes.
            onSaved()
        }
    }

    private fun normalizeMenus(storedMenus: List<HomeMenu>): List<HomeMenu> {
        val storedByTitle = storedMenus.associateBy(HomeMenu::title)
        return homeTabTypes.mapIndexed { index, type ->
            storedByTitle[type.name]?.copy(position = index)
                ?: HomeMenu(position = index, title = type.name, isEnable = true)
        }
    }
}
