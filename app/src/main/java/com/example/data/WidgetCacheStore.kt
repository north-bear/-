package com.example.data

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
data class WidgetNoteSnapshot(
    val id: String,
    val title: String,
    val snippet: String,
    val colorKey: String,
    val pinnedToTree: Boolean,
    val treeTier: Long
)

data class WidgetConfig(
    val shape: BlossomShape = BlossomShape.M3_BLOSSOM,
    val palette: BlossomPalette = BlossomPalette.SYSTEM_MD3,
    val showPetals: Boolean = true,
    val maxTreeNotes: Int = 6
)

data class WidgetCacheState(
    val signedIn: Boolean,
    val notes: List<WidgetNoteSnapshot>,
    val activeIndex: Int,
    val config: WidgetConfig
)

object WidgetCacheStore {
    private const val PREFS_NAME = "pinenote_widget_cache"
    private const val KEY_SIGNED_IN = "signed_in"
    private const val KEY_NOTES_JSON = "notes_json"
    private const val KEY_ACTIVE_INDEX = "active_index"
    private const val KEY_BLOSSOM_SHAPE = "blossom_shape"
    private const val KEY_BLOSSOM_PALETTE = "blossom_palette"
    private const val KEY_SHOW_PETALS = "show_petals"
    private const val KEY_MAX_NOTES = "max_tree_notes"

    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, WidgetNoteSnapshot::class.java)
    private val adapter = moshi.adapter<List<WidgetNoteSnapshot>>(listType)

    fun saveNotes(context: Context, signedIn: Boolean, notes: List<TreeNote>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prioritized = selectTreeNotes(notes, limit = 8).map { note ->
            WidgetNoteSnapshot(
                id = note.id,
                title = note.widgetTitle(),
                snippet = note.widgetSnippet(),
                colorKey = note.colorKey,
                pinnedToTree = note.pinnedToTree,
                treeTier = note.treeTier
            )
        }
        val json = adapter.toJson(prioritized)
        val clampedIndex = if (prioritized.isEmpty()) 0 else {
            prefs.getInt(KEY_ACTIVE_INDEX, 0).coerceIn(0, prioritized.lastIndex)
        }
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, signedIn)
            .putString(KEY_NOTES_JSON, json)
            .putInt(KEY_ACTIVE_INDEX, clampedIndex)
            .apply()
    }

    fun clearOnSignOut(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, false)
            .putInt(KEY_ACTIVE_INDEX, 0)
            .remove(KEY_NOTES_JSON)
            .apply()
    }

    fun loadConfig(context: Context): WidgetConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return WidgetConfig(
            shape = BlossomShape.safeFromName(prefs.getString(KEY_BLOSSOM_SHAPE, null)),
            palette = BlossomPalette.safeFromName(prefs.getString(KEY_BLOSSOM_PALETTE, null)),
            showPetals = prefs.getBoolean(KEY_SHOW_PETALS, true),
            maxTreeNotes = prefs.getInt(KEY_MAX_NOTES, 6).coerceIn(3, 8)
        )
    }

    fun saveConfig(context: Context, config: WidgetConfig) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_BLOSSOM_SHAPE, config.shape.name)
            .putString(KEY_BLOSSOM_PALETTE, config.palette.name)
            .putBoolean(KEY_SHOW_PETALS, config.showPetals)
            .putInt(KEY_MAX_NOTES, config.maxTreeNotes.coerceIn(3, 8))
            .apply()
    }

    fun stepActiveIndex(context: Context, delta: Int) {
        val state = loadCacheState(context)
        if (state.notes.isEmpty()) return
        val size = state.notes.size
        val next = ((state.activeIndex + delta) % size + size) % size
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ACTIVE_INDEX, next).apply()
    }

    fun cycleTheme(context: Context): WidgetConfig {
        val current = loadConfig(context)
        val values = BlossomPalette.entries
        val next = values[(current.palette.ordinal + 1) % values.size]
        val updated = current.copy(palette = next)
        saveConfig(context, updated)
        return updated
    }

    fun loadCacheState(context: Context): WidgetCacheState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val signedIn = prefs.getBoolean(KEY_SIGNED_IN, false)
        val rawJson = prefs.getString(KEY_NOTES_JSON, null)
        val notes = if (rawJson.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { adapter.fromJson(rawJson) }.getOrNull().orEmpty()
        }
        val activeIndex = if (notes.isEmpty()) 0 else {
            prefs.getInt(KEY_ACTIVE_INDEX, 0).coerceIn(0, notes.lastIndex)
        }
        return WidgetCacheState(
            signedIn = signedIn,
            notes = notes,
            activeIndex = activeIndex,
            config = loadConfig(context)
        )
    }

    fun selectTreeNotes(notes: List<TreeNote>, limit: Int): List<TreeNote> {
        val pinned = notes.filter { it.pinnedToTree }
        val pool = if (pinned.isNotEmpty()) pinned else notes
        return pool.take(limit)
    }
}
