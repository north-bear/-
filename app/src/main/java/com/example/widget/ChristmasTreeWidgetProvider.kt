package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.R
import com.example.data.BlossomPalette
import com.example.data.BlossomShape
import com.example.data.NoteColor
import com.example.data.WidgetCacheStore

class ChristmasTreeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_CYCLE_THEME -> {
                WidgetCacheStore.cycleTheme(context)
                updateAllWidgets(context)
            }
            ACTION_PREV_NOTE -> {
                WidgetCacheStore.stepActiveIndex(context, -1)
                updateAllWidgets(context)
            }
            ACTION_NEXT_NOTE -> {
                WidgetCacheStore.stepActiveIndex(context, +1)
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_CYCLE_THEME = "com.example.widget.ACTION_CYCLE_THEME"
        const val ACTION_PREV_NOTE = "com.example.widget.ACTION_PREV_NOTE"
        const val ACTION_NEXT_NOTE = "com.example.widget.ACTION_NEXT_NOTE"
        const val EXTRA_WIDGET_ACTION = "extra_widget_action"
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val ACTION_QUICK_ADD = "quick_add"
        const val ACTION_OPEN_NOTE = "open_note"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ChristmasTreeWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateSingleWidget(context, appWidgetManager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, ChristmasTreeWidgetProvider::class.java)
            return appWidgetManager.requestPinAppWidget(provider, null, null)
        }

        private fun isSystemDarkMode(context: Context): Boolean {
            val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightMask == Configuration.UI_MODE_NIGHT_YES
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val cache = WidgetCacheStore.loadCacheState(context)
            val isDark = isSystemDarkMode(context)
            val views = RemoteViews(context.packageName, R.layout.widget_christmas_tree)

            val activeNote = if (cache.signedIn && cache.notes.isNotEmpty()) {
                cache.notes.getOrNull(cache.activeIndex) ?: cache.notes.first()
            } else {
                null
            }
            val activeColor = activeNote?.let { NoteColor.safeFromKey(it.colorKey) }

            if (cache.config.palette == BlossomPalette.SYSTEM_MD3) {
                // Use VectorDrawable + @color/widget_md3_* so Launcher switches Light/Dark & Material You automatically
                val shapeDrawable = when (cache.config.shape) {
                    BlossomShape.M3_BLOSSOM -> R.drawable.bg_md3_cookie_7
                    BlossomShape.FIVE_PETAL_SAKURA -> R.drawable.bg_sakura_5
                }
                views.setImageViewResource(R.id.widget_tree_canvas, shapeDrawable)
            } else {
                val blossomBitmap = ChristmasTreeBitmapRenderer.renderBlossomBitmap(
                    config = cache.config,
                    activeNoteColor = activeColor,
                    isDark = isDark
                )
                views.setImageViewBitmap(R.id.widget_tree_canvas, blossomBitmap)

                val paletteColors = resolveCustomColors(cache.config.palette, activeColor, isDark)
                views.setTextColor(R.id.widget_note_title, paletteColors.title)
                views.setTextColor(R.id.widget_note_snippet, paletteColors.snippet)
                views.setInt(R.id.widget_badge_bg, "setColorFilter", paletteColors.badgeBg)
                views.setTextColor(R.id.widget_badge_plus, paletteColors.badgeFg)
                views.setTextColor(R.id.widget_prev_btn, paletteColors.title)
                views.setTextColor(R.id.widget_page_indicator, paletteColors.title)
                views.setTextColor(R.id.widget_next_btn, paletteColors.title)
            }

            // Top circular '+' badge -> Quick Add
            val quickAddIntent = createActivityPendingIntent(
                context = context,
                requestCode = 1001 + appWidgetId,
                actionType = ACTION_QUICK_ADD,
                noteId = null
            )
            views.setOnClickPendingIntent(R.id.widget_star_hotspot, quickAddIntent)

            if (activeNote == null) {
                views.setTextViewText(
                    R.id.widget_note_title,
                    if (cache.signedIn) context.getString(R.string.widget_empty_prompt)
                    else context.getString(R.string.widget_signin_prompt)
                )
                views.setTextViewText(
                    R.id.widget_note_snippet,
                    if (cache.signedIn) "支持 Keep 彩色便签与清单" else "同步个人彩色便利贴"
                )
                views.setViewVisibility(R.id.widget_pager_bar, View.INVISIBLE)
                views.setOnClickPendingIntent(
                    R.id.widget_note_body,
                    createActivityPendingIntent(
                        context = context,
                        requestCode = 3001 + appWidgetId,
                        actionType = if (cache.signedIn) ACTION_QUICK_ADD else null,
                        noteId = null
                    )
                )
            } else {
                views.setTextViewText(R.id.widget_note_title, activeNote.title)
                views.setTextViewText(R.id.widget_note_snippet, activeNote.snippet)
                views.setOnClickPendingIntent(
                    R.id.widget_note_body,
                    createActivityPendingIntent(
                        context = context,
                        requestCode = 4000 + appWidgetId,
                        actionType = ACTION_OPEN_NOTE,
                        noteId = activeNote.id
                    )
                )

                if (cache.notes.size > 1) {
                    views.setViewVisibility(R.id.widget_pager_bar, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_page_indicator,
                        "${cache.activeIndex + 1}/${cache.notes.size}"
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_prev_btn,
                        createBroadcastPendingIntent(context, 5001 + appWidgetId, ACTION_PREV_NOTE)
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_next_btn,
                        createBroadcastPendingIntent(context, 6001 + appWidgetId, ACTION_NEXT_NOTE)
                    )
                } else {
                    views.setViewVisibility(R.id.widget_pager_bar, View.INVISIBLE)
                }
            }

            // Full updateAppWidget required whenever replacing drawables/bitmaps cleanly
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private data class CustomWidgetColors(
            val title: Int,
            val snippet: Int,
            val badgeBg: Int,
            val badgeFg: Int
        )

        private fun resolveCustomColors(
            palette: BlossomPalette,
            activeColor: NoteColor?,
            isDark: Boolean
        ): CustomWidgetColors {
            return if (palette == BlossomPalette.MATCH_NOTE && activeColor != null) {
                CustomWidgetColors(
                    title = if (isDark) 0xFFEFF6F2.toInt() else 0xFF14221D.toInt(),
                    snippet = if (isDark) 0xCCB8D6C6.toInt() else 0xCC35463F.toInt(),
                    badgeBg = activeColor.ornamentColor.toArgb(),
                    badgeFg = 0xFFFFFFFF.toInt()
                )
            } else {
                CustomWidgetColors(
                    title = palette.textPrimaryHex(isDark).toInt(),
                    snippet = palette.textSecondaryHex(isDark).toInt(),
                    badgeBg = palette.badgeBgHex(isDark).toInt(),
                    badgeFg = palette.badgeFgHex(isDark).toInt()
                )
            }
        }

        private fun createBroadcastPendingIntent(
            context: Context,
            requestCode: Int,
            action: String
        ): PendingIntent {
            val intent = Intent(context, ChristmasTreeWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createActivityPendingIntent(
            context: Context,
            requestCode: Int,
            actionType: String?,
            noteId: String?
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (actionType != null) putExtra(EXTRA_WIDGET_ACTION, actionType)
                if (noteId != null) putExtra(EXTRA_NOTE_ID, noteId)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
