package com.example.llpclient.data.local

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.llpclient.data.remote.dto.Receiver
import com.example.llpclient.data.remote.dto.RecipientTypeInfo
import com.example.llpclient.data.remote.dto.ReceiversPayload
import com.example.llpclient.data.remote.dto.SchoolReceiver
import com.example.llpclient.data.remote.dto.SendMessageRequest
import com.example.llpclient.data.remote.dto.StudentSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Base64
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class CreateMessageRepository @Inject constructor(
    authManager: AuthManager
) {
    private val messageService = authManager.messageService

    suspend fun getRecipientTypes(): Result<List<RecipientTypeInfo>> {
        return withContext(Dispatchers.IO) {
            Log.d("CreateMessageRepo", "Attempting to fetch recipient types...")
            try {
                val response = messageService.getRecipientTypes()
                if (response.isSuccessful && response.body()?.data?.list != null) {
                    val types = response.body()!!.data!!.list!!
                    Log.i("CreateMessageRepo", "Successfully fetched ${types.size} recipient types.")
                    Result.success(types)
                } else {
                    val errorMsg = "Failed to fetch recipient types. Code: ${response.code()}, Message: ${response.message()}"
                    Log.e("CreateMessageRepo", errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("CreateMessageRepo", "Error fetching recipient types", e)
                Result.failure(e)
            }
        }
    }
    suspend fun getStudentSubjectTeachers(): Result<List<StudentSubject>> {
        return withContext(Dispatchers.IO) {
            Log.d("CreateMessageRepo", "Attempting to fetch student subject teachers...")
            try {
                val response = messageService.getStudentSubjectTeachers()
                if (response.isSuccessful && response.body()?.data != null) {
                    val subjects = response.body()!!.data!!
                    Log.i("CreateMessageRepo", "Successfully fetched ${subjects.size} student subjects.")
                    Result.success(subjects)
                } else {
                    val errorMsg = "Failed to fetch student subjects. Code: ${response.code()}, Message: ${response.message()}"
                    Log.e("CreateMessageRepo", errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("CreateMessageRepo", "Error fetching student subjects", e)
                Result.failure(e)
            }
        }
    }
    suspend fun getReceiversForType(typeId: String): Result<List<Receiver>> {
        return withContext(Dispatchers.IO) {
            Log.d("CreateMessageRepo", "Attempting to fetch recipients for type: $typeId")
            try {
                val response = messageService.getReceiversForType(typeId)
                if (response.isSuccessful && response.body() != null) {
                    val recipients = response.body()?.receivers ?: emptyList()
                    Log.i("CreateMessageRepo", "Successfully fetched ${recipients.size} recipients for type $typeId.")
                    Result.success(recipients)
                } else {
                    val errorMsg = "Failed to fetch recipients for type $typeId. Code: ${response.code()}, Message: ${response.message()}"
                    Log.e("CreateMessageRepo", errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("CreateMessageRepo", "Error fetching recipients for type $typeId", e)
                Result.failure(e)
            }
        }
    }

    suspend fun sendMessage(recipientAccountIds: List<String>, topic: String, content: String): Result<Unit> {
        if (recipientAccountIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Recipient list cannot be empty."))
        }

        return withContext(Dispatchers.IO) {
            Log.d("CreateMessageRepo", "Attempting to send message to ${recipientAccountIds.size} recipients.")
            try {
                val encodedTopic = Base64.getEncoder().encodeToString(topic.toByteArray(Charsets.UTF_8))
                val encodedContent = Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))

                val schoolReceivers = recipientAccountIds.map { SchoolReceiver(accountId = it) }

                val messageRequest = SendMessageRequest(
                    topic = encodedTopic,
                    content = encodedContent,
                    receivers = ReceiversPayload(schoolReceivers = schoolReceivers)
                )

                Log.d("CreateMessageRepo", "Sending request body: $messageRequest")
                val response = messageService.sendMessage(messageRequest)

                if (response.isSuccessful) {
                    Log.i("CreateMessageRepo", "Message sent successfully. Code: ${response.code()}")
                    Result.success(Unit)
                } else {
                    val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { "Could not read error body."}
                    val errorMsg = "Failed to send message. Code: ${response.code()}, Message: ${response.message()}, Body: $errorBody"
                    Log.e("CreateMessageRepo", errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("CreateMessageRepo", "Error sending message", e)
                Result.failure(e)
            }
        }
    }
}