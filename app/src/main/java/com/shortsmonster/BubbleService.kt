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
        monster.text = Store.emoji(n)
        monster.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22f + min(n, 150) * 0.35f) // starts tiny, grows with each short
        if (shown) wm.updateViewLayout(root, lp)
    }

    private fun bump() {
        val n = Store.inc(this); refresh()
        monster.animate().scaleX(1.35f).scaleY(1.35f).setDuration(120).withEndAction {
            monster.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
        if (n % 20 == 0) Toast.makeText(this, lines[(n / 20 - 1) % lines.size], Toast.LENGTH_LONG).show()
    }

    private fun show() { if (!shown) { refresh(); wm.addView(root, lp); shown = true } else refresh() }
    private fun hide() { if (shown) { wm.removeView(root); shown = false } }

    private var lastSig = ""

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg == YT) show()
            else if (pkg != packageName && !pkg.contains("systemui") && !pkg.contains("inputmethod") && !pkg.contains("honeyboard")) { hide(); lastSig = "" }
        }
        val mode = Store.mode(this)
        if (mode == 0 || pkg != YT) return
        if (e.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            e.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        val now = System.currentTimeMillis()
        if (now - lastCheck < 250) return                 // do not scan the screen too often
        lastCheck = now
        val sig = shortSignature() ?: run { lastSig = ""; return }   // null = not on Shorts
        val swiped = e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        val changed = lastSig.isNotEmpty() && sig != lastSig
        if (lastSig.isEmpty()) { lastSig = sig; lastShortStart = now; return }  // just entered Shorts
        if (!(changed || swiped)) return
        if (now - lastSwipe < 1200) { if (changed) lastSig = sig; return }      // same swipe seen by both signals
        lastSwipe = now
        lastSig = sig
        if (mode == 1) bump()                                                      // Level 1: every new short
        else { if (now - lastShortStart >= 3000) bump(); }                         // Level 2: stayed 3s+
        lastShortStart = now
    }

    private var lastCheck = 0L

    private var onShorts = false
    private var misses = 0

    // Fingerprint of everything readable on the visible screen (digits ignored, so like counts do not matter).
    // Returns null when the Shorts player is not showing.
    private fun shortSignature(): String? {
        val r = rootInActiveWindow ?: return null
        val set = java.util.TreeSet<String>(); val found = booleanArrayOf(false)
        walk(r, 0, set, found)
        if (found[0]) { onShorts = true; misses = 0 } else if (onShorts && ++misses >= 3) { onShorts = false; misses = 0 }
        return if (onShorts) set.joinToString("|") else null
    }
    private fun walk(n: AccessibilityNodeInfo, depth: Int, out: MutableSet<String>, found: BooleanArray) {
        if (!n.isVisibleToUser) return
        val id = n.viewIdResourceName ?: ""
        if (id.contains("reel", true) || id.contains("shorts", true)) found[0] = true
        val t = (n.text?.toString() ?: n.contentDescription?.toString() ?: "")
        if (t.length > 2 && t.none { it.isDigit() } && out.size < 40) out.add(t.take(50))
        if (depth > 14) return
        for (i in 0 until n.childCount) { val c = n.getChild(i) ?: continue; walk(c, depth + 1, out, found) }
    }

    override fun onInterrupt() {}
    override fun onDestroy() { hide(); super.onDestroy() }
}
