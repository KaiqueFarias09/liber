package com.example.liber.data.local

import android.net.Uri
import androidx.room.TypeConverter
import com.example.liber.data.model.AnnotationType

class Converters {
    @TypeConverter
    fun fromUri(value: Uri?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toUri(value: String?): Uri? {
        return value?.let { Uri.parse(it) }
    }

    @TypeConverter
    fun fromAnnotationType(value: AnnotationType): String {
        return value.name
    }

    @TypeConverter
    fun toAnnotationType(value: String): AnnotationType {
        return AnnotationType.valueOf(value)
    }
}
