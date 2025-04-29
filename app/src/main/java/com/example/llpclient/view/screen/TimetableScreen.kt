package com.example.llpclient.view.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.llpclient.view.model.Grade
import com.example.llpclient.view.model.Timetable
import com.example.llpclient.view.model.TimetablePeriod
import com.example.llpclient.view.model.TimetableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.SortedMap

sealed class TimetableUiState {
    object Loading : TimetableUiState()
    data class Error(val message: String) : TimetableUiState()
    object Empty : TimetableUiState()
    data class Success(
        val day: Pair<String, List<TimetablePeriod?>>,
        val isRefreshing: Boolean
    ) : TimetableUiState()
}

@Composable
fun TimetableScreen(
    timetableViewModel: TimetableViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Timetable implementation is being worked on")
    }
}