package com.cat.ghwidget

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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

    fun fetch(username: String): List<Cell>? = when (val r = fetchWithDetail(username)) {
        is FetchResult.Success -> r.cells
        is FetchResult.Failure -> null
    }

    /** 带失败原因的抓取：用于配置页向用户显示具体错误 */
    fun fetchWithDetail(username: String): FetchResult {
        val request = Request.Builder()
            .url("https://github.com/users/$username/contributions")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val why = when (resp.code) {
                        404 -> "（用户名不存在？）"
                        403, 429 -> "（被 GitHub 限流，稍后再试）"
                        else -> ""
                    }
                    return FetchResult.Failure("GitHub 返回 HTTP ${resp.code}$why")
                }
                val html = resp.body!!.string()
                val cells = parse(html)
                if (cells.isEmpty()) {
                    val diag = buildString {
                        append("len="); append(html.length)
                        append(" svg="); append(if (html.contains("<svg")) 1 else 0)
                        append(" rect="); append(if (html.contains("<rect")) 1 else 0)
                        append(" date="); append(if (html.contains("data-date")) 1 else 0)
                    }
                    FetchResult.Failure("页面无贡献数据（GitHub 可能改版）\n$diag\n${html.take(200).replace('\n', ' ')}")
                } else FetchResult.Success(cells)
            }
        } catch (e: SocketTimeoutException) {
            FetchResult.Failure("连接 GitHub 超时：当前网络可能无法直连 GitHub")
        } catch (e: UnknownHostException) {
            FetchResult.Failure("无法解析 GitHub 域名：请检查网络")
        } catch (e: IOException) {
            FetchResult.Failure("网络错误：${e.message ?: "连接失败"}")
        }
    }

    sealed class FetchResult {
        data class Success(val cells: List<Cell>) : FetchResult()
        data class Failure(val reason: String) : FetchResult()
    }

    /** 属性顺序无关的轻量解析：旧版为 SVG <rect>，2026 新版为表格 <td class="ContributionCalendar-day"> */
    fun parse(html: String): List<Cell> {
        val dateRe = Regex("""data-date="(\d{4}-\d{2}-\d{2})"""")
        val levelRe = Regex("""data-level="(\d)"""")
        val cells = ArrayList<Cell>(380)
        Regex("""<(?:rect|td)[^>]*>""").findAll(html).forEach { m ->
            val tag = m.value
            val date = dateRe.find(tag)?.groupValues?.get(1) ?: return@forEach
            val level = levelRe.find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            cells.add(Cell(date, level))
        }
        return cells.sortedBy { it.date }
    }
}
