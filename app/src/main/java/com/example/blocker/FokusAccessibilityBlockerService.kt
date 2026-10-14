package com.example.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.database.FokusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FokusAccessibilityBlockerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastBlockedPkg: String? = null
    private var lastBlockedTime: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName == packageName) return // Ignore self

        val isSessionActive = BlockerManager.isFocusActive.value
        if (!isSessionActive) return

        val blockedSet = BlockerManager.blockedPackagesSet.value
        if (blockedSet.contains(pkgName)) {
            val now = System.currentTimeMillis()
            // Throttle duplicate events within 1.5 seconds
            if (pkgName == lastBlockedPkg && (now - lastBlockedTime) < 1500) {
                return
            }
            lastBlockedPkg = pkgName
            lastBlockedTime = now

            // Launch Blocker Overlay Activity
            val pm = packageManager
            val appLabel = runCatching {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(pkgName)

            // Log interruption in database
            serviceScope.launch {
                val db = FokusDatabase.getInstance(applicationContext)
                val sessionId = BlockerManager.currentActiveSessionId.value
                db.fokusDao().insertInterruptionLog(
                    com.example.data.model.InterruptionLogEntity(
                        packageName = pkgName,
                        appName = appLabel,
                        duringSessionId = sessionId
                    )
                )
            }

            // Launch overlay
            val overlayIntent = Intent(this, BlockedOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_APP_NAME, appLabel)
                putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_PACKAGE, pkgName)
            }
            startActivity(overlayIntent)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
