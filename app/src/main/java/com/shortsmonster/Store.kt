package com.shortsmonster
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object Store {
    private fun p(c: Context) = c.getSharedPreferences("sm", Context.MODE_PRIVATE)
    fun day(offset: Int = 0): String {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -offset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    fun label(offset: Int): String {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -offset)
        return SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(cal.time)
    }
    fun count(c: Context, offset: Int = 0) = p(c).getInt("c_" + day(offset), 0)
    fun week(c: Context) = (0..6).sumOf { count(c, it) }
    fun inc(c: Context): Int { val n = count(c) + 1; p(c).edit().putInt("c_" + day(), n).apply(); return n }
    fun reset(c: Context) { p(c).edit().clear().apply() }
    fun mode(c: Context) = p(c).getInt("mode", 0)            // 0 manual, 1 level one, 2 level two
    fun setMode(c: Context, m: Int) = p(c).edit().putInt("mode", m).apply()
    fun secs(c: Context) = p(c).getInt("secs", 30)           // average seconds per short
    fun setSecs(c: Context, s: Int) = p(c).edit().putInt("secs", s.coerceIn(1, 600)).apply()
    fun x(c: Context, def: Int) = p(c).getInt("x", def)
    fun y(c: Context) = p(c).getInt("y", 140)
    fun setPos(c: Context, x: Int, y: Int) = p(c).edit().putInt("x", x).putInt("y", y).apply()
     fun emoji(n: Int) = when { n < 20 -> "🐣"; n < 60 -> "👾"; n < 120 -> "🦖"; else -> "🐉" }
    fun stage(n: Int) = when { n < 20 -> "Baby blob"; n < 60 -> "Snack monster"; n < 120 -> "Chonky beast"; else -> "Doom dragon" }
     fun fmt(sec: Long): String { val h = sec / 3600; val m = (sec % 3600) / 60; return if (h > 0) "${h}h ${m}m" else "${m}m ${sec % 60}s" }
}
