package com.example.projectassistent2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZbukanjeScreen(onBackClick: () -> Unit) {
    var area by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("Grubo žbukanje") }
    var materialCalculation by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Žbukanje") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2E2E2E))
                    .padding(paddingValues)
                    .padding(16.dp),
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
                        // Perform calculation based on the entered area and selected option
                        val areaValue = area.toDoubleOrNull() ?: 0.0
                        materialCalculation = calculateMaterials(areaValue, selectedOption)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Izračunaj količinu materijala")
                }

                if (materialCalculation.isNotEmpty()) {
                    Text(
                        text = "Rezultati izračuna:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    materialCalculation.forEach { (key, value) ->
                        Text(
                            text = "$key: $value",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                    // Approximate cost section
                    Text(
                        text = "Aproksimacija troškova:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Pijesak: 0€",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Cement: 0€",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Vapno: 0€",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Ukupno: 0€",
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
                    Image(
                        painter = painterResource(id = R.drawable.ic_user_profile),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Korisnik: Ivan Berlančić",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    )
}

private fun calculateMaterials(area: Double, type: String): Map<String, String> {
    return when (type) {
        "Grubo žbukanje" -> mapOf(
            "Pijesak" to "${(area * 2).toInt()} kg",
            "Cement" to "${(area * 1).toInt()} kg",
            "Vapno" to "${(area * 1).toInt()} kg"
        )
        "Fino žbukanje" -> mapOf(
            "Pijesak" to "${(area * 1.5).toInt()} kg",
            "Cement" to "${(area * 0.5).toInt()} kg",
            "Vapno" to "${(area * 0.5).toInt()} kg"
        )
        else -> emptyMap()
    }
}

