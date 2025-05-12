package com.example.llpclient.view.model

import androidx.compose.ui.graphics.Color

data class TimetablePeriod(
    val id: String,
    val subjectName: String,
    val hourFrom: String,
    val hourTo: String,
    val isCancelled: Boolean = false,
    val isSubstitution: Boolean = false,
    val substitutionNote: String? = null,
    val teacherName: String = "",
    val classroomInfo: String? = null,
    val color: Color
)