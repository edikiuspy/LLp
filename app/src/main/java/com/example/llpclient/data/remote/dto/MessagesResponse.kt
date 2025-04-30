package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessagesResponse(
    @SerializedName("data") val data: List<MessageDto>
)

data class MessageDto(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("senderFirstName") val senderFirstName: String,
    @SerializedName("senderLastName") val senderLastName: String,
    @SerializedName("senderName") val senderName: String,
    @SerializedName("topic") val topic: String,
    @SerializedName("content") val content: String,
    @SerializedName("sendDate") val sendDate: String,
    @SerializedName("readDate") val readDate: String?,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("category") val category: String? = null,
    @SerializedName("otherNodeUuid") val otherNodeUuid: String? = null,
    @SerializedName("otherNodeAccountId") val otherNodeAccountId: String? = null,
    @SerializedName("isAnyFileAttached") val isAnyFileAttached: Boolean = false
)
