package com.cat.ghwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * 本地 Canvas 绘制高清热力图：圆角方块 + 自定义主题色板，
 * 自动按 widget 宽高比缩放铺满，浅色/深色双主题。
 */
object ContributionRenderer {

    // GitHub 官方绿（浅色 / 深色），改色板即可换主题（猫咪配色、霓虹等）
    private val LIGHT = listOf("EBEDF0", "9BE9A8", "40C463", "30A14E", "216E39").map { Color.parseColor("#$it") }
    private val DARK = listOf("161B22", "0E4429", "006D32", "26A641", "39D353").map { Color.parseColor("#$it") }

    /** 渲染 cells（按日期升序）到 w×h 的透明背景 Bitmap */
    fun render(cells: List<GithubGraphFetcher.Cell>, w: Int, h: Int, dark: Boolean): Bitmap? {
        if (cells.isEmpty() || w < 20 || h < 20) return null
        val palette = if (dark) DARK else LIGHT
        val rows = 7
        val cols = (cells.size + rows - 1) / rows
        val gapRatio = 0.25f

        // 解方程让 52列×7行 恰好铺满可用空间
        val size = minOf(
            w / (cols + (cols - 1) * gapRatio),
            h / (rows + (rows - 1) * gapRatio)
        ).toInt().coerceAtLeast(2)
        val gap = (size * gapRatio).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val radius = size * 0.22f

        for ((i, cell) in cells.withIndex()) {
            val col = i / rows
            val row = i % rows
            val x = col * (size + gap)
            val y = row * (size + gap)
            if (x + size > w) break
            paint.color = palette[cell.level.coerceIn(0, palette.size - 1)]
            canvas.drawRoundRect(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat(), radius, radius, paint)
        }
        return bitmap
    }
}
