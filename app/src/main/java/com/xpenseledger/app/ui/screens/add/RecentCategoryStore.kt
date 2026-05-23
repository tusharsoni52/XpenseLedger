package com.xpenseledger.app.ui.screens.add

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the last [MAX_RECENT] category IDs (Long) the user selected when
 * adding an expense.  Written as plain SharedPreferences because category IDs
 * are non-sensitive integers — no PII involved.
 *
 * Thread-safe: SharedPreferences.apply() is used (async commit).
 */
@Singleton
class RecentCategoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME  = "recent_categories"
        private const val KEY_RECENTS = "recent_ids"
        const val MAX_RECENT = 3
        private const val SEPARATOR = ","
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns up to [MAX_RECENT] most-recently-used category IDs, newest first. */
    fun getRecent(): List<Long> {
        val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return raw.split(SEPARATOR)
            .mapNotNull { it.trim().toLongOrNull() }
            .take(MAX_RECENT)
    }

    /**
     * Saves [categoryId] as the most-recently used.
     * Removes any prior occurrence so there are no duplicates;
     * trims to [MAX_RECENT] entries.
     */
    fun record(categoryId: Long) {
        val current = getRecent().toMutableList()
        current.remove(categoryId)
        current.add(0, categoryId)
        val trimmed = current.take(MAX_RECENT)
        prefs.edit { putString(KEY_RECENTS, trimmed.joinToString(SEPARATOR)) }
    }
}

