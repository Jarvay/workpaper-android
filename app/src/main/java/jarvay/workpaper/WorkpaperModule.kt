package jarvay.workpaper

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class WorkpaperModule {
    @Provides
    fun provideWorkpaper(@ApplicationContext context: Context): Workpaper {
        return Workpaper(context)
    }
}