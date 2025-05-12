package com.example.llpclient.view.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llpclient.data.local.TimetableRepository
import com.example.llpclient.view.model.TimetablePeriod
import com.example.llpclient.view.screen.TimetableUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    private val _timetable = MutableStateFlow<Map<String, List<TimetablePeriod?>>>(emptyMap())
    val timetable: StateFlow<Map<String, List<TimetablePeriod?>>> = _timetable.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentWeekMonday = MutableStateFlow(getInitialMonday())
    val currentWeekMonday: StateFlow<LocalDate> = _currentWeekMonday.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    val uiState: StateFlow<TimetableUiState> = combine(
        selectedDate,
        timetable,
        isLoading,
        error
    ) { selectedDate, timetableData, isLoading, error ->
        val selectedDateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lessons = timetableData[selectedDateStr] ?: emptyList()

        when {
            isLoading && timetableData.isEmpty() -> TimetableUiState.Loading
            error != null -> TimetableUiState.Error(error)
            timetableData.values.all { it.isEmpty() } -> TimetableUiState.Empty
            else -> TimetableUiState.Success(
                day = selectedDateStr to lessons,
                isRefreshing = isLoading
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState.Loading
    )
    init {
        loadTimetable()
    }

    private fun getInitialMonday(): LocalDate {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun loadTimetable() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val weekStart = _currentWeekMonday.value.format(DateTimeFormatter.ISO_LOCAL_DATE)

            timetableRepository.getTimetableForWeek(weekStart)
                .onSuccess { data ->
                    _timetable.value = data

                    val today = LocalDate.now()
                    val currentMonday = _currentWeekMonday.value
                    if (_selectedDate.value.isBefore(currentMonday) || _selectedDate.value.isAfter(currentMonday.plusDays(6))) {
                        _selectedDate.value = if (today in currentMonday..currentMonday.plusDays(6)) {
                            today
                        } else {
                            currentMonday
                        }
                    }
                }
                .onFailure { exception ->
                    _error.value = "Failed to load timetable: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun refreshTimetable() {
        loadTimetable()
    }

    fun selectDay(date: LocalDate) {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        if (monday != _currentWeekMonday.value) {
            _currentWeekMonday.value = monday
            _selectedDate.value = date
            loadTimetable()
        } else {
            _selectedDate.value = date
        }
    }

    fun navigateToNextWeek() {
        selectDay(_currentWeekMonday.value.plusWeeks(1))
    }

    fun navigateToPreviousWeek() {
        selectDay(_currentWeekMonday.value.minusWeeks(1))
    }

    fun getDatesForCurrentWeek(): List<LocalDate> {
        val monday = _currentWeekMonday.value
        return (0..6).map { monday.plusDays(it.toLong()) }
    }
}
