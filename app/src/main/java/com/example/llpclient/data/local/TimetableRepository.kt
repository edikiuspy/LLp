package com.example.llpclient.data.local

import android.util.Log
import com.example.llpclient.data.remote.LibrusApiService
import com.example.llpclient.data.remote.dto.TimetablePeriodDto
import com.example.llpclient.view.model.TimetablePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import javax.inject.Inject

class TimetableRepository @Inject constructor(
    authManager: AuthManager
) {
    private val apiService: LibrusApiService = authManager.apiService

    suspend fun getTimetable(): Result<Map<String, List<TimetablePeriod?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val timetableResponse = apiService.getTimetable()

                if (timetableResponse.isSuccessful) {
                    val apiTimetableBody = timetableResponse.body()
                    if (apiTimetableBody != null) {
                        val apiTimetable: Map<String, List<List<TimetablePeriodDto>>> = apiTimetableBody.timetable

                        val timetable: Map<String, List<TimetablePeriod?>> = apiTimetable.mapValues { (_, listOfLists) ->
                            listOfLists.map { innerList ->
                                val elem = innerList.firstOrNull()
                                if (elem != null) {
                                    TimetablePeriod(subject = elem.subject.name)
                                } else {
                                    null
                                }

                            }
                        }

                        Result.success(timetable)
                    } else {
                        Result.failure(IOException("Timetable response body is null"))
                    }
                } else {
                    Result.failure(IOException("Network request failed with code: ${timetableResponse.code()}"))
                }
            } catch (e: Exception) {
                Log.e("TimetableRepository", "Error fetching timetable", e)
                Result.failure(e) // Return the actual exception
            }
        }
    }
}
