package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

object ReadingShortcutHelper {
    const val SHORTCUT_ID = "shortcut_reading_screen"

    fun pinReadingShortcut(context: Context) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("open_reading_mode", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pinShortcutInfo = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                .setShortLabel("Reading Mode")
                .setLongLabel("Open Reading Screen")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()

            val success = ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
            if (success) {
                Toast.makeText(context, "Shortcut requested for Home Screen", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Unable to pin shortcut on this launcher", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Pinning shortcuts not supported by launcher", Toast.LENGTH_SHORT).show()
        }
    }

    fun publishDynamicShortcut(context: Context) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("open_reading_mode", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                .setShortLabel("Reading Mode")
                .setLongLabel("Open Reading Screen")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (_: Exception) {}
    }
}
