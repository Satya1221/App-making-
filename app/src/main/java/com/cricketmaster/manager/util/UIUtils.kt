package com.cricketmaster.manager.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

object UIUtils {

    // Dark professional sports theme colors
    val bg = Color.rgb(10, 13, 20)
    val surface = Color.rgb(23, 27, 38)
    val surface2 = Color.rgb(30, 35, 48)
    val border = Color.rgb(49, 57, 76)
    val gold = Color.rgb(239, 190, 67)
    val textPrimary = Color.rgb(235, 238, 246)
    val muted = Color.rgb(151, 159, 177)
    val green = Color.rgb(63, 205, 125)
    val red = Color.rgb(226, 78, 91)
    val blue = Color.rgb(74, 144, 226)

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

    fun bgDrawable(context: Context, color: Int, radiusDp: Int = 12, strokeDp: Int = 0, strokeColor: Int = border): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(context, radiusDp).toFloat()
            if (strokeDp > 0) {
                setStroke(dp(context, strokeDp), strokeColor)
            }
        }
    }

    fun tv(context: Context, value: String, sizeSp: Float = 14f, color: Int = textPrimary, bold: Boolean = false): TextView {
        return TextView(context).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
            setPadding(dp(context, 4), dp(context, 2), dp(context, 4), dp(context, 2))
            if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    fun card(context: Context, parent: LinearLayout, title: String, body: String, action: (() -> Unit)? = null) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
            background = bgDrawable(context, surface, 14, 1)
            if (action != null) {
                isClickable = true
                setOnClickListener { action() }
            }
        }
        container.addView(tv(context, title, 16f, textPrimary, true))
        container.addView(tv(context, body, 13f, muted))

        val lp = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(context, 5), 0, dp(context, 5))
        }
        parent.addView(container, lp)
    }

    fun pill(context: Context, parent: LinearLayout, label: String, value: String, valueColor: Int = gold) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8))
            background = bgDrawable(context, surface2, 10, 1)
        }
        container.addView(tv(context, label.uppercase(), 9.5f, muted, true))
        container.addView(tv(context, value, 15f, valueColor, true))

        val lp = LinearLayout.LayoutParams(0, -2, 1f).apply {
            setMargins(dp(context, 3), 0, dp(context, 3), 0)
        }
        parent.addView(container, lp)
    }

    fun button(context: Context, parent: LinearLayout, label: String, primary: Boolean = true, run: () -> Unit) {
        val b = TextView(context).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
            background = bgDrawable(context, if (primary) gold else surface, 12, 1, if (primary) gold else border)
            setTextColor(if (primary) Color.BLACK else textPrimary)
            setOnClickListener { run() }
        }
        val lp = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(context, 5), 0, dp(context, 5))
        }
        parent.addView(b, lp)
    }

    fun sectionHeader(context: Context, parent: LinearLayout, title: String, rightText: String = "") {
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(context, 14), 0, dp(context, 6))
        }
        row.addView(tv(context, title.uppercase(), 11.5f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (rightText.isNotBlank()) {
            row.addView(tv(context, rightText.uppercase(), 11f, gold, true))
        }
        parent.addView(row)
    }

    fun Double.format(digits: Int = 2) = "%.${digits}f".format(this)
}
