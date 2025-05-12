package com.example.llpclient.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TimetableApiResponseDto(
    @SerializedName("Timetable")
    val timetable: Map<String, List<List<LessonDto>>>,
    @SerializedName("Pages")
    val pages: PagesDto,
    @SerializedName("Resources")
    val resources: Map<String, ResourceUrlDto>,
    @SerializedName("Url")
    val url: String
)

data class LessonDto(
    @SerializedName("Lesson")
    val lesson: IdUrlLinkDto,
    @SerializedName("Classroom")
    val classroom: IdUrlLinkDto? = null,
    @SerializedName("DateFrom")
    val dateFrom: String,
    @SerializedName("DateTo")
    val dateTo: String,
    @SerializedName("LessonNo")
    val lessonNo: String,
    @SerializedName("TimetableEntry")
    val timetableEntry: IdUrlLinkDto,
    @SerializedName("DayNo")
    val dayNo: String,
    @SerializedName("Subject")
    val subject: SubjectDetailsDto,
    @SerializedName("Teacher")
    val teacher: TeacherDetailsDto,
    @SerializedName("Class")
    val classInfo: IdUrlLinkDto? = null,
    @SerializedName("IsSubstitutionClass")
    val isSubstitutionClass: Boolean,
    @SerializedName("IsCanceled")
    val isCanceled: Boolean,
    @SerializedName("SubstitutionNote")
    val substitutionNote: String? = null,
    @SerializedName("HourFrom")
    val hourFrom: String,
    @SerializedName("HourTo")
    val hourTo: String,
    @SerializedName("VirtualClass")
    val virtualClass: IdUrlLinkDto? = null,
    @SerializedName("VirtualClassName")
    val virtualClassName: String? = null,


    @SerializedName("OrgClassroom")
    val orgClassroom: IdUrlLinkDto? = null,
    @SerializedName("OrgDate")
    val orgDate: String? = null,
    @SerializedName("OrgLessonNo")
    val orgLessonNo: String? = null,
    @SerializedName("OrgLesson")
    val orgLesson: IdUrlLinkDto? = null,
    @SerializedName("OrgSubject")
    val orgSubject: IdUrlLinkDto? = null,
    @SerializedName("OrgTeacher")
    val orgTeacher: IdUrlLinkDto? = null,
    @SerializedName("OrgHourFrom")
    val orgHourFrom: String? = null,
    @SerializedName("OrgHourTo")
    val orgHourTo: String? = null,
    @SerializedName("SubstitutionClassUrl")
    val substitutionClassUrl: String? = null
)

data class IdUrlLinkDto(
    @SerializedName("Id")
    val id: String,
    @SerializedName("Url")
    val url: String
)

data class SubjectDetailsDto(
    @SerializedName("Id")
    val id: String,
    @SerializedName("Name")
    val name: String,
    @SerializedName("Short")
    val short: String,
    @SerializedName("Url")
    val url: String
)

data class TeacherDetailsDto(
    @SerializedName("Id")
    val id: String,
    @SerializedName("FirstName")
    val firstName: String,
    @SerializedName("LastName")
    val lastName: String,
    @SerializedName("Url")
    val url: String
)

data class PagesDto(
    @SerializedName("Next")
    val next: String? = null,
    @SerializedName("Prev")
    val prev: String? = null
)

data class ResourceUrlDto(
    @SerializedName("Url")
    val url: String
)