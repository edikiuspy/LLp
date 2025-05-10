package com.example.llpclient.data.remote

import com.example.llpclient.data.remote.dto.CommentApiResponse
import com.example.llpclient.data.remote.dto.GradeCategoryResponse
import com.example.llpclient.data.remote.dto.GradesResponse
import com.example.llpclient.data.remote.dto.MessagesResponse
import com.example.llpclient.data.remote.dto.ReceiverGroupResponse
import com.example.llpclient.data.remote.dto.RecipientTypesResponse
import com.example.llpclient.data.remote.dto.SendMessageRequest
import com.example.llpclient.data.remote.dto.StudentSubjectsResponse
import com.example.llpclient.data.remote.dto.SubjectResponse
import com.example.llpclient.data.remote.dto.TimetableResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

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

    @GET("2.0/Grades")
    suspend fun getGrades(): Response<GradesResponse>

    @GET("2.0/Subjects/{id}")
    suspend fun getSubjectDetails(
        @Path("id") subjectId: Int
    ): Response<SubjectResponse>

    @GET("2.0/Grades/Categories/{id}")
    suspend fun getCategoryDetails(
        @Path("id") categoryId: Int
    ): Response<GradeCategoryResponse>
    @GET("2.0/Grades/Comments/{id}")
    suspend fun getCommentDetails(
        @Path("id") commentId: Int
    ): Response<CommentApiResponse>

    @GET("api/inbox/messages")
    suspend fun getMessages(): Response<MessagesResponse>
    @GET("wiadomosci3")
    suspend fun visitMessages(): ResponseBody
    @GET("api/receivers/types")
    suspend fun getRecipientTypes(): Response<RecipientTypesResponse>

    @GET("api/receivers/groups/school-employees")
    suspend fun getReceiversForType(@Query("receiverType") receiverType: String): Response<ReceiverGroupResponse>
    @GET("api/receivers/student-subjects")
    suspend fun getStudentSubjectTeachers(): Response<StudentSubjectsResponse>
    @POST("api/messages")
    suspend fun sendMessage(@Body messageRequest: SendMessageRequest): Response<ResponseBody>
    @GET("2.0/Timetables")
    suspend fun getTimetable(): Response<TimetableResponse>
}