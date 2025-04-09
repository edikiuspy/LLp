package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SubjectResponse(
    @SerializedName("Subject") val subject: SubjectDto
)


data class SubjectDto(
    @SerializedName("Id") val id: Int,
    @SerializedName("Name") val name: String
)