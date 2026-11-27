package com.shortsmonster
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var box: LinearLayout
    private var secEt: EditText? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(32), dp(20), dp(32)) }
        setContentView(ScrollView(this).apply { addView(box) })
    }
    override fun onResume() { super.onResume(); render() }
    override fun onPause() { super.onPause(); secEt?.text?.toString()?.toIntOrNull()?.let { Store.setSecs(this, it) } }

    private fun t(s: String, size: Float = 16f, bold: Boolean = false, color: Int = Color.DKGRAY) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun render() {
        secEt?.text?.toString()?.toIntOrNull()?.let { Store.setSecs(this, it) }
        box.removeAllViews()
        val secs = Store.secs(this).toLong()
        val today = Store.count(this); val week = Store.week(this)
        box.addView(t("👾 Shorts Monster", 28f, true, Color.BLACK))
        box.addView(t("Today", 14f)); box.addView(t("$today Shorts", 34f, true, 0xFFE53935.toInt()))
        box.addView(t("Time lost: ${Store.fmt(today * secs)}", 18f))
        box.addView(t("Last 7 days", 14f).apply { setPadding(0, dp(18), 0, 0) })
        box.addView(t("$week Shorts", 26f, true, 0xFFE53935.toInt()))
        box.addView(t("Time lost: ${Store.fmt(week * secs)}", 18f))
        for (i in 0..6) box.addView(t("${Store.label(i)}  —  ${Store.count(this, i)} Shorts  (${Store.fmt(Store.count(this, i) * secs)})", 14f))

        box.addView(t("Counting mode", 18f, true, Color.BLACK).apply { setPadding(0, dp(24), 0, 0) })
        val rg = RadioGroup(this)
        val opts = listOf(
            "Level three — manual: tap the bubble after each short to add one",
            "Level one — auto count every Shorts swipe",
            "Level two — auto count only shorts you stayed on for 3+ seconds"
        )
        opts.forEachIndexed { i, s -> rg.addView(RadioButton(this).apply { text = s; id = 100 + i; isChecked = Store.mode(this@MainActivity) == i }) }
        rg.setOnCheckedChangeListener { _, id -> Store.setMode(this, id - 100) }
        box.addView(rg)

        box.addView(t("Average seconds per short (for time lost)", 14f).apply { setPadding(0, dp(16), 0, 0) })
        secEt = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(secs.toString()) }
        box.addView(secEt)

        val on = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(packageName) == true
        box.addView(t(if (on) "✅ Monster is active — open YouTube and it appears." else "⚠️ Turn on \"Shorts Monster\" in Accessibility settings so the bubble can appear over YouTube.", 14f).apply { setPadding(0, dp(20), 0, dp(6)) })
        box.addView(Button(this).apply { text = "Open accessibility settings"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        box.addView(Button(this).apply { text = "Open YouTube"; setOnClickListener { packageManager.getLaunchIntentForPackage("com.google.android.youtube")?.let { startActivity(it) } } })
        box.addView(Button(this).apply { text = "Reset all counts"; setOnClickListener { Store.reset(this@MainActivity); render() } })
        box.addView(t("Drag the bubble anywhere. A funny warning pops up every 20 shorts.", 12f).apply { gravity = Gravity.CENTER })
    }
}
