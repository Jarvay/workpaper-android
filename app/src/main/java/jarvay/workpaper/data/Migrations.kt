package jarvay.workpaper.data

import android.util.Log
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson

val Migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `wallpapers` (`id` INTEGER NOT NULL, `albumId` INTEGER NOT NULL, `contentUri` TEXT NOT NULL,
            PRIMARY KEY(`id`))
        """.trimIndent()
        )

        val cursor = db.query("SELECT * FROM albums")
        val gson = Gson()

        val wallpapers: MutableList<String> = mutableListOf()
        while (cursor.moveToNext()) {
            val albumId = cursor.getLongOrNull(cursor.getColumnIndex("id"))
            val wallpaperUrisString = cursor.getStringOrNull(cursor.getColumnIndex("wallpaperUris"))
            val wallpaperUris =
                gson.fromJson(wallpaperUrisString, Array<String>::class.java).toList()
            wallpaperUris.forEach {
                wallpapers.add("($albumId, '$it')")
            }
        }
        val values = wallpapers.joinToString(separator = ",")
        Log.d(javaClass.simpleName, "INSERT INTO wallpapers(albumId, contentUri) VALUES $values;")
        db.execSQL("INSERT INTO wallpapers(`albumId`, `contentUri`) VALUES $values;")

        db.execSQL("ALTER TABLE albums DROP COLUMN wallpaperUris")
    }
}