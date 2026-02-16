package com.whatsappclone.core.network.api

import com.whatsappclone.core.network.model.ApiResponse
import com.whatsappclone.core.network.model.dto.MediaUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface MediaApi {

    @Multipart
    @POST("media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody
    ): Response<ApiResponse<MediaUploadResponse>>

    @GET("media/{mediaId}")
    suspend fun getMediaMetadata(
        @Path("mediaId") mediaId: String
    ): Response<ApiResponse<MediaUploadResponse>>
}
