package com.whatsappclone.core.network.api

import com.whatsappclone.core.network.model.ApiResponse
import com.whatsappclone.core.network.model.dto.ContactSyncRequest
import com.whatsappclone.core.network.model.dto.ContactSyncResponse
import com.whatsappclone.core.network.model.dto.PresenceDto
import com.whatsappclone.core.network.model.dto.UpdateProfileRequest
import com.whatsappclone.core.network.model.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApi {

    @GET("users/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    @PATCH("users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserDto>>

    @GET("users/{userId}")
    suspend fun getUser(
        @Path("userId") userId: String
    ): Response<ApiResponse<UserDto>>

    @GET("users/{userId}/presence")
    suspend fun getPresence(
        @Path("userId") userId: String
    ): Response<ApiResponse<PresenceDto>>

    @POST("users/contacts/sync")
    suspend fun syncContacts(
        @Body request: ContactSyncRequest
    ): Response<ApiResponse<ContactSyncResponse>>

    @POST("users/{userId}/block")
    suspend fun blockUser(
        @Path("userId") userId: String
    ): Response<ApiResponse<Unit>>

    @DELETE("users/{userId}/block")
    suspend fun unblockUser(
        @Path("userId") userId: String
    ): Response<ApiResponse<Unit>>
}
