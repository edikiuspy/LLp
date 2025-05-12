package com.example.llpclient.view.model

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llpclient.data.local.CreateMessageRepository
import com.example.llpclient.data.remote.dto.Receiver
import com.example.llpclient.data.remote.dto.RecipientTypeInfo
import com.example.llpclient.data.remote.dto.StudentSubject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject


sealed class SendResult {
    object Success : SendResult()
    data class Error(val message: String) : SendResult()
    object Idle : SendResult()
}

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class CreateMessageViewModel @Inject constructor(
    private val repository: CreateMessageRepository
) : ViewModel() {


    var isSending by mutableStateOf(false)
    private val _sendResult = MutableSharedFlow<SendResult>()
    val sendResult = _sendResult.asSharedFlow()
    var recipientTypes by mutableStateOf<List<RecipientTypeInfo>>(emptyList())
    var isLoadingInitialData by mutableStateOf(false)
    var initialDataError by mutableStateOf<String?>(null)
    var studentSubjectsList by mutableStateOf<List<StudentSubject>>(emptyList())
    var allTeachersList by mutableStateOf<List<Receiver>>(emptyList())
    var expandedTypeId by mutableStateOf<String?>(null)
    var individualsInExpandedType by mutableStateOf<List<Receiver>>(emptyList())
    var isLoadingIndividuals by mutableStateOf(false)
    var individualError by mutableStateOf<String?>(null)
    var selectedReceivers by mutableStateOf<Set<Receiver>>(emptySet())


    fun loadInitialRecipientData() {
        if (isLoadingInitialData || recipientTypes.isNotEmpty()) return

        viewModelScope.launch {
            isLoadingInitialData = true
            initialDataError = null
            Log.d("CreateMessageVM", "Loading initial recipient data (Types, Subjects, Teachers)...")

            try {
                coroutineScope {
                    val typesDeferred = async { repository.getRecipientTypes() }
                    val subjectsDeferred = async { repository.getStudentSubjectTeachers() }

                    val teachersDeferred = async { repository.getReceiversForType("teachers") }

                    val typesResult = typesDeferred.await()
                    val subjectsResult = subjectsDeferred.await()
                    val teachersResult = teachersDeferred.await()


                    recipientTypes = typesResult.getOrElse {
                        throw IOException("Failed to load recipient types", it)
                    }
                    studentSubjectsList = subjectsResult.getOrElse {
                        Log.e("CreateMessageVM", "Failed to load student subjects, label modification won't work.", it)
                        emptyList()
                    }
                    allTeachersList = teachersResult.getOrElse {
                        Log.e("CreateMessageVM", "Failed to load full teachers list, label modification won't work.", it)
                        emptyList()
                    }

                    Log.d("CreateMessageVM", "Initial data loaded. Types: ${recipientTypes.size}, Subjects: ${studentSubjectsList.size}, Teachers: ${allTeachersList.size}")
                }
            } catch (e: Exception) {
                val errorMsg = "Failed to load initial recipient data: ${e.localizedMessage ?: "Unknown error"}"
                Log.e("CreateMessageVM", errorMsg, e)
                initialDataError = errorMsg
                recipientTypes = emptyList()
                studentSubjectsList = emptyList()
                allTeachersList = emptyList()
            } finally {
                isLoadingInitialData = false
            }
        }
    }


    fun toggleTypeExpansion(typeId: String?) {
        if (typeId == null || typeId == expandedTypeId) {

            expandedTypeId = null
            individualsInExpandedType = emptyList()
            individualError = null
            isLoadingIndividuals = false
            return
        }


        expandedTypeId = typeId

        individualsInExpandedType = emptyList()
        individualError = null
        isLoadingIndividuals = false


        if (typeId == "teachers") {
            Log.d("CreateMessageVM", "Expanding 'teachers'. Processing labels...")

            if (allTeachersList.isEmpty()) {
                Log.w("CreateMessageVM", "Teachers list is empty, cannot display individuals.")


                individualsInExpandedType = emptyList()
                return
            }


            val subjectMap = studentSubjectsList
                .filter { it.teacherIdentifier != null }
                .associateBy { it.teacherIdentifier!! }

            val modifiedTeachers = allTeachersList.map { teacher ->
                val subjectInfo = teacher.accountId.let { subjectMap[it] }
                if (subjectInfo != null) {

                    teacher.copy(label = "${subjectInfo.subject ?: "Subject"} - ${teacher.label}")
                } else {

                    teacher
                }
            }.sortedBy { it.label }

            individualsInExpandedType = modifiedTeachers
            Log.d("CreateMessageVM", "Processed ${modifiedTeachers.size} teachers for display.")

            return
        }


        viewModelScope.launch {
            isLoadingIndividuals = true
            Log.d("CreateMessageVM", "Fetching individuals for standard type $typeId...")
            val result = repository.getReceiversForType(typeId)

            if (expandedTypeId == typeId) {
                result.onSuccess { individuals ->
                    individualsInExpandedType = individuals.sortedBy { it.label }
                    Log.d("CreateMessageVM", "Standard individuals fetched for $typeId: ${individuals.size}")
                }.onFailure { exception ->
                    val errorMsg = "Error fetching individuals for $typeId: ${exception.localizedMessage}"
                    Log.e("CreateMessageVM", errorMsg, exception)
                    individualError = errorMsg
                    individualsInExpandedType = emptyList()
                }
                isLoadingIndividuals = false
            } else {
                Log.d("CreateMessageVM", "Expansion changed while loading for $typeId, discarding.")
                if (expandedTypeId == null) {
                    isLoadingIndividuals = false
                }
            }
        }
    }


    fun toggleRecipientSelection(receiver: Receiver) {
        selectedReceivers = if (selectedReceivers.contains(receiver)) {
            selectedReceivers - receiver
        } else {
            selectedReceivers + receiver
        }
        Log.d("CreateMessageVM", "Selection changed. Count: ${selectedReceivers.size}")
    }

    fun isRecipientSelected(receiver: Receiver): Boolean {
        return selectedReceivers.contains(receiver)
    }


    fun sendMessage(topic: String, content: String) {
        if (isSending) return
        val recipientAccountIds = selectedReceivers.map { it.accountId }

        if (recipientAccountIds.isEmpty()) { viewModelScope.launch { _sendResult.emit(SendResult.Error("Please select at least one recipient.")) }; return }
        if (topic.isBlank()) { viewModelScope.launch { _sendResult.emit(SendResult.Error("Topic cannot be empty.")) }; return }
        if (content.isBlank()) { viewModelScope.launch { _sendResult.emit(SendResult.Error("Message content cannot be empty.")) }; return }

        viewModelScope.launch {
            isSending = true
            _sendResult.emit(SendResult.Idle)
            val result = repository.sendMessage(recipientAccountIds, topic, content)
            result.onSuccess {
                Log.i("CreateMessageVM", "Message sent successfully.")
                _sendResult.emit(SendResult.Success)
            }.onFailure { exception ->
                val errorMsg = "Failed to send message: ${exception.localizedMessage ?: "Unknown error"}"
                Log.e("CreateMessageVM", errorMsg, exception)
                _sendResult.emit(SendResult.Error(errorMsg))
            }
            isSending = false
        }
    }


    fun clearResult() {
        viewModelScope.launch {
            _sendResult.emit(SendResult.Idle)
        }
    }
}