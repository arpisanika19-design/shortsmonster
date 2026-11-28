package com.shortsmonster
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var box: LinearLayout
    private var secEt: EditText? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(c: Int, r: Int = 18) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(30), dp(14), dp(30)) }
        setContentView(ScrollView(this).apply { setBackgroundColor(0xFFFFF3E0.toInt()); addView(box) })
    }
    override fun onResume() { super.onResume(); render() }
    override fun onPause() { super.onPause(); secEt?.text?.toString()?.toIntOrNull()?.let { Store.setSecs(this, it) } }

    private fun tv(s: String, size: Float, color: Int = 0xFF333333.toInt(), bold: Boolean = false, g: Int = Gravity.START) =
        TextView(this).apply { text = s; textSize = size; setTextColor(color); gravity = g; if (bold) setTypeface(typeface, Typeface.BOLD) }

    private fun card(color: Int = Color.WHITE) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = bg(color); setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
    }
    private fun btn(label: String, color: Int, go: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); background = bg(color, 24); isAllCaps = false
        setOnClickListener { go() }; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
    }
    private fun statCard(title: String, n: Int, time: String, color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = bg(color); setPadding(dp(12), dp(12), dp(12), dp(12))
        layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), dp(12)) }
        addView(tv(title, 13f, Color.WHITE, g = Gravity.CENTER))
        addView(tv("$n", 36f, Color.WHITE, true, Gravity.CENTER))
        addView(tv("shorts", 12f, Color.WHITE, g = Gravity.CENTER))
        addView(tv("⏳ $time lost", 13f, Color.WHITE, true, Gravity.CENTER))
    }
    private fun mood(n: Int) = when {
        n == 0 -> "Zero shorts today. The monster is starving… in a good way! 🎉"
        n < 20 -> "Tiny baby monster. Let's keep it tiny! 🍼"
        n < 60 -> "It's snacking on your time. Nom nom nom 🍪"
        n < 120 -> "Chonky! Your thumb needs a nap 😴"
        else -> "The dragon owns your evening now. Go touch grass! 🌱"
    }

    private fun render() {
        secEt?.text?.toString()?.toIntOrNull()?.let { Store.setSecs(this, it) }
        box.removeAllViews()
        val secs = Store.secs(this).toLong()
        val today = Store.count(this); val week = Store.week(this)

        box.addView(card(0xFF7C4DFF.toInt()).apply {
            addView(tv(Store.emoji(today), 64f, g = Gravity.CENTER))
            addView(tv(Store.stage(today), 22f, Color.WHITE, true, Gravity.CENTER))
            addView(tv(mood(today), 14f, 0xFFEDE7F6.toInt(), g = Gravity.CENTER).apply { setPadding(0, dp(6), 0, 0) })
        })

        box.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(statCard("Today", today, Store.fmt(today * secs), 0xFFFF7043.toInt()))
            addView(statCard("Last 7 days", week, Store.fmt(week * secs), 0xFF26A69A.toInt()))
        })

        val into = today % 20
        box.addView(card().apply {
            addView(tv("🔥 Next monster scolding in ${20 - into} shorts", 14f, bold = true))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; background = bg(0xFFEEEEEE.toInt(), 8)
                layoutParams = LinearLayout.LayoutParams(-1, dp(14)).apply { topMargin = dp(8) }
                addView(View(this@MainActivity).apply { background = bg(0xFFFF7043.toInt(), 8); layoutParams = LinearLayout.LayoutParams(0, -1, into.toFloat()) })
                addView(View(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(0, -1, (20 - into).toFloat()) })
            })
        })

        val maxC = maxOf(1, (0..6).maxOf { Store.count(this, it) })
        box.addView(card().apply {
            addView(tv("📊 Your last 7 days", 14f, bold = true).apply { setPadding(0, 0, 0, dp(8)) })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
                for (i in 6 downTo 0) {
                    val c = Store.count(this@MainActivity, i)
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                        addView(tv("$c", 12f, g = Gravity.CENTER))
                        addView(View(this@MainActivity).apply {
                            background = bg(if (i == 0) 0xFFFF7043.toInt() else 0xFFB39DDB.toInt(), 6)
                            layoutParams = LinearLayout.LayoutParams(dp(22), maxOf(dp(4), dp(90) * c / maxC))
                        })
                        addView(tv(Store.label(i).take(3), 11f, 0xFF777777.toInt(), g = Gravity.CENTER))
                    })
                }
            })
        })

        box.addView(card().apply {
            addView(tv("🎮 Pick your level", 16f, bold = true))
            val rg = RadioGroup(this@MainActivity)
            listOf(
                "🖐️ Level three — manual: tap the monster after each short",
                "🤖 Level one — auto count every Shorts swipe",
                "🧠 Level two — auto count only shorts you stayed on for 3+ seconds"
            ).forEachIndexed { i, s -> rg.addView(RadioButton(this@MainActivity).apply { text = s; id = 100 + i; isChecked = Store.mode(this@MainActivity) == i }) }
            rg.setOnCheckedChangeListener { _, id -> Store.setMode(this@MainActivity, id - 100) }
            addView(rg)
            addView(tv("⏱️ Average seconds per short", 13f, 0xFF777777.toInt()).apply { setPadding(0, dp(10), 0, 0) })
            secEt = EditText(this@MainActivity).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(secs.toString()) }
            addView(secEt)
        })

        val on = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(packageName) == true
        box.addView(card(if (on) 0xFFE8F5E9.toInt() else 0xFFFFEBEE.toInt()).apply {
            addView(tv(if (on) "✅ Monster is awake! Open YouTube and it appears." else "⚠️ Wake the monster: turn on \"Shorts Monster\" in Accessibility settings.", 14f))
        })
        box.addView(btn("⚙️ Accessibility settings", 0xFF5C6BC0.toInt()) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        box.addView(btn("▶️ Open YouTube", 0xFFE53935.toInt()) { packageManager.getLaunchIntentForPackage("com.google.android.youtube")?.let { startActivity(it) } })
        box.addView(btn("🗑️ Reset all counts", 0xFF9E9E9E.toInt()) { Store.reset(this); render() })
        box.addView(tv("Drag the bubble anywhere. The monster grows and evolves as shorts pile up!", 12f, 0xFF777777.toInt(), g = Gravity.CENTER))
    }
}
