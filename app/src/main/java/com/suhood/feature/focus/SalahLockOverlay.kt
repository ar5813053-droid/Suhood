package com.suhood.feature.focus

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
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
 * Full-screen lock via TYPE_ACCESSIBILITY_OVERLAY — shows on top of Instagram
 * without Activity / notification click.
 */
object SalahLockOverlay {
    @Volatile private var root: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null
    private var timerView: TextView? = null

    fun isShowing(): Boolean = root != null

    fun show(service: AccessibilityService) {
        handler.post {
            val store = SalahLockStore.get(service)
            val snap = store.reconcileNow()
            if (!SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
                hide(service)
                return@post
            }
            if (root != null) {
                updateTimer(store)
                return@post
            }
            val ctx = service
            val density = ctx.resources.displayMetrics.density
            fun dp(v: Int) = (v * density).toInt()

            val container = FrameLayout(ctx).apply {
                setBackgroundColor(Color.BLACK)
                isClickable = true
                isFocusable = true
            }

            // Background image
            val bg = ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = 0.55f
                runCatching { setImageResource(R.drawable.bg_mosque_dusk) }
            }
            container.addView(bg, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(28), dp(48), dp(28), dp(24))
            }

            val logo = ImageView(ctx).apply {
                runCatching { setImageResource(R.drawable.ic_suhood_logo) }
                adjustViewBounds = true
            }
            col.addView(logo, LinearLayout.LayoutParams(dp(64), dp(64)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })

            col.addView(TextView(ctx).apply {
                text = "Suhood"
                setTextColor(Color.parseColor("#E8E8EC"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, lp(dp(8)))

            col.addView(TextView(ctx).apply {
                text = "It's Salah Time"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, lp(dp(28)))

            col.addView(TextView(ctx).apply {
                text = "Pause distractions and\nreturn to Allah."
                setTextColor(Color.parseColor("#B8BEC6"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = Gravity.CENTER
            }, lp(dp(10)))

            // Glass card
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(20), dp(22), dp(20), dp(22))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#331A2228"))
                    cornerRadius = dp(22).toFloat()
                    setStroke(dp(1), Color.parseColor("#44FFFFFF"))
                }
            }
            card.addView(TextView(ctx).apply {
                text = "APP LOCKED FOR"
                setTextColor(Color.parseColor("#9AA3AE"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                letterSpacing = 0.12f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            })
            val timer = TextView(ctx).apply {
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
            card.addView(TextView(ctx).apply {
                text = "You can open the app again after\n10 minutes."
                setTextColor(Color.parseColor("#9AA3AE"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
            }, lp(dp(8)))

            col.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(36) })

            val spacer = View(ctx)
            col.addView(spacer, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))

            val btn = TextView(ctx).apply {
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
                    hide(service)
                    runCatching { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) }
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
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
            runCatching {
                service.getSystemService(WindowManager::class.java).addView(container, params)
                root = container
                startTicker(service)
            }
        }
    }

    fun hide(service: AccessibilityService) {
        handler.post {
            ticker?.let { handler.removeCallbacks(it) }
            ticker = null
            val v = root ?: return@post
            runCatching {
                service.getSystemService(WindowManager::class.java).removeView(v)
            }
            root = null
            timerView = null
        }
    }

    private fun startTicker(service: AccessibilityService) {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = object : Runnable {
            override fun run() {
                val store = SalahLockStore.get(service)
                val snap = store.reconcileNow()
                if (!SalahLockLogic.isActive(snap, System.currentTimeMillis())) {
                    hide(service)
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
