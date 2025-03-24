package com.example.llpclient

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
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

    private val gson = Gson()
    private val subjectCache = mutableMapOf<Int, String>()
    private val categoryCache = mutableMapOf<Int, String>()

    suspend fun getGrades(): Result<List<Grade>> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getLoggedInUser() ?:
                return@withContext Result.failure(Exception("User not logged in"))

                val token = user.authToken
                val gradesResponse = fetchGrades(token)

                if (gradesResponse.isSuccess) {
                    val gradesJson = gradesResponse.getOrThrow()
                    val gradesArray = JsonParser.parseString(gradesJson)
                        .asJsonObject
                        .getAsJsonArray("Grades")

                    val grades = coroutineScope {
                        gradesArray.map { gradeElement ->
                            async {
                                val gradeObj = gradeElement.asJsonObject
                                val id = gradeObj.get("Id").asInt
                                val grade = gradeObj.get("Grade").asString
                                val date = gradeObj.get("Date").asString
                                val addDate = gradeObj.get("AddDate").asString
                                val semester = gradeObj.get("Semester").asInt

                                val categoryObj = gradeObj.getAsJsonObject("Category")
                                val categoryId = categoryObj.get("Id").asInt

                                val subjectObj = gradeObj.getAsJsonObject("Subject")
                                val subjectId = subjectObj.get("Id").asInt

                                val hasComments = gradeObj.has("Comments") &&
                                        !gradeObj.get("Comments").isJsonNull &&
                                        gradeObj.getAsJsonArray("Comments").size() > 0

                                val subjectName = getSubjectName(token, subjectId)
                                val categoryName = getCategoryName(token, categoryId)

                                Grade(
                                    id = id,
                                    value = grade,
                                    date = date,
                                    addDate = addDate,
                                    subjectId = subjectId,
                                    subjectName = subjectName,
                                    category = GradeCategory(
                                        id = categoryId,
                                        name = categoryName
                                    ),
                                    hasComments = hasComments,
                                    semester = semester
                                )
                            }
                        }.awaitAll()
                    }

                    Result.success(grades)
                } else {
                    Result.failure(gradesResponse.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                Log.e("GradesRepository", "Error fetching grades", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchGrades(token: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.librus.pl/2.0/Grades")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    Result.success(responseBody)
                } else {
                    Result.failure(IOException("Failed to fetch grades. Response code: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e("GradesRepository", "Error in network call", e)
                Result.failure(e)
            }
        }
    }

    private fun getSubjectName(token: String, subjectId: Int): String {
        return subjectCache.getOrPut(subjectId) {
            try {
                val request = Request.Builder()
                    .url("https://api.librus.pl/2.0/Subjects/$subjectId")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonObject = JsonParser.parseString(responseBody).asJsonObject
                    jsonObject.getAsJsonObject("Subject").get("Name").asString
                } else {
                    "Subject #$subjectId"
                }
            } catch (e: Exception) {
                Log.e("GradesRepository", "Error fetching subject name for ID $subjectId", e)
                "Subject #$subjectId"
            }
        }
    }

    private fun getCategoryName(token: String, categoryId: Int): String {
        return categoryCache.getOrPut(categoryId) {
            try {
                val request = Request.Builder()
                    .url("https://api.librus.pl/2.0/Grades/Categories/$categoryId")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonObject = JsonParser.parseString(responseBody).asJsonObject
                    jsonObject.getAsJsonObject("Category").get("Name").asString
                } else {
                    "Category #$categoryId"
                }
            } catch (e: Exception) {
                Log.e("GradesRepository", "Error fetching category name for ID $categoryId", e)
                "Category #$categoryId"
            }
        }
    }
}