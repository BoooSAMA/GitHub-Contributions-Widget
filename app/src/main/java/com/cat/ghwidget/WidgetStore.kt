package com.cat.ghwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.io.File

/** 轻量持久化：解析数据存 SharedPreferences，最近一张渲染图存 cacheDir（无网兜底显示） */
object WidgetStore {

    private const val PREFS = "gh_widget"
    private const val KEY_CELLS = "cells"
    private const val KEY_USER = "username"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_SPAN = "span_weeks"
    private const val KEY_CORNER = "corner_pct"
    private const val KEY_STROKE = "stroke_pct"
    private const val KEY_BG_MODE = "bg_mode"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_BG_ALPHA = "bg_alpha"
    private const val KEY_MARGIN = "margin_pct"
    private const val KEY_CONTRAST = "contrast_pct"
    private const val KEY_GLASS = "glass_enabled"
    private const val KEY_MONTH_MODE = "month_mode"
    private const val KEY_MONTH_COLOR = "month_color"
    private const val KEY_MONTH_SCALE = "month_scale"
    private const val CACHE_FILE = "gh_graph.png"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    /** 小组件显示跨度（周）：0=按窗口尺寸自动适配 */
    const val SPAN_AUTO = 0

    /** 卡牌底色模式：跟随主题 GitHub 色 / 纯透明 / 固定色+透明度 */
    const val BG_AUTO = 0
    const val BG_TRANS = 1
    const val BG_FIXED = 2

    fun themeMode(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_THEME, THEME_SYSTEM)

    fun setThemeMode(ctx: Context, mode: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME, mode).apply()
    }

    fun spanWeeks(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SPAN, SPAN_AUTO)

    fun setSpanWeeks(ctx: Context, weeks: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SPAN, weeks).apply()
    }

    fun cornerPct(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_CORNER, 7)

    fun setCornerPct(ctx: Context, pct: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CORNER, pct).apply()
    }

    fun strokePct(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_STROKE, 7)

    fun setStrokePct(ctx: Context, pct: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_STROKE, pct).apply()
    }

    fun bgMode(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BG_MODE, BG_AUTO)

    fun setBgMode(ctx: Context, mode: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_BG_MODE, mode).apply()
    }

    fun bgColor(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BG_COLOR, 0xFF161B22.toInt())

    fun setBgColor(ctx: Context, color: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_BG_COLOR, color).apply()
    }

    fun bgAlpha(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BG_ALPHA, 100)

    fun setBgAlpha(ctx: Context, pct: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_BG_ALPHA, pct).apply()
    }

    fun marginPct(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MARGIN, 4)

    fun setMarginPct(ctx: Context, pct: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MARGIN, pct).apply()
    }

    fun contrastPct(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_CONTRAST, 30)

    fun setContrastPct(ctx: Context, pct: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CONTRAST, pct).apply()
    }

    fun glassEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_GLASS, true)

    fun setGlassEnabled(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GLASS, on).apply()
    }

    fun monthMode(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MONTH_MODE, BG_AUTO)

    fun setMonthMode(ctx: Context, mode: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MONTH_MODE, mode).apply()
    }

    fun monthColor(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MONTH_COLOR, 0xFF767676.toInt())

    fun setMonthColor(ctx: Context, color: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MONTH_COLOR, color).apply()
    }

    fun monthScale(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MONTH_SCALE, 105)

    fun setMonthScale(ctx: Context, pct: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MONTH_SCALE, pct).apply()
    }

    /** 汇总全部客制化设置为渲染配置；dark 由调用方按当前主题传入 */
    fun renderConfig(ctx: Context, dark: Boolean): RenderConfig {
        val mode = bgMode(ctx)
        val bg: Int? = when (mode) {
            BG_TRANS -> 0x00000000
            BG_FIXED -> {
                val a = (bgAlpha(ctx).coerceIn(0, 100)) * 255 / 100
                (bgColor(ctx) and 0x00FFFFFF) or (a shl 24)
            }
            else -> null
        }
        return RenderConfig(
            spanWeeks = spanWeeks(ctx),
            cornerPct = cornerPct(ctx) / 100f,
            strokePct = strokePct(ctx) / 100f,
            contrast = contrastPct(ctx) / 100f,
            marginPct = marginPct(ctx) / 100f,
            cardBg = bg,
            monthColor = if (monthMode(ctx) == BG_AUTO) null else monthColor(ctx),
            monthScale = monthScale(ctx) / 100f
        )
    }

    fun username(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)
            ?: ctx.getString(R.string.github_username)

    fun saveUsername(ctx: Context, name: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER, name.trim()).apply()
    }

    fun saveCells(ctx: Context, cells: List<GithubGraphFetcher.Cell>) {
        val arr = JSONArray()
        cells.forEach { arr.put(org.json.JSONObject().put("d", it.date).put("l", it.level)) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CELLS, arr.toString()).apply()
    }

    fun loadCells(ctx: Context): List<GithubGraphFetcher.Cell> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CELLS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                GithubGraphFetcher.Cell(o.getString("d"), o.getInt("l"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBitmap(ctx: Context, bitmap: Bitmap) {
        runCatching {
            File(ctx.cacheDir, CACHE_FILE).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    fun loadBitmap(ctx: Context): Bitmap? = runCatching {
        BitmapFactory.decodeFile(File(ctx.cacheDir, CACHE_FILE).absolutePath)
    }.getOrNull()
}
