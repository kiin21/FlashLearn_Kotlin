package com.kotlin.flashlearn.domain.widget

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WidgetJson {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    fun decodeStringList(json: String?): MutableList<String> {
        if (json.isNullOrBlank()) return mutableListOf()
        return runCatching { gson.fromJson<List<String>>(json, listType).toMutableList() }
            .getOrDefault(mutableListOf())
    }

    fun encodeStringList(list: List<String>): String {
        return runCatching { gson.toJson(list) }.getOrDefault("[]")
    }
}