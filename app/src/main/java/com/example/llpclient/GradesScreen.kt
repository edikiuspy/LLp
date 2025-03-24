package com.example.llpclient

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GradesScreen(
    gradesViewModel: GradesViewModel = viewModel(),
    paddingValues: PaddingValues
) {
    val grades by gradesViewModel.grades.collectAsState()
    val isLoading by gradesViewModel.isLoading.collectAsState()
    val error by gradesViewModel.error.collectAsState()

    LaunchedEffect(key1 = true) {
        gradesViewModel.loadGrades()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Text(
            "Your Grades",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading && grades.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            SwipeRefresh(
                state = rememberSwipeRefreshState(isLoading),
                onRefresh = { gradesViewModel.refreshGrades() }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(grades) { grade ->
                        GradeItem(grade = grade)
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GradeItem(grade: Grade) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getGradeColor(grade.value)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = grade.value,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))


            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = grade.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = grade.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatDate(grade.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            if (grade.hasComments) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "Has comments",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun getGradeColor(grade: String): Color {
    return when (grade) {
        "6", "6-" -> Color(0xFF388E3C)
        "5+", "5", "5-" -> Color(0xFF4CAF50)
        "4+", "4", "4-" -> Color(0xFF8BC34A)
        "3+", "3", "3-" -> Color(0xFFFFC107)
        "2+", "2", "2-" -> Color(0xFFFF9800)
        "1+", "1" -> Color(0xFFF44336)
        "+" -> Color(0xFF2196F3)
        "-" -> Color(0xFFFF9800)
        else -> Color.Gray
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(dateString: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val date = LocalDate.parse(dateString, inputFormatter)
    return date.format(outputFormatter)
}
