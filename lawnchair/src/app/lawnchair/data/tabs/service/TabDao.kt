package app.lawnchair.data.tabs.service

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Relation
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import app.lawnchair.data.tabs.TabInfoEntity
import app.lawnchair.data.tabs.TabItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabInfoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabItems(items: List<TabItemEntity>)

    @Query("SELECT id FROM AppDrawerTabs ORDER BY rank ASC")
    suspend fun getAllTabIds(): List<Int>

    @Query("SELECT * FROM AppDrawerTabs ORDER BY rank ASC")
    @Transaction
    fun getAllTabsWithItems(): Flow<List<TabWithItems>>

    @Query("SELECT MAX(rank) FROM AppDrawerTabs")
    suspend fun getMaxTabRank(): Int?

    @Query("DELETE FROM AppDrawerTabItems WHERE tabId = :tabId")
    suspend fun deleteTabItemsByTabId(tabId: Int)

    @Query("UPDATE AppDrawerTabs SET title = :title, timestamp = :timestamp WHERE id = :id")
    suspend fun updateTabTitle(id: Int, title: String, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun replaceTabItems(tabId: Int, title: String, items: List<TabItemEntity>) {
        updateTabTitle(tabId, title)
        deleteTabItemsByTabId(tabId)
        insertTabItems(items.map { it.copy(tabId = tabId) })
    }

    @Query(
        value = """
                UPDATE AppDrawerTabs
                SET hide = :hide, timestamp = :timestamp
                WHERE id = :tabId
            """,
    )
    suspend fun setTabHidden(
        tabId: Int,
        hide: Boolean,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM AppDrawerTabs WHERE id = :tabId")
    suspend fun deleteTab(tabId: Int)

    @Query("UPDATE AppDrawerTabs SET rank = :rank WHERE id = :id")
    suspend fun updateTabRank(id: Int, rank: Int)

    @Transaction
    suspend fun updateTabRanks(orderedIds: List<Int>) {
        val currentIds = getAllTabIds().toSet()
        if (orderedIds.size != currentIds.size || orderedIds.toSet() != currentIds) {
            return
        }
        orderedIds.forEachIndexed { index, id ->
            updateTabRank(id, index)
        }
    }

    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}

data class TabWithItems(
    @Embedded val tab: TabInfoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tabId",
    )
    val items: List<TabItemEntity>,
)
