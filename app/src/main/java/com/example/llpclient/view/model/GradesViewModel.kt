package com.example.llpclient.view.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llpclient.data.local.GradesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class GradesViewModel @Inject constructor(
    private val gradesRepository: GradesRepository
) : ViewModel() {
    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    init {
        loadGrades()
    }
    fun loadGrades() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            gradesRepository.getGrades()
                .onSuccess { gradesData ->
                    _grades.value = gradesData
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
