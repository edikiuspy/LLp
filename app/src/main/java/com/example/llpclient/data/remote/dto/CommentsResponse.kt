package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CommentApiResponse(
    @SerializedName("Comment") val comment: CommentDetailDto,
)

data class CommentDetailDto(
    @SerializedName("Id") val id: Int,
    @SerializedName("Text") val text: String,
)