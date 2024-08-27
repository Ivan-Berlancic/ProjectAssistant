package com.example.projectassistent2

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjektiScreen(navController: NavHostController, currentUser: FirebaseUser?) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    var projects by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var projectName by remember { mutableStateOf(TextFieldValue("")) }
    var projectDescription by remember { mutableStateOf(TextFieldValue("")) }
    var projectStartDate by remember { mutableStateOf(TextFieldValue("")) }
    var projectEndDate by remember { mutableStateOf(TextFieldValue("")) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        coroutineScope.launch {
            refreshProjects(firestore, currentUser, onProjectsLoaded = { loadedProjects ->
                projects = loadedProjects
            }, onError = { error ->
                errorMessage = error
            })
        }
    }

    Scaffold(
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2E2E2E))
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Projekti",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                projects?.let {
                    it.forEach { project ->
                        ProjectCard(project) {
                            coroutineScope.launch {
                                deleteProjectFromFirestore(context, currentUser, project["id"] as String) {
                                    projects = projects?.filterNot { it["id"] == project["id"] }
                                }
                            }
                        }
                    }
                } ?: errorMessage?.let {
                    Text("Error: $it", color = Color.Red)
                }

                TextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Naziv projekta") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text("Opis projekta") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = projectStartDate,
                    onValueChange = { projectStartDate = it },
                    label = { Text("Početak (dd/MM/yyyy)") },
                    placeholder = { Text("dd/MM/yyyy") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = projectStartDate.text.isNotEmpty() && parseDate(projectStartDate.text) == null
                )
                OutlinedTextField(
                    value = projectEndDate,
                    onValueChange = { projectEndDate = it },
                    label = { Text("Kraj (dd/MM/yyyy)") },
                    placeholder = { Text("dd/MM/yyyy") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = projectEndDate.text.isNotEmpty() && parseDate(projectEndDate.text) == null
                )

                Button(
                    onClick = {
                        val name = projectName.text.trim()
                        val description = projectDescription.text.trim()
                        val startDate = parseDate(projectStartDate.text)
                        val endDate = parseDate(projectEndDate.text)

                        if (name.isNotEmpty() && description.isNotEmpty() && startDate != null && endDate != null) {
                            coroutineScope.launch {
                                saveProjectToFirestore(
                                    context,
                                    currentUser,
                                    name,
                                    description,
                                    startDate,
                                    endDate
                                ) {
                                    projectName = TextFieldValue("")
                                    projectDescription = TextFieldValue("")
                                    projectStartDate = TextFieldValue("")
                                    projectEndDate = TextFieldValue("")

                                    coroutineScope.launch {
                                        refreshProjects(
                                            firestore,
                                            currentUser,
                                            onProjectsLoaded = { loadedProjects ->
                                                projects = loadedProjects
                                            },
                                            onError = { error ->
                                                errorMessage = error
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Molimo popunite sva polja ispravno.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Spremi projekt")
                }

            }
        }
    )
}

fun parseDate(dateString: String): Date? {
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString)
    } catch (e: Exception) {
        null
    }
}

suspend fun refreshProjects(
    firestore: FirebaseFirestore,
    currentUser: FirebaseUser?,
    onProjectsLoaded: (List<Map<String, Any>>) -> Unit,
    onError: (String) -> Unit
) {
    if (currentUser != null) {
        try {
            val projectDocs = firestore.collection("users")
                .document(currentUser.uid)
                .collection("Projekti")
                .get()
                .await()

            val loadedProjects = projectDocs.documents.map { doc ->
                val data = doc.data ?: emptyMap<String, Any>()
                data + ("id" to doc.id)
            }
            onProjectsLoaded(loadedProjects)
        } catch (e: Exception) {
            onError(e.message ?: "Greška pri učitavanju projekata")
        }
    } else {
        onError("Korisnik nije prijavljen.")
    }
}

suspend fun saveProjectToFirestore(
    context: Context,
    currentUser: FirebaseUser?,
    name: String,
    description: String,
    startDate: Date,
    endDate: Date,
    onComplete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val projectData = mapOf(
        "Naziv" to name,
        "Opis" to description,
        "Pocetak" to startDate,
        "Kraj" to endDate
    )

    db.collection("users").document(currentUser?.uid ?: "")
        .collection("Projekti").add(projectData)
        .addOnSuccessListener {
            Toast.makeText(context, "Projekt spremljen!", Toast.LENGTH_SHORT).show()
            NotificationHelper.showNotification(
                context,
                "Projekt stvoren",
                "Projekt '$name' je uspješno stvoren."
            )
            onComplete()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Greška pri spremanju projekta: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

suspend fun deleteProjectFromFirestore(
    context: Context,
    currentUser: FirebaseUser?,
    projectId: String,
    onComplete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("users").document(currentUser?.uid ?: "")
        .collection("Projekti").document(projectId)
        .delete()
        .addOnSuccessListener {
            Toast.makeText(context, "Projekt obrisan!", Toast.LENGTH_SHORT).show()
            onComplete()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Greška pri brisanju projekta: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

@Composable
fun ProjectCard(project: Map<String, Any>, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Naziv: ${project["Naziv"]}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Opis: ${project["Opis"]}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Početak: ${SimpleDateFormat("dd/MM/yyyy").format((project["Pocetak"] as com.google.firebase.Timestamp).toDate())}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Kraj: ${SimpleDateFormat("dd/MM/yyyy").format((project["Kraj"] as com.google.firebase.Timestamp).toDate())}",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onDeleteClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("Obriši projekt")
            }
        }
    }
}
