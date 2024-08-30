package jarvay.workpaper.request

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class RetrofitClientModule {
    @Provides
    fun provideRetrofitClient(@ApplicationContext context: Context): RetrofitClient {
        return RetrofitClient(context)
    }
}