package com.example.llpclient

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Grade(
    val id: Int,
    val value: String,
    val date: String,
    val addDate: String,
    val subjectId: Int,
    val subjectName: String,
    val category: GradeCategory,
    val hasComments: Boolean,
    val semester: Int
)

data class GradeCategory(
    val id: Int,
    val name: String = ""
)

class GradesViewModel(application: Application) : AndroidViewModel(application) {
    private val gradesRepository = GradesRepository(application.applicationContext)

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadGrades() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            gradesRepository.getGrades()
                .onSuccess { grades ->
                    _grades.value = grades
                }
                .onFailure { exception ->
                    _error.value = "Failed to load grades: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun refreshGrades() {
        loadGrades()
    }
}
