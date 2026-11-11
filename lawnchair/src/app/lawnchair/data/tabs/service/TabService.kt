package app.lawnchair.data.tabs.service

import android.content.Context
import app.lawnchair.data.AppDatabase
import app.lawnchair.data.tabs.TabEntry
import app.lawnchair.data.tabs.TabInfoEntity
import app.lawnchair.data.tabs.TabItemEntity
import app.lawnchair.util.MainThreadInitializedObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TabService(private val context: Context) {
    private val tabDao = AppDatabase.INSTANCE.get(context).tabDao()

    fun getTabsFlow(): Flow<List<TabEntry>> {
        return tabDao.getAllTabsWithItems().map { list ->
            list.map { it.toTabEntry() }
        }
    }

    suspend fun saveTabInfo(title: String, hide: Boolean = false): Long = withContext(Dispatchers.IO) {
        val nextRank = (tabDao.getMaxTabRank() ?: -1) + 1
        val tabInfo = TabInfoEntity(
            title = title,
            hide = hide,
            rank = nextRank,
        )
        tabDao.insertTab(tabInfo)
    }

    suspend fun renameTabInfo(tabId: Int, newTitle: String) = withContext(Dispatchers.IO) {
        tabDao.updateTabTitle(tabId, newTitle)
    }

    suspend fun updateTabWithItems(tabId: Int, title: String, componentKeys: List<String>) = withContext(Dispatchers.IO) {
        val items = componentKeys.mapIndexed { index, key ->
            TabItemEntity(
                tabId = tabId,
                rank = index,
                componentKey = key,
            )
        }
        tabDao.replaceTabItems(tabId, title, items)
    }

    suspend fun setTabHidden(tabId: Int, hide: Boolean) = withContext(Dispatchers.IO) {
        tabDao.setTabHidden(tabId, hide)
    }

    suspend fun deleteTabInfo(tabId: Int) = withContext(Dispatchers.IO) {
        tabDao.deleteTab(tabId)
    }

    suspend fun updateTabRanks(orderedIds: List<Int>) = withContext(Dispatchers.IO) {
        tabDao.updateTabRanks(orderedIds)
    }

    private fun TabWithItems.toTabEntry() = TabEntry(
        id = tab.id,
        title = tab.title,
        hide = tab.hide,
        rank = tab.rank,
        itemComponentKeys = items
            .sortedBy { it.rank }
            .mapNotNull { it.componentKey },
    )

    companion object {
        @JvmField
        val INSTANCE = MainThreadInitializedObject<TabService> { context -> TabService(context) }
    }
}
