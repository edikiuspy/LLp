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
    suspend fun getUserData(@Header("Authorization") authToken: String): Response<LoginResponse>
    @GET("2.0/Grades")
    suspend fun getGrades(@Header("Authorization") token: String): Response<GradesResponse>


    @GET("2.0/Subjects/{id}")
    suspend fun getSubjectDetails(
        @Header("Authorization") token: String,
        @Path("id") subjectId: Int
    ): Response<SubjectResponse>

    @GET("2.0/Grades/Categories/{id}")
    suspend fun getCategoryDetails(
        @Header("Authorization") token: String,
        @Path("id") categoryId: Int
    ): Response<GradeCategoryResponse>
}