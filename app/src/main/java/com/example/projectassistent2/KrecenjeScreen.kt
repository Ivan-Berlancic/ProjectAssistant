package com.example.projectassistent2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun KrecenjeScreen(onBackClick: () -> Unit) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val userMaterials = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val paintCalculation = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val paintCosts = remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    var area by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            firestore.collection("Materijali")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val materials = document.data?.mapValues { (key, value) ->
                        (value as Long).toInt()
                    } ?: emptyMap()
                    userMaterials.value = materials.filterKeys { it !in listOf("Cement", "Pijesak", "Vapno") }
                }
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
                    text = "Krečenje",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                TextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Unesite kvadraturu za krečenje") },
                    trailingIcon = { Text("m²") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val areaValue = area.toDoubleOrNull() ?: 0.0
                        if (currentUser != null) {
                            paintCalculation.value = calculatePaintNeeded(areaValue, userMaterials.value)
                            paintCosts.value = calculatePaintCost(paintCalculation.value, userMaterials.value)
                        } else {
                            paintCalculation.value = calculatePaintNeeded(areaValue, mapOf(
                                "Bijela" to 0, "Siva" to 0, "Crvena" to 0, "Zelena" to 0, "Plava" to 0
                            ))
                            paintCosts.value = calculatePaintCost(paintCalculation.value, mapOf())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Izračunaj količinu boje")
                }

                if (paintCalculation.value.isNotEmpty()) {
                    Text(
                        text = "Rezultati izračuna:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    paintCalculation.value.forEach { (paint, requiredLiters) ->
                        val userLiters = userMaterials.value[paint] ?: 0
                        val neededLiters = maxOf(0, requiredLiters - userLiters)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$paint: $requiredLiters L",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Nedostaje: $neededLiters L",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (neededLiters == 0) Color.Green else Color.Red
                            )
                        }
                    }

                    Text(
                        text = "Aproksimacija troškova:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    paintCosts.value.forEach { (paint, cost) ->
                        Text(
                            text = "$paint: ${cost}€",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Korisnik: ${currentUser?.email ?: "Gost"}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    )
}

private fun calculatePaintNeeded(area: Double, userMaterials: Map<String, Int>): Map<String, Int> {

    val coveragePerLiter = 10.0

    return userMaterials.keys.associateWith { paint ->
        (area / coveragePerLiter).toInt()
    }
}

private fun calculatePaintCost(
    paintCalculation: Map<String, Int>,
    userMaterials: Map<String, Int>
): Map<String, Double> {
    val prices = mapOf(
        "Crvena" to 5.0,
        "Crna" to 5.0,
        "Plava" to 4.5,
        "Zelena" to 4.0,
        "Žuta" to 3.5,
        "Bijela" to 3.0,
        "Siva" to 3.5
    )

    return paintCalculation.map { (paint, requiredLiters) ->
        val userLiters = userMaterials[paint] ?: 0
        val neededLiters = maxOf(0, requiredLiters - userLiters)
        val cost = (prices[paint] ?: 0.0) * neededLiters
        paint to cost
    }.toMap()
}

