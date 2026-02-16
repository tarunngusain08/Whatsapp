package com.whatsappclone.core.network.api

import com.whatsappclone.core.network.model.ApiResponse
import com.whatsappclone.core.network.model.dto.DeviceTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApi {

    @POST("notifications/devices")
    suspend fun registerDevice(
        @Body request: DeviceTokenRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("notifications/devices/{token}")
    suspend fun unregisterDevice(
        @Path("token") token: String
    ): Response<ApiResponse<Unit>>
}
