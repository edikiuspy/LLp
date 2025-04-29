package com.example.llpclient.data.local
import android.util.Log
import com.example.llpclient.view.model.Grade
import com.example.llpclient.view.model.GradeCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class GradesRepository @Inject constructor(
    authManager: AuthManager

) {
    private val apiService = authManager.apiService
    private val subjectCache = mutableMapOf<Int, String>()
    private val categoryCache = mutableMapOf<Int, String>()
    private val commentCache = mutableMapOf<Int, String>()
    suspend fun getGrades(): Result<List<Grade>> {
        return withContext(Dispatchers.IO) {
            try {
                val gradesResponse = apiService.getGrades()

                if (gradesResponse.isSuccessful && gradesResponse.body() != null) {
                    val apiGrades = gradesResponse.body()!!.grades


                    val grades = coroutineScope {
                        apiGrades.map { apiGrade ->
                            async {
                                val subjectName = getSubjectName(apiGrade.subject.id)
                                val categoryName = getCategoryName(apiGrade.category.id)
                                val commentText = mutableListOf<String>()
                                apiGrade.comments?.forEach { comment ->
                                    commentText.add(getCommentText(comment.id).toString())
                                }
                                val hasComments = apiGrade.comments != null

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
                                    comments = commentText.joinToString(),
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
    private suspend fun getSubjectName(subjectId: Int): String {
        subjectCache[subjectId]?.let { return it }

        return try {
            val response = apiService.getSubjectDetails(subjectId)
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


    private suspend fun getCategoryName(categoryId: Int): String {
        categoryCache[categoryId]?.let { return it }

        return try {
            val response = apiService.getCategoryDetails(categoryId)
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

    private suspend fun getCommentText(commentId: Int): String? {
        commentCache[commentId]?.let {
            Log.d("GradesRepository", "Cache hit for comment ID $commentId")
            return it
        }
        Log.d("GradesRepository", "Cache miss for comment ID $commentId, fetching...")

        return try {
            val response = apiService.getCommentDetails(commentId)

            if (response.isSuccessful && response.body() != null) {
                val commentDto = response.body()!!.comment
                val commentText = commentDto.text


                Log.d("GradesRepository", "Fetched comment text for ID $commentId: '$commentText'")
                commentCache[commentId] = commentText
                commentText
            } else {
                Log.w(
                    "GradesRepository",
                    "Failed to fetch comment details for ID $commentId. Code: ${response.code()}, Message: ${response.message()}"
                )
                null
            }
        } catch (e: Exception) {
            Log.e("GradesRepository", "Error fetching comment details for ID $commentId", e)
            null
        }
    }
}