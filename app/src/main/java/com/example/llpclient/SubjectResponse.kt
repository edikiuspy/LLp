package com.example.llpclient

import com.google.gson.annotations.SerializedName

data class SubjectResponse(
    @SerializedName("Subject") val subject: SubjectDto
)

