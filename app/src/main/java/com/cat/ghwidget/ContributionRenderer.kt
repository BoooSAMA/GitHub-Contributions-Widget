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

    // GitHub 原生观感：不透明卡片底色 + 空格子描边让 level0/1 可辨；月份标签色
    private val BG = intArrayOf(Color.WHITE, 0xFF0D1117.toInt())
    private val LABEL = intArrayOf(0xFF767676.toInt(), 0xFF7D8590.toInt())
    private val EMPTY_STROKE = intArrayOf(0xFFD0D7DE.toInt(), 0xFF30363D.toInt())
    private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** 渲染 cells（按日期升序）到 w×h Bitmap；spanWeeks=0 按宽高比自动适配显示最近列，>0 固定周数 */
    fun render(
        cells: List<GithubGraphFetcher.Cell>, w: Int, h: Int, dark: Boolean,
        spanWeeks: Int = 0, cornerPct: Float = 0.07f, strokePct: Float = 0.07f
    ): Bitmap? {
        if (cells.isEmpty() || w < 20 || h < 20) return null
        val bg = BG[if (dark) 1 else 0]
        val labelColor = LABEL[if (dark) 1 else 0]
        val palette = if (dark) DARK else LIGHT
        val rows = 7
        val gapRatio = 0.25f
        val totalCols = (cells.size + rows - 1) / rows

        // 顶部预留月份标签带；格子过小时标签省略
        val labelH = (h * 0.15f).toInt()
        val availH = if (labelH >= 16) h - labelH else h
        val showLabels = availH != h

        // 高度铺满定单格尺寸，推出宽度能容纳的列数
        val sizeH = (availH / (rows + (rows - 1) * gapRatio))
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
            availH / (rows + (rows - 1) * gapRatio)
        ).toInt().coerceAtLeast(2)
        val gap = (size * gapRatio).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val radius = size * 0.22f
        val cardRadius = minOf(w, h) * cornerPct
        paint.color = bg
        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), cardRadius, cardRadius, paint)
        paint.style = Paint.Style.FILL_AND_STROKE

        val gridW = displayCols * size + (displayCols - 1) * gap
        val gridH = rows * size + (rows - 1) * gap
        val ox = ((w - gridW) / 2f)
        val oy = ((h - gridH) / 2f).coerceAtLeast(labelH.toFloat())

        val strokeW = if (strokePct <= 0f) 0f else maxOf(1f, size * strokePct)
        val strokePaint = if (strokeW > 0f) Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            setColor(EMPTY_STROKE[if (dark) 1 else 0])
            strokeWidth = strokeW
        } else null
        for ((i, cell) in sub.withIndex()) {
            val col = i / rows
            val row = i % rows
            val x = ox + col * (size + gap)
            val y = oy + row * (size + gap)
            paint.color = palette[cell.level.coerceIn(0, palette.size - 1)]
            canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
            if (cell.level == 0) strokePaint?.let { canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, it) }
        }

        if (showLabels && size >= 6) drawMonthLabels(canvas, sub, size, gap, ox, labelH, labelColor, displayCols, rows)
        return bitmap
    }

    /** 月份标签画在每月首列上方，与上一标签重叠时跳过（GitHub 同款处理） */
    private fun drawMonthLabels(
        canvas: Canvas, sub: List<GithubGraphFetcher.Cell>, size: Int, gap: Int,
        ox: Float, labelH: Int, labelPaintColor: Int, displayCols: Int, rows: Int
    ) {
        val firstCol = mutableMapOf<String, Int>()
        sub.forEachIndexed { i, cell ->
            firstCol.putIfAbsent(cell.date.take(7), i / rows)
        }
        val labelColorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setColor(labelPaintColor)
            textSize = (size * 1.05f).coerceIn(10f, 22f)
        }
        val baseline = labelH * 0.62f
        var lastEnd = -1f
        val labels = firstCol.entries.sortedBy { it.value }
        labels.forEach { (ym, col) ->
            val month = MONTHS[ym.substring(5, 7).toInt() - 1]
            val x = ox + col * (size + gap)
            val textW = labelColorPaint.measureText(month)
            if (x < lastEnd) return@forEach
            if (x + textW > ox + displayCols * (size + gap)) return@forEach
            canvas.drawText(month, x, baseline, labelColorPaint)
            lastEnd = x + textW + size * 0.4f
        }
    }
}
