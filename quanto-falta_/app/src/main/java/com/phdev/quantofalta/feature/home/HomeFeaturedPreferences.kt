package com.phdev.quantofalta.feature.home

import android.content.Context

class HomeFeaturedPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("home_featured_preferences", Context.MODE_PRIVATE)

    fun selectedEventId(): String? = prefs.getString(KEY_EVENT_ID, null)
    fun selectedPage(): Int = prefs.getInt(KEY_PAGE, 0)
    fun selectedType(): String? = prefs.getString(KEY_TYPE, null)

    fun saveSelection(eventId: String, page: Int, type: String) {
        prefs.edit()
            .putString(KEY_EVENT_ID, eventId)
            .putInt(KEY_PAGE, page)
            .putString(KEY_TYPE, type)
            .apply()
    }

    fun clearSelectedEvent() {
        prefs.edit().remove(KEY_EVENT_ID).remove(KEY_PAGE).remove(KEY_TYPE).apply()
    }

    private companion object {
        const val KEY_EVENT_ID = "selected_featured_event_id"
        const val KEY_PAGE = "selected_featured_page"
        const val KEY_TYPE = "selected_featured_type"
    }
}
