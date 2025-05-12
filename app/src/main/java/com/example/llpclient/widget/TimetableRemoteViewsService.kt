package com.example.llpclient.widget

import android.content.Intent
import android.widget.RemoteViewsService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimetableRemoteViewsService : RemoteViewsService() {

    @Inject
    lateinit var factory: TimetableRemoteViewsFactory

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return factory
    }
}