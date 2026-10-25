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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

/**
 * Prayer-Pause style gate over a distracting app.
 * TYPE_ACCESSIBILITY_OVERLAY so it appears without relying on Activity start.
 * User must complete a mini-challenge (or has used 3/day) before continuing.
 */
object FocusChallengeOverlay {
    @Volatile private var root: View? = null
    private val handler = Handler(Looper.getMainLooper())

    fun isShowing(): Boolean = root != null

    fun hide(service: AccessibilityService) {
        handler.post {
            val v = root ?: return@post
            runCatching {
                service.getSystemService(WindowManager::class.java).removeView(v)
            }
            root = null
        }
    }

    fun show(
        service: AccessibilityService,
        packageName: String,
        remainingChallenges: Int
    ) {
        handler.post {
            if (root != null) return@post
            val ctx = service
            val density = ctx.resources.displayMetrics.density
            fun dp(v: Int) = (v * density).toInt()

            val container = FrameLayout(ctx).apply {
                setBackgroundColor(Color.parseColor("#F008090B"))
                isClickable = true
                isFocusable = true
            }

            val scroll = ScrollView(ctx)
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(24), dp(48), dp(24), dp(32))
            }

            col.addView(TextView(ctx).apply {
                text = "Suhood"
                setTextColor(Color.parseColor("#C7CBD1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                letterSpacing = 0.15f
            })
            col.addView(TextView(ctx).apply {
                text = "Pause before you scroll"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, lp(dp(12)))
            col.addView(TextView(ctx).apply {
                text = if (remainingChallenges > 0)
                    "Complete a short Islamic pause · $remainingChallenges of 3 left today"
                else
                    "You've used all 3 mindful pauses today"
                setTextColor(Color.parseColor("#9AA3AE"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
            }, lp(dp(8)))

            val kind = Random.nextInt(3)
            when {
                remainingChallenges <= 0 -> {
                    col.addView(card(ctx, dp) {
                        it.addView(tv(ctx, "Come back tomorrow for more pauses, or return to home.", 15, "#B8BEC6"))
                        it.addView(btn(ctx, dp, "Return home") {
                            hide(service)
                            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                        }, lp(dp(16)))
                    }, lp(dp(24)))
                }
                kind == 0 -> addTasbeeh(ctx, col, dp, service)
                kind == 1 -> addDhikrQuiz(ctx, col, dp, service)
                else -> addAyah(ctx, col, dp, service)
            }

            col.addView(TextView(ctx).apply {
                text = "After you finish, you get a few minutes with the app."
                setTextColor(Color.parseColor("#6B7280"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
            }, lp(dp(20)))

            scroll.addView(col)
            container.addView(scroll, FrameLayout.LayoutParams(
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
            }
        }
    }

    private fun completeAndDismiss(service: AccessibilityService) {
        FocusChallengeStore.get(service).recordChallengeCompleted()
        hide(service)
        // Allow the blocked app — short pass is active; do not HOME again
    }

    private fun addTasbeeh(
        ctx: android.content.Context,
        col: LinearLayout,
        dp: (Int) -> Int,
        service: AccessibilityService
    ) {
        val phrases = listOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar")
        val target = phrases.random()
        val need = 11
        var count = 0
        val countTv = tv(ctx, "0 / $need", 36, "#9FE8C8").apply {
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        col.addView(card(ctx, dp) {
            it.addView(tv(ctx, "Tasbeeh", 13, "#C7CBD1"))
            it.addView(tv(ctx, "Tap $need times", 14, "#9AA3AE"), lp(dp(4)))
            it.addView(tv(ctx, target, 28, "#FFFFFF").apply {
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, lp(dp(12)))
            it.addView(countTv, lp(dp(8)))
            it.addView(btn(ctx, dp, "Tap") {
                count++
                countTv.text = "$count / $need"
                if (count >= need) completeAndDismiss(service)
            }, lp(dp(16)))
        }, lp(dp(24)))
    }

    private fun addDhikrQuiz(
        ctx: android.content.Context,
        col: LinearLayout,
        dp: (Int) -> Int,
        service: AccessibilityService
    ) {
        val pairs = listOf(
            "Glory be to Allah" to "SubhanAllah",
            "Praise be to Allah" to "Alhamdulillah",
            "Allah is the Greatest" to "Allahu Akbar",
            "There is no god but Allah" to "La ilaha illallah"
        )
        val (meaning, answer) = pairs.random()
        val options = pairs.map { it.second }.shuffled()
        col.addView(card(ctx, dp) {
            it.addView(tv(ctx, "Dhikr quiz", 13, "#C7CBD1"))
            it.addView(tv(ctx, "What is the Arabic for:", 14, "#9AA3AE"), lp(dp(8)))
            it.addView(tv(ctx, meaning, 18, "#FFFFFF").apply {
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, lp(dp(8)))
            options.forEach { opt ->
                it.addView(btn(ctx, dp, opt) {
                    if (opt == answer) completeAndDismiss(service)
                }, lp(dp(8)))
            }
        }, lp(dp(24)))
    }

    private fun addAyah(
        ctx: android.content.Context,
        col: LinearLayout,
        dp: (Int) -> Int,
        service: AccessibilityService
    ) {
        data class A(val ar: String, val en: String)
        val ayah = listOf(
            A("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Most Gracious, the Most Merciful."),
            A("إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "You alone we worship, and You alone we ask for help."),
            A("قُلْ هُوَ اللَّهُ أَحَدٌ", "Say: He is Allah, the One.")
        ).random()
        col.addView(card(ctx, dp) {
            it.addView(tv(ctx, "Quran · reflect", 13, "#C7CBD1"))
            it.addView(tv(ctx, ayah.ar, 24, "#FFFFFF").apply {
                gravity = Gravity.CENTER
                setLineSpacing(8f, 1f)
            }, lp(dp(12)))
            it.addView(tv(ctx, ayah.en, 15, "#B8BEC6").apply {
                gravity = Gravity.CENTER
            }, lp(dp(8)))
            it.addView(btn(ctx, dp, "I reflected — Continue") {
                completeAndDismiss(service)
            }, lp(dp(16)))
        }, lp(dp(24)))
    }

    private fun card(
        ctx: android.content.Context,
        dp: (Int) -> Int,
        build: (LinearLayout) -> Unit
    ): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#331A2228"))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.parseColor("#44FFFFFF"))
            }
            build(this)
        }
    }

    private fun btn(
        ctx: android.content.Context,
        dp: (Int) -> Int,
        label: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(ctx).apply {
            text = label
            setTextColor(Color.parseColor("#9FE8C8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A2E2A"))
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), Color.parseColor("#669FE8C8"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun tv(ctx: android.content.Context, text: String, sp: Int, color: String) =
        TextView(ctx).apply {
            this.text = text
            setTextColor(Color.parseColor(color))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp.toFloat())
            gravity = Gravity.CENTER
        }

    private fun lp(top: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = top }
}
