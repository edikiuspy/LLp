package com.example.llpclient

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Filter6
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.llpclient.ui.theme.LLpClientTheme
import com.example.llpclient.view.model.GradesViewModel
import com.example.llpclient.view.model.LoginViewModel
import com.example.llpclient.view.model.MessagesViewModel
import com.example.llpclient.view.model.TimetableViewModel
import com.example.llpclient.view.screen.CreateMessageScreen
import com.example.llpclient.view.screen.GradesScreen
import com.example.llpclient.view.screen.LoginScreen
import com.example.llpclient.view.screen.MessagesScreen
import com.example.llpclient.view.screen.TimetableScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Grades : Screen("grades", "Grades")
    object Timetable : Screen("timetable", "Timetable")
    object Messages : Screen("messages", "Messages")
    object CreateMessage : Screen("create_message_route", "New Message")
}

val drawerScreens = listOf(
    Screen.Home,
    Screen.Grades,
    Screen.Timetable,
    Screen.Messages
)

@AndroidEntryPoint
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
    loginViewModel: LoginViewModel = hiltViewModel(),
    gradesViewModel: GradesViewModel = hiltViewModel(),
    timetableViewModel: TimetableViewModel = hiltViewModel(),
    messagesViewModel: MessagesViewModel = hiltViewModel()
) {
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
    val navController = rememberNavController()

    if (!isLoggedIn) {
        LoginScreen(viewModel = loginViewModel) {
        }
    } else {
        MainScaffoldWithDrawer(
            navController = navController,
            loginViewModel = loginViewModel,
            gradesViewModel = gradesViewModel,
            timetableViewModel = timetableViewModel,
            messagesViewModel = messagesViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldWithDrawer(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    gradesViewModel: GradesViewModel,
    timetableViewModel: TimetableViewModel,
    messagesViewModel: MessagesViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = drawerScreens.find { it.route == currentRoute }
        ?: Screen.Home

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "LLp dev build",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))


                    drawerScreens.forEach { screen ->
                        NavigationDrawerItem(
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Home -> Icons.Default.Home
                                        Screen.Grades -> Icons.Default.Filter6
                                        Screen.Timetable -> Icons.Outlined.CalendarMonth
                                        Screen.Messages -> Icons.AutoMirrored.Outlined.Message
                                        else -> Icons.Default.Home
                                    },
                                    contentDescription = screen.title
                                )
                            },
                            onClick = {
                                navController.navigate(screen.route) {



                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }


                                    launchSingleTop = true

                                    restoreState = true
                                }
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("LLp dev build - ${currentScreen.title}") },
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

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Welcome Home!")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loginViewModel.logout() }) {
                                Text("Logout")
                            }
                        }
                    }
                }
                composable(Screen.Grades.route) {
                    GradesScreen(gradesViewModel, PaddingValues())
                }
                composable(Screen.Timetable.route) {
                    TimetableScreen(timetableViewModel)
                }
                composable(Screen.Messages.route) {
                    MessagesScreen(
                        messagesViewModel = messagesViewModel,
                        navController = navController
                    )
                }
                composable(Screen.CreateMessage.route) {

                    CreateMessageScreen(navController = navController)
                }

            }
        }
    }
}



