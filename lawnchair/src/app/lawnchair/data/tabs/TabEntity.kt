package app.lawnchair.data.tabs

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Entity(tableName = "AppDrawerTabs")
data class TabInfoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val hide: Boolean = false,
    val rank: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "AppDrawerTabItems",
    foreignKeys = [
        ForeignKey(
            entity = TabInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tabId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tabId"])],
)
data class TabItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tabId: Int,
    val rank: Int = 0,
    @ColumnInfo(name = "item_info") val componentKey: String?,
    val timestamp: Long = System.currentTimeMillis(),
)

@Immutable
data class TabEntry(
    val id: Int,
    val title: String,
    val hide: Boolean = false,
    val rank: Int = 0,
    val itemComponentKeys: List<String> = emptyList(),
)

object ActiveTabState {
    const val ALL_APPS_TAB_ID = -1

    private val _currentTabId = MutableStateFlow(ALL_APPS_TAB_ID)
    val currentTabId: StateFlow<Int> = _currentTabId.asStateFlow()

    fun setCurrentTabId(id: Int) {
        _currentTabId.value = id
    }
}
