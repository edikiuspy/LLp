package com.example.llpclient.view.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.unit.sp
import com.example.llpclient.view.model.Grade
import com.example.llpclient.view.model.GradesViewModel
import java.util.SortedMap
import kotlin.math.max



sealed class GradesUiState {
    object Loading : GradesUiState()
    data class Error(val message: String) : GradesUiState()
    object Empty : GradesUiState()
    data class Success(
        val groupedGrades: SortedMap<String, List<Grade>>,
        val isRefreshing: Boolean
    ) : GradesUiState()
}



@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GradesScreen(
    gradesViewModel: GradesViewModel = viewModel(),
    paddingValues: PaddingValues
) {
    val grades by gradesViewModel.grades.collectAsState()
    val isLoading by gradesViewModel.isLoading.collectAsState()
    val error by gradesViewModel.error.collectAsState()



    val groupedGrades by remember(grades) {
        derivedStateOf {
            grades.groupBy { it.subjectName }
                .toSortedMap()
        }
    }


    val uiState = remember(isLoading, error, grades, groupedGrades) {
        when {

            isLoading && grades.isEmpty() -> GradesUiState.Loading
            error != null -> GradesUiState.Error(error ?: "Unknown error")

            !isLoading && groupedGrades.isEmpty() -> GradesUiState.Empty

            else -> GradesUiState.Success(groupedGrades, isLoading)
        }
    }

    val expandedSubjects = remember { mutableStateOf(emptySet<String>()) }

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
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )


        when (uiState) {
            is GradesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is GradesUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is GradesUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No grades found.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is GradesUiState.Success -> {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(uiState.isRefreshing),
                    onRefresh = { gradesViewModel.refreshGrades() }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        uiState.groupedGrades.forEach { (subjectName, subjectGrades) ->
                            val isExpanded = subjectName in expandedSubjects.value

                            item(key = "header_$subjectName") {
                                SubjectHeader(
                                    subjectName = subjectName,
                                    isExpanded = isExpanded,
                                    gradeCount = subjectGrades.size,
                                    onClick = {

                                        expandedSubjects.value = when (isExpanded) {
                                            true -> expandedSubjects.value - subjectName
                                            false -> expandedSubjects.value + subjectName
                                        }
                                    }
                                )
                            }



                            items(
                                items = when (isExpanded) {
                                    true -> subjectGrades
                                    false -> emptyList()
                                },
                                key = { grade -> "grade_${grade.id}" }
                            ) { grade ->
                                Box(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                                    GradeItem(grade = grade)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectHeader(
    subjectName: String,
    isExpanded: Boolean,
    gradeCount: Int,
    onClick: () -> Unit
) {
    val initialFontSize = MaterialTheme.typography.titleLarge.fontSize.value
    var fontSize by remember(subjectName) { mutableFloatStateOf(initialFontSize) }
    val minFontSize = 12f

    val rotationAngle by animateFloatAsState(

        targetValue = when (isExpanded) {
            true -> 180f
            false -> 0f
        },
        label = "ArrowRotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = fontSize.sp
                    ),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    onTextLayout = { layoutResult ->

                        when {
                            layoutResult.hasVisualOverflow && fontSize > minFontSize -> {
                                fontSize = max(minFontSize, fontSize * 0.9f)
                            }
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(

                    text = "$gradeCount ${when (gradeCount) {
                        1 -> "grade"
                        else -> "grades"
                    }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,

                    contentDescription = when (isExpanded) {
                        true -> "Collapse"
                        false -> "Expand"
                    },
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GradeItem(grade: Grade) {
    Card(
        modifier = Modifier.fillMaxWidth(),

        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getGradeColor(grade.value)
                    ),
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = grade.value,
                            style = MaterialTheme.typography.titleMedium,
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
                    text = grade.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formatDate(grade.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            when (grade.hasComments) {
                true -> {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Has comments",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }


                false -> Unit
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

        "np", "nk" -> Color(0xFF9E9E9E)
        "zw" -> Color(0xFF607D8B)
        else -> Color.Gray
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(dateString: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val date = LocalDate.parse(dateString, inputFormatter)
        date.format(outputFormatter)
    } catch (_: Exception) {
        dateString
    }
}


