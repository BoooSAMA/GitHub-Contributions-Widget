package com.cat.ghwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import kotlin.concurrent.thread

/**
 * 配置页：输入 GitHub 用户名 → 免登录实时抓取 → Canvas 预览绿墙 →
 * 保存并立即刷新桌面上的所有小组件。
 */
class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var btn: Button
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var error: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.et_username)
        btn = findViewById(R.id.btn_apply)
        preview = findViewById(R.id.img_preview)
        status = findViewById(R.id.tv_status)
        error = findViewById(R.id.tv_error)

        input.setText(WidgetStore.username(this))
        status.text = getString(R.string.status_idle)

        // 有缓存图先显示，页面不空白
        WidgetStore.loadBitmap(this)?.let { preview.setImageBitmap(it) }

        btn.setOnClickListener { apply() }
    }

    private fun apply() {
        val name = input.text.toString().trim()
        if (name.isEmpty()) {
            showError(getString(R.string.msg_need_username))
            return
        }
        // 加载态：禁用按钮防重复提交
        btn.isEnabled = false
        btn.text = getString(R.string.btn_loading)
        showError(null)

        thread(name = "gh-preview") {
            val result = GithubGraphFetcher.fetchWithDetail(name)
            runOnUiThread {
                when (result) {
                    is GithubGraphFetcher.FetchResult.Failure -> {
                        showError(result.reason)
                        resetButton()
                    }
                    is GithubGraphFetcher.FetchResult.Success -> applyResult(name, result.cells)
                }
            }
        }
    }

    private fun applyResult(name: String, cells: List<GithubGraphFetcher.Cell>) {
        if (cells.isEmpty()) {
            status.setTextColor(getColor(R.color.text_secondary))
            status.text = getString(R.string.msg_empty_user)
            resetButton()
            return
        }
        val w = preview.width.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels - 80)
        val h = preview.height.takeIf { it > 0 } ?: (140 * resources.displayMetrics.density).toInt()
        val bm: Bitmap? = ContributionRenderer.render(cells, w, h, isDark())
        WidgetStore.saveUsername(this, name)
        WidgetStore.saveCells(this, cells)
        bm?.let {
            WidgetStore.saveBitmap(this, it)
            preview.setImageBitmap(it)
        }
        status.setTextColor(getColor(R.color.success))
        status.text = getString(R.string.msg_saved, name)
        resetButton()
        refreshWidgets()
    }

    private fun resetButton() {
        btn.isEnabled = true
        btn.text = getString(R.string.btn_apply)
    }

    private fun showError(msg: String?) {
        if (msg == null) {
            error.visibility = android.view.View.GONE
        } else {
            error.text = msg
            error.visibility = android.view.View.VISIBLE
        }
    }

    /** 保存成功后立即刷新桌面所有小组件实例 */
    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, GithubWidgetProvider::class.java))
        for (id in ids) GithubWidgetProvider.updateAppWidget(this, mgr, id)
    }

    private fun isDark(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
