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
import java.util.Locale
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
    private lateinit var llBgPills: LinearLayout
    private lateinit var llGlassPills: LinearLayout
    private lateinit var llMonthPills: LinearLayout
    private lateinit var llLangPills: LinearLayout

    override fun attachBaseContext(base: Context) {
        val cfg = Configuration(base.resources.configuration)
        val mode = WidgetStore.themeMode(base)
        if (mode != WidgetStore.THEME_SYSTEM) {
            cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (mode == WidgetStore.THEME_DARK) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val locale = Locale(if (WidgetStore.langMode(base) == WidgetStore.LANG_ZH) "zh" else "en")
        cfg.setLocale(locale)
        Locale.setDefault(locale)
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
        llBgPills = findViewById(R.id.ll_bg_pills)
        llGlassPills = findViewById(R.id.ll_glass_pills)
        llMonthPills = findViewById(R.id.ll_month_pills)
        llLangPills = findViewById(R.id.ll_lang_pills)
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
                        showError(failureMessage(result))
                        resetButton()
                    }
                    is GithubGraphFetcher.FetchResult.Success -> applyResult(name, result.cells)
                }
            }
        }
    }

    private fun failureMessage(f: GithubGraphFetcher.FetchResult.Failure): String {
        val resId = when (f.kind) {
            GithubGraphFetcher.FetchResult.Failure.Kind.HTTP -> R.string.error_http
            GithubGraphFetcher.FetchResult.Failure.Kind.NOT_FOUND -> R.string.error_http_404
            GithubGraphFetcher.FetchResult.Failure.Kind.RATE_LIMIT -> R.string.error_http_429
            GithubGraphFetcher.FetchResult.Failure.Kind.EMPTY -> R.string.error_empty
            GithubGraphFetcher.FetchResult.Failure.Kind.TIMEOUT -> R.string.error_timeout
            GithubGraphFetcher.FetchResult.Failure.Kind.DNS -> R.string.error_dns
            GithubGraphFetcher.FetchResult.Failure.Kind.NETWORK -> R.string.error_network
        }
        return if (f.detail.isEmpty()) getString(resId) else getString(resId, f.detail)
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
        val bm: Bitmap? = ContributionRenderer.render(cells, w, h, isDark(), WidgetStore.renderConfig(this, isDark()))
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

        val sbAlpha = findViewById<SeekBar>(R.id.sb_bgalpha)
        val tvAlpha = findViewById<TextView>(R.id.tv_bgalpha_label)
        val updateAlphaLabel = { v: Int ->
            tvAlpha.text = getString(R.string.label_bgalpha, v)
        }
        sbAlpha.progress = WidgetStore.bgAlpha(this)
        updateAlphaLabel(sbAlpha.progress)
        sbAlpha.setOnSeekBarChangeListener(sliderListener(
            onChange = { v ->
                WidgetStore.setBgAlpha(this, v)
                updateAlphaLabel(v)
            },
            onCommit = {
                rebuildPreview()
                refreshWidgets()
            }
        ))

        val sbMargin = findViewById<SeekBar>(R.id.sb_margin)
        val tvMargin = findViewById<TextView>(R.id.tv_margin_label)
        val updateMarginLabel = { v: Int ->
            tvMargin.text = getString(R.string.label_margin, v)
        }
        sbMargin.progress = WidgetStore.marginPct(this)
        updateMarginLabel(sbMargin.progress)
        sbMargin.setOnSeekBarChangeListener(sliderListener(
            onChange = { v ->
                WidgetStore.setMarginPct(this, v)
                updateMarginLabel(v)
            },
            onCommit = {
                rebuildPreview()
                refreshWidgets()
            }
        ))

        val sbContrast = findViewById<SeekBar>(R.id.sb_contrast)
        val tvContrast = findViewById<TextView>(R.id.tv_contrast_label)
        val updateContrastLabel = { v: Int ->
            tvContrast.text = getString(R.string.label_contrast, v)
        }
        sbContrast.progress = WidgetStore.contrastPct(this)
        updateContrastLabel(sbContrast.progress)
        sbContrast.setOnSeekBarChangeListener(sliderListener(
            onChange = { v ->
                WidgetStore.setContrastPct(this, v)
                updateContrastLabel(v)
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
        val langLabels = listOf(getString(R.string.lang_en), getString(R.string.lang_zh))
        buildPills(llLangPills, langLabels, WidgetStore.langMode(this)) { index ->
            if (index != WidgetStore.langMode(this)) {
                WidgetStore.setLangMode(this, index)
                recreate()
            }
        }

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

        val bgOptions = intArrayOf(
            WidgetStore.BG_AUTO, WidgetStore.BG_TRANS,
            0xFFFFFFFF.toInt(), 0xFFF6F8FA.toInt(), 0xFF000000.toInt(), 0xFF161B22.toInt()
        )
        val bgLabels = listOf(
            getString(R.string.bg_auto), getString(R.string.bg_trans),
            getString(R.string.bg_white), getString(R.string.bg_offwhite),
            getString(R.string.bg_black), getString(R.string.bg_charcoal)
        )
        val bgSel = if (WidgetStore.bgMode(this) == WidgetStore.BG_FIXED) {
            val colors = bgOptions.drop(2)
            val idx = colors.indexOf(WidgetStore.bgColor(this))
            if (idx >= 0) idx + 2 else -1
        } else if (WidgetStore.bgMode(this) == WidgetStore.BG_TRANS) 1 else 0
        buildPills(llBgPills, bgLabels, bgSel) { index ->
            val opt = bgOptions[index]
            if (opt == WidgetStore.BG_AUTO || opt == WidgetStore.BG_TRANS) {
                WidgetStore.setBgMode(this, opt)
            } else {
                WidgetStore.setBgMode(this, WidgetStore.BG_FIXED)
                WidgetStore.setBgColor(this, opt)
            }
            buildAppearanceSection()
            rebuildPreview()
            refreshWidgets()
        }

        val glassLabels = listOf(getString(R.string.glass_on), getString(R.string.glass_off))
        buildPills(llGlassPills, glassLabels, if (WidgetStore.glassEnabled(this)) 0 else 1) { index ->
            val on = index == 0
            if (on != WidgetStore.glassEnabled(this)) {
                WidgetStore.setGlassEnabled(this, on)
                buildAppearanceSection()
                refreshWidgets()
            }
        }

        val monthOptions = intArrayOf(
            WidgetStore.BG_AUTO, 0xFF767676.toInt(), 0xFF4E5A64.toInt(),
            0xFFFFFFFF.toInt(), 0xFF000000.toInt()
        )
        val monthLabels = listOf(
            getString(R.string.bg_auto), getString(R.string.month_gray),
            getString(R.string.month_slate), getString(R.string.bg_white), getString(R.string.bg_black)
        )
        val monthSel = if (WidgetStore.monthMode(this) == WidgetStore.BG_AUTO) 0
        else {
            val idx = monthOptions.drop(1).indexOf(WidgetStore.monthColor(this))
            if (idx >= 0) idx + 1 else -1
        }
        buildPills(llMonthPills, monthLabels, monthSel) { index ->
            val opt = monthOptions[index]
            if (opt == WidgetStore.BG_AUTO) {
                WidgetStore.setMonthMode(this, WidgetStore.BG_AUTO)
            } else {
                WidgetStore.setMonthMode(this, WidgetStore.BG_FIXED)
                WidgetStore.setMonthColor(this, opt)
            }
            buildAppearanceSection()
            rebuildPreview()
            refreshWidgets()
        }

        val sbMonth = findViewById<SeekBar>(R.id.sb_monthscale)
        val tvMonth = findViewById<TextView>(R.id.tv_monthscale_label)
        val updateMonthLabel = { v: Int ->
            tvMonth.text = getString(R.string.label_monthscale, v)
        }
        sbMonth.progress = WidgetStore.monthScale(this).coerceIn(60, 200)
        updateMonthLabel(sbMonth.progress)
        sbMonth.setOnSeekBarChangeListener(sliderListener(
            onChange = { v ->
                WidgetStore.setMonthScale(this, v)
                updateMonthLabel(v)
            },
            onCommit = {
                rebuildPreview()
                refreshWidgets()
            }
        ))
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
        ContributionRenderer.render(cells, w, h, isDark(), WidgetStore.renderConfig(this, isDark()))?.let {
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
