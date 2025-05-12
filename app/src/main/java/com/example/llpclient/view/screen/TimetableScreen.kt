package com.example.llpclient.view.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.llpclient.view.model.TimetablePeriod
import com.example.llpclient.view.model.TimetableViewModel
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class TimetableUiState {
    object Loading : TimetableUiState()
    data class Error(val message: String) : TimetableUiState()
    object Empty : TimetableUiState()
    data class Success(
        val day: Pair<String, List<TimetablePeriod?>>,
        val isRefreshing: Boolean
    ) : TimetableUiState()
}

@OptIn(ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetableScreen(
    timetableViewModel: TimetableViewModel = viewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by timetableViewModel.uiState.collectAsState()
    val currentWeekMonday by timetableViewModel.currentWeekMonday.collectAsState()
    val selectedDate by timetableViewModel.selectedDate.collectAsState()

    val weekDates = remember(currentWeekMonday) { timetableViewModel.getDatesForCurrentWeek() }
    LaunchedEffect(key1 = true) {
        timetableViewModel.refreshTimetable()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TimetableHeader(
            selectedDate = selectedDate,
            weekDates = weekDates,
            onPreviousWeek = { timetableViewModel.navigateToPreviousWeek() },
            onNextWeek = { timetableViewModel.navigateToNextWeek() },
            onDateSelected = { date -> timetableViewModel.selectDay(date) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = uiState) {
            is TimetableUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is TimetableUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(onClick = { timetableViewModel.refreshTimetable() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is TimetableUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No timetable data available for this week. Try changing the week or refresh.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is TimetableUiState.Success -> {
                val (_, lessonsForDay) = state.day

                val pullRefreshState = rememberPullRefreshState(
                    refreshing = state.isRefreshing,
                    onRefresh = { timetableViewModel.refreshTimetable() }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    val actualLessons = lessonsForDay.filterNotNull()

                    if (actualLessons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No lessons scheduled for this day.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = actualLessons, key = { period -> period.id }) { period ->
                                TimetablePeriodItem(period = period)
                            }

                            item {
                                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = state.isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetableHeader(
    selectedDate: LocalDate,
    weekDates: List<LocalDate>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val polishLocale = Locale("pl", "PL")

    val headerDateFormatter = remember(selectedDate) {
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", polishLocale)
            .format(selectedDate)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(polishLocale) else it.toString() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Week")
            }
            Text(
                text = headerDateFormatter,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            IconButton(onClick = onNextWeek) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Week")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDates.forEach { date ->
                DaySelectItem(
                    date = date,
                    isSelected = date.isEqual(selectedDate),
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DaySelectItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayLetter = remember(date) {
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "P"
            DayOfWeek.TUESDAY -> "W"
            DayOfWeek.WEDNESDAY -> "Ś"
            DayOfWeek.THURSDAY -> "C"
            DayOfWeek.FRIDAY -> "P"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "N"
        }
    }
    val dayNumber = remember(date) { date.dayOfMonth.toString() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .widthIn(min = 40.dp)
    ) {
        Text(
            text = dayLetter,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TimetablePeriodItem(period: TimetablePeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = period.color)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = period.subjectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${period.hourFrom} - ${period.hourTo}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            if (period.teacherName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = period.teacherName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }


            if (period.isCancelled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ODWOŁANE" + (period.substitutionNote?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            } else if (period.isSubstitution) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ZASTĘPSTWO" + (period.substitutionNote?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}