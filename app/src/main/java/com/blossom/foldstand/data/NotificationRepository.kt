package com.blossom.foldstand.data

import android.content.Context
import androidx.core.content.edit
import com.blossom.foldstand.domain.NotificationItem
import org.json.JSONArray
import org.json.JSONObject

object NotificationRepository {
    private const val PREFS = "standby_notifications"
    private const val KEY_ITEMS = "items"

    fun read(context: Context): List<NotificationItem> {
        val array = runCatching {
            JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]"))
        }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                NotificationItem(
                    appName = item.optString("appName"),
                    title = item.optString("title"),
                    text = item.optString("text"),
                    postedAtMillis = item.optLong("postedAt"),
                )
            }.getOrNull()
        }
    }

    fun append(context: Context, item: NotificationItem) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = runCatching { JSONArray(prefs.getString(KEY_ITEMS, "[]")) }.getOrDefault(JSONArray())
        val next = JSONArray()
        next.put(
            JSONObject().apply {
                put("appName", item.appName)
                put("title", item.title)
                put("text", item.text)
                put("postedAt", item.postedAtMillis)
            },
        )
        for (index in 0 until minOf(old.length(), 11)) next.put(old.getJSONObject(index))
        prefs.edit { putString(KEY_ITEMS, next.toString()) }
    }
}
