package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GradesResponse(
    @SerializedName("Grades") val grades: List<GradeDto>
)

data class GradeDto(
    @SerializedName("Id") val id: Int,
    @SerializedName("Grade") val grade: String,
    @SerializedName("Date") val date: String,
    @SerializedName("AddDate") val addDate: String,
    @SerializedName("Subject") val subject: ResourceLink,
    @SerializedName("Category") val category: ResourceLink,
    @SerializedName("Comments") val comments: List<ResourceLink>?,
    @SerializedName("Semester") val semester: Int,
    @SerializedName("IsConstituent") val isConstituent: Boolean,
    @SerializedName("IsFinal") val isFinal: Boolean,
    @SerializedName("IsSemester") val isSemester: Boolean
)

data class ResourceLink(
    @SerializedName("Id") val id: Int,
    @SerializedName("Url") val url: String
)