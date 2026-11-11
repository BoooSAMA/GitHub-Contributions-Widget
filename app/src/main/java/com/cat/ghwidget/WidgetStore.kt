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
    private const val CACHE_FILE = "gh_graph.png"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    /** 小组件显示跨度（周）：0=按窗口尺寸自动适配 */
    const val SPAN_AUTO = 0

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
