package com.example.llpclient.data.local

import android.util.Log
import com.example.llpclient.view.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    authManager: AuthManager

) {
    private val messagesService=authManager.messageService
    suspend fun getMessages(): Result<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                val messagesResponse = messagesService.getMessages()
                if (messagesResponse.isSuccessful && messagesResponse.body() != null) {
                    val apiMessages = messagesResponse.body()!!.data
                    val messages = coroutineScope {
                        apiMessages.map { apiMessage ->
                            async {
                                Message(
                                    id = apiMessage.messageId,
                                    sendDate = apiMessage.sendDate,
                                    topic = apiMessage.topic,
                                    content = apiMessage.content,
                                    senderFirstName = apiMessage.senderFirstName,
                                    senderLastName = apiMessage.senderLastName,
                                    senderName = apiMessage.senderName,
                                )
                            }
                        }.awaitAll()
                    }
                    Result.success(messages)
                } else {
                    val errorMsg = "Failed to fetch messages. Code: ${messagesResponse.code()}, Message: ${messagesResponse.message()}"
                    Log.e("MessagesRepository", errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("MessagesRepository", "Error fetching messages", e)
                Result.failure(e)
            }
        }
    }
}