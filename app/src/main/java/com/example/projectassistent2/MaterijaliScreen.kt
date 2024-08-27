package com.example.projectassistent2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun MaterijaliScreen(navController: NavHostController, currentUser: FirebaseUser?) {
    val db = FirebaseFirestore.getInstance()
    val userUID = currentUser?.uid
    var materials by remember { mutableStateOf<Map<String, Any>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var cementInput by remember { mutableStateOf(TextFieldValue("")) }
    var pijesakInput by remember { mutableStateOf(TextFieldValue("")) }
    var vapnoInput by remember { mutableStateOf(TextFieldValue("")) }

    var paintName by remember { mutableStateOf(TextFieldValue("")) }
    var paintQuantity by remember { mutableStateOf(TextFieldValue("")) }
    val paintInputs = remember { mutableStateMapOf<String, TextFieldValue>() }

    LaunchedEffect(userUID) {
        if (userUID != null) {
            try {
                val document = db.collection("Materijali").document(userUID).get().await()
                if (document.exists()) {
                    materials = document.data

                    cementInput = TextFieldValue(document.get("Cement").toString())
                    pijesakInput = TextFieldValue(document.get("Pijesak").toString())
                    vapnoInput = TextFieldValue(document.get("Vapno").toString())

                    document.data?.forEach { (key, value) ->
                        if (key !in listOf("Cement", "Pijesak", "Vapno")) {
                            paintInputs[key] = TextFieldValue(value.toString())
                        }
                    }
                } else {
                    materials = emptyMap()
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentUser != null) {
                if (materials != null) {
                    Text("Moji materijali:")
                    Text("Cement: ${materials!!["Cement"]} kg")
                    Text("Pijesak: ${materials!!["Pijesak"]} kg")
                    Text("Vapno: ${materials!!["Vapno"]} kg")

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = cementInput,
                        onValueChange = { cementInput = it },
                        label = { Text("Cement") }
                    )
                    TextField(
                        value = pijesakInput,
                        onValueChange = { pijesakInput = it },
                        label = { Text("Pijesak") }
                    )
                    TextField(
                        value = vapnoInput,
                        onValueChange = { vapnoInput = it },
                        label = { Text("Vapno") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Dodaj novu boju")
                    TextField(
                        value = paintName,
                        onValueChange = { paintName = it },
                        label = { Text("Boja") }
                    )
                    TextField(
                        value = paintQuantity,
                        onValueChange = { paintQuantity = it },
                        label = { Text("Količina boje (litre)") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val name = paintName.text.trim()
                        val quantity = paintQuantity.text.toIntOrNull() ?: 0

                        if (name.isNotEmpty() && quantity > 0) {
                            paintInputs[name] = TextFieldValue(quantity.toString())
                            materials = materials?.plus(name to quantity)

                            db.collection("Materijali").document(userUID!!)
                                .update(name, quantity)
                                .addOnSuccessListener {
                                    paintName = TextFieldValue("")
                                    paintQuantity = TextFieldValue("")
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = e.message
                                }
                        }
                    }) {
                        Text("Dodaj boju")
                    }

                    paintInputs.forEach { (paint, input) ->
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = input,
                            onValueChange = { newValue -> paintInputs[paint] = newValue },
                            label = { Text(paint) }
                        )
                    }
                } else if (errorMessage != null) {
                    Text("Error: $errorMessage", color = Color.Red)
                } else {
                    Text("Loading...")
                }
            } else {
                Text("Please log in to view and add materials.", color = Color.Red)
            }
        }

        Button(
            onClick = {
                val newMaterials = linkedMapOf<String, Any>(
                    "Cement" to (cementInput.text.toIntOrNull() ?: 0),
                    "Pijesak" to (pijesakInput.text.toIntOrNull() ?: 0),
                    "Vapno" to (vapnoInput.text.toIntOrNull() ?: 0)
                ) + paintInputs.mapValues { it.value.text.toIntOrNull() ?: 0 }

                db.collection("Materijali").document(userUID!!)
                    .set(newMaterials)
                    .addOnSuccessListener {
                        materials = newMaterials
                    }
                    .addOnFailureListener { e ->
                        errorMessage = e.message
                    }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Dodaj/osvježi materijale")
        }
    }
}




