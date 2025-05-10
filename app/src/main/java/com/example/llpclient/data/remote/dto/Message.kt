package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("data") val data: Message
)

data class Message(
    @SerializedName("messageId") val messageId: Int,
    @SerializedName("Message") val message: String,

)
