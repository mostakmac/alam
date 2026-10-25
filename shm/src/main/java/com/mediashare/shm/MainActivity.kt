package com.mediashare.shm

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.mediashare.shared.FirebaseBootstrap
import com.mediashare.shared.FirebaseClientConfig
import java.util.UUID

class MainActivity : Activity() {
    private val selectedMedia = mutableListOf<Uri>()
    private var firebaseApp: FirebaseApp? = null
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var shareListener: ListenerRegistration? = null
    private var uploadIndex = 0
    private var successfulUploads = 0
    private lateinit var accountCard: LinearLayout
    private lateinit var sharingCard: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var accountStatus: TextView
    private lateinit var selectionSummary: TextView
    private lateinit var mediaPreview: LinearLayout
    private lateinit var shareButton: Button
    private lateinit var uploadStatus: TextView
    private lateinit var sharesList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = FirebaseClientConfig(
            apiKey = BuildConfig.FIREBASE_API_KEY,
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET,
            senderId = BuildConfig.FIREBASE_SENDER_ID,
            appId = BuildConfig.FIREBASE_APP_ID,
            appName = "mediashare-shm"
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
        shareListener?.remove()
        super.onDestroy()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(PALE_BLUE)
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
        val logo = text("SHM", 18f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(BLUE, dp(14))
        }
        brand.addView(logo, LinearLayout.LayoutParams(dp(54), dp(48)))
        brand.addView(text("  YOUR MEDIA. YOUR CHOICE.", 12f, NAVY, true))
        brand.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        brand.addView(text("SECURE", 11f, DARK_GREEN, true).apply {
            setPadding(dp(11), dp(7), dp(11), dp(7))
            background = rounded(PALE_GREEN, dp(14))
        })
        content.addView(brand)
        content.addView(text("Share on\nyour terms.", 36f, NAVY, true).apply {
            setPadding(0, dp(26), 0, dp(9))
        })
        content.addView(text(
            "Choose specific photos or videos to share with your authorized REV admin.",
            16f,
            MUTED
        ))

        val privacyCard = card()
        privacyCard.addView(text("YOU’RE IN CONTROL", 12f, DARK_GREEN, true))
        privacyCard.addView(text(
            "SHM cannot browse your gallery. Android’s secure picker gives access only to items you select. Each upload needs your confirmation.",
            15f,
            NAVY
        ).apply { setPadding(0, dp(8), 0, 0) })
        content.addView(privacyCard, matchWrap(dp(16)))

        accountCard = card()
        content.addView(accountCard, matchWrap(dp(16)))

        sharingCard = card().apply { visibility = View.GONE }
        sharingCard.addView(text("Choose media to share", 21f, NAVY, true))
        sharingCard.addView(text(
            "Select photos or videos. They will upload to your configured sharing service only after you confirm.",
            14f,
            MUTED
        ).apply { setPadding(0, dp(6), 0, dp(14)) })
        val pickerButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        pickerButtons.addView(button("Choose photos", BLUE, Color.WHITE) {
            openPicker("image/*")
        }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(5) })
        pickerButtons.addView(button("Choose videos", PINK, Color.WHITE) {
            openPicker("video/*")
        }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(5) })
        sharingCard.addView(pickerButtons)
        selectionSummary = text("No media selected", 14f, MUTED).apply {
            setPadding(0, dp(14), 0, dp(6))
        }
        sharingCard.addView(selectionSummary)
        mediaPreview = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sharingCard.addView(mediaPreview)
        uploadStatus = text("", 14f, DARK_GREEN).apply { setPadding(0, dp(8), 0, 0) }
        sharingCard.addView(uploadStatus)
        shareButton = button("Review and share", BLUE, Color.WHITE) { confirmShare() }
        shareButton.isEnabled = false
        sharingCard.addView(shareButton, matchWrap(dp(14)))
        sharingCard.addView(text("Your shared items", 19f, NAVY, true))
        sharingCard.addView(text(
            "Revoke a share to remove it from REV and delete its stored file.",
            13f,
            MUTED
        ).apply { setPadding(0, dp(4), 0, dp(8)) })
        sharesList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sharingCard.addView(sharesList)
        content.addView(sharingCard, matchWrap(dp(16)))

        content.addView(text(
            "Cross-device sharing requires SHM and REV to use the same Firebase project. The user chooses and approves every item.",
            13f,
            MUTED
        ).apply { setPadding(dp(4), dp(4), dp(4), 0) })
    }

    private fun renderAccount(user: FirebaseUser?) {
        if (!::accountCard.isInitialized) return
        shareListener?.remove()
        shareListener = null
        accountCard.removeAllViews()
        if (firebaseApp == null || auth == null) {
            sharingCard.visibility = View.GONE
            accountStatus = text("Firebase setup needed", 20f, NAVY, true)
            accountCard.addView(accountStatus)
            accountCard.addView(text(
                "Set the Firebase Actions variables from the README and enable Email/Password sign-in, Firestore, and Storage.",
                14f,
                MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })
            return
        }
        if (user != null) {
            accountStatus = text("Signed in as ${user.email ?: "SHM user"}", 16f, NAVY, true)
            accountCard.addView(accountStatus)
            accountCard.addView(button("Sign out", PALE_PINK, PINK) {
                auth?.signOut()
            }, matchWrap(dp(6)))
            sharingCard.visibility = View.VISIBLE
            subscribeToMyShares(user.uid)
            return
        }

        sharingCard.visibility = View.GONE
        accountCard.addView(text("Sign in to share safely", 20f, NAVY, true))
        accountCard.addView(text(
            "Create your SHM account once, then use it when sharing from any device.",
            14f,
            MUTED
        ).apply { setPadding(0, dp(6), 0, dp(12)) })
        emailInput = editText("Email address", false)
        passwordInput = editText("Password (at least 6 characters)", true)
        accountCard.addView(emailInput, matchWrap(dp(8)))
        accountCard.addView(passwordInput, matchWrap(dp(12)))
        accountStatus = text("", 13f, PINK)
        accountCard.addView(accountStatus)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("Sign in", BLUE, Color.WHITE) {
            authenticate(createAccount = false)
        }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { rightMargin = dp(5) })
        actions.addView(button("Create account", GREEN, Color.WHITE) {
            authenticate(createAccount = true)
        }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { leftMargin = dp(5) })
        accountCard.addView(actions)
    }

    private fun authenticate(createAccount: Boolean) {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        if (email.isBlank() || password.length < 6) {
            accountStatus.text = "Enter a valid email and a password with at least 6 characters."
            return
        }
        accountStatus.setTextColor(MUTED)
        accountStatus.text = if (createAccount) "Creating your account…" else "Signing in…"
        val task = if (createAccount) {
            auth?.createUserWithEmailAndPassword(email, password)
        } else {
            auth?.signInWithEmailAndPassword(email, password)
        }
        task?.addOnFailureListener { error ->
            Log.w(TAG, "Firebase sign-in failed", error)
            accountStatus.setTextColor(PINK)
            accountStatus.text = error.localizedMessage ?: "Could not sign in. Check your connection and try again."
        } ?: run {
            accountStatus.setTextColor(PINK)
            accountStatus.text = "Firebase is not configured yet."
        }
    }

    private fun subscribeToMyShares(uid: String) {
        shareListener = firestore?.collection("shares")
            ?.whereEqualTo("ownerUid", uid)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Could not load user's shared media", error)
                    renderShareRows(emptyList())
                    uploadStatus.text = error.localizedMessage ?: "Could not load your shares."
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().map { document ->
                    ShareRow(
                        id = document.id,
                        name = document.getString("fileName") ?: "Shared media",
                        status = document.getString("status") ?: "uploading",
                        storagePath = document.getString("storagePath").orEmpty()
                    )
                }
                renderShareRows(items)
            }
    }

    private fun renderShareRows(items: List<ShareRow>) {
        if (!::sharesList.isInitialized) return
        sharesList.removeAllViews()
        if (items.isEmpty()) {
            sharesList.addView(text("Nothing shared yet.", 14f, MUTED))
            return
        }
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(9), dp(8), dp(9))
                background = rounded(PALE_BLUE, dp(14))
            }
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            details.addView(text(item.name, 14f, NAVY, true))
            details.addView(text(item.status.replaceFirstChar(Char::uppercase), 12f, MUTED))
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(button("Revoke", PALE_PINK, PINK) {
                revokeShare(item)
            })
            sharesList.addView(row, matchWrap(dp(6)))
        }
    }

    private fun revokeShare(item: ShareRow) {
        AlertDialog.Builder(this)
            .setTitle("Revoke this share?")
            .setMessage("This removes the shared file and its listing from the REV inbox.")
            .setNegativeButton("Keep share", null)
            .setPositiveButton("Revoke") { _, _ ->
                val shareDocument = firestore?.collection("shares")?.document(item.id)
                if (item.storagePath.isBlank()) {
                    shareDocument?.delete()
                        ?.addOnSuccessListener { toast("Share revoked.") }
                        ?.addOnFailureListener { error -> showError("Could not remove the share", error) }
                    return@setPositiveButton
                }
                val removeFile = storage?.reference?.child(item.storagePath)?.delete()
                removeFile?.addOnSuccessListener {
                    firestore?.collection("shares")?.document(item.id)?.delete()
                        ?.addOnSuccessListener { toast("Share revoked.") }
                        ?.addOnFailureListener { error -> showError("Could not remove the share", error) }
                }?.addOnFailureListener { error ->
                    if (error is com.google.firebase.storage.StorageException &&
                        error.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND
                    ) {
                        firestore?.collection("shares")?.document(item.id)?.delete()
                            ?.addOnFailureListener { deleteError -> showError("Could not remove the share", deleteError) }
                    } else {
                        showError("Could not delete the stored media", error)
                    }
                } ?: showError("Sharing service is not available", IllegalStateException("Firebase is not configured"))
            }
            .show()
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
        } catch (error: android.content.ActivityNotFoundException) {
            Log.e(TAG, "No compatible Android photo picker was found", error)
            toast("This device does not have a compatible photo picker.")
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
            val kind = if (contentResolver.getType(uri).orEmpty().startsWith("video/")) "Video" else "Photo"
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(7), dp(10), dp(7))
                background = rounded(PALE_BLUE, dp(13))
            }
            val thumbnail = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(if (kind == "Video") PALE_PINK else PALE_GREEN, dp(10))
                setImageURI(uri)
            }
            row.addView(thumbnail, LinearLayout.LayoutParams(dp(48), dp(48)))
            val name = getDisplayName(uri) ?: "$kind ${index + 1}"
            row.addView(text("$kind  ·  $name", 13f, NAVY, true).apply {
                setPadding(dp(10), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            mediaPreview.addView(row, matchWrap(dp(5)))
        }
    }

    private fun confirmShare() {
        if (selectedMedia.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Confirm sharing")
            .setMessage(
                "Upload these ${selectedMedia.size} selected original item(s), their file names, and a random account ID to the configured Firebase project for authorized REV admins? A recipient may keep a downloaded copy. You can later revoke the server copy."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Share selected items") { _, _ -> startUploads() }
            .show()
    }

    private fun startUploads() {
        if (auth?.currentUser == null || selectedMedia.isEmpty()) return
        uploadIndex = 0
        successfulUploads = 0
        shareButton.isEnabled = false
        uploadNext(auth?.currentUser)
    }

    private fun uploadNext(user: FirebaseUser?) {
        if (user == null) return
        if (uploadIndex >= selectedMedia.size) {
            uploadStatus.text = if (successfulUploads == 0) {
                "No supported media was shared. Select a photo or video and try again."
            } else {
                "$successfulUploads approved item(s) have been shared with REV."
            }
            selectedMedia.clear()
            refreshSelection()
            shareButton.isEnabled = true
            return
        }
        val uri = selectedMedia[uploadIndex]
        val contentType = contentResolver.getType(uri)
        if (contentType == null ||
            (!contentType.startsWith("image/") && !contentType.startsWith("video/"))
        ) {
            uploadIndex++
            uploadStatus.text = "Skipped a file that is not a recognized photo or video."
            uploadNext(user)
            return
        }
        val shareId = "${user.uid}_${UUID.randomUUID()}"
        val name = sanitizeFileName(getDisplayName(uri) ?: "media_${uploadIndex + 1}")
        val objectPath = "shares/${user.uid}/$shareId/$name"
        val document = firestore?.collection("shares")?.document(shareId)
        val userStorage = storage
        if (document == null || userStorage == null) {
            uploadFailed("Firebase is not configured. No media was uploaded.")
            return
        }

        uploadStatus.text = "Preparing item ${uploadIndex + 1} of ${selectedMedia.size}…"
        val metadata = hashMapOf<String, Any>(
            "ownerUid" to user.uid,
            "fileName" to name,
            "contentType" to contentType,
            "storagePath" to objectPath,
            "status" to "uploading",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        document.set(metadata)
            .addOnSuccessListener {
                uploadStatus.text = "Uploading item ${uploadIndex + 1} of ${selectedMedia.size}…"
                userStorage.reference.child(objectPath).putFile(uri)
                    .addOnSuccessListener {
                        document.update("status", "approved")
                            .addOnSuccessListener {
                                successfulUploads++
                                uploadIndex++
                                uploadNext(auth?.currentUser)
                            }
                            .addOnFailureListener { error ->
                                Log.e(TAG, "Media uploaded, but approval metadata could not be saved", error)
                                userStorage.reference.child(objectPath).delete()
                                    .addOnFailureListener { cleanupError ->
                                        Log.e(TAG, "Could not remove media after approval failed", cleanupError)
                                    }
                                    .addOnCompleteListener {
                                        document.delete().addOnFailureListener { cleanupError ->
                                            Log.e(TAG, "Could not remove failed share metadata", cleanupError)
                                        }
                                        uploadFailed("Upload finished, but sharing could not be confirmed. Try again.")
                                    }
                            }
                    }
                    .addOnFailureListener { error ->
                        document.delete().addOnFailureListener { cleanupError ->
                            Log.e(TAG, "Could not remove incomplete share record", cleanupError)
                        }
                        uploadFailed(error.localizedMessage ?: "Upload failed. Check your connection and try again.")
                    }
            }
            .addOnFailureListener { error ->
                uploadFailed(error.localizedMessage ?: "Could not start sharing. Check your connection and try again.")
            }
    }

    private fun uploadFailed(message: String) {
        shareButton.isEnabled = true
        uploadStatus.text = message
        toast(message)
    }

    private fun getDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val result = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor = result
            if (result?.moveToFirst() == true) {
                result.getString(result.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Could not read selected media name", error)
            null
        } finally {
            cursor?.close()
        }
    }

    private fun sanitizeFileName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "media" }

    private fun editText(hintText: String, password: Boolean) =
        EditText(this).apply {
            hint = hintText
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(PALE_BLUE, dp(13))
            if (password) {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
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

    private fun showError(action: String, error: Exception) {
        Log.e(TAG, action, error)
        toast("$action: ${error.localizedMessage ?: "unknown error"}")
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private data class ShareRow(
        val id: String,
        val name: String,
        val status: String,
        val storagePath: String
    )

    companion object {
        private const val TAG = "MediaShareSHM"
        private const val REQUEST_MEDIA = 11
        private val BLUE = Color.rgb(40, 120, 240)
        private val PINK = Color.rgb(234, 84, 151)
        private val GREEN = Color.rgb(31, 155, 103)
        private val PALE_BLUE = Color.rgb(244, 247, 255)
        private val PALE_GREEN = Color.rgb(229, 248, 237)
        private val PALE_PINK = Color.rgb(255, 234, 244)
        private val NAVY = Color.rgb(26, 42, 77)
        private val MUTED = Color.rgb(102, 117, 145)
        private val DARK_GREEN = Color.rgb(29, 137, 87)
    }
}
