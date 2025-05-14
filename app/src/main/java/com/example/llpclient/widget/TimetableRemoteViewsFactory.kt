package com.example.llpclient.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RequiresApi
import com.example.llpclient.R
import com.example.llpclient.data.local.TimetableRepository
import com.example.llpclient.view.model.TimetablePeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class TimetableRemoteViewsFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timetableRepository: TimetableRepository
) : RemoteViewsService.RemoteViewsFactory {

    private var lessons: List<TimetablePeriod> = emptyList()
    private val logTag = "TimetableWidgetFactory"


    override fun onCreate() {
        onDataSetChanged()
        Log.d(logTag, "onCreate")
    }

    override fun onDataSetChanged() {
        Log.d(logTag, "onDataSetChanged called. Fetching data...")

        val currentDate = LocalDate.now()
        val currentDayString = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val mondayOfWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val mondayOfWeekString = mondayOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)

        Log.d(logTag, "Current date: $currentDayString, Monday of this week: $mondayOfWeekString")

        val identityToken = Binder.clearCallingIdentity()
        try {
            lessons = runBlocking(Dispatchers.IO) {
                try {
                    Log.d(logTag, "Fetching timetable for week starting: $mondayOfWeekString")
                    val result = timetableRepository.getTimetableForWeek(mondayOfWeekString)

                    if (result.isSuccess) {
                        val weekData = result.getOrNull()
                        val todaysLessons = weekData?.get(currentDayString)?.filterNotNull() ?: emptyList()
                        Log.d(logTag, "Successfully fetched ${todaysLessons.size} lessons for $currentDayString (from week $mondayOfWeekString)")
                        todaysLessons
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e(logTag, "Repository failed to fetch timetable for week $mondayOfWeekString. Message: $errorMsg")
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e(logTag, "Exception during timetable fetch for week $mondayOfWeekString: ${e.message}", e)
                    emptyList()
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
        Log.d(logTag, "onDataSetChanged completed. Lesson count for $currentDayString: ${lessons.size}")
    }

    override fun onDestroy() {
        Log.d(logTag, "onDestroy")
        lessons = emptyList()
    }

    override fun getCount(): Int = lessons.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position == AdapterView.INVALID_POSITION || position < 0 || position >= lessons.size) {
            Log.w(logTag, "Invalid position requested: $position, lessons size: ${lessons.size}")
            return null
        }

        val lesson = lessons[position]
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_timetable_item_layout)

        remoteViews.setTextViewText(R.id.widget_item_subject, lesson.subjectName)
        remoteViews.setTextViewText(R.id.widget_item_time, "${lesson.hourFrom} - ${lesson.hourTo}")

        try {
            val androidColor = Color.valueOf(lesson.color.red, lesson.color.green, lesson.color.blue, lesson.color.alpha).toArgb()
            remoteViews.setInt(R.id.widget_item_root, "setBackgroundColor", androidColor)
        } catch (e: Exception) {
            Log.e(logTag, "Error setting color for lesson: ${lesson.subjectName}", e)
        }

        var details = ""
        val cancelledText = context.getString(R.string.widget_status_cancelled)
        val substitutionText = context.getString(R.string.widget_status_substitution)

        if (lesson.isCancelled) {
            details += cancelledText
            lesson.substitutionNote?.let { if (it.isNotBlank()) details += ": $it" }
        } else if (lesson.isSubstitution) {
            details += substitutionText
            lesson.substitutionNote?.let { if (it.isNotBlank()) details += ": $it" }
        }

        if (details.isNotEmpty()) {
            remoteViews.setTextViewText(R.id.widget_item_details, details)
            remoteViews.setViewVisibility(R.id.widget_item_details, android.view.View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.widget_item_details, android.view.View.GONE)
        }

        val fillInIntent = Intent()
        val extras = Bundle()
        fillInIntent.putExtras(extras)
        remoteViews.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_item_loading_layout)
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position >= 0 && position < lessons.size) {
            lessons[position].id.hashCode().toLong()
        } else {
            AdapterView.INVALID_ROW_ID
        }
    }

    override fun hasStableIds(): Boolean = true
}