package com.eaters.eaters.interfaces

import com.eaters.eaters.models.FeedbackRequest
import com.eaters.eaters.models.FeedbackResponse
import com.eaters.eaters.models.SendMessageRequest
import com.eaters.eaters.models.SendMessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("api/chat")
    fun sendMessage(@Body request: SendMessageRequest): Call<SendMessageResponse>
    @Multipart
    @POST("api/chat")
    fun sendMessageWithFile(
        @Part("message") message: RequestBody,
        @Part file: MultipartBody.Part
    ): Call<SendMessageResponse>
    @Multipart
    @POST("api/chat")
    fun sendMessageWithFileAndName(
        @Part("message") message: RequestBody,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part
    ): Call<SendMessageResponse>

    @Headers("Content-Type: application/json")
    @POST("api/mobile/feedback")
    fun getFeedback(@Body request: FeedbackRequest): Call<FeedbackResponse>

    @Headers("Content-Type: application/json")
    @POST("api/mobile/detailedfeedback")
    fun getDetailedFeedback(@Body request: FeedbackRequest): Call<FeedbackResponse>

}