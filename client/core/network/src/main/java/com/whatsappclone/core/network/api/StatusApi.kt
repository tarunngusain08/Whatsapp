package com.whatsappclone.core.network.api

import com.whatsappclone.core.network.model.ApiResponse
import com.whatsappclone.core.network.model.dto.CreateStatusRequest
import com.whatsappclone.core.network.model.dto.StatusDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StatusApi {

    @POST("users/statuses")
    suspend fun createStatus(
        @Body request: CreateStatusRequest
    ): Response<ApiResponse<StatusDto>>

    @GET("users/statuses")
    suspend fun getContactStatuses(): Response<ApiResponse<List<StatusDto>>>

    @GET("users/statuses/me")
    suspend fun getMyStatuses(): Response<ApiResponse<List<StatusDto>>>

    @DELETE("users/statuses/{statusId}")
    suspend fun deleteStatus(
        @Path("statusId") statusId: String
    ): Response<ApiResponse<Unit>>

    @POST("users/statuses/{statusId}/view")
    suspend fun viewStatus(
        @Path("statusId") statusId: String
    ): Response<ApiResponse<Unit>>
}
