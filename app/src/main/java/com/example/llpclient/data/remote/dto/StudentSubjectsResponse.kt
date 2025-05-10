package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StudentSubjectsResponse(
    @SerializedName("data") val data: List<StudentSubject>?
)