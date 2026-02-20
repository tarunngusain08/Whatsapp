package com.whatsappclone.core.network.api

import com.whatsappclone.core.network.model.ApiResponse
import com.whatsappclone.core.network.model.PaginatedData
import com.whatsappclone.core.network.model.dto.MarkReadRequest
import com.whatsappclone.core.network.model.dto.MessageDto
import com.whatsappclone.core.network.model.dto.ReactRequest
import com.whatsappclone.core.network.model.dto.ReceiptDto
import com.whatsappclone.core.network.model.dto.SendMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApi {

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<PaginatedData<MessageDto>>>

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body request: SendMessageRequest
    ): Response<ApiResponse<MessageDto>>

    @DELETE("chats/{chatId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Unit>>

    @PUT("chats/{chatId}/messages/{messageId}/star")
    suspend fun starMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Unit>>

    @DELETE("chats/{chatId}/messages/{messageId}/star")
    suspend fun unstarMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Unit>>

    @POST("chats/{chatId}/messages/read")
    suspend fun markRead(
        @Path("chatId") chatId: String,
        @Body request: MarkReadRequest
    ): Response<ApiResponse<Unit>>

    @POST("chats/{chatId}/messages/{messageId}/react")
    suspend fun reactToMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String,
        @Body request: ReactRequest
    ): Response<Unit>

    @DELETE("chats/{chatId}/messages/{messageId}/react")
    suspend fun removeReaction(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<Unit>

    @GET("messages/{messageId}/receipts")
    suspend fun getMessageReceipts(
        @Path("messageId") messageId: String
    ): Response<ApiResponse<List<ReceiptDto>>>
}
