package com.cat.ghwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import kotlin.concurrent.thread

/**
 * 绿墙小组件：onUpdate/onAppWidgetOptionsChanged 时
 * 1) 先用本地缓存立即渲染（不闪空白）
 * 2) 后台线程拉最新数据 → Canvas 渲染 → 回主线程更新
 * 点击小组件跳转 GitHub 个人主页。
 */
class GithubWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle
    ) {
        // 用户拖动缩放小组件时按新尺寸重绘
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        private val main = Handler(Looper.getMainLooper())

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_github)
            val username = WidgetStore.username(context)

            // 点击整个组件打开 GitHub 主页
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, appWidgetId,
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$username")),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            // 1) 按当前尺寸用缓存数据立刻渲染
            val (wPx, hPx) = widgetSizePx(context, appWidgetManager, appWidgetId)
            val dark = isDark(context)
            val cachedCells = WidgetStore.loadCells(context)
            val cacheBm = WidgetStore.loadBitmap(context)

            val immediate = ContributionRenderer.render(cachedCells, wPx, hPx, dark) ?: cacheBm
            if (immediate != null) {
                views.setImageViewBitmap(R.id.img_graph, immediate)
                views.setViewVisibility(R.id.tv_status, android.view.View.GONE)
            } else {
                views.setViewVisibility(R.id.tv_status, android.view.View.VISIBLE)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // 2) 后台拉取最新数据
            thread(name = "gh-fetch-$appWidgetId") {
                val cells = GithubGraphFetcher.fetch(username)
                    ?.takeIf { it.isNotEmpty() && it.size >= cachedCells.size }
                    ?: return@thread // 网络失败或数据更旧 → 保留缓存图
                WidgetStore.saveCells(context, cells)
                val fresh = ContributionRenderer.render(cells, wPx, hPx, dark) ?: return@thread
                WidgetStore.saveBitmap(context, fresh)
                main.post {
                    val v = RemoteViews(context.packageName, R.layout.widget_github)
                    v.setImageViewBitmap(R.id.img_graph, fresh)
                    v.setViewVisibility(R.id.tv_status, android.view.View.GONE)
                    appWidgetManager.updateAppWidget(appWidgetId, v)
                }
            }
        }

        /** 用 options 里的 min/max 尺寸（dp）估算可用绘制像素，扣除布局 6dp padding */
        private fun widgetSizePx(context: Context, mgr: AppWidgetManager, id: Int): Pair<Int, Int> {
            val o = mgr.getAppWidgetOptions(id)
            var wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            // 横屏时系统把 min/max 交换，取 max_width 作宽度兜底
            if (wDp <= 0) wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 250)
            val density = context.resources.displayMetrics.density
            val pad = (6 * density).toInt()
            return Pair(
                (wDp * density).toInt().coerceAtLeast(60) - 2 * pad,
                (hDp * density).toInt().coerceAtLeast(60) - 2 * pad
            )
        }

        private fun isDark(context: Context): Boolean =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}
