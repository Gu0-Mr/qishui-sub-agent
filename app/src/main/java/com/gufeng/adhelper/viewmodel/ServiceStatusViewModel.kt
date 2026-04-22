package com.gufeng.adhelper.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * 服务状态ViewModel
 * 作者：古封
 * 功能：管理服务状态的UI状态
 */
class ServiceStatusViewModel : ViewModel() {

    /**
     * 服务状态数据
     */
    data class ServiceStatus(
        val isAccessibilityEnabled: Boolean = false,
        val isKeepAliveEnabled: Boolean = false,
        val isOverlayEnabled: Boolean = false,
        val isServiceRunning: Boolean = false,
        val skipCount: Int = 0,
        val todaySkipCount: Int = 0,
        val totalSkipCount: Int = 0
    )
    
    /**
     * 权限状态
     */
    data class PermissionStatus(
        val hasAccessibilityPermission: Boolean = false,
        val hasOverlayPermission: Boolean = false,
        val hasBatteryOptimizationDisabled: Boolean = false,
        val hasAutoStartPermission: Boolean = false
    )

    private val _serviceStatus = MutableLiveData(ServiceStatus())
    val serviceStatus: LiveData<ServiceStatus> = _serviceStatus
    
    private val _permissionStatus = MutableLiveData(PermissionStatus())
    val permissionStatus: LiveData<PermissionStatus> = _permissionStatus
    
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * 更新服务状态
     */
    fun updateStatus(
        isAccessibilityEnabled: Boolean,
        isKeepAliveEnabled: Boolean,
        isOverlayEnabled: Boolean,
        isServiceRunning: Boolean,
        skipCount: Int = _serviceStatus.value?.skipCount ?: 0,
        todaySkipCount: Int = _serviceStatus.value?.todaySkipCount ?: 0,
        totalSkipCount: Int = _serviceStatus.value?.totalSkipCount ?: 0
    ) {
        _serviceStatus.value = ServiceStatus(
            isAccessibilityEnabled = isAccessibilityEnabled,
            isKeepAliveEnabled = isKeepAliveEnabled,
            isOverlayEnabled = isOverlayEnabled,
            isServiceRunning = isServiceRunning,
            skipCount = skipCount,
            todaySkipCount = todaySkipCount,
            totalSkipCount = totalSkipCount
        )
    }

    /**
     * 更新权限状态
     */
    fun updatePermissionStatus(
        hasAccessibility: Boolean,
        hasOverlay: Boolean,
        hasBatteryOptimization: Boolean,
        hasAutoStart: Boolean
    ) {
        _permissionStatus.value = PermissionStatus(
            hasAccessibilityPermission = hasAccessibility,
            hasOverlayPermission = hasOverlay,
            hasBatteryOptimizationDisabled = hasBatteryOptimization,
            hasAutoStartPermission = hasAutoStart
        )
    }

    /**
     * 显示提示消息
     */
    fun showToast(message: String) {
        _toastMessage.value = message
    }

    /**
     * 清除提示消息
     */
    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * 设置加载状态
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * 检查是否所有权限都已授权
     */
    fun isAllPermissionsGranted(): Boolean {
        val status = _permissionStatus.value ?: return false
        val serviceStatus = _serviceStatus.value ?: return false
        
        return serviceStatus.isAccessibilityEnabled &&
               status.hasOverlayPermission &&
               status.hasBatteryOptimizationDisabled
    }

    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(): List<String> {
        val status = _permissionStatus.value ?: return emptyList()
        val serviceStatus = _serviceStatus.value ?: return emptyList()
        val missing = mutableListOf<String>()
        
        if (!serviceStatus.isAccessibilityEnabled) {
            missing.add("无障碍服务")
        }
        if (!status.hasOverlayPermission) {
            missing.add("悬浮窗权限")
        }
        if (!status.hasBatteryOptimizationDisabled) {
            missing.add("电池优化")
        }
        
        return missing
    }
}
