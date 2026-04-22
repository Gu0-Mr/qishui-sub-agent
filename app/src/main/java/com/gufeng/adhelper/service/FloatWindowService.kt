package com.gufeng.adhelper.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.gufeng.adhelper.R

/**
 * 悬浮窗服务
 * 作者：古封
 * 功能：显示悬浮窗，展示服务状态和广告跳过统计
 */
class FloatWindowService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "ad_helper_float_window"
        private const val CHANNEL_NAME = "悬浮窗服务"
        
        private const val FLOAT_WIDTH = 200 // dp
        private const val FLOAT_HEIGHT = 80 // dp
        
        @Volatile
        var isShowing = false
            private set
        
        private var floatWindow: View? = null
        private var windowManager: WindowManager? = null
        
        fun show(context: Context) {
            if (isShowing) return
            
            // Android 14+ 悬浮窗权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }
            
            val intent = Intent(context, FloatWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun hide(context: Context) {
            context.stopService(Intent(context, FloatWindowService::class.java))
        }
        
        fun updateSkipCount(count: Int) {
            floatWindow?.let { window ->
                window.findViewById<TextView>(R.id.tv_skip_count)?.text = count.toString()
            }
        }
    }

    private lateinit var notificationManager: WindowManager
    
    private var layoutParams: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    private var cardView: CardView? = null
    private var pulseAnimator: ValueAnimator? = null

    override fun onCreate() {
        super.onCreate()
        isShowing = true
        createFloatWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        removeFloatWindow()
    }

    /**
     * 创建通知（用于前台服务）
     */
    private fun createNotification(): android.app.Notification {
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮窗显示服务"
                setShowBadge(false)
            }
        } else {
            null
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel!!)
        }
        
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮窗服务运行中")
            .setContentText("点击打开应用")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    /**
     * 创建悬浮窗
     */
    private fun createFloatWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatWindow = inflater.inflate(R.layout.layout_float_window, null)
        
        val density = resources.displayMetrics.density
        val width = (FLOAT_WIDTH * density).toInt()
        val height = (FLOAT_HEIGHT * density).toInt()
        
        layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = width
            height = height
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - width) / 2
            y = (resources.displayMetrics.heightPixels / 4)
        }
        
        setupFloatWindowView()
        
        try {
            windowManager?.addView(floatWindow, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置悬浮窗视图
     */
    private fun setupFloatWindowView() {
        floatWindow?.let { window ->
            cardView = window.findViewById(R.id.card_float_window)
            
            // 初始化数据
            window.findViewById<TextView>(R.id.tv_skip_count)?.text = 
                AdAccessibilityService.skipCount.toString()
            
            val status = if (AdAccessibilityService.isServiceRunning) "运行中" else "未启动"
            window.findViewById<TextView>(R.id.tv_service_status)?.text = status
            
            // 点击事件 - 打开主界面
            window.findViewById<View>(R.id.card_float_window)?.setOnClickListener {
                val intent = android.content.Intent(this, MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
            }
            
            // 关闭按钮
            window.findViewById<ImageView>(R.id.iv_close)?.setOnClickListener {
                hide(this)
            }
            
            // 拖动事件
            setupDragEvent(window)
            
            // 开始脉冲动画
            startPulseAnimation()
        }
    }

    /**
     * 设置拖动事件
     */
    private fun setupDragEvent(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 吸附到边缘
                    val screenWidth = resources.displayMetrics.widthPixels
                    val targetX = if (event.rawX < screenWidth / 2) {
                        0
                    } else {
                        screenWidth - view.width
                    }
                    
                    ValueAnimator.ofInt(layoutParams?.x ?: 0, targetX).apply {
                        duration = 200
                        addUpdateListener { animator ->
                            layoutParams?.x = animator.animatedValue as Int
                            windowManager?.updateViewLayout(view, layoutParams)
                        }
                        start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 开始脉冲动画
     */
    private fun startPulseAnimation() {
        cardView?.let { card ->
            pulseAnimator = ValueAnimator.ofFloat(1f, 0.95f, 1f).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { animator ->
                    val scale = animator.animatedValue as Float
                    card.scaleX = scale
                    card.scaleY = scale
                }
                start()
            }
        }
    }

    /**
     * 移除悬浮窗
     */
    private fun removeFloatWindow() {
        pulseAnimator?.cancel()
        floatWindow?.let { window ->
            try {
                windowManager?.removeView(window)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatWindow = null
        layoutParams = null
    }
}
