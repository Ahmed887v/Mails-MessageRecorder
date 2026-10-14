package com.newrecorder.app.data.local

import androidx.room.TypeConverter
import com.newrecorder.app.data.local.entity.SourceCategory

class Converters {
    @TypeConverter
    fun fromSourceCategory(value: SourceCategory): String = value.name

    @TypeConverter
    fun toSourceCategory(value: String): SourceCategory =
        runCatching { SourceCategory.valueOf(value) }.getOrDefault(SourceCategory.OTHER)
}
