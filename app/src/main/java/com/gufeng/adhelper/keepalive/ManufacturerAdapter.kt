package com.gufeng.adhelper.keepalive

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * 厂商适配引导工具
 * 作者：古封
 * 功能：针对不同手机厂商，引导用户开启必要的后台权限
 */
class ManufacturerAdapter(private val context: Context) {

    companion object {
        // 厂商识别
        private val MANUFACTURERS = mapOf(
            "xiaomi" to XiaomiAdapter(),
            "redmi" to XiaomiAdapter(),
            "huawei" to HuaweiAdapter(),
            "honor" to HuaweiAdapter(),
            "oppo" to OppoAdapter(),
            "realme" to OppoAdapter(),
            "vivo" to VivoAdapter(),
            "samsung" to SamsungAdapter(),
            "meizu" to MeizuAdapter(),
            "oneplus" to OnePlusAdapter(),
            "lenovo" to LenovoAdapter(),
            "asus" to AsusAdapter()
        )
    }

    /**
     * 获取当前厂商的适配器
     */
    fun getAdapter(): ManufacturerAdapterBase {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return MANUFACTURERS.entries.find { manufacturer.contains(it.key) }?.value 
            ?: DefaultAdapter()
    }

    /**
     * 获取厂商名称
     */
    fun getManufacturerName(): String {
        return Build.MANUFACTURER
    }

    /**
     * 获取设备型号
     */
    fun getDeviceModel(): String {
        return Build.MODEL
    }

    /**
     * 是否为需要特殊处理的厂商
     */
    fun isSpecialManufacturer(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return MANUFACTURERS.keys.any { manufacturer.contains(it) }
    }
}

/**
 * 厂商适配器基类
 */
abstract class ManufacturerAdapterBase {
    
    /**
     * 获取厂商名称
     */
    abstract fun getName(): String
    
    /**
     * 获取自启动设置Intent
     */
    abstract fun getAutoStartIntent(context: Context): Intent?
    
    /**
     * 获取电池优化Intent
     */
    abstract fun getBatteryOptimizationIntent(context: Context): Intent?
    
    /**
     * 获取后台弹出界面权限Intent
     */
    abstract fun getDisplayOverOtherAppsIntent(context: Context): Intent?
    
    /**
     * 获取完整的引导步骤
     */
    abstract fun getGuideSteps(): List<String>
    
    /**
     * 获取简要提示
     */
    abstract fun getBriefTip(): String
}

/**
 * 小米适配器
 */
class XiaomiAdapter : ManufacturerAdapterBase() {
    override fun getName() = "小米/红米"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
        } catch (e: Exception) {
            getDefaultAutoStartIntent()
        }
    }
    
    private fun getDefaultAutoStartIntent(): Intent {
        return Intent().apply {
            action = "miui.intent.action.OP_AUTO_START"
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", "汽水音乐广告助手")
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            putExtra("extra_pkgname", context.packageName)
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「手机管家」",
        "点击「应用管理」",
        "找到并点击「汽水音乐广告助手」",
        "开启「自启动」开关",
        "点击「省电策略」选择「无限制」"
    )
    
    override fun getBriefTip() = "开启自启动+关闭省电限制"
}

/**
 * 华为适配器
 */
class HuaweiAdapter : ManufacturerAdapterBase() {
    override fun getName() = "华为/荣耀"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
        } catch (e: Exception) {
            try {
                Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.permission.PermissionActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「手机管家」",
        "点击「启动管理」",
        "找到「汽水音乐广告助手」",
        "关闭「自动管理」",
        "手动开启「自启动」和「允许后台活动」"
    )
    
    override fun getBriefTip() = "开启自启动和后台运行权限"
}

/**
 * OPPO适配器
 */
class OppoAdapter : ManufacturerAdapterBase() {
    override fun getName() = "OPPO/真我"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        } catch (e: Exception) {
            try {
                Intent().apply {
                    component = ComponentName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                }
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.coloros.safecenter"
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.coloros.safecenter"
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「手机管家」",
        "点击「权限隐私」",
        "点击「自启动管理」",
        "开启「汽水音乐广告助手」",
        "在「电量」设置中允许后台运行"
    )
    
    override fun getBriefTip() = "开启自启动和后台运行"
}

