package com.whatsappclone.core.network.api

import com.whatsappclone.core.network.model.ApiResponse
import com.whatsappclone.core.network.model.PaginatedData
import com.whatsappclone.core.network.model.dto.AddParticipantsRequest
import com.whatsappclone.core.network.model.dto.ChatDto
import com.whatsappclone.core.network.model.dto.CreateChatRequest
import com.whatsappclone.core.network.model.dto.DisappearingTimerRequest
import com.whatsappclone.core.network.model.dto.MuteChatRequest
import com.whatsappclone.core.network.model.dto.UpdateRoleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @GET("chats")
    suspend fun getChats(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<PaginatedData<ChatDto>>>

    @POST("chats")
    suspend fun createChat(
        @Body request: CreateChatRequest
    ): Response<ApiResponse<ChatDto>>

    @GET("chats/{chatId}")
    suspend fun getChatDetail(
        @Path("chatId") chatId: String
    ): Response<ApiResponse<ChatDto>>

    @PATCH("chats/{chatId}")
    suspend fun updateChat(
        @Path("chatId") chatId: String,
        @Body request: CreateChatRequest
    ): Response<ApiResponse<ChatDto>>

    @PUT("chats/{chatId}/mute")
    suspend fun muteChat(
        @Path("chatId") chatId: String,
        @Body request: MuteChatRequest
    ): Response<ApiResponse<Unit>>

    @POST("chats/{chatId}/participants")
    suspend fun addParticipants(
        @Path("chatId") chatId: String,
        @Body request: AddParticipantsRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("chats/{chatId}/participants/{userId}")
    suspend fun removeParticipant(
        @Path("chatId") chatId: String,
        @Path("userId") userId: String
    ): Response<ApiResponse<Unit>>

    @PATCH("chats/{chatId}/participants/{userId}/role")
    suspend fun updateRole(
        @Path("chatId") chatId: String,
        @Path("userId") userId: String,
        @Body request: UpdateRoleRequest
    ): Response<ApiResponse<Unit>>

    @PUT("chats/{chatId}/disappearing")
    suspend fun setDisappearingTimer(
        @Path("chatId") chatId: String,
        @Body request: DisappearingTimerRequest
    ): Response<ApiResponse<Unit>>
}
