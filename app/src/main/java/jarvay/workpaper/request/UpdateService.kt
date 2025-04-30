package jarvay.workpaper.request

import jarvay.workpaper.request.response.Release
import jarvay.workpaper.request.response.UpdatingLogItem
import retrofit2.http.GET

interface UpdateService {
    @GET("index.json")
    suspend fun data(): Release

    @GET("updating-log.json")
    suspend fun updatingLog(): List<UpdatingLogItem>
}