/**
 * Vivo适配器
 */
class VivoAdapter : ManufacturerAdapterBase() {
    override fun getName() = "Vivo"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return Intent().apply {
            action = "vivo.intent.action.HOME_MANAGER"
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent().apply {
            action = "vivo.intent.action.PERMISSION_EDITOR"
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「i管家」",
        "点击「应用管理」",
        "切换到「权限管理」",
        "找到「汽水音乐广告助手」",
        "开启「自启动」和「后台管理」"
    )
    
    override fun getBriefTip() = "开启自启动和后台管理"
}

/**
 * 三星适配器
 */
class SamsungAdapter : ManufacturerAdapterBase() {
    override fun getName() = "三星"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.samsung.android.sm.ui.battery.BatteryActivity"
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent().apply {
            action = "android.settings.APPLICATION_DETAILS_SETTINGS"
            data = android.net.Uri.parse("package:${context.packageName}")
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「设置」",
        "点击「电池和设备维护」",
        "点击「电池」",
        "点击「后台使用情况」",
        "找到「汽水音乐广告助手」并设为「允许」"
    )
    
    override fun getBriefTip() = "允许后台使用和自启动"
}

/**
 * 魅蓝适配器
 */
class MeizuAdapter : ManufacturerAdapterBase() {
    override fun getName() = "魅蓝"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.permission.SmartBGActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.meizu.safe"
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.meizu.safe"
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「手机管家」",
        "点击「权限管理」",
        "找到「汽水音乐广告助手」",
        "开启「自启动」开关",
        "在「待机管理」中选择「不限制」"
    )
    
    override fun getBriefTip() = "开启自启动和不限制待机"
}

/**
 * 一加适配器
 */
class OnePlusAdapter : ManufacturerAdapterBase() {
    override fun getName() = "一加"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return Intent().apply {
            action = "oneplus.intent.action.POWER_MANAGER"
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent().apply {
            action = "oneplus.intent.action.PERMISSION_MANAGER"
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「设置」",
        "点击「应用」",
        "找到「汽水音乐广告助手」",
        "点击「电池」选择「不优化」",
        "开启「自启动管理」"
    )
    
    override fun getBriefTip() = "不优化电池+开启自启动"
}

/**
 * 联想适配器
 */
class LenovoAdapter : ManufacturerAdapterBase() {
    override fun getName() = "联想"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.lenovo.security",
                    "com.lenovo.security.purebackground.PureBackgroundActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.lenovo.security"
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.lenovo.security"
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「安全中心」",
        "点击「后台管理」",
        "找到「汽水音乐广告助手」",
        "设置为「允许后台运行」",
        "开启「自启动」"
    )
    
    override fun getBriefTip() = "允许后台运行和自启动"
}

/**
 * 华硕适配器
 */
class AsusAdapter : ManufacturerAdapterBase() {
    override fun getName() = "华硕"
    
    override fun getAutoStartIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.autostart.AutoStartActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.asus.mobilemanager"
        }
    }
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? {
        return Intent().apply {
            action = "com.asus.mobilemanager"
        }
    }
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「华硕管理」",
        "点击「自动启动管理」",
        "找到「汽水音乐广告助手」",
        "开启「允许自启动」",
        "在「电池管理」中选择「无限制」"
    )
    
    override fun getBriefTip() = "开启自启动和无限制电池"
}

/**
 * 默认适配器
 */
class DefaultAdapter : ManufacturerAdapterBase() {
    override fun getName() = "通用"
    
    override fun getAutoStartIntent(context: Context): Intent? = null
    
    override fun getBatteryOptimizationIntent(context: Context): Intent? = null
    
    override fun getDisplayOverOtherAppsIntent(context: Context): Intent? = null
    
    override fun getGuideSteps(): List<String> = listOf(
        "打开「设置」",
        "找到「应用管理」",
        "选择「汽水音乐广告助手」",
        "开启必要的权限"
    )
    
    override fun getBriefTip() = "开启必要的应用权限"
}
