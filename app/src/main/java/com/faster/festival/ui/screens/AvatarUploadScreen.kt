package com.faster.festival.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.ui.components.AvatarDisplay
import com.faster.festival.ui.viewmodel.ProfileEditUiState
import com.faster.festival.ui.viewmodel.ProfileEditViewModel
import java.io.File

/**
 * Avatar Upload Screen
 * Allows users to select and upload a profile avatar
 * Supports camera and gallery selection with runtime permission check
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarUploadScreen(
    currentAvatarUrl: String? = null,
    userName: String? = null,
    viewModel: ProfileEditViewModel? = null,
    onBackClick: () -> Unit = {},
    onUploadSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ✅ Safely instantiate ViewModel with factory if not provided
    val actualViewModel = viewModel ?: run {
        val factory = ProfileEditViewModel.Factory(
            profileRepository = com.faster.festival.di.NetworkModule.profileRepository,
            sessionManager = com.faster.festival.data.local.EncryptedSessionManager(
                androidx.compose.ui.platform.LocalContext.current
            )
        )
        viewModel(factory = factory)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // ✅ Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - launch camera
            launchCameraCapture(context, selectedImageUri) { uri ->
                selectedImageUri = uri
            }
        } else {
            // Permission denied - show settings dialog
            showPermissionDialog = true
        }
    }

    // Camera launcher with EXTRA_OUTPUT for direct file write
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            // ✅ Photo captured successfully - compress before upload
            try {
                val compressedFile = compressImageToMaxSize(context, selectedImageUri!!)
                if (compressedFile != null) {
                    selectedImageUri = Uri.fromFile(compressedFile)
                    android.util.Log.d("AvatarUploadScreen", "Image compressed to ${compressedFile.length()} bytes")
                } else {
                    actualViewModel.setError("Failed to compress image")
                }
            } catch (e: Exception) {
                android.util.Log.e("AvatarUploadScreen", "Compression failed", e)
                actualViewModel.setError("Failed to compress image: ${e.message}")
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // ✅ Image selected from gallery - compress before upload
            try {
                val compressedFile = compressImageToMaxSize(context, uri)
                if (compressedFile != null) {
                    selectedImageUri = Uri.fromFile(compressedFile)
                    android.util.Log.d("AvatarUploadScreen", "Image compressed to ${compressedFile.length()} bytes")
                } else {
                    actualViewModel.setError("Failed to compress image")
                }
            } catch (e: Exception) {
                android.util.Log.e("AvatarUploadScreen", "Compression failed", e)
                actualViewModel.setError("Failed to compress image: ${e.message}")
            }
        }
    }

    val editState by actualViewModel.editState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Change Avatar",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.Close, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Current Avatar Preview
            if (selectedImageUri != null) {
                Text(
                    "Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            } else if (!currentAvatarUrl.isNullOrBlank()) {
                Text(
                    "Current Avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                AvatarDisplay(
                    avatarUrl = currentAvatarUrl,
                    userName = userName,
                    size = 150.dp,
                    onEditClick = {},
                    showEditButton = false,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Upload Options
            Text(
                "Choose Image Source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Camera Button
                UploadOptionButton(
                    icon = Icons.Filled.CameraAlt,
                    label = "Camera",
                    onClick = {
                        // ✅ Check CAMERA permission at runtime
                        val cameraPermission = android.Manifest.permission.CAMERA
                        val permissionStatus = ContextCompat.checkSelfPermission(context, cameraPermission)

                        if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            // Permission already granted - launch camera
                            try {
                                launchCameraCapture(context, selectedImageUri) { uri ->
                                    selectedImageUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AvatarUploadScreen", "Failed to launch camera", e)
                                actualViewModel.setError("Camera not available: ${e.message}")
                            }
                        } else {
                            // Request permission
                            permissionLauncher.launch(cameraPermission)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Gallery Button
                UploadOptionButton(
                    icon = Icons.Filled.Collections,
                    label = "Gallery",
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ Permission Denied Dialog
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("Camera Permission Required") },
                    text = {
                        Text(
                            "This app needs camera permission to capture photos for your avatar. " +
                            "Please enable it in app settings."
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showPermissionDialog = false
                            // Open app settings
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }) {
                            Text("Open Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // ...existing code...
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column {
                            Text(
                                "Recommended:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Square image, at least 400x400px",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // State Messages
            when (editState) {
                is ProfileEditUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is ProfileEditUiState.Success -> {
                    val successState = editState as? ProfileEditUiState.Success
                    if (successState != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Green.copy(alpha = 0.1f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.Green
                                ).brush
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    successState.message,
                                    color = Color.Green.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(2000)
                            onUploadSuccess()
                        }
                    }
                }
                is ProfileEditUiState.Error -> {
                    val errorState = editState as? ProfileEditUiState.Error
                    if (errorState != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        errorState.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                ProfileEditUiState.Idle -> {}
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Upload Button
            Button(
                onClick = {
                    selectedImageUri?.let { uri ->
                        // ✅ Image already compressed by gallery/camera launchers
                        val file = uriToFile(context, uri)
                        if (file != null && file.exists()) {
                            // ✅ Call ViewModel to upload compressed file
                            actualViewModel.uploadAvatar(file)
                        } else {
                            actualViewModel.setError("Image file not found")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = selectedImageUri != null && editState !is ProfileEditUiState.Loading
            ) {
                if (editState is ProfileEditUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Upload Avatar")
                }
            }

            // Cancel Button
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = editState !is ProfileEditUiState.Loading
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * ✅ Helper function: Convert URI to File
 * Reads the URI content and creates a temporary file for upload
 */
