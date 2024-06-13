package com.example.projectassistent2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.projectassistent2.ui.theme.ProjectAssistent2Theme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialize Firebase
        setContent {
            ProjectAssistent2Theme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember { mutableStateOf(auth.currentUser) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Project Assistant", color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle menu click */ }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("main") { MainContent(navController, currentUser) }
            composable("zbukanje") { ZbukanjeScreen { navController.navigateUp() } }
            composable("login") { LoginScreen(navController, currentUser) }
            composable("register") { RegisterScreen(navController) }
        }
    }
}

@Composable
fun MainContent(navController: NavHostController, currentUser: MutableState<FirebaseUser?>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MenuButton(
            text = "Žbukanje",
            onClick = { navController.navigate("zbukanje") }
        )
        MenuButton(text = "Krečenje")
        MenuButton(text = "Moji radovi")
        if (currentUser.value != null) {
            MenuButton(
                text = "Moji materijali",
                onClick = { navController.navigate("materials") }
            )
        } else {
            MenuButton(
                text = "Login",
                onClick = { navController.navigate("login") }
            )
            MenuButton(
                text = "Register",
                onClick = { navController.navigate("register") }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        UserProfile(currentUser)
    }
}

@Composable
fun MenuButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun UserProfile(currentUser: MutableState<FirebaseUser?>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Korisnik: ${currentUser.value?.email ?: "Guest"}",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ProjectAssistent2Theme {
        MainScreen()
    }
}
