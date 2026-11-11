package com.cat.ghwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.concurrent.thread

/**
 * 配置页：输入 GitHub 用户名 → 免登录实时抓取 → Canvas 预览绿墙 →
 * 保存并立即刷新桌面上的所有小组件；底部提供主题模式与小组件跨度设置。
 */
class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var btn: Button
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var error: TextView
    private lateinit var llThemePills: LinearLayout
    private lateinit var llSpanPills: LinearLayout

    override fun attachBaseContext(base: Context) {
        val mode = WidgetStore.themeMode(base)
        if (mode == WidgetStore.THEME_SYSTEM) {
            super.attachBaseContext(base)
            return
        }
        val cfg = Configuration(base.resources.configuration)
        cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (mode == WidgetStore.THEME_DARK) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        super.attachBaseContext(base.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.et_username)
        btn = findViewById(R.id.btn_apply)
        preview = findViewById(R.id.img_preview)
        status = findViewById(R.id.tv_status)
        error = findViewById(R.id.tv_error)
        llThemePills = findViewById(R.id.ll_theme_pills)
        llSpanPills = findViewById(R.id.ll_span_pills)
        setupSliders()

        input.setText(WidgetStore.username(this))
        status.text = getString(R.string.status_idle)

        // 有缓存图先显示，页面不空白
        WidgetStore.loadBitmap(this)?.let { preview.setImageBitmap(it) }

        buildAppearanceSection()
        btn.setOnClickListener { apply() }
    }

    private fun apply() {
        val name = input.text.toString().trim()
        if (name.isEmpty()) {
            showError(getString(R.string.msg_need_username))
            return
        }
        // 加载态：禁用按钮防重复提交
        btn.isEnabled = false
        btn.text = getString(R.string.btn_loading)
        showError(null)

        thread(name = "gh-preview") {
            val result = GithubGraphFetcher.fetchWithDetail(name)
            runOnUiThread {
                when (result) {
                    is GithubGraphFetcher.FetchResult.Failure -> {
                        showError(result.reason)
                        resetButton()
                    }
                    is GithubGraphFetcher.FetchResult.Success -> applyResult(name, result.cells)
                }
            }
        }
    }

    private fun applyResult(name: String, cells: List<GithubGraphFetcher.Cell>) {
        if (cells.isEmpty()) {
            status.setTextColor(getColor(R.color.text_secondary))
            status.text = getString(R.string.msg_empty_user)
            resetButton()
            return
        }
        val w = preview.width.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels - 80)
        val h = preview.height.takeIf { it > 0 } ?: (140 * resources.displayMetrics.density).toInt()
        val bm: Bitmap? = ContributionRenderer.render(
            cells, w, h, isDark(), WidgetStore.spanWeeks(this),
            WidgetStore.cornerPct(this) / 100f, WidgetStore.strokePct(this) / 100f
        )
        WidgetStore.saveUsername(this, name)
        WidgetStore.saveCells(this, cells)
        bm?.let {
            WidgetStore.saveBitmap(this, it)
            preview.setImageBitmap(it)
        }
        status.setTextColor(getColor(R.color.success))
        status.text = getString(R.string.msg_saved, name)
        resetButton()
        refreshWidgets()
    }

    private fun setupSliders() {
        val sbCorner = findViewById<SeekBar>(R.id.sb_corner)
        val tvCorner = findViewById<TextView>(R.id.tv_corner_label)
        val sbStroke = findViewById<SeekBar>(R.id.sb_stroke)
        val tvStroke = findViewById<TextView>(R.id.tv_stroke_label)
        val updateCornerLabel = { v: Int ->
            tvCorner.text = getString(R.string.label_corner, v)
        }
        val updateStrokeLabel = { v: Int ->
            tvStroke.text = getString(R.string.label_stroke, v)
        }
        sbCorner.progress = WidgetStore.cornerPct(this)
        updateCornerLabel(sbCorner.progress)
        sbStroke.progress = WidgetStore.strokePct(this)
        updateStrokeLabel(sbStroke.progress)
        sbCorner.setOnSeekBarChangeListener(sliderListener(
            onChange = { v ->
                WidgetStore.setCornerPct(this, v)
                updateCornerLabel(v)
            },
            onCommit = {
                rebuildPreview()
                refreshWidgets()
            }
        ))
        sbStroke.setOnSeekBarChangeListener(sliderListener(
            onChange = { v ->
                WidgetStore.setStrokePct(this, v)
                updateStrokeLabel(v)
            },
            onCommit = {
                rebuildPreview()
                refreshWidgets()
            }
        ))
    }

    private fun sliderListener(onChange: (Int) -> Unit, onCommit: () -> Unit): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) onChange(value)
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) = onCommit()
        }

    private fun buildAppearanceSection() {
        val themeLabels = listOf(
            getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark)
        )
        buildPills(llThemePills, themeLabels, WidgetStore.themeMode(this)) { mode ->
            if (mode != WidgetStore.themeMode(this)) {
                WidgetStore.setThemeMode(this, mode)
                refreshWidgets()
                recreate()
            }
        }

        val spanOptions = intArrayOf(WidgetStore.SPAN_AUTO, 52, 26, 13)
        val spanLabels = listOf(
            getString(R.string.span_auto), getString(R.string.span_all),
            getString(R.string.span_half), getString(R.string.span_quarter)
        )
        val spanSelected = spanOptions.indexOf(WidgetStore.spanWeeks(this)).let { if (it >= 0) it else 0 }
        buildPills(llSpanPills, spanLabels, spanSelected) { index ->
            if (spanOptions[index] != WidgetStore.spanWeeks(this)) {
                WidgetStore.setSpanWeeks(this, spanOptions[index])
                buildAppearanceSection()
                rebuildPreview()
                refreshWidgets()
            }
        }
    }

    private fun buildPills(container: LinearLayout, labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        container.removeAllViews()
        labels.forEachIndexed { index, label ->
            val pill = TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                minHeight = dp(36)
                minWidth = dp(44)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                background = getDrawable(
                    if (index == selected) R.drawable.pill_bg_active else R.drawable.pill_bg
                )
                setTextColor(
                    if (index == selected) getColor(R.color.primary) else getColor(R.color.text_primary)
                )
                setOnClickListener { onSelect(index) }
            }
            container.addView(
                pill,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
            )
        }
    }

    private fun rebuildPreview() {
        val cells = WidgetStore.loadCells(this)
        if (cells.isEmpty()) return
        val w = preview.width.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels - 80)
        val h = preview.height.takeIf { it > 0 } ?: (140 * resources.displayMetrics.density).toInt()
        ContributionRenderer.render(
            cells, w, h, isDark(), WidgetStore.spanWeeks(this),
            WidgetStore.cornerPct(this) / 100f, WidgetStore.strokePct(this) / 100f
        )?.let {
            WidgetStore.saveBitmap(this, it)
            preview.setImageBitmap(it)
        }
    }

    private fun resetButton() {
        btn.isEnabled = true
        btn.text = getString(R.string.btn_apply)
    }

    private fun showError(msg: String?) {
        if (msg == null) {
            error.visibility = View.GONE
        } else {
            error.text = msg
            error.visibility = View.VISIBLE
        }
    }

    /** 保存成功后立即刷新桌面所有小组件实例 */
    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, GithubWidgetProvider::class.java))
        for (id in ids) GithubWidgetProvider.updateAppWidget(this, mgr, id)
    }

    private fun isDark(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
