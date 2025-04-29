package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TimetableResponse(
    // List of List ( kill me )
    @SerializedName("Timetable") var timetable: Map<String, List<MutableList<TimetablePeriodDto>>>
)

data class TimetablePeriodDto(
    @SerializedName("Subject") var subject: TimetableSubject
)

data class TimetableSubject(
    @SerializedName("Name") var name: String
)