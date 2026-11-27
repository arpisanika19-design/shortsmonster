package com.shortsmonster
import android.accessibilityservice.AccessibilityService
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.min

class BubbleService : AccessibilityService() {
    private val YT = "com.google.android.youtube"
    private lateinit var wm: WindowManager
    private lateinit var root: FrameLayout
    private lateinit var monster: TextView
    private lateinit var badge: TextView
    private lateinit var lp: WindowManager.LayoutParams
    private var shown = false
    private var lastSwipe = 0L
    private var lastShortStart = 0L

    private val lines = listOf(
        "20 Shorts already? Your thumb has a gym membership now.",
        "40! The monster is eating your free time. Feed it something else.",
        "60 Shorts. Somewhere a book is feeling very lonely.",
        "80! Your future self just sighed loudly.",
        "100 Shorts! Legends say there is a sun outside.",
        "120... even the algorithm is worried about you.",
        "140! Drink some water. Blink. Look at a wall.",
        "160 Shorts. Your sofa is filing for a joint account.",
        "180! The monster is now bigger than your to-do list.",
        "200! Congratulations, you have unlocked: Go outside."
    )

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val d = resources.displayMetrics.density
        monster = TextView(this).apply { text = "👾"; gravity = Gravity.CENTER }
        badge = TextView(this).apply {
            setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize = 11f
            setPadding((6 * d).toInt(), (1 * d).toInt(), (6 * d).toInt(), (1 * d).toInt())
            background = GradientDrawable().apply { cornerRadius = 40f; setColor(0xFFE53935.toInt()) }
        }
        root = FrameLayout(this).apply {
            addView(monster, FrameLayout.LayoutParams(-2, -2).apply { setMargins(0, (10 * d).toInt(), (12 * d).toInt(), 0) })
            addView(badge, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END))
        }
        lp = WindowManager.LayoutParams(
            -2, -2, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Store.x(this@BubbleService, resources.displayMetrics.widthPixels - (110 * d).toInt()); y = Store.y(this@BubbleService)
        }
        root.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - sx; val dy = e.rawY - sy
                        if (abs(dx) > 12 || abs(dy) > 12) moved = true
                        if (moved) { lp.x = ox + dx.toInt(); lp.y = oy + dy.toInt(); wm.updateViewLayout(root, lp) }
                    }
                    MotionEvent.ACTION_UP -> if (moved) Store.setPos(this@BubbleService, lp.x, lp.y)
                    else if (Store.mode(this@BubbleService) == 0) bump()
                }
                return true
            }
        })
        refresh()
    }

    private fun refresh() {
        val n = Store.count(this)
        badge.text = n.toString()
        monster.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22f + min(n, 150) * 0.35f) // starts tiny, grows with each short
        if (shown) wm.updateViewLayout(root, lp)
    }

    private fun bump() {
        val n = Store.inc(this); refresh()
        if (n % 20 == 0) Toast.makeText(this, lines[(n / 20 - 1) % lines.size], Toast.LENGTH_LONG).show()
    }

    private fun show() { if (!shown) { refresh(); wm.addView(root, lp); shown = true } else refresh() }
    private fun hide() { if (shown) { wm.removeView(root); shown = false } }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg == YT) show()
            else if (pkg != packageName && !pkg.contains("systemui") && !pkg.contains("inputmethod") && !pkg.contains("honeyboard")) hide()
        }
        val mode = Store.mode(this)
        if (mode > 0 && pkg == YT && e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && onShortsScreen()) {
            val now = System.currentTimeMillis()
            if (now - lastSwipe < 700) return            // ignore duplicate scroll events from one swipe
            lastSwipe = now
            if (mode == 1) bump()                         // Level 1: every swipe counts
            else { if (lastShortStart == 0L || now - lastShortStart >= 3000) bump(); lastShortStart = now } // Level 2: only shorts you stayed on 3s+
        }
    }

    private fun onShortsScreen(): Boolean = rootInActiveWindow?.let { find(it, 0) } ?: false
    private fun find(n: AccessibilityNodeInfo, depth: Int): Boolean {
        if (n.viewIdResourceName?.contains("reel", true) == true) return true
        if (depth > 9) return false
        for (i in 0 until n.childCount) { val c = n.getChild(i) ?: continue; if (find(c, depth + 1)) return true }
        return false
    }

    override fun onInterrupt() {}
    override fun onDestroy() { hide(); super.onDestroy() }
}
