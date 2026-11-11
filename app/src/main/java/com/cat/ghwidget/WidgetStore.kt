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
    private const val CACHE_FILE = "gh_graph.png"

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
