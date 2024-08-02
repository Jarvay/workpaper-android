package jarvay.workpaper.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jarvay.workpaper.data.album.AlbumDao
import jarvay.workpaper.data.day.DayDao
import jarvay.workpaper.data.day.RuleDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppDatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideDayDao(appDatabase: AppDatabase): DayDao {
        return appDatabase.dayDao()
    }

    @Provides
    fun provideRuleDao(appDatabase: AppDatabase): RuleDao {
        return appDatabase.ruleDao()
    }

    @Provides
    fun provideAlbumDao(appDatabase: AppDatabase): AlbumDao {
        return appDatabase.albumDao()
    }

}