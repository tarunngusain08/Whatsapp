package com.whatsappclone.core.database.converter

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONException

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { list ->
            JSONArray(list).toString()
        }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        return try {
            val jsonArray = JSONArray(value)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (_: JSONException) {
            // Backward compatibility: handle legacy comma-separated values
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
