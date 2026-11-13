package com.suhood.feature.focus

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.suhood.app.R

/**
 * Full-screen Salah lock overlay.
 *
 * Presentation paths (same UI, different window type):
 * 1. TYPE_ACCESSIBILITY_OVERLAY — preferred when [AccessibilityService] is connected
 *    (same reliable stack as FocusChallengeOverlay / 3-scroll gate).
 * 2. TYPE_APPLICATION_OVERLAY — Display-over-apps fallback when Accessibility is
 *    unavailable but SYSTEM_ALERT_WINDOW is granted.
 *
 * Detection stays outside this class (Accessibility events / Usage Access).
 * Overlay is reused when already showing (no duplicate windows).
 */
object SalahLockOverlay {
    @Volatile private var root: View? = null
    @Volatile private var attachedWm: WindowManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null
    private var timerView: TextView? = null

    fun isShowing(): Boolean = root != null

    /** Preferred path — Accessibility overlay (mirrors FocusChallengeOverlay). */
    fun show(service: AccessibilityService) {
        handler.post {
            val store = SalahLockStore.get(service)
            val snap = store.reconcileNow()
            if (!SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
                hideInternal()
                return@post
            }
            if (root != null) {
                updateTimer(store)
                return@post
            }
            attach(
                context = service,
                windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                onHome = {
                    runCatching {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    }
                }
            )
        }
    }

    /**
     * Display-over-apps fallback — works without AccessibilityService.
     * Caller must ensure [Settings.canDrawOverlays] is true.
     */
    fun showFromContext(context: Context) {
        handler.post {
            val app = context.applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(app)) {
                return@post
            }
            val store = SalahLockStore.get(app)
            val snap = store.reconcileNow()
            if (!SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
                hideInternal()
                return@post
            }
            if (root != null) {
                updateTimer(store)
                return@post
            }
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            attach(
                context = app,
                windowType = type,
                onHome = {
                    runCatching {
                        app.startActivity(
                            Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_HOME)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        }
    }

    fun hide(service: AccessibilityService) {
        handler.post { hideInternal() }
    }

    fun hideFromContext(context: Context) {
        handler.post { hideInternal() }
    }

    private fun hideInternal() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
        val v = root
        val wm = attachedWm
        if (v != null && wm != null) {
            runCatching { wm.removeView(v) }
        }
        root = null
        attachedWm = null
        timerView = null
    }

    private fun attach(context: Context, windowType: Int, onHome: () -> Unit) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val store = SalahLockStore.get(context)
        val snap = store.reconcileNow()

        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            isFocusable = true
        }

        val bg = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.55f
            runCatching { setImageResource(R.drawable.bg_mosque_dusk) }
        }
        container.addView(
            bg,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(48), dp(28), dp(24))
        }

        val logo = ImageView(context).apply {
            runCatching { setImageResource(R.drawable.ic_suhood_logo) }
            adjustViewBounds = true
        }
        col.addView(logo, LinearLayout.LayoutParams(dp(64), dp(64)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        col.addView(TextView(context).apply {
            text = "Suhood"
            setTextColor(Color.parseColor("#E8E8EC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, lp(dp(8)))

        col.addView(TextView(context).apply {
            text = "It's Salah Time"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, lp(dp(28)))

        col.addView(TextView(context).apply {
            text = "Pause distractions and\nreturn to Allah."
            setTextColor(Color.parseColor("#B8BEC6"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
        }, lp(dp(10)))

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#331A2228"))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.parseColor("#44FFFFFF"))
            }
        }
        card.addView(TextView(context).apply {
            text = "APP LOCKED FOR"
            setTextColor(Color.parseColor("#9AA3AE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        })
        val timer = TextView(context).apply {
            text = SalahLockLogic.formatMmSs(
                SalahLockLogic.remainingMs(snap, System.currentTimeMillis())
            )
            setTextColor(Color.parseColor("#9FE8C8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        timerView = timer
        card.addView(timer, lp(dp(6)))
        card.addView(TextView(context).apply {
            text = "You can open the app again after\nthe countdown ends."
            setTextColor(Color.parseColor("#9AA3AE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
        }, lp(dp(8)))

        col.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(36) })

        col.addView(View(context), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val btn = TextView(context).apply {
            text = "  I Understand — Exit App"
            setTextColor(Color.parseColor("#9FE8C8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A2E2A"))
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), Color.parseColor("#739FE8C8"))
            }
            setOnClickListener {
                // Does not unlock — only leaves the blocked app
                hideInternal()
                onHome()
            }
        }
        col.addView(btn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })

        container.addView(col, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        val wm = context.getSystemService(WindowManager::class.java)
        runCatching {
            wm.addView(container, params)
            root = container
            attachedWm = wm
            startTicker(context)
        }
    }

    private fun startTicker(context: Context) {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = object : Runnable {
            override fun run() {
                val store = SalahLockStore.get(context)
                val snap = store.reconcileNow()
                if (!SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
                    hideInternal()
                    return
                }
                updateTimer(store)
                handler.postDelayed(this, 1000L)
            }
        }
        handler.post(ticker!!)
    }

    private fun updateTimer(store: SalahLockStore) {
        val ms = SalahLockLogic.remainingMs(store.read(), System.currentTimeMillis())
        timerView?.text = SalahLockLogic.formatMmSs(ms)
    }

    private fun lp(top: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = top }
}
