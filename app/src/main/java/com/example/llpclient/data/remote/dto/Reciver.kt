package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class Receiver(
    @SerializedName("accountId") val accountId: String,
    @SerializedName("label") val label: String,
    @SerializedName("userId") val userId: String
)