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

    /** 渲染 cells（按日期升序）到 w×h 的透明背景 Bitmap；spanWeeks=0 按宽高比自动适配显示最近列，>0 固定周数 */
    fun render(cells: List<GithubGraphFetcher.Cell>, w: Int, h: Int, dark: Boolean, spanWeeks: Int = 0): Bitmap? {
        if (cells.isEmpty() || w < 20 || h < 20) return null
        val palette = if (dark) DARK else LIGHT
        val rows = 7
        val gapRatio = 0.25f
        val totalCols = (cells.size + rows - 1) / rows

        // 高度铺满定单格尺寸，推出宽度能容纳的列数
        val sizeH = (h / (rows + (rows - 1) * gapRatio))
        val gapH = sizeH * gapRatio
        val colsFit = ((w + gapH) / (sizeH + gapH)).toInt().coerceAtLeast(3)
        val displayCols = if (spanWeeks <= 0) minOf(totalCols, colsFit) else minOf(totalCols, spanWeeks)
        if (displayCols < 1) return null

        // 取尾部（最新）的整列数据：起点对齐列边界
        val end = cells.size
        var start = end - displayCols * rows
        start = ((start + rows - 1) / rows) * rows
        if (start >= end) start = end - rows
        val sub = cells.subList(start, end)

        val size = minOf(
            w / (displayCols + (displayCols - 1) * gapRatio),
            h / (rows + (rows - 1) * gapRatio)
        ).toInt().coerceAtLeast(2)
        val gap = (size * gapRatio).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val radius = size * 0.22f

        val gridW = displayCols * size + (displayCols - 1) * gap
        val gridH = rows * size + (rows - 1) * gap
        val ox = ((w - gridW) / 2f)
        val oy = ((h - gridH) / 2f)

        for ((i, cell) in sub.withIndex()) {
            val col = i / rows
            val row = i % rows
            val x = ox + col * (size + gap)
            val y = oy + row * (size + gap)
            paint.color = palette[cell.level.coerceIn(0, palette.size - 1)]
            canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
        }
        return bitmap
    }
}
