package jarvay.workpaper.data

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun strListToString(data: List<String>?): String = gson.toJson(data)

    @TypeConverter
    fun stringToStrList(data: String) = gson.fromJson(data, Array<String>::class.java).toList()

    @TypeConverter
    fun intListToString(data: List<Int>?): String = gson.toJson(data)

    @TypeConverter
    fun stringToIntList(data: String) = gson.fromJson(data, Array<Int>::class.java).toList()
}
