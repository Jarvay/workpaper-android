package jarvay.workpaper.request

import android.content.Context
import android.net.ConnectivityManager
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.EntryPoints
import jarvay.workpaper.EntryPoint
import jarvay.workpaper.R
import jarvay.workpaper.others.Global
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

const val REPO = "Jarvay/workpaper-android-release"

enum class RepoHost(val value: String) {
    GH_FAST("ghfast.top"),
    CDN_JSDELIVR("cdn.jsdelivr.net"),
    FASTLY_JSDELIVR("fastly.jsdelivr.net"),
    GCORE_JSDELIVR("gcore.jsdelivr.net"),
}

val REPO_MIRRORS_MAP = mapOf(
    Pair(
        RepoHost.GH_FAST.value,
        "https://ghfast.top/https://raw.githubusercontent.com/$REPO/main/"
    ),
    Pair(RepoHost.CDN_JSDELIVR.value, "https://cdn.jsdelivr.net/gh/$REPO@latest/"),
    Pair(RepoHost.FASTLY_JSDELIVR.value, "https://fastly.jsdelivr.net/gh/$REPO@latest/"),
    Pair(RepoHost.GCORE_JSDELIVR.value, "https://gcore.jsdelivr.net/gh/$REPO@latest/"),
)

@Singleton
class RequestInterceptor(val context: Context) : Interceptor {
    companion object {
        const val LOG_TAG = "RETROFIT_INTERCEPTOR"
    }

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

        val workpaper =
            EntryPoints.get(context.applicationContext, EntryPoint::class.java).workpaper()
        val mirrorKey = runBlocking {
            workpaper.settingsPreferencesRepository.settingsPreferencesFlow.first().repoMirror
        }
        val mirror =
            (REPO_MIRRORS_MAP[mirrorKey] ?: REPO_MIRRORS_MAP[RepoHost.GH_FAST.value]).toString()
        val url = chain.request().url.toString().replace(RetrofitClient.BASE_URL, mirror)

        LogUtils.i(LOG_TAG, url, chain.request().body)

        try {
            return chain.proceed(
                chain.request().newBuilder().url(
                    url
                ).build()
            )
        } catch (e: Exception) {
            LogUtils.e("RetrofitClient request error", e.message ?: e.toString())
            throw e
        }
    }
}

@Singleton
class RetrofitClient(context: Context) {
    companion object {
        const val BASE_URL =
            "https://$(base_url)$"
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