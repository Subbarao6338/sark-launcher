package app.lawnchair.data.tabs.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lawnchair.data.tabs.TabEntry
import app.lawnchair.data.tabs.service.TabService
import app.lawnchair.preferences2.ReloadHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TabViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository: TabService = TabService.INSTANCE.get(application)

    val tabs: StateFlow<List<TabEntry>?> = repository.getTabsFlow()
        .distinctUntilChanged()
        .catch { exception ->
            Log.e("TabViewModel", "Error in tabs flow", exception)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val allTabPackages: StateFlow<Set<String>?> = tabs
        .map { tabList ->
            tabList?.flatMap { tab -> tab.itemComponentKeys }
                ?.map { key -> key.substringBefore("/") }
                ?.toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val reloadHelper = ReloadHelper(application)

    fun getTabFlowForId(tabId: Int): Flow<TabEntry?> {
        return tabs.map { list -> list?.find { it.id == tabId } }
    }

    fun renameTab(tabId: Int, title: String) {
        viewModelScope.launch {
            repository.renameTabInfo(tabId, title)
            reloadHelper.reloadGrid()
        }
    }

    fun updateTabItems(id: Int, title: String, componentKeys: List<String>) {
        viewModelScope.launch {
            repository.updateTabWithItems(id, title, componentKeys)
            reloadHelper.reloadGrid()
        }
    }

    fun createTab(title: String) {
        viewModelScope.launch {
            repository.saveTabInfo(title)
            reloadHelper.reloadGrid()
        }
    }

    fun deleteTab(id: Int) {
        viewModelScope.launch {
            repository.deleteTabInfo(id)
            reloadHelper.reloadGrid()
        }
    }

    fun updateTabOrder(orderedIds: List<Int>) {
        viewModelScope.launch {
            repository.updateTabRanks(orderedIds)
            reloadHelper.reloadGrid()
        }
    }
}
