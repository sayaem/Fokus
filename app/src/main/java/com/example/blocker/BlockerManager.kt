package com.example.blocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false
)

data class PermissionDiagnostics(
    val hasUsageStats: Boolean,
    val hasOverlay: Boolean,
    val hasAccessibility: Boolean,
    val isBatteryOptimizationIgnored: Boolean,
    val hasPostNotifications: Boolean
) {
    val isStrictBlockingReady: Boolean
        get() = (hasAccessibility || (hasUsageStats && hasOverlay))
}

object BlockerManager {

    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private val _currentActiveSessionId = MutableStateFlow<String?>(null)
    val currentActiveSessionId: StateFlow<String?> = _currentActiveSessionId.asStateFlow()

    private val _blockedPackagesSet = MutableStateFlow<Set<String>>(emptySet())
    val blockedPackagesSet: StateFlow<Set<String>> = _blockedPackagesSet.asStateFlow()

    fun startFocusSession(sessionId: String, blockedPackages: Set<String>) {
        _currentActiveSessionId.value = sessionId
        _blockedPackagesSet.value = blockedPackages
        _isFocusActive.value = true
    }

    fun stopFocusSession() {
        _isFocusActive.value = false
        _currentActiveSessionId.value = null
    }

    fun emergencyUnblock() {
        stopFocusSession()
    }

    fun checkDiagnostics(context: Context): PermissionDiagnostics {
        val hasUsage = checkUsageStatsPermission(context)
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        val hasAccess = isAccessibilityServiceEnabled(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        return PermissionDiagnostics(
            hasUsageStats = hasUsage,
            hasOverlay = hasOverlay,
            hasAccessibility = hasAccess,
            isBatteryOptimizationIgnored = batteryIgnored,
            hasPostNotifications = hasNotif
        )
    }

    fun checkUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${FokusAccessibilityBlockerService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                componentName.contains(FokusAccessibilityBlockerService::class.java.simpleName)
            ) {
                return true
            }
        }
        return false
    }

    fun getInstalledUserApps(context: Context, selectedPackages: Set<String>): List<AppInfoItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val list = mutableListOf<AppInfoItem>()
        val seen = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            // Skip FOKUS and core system essentials
            if (pkg == context.packageName || seen.contains(pkg) || isEssentialSystemApp(pkg)) {
                continue
            }
            seen.add(pkg)
            val appName = info.loadLabel(pm).toString()
            val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            list.add(
                AppInfoItem(
                    packageName = pkg,
                    appName = appName,
                    isSystemApp = isSystem,
                    isSelected = selectedPackages.contains(pkg)
                )
            )
        }
        return list.sortedBy { it.appName.lowercase() }
    }

    private fun isEssentialSystemApp(pkg: String): Boolean {
        return pkg.startsWith("com.android.dialer") ||
                pkg.startsWith("com.android.phone") ||
                pkg.startsWith("com.android.emergency") ||
                pkg.startsWith("com.google.android.dialer") ||
                pkg == "com.android.settings" ||
                pkg == "com.google.android.packageinstaller" ||
                pkg == "com.android.systemui"
    }

    fun parsePackagesJson(json: String): Set<String> {
        return try {
            val array = JSONArray(json)
            val set = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun packagesToJson(set: Set<String>): String {
        val array = JSONArray()
        set.forEach { array.put(it) }
        return array.toString()
    }
}
