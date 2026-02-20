package com.whatsappclone.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (_: Exception) {
            // Backward compatibility: handle legacy comma-separated values
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
