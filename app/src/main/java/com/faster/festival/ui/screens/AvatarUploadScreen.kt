package com.faster.festival.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.ui.viewmodel.ProfileEditUiState
import com.faster.festival.ui.viewmodel.ProfileEditViewModel
import java.io.File

// Light theme palette (consistent with HomeScreen)
private val AvatarBg = Color(0xFFF7F7F7)
private val AvatarCardBg = Color(0xFFF5F5F5)
private val AvatarWhite = Color.White
private val AvatarCoralRed = Color(0xFFE53935)
private val AvatarDarkNavy = Color(0xFF0D1B2A)
private val AvatarTextDark = Color(0xFF222222)
private val AvatarTextMedium = Color(0xFF333333)
private val AvatarTextLight = Color(0xFF666666)
private val AvatarBorderLight = Color(0xFFE0E0E0)

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraCapture(context, selectedImageUri) { uri ->
                selectedImageUri = uri
            }
        } else {
            showPermissionDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
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
            .background(AvatarBg)
    ) {
        // ── Top Bar ─────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Change Avatar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AvatarTextDark
                    )
                    Text(
                        text = "Update your profile photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = AvatarTextLight,
                        letterSpacing = 0.5.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AvatarTextDark
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AvatarBg
            )
        )

        // ── Content ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Avatar Preview ──────────────────────────────────────
            if (selectedImageUri != null) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AvatarTextDark,
                    modifier = Modifier.align(Alignment.Start)
                )

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(AvatarBorderLight)
                        .border(
                            width = 3.dp,
                            color = AvatarCoralRed,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (!currentAvatarUrl.isNullOrBlank()) {
                Text(
                    text = "Current Avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AvatarTextDark,
                    modifier = Modifier.align(Alignment.Start)
                )

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(AvatarBorderLight)
                        .border(
                            width = 3.dp,
                            color = AvatarBorderLight,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentAvatarUrl,
                        contentDescription = "Current avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // No avatar placeholder
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(AvatarBorderLight)
                        .border(
                            width = 3.dp,
                            color = AvatarBorderLight,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AvatarTextLight,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            // ── Upload Options ──────────────────────────────────────
            Text(
                text = "Choose Source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AvatarTextDark,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Camera Button
                AvatarSourceCard(
                    icon = Icons.Filled.CameraAlt,
                    label = "Camera",
                    cardBg = AvatarWhite,
                    iconTint = AvatarCoralRed,
                    onClick = {
                        val cameraPermission = android.Manifest.permission.CAMERA
                        val permissionStatus = ContextCompat.checkSelfPermission(context, cameraPermission)

                        if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
                            permissionLauncher.launch(cameraPermission)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Gallery Button
                AvatarSourceCard(
                    icon = Icons.Filled.Collections,
                    label = "Gallery",
                    cardBg = AvatarWhite,
                    iconTint = AvatarDarkNavy,
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Info Card ───────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = AvatarWhite)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(18.dp),
                        tint = AvatarTextLight
                    )
                    Column {
                        Text(
                            "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AvatarTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Square image, at least 400x400px",
                            style = MaterialTheme.typography.labelSmall,
                            color = AvatarTextLight
                        )
                    }
                }
            }

            // ── Permission Denied Dialog ────────────────────────────
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    containerColor = AvatarWhite,
                    titleContentColor = AvatarTextDark,
                    textContentColor = AvatarTextMedium,
                    title = { Text("Camera Permission Required") },
                    text = {
                        Text(
                            "This app needs camera permission to capture photos for your avatar. " +
                            "Please enable it in app settings."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPermissionDialog = false
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AvatarCoralRed)
                        ) {
                            Text("Open Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text("Cancel", color = AvatarTextLight)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── State Messages ──────────────────────────────────────
            when (editState) {
                is ProfileEditUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AvatarCoralRed
                        )
                    }
                }
                is ProfileEditUiState.Success -> {
                    val successState = editState as? ProfileEditUiState.Success
                    if (successState != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    successState.message,
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
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
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = AvatarCoralRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    errorState.message,
                                    color = AvatarCoralRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                ProfileEditUiState.Idle -> {}
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Upload Button ───────────────────────────────────────
            Button(
                onClick = {
                    selectedImageUri?.let { uri ->
                        val file = uriToFile(context, uri)
                        if (file != null && file.exists()) {
                            actualViewModel.uploadAvatar(file)
                        } else {
                            actualViewModel.setError("Image file not found")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = selectedImageUri != null && editState !is ProfileEditUiState.Loading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AvatarCoralRed,
                    contentColor = Color.White,
                    disabledContainerColor = AvatarCoralRed.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                if (editState is ProfileEditUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Upload Avatar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // ── Cancel Button ───────────────────────────────────────
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = editState !is ProfileEditUiState.Loading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AvatarTextMedium,
                    disabledContentColor = AvatarTextLight
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.linearGradient(
                        colors = listOf(AvatarBorderLight, AvatarBorderLight)
                    )
                )
            ) {
                Text(
                    "Cancel",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Source Card for Camera / Gallery option
 */
@Composable
private fun AvatarSourceCard(
    icon: ImageVector,
    label: String,
    cardBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = iconTint
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = AvatarTextDark
            )
        }
    }
}

/**
 * Helper function: Convert URI to File
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
 * Helper function: Launch camera capture with proper FileProvider URI
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
 * Compress image to max 5MB for API constraints
 */
private fun compressImageToMaxSize(
    context: android.content.Context,
    imageUri: Uri,
    maxSizeBytes: Long = 5 * 1024 * 1024 // 5MB
): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: return null

        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            ?: return null
        inputStream.close()

        if (originalBitmap.byteCount <= maxSizeBytes) {
            val cachedFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}_compressed.jpg")
            cachedFile.outputStream().use { out ->
                originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }
            originalBitmap.recycle()
            return cachedFile
        }

        var quality = 90
        var outputFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}_compressed.jpg")
        var fileSize: Long

        do {
            outputFile.delete()
            outputFile.outputStream().use { out ->
                originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            }
            fileSize = outputFile.length()

            if (fileSize > maxSizeBytes && quality > 20) {
                quality -= 10
            } else {
                break
            }
        } while (fileSize > maxSizeBytes && quality > 20)

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
