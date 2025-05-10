package com.example.llpclient.view.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llpclient.data.local.MessagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class Message(
    val id: Int,
    val sendDate: String,
    val topic: String,
    val content: String,
    val senderFirstName: String,
    val senderLastName: String,
    val senderName: String
)



@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMessages()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadMessages() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            messagesRepository.getMessages()
                .onSuccess { messageDtos ->
                    _messages.value = messageDtos
                        .sortedByDescending { parseDateToSort(it.sendDate) }
                }
                .onFailure { exception ->
                    _error.value = "Failed to load messages: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDateToSort(dateString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    fun refreshMessages() {
        loadMessages()
    }
}