private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("avatar", ".jpg", context.cacheDir)

        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()

        tempFile
    } catch (e: Exception) {
        android.util.Log.e("AvatarUploadScreen", "Failed to convert URI to File", e)
        null
    }
}

/**
 * ✅ Helper function: Launch camera capture with proper FileProvider URI
 */
private fun launchCameraCapture(
    context: android.content.Context,
    currentUri: Uri?,
    onUriCreated: (Uri) -> Unit
) {
    try {
        val tempFile = File.createTempFile(
            "avatar_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )

        onUriCreated(uri)
    } catch (e: Exception) {
        android.util.Log.e("AvatarUploadScreen", "Failed to create camera URI", e)
        throw e
    }
}

/**
 * ✅ Compress image to max 5MB for API constraints
 * Uses android.graphics.Bitmap for compression
 * Returns compressed file or null if compression fails
 */
private fun compressImageToMaxSize(
    context: android.content.Context,
    imageUri: Uri,
    maxSizeBytes: Long = 5 * 1024 * 1024 // 5MB
): File? {
    return try {
        // Read image file
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: return null

        // Decode bitmap
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            ?: return null
        inputStream.close()

        // Check if compression needed
        if (originalBitmap.byteCount <= maxSizeBytes) {
            // Image already small enough, just save to cache
            val cachedFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}_compressed.jpg")
            cachedFile.outputStream().use { out ->
                originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }
            originalBitmap.recycle()
            return cachedFile
        }

        // Compress iteratively
        var quality = 90
        var outputFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}_compressed.jpg")
        var fileSize: Long

        do {
            outputFile.delete()
            outputFile.outputStream().use { out ->
                originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            }
            fileSize = outputFile.length()

            // Reduce quality if still too large
            if (fileSize > maxSizeBytes && quality > 20) {
                quality -= 10
            } else {
                break
            }
        } while (fileSize > maxSizeBytes && quality > 20)

        // Scale down if still too large
        if (fileSize > maxSizeBytes) {
            val scaleFactor = 0.8
            val newWidth = (originalBitmap.width * scaleFactor).toInt()
            val newHeight = (originalBitmap.height * scaleFactor).toInt()

            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap,
                newWidth,
                newHeight,
                true
            )

            quality = 85
            do {
                outputFile.delete()
                outputFile.outputStream().use { out ->
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
                }
                fileSize = outputFile.length()

                if (fileSize > maxSizeBytes && quality > 20) {
                    quality -= 10
                } else {
                    break
                }
            } while (fileSize > maxSizeBytes && quality > 20)

            scaledBitmap.recycle()
        }

        originalBitmap.recycle()

        android.util.Log.d("AvatarUploadScreen", "Image compressed to ${outputFile.length()} bytes (quality: $quality)")
        outputFile
    } catch (e: Exception) {
        android.util.Log.e("AvatarUploadScreen", "Image compression failed", e)
        null
    }
}

/**
 * Upload Option Button Component
 */
@Composable
fun UploadOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
