package com.example.llpclient.view.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llpclient.data.local.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias Timetable = Map<String, List<TimetablePeriod?>>

data class TimetablePeriod(
    val subject: String
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository
) : ViewModel() {
    private val _timetable = MutableStateFlow<Timetable>(emptyMap())
    val timetable: StateFlow<Timetable> = _timetable.asStateFlow()

    private val _weekStart = MutableStateFlow<String?>(null)
    val weekStart: StateFlow<String?> = _weekStart.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTimetable()
    }

    fun loadTimetable() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            timetableRepository.getTimetable()
                .onSuccess { timetableData ->
                    _timetable.value = timetableData
                }
                .onFailure { exception ->
                    _error.value = "Failed to load timetable: ${exception.message}"
                }

            _isLoading.value = false
        }
    }
}