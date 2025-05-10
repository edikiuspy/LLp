package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SendMessageRequest(
    @SerializedName("topic") val topic: String,
    @SerializedName("content") val content: String,
    @SerializedName("copyTo") val copyTo: String? = "",
    @SerializedName("receivers") val receivers: ReceiversPayload,
    @SerializedName("storageId") val storageId: String? = null,
    @SerializedName("category") val category: String = "normal"
)

data class ReceiversPayload(
    @SerializedName("schoolReceivers") val schoolReceivers: List<SchoolReceiver>
)

data class SchoolReceiver(
    @SerializedName("accountId") val accountId: String
)