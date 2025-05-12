package com.example.llpclient.data.local

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import com.example.llpclient.data.remote.LibrusApiService
import com.example.llpclient.data.remote.dto.LessonDto
import com.example.llpclient.view.model.TimetablePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class TimetableRepository @Inject constructor(
    authManager: AuthManager
) {
    private val apiService: LibrusApiService = authManager.apiService


    private val subjectColorMap = mutableMapOf<String, Color>()
    private val colorPalette = listOf(
        Color(0xFFF57C00),
        Color(0xFF388E3C),
        Color(0xFF1E88E5),
        Color(0xFFD81B60),
        Color(0xFF00897B),
        Color(0xFF5E35B1),
        Color(0xFFE53935),
        Color(0xFF546E7A),
        Color(0xFF6D4C41),
        Color(0xFFC0CA33),
        Color(0xFF039BE5),
        Color(0xFF43A047)
    )
    private var nextColorIndex = 0

    private fun getColorForSubject(subjectName: String): Color {
        return subjectColorMap.getOrPut(subjectName) {
            val color = colorPalette[nextColorIndex % colorPalette.size]
            nextColorIndex++
            color
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getTimetableForWeek(weekStartDate: String): Result<Map<String, List<TimetablePeriod?>>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TimetableRepository", "Fetching timetable for week starting: $weekStartDate")
                val timetableResponse = apiService.getTimetable(weekStartDate)

                if (timetableResponse.isSuccessful) {
                    val apiTimetableBody = timetableResponse.body()
                    if (apiTimetableBody != null) {
                        val sourceTimetable: Map<String, List<List<LessonDto>>> = apiTimetableBody.timetable
                        val processedTimetable = coroutineScope {
                            sourceTimetable.mapValues { (_, dailyScheduleSlots) ->
                                dailyScheduleSlots.map { lessonSlotList ->
                                    val lessonDto = lessonSlotList.firstOrNull()
                                    lessonDto?.let { mapLessonDtoToTimetablePeriod(it) }
                                }
                            }
                        }
                        Result.success(processedTimetable)
                    } else {
                        val errorMsg = "Timetable response body is null for week $weekStartDate"
                        Log.e("TimetableRepository", errorMsg)
                        Result.failure(IOException(errorMsg))
                    }
                } else {
                    val errorMsg = "Failed to fetch timetable for week $weekStartDate. Code: ${timetableResponse.code()}, Message: ${timetableResponse.message()}"
                    Log.e("TimetableRepository", errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("TimetableRepository", "Error fetching timetable for week $weekStartDate: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private fun mapLessonDtoToTimetablePeriod(dto: LessonDto): TimetablePeriod {
        val subjectName = dto.subject.name
        val color = getColorForSubject(subjectName)

        return TimetablePeriod(
            id = dto.timetableEntry.id,
            subjectName = subjectName,
            hourFrom = dto.hourFrom,
            hourTo = dto.hourTo,
            isCancelled = dto.isCanceled,
            isSubstitution = dto.isSubstitutionClass,
            substitutionNote = dto.substitutionNote,
            teacherName = "${dto.teacher.firstName} ${dto.teacher.lastName}",
            classroomInfo = dto.classroom?.id ?: dto.orgClassroom?.id,
            color = color
        )
    }
}