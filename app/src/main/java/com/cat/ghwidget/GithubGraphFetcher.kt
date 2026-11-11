package com.cat.ghwidget

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 免登录抓取 GitHub 公开贡献页，解析 SVG 热力图节点。
 * 返回按日期升序的格子列表（每 7 个为一列/一周）。
 */
object GithubGraphFetcher {

    /** 一天的贡献格子：date = yyyy-MM-dd，level = 0..4 */
    data class Cell(val date: String, val level: Int)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun fetch(username: String): List<Cell>? {
        val request = Request.Builder()
            .url("https://github.com/users/$username/contributions")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                parse(resp.body!!.string())
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 属性顺序无关的轻量解析：只挑带 data-date 的 <rect> 标签 */
    fun parse(html: String): List<Cell> {
        val dateRe = Regex("""data-date="(\d{4}-\d{2}-\d{2})"""")
        val levelRe = Regex("""data-level="(\d)"""")
        val cells = ArrayList<Cell>(380)
        Regex("""<rect[^>]*>""").findAll(html).forEach { m ->
            val tag = m.value
            val date = dateRe.find(tag)?.groupValues?.get(1) ?: return@forEach
            val level = levelRe.find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            cells.add(Cell(date, level))
        }
        return cells.sortedBy { it.date }
    }
}
