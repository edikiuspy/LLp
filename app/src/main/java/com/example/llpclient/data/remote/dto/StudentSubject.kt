package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StudentSubject(
    @SerializedName("teacherIdentifier") val teacherIdentifier: String?,
    @SerializedName("subject") val subject: String?
)