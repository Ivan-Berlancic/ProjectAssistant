package com.example.projectassistent2

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MojiRadoviScreen(navController: NavHostController, currentUser: FirebaseUser?) {
    val photos = remember { mutableStateListOf<Pair<String, String>>() }
    val context = LocalContext.current

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    fun allPermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val imageUri = photos.last().first
            val date = photos.last().second
            UploadPhotoToFirebase(context, currentUser, Uri.parse(imageUri), date) { downloadUri ->
                photos[photos.size - 1] = downloadUri to date
            }
        } else {
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsResult ->
        if (permissionsResult.values.all { it }) {
            val uri = createImageUri(context)
            if (uri != null) {
                photos.add(uri.toString() to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                takePictureLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
            }
        } else {
            val deniedPermissions = permissionsResult.filterValues { !it }.keys
            Toast.makeText(context, "The following permissions were denied: $deniedPermissions", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!allPermissionsGranted()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Moji radovi", style = MaterialTheme.typography.headlineLarge)

        Button(onClick = {
            if (allPermissionsGranted()) {
                val uri = createImageUri(context)
                if (uri != null) {
                    photos.add(uri.toString() to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    takePictureLauncher.launch(uri)
                } else {
                    Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
                }
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }) {
            Text("Add Photo")
        }

        photos.forEach { (uri, date) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(painter = rememberAsyncImagePainter(uri), contentDescription = null, modifier = Modifier.size(200.dp))
                Text(text = "Date: $date", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun createImageUri(context: Context): Uri? {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }
    }

    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
}

fun UploadPhotoToFirebase(
    context: Context,
    currentUser: FirebaseUser?,
    uri: Uri,
    date: String,
    onUploadComplete: (String) -> Unit
) {
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference.child("users/${currentUser?.uid}/photos/${uri.lastPathSegment}")

    val uploadTask = storageRef.putFile(uri)
    uploadTask.addOnSuccessListener {
        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
            val db = FirebaseFirestore.getInstance()
            val photoData = mapOf(
                "uri" to downloadUri.toString(),
                "date" to date
            )
            db.collection("users").document(currentUser?.uid ?: "")
                .collection("photos").add(photoData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Photo uploaded and saved!", Toast.LENGTH_SHORT).show()
                    onUploadComplete(downloadUri.toString())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Failed to upload photo: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}