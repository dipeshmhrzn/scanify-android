package com.scanify.app.presentation.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

fun sendEmail(context: Context, title: String, description: String) {
    val supportEmail = "scanifylabs.dev@gmail.com"

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, description)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app installed.", Toast.LENGTH_SHORT).show()
    }
}