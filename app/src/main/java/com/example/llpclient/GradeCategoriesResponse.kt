package com.example.llpclient

import com.google.gson.annotations.SerializedName

data class GradeCategoryResponse(
    @SerializedName("Category") val category: GradeCategoryDto
)

data class GradeCategoryDto(
    @SerializedName("Id") val id: Int,
    @SerializedName("Name") val name: String
)