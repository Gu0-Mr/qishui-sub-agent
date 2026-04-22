package com.gufeng.adhelper.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.gufeng.adhelper.R
import com.gufeng.adhelper.databinding.ActivityMainBinding
import com.gufeng.adhelper.keepalive.BatteryOptimizationHelper
import com.gufeng.adhelper.keepalive.ManufacturerAdapter
import com.gufeng.adhelper.service.AdAccessibilityService
import com.gufeng.adhelper.service.FloatWindowService
import com.gufeng.adhelper.service.KeepAliveService
import com.gufeng.adhelper.utils.PreferencesManager
import com.gufeng.adhelper.viewmodel.ServiceStatusViewModel

/**
 * 主界面
 * 作者：古封
 * 功能：显示服务状态、管理权限、统计数据
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ServiceStatusViewModel
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var batteryHelper: BatteryOptimizationHelper
    private lateinit var manufacturerAdapter: ManufacturerAdapter
    
    private val accessibilityManager by lazy {
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateServiceStatus()
            updateStatistics()
            handler.postDelayed(this, 1000)
        }
    }
    
    // 悬浮窗权限请求
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkOverlayPermission() }
    
    // 电池优化权限请求
    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkBatteryOptimization() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        batteryHelper = BatteryOptimizationHelper(this)
        manufacturerAdapter = ManufacturerAdapter(this)
        
        viewModel = ViewModelProvider(this)[ServiceStatusViewModel::class.java]
        
        initViews()
        setupListeners()
        checkAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        // 每次进入页面都刷新权限状态
        checkAccessibilityService()
        checkOverlayPermission()
        checkBatteryOptimization()
        updateServiceStatus()
        updateStatistics()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        // 设置厂商信息
        binding.tvManufacturer.text = manufacturerAdapter.getAdapter().getName()
        
        // 设置目标应用
        binding.tvTargetApp.text = "汽水音乐"
        
        // 显示引导提示
        updateGuideText()
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 无障碍服务开关
        binding.switchAccessibilityService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                openAccessibilitySettings()
            } else {
                preferencesManager.isServiceEnabled = false
                updateServiceStatus()
            }
        }
        
        // 前台服务开关
        binding.switchKeepAlive.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.isKeepAliveEnabled = isChecked
            if (isChecked) {
                KeepAliveService.start(this)
            } else {
                KeepAliveService.stop(this)
            }
            updateServiceStatus()
        }
        
        // 悬浮窗开关
        binding.switchFloatWindow.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Settings.canDrawOverlays(this)) {
                    FloatWindowService.show(this)
                } else {
                    requestOverlayPermission()
                    binding.switchFloatWindow.isChecked = false
                }
            } else {
                FloatWindowService.hide(this)
            }
            preferencesManager.isFloatWindowEnabled = isChecked
        }
        
        // 电池优化按钮
        binding.btnBatteryOptimization.setOnClickListener {
            openBatteryOptimization()
        }
        
        // 厂商自启动设置按钮
        binding.btnManufacturerSettings.setOnClickListener {
            openManufacturerSettings()
        }
        
        // 重置统计按钮
        binding.btnResetStats.setOnClickListener {
            showResetConfirmDialog()
        }
        
        // 版本信息
        binding.tvVersion.text = "v${getAppVersion()}"
    }

    /**
     * 检查所有权限
     */
    private fun checkAllPermissions() {
        checkAccessibilityService()
        checkOverlayPermission()
        checkBatteryOptimization()
    }

    /**
     * 检查无障碍服务状态
     */
    private fun checkAccessibilityService() {
        val serviceInfo = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )
        
        val isEnabled = serviceInfo.any { 
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name == AdAccessibilityService::class.java.name
        }
        
        binding.switchAccessibilityService.isChecked = isEnabled
        preferencesManager.isServiceEnabled = isEnabled
        updateServiceStatus()
    }

    /**
     * 检查悬浮窗权限
     */
    private fun checkOverlayPermission() {
        val hasPermission = Settings.canDrawOverlays(this)
        binding.switchFloatWindow.isChecked = hasPermission && FloatWindowService.isShowing
        binding.tvOverlayStatus.text = if (hasPermission) "已授权" else "未授权"
    }

    /**
     * 检查电池优化状态
     */
    private fun checkBatteryOptimization() {
        val isIgnoring = batteryHelper.isIgnoringBatteryOptimizations()
        binding.tvBatteryStatus.text = if (isIgnoring) "已关闭" else "未关闭"
        binding.btnBatteryOptimization.text = if (isIgnoring) "已设置" else "去设置"
    }

    /**
     * 更新服务状态显示
     */
    private fun updateServiceStatus() {
        val isAccessibilityEnabled = binding.switchAccessibilityService.isChecked
        val isKeepAliveEnabled = preferencesManager.isKeepAliveEnabled
        val isOverlayEnabled = binding.switchFloatWindow.isChecked
        
        // 服务状态
        val serviceStatus = when {
            isAccessibilityEnabled && AdAccessibilityService.isServiceRunning -> "运行中"
            isAccessibilityEnabled -> "已开启"
            else -> "未开启"
        }
        binding.tvServiceStatus.text = serviceStatus
        
        // 服务状态颜色
        val statusColor = when {
            isAccessibilityEnabled && AdAccessibilityService.isServiceRunning -> 
                ContextCompat.getColor(this, R.color.status_running)
            isAccessibilityEnabled -> 
                ContextCompat.getColor(this, R.color.status_enabled)
            else -> 
                ContextCompat.getColor(this, R.color.status_disabled)
        }
        binding.tvServiceStatus.setTextColor(statusColor)
        binding.ivStatusIndicator.setColorFilter(statusColor)
        
        // 综合状态
        val allEnabled = isAccessibilityEnabled && isKeepAliveEnabled && isOverlayEnabled
        binding.tvOverallStatus.text = if (allEnabled) "全部就绪" else "部分未开启"
        
        // 更新ViewModel
        viewModel.updateStatus(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isKeepAliveEnabled = isKeepAliveEnabled,
            isOverlayEnabled = isOverlayEnabled,
            isServiceRunning = AdAccessibilityService.isServiceRunning
        )
    }

    /**
     * 更新统计数据
     */
    private fun updateStatistics() {
        binding.tvSkipCount.text = AdAccessibilityService.skipCount.toString()
        binding.tvTodaySkipCount.text = preferencesManager.todaySkipCount.toString()
        binding.tvTotalSkipCount.text = preferencesManager.totalSkipCount.toString()
    }

    /**
     * 更新引导文本
     */
    private fun updateGuideText() {
        val guideText = buildString {
            if (!binding.switchAccessibilityService.isChecked) {
                append("• 请先开启无障碍服务\n")
            }
            if (!preferencesManager.isKeepAliveEnabled) {
                append("• 建议开启后台保活\n")
            }
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                append("• 请授权悬浮窗权限\n")
            }
            if (!batteryHelper.isIgnoringBatteryOptimizations()) {
                append("• 建议关闭电池优化\n")
            }
            if (isEmpty()) {
                append("✓ 所有权限已就绪，广告助手运行中")
            }
        }
        binding.tvGuide.text = guideText
    }

    /**
     * 打开无障碍服务设置
     */
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "请找到「汽水音乐广告助手」并开启", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开无障碍设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 请求悬浮窗权限
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    /**
     * 打开电池优化设置
     */
    private fun openBatteryOptimization() {
        val intent = batteryHelper.requestIgnoreBatteryOptimizations()
        if (intent != null) {
            try {
                batteryOptimizationLauncher.launch(intent)
            } catch (e: Exception) {
                // 备用方案：打开电池优化设置页面
                batteryOptimizationLauncher.launch(
                    batteryHelper.getBatteryOptimizationSettingsIntent()
                )
            }
        } else {
            Toast.makeText(this, "已经是最佳电池设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开厂商自启动设置
     */
    private fun openManufacturerSettings() {
        val intent = manufacturerAdapter.getAdapter().getAutoStartIntent(this)
        if (intent != null) {
            try {
                startActivity(intent)
            } catch (e: Exception) {
                showManufacturerGuideDialog()
            }
        } else {
            showManufacturerGuideDialog()
        }
    }

    /**
     * 显示厂商引导对话框
     */
    private fun showManufacturerGuideDialog() {
        val adapter = manufacturerAdapter.getAdapter()
        val steps = adapter.getGuideSteps()
        
        AlertDialog.Builder(this)
            .setTitle("${adapter.getName()}设置指引")
            .setMessage(steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n"))
            .setPositiveButton("知道了") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("打开设置") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    })
                } catch (e: Exception) {
                    Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    /**
     * 显示重置确认对话框
     */
    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("重置统计")
            .setMessage("确定要重置所有统计数据吗？")
            .setPositiveButton("确定") { _, _ ->
                AdAccessibilityService.resetSkipCount()
                preferencesManager.resetTodaySkipCount()
                updateStatistics()
                Toast.makeText(this, "统计已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 获取应用版本
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}
