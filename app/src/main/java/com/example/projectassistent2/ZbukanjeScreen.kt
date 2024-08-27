import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun ZbukanjeScreen(onBackClick: () -> Unit) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val userMaterials = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val materialCalculation = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val materialCosts = remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    var area by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("Grubo žbukanje") }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            firestore.collection("Materijali")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val materials = document.data?.mapValues { (key, value) ->
                        (value as Long).toInt()
                    } ?: emptyMap()
                    userMaterials.value = materials
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
                    text = "Žbukanje",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                TextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Unesite kvadraturu za žbukanje") },
                    trailingIcon = { Text("m²") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Odaberite vrstu žbukanja:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedOption == "Grubo žbukanje",
                                onClick = { selectedOption = "Grubo žbukanje" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Grubo žbukanje", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedOption == "Fino žbukanje",
                                onClick = { selectedOption = "Fino žbukanje" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Fino žbukanje", color = Color.White)
                        }
                    }
                }

                Button(
                    onClick = {
                        val areaValue = area.toDoubleOrNull() ?: 0.0
                        materialCalculation.value = calculateMaterials(areaValue, selectedOption)
                        materialCosts.value = calculateCost(materialCalculation.value, userMaterials.value)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Izračunaj količinu materijala")
                }

                if (materialCalculation.value.isNotEmpty()) {
                    Text(
                        text = "Rezultati izračuna:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    materialCalculation.value.forEach { (material, requiredQty) ->
                        val userQty = userMaterials.value[material] ?: 0
                        val neededQty = maxOf(0, requiredQty - userQty)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$material: $requiredQty kg",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Nedostaje: $neededQty kg",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (neededQty == 0) Color.Green else Color.Red
                            )
                        }
                    }

                    Text(
                        text = "Aproksimacija troškova:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    materialCosts.value.forEach { (material, cost) ->
                        Text(
                            text = "$material: ${cost}€",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Ukupno: ${materialCosts.value.values.sum()}€",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
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

private fun calculateMaterials(area: Double, type: String): Map<String, Int> {
    return when (type) {
        "Grubo žbukanje" -> mapOf(
            "Pijesak" to (area * 2).toInt(),
            "Cement" to (area * 1).toInt(),
            "Vapno" to (area * 1).toInt()
        )
        "Fino žbukanje" -> mapOf(
            "Pijesak" to (area * 1.5).toInt(),
            "Cement" to (area * 0.5).toInt(),
            "Vapno" to (area * 0.5).toInt()
        )
        else -> emptyMap()
    }
}

private fun calculateCost(
    materialCalculation: Map<String, Int>,
    userMaterials: Map<String, Int>
): Map<String, Double> {

    val prices = mapOf(
        "Pijesak" to 0.1,
        "Cement" to 0.2,
        "Vapno" to 0.15
    )

    return materialCalculation.map { (material, requiredQty) ->
        val userQty = userMaterials[material] ?: 0
        val neededQty = maxOf(0, requiredQty - userQty)
        val cost = (prices[material] ?: 0.0) * neededQty
        material to cost
    }.toMap()
}
