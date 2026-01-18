package com.kotlin.flashlearn.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

/**
 * Room type converters for complex types.
 */
object Converters {

    @TypeConverter
    @JvmStatic
    fun fromStringList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return "[]"
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    @JvmStatic
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val jsonArray = JSONArray(value)
            List(jsonArray.length()) { index -> jsonArray.optString(index) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
