package com.wudi.qishuihelper

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var logView: TextView
    private lateinit var statusView: TextView
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }

        val title = TextView(this).apply {
            text = "汽水助手"
            textSize = 28f
            setTextColor(0xFF17202A.toInt())
            typeface = Typeface.DEFAULT_BOLD
        }
        page.addView(title)

        val subtitle = TextView(this).apply {
            text = "汽水音乐快捷入口与手动操作辅助"
            textSize = 14f
            setTextColor(0xFF657080.toInt())
            setPadding(0, 8, 0, 22)
        }
        page.addView(subtitle)

        statusView = TextView(this).apply {
            text = "状态：待机"
            textSize = 16f
            setTextColor(0xFF176B45.toInt())
            setPadding(0, 14, 0, 18)
        }
        page.addView(statusView)

        page.addView(makeButton("打开汽水音乐") {
            val launch = packageManager.getLaunchIntentForPackage(TARGET_PACKAGE)
            if (launch != null) {
                startActivity(launch)
                statusView.text = "状态：已请求打开汽水音乐"
                addLog("已启动汽水音乐")
            } else {
                statusView.text = "状态：未检测到汽水音乐"
                addLog("未检测到汽水音乐，请先安装官方应用")
                Toast.makeText(this, "未检测到汽水音乐，请先安装官方应用", Toast.LENGTH_LONG).show()
            }
        })

        page.addView(makeButton("开启页面状态识别（无障碍权限）") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            addLog("已打开无障碍设置，请手动开启汽水助手")
        })

        page.addView(makeButton("检查识别服务状态") {
            val enabled = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val active = enabled.contains(
                "$packageName/.QishuiAccessibilityService",
                ignoreCase = true
            ) || enabled.contains(
                "$packageName/com.wudi.qishuihelper.QishuiAccessibilityService",
                ignoreCase = true
            )
            statusView.text = if (active) "状态：页面识别已开启" else "状态：页面识别未开启"
            addLog(if (active) "无障碍识别服务已开启" else "无障碍识别服务尚未开启")
        })

        page.addView(makeButton("打开汽水音乐应用信息") {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$TARGET_PACKAGE")
                })
                addLog("已打开汽水音乐应用信息页")
            } catch (_: Exception) {
                addLog("无法打开应用信息页")
            }
        })

        val note = TextView(this).apply {
            text = "说明：识别汽水音乐页面中的状态文字并发送提醒；所有页面操作均由你手动完成。本工具不会自动点击、模拟观看广告或领取奖励。首次使用请开启无障碍权限，并允许通知。"
            textSize = 13f
            setTextColor(0xFF596579.toInt())
            setPadding(0, 18, 0, 18)
        }
        page.addView(note)

        val logTitle = TextView(this).apply {
            text = "运行日志"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF17202A.toInt())
            setPadding(0, 8, 0, 8)
        }
        page.addView(logTitle)

        logView = TextView(this).apply {
            text = "尚无记录"
            textSize = 13f
            setTextColor(0xFF465160.toInt())
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val scroll = ScrollView(this).apply { addView(logView) }
        page.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(page)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        addLog("汽水助手已启动")
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }

    private fun addLog(message: String) {
        val line = "[${dateFormat.format(Date())}] $message"
        logView.text = if (logView.text.toString() == "尚无记录") line
        else "$line\n${logView.text}"
    }

    companion object {
        private const val TARGET_PACKAGE = "com.luna.music"
    }
}
