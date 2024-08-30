package jarvay.workpaper.request

import jarvay.workpaper.request.response.Release
import retrofit2.http.GET

interface UpdateService {
    @GET("index.json")
    suspend fun data(): Release
}