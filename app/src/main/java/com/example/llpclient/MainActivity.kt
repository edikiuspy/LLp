package com.example.llpclient

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.llpclient.ui.theme.LLpClientTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LLpClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}




@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppContent(
    loginViewModel: LoginViewModel = viewModel(),
    gradesViewModel: GradesViewModel = viewModel()
) {
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    var currentScreen by remember { mutableStateOf("Home") }

    if (!isLoggedIn) {
        LoginScreen(viewModel = loginViewModel){}
    } else {

        DetailedDrawerExample(
            currentScreen = currentScreen,
            onScreenSelected = { screen -> currentScreen = screen },
            content = { paddingValues ->
                when (currentScreen) {
                    "Grades" -> GradesScreen(gradesViewModel, paddingValues)
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("You are logged in!")
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { loginViewModel.logout() }
                                ) {
                                    Text("Logout")
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedDrawerExample(
    currentScreen: String = "Home",
    onScreenSelected: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text("LLp dev build", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider()

                    NavigationDrawerItem(
                        label = { Text("Home") },
                        selected = currentScreen == "Home",
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        onClick = {
                            onScreenSelected("Home")
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Grades") },
                        selected = currentScreen == "Grades",
                        icon = { Icon(Icons.Default.Filter6, contentDescription = null) },
                        onClick = {
                            onScreenSelected("Grades")
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Timetable") },
                        selected = currentScreen == "Timetable",
                        icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                        onClick = {
                            onScreenSelected("Timetable")
                            scope.launch { drawerState.close() }
                        },
                    )

                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("LLp dev build - $currentScreen") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}