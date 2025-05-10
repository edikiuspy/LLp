package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReceiverGroupResponse(
    @SerializedName("receivers") val receivers: List<Receiver>?
)