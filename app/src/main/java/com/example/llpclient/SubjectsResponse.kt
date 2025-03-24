package com.example.llpclient

import com.google.gson.annotations.SerializedName

data class SubjectsResponse(
    @SerializedName("Subjects") val subjects: List<SubjectDto>
)

data class SubjectDto(
    @SerializedName("Id") val id: Int,
    @SerializedName("Name") val name: String
)