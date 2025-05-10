package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RecipientTypesResponse(
    @SerializedName("data") val data: RecipientTypesData?
)