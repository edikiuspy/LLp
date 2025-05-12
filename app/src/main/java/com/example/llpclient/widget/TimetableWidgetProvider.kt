package com.example.llpclient.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.example.llpclient.MainActivity
import com.example.llpclient.R
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Suppress("DEPRECATION")
@AndroidEntryPoint
class TimetableWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.example.llpclient.widget.ACTION_REFRESH"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (ACTION_REFRESH == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (appWidgetIds != null) {

                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_timetable_list)

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }


    override fun onEnabled(context: Context) {


    }

    override fun onDisabled(context: Context) {


    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_timetable_layout)
        val intent = Intent(context, TimetableRemoteViewsService::class.java).apply {

            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = this.toUri(Intent.URI_INTENT_SCHEME).toUri()
        }
        remoteViews.setRemoteAdapter(R.id.widget_timetable_list, intent)
        val polishLocale = Locale("pl", "PL")
        val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", polishLocale)
        val formattedDate = LocalDate.now().format(headerDateFormatter)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(polishLocale) else it.toString() }
        remoteViews.setTextViewText(R.id.widget_header_date, formattedDate)
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_root_layout, openAppPendingIntent)
        val refreshIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)


        val itemClickIntent = Intent(context, MainActivity::class.java)


        val itemClickPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 1000,
            itemClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setPendingIntentTemplate(R.id.widget_timetable_list, itemClickPendingIntent)

        remoteViews.setEmptyView(R.id.widget_timetable_list, R.id.widget_empty_view)
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_timetable_list)
    }
}