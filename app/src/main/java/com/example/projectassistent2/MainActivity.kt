package com.example.projectassistent2

import ZbukanjeScreen
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Notification permissions are required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

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
    val auth = FirebaseAuth.getInstance()
    val currentUser = remember { mutableStateOf(auth.currentUser) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser.value = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(authStateListener)
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navController = navController,
                currentUser = currentUser,
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Project Assistant", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
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
                startDestination = if (currentUser.value != null) "main" else "login",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("main") { MainContent(navController, currentUser) }
                composable("zbukanje") { ZbukanjeScreen { navController.navigateUp() } }
                composable("krecenje") { KrecenjeScreen { navController.navigateUp() } }
                composable("login") { LoginScreen(navController, currentUser) }
                composable("register") { RegisterScreen(navController) }
                composable("materijali") { MaterijaliScreen(navController, currentUser.value) }
                composable("mojiRadovi") { MojiRadoviScreen(navController, currentUser.value) }
                composable("projekti") { ProjektiScreen(navController, currentUser.value) }
            }
        }
    }
}

@Composable
fun DrawerContent(
    navController: NavHostController,
    currentUser: MutableState<FirebaseUser?>,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("main") }) {
            Text(text = "Home")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentUser.value != null) {
            Button(onClick = onLogout) {
                Text(text = "Logout")
            }

        } else {
            Button(onClick = { navController.navigate("login") }) {
                Text(text = "Login")
            }
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
            imageRes = R.drawable.ic_zbukanje,
            onClick = { navController.navigate("zbukanje") }
        )
        MenuButton(
            text = "Krečenje",
            imageRes = R.drawable.ic_krecenje,
            onClick = { navController.navigate("krecenje") }
        )

        if (currentUser.value != null) {
            MenuButton(
                text = "Moji radovi",
                imageRes = R.drawable.ic_moji_radovi,
                onClick = { navController.navigate("mojiRadovi") }
            )
            MenuButton(
                text = "Projekti",
                imageRes = R.drawable.ic_moji_projekti,
                onClick = { navController.navigate("projekti") }
            )
        }

        if (currentUser.value != null) {
            MenuButton(
                text = "Moji materijali",
                imageRes = R.drawable.ic_moji_materijali,
                onClick = { navController.navigate("materijali") }
            )
        } else {
            MenuButton(
                text = "Login",
                imageRes = null,
                onClick = { navController.navigate("login") }
            )
            MenuButton(
                text = "Register",
                imageRes = null,
                onClick = { navController.navigate("register") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        UserProfile(currentUser)
    }
}

@Composable
fun MenuButton(text: String, imageRes: Int? = null, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        imageRes?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
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