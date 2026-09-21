package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R

class ReadingModeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, "Active Document", "Tap Visit Screen to open reader or Listen to start speech.")
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            docTitle: String = "Active Document",
            previewText: String = "Tap Visit Screen to open reader or Listen to start speech."
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_reading_mode)

            views.setTextViewText(R.id.widget_doc_title, docTitle)
            views.setTextViewText(R.id.widget_preview_text, previewText)

            // Visit Reading Screen Intent
            val visitIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_reading_mode", true)
            }
            val visitPendingIntent = PendingIntent.getActivity(
                context,
                101,
                visitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_visit, visitPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, visitPendingIntent)

            // Listen / Read Aloud Intent
            val readIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_reading_mode", true)
                putExtra("start_reading", true)
            }
            val readPendingIntent = PendingIntent.getActivity(
                context,
                102,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_read, readPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAll(context: Context, docTitle: String, previewText: String) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, ReadingModeWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                for (id in ids) {
                    updateAppWidget(context, appWidgetManager, id, docTitle, previewText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun requestPinWidget(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                val provider = ComponentName(context, ReadingModeWidgetProvider::class.java)
                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                    Toast.makeText(context, "Adding Reading Screen widget to Home Screen...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please add the Reading Screen widget from your home screen launcher menu.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Long-press your phone's home screen to add the Reading Screen widget.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
