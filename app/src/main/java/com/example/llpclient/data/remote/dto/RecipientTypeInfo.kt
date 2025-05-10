package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RecipientTypeInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)