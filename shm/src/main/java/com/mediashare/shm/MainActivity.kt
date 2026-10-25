package com.mediashare.shm

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val selectedMedia = mutableListOf<Uri>()
    private lateinit var selectionSummary: TextView
    private lateinit var mediaPreview: LinearLayout
    private lateinit var shareButton: Button
    private lateinit var confirmationCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(PALE_BLUE)
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
        val logo = text("SHM", 18f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        logo.background = rounded(BLUE, dp(14))
        logo.layoutParams = LinearLayout.LayoutParams(dp(54), dp(48))
        brand.addView(logo)
        val tagline = text("  YOUR MEDIA. YOUR CHOICE.", 12f, NAVY, true)
        brand.addView(tagline)
        brand.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val demoTag = text("DEMO", 11f, AMBER, true).apply {
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = rounded(PALE_YELLOW, dp(14))
        }
        brand.addView(demoTag)
        content.addView(brand)

        content.addView(text("Share on\nyour terms.", 36f, NAVY, true).apply {
            setPadding(0, dp(26), 0, dp(10))
        })
        content.addView(text(
            "Choose exactly what you want to share. Nothing leaves this phone in this demo.",
            16f,
            MUTED
        ))

        val privacyCard = card()
        privacyCard.addView(text("YOU’RE IN CONTROL", 12f, DARK_GREEN, true))
        privacyCard.addView(text(
            "SHM never browses your gallery. Android’s photo picker only gives access to the items you choose.",
            15f,
            NAVY
        ).apply { setPadding(0, dp(8), 0, 0) })
        content.addView(privacyCard, matchWrap(dp(18)))

        val selectionCard = card()
        selectionCard.addView(text("Choose media", 22f, NAVY, true))
        selectionCard.addView(text(
            "Pick photos or videos using Android’s secure picker. No gallery permission is needed.",
            14f,
            MUTED
        ).apply { setPadding(0, dp(6), 0, dp(14)) })
        val pickerButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val photoButton = button("Choose photos", BLUE, Color.WHITE) {
            openPicker("image/*")
        }
        pickerButtons.addView(photoButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply {
            rightMargin = dp(6)
        })
        val videoButton = button("Choose videos", PINK, Color.WHITE) {
            openPicker("video/*")
        }
        pickerButtons.addView(videoButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply {
            leftMargin = dp(6)
        })
        selectionCard.addView(pickerButtons)
        selectionSummary = text("No media selected", 14f, MUTED).apply {
            setPadding(0, dp(14), 0, dp(6))
        }
        selectionCard.addView(selectionSummary)
        mediaPreview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        selectionCard.addView(mediaPreview)
        content.addView(selectionCard, matchWrap(dp(16)))

        val requestCard = card()
        requestCard.addView(text("Before you share", 20f, NAVY, true))
        requestCard.addView(text(
            "In a connected version, REV would request media and you would approve each share. This demo has no server, sends no requests, and uploads no files.",
            14f,
            MUTED
        ).apply { setPadding(0, dp(8), 0, dp(14)) })
        shareButton = button("Review selected media", BLUE, Color.WHITE) {
            reviewShare()
        }.apply { isEnabled = false }
        requestCard.addView(shareButton, matchWrap())
        content.addView(requestCard, matchWrap(dp(16)))

        confirmationCard = card().apply {
            visibility = View.GONE
            setBackgroundColor(PALE_GREEN)
        }
        confirmationCard.addView(text("DEMO SHARE RECORDED", 13f, DARK_GREEN, true))
        confirmationCard.addView(text(
            "This is only a local preview. No media was uploaded or sent to REV.",
            15f,
            NAVY
        ).apply { setPadding(0, dp(6), 0, 0) })
        content.addView(confirmationCard, matchWrap())
    }

    private fun openPicker(mimeType: String) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = mimeType
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 10)
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
        try {
            startActivityForResult(intent, REQUEST_MEDIA)
        } catch (_: android.content.ActivityNotFoundException) {
            toast("This device does not have a compatible media picker.")
        }
    }

    @Deprecated("Picker results use the platform activity result callback.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_MEDIA || resultCode != RESULT_OK || data == null) return
        selectedMedia.clear()
        data.data?.let(selectedMedia::add)
        data.clipData?.let { clips ->
            for (index in 0 until clips.itemCount) {
                val uri = clips.getItemAt(index).uri
                if (selectedMedia.none { it == uri }) selectedMedia.add(uri)
            }
        }
        refreshSelection()
    }

    private fun refreshSelection() {
        selectionSummary.text = when (selectedMedia.size) {
            0 -> "No media selected"
            1 -> "1 item selected"
            else -> "${selectedMedia.size} items selected"
        }
        shareButton.isEnabled = selectedMedia.isNotEmpty()
        mediaPreview.removeAllViews()
        selectedMedia.forEachIndexed { index, uri ->
            val mime = contentResolver.getType(uri).orEmpty()
            val kind = if (mime.startsWith("video/")) "Video" else "Photo"
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = rounded(PALE_BLUE, dp(14))
            }
            val thumbnail = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(if (kind == "Video") PALE_PINK else PALE_GREEN, dp(12))
                try {
                    setImageURI(uri)
                } catch (_: SecurityException) {
                    contentDescription = "Preview unavailable"
                }
            }
            row.addView(thumbnail, LinearLayout.LayoutParams(dp(48), dp(48)))
            val detail = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }
            detail.addView(text("$kind ${index + 1}", 15f, NAVY, true))
            detail.addView(text("Selected on this device only", 12f, MUTED))
            row.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
            mediaPreview.addView(row, matchWrap(dp(6)))
        }
    }

    private fun reviewShare() {
        if (selectedMedia.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Review before sharing")
            .setMessage(
                "You selected ${selectedMedia.size} item(s). This is a demo: confirming only shows a local preview. Nothing is uploaded or sent to an admin."
            )
            .setNegativeButton("Go back", null)
            .setPositiveButton("Confirm demo") { _, _ ->
                confirmationCard.visibility = View.VISIBLE
                toast("Demo only — no media was sent.")
            }
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
            textSize = 14f
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

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        private const val REQUEST_MEDIA = 11
        private val BLUE = Color.rgb(40, 120, 240)
        private val PINK = Color.rgb(234, 84, 151)
        private val PALE_BLUE = Color.rgb(244, 247, 255)
        private val PALE_GREEN = Color.rgb(229, 248, 237)
        private val PALE_PINK = Color.rgb(255, 234, 244)
        private val PALE_YELLOW = Color.rgb(255, 248, 219)
        private val NAVY = Color.rgb(26, 42, 77)
        private val MUTED = Color.rgb(102, 117, 145)
        private val DARK_GREEN = Color.rgb(29, 137, 87)
        private val AMBER = Color.rgb(148, 104, 0)
    }
}
