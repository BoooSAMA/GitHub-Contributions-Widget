package com.cat.ghwidget

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** 安装成功说明页（无业务逻辑） */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv_user).text =
            getString(R.string.hint_user, WidgetStore.username(this))
    }
}
