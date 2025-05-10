package com.example.llpclient.view.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.llpclient.view.model.Message
import com.example.llpclient.view.model.MessagesViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

sealed class MessagesUiState {
    object Loading : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
    object Empty : MessagesUiState()
    data class Success(val messages: List<Message>, val isRefreshing: Boolean) : MessagesUiState()
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MessagesScreen(
    messagesViewModel: MessagesViewModel = viewModel(),
    navController: NavController
) {
    val messages by messagesViewModel.messages.collectAsState()
    val isLoading by messagesViewModel.isLoading.collectAsState()
    val error by messagesViewModel.error.collectAsState()

    val uiState = remember(isLoading, error, messages) {
        when {
            isLoading && messages.isEmpty() -> MessagesUiState.Loading
            error != null -> MessagesUiState.Error(error ?: "Unknown error")
            !isLoading && messages.isEmpty() -> MessagesUiState.Empty
            else -> MessagesUiState.Success(messages, isLoading)
        }
    }


    LaunchedEffect(key1 = true) {
        if (messages.isEmpty() && error == null) {
            messagesViewModel.loadMessages()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("create_message_route")
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create New Message"
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when (uiState) {
                is MessagesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MessagesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { messagesViewModel.refreshMessages() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is MessagesUiState.Empty -> {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing = isLoading),
                        onRefresh = { messagesViewModel.refreshMessages() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No messages found.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is MessagesUiState.Success -> {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(uiState.isRefreshing),
                        onRefresh = { messagesViewModel.refreshMessages() }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.messages,
                                key = { message -> message.id }
                            ) { message ->
                                MessageItem(message = message)
                                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MessageItem(message: Message) {
    var showPopup by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPopup=true}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message.senderName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.topic,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatMessageDate(message.sendDate),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.wrapContentWidth(Alignment.End)
        )

    }

    if (showPopup) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp

        val annotatedContent = remember(message.content) {
            try {
                val htmlContent = String(Base64.getDecoder().decode(message.content))
                HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY).let { spanned ->
                    buildAnnotatedString { append(spanned) }
                }
            } catch (_: IllegalArgumentException) {
                AnnotatedString("Error: Content could not be displayed.")
            } catch (e: Exception) {
                AnnotatedString("Error processing content.")
            }
        }

        Dialog(
            onDismissRequest = { showPopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 600.dp)
                    .heightIn(min = 150.dp, max = screenHeight * 0.8f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 20.dp),
                ) {
                    Text(
                        text = message.topic,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = annotatedContent,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showPopup = false }
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatMessageDate(dateString: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ISO_DATE_TIME
        val outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
        val dateTime = LocalDateTime.parse(dateString, inputFormatter)
        dateTime.format(outputFormatter)
    } catch (_: Exception) {
        try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            val date = LocalDate.parse(dateString, inputFormatter)
            date.format(outputFormatter)
        } catch (_: Exception) {
            dateString
        }
    }
}