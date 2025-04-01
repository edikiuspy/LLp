package com.example.llpclient

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class GradesRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)

            .build()
    }


    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.librus.pl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    private val apiService: LibrusApiService by lazy {
        retrofit.create(LibrusApiService::class.java)
    }


    private val subjectCache = mutableMapOf<Int, String>()
    private val categoryCache = mutableMapOf<Int, String>()

    suspend fun getGrades(): Result<List<Grade>> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getLoggedInUser() ?:
                return@withContext Result.failure(Exception("User not logged in"))

                val token = "Bearer ${user.authToken}"


                val gradesResponse = apiService.getGrades(token)

                if (gradesResponse.isSuccessful && gradesResponse.body() != null) {
                    val apiGrades = gradesResponse.body()!!.grades


                    val grades = coroutineScope {
                        apiGrades.map { apiGrade ->
                            async {
                                val subjectName = getSubjectName(token, apiGrade.subject.id)
                                val categoryName = getCategoryName(token, apiGrade.category.id)

                                val hasComments = apiGrade.comments!=null

                                Grade(
                                    id = apiGrade.id,
                                    value = apiGrade.grade,
                                    date = apiGrade.date,
                                    addDate = apiGrade.addDate,
                                    subjectId = apiGrade.subject.id,
                                    subjectName = subjectName,
                                    category = GradeCategory(
                                        id = apiGrade.category.id,
                                        name = categoryName
                                    ),
                                    hasComments = hasComments,
                                    semester = apiGrade.semester
                                )
                            }
                        }.awaitAll()
                    }
                    Result.success(grades)
                } else {

                    val errorMsg = "Failed to fetch grades. Code: ${gradesResponse.code()}, Message: ${gradesResponse.message()}"
                    Log.e("GradesRepository", errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {

                Log.e("GradesRepository", "Error fetching grades", e)
                Result.failure(e)
            }
        }
    }


    private suspend fun getSubjectName(token: String, subjectId: Int): String {

        subjectCache[subjectId]?.let { return it }


        return try {
            val response = apiService.getSubjectDetails(token, subjectId)
            if (response.isSuccessful && response.body() != null) {
                val name = response.body()!!.subject.name
                subjectCache[subjectId] = name
                name
            } else {
                Log.w("GradesRepository", "Failed to fetch subject name for ID $subjectId. Code: ${response.code()}")
                "Subject #$subjectId"
            }
        } catch (e: Exception) {
            Log.e("GradesRepository", "Error fetching subject name for ID $subjectId", e)
            "Subject #$subjectId"
        }
    }


    private suspend fun getCategoryName(token: String, categoryId: Int): String {

        categoryCache[categoryId]?.let { return it }


        return try {
            val response = apiService.getCategoryDetails(token, categoryId)
            if (response.isSuccessful && response.body() != null) {
                val name = response.body()!!.category.name
                categoryCache[categoryId] = name
                name
            } else {
                Log.w("GradesRepository", "Failed to fetch category name for ID $categoryId. Code: ${response.code()}")
                "Category #$categoryId"
            }
        } catch (e: Exception) {
            Log.e("GradesRepository", "Error fetching category name for ID $categoryId", e)
            "Category #$categoryId"
        }
    }




}