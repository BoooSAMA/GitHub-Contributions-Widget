package com.cat.ghwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** 渲染客制化参数全部收拢于此；默认值 = GitHub 原生观感 */
data class RenderConfig(
    val spanWeeks: Int = 0,
    val cornerPct: Float = 0.07f,
    val strokePct: Float = 0.07f,
    val contrast: Float = 0f,
    val marginPct: Float = 0f,
    /** null = 跟随主题的 GitHub 卡片色；否则用此 ARGB（alpha 0 = 纯透明底） */
    val cardBg: Int? = null,
    /** null = 跟随主题的 GitHub 月份灰；否则固定色 */
    val monthColor: Int? = null,
    /** 月份字号相对格子尺寸的倍率（1.05 = GitHub 观感） */
    val monthScale: Float = 1.05f
)

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

    // 对比度增强的目标色：空格子/level1/描边从 GitHub 原色向更可辨的颜色插值
    private val L0_FROM = intArrayOf(0xFFEBEDF0.toInt(), 0xFF161B22.toInt())
    private val L0_TO = intArrayOf(0xFFC9D1D9.toInt(), 0xFF4E5A64.toInt())
    private val L1_FROM = intArrayOf(0xFF9BE9A8.toInt(), 0xFF0E4429.toInt())
    private val L1_TO = intArrayOf(0xFF8CD998.toInt(), 0xFF1F7139.toInt())
    private val STROKE_FROM = intArrayOf(0xFFD0D7DE.toInt(), 0xFF30363D.toInt())
    private val STROKE_TO = intArrayOf(0xFFA8B2BC.toInt(), 0xFF5A6670.toInt())
    private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    private fun blend(from: Int, to: Int, t: Float): Int {
        if (t <= 0f) return from
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r, g, b)
    }

    /** 渲染 cells（按日期升序）到 w×h Bitmap；spanWeeks=0 按宽高比自动适配显示最近列，>0 固定周数 */
    fun render(cells: List<GithubGraphFetcher.Cell>, w: Int, h: Int, dark: Boolean, cfg: RenderConfig = RenderConfig()): Bitmap? {
        if (cells.isEmpty() || w < 20 || h < 20) return null
        val palette = if (dark) DARK else LIGHT
        val rows = 7
        val gapRatio = 0.25f
        val totalCols = (cells.size + rows - 1) / rows

        // 边距：卡牌整体内缩，0 = 铺满整个组件区域
        val inset = (minOf(w, h) * cfg.marginPct).toInt()
        val cw = w - 2 * inset
        val ch = h - 2 * inset
        if (cw < 20 || ch < 20) return null

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        canvas.save()
        canvas.translate(inset.toFloat(), inset.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // 卡牌底色：alpha=0 纯透明不画底，其余画圆角矩形
        val bg = cfg.cardBg ?: BG[if (dark) 1 else 0]
        if (Color.alpha(bg) > 0) {
            val cardRadius = minOf(cw, ch) * cfg.cornerPct
            paint.color = bg
            canvas.drawRoundRect(0f, 0f, cw.toFloat(), ch.toFloat(), cardRadius, cardRadius, paint)
        }

        // 顶部预留月份标签带；格子过小时标签省略
        val labelH = (ch * 0.15f).toInt()
        val availH = if (labelH >= 16) ch - labelH else ch
        val showLabels = availH != ch

        // 高度铺满定单格尺寸，推出宽度能容纳的列数
        val sizeH = (availH / (rows + (rows - 1) * gapRatio))
        val gapH = sizeH * gapRatio
        val colsFit = ((cw + gapH) / (sizeH + gapH)).toInt().coerceAtLeast(3)
        val displayCols = if (cfg.spanWeeks <= 0) minOf(totalCols, colsFit) else minOf(totalCols, cfg.spanWeeks)
        if (displayCols < 1) {
            canvas.restore()
            return bitmap
        }

        // 取尾部（最新）的整列数据：起点对齐列边界
        val end = cells.size
        var start = end - displayCols * rows
        start = ((start + rows - 1) / rows) * rows
        if (start >= end) start = end - rows
        val sub = cells.subList(start, end)

        val size = minOf(
            cw / (displayCols + (displayCols - 1) * gapRatio),
            availH / (rows + (rows - 1) * gapRatio)
        ).toInt().coerceAtLeast(2)
        val gap = (size * gapRatio).toInt().coerceAtLeast(1)
        val radius = size * 0.22f

        val gridW = displayCols * size + (displayCols - 1) * gap
        val gridH = rows * size + (rows - 1) * gap
        val ox = ((cw - gridW) / 2f)
        val oy = ((ch - gridH) / 2f).coerceAtLeast(labelH.toFloat())

        val t = cfg.contrast.coerceIn(0f, 1f)
        val strokeW = if (cfg.strokePct <= 0f) 0f else maxOf(1f, size * cfg.strokePct)
        val strokeColor = blend(STROKE_FROM[if (dark) 1 else 0], STROKE_TO[if (dark) 1 else 0], t)
        val strokePaint = if (strokeW > 0f) Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = strokeColor
            strokeWidth = strokeW
        } else null

        val p0 = blend(L0_FROM[if (dark) 1 else 0], L0_TO[if (dark) 1 else 0], t)
        val p1 = blend(L1_FROM[if (dark) 1 else 0], L1_TO[if (dark) 1 else 0], t)
        for ((i, cell) in sub.withIndex()) {
            val col = i / rows
            val row = i % rows
            val x = ox + col * (size + gap)
            val y = oy + row * (size + gap)
            paint.color = when (cell.level) {
                0 -> p0
                1 -> p1
                else -> palette[cell.level.coerceIn(0, palette.size - 1)]
            }
            canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
            if (cell.level == 0) strokePaint?.let { canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, it) }
        }

        if (showLabels && size >= 6) {
            val mColor = cfg.monthColor ?: LABEL[if (dark) 1 else 0]
            drawMonthLabels(canvas, sub, size, gap, ox, labelH, mColor, displayCols, cfg.monthScale)
        }
        canvas.restore()
        return bitmap
    }

    /** 月份标签画在每月首列上方，与上一标签重叠时跳过（GitHub 同款处理） */
    private fun drawMonthLabels(
        canvas: Canvas, sub: List<GithubGraphFetcher.Cell>, size: Int, gap: Int,
        ox: Float, labelH: Int, labelPaintColor: Int, displayCols: Int, scale: Float
    ) {
        val firstCol = mutableMapOf<String, Int>()
        sub.forEachIndexed { i, cell ->
            firstCol.putIfAbsent(cell.date.take(7), i / 7)
        }
        val labelColorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setColor(labelPaintColor)
            textSize = (size * scale).coerceIn(6f, 40f)
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
