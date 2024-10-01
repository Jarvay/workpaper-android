package jarvay.workpaper.request

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import jarvay.workpaper.R
import jarvay.workpaper.others.Global
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class RequestInterceptor(val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (connectivityManager.activeNetwork == null) {
            Global.apply {
                workpaperAppScope?.launch {
                    snackbarHostState?.showSnackbar(context.getString(R.string.tips_network_not_available))
                }
            }
        }

        try {
            return chain.proceed(chain.request())
        } catch (e: Exception) {
            Log.e("RetrofitClient request error", e.message ?: e.toString())
            throw e;
        }
    }
}

@Singleton
class RetrofitClient(context: Context) {
    companion object {
        const val BASE_URL =
            "https://ghp.ci/https://raw.githubusercontent.com/Jarvay/workpaper-android-release/main/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(RequestInterceptor(context))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    val updateService: UpdateService = retrofit.create(UpdateService::class.java)
}