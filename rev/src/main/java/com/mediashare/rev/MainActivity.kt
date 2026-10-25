package com.mediashare.rev

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.mediashare.shared.FirebaseBootstrap
import com.mediashare.shared.FirebaseClientConfig
import java.io.File
import java.util.Date

class MainActivity : Activity() {
    private var firebaseApp: FirebaseApp? = null
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var adminListener: ListenerRegistration? = null
    private var shareListener: ListenerRegistration? = null
    private lateinit var accountCard: LinearLayout
    private lateinit var inboxCard: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var accountStatus: TextView
    private lateinit var inboxStatus: TextView
    private lateinit var mediaList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = FirebaseClientConfig(
            apiKey = BuildConfig.FIREBASE_API_KEY,
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET,
            senderId = BuildConfig.FIREBASE_SENDER_ID,
            appId = BuildConfig.FIREBASE_APP_ID,
            appName = "mediashare-rev"
        )
        try {
            firebaseApp = FirebaseBootstrap.initialize(this, config)
            firebaseApp?.let {
                auth = FirebaseAuth.getInstance(it)
                firestore = FirebaseFirestore.getInstance(it)
                storage = FirebaseStorage.getInstance(it)
            }
        } catch (error: FirebaseException) {
            Log.e(TAG, "Firebase initialization failed", error)
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Firebase client configuration is invalid", error)
        }
        buildScreen()
        renderAccount(auth?.currentUser)
        auth?.addAuthStateListener(authStateListener)
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        renderAccount(firebaseAuth.currentUser)
    }

    override fun onDestroy() {
        auth?.removeAuthStateListener(authStateListener)
        adminListener?.remove()
        shareListener?.remove()
        super.onDestroy()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(PALE_GREEN)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(36))
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
        brand.addView(text("  SECURE MEDIA INBOX", 12f, NAVY, true))
        brand.addView(android.view.View(this), LinearLayout.LayoutParams(0, 1, 1f))
        brand.addView(text("ADMIN", 11f, DARK_GREEN, true).apply {
            setPadding(dp(11), dp(7), dp(11), dp(7))
            background = rounded(PALE_YELLOW, dp(14))
        })
        content.addView(brand)
        content.addView(text("Approved media,\nall in one place.", 32f, NAVY, true).apply {
            setPadding(0, dp(26), 0, dp(9))
        })
        content.addView(text(
            "Only items the user explicitly selects and approves appear in this inbox.",
            16f,
            MUTED
        ))

        val privacyCard = card().apply { background = rounded(PALE_YELLOW, dp(22)) }
        privacyCard.addView(text("PRIVATE BY DEFAULT", 12f, DARK_GREEN, true))
        privacyCard.addView(text(
            "REV has no gallery access. Only admin accounts you approve can view shared items. Previously downloaded copies cannot be recalled.",
            14f,
            NAVY
        ).apply { setPadding(0, dp(7), 0, 0) })
        content.addView(privacyCard, matchWrap(dp(16)))

        accountCard = card()
        content.addView(accountCard, matchWrap(dp(16)))

        inboxCard = card().apply { visibility = android.view.View.GONE }
        inboxCard.addView(text("SHARED WITH YOU", 12f, DARK_GREEN, true))
        inboxStatus = text("Checking admin access…", 14f, MUTED).apply {
            setPadding(0, dp(7), 0, dp(10))
        }
        inboxCard.addView(inboxStatus)
        mediaList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        inboxCard.addView(mediaList)
        content.addView(inboxCard, matchWrap(dp(18)))
    }

    private fun renderAccount(user: FirebaseUser?) {
        if (!::accountCard.isInitialized) return
        adminListener?.remove()
        adminListener = null
        shareListener?.remove()
        shareListener = null
        inboxCard.visibility = android.view.View.GONE
        accountCard.removeAllViews()

        if (firebaseApp == null || auth == null || firestore == null || storage == null) {
            accountCard.addView(text("Firebase setup needed", 20f, NAVY, true))
            accountCard.addView(text(
                "Set up both Firebase Android apps and the GitHub Actions variables as described in the README.",
                14f,
                MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })
            return
        }
        if (user != null) {
            accountCard.addView(text("Signed in as ${user.email ?: "REV admin"}", 16f, NAVY, true))
            accountCard.addView(button("Sign out", PALE_PINK, PINK) {
                auth?.signOut()
            }, matchWrap(dp(6)))
            checkAdminAccess(user.uid)
            return
        }

        accountCard.addView(text("Admin sign in", 20f, NAVY, true))
        accountCard.addView(text(
            "REV accounts must be created in Firebase and explicitly allowlisted by an owner.",
            14f,
            MUTED
        ).apply { setPadding(0, dp(6), 0, dp(12)) })
        emailInput = editText("Admin email address", false)
        passwordInput = editText("Password", true)
        accountCard.addView(emailInput, matchWrap(dp(8)))
        accountCard.addView(passwordInput, matchWrap(dp(12)))
        accountStatus = text("", 13f, PINK)
        accountCard.addView(accountStatus)
        accountCard.addView(button("Sign in to REV", BLUE, Color.WHITE) {
            signIn()
        }, matchWrap())
    }

    private fun signIn() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        if (email.isBlank() || password.isBlank()) {
            accountStatus.text = "Enter your approved administrator email and password."
            return
        }
        accountStatus.setTextColor(MUTED)
        accountStatus.text = "Signing in…"
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnFailureListener { error ->
                Log.w(TAG, "REV admin sign-in failed", error)
                accountStatus.setTextColor(PINK)
                accountStatus.text = error.localizedMessage ?: "Sign-in failed. Check your account and try again."
            }
    }

    private fun checkAdminAccess(uid: String) {
        inboxCard.visibility = android.view.View.VISIBLE
        inboxStatus.text = "Verifying the admin allowlist…"
        adminListener = firestore?.collection("admins")?.document(uid)
            ?.addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e(TAG, "Admin allowlist check failed", error)
                    inboxStatus.text = error.localizedMessage ?: "Could not verify admin access."
                    return@addSnapshotListener
                }
                if (document?.getBoolean("enabled") != true) {
                    inboxStatus.text = "This account is not on the admin allowlist. Ask the Firebase project owner for access."
                    mediaList.removeAllViews()
                    shareListener?.remove()
                    shareListener = null
                    return@addSnapshotListener
                }
                inboxStatus.text = "Admin access verified. Approved user shares update here."
                subscribeToApprovedShares()
            }
    }

    private fun subscribeToApprovedShares() {
        if (shareListener != null) return
        shareListener = firestore?.collection("shares")
            ?.whereEqualTo("status", "approved")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Could not load the approved share inbox", error)
                    inboxStatus.text = error.localizedMessage ?: "Could not load approved media."
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().map { document ->
                    SharedItem(
                        id = document.id,
                        ownerUid = document.getString("ownerUid") ?: "",
                        fileName = document.getString("fileName") ?: "Shared media",
                        contentType = document.getString("contentType") ?: "application/octet-stream",
                        storagePath = document.getString("storagePath").orEmpty(),
                        createdAt = document.getDate("createdAt")
                    )
                }.sortedByDescending { it.createdAt ?: Date(0) }
                inboxStatus.text = if (items.isEmpty()) {
                    "No approved media has been shared yet."
                } else {
                    "${items.size} approved item(s) · updates automatically"
                }
                renderShares(items)
            }
    }

    private fun renderShares(items: List<SharedItem>) {
        mediaList.removeAllViews()
        if (items.isEmpty()) {
            mediaList.addView(text(
                "When an SHM user confirms a share, the selected photo or video will appear here.",
                14f,
                MUTED
            ))
            return
        }
        items.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(13), dp(14), dp(13))
                background = rounded(PALE_BLUE, dp(16))
            }
            card.addView(text(item.fileName, 16f, NAVY, true))
            card.addView(text(
                "${if (item.contentType.startsWith("video/")) "Video" else "Photo"}  ·  SHM user ${item.ownerUid.takeLast(6)}",
                13f,
                MUTED
            ).apply { setPadding(0, dp(4), 0, dp(2)) })
            item.createdAt?.let { date ->
                card.addView(text(
                    android.text.format.DateFormat.getMediumDateFormat(this).format(date),
                    12f,
                    MUTED
                ))
            }
            val preview = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                visibility = android.view.View.GONE
            }
            card.addView(preview, LinearLayout.LayoutParams(-1, dp(220)).apply {
                topMargin = dp(8)
            })
            card.addView(button(
                if (item.contentType.startsWith("video/")) "Download and open video" else "Download and view photo",
                BLUE,
                Color.WHITE
            ) {
                downloadApprovedItem(item, preview)
            }, matchWrap(dp(7)))
            mediaList.addView(card, matchWrap(dp(10)))
        }
    }

    private fun downloadApprovedItem(item: SharedItem, preview: ImageView) {
        val currentUser = auth?.currentUser
        if (currentUser == null || item.storagePath.isBlank()) {
            toast("This shared item is unavailable.")
            return
        }
        val folder = File(cacheDir, "shared-media")
        if (!folder.exists() && !folder.mkdirs()) {
            toast("Could not create a temporary folder for this file.")
            return
        }
        val extension = item.fileName.substringAfterLast('.', "")
            .replace(Regex("[^A-Za-z0-9]"), "")
            .take(10)
            .ifBlank { if (item.contentType.startsWith("video/")) "mp4" else "jpg" }
        val file = File(folder, "${item.id.hashCode()}.$extension")
        toast("Downloading approved media…")
        storage?.reference?.child(item.storagePath)?.getFile(file)
            ?.addOnSuccessListener {
                if (item.contentType.startsWith("image/")) {
                    preview.setImageURI(Uri.fromFile(file))
                    preview.visibility = android.view.View.VISIBLE
                }
                openDownloadedFile(file, item.contentType)
            }
            ?.addOnFailureListener { error ->
                Log.e(TAG, "Could not download approved media", error)
                toast(error.localizedMessage ?: "Download failed. Check your connection and admin access.")
            }
            ?: toast("Firebase Storage is unavailable.")
    }

    private fun openDownloadedFile(file: File, mimeType: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(packageManager) == null) {
                toast("The file was downloaded for this session, but no compatible viewer is installed.")
                return
            }
            startActivity(intent)
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Could not open the downloaded file", error)
            toast("Downloaded the file, but could not open it.")
        } catch (error: android.content.ActivityNotFoundException) {
            Log.e(TAG, "No viewer is available for this media type", error)
            toast("Downloaded the file, but no compatible viewer is installed.")
        }
    }

    private fun editText(hintText: String, password: Boolean) =
        EditText(this).apply {
            hint = hintText
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(PALE_GREEN, dp(13))
            inputType = if (password) {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
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

    private fun button(value: String, color: Int, textColor: Int, action: () -> Unit) =
        Button(this).apply {
            text = value
            isAllCaps = false
            textSize = 14f
            setTextColor(textColor)
            background = rounded(color, dp(15))
            setOnClickListener { action() }
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

    private data class SharedItem(
        val id: String,
        val ownerUid: String,
        val fileName: String,
        val contentType: String,
        val storagePath: String,
        val createdAt: Date?
    )

    companion object {
        private const val TAG = "MediaShareREV"
        private val BLUE = Color.rgb(40, 120, 240)
        private val PINK = Color.rgb(234, 84, 151)
        private val GREEN = Color.rgb(31, 155, 103)
        private val PALE_GREEN = Color.rgb(245, 250, 247)
        private val PALE_BLUE = Color.rgb(244, 247, 255)
        private val PALE_YELLOW = Color.rgb(255, 248, 219)
        private val PALE_PINK = Color.rgb(255, 234, 244)
        private val NAVY = Color.rgb(26, 42, 77)
        private val MUTED = Color.rgb(102, 117, 145)
        private val DARK_GREEN = Color.rgb(29, 137, 87)
    }
}
