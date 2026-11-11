package app.lawnchair.feed

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.lawnchair.LawnchairLauncher
import app.lawnchair.font.FontManager
import app.lawnchair.theme.color.tokens.ColorTokens
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.widget.picker.WidgetsFullSheet
import com.android.systemui.plugins.shared.LauncherOverlayManager
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlay
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks

class HomescreenFeedOverlay(
    private val launcher: LawnchairLauncher,
) : LauncherOverlay,
    LauncherOverlayManager {

    private var overlayCallbacks: LauncherOverlayCallbacks? = null
    private var containerView: FrameLayout? = null
    private var feedContentView: LinearLayout? = null
    private var isAttached = false
    private var scrollProgress = 0f

    private fun ensureViews() {
        if (containerView != null) return
        val root = launcher.dragLayer ?: return

        containerView = FrameLayout(launcher).apply {
            setBackgroundColor(ColorTokens.ColorBackground.resolveColor(launcher))
            visibility = View.INVISIBLE
            elevation = Utilities.dpToPx(10f).toFloat()
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            layoutParams = lp
        }

        val scrollView = ScrollView(launcher).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val fontManager = FontManager.INSTANCE.get(launcher)

        feedContentView = LinearLayout(launcher).apply {
            orientation = LinearLayout.VERTICAL
            val pad = Utilities.dpToPx(24f).toInt()
            val topPad = Utilities.dpToPx(56f).toInt()
            setPadding(pad, topPad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        // Header Title
        val titleView = TextView(launcher).apply {
            text = launcher.getString(R.string.homescreen_feed_title)
            textSize = 28f
            setTextColor(ColorTokens.TextColorPrimary.resolveColor(launcher))
            fontManager.setCustomFont(this, R.id.font_heading)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = Utilities.dpToPx(16f).toInt()
            }
            layoutParams = lp
        }
        feedContentView?.addView(titleView)

        // Add Widget / Shortcut Button
        val addWidgetButton = Button(launcher).apply {
            text = launcher.getString(R.string.add_widget_to_feed)
            isAllCaps = false
            setTextColor(ColorTokens.TextColorPrimaryInverse.resolveColor(launcher))
            setBackgroundColor(ColorTokens.AllAppsTabBackgroundSelected.resolveColor(launcher))
            fontManager.setCustomFont(this, R.id.font_button)
            setOnClickListener {
                hideOverlay(true)
                WidgetsFullSheet.show(launcher, true)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = Utilities.dpToPx(24f).toInt()
            }
            layoutParams = lp
        }
        feedContentView?.addView(addWidgetButton)

        // Empty state / hint
        val emptyHint = TextView(launcher).apply {
            text = launcher.getString(R.string.feed_empty_hint)
            textSize = 15f
            setTextColor(ColorTokens.TextColorSecondary.resolveColor(launcher))
            fontManager.setCustomFont(this, R.id.font_body_medium)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            layoutParams = lp
        }
        feedContentView?.addView(emptyHint)

        scrollView.addView(feedContentView)
        containerView?.addView(scrollView)
        root.addView(containerView)
    }

    override fun onScrollInteractionBegin() {
        ensureViews()
        containerView?.visibility = View.VISIBLE
    }

    override fun onScrollInteractionEnd() {
        if (scrollProgress >= 0.5f) {
            openOverlay()
        } else {
            hideOverlay(true)
        }
    }

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        scrollProgress = progress
        ensureViews()
        containerView?.let { view ->
            if (progress > 0f) {
                view.visibility = View.VISIBLE
                val width = view.width.toFloat().takeIf { it > 0 } ?: launcher.resources.displayMetrics.widthPixels.toFloat()
                view.translationX = (progress - 1f) * width
                view.alpha = progress.coerceIn(0f, 1f)
            } else {
                view.visibility = View.INVISIBLE
            }
        }
        overlayCallbacks?.onOverlayScrollChanged(progress)
    }

    override fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks) {
        overlayCallbacks = callbacks
    }

    override fun openOverlay() {
        ensureViews()
        containerView?.let { view ->
            view.visibility = View.VISIBLE
            view.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(250)
                .start()
        }
        scrollProgress = 1f
        overlayCallbacks?.onOverlayScrollChanged(1f)
    }

    override fun hideOverlay(animate: Boolean) {
        containerView?.let { view ->
            val width = view.width.toFloat().takeIf { it > 0 } ?: launcher.resources.displayMetrics.widthPixels.toFloat()
            if (animate) {
                view.animate()
                    .translationX(-width)
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction {
                        view.visibility = View.INVISIBLE
                    }
                    .start()
            } else {
                view.translationX = -width
                view.alpha = 0f
                view.visibility = View.INVISIBLE
            }
        }
        scrollProgress = 0f
        overlayCallbacks?.onOverlayScrollChanged(0f)
    }

    override fun hideOverlay(duration: Int) {
        hideOverlay(duration > 0)
    }

    override fun onAttachedToWindow() {
        isAttached = true
    }

    override fun onDetachedFromWindow() {
        isAttached = false
        containerView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            containerView = null
        }
    }

    override fun onActivityDestroyed() {
        containerView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            containerView = null
        }
    }
}
