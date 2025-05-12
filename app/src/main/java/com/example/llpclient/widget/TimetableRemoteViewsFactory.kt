package com.example.llpclient.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.widget.AdapterView
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RequiresApi
import com.example.llpclient.R
import com.example.llpclient.data.local.TimetableRepository
import com.example.llpclient.view.model.TimetablePeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class TimetableRemoteViewsFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timetableRepository: TimetableRepository
) : RemoteViewsService.RemoteViewsFactory {

    private var lessons: List<TimetablePeriod> = emptyList()

    override fun onCreate() {

    }

    override fun onDataSetChanged() {
        val todayDateString = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val identityToken = Binder.clearCallingIdentity()
        try {
            lessons = runBlocking {
                val result = timetableRepository.getTimetableForWeek(todayDateString)
                if (result.isSuccess) {
                    val weekData = result.getOrNull()
                    weekData?.get(todayDateString)?.filterNotNull() ?: emptyList()
                } else {
                    emptyList()
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        lessons = emptyList()
    }

    override fun getCount(): Int = lessons.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position == AdapterView.INVALID_POSITION || position >= lessons.size) {
            return null
        }
        val lesson = lessons[position]
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_timetable_item_layout)

        remoteViews.setTextViewText(R.id.widget_item_subject, lesson.subjectName)
        remoteViews.setTextViewText(R.id.widget_item_time, "${lesson.hourFrom} - ${lesson.hourTo}")

        val androidColor = Color.valueOf(lesson.color.red, lesson.color.green, lesson.color.blue, lesson.color.alpha).toArgb()
        remoteViews.setInt(R.id.widget_item_root, "setBackgroundColor", androidColor)

        var details = ""
        if (lesson.isCancelled) {
            details += "ODWOŁANE"
            lesson.substitutionNote?.let { if (it.isNotBlank()) details += ": $it" }
        } else if (lesson.isSubstitution) {
            details += "ZASTĘPSTWO"
            lesson.substitutionNote?.let { if (it.isNotBlank()) details += ": $it" }
        }

        if (details.isNotEmpty()) {
            remoteViews.setTextViewText(R.id.widget_item_details, details)
            remoteViews.setViewVisibility(R.id.widget_item_details, android.view.View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.widget_item_details, android.view.View.GONE)
        }

        val fillInIntent = Intent()
        remoteViews.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_item_loading_layout)
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = lessons.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}