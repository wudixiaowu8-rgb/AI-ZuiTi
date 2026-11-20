package com.wudi.qishuihelper

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

class QishuiAccessibilityService : AccessibilityService() {
    private val phrases = listOf("广告已结束", "广告播放完成", "领取奖励", "奖励已到账", "领取成功")
    private var lastPhrase = ""
    private var lastNoticeAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != TARGET_PACKAGE) return

        val collected = StringBuilder()
        event.text?.forEach { collected.append(it).append(' ') }
        rootInActiveWindow?.let { root ->
            collectText(root, collected, 0)
            root.recycle()
        }

        val screenText = collected.toString()
        val match = phrases.firstOrNull { screenText.contains(it, ignoreCase = true) } ?: return
        val now = System.currentTimeMillis()
        if (match == lastPhrase && now - lastNoticeAt < 45_000L) return

        lastPhrase = match
        lastNoticeAt = now
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("last_status", match)
            .putLong("last_status_at", now)
            .apply()
        showReminder(match)
    }

    private fun collectText(node: AccessibilityNodeInfo, out: StringBuilder, depth: Int) {
        if (depth > 18) return
        node.text?.let { out.append(it).append(' ') }
        node.contentDescription?.let { out.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, out, depth + 1)
            child.recycle()
        }
    }

    private fun showReminder(phrase: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "汽水助手提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("汽水音乐状态提醒")
            .setContentText("检测到“$phrase”，请自行确认页面并手动操作。")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "检测到“$phrase”。汽水助手不会自动点击、观看广告或领取奖励，请自行确认页面并手动操作。"
            ))
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(2201, notification)
        } catch (_: SecurityException) {
            // Notifications may be disabled by the user.
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TARGET_PACKAGE = "com.luna.music"
        private const val PREFS = "qishui_helper"
        private const val CHANNEL_ID = "qishui_status"
    }
}
