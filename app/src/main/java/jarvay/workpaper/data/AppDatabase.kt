package jarvay.workpaper.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.album.AlbumDao
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleAlbumRelation
import jarvay.workpaper.data.rule.RuleAlbumRelationDao
import jarvay.workpaper.data.rule.RuleDao
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.data.wallpaper.WallpaperDao

@Database(
    entities = [
        Rule::class,
        Album::class,
        RuleAlbumRelation::class,
        Wallpaper::class
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao

    abstract fun albumDao(): AlbumDao

    abstract fun ruleAlbumRelationDao(): RuleAlbumRelationDao

    abstract fun wallpaperDao(): WallpaperDao

    companion object {
        private const val DB_FILE = "workpaper.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: getRoomDatabase(getDatabaseBuilder(context)).also { instance = it }
            }
        }

        private fun getDatabaseBuilder(ctx: Context): Builder<AppDatabase> {
            val appContext = ctx.applicationContext
            val dbFile = appContext.getDatabasePath(DB_FILE)
            return Room.databaseBuilder(
                context = appContext,
                klass = AppDatabase::class.java,
                name = dbFile.absolutePath
            )
        }

        private fun getRoomDatabase(builder: Builder<AppDatabase>): AppDatabase {
            return builder
                .allowMainThreadQueries()
                .addMigrations(
                    Migration_1_2
                )
                .build()
        }
    }
}