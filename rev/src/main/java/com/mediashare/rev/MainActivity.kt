package com.mediashare.rev

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(PALE_GREEN)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(30), dp(22), dp(36))
        }
        scroll.addView(content)
        setContentView(scroll)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = text("REV", 18f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(GREEN, dp(14))
        }
        brand.addView(logo, LinearLayout.LayoutParams(dp(54), dp(48)))
        brand.addView(text("  CONSENT-FIRST ADMIN", 12f, NAVY, true))
        brand.addView(android.view.View(this), LinearLayout.LayoutParams(0, 1, 1f))
        brand.addView(text("DEMO", 11f, DARK_GREEN, true).apply {
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = rounded(PALE_YELLOW, dp(14))
        })
        content.addView(brand)

        content.addView(text("Respectful\nmedia sharing.", 34f, NAVY, true).apply {
            setPadding(0, dp(26), 0, dp(10))
        })
        content.addView(text(
            "The user always chooses what to share. Admins never browse a device gallery.",
            16f,
            MUTED
        ))

        val statusCard = card()
        statusCard.addView(text("SHARING STATUS", 12f, DARK_GREEN, true))
        statusCard.addView(text("No user connection", 22f, NAVY, true).apply {
            setPadding(0, dp(7), 0, dp(4))
        })
        statusCard.addView(text(
            "Demo mode is running locally. No users are monitored and no notifications are sent.",
            14f,
            MUTED
        ))
        content.addView(statusCard, matchWrap(dp(18)))

        val actionCard = card()
        actionCard.addView(text("Request a share", 21f, NAVY, true))
        actionCard.addView(text(
            "A real request would tell the user what you’re asking for and wait for their approval.",
            14f,
            MUTED
        ).apply { setPadding(0, dp(7), 0, dp(14)) })
        actionCard.addView(button("Preview consent request", BLUE, Color.WHITE) {
            previewRequest()
        }, matchWrap())
        actionCard.addView(text(
            "Preview only: this request is not delivered to anyone.",
            12f,
            MUTED
        ).apply { setPadding(0, dp(8), 0, 0) })
        content.addView(actionCard, matchWrap(dp(18)))

        val inboxCard = card()
        inboxCard.addView(text("Shared with you", 21f, NAVY, true))
        inboxCard.addView(text(
            "No shared media yet",
            16f,
            NAVY,
            true
        ).apply { setPadding(0, dp(16), 0, dp(4)) })
        inboxCard.addView(text(
            "When connected, only files a user deliberately selects and approves will appear here.",
            14f,
            MUTED
        ))
        inboxCard.addView(button("View sample inbox card", PINK, Color.WHITE) {
            showSampleInbox()
        }, matchWrap(dp(10)))
        content.addView(inboxCard, matchWrap(dp(18)))

        val privacyCard = card().apply { background = rounded(PALE_YELLOW, dp(22)) }
        privacyCard.addView(text("PRIVACY BY DESIGN", 12f, DARK_GREEN, true))
        privacyCard.addView(text(
            "No gallery permission • No silent downloads • No online-status tracking",
            14f,
            NAVY,
            true
        ).apply { setPadding(0, dp(8), 0, dp(5)) })
        privacyCard.addView(text(
            "To connect SHM and REV across phones, configure a secure backend, user sign-in, and explicit per-share consent.",
            13f,
            MUTED
        ))
        content.addView(privacyCard, matchWrap())
    }

    private fun previewRequest() {
        AlertDialog.Builder(this)
            .setTitle("Consent request preview")
            .setMessage(
                "Example request: “Would you like to choose photos or videos to share with REV?”\n\nThe user could approve or decline. This demo sends no request and contacts no one."
            )
            .setNegativeButton("Close", null)
            .setPositiveButton("Got it") { _, _ ->
                Toast.makeText(this, "Preview only — nothing was sent.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun showSampleInbox() {
        AlertDialog.Builder(this)
            .setTitle("Sample inbox card")
            .setMessage(
                "Example: “SHM user approved 2 selected photos.”\n\nThis is sample text only. There are no user files in this demo, and nothing can be downloaded."
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.WHITE, dp(22))
        elevation = dp(2).toFloat()
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun button(value: String, backgroundColor: Int, textColor: Int, onClick: () -> Unit) =
        Button(this).apply {
            text = value
            isAllCaps = false
            textSize = 15f
            setTextColor(textColor)
            background = rounded(backgroundColor, dp(16))
            setOnClickListener { onClick() }
        }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun matchWrap(bottom: Int = 0) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            if (bottom > 0) bottomMargin = bottom
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BLUE = Color.rgb(40, 120, 240)
        private val PINK = Color.rgb(234, 84, 151)
        private val GREEN = Color.rgb(31, 155, 103)
        private val PALE_GREEN = Color.rgb(245, 250, 247)
        private val PALE_YELLOW = Color.rgb(255, 248, 219)
        private val NAVY = Color.rgb(26, 42, 77)
        private val MUTED = Color.rgb(102, 117, 145)
        private val DARK_GREEN = Color.rgb(29, 137, 87)
    }
}
