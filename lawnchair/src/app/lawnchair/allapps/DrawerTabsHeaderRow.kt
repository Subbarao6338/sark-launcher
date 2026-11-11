package app.lawnchair.allapps

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.lawnchair.data.tabs.ActiveTabState
import app.lawnchair.data.tabs.TabEntry
import app.lawnchair.data.tabs.service.TabService
import app.lawnchair.font.FontManager
import app.lawnchair.launcher
import app.lawnchair.theme.color.tokens.ColorTokens
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.systemui.plugins.AllAppsRow

class DrawerTabsHeaderRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr),
    AllAppsRow {

    private val scrollView = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val tabsStrip = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = Utilities.dpToPx(16f).toInt()
        setPadding(pad, 0, pad, 0)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private var heightUpdatedListener: AllAppsRow.OnHeightUpdatedListener? = null
    private val tabService = TabService.INSTANCE.get(context)
    private var tabs: List<TabEntry> = emptyList()
    private var currentTabId = ActiveTabState.ALL_APPS_TAB_ID

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        scrollView.addView(tabsStrip)
        addView(scrollView)

        val owner = context.launcher as? LifecycleOwner
        owner?.lifecycleScope?.launchWhenStarted {
            tabService.getTabsFlow().collect { updatedTabs ->
                tabs = updatedTabs
                rebuildTabs()
            }
        }
        owner?.lifecycleScope?.launchWhenStarted {
            ActiveTabState.currentTabId.collect { tabId ->
                currentTabId = tabId
                updateSelectedTab()
            }
        }
    }

    private fun rebuildTabs() {
        tabsStrip.removeAllViews()
        if (tabs.isEmpty()) {
            visibility = GONE
            heightUpdatedListener?.onHeightUpdated()
            return
        }

        visibility = VISIBLE
        val fontManager = FontManager.INSTANCE.get(context)

        // "All" tab
        addTabButton(
            title = context.getString(R.string.all_apps_tab_label),
            id = ActiveTabState.ALL_APPS_TAB_ID,
            fontManager = fontManager,
        )

        // Custom tabs
        tabs.forEach { tab ->
            addTabButton(
                title = tab.title,
                id = tab.id,
                fontManager = fontManager,
            )
        }

        updateSelectedTab()
        heightUpdatedListener?.onHeightUpdated()
    }

    private fun addTabButton(title: String, id: Int, fontManager: FontManager) {
        val btn = Button(context).apply {
            text = title
            isAllCaps = false
            textSize = 14f
            val horizontalPadding = Utilities.dpToPx(12f).toInt()
            val verticalPadding = Utilities.dpToPx(6f).toInt()
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            tag = id
            fontManager.setCustomFont(this, R.id.font_body_medium)
            setOnClickListener {
                ActiveTabState.setCurrentTabId(id)
            }
        }

        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginEnd = Utilities.dpToPx(8f).toInt()
        }
        tabsStrip.addView(btn, lp)
    }

    private fun updateSelectedTab() {
        val selectedBgColor = ColorTokens.AllAppsTabBackgroundSelected.resolveColor(context)
        val selectedTextColor = ColorTokens.TextColorPrimaryInverse.resolveColor(context)
        val unselectedTextColor = ColorTokens.TextColorPrimary.resolveColor(context)

        for (i in 0 until tabsStrip.childCount) {
            val child = tabsStrip.getChildAt(i) as? Button ?: continue
            val id = child.tag as? Int ?: continue
            val isSelected = (id == currentTabId)

            val bg = GradientDrawable().apply {
                cornerRadius = Utilities.dpToPx(16f).toFloat()
                if (isSelected) {
                    setColor(selectedBgColor)
                } else {
                    setColor(Color.TRANSPARENT)
                }
            }
            child.background = bg
            if (isSelected) {
                child.setTextColor(selectedTextColor)
            } else {
                child.setTextColor(unselectedTextColor)
            }
        }
    }

    override fun setup(parent: ViewGroup): View {
        return this
    }

    override fun getExpectedHeight(): Int {
        return if (tabs.isEmpty() || visibility == GONE) 0 else Utilities.dpToPx(48f).toInt()
    }

    override fun setOnHeightUpdatedListener(listener: AllAppsRow.OnHeightUpdatedListener?) {
        heightUpdatedListener = listener
    }
}
