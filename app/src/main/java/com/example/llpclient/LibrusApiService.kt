package com.example.llpclient

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface LibrusApiService {
    @GET("OAuth/Authorization")
    suspend fun initializeAuth(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @FormUrlEncoded
    @POST("OAuth/Authorization")
    suspend fun login(
        @QueryMap params: Map<String, String>,
        @Field("action") action: String,
        @Field("login") login: String,
        @Field("pass") pass: String
    ): Response<ResponseBody>

    @GET("OAuth/Authorization/PerformLogin")
    suspend fun performLogin(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @GET("2.0/")
    suspend fun getUserData(@Header("Authorization") authToken: String): Response<com.example.llpclient.data.remote.LoginResponse>
}