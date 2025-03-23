package com.example.llpclient.data.remote

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: Any?
)
