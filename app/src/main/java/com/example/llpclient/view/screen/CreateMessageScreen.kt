package com.example.llpclient.view.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.llpclient.data.remote.dto.Receiver
import com.example.llpclient.data.remote.dto.RecipientTypeInfo
import com.example.llpclient.view.model.CreateMessageViewModel
import com.example.llpclient.view.model.SendResult
import kotlinx.coroutines.flow.collectLatest

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMessageScreen(
    navController: NavHostController,
    viewModel: CreateMessageViewModel = hiltViewModel()
) {
    var topic by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    val isSending by remember { derivedStateOf { viewModel.isSending } }
    val recipientTypes by remember { derivedStateOf { viewModel.recipientTypes } }
    val isLoadingInitialData by remember { derivedStateOf { viewModel.isLoadingInitialData } }
    val initialDataError by remember { derivedStateOf { viewModel.initialDataError } }
    val expandedTypeId by remember { derivedStateOf { viewModel.expandedTypeId } }
    val individualsInExpandedType by remember { derivedStateOf { viewModel.individualsInExpandedType } }
    val isLoadingIndividuals by remember { derivedStateOf { viewModel.isLoadingIndividuals } }
    val individualError by remember { derivedStateOf { viewModel.individualError } }
    val selectedReceivers by remember { derivedStateOf { viewModel.selectedReceivers } }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = Unit) {
        viewModel.loadInitialRecipientData()
    }

    LaunchedEffect(key1 = viewModel.sendResult) {
        viewModel.sendResult.collectLatest { result ->
            when (result) {
                is SendResult.Success -> {
                    Toast.makeText(context, "Message Sent!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
                is SendResult.Error -> {
                    showSnackbar = result.message
                }
                is SendResult.Idle -> {
                    showSnackbar = null
                }
            }
        }
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar != null) {
            val result = snackbarHostState.showSnackbar(
                message = showSnackbar!!,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.Dismissed || result == SnackbarResult.ActionPerformed) {
                showSnackbar = null
                viewModel.clearResult()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Message") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Odbiorcy wiadomości",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                if (isLoadingInitialData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else if (initialDataError != null) {
                    Text(
                        "Error loading recipient data: $initialDataError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                else {
                    recipientTypes.forEach { type ->
                        RecipientTypeItem(
                            typeInfo = type,
                            isExpanded = expandedTypeId == type.id,
                            individuals = if (expandedTypeId == type.id) individualsInExpandedType else emptyList(),
                            isLoadingIndividuals = isLoadingIndividuals && expandedTypeId == type.id && type.id != "teachers",
                            individualError = if (expandedTypeId == type.id && type.id != "teachers") individualError else null,
                            isRecipientSelected = { receiver -> viewModel.isRecipientSelected(receiver) },
                            onExpandToggle = { viewModel.toggleTypeExpansion(type.id) },
                            onRecipientToggle = { receiver -> viewModel.toggleRecipientSelection(receiver) }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !isSending
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Message") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !isSending
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.sendMessage(topic, content) },
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isSending && selectedReceivers.isNotEmpty() && topic.isNotBlank() && content.isNotBlank()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                    else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Send")
                    }
                }
            }
        }
    }
}


@Composable
fun RecipientTypeItem(
    typeInfo: RecipientTypeInfo,
    isExpanded: Boolean,
    individuals: List<Receiver>,
    isLoadingIndividuals: Boolean,
    individualError: String?,
    isRecipientSelected: (Receiver) -> Boolean,
    onExpandToggle: () -> Unit,
    onRecipientToggle: (Receiver) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = if (isExpanded) "Collapse ${typeInfo.name}" else "Expand ${typeInfo.name}",
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = typeInfo.name ?: "Unknown Type",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                if (isLoadingIndividuals) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                else if (individualError != null) {
                    Text(
                        "Error loading recipients: $individualError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }
                else if (individuals.isEmpty()) {
                    Text(
                        "No recipients found in this group.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }
                else {
                    individuals.forEach { individual ->
                        RecipientIndividualItem(
                            receiver = individual,
                            isSelected = isRecipientSelected(individual),
                            onToggle = { onRecipientToggle(individual) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipientIndividualItem(
    receiver: Receiver,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = receiver.label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}