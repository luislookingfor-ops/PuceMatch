package com.example.pucematch.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pucematch.PuceMatchApplication

@Composable
fun EditProfileScreenStateful(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as PuceMatchApplication
    val viewModel: EditProfileViewModel = viewModel(
        factory = EditProfileViewModel.provideFactory(app.repository, app.getCurrentUserId())
    )

    val name by viewModel.name.collectAsState()
    val career by viewModel.career.collectAsState()
    val bio by viewModel.bio.collectAsState()
    val interests by viewModel.interests.collectAsState()
    val matchType by viewModel.matchType.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    // Regresar al Home tras guardar exitosamente
    LaunchedEffect(isSaved) {
        if (isSaved) {
            navController.popBackStack()
        }
    }

    EditProfileScreenStateless(
        name = name,
        career = career,
        bio = bio,
        interests = interests,
        matchType = matchType,
        avatarUri = avatarUri,
        onNameChange = { viewModel.onNameChange(it) },
        onCareerChange = { viewModel.onCareerChange(it) },
        onBioChange = { viewModel.onBioChange(it) },
        onInterestsChange = { viewModel.onInterestsChange(it) },
        onMatchTypeChange = { viewModel.onMatchTypeChange(it) },
        onAvatarChange = { viewModel.onAvatarChange(it) },
        onSaveClick = { viewModel.saveProfile() },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreenStateless(
    name: String,
    career: String,
    bio: String,
    interests: String,
    matchType: String,
    avatarUri: String?,
    onNameChange: (String) -> Unit,
    onCareerChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onInterestsChange: (String) -> Unit,
    onMatchTypeChange: (String) -> Unit,
    onAvatarChange: (String?) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Abre directamente la galería del dispositivo para elegir una foto
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = com.example.pucematch.ui.utils.convertUriToBase64(context, it)
            onAvatarChange(base64)
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Editar Perfil",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Selector de Imagen de Perfil
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            // Abrir la galería del dispositivo para seleccionar foto
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        val bitmap = remember(avatarUri) {
                            com.example.pucematch.ui.utils.loadProfileBitmap(avatarUri)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Editar foto",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Cargar foto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "Toca la imagen para cambiar tu foto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Nombre
                        OutlinedTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = { Text("Nombre Completo") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Carrera
                        OutlinedTextField(
                            value = career,
                            onValueChange = onCareerChange,
                            label = { Text("Carrera") },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Intereses
                        OutlinedTextField(
                            value = interests,
                            onValueChange = onInterestsChange,
                            label = { Text("Intereses (ej: Kotlin,UI,Git)") },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Biografía
                        OutlinedTextField(
                            value = bio,
                            onValueChange = onBioChange,
                            label = { Text("Biografía") },
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Categoría de Interés
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Categoría de Conexión",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Educativo", "Recreacional", "Sentimental").forEach { type ->
                                    FilterChip(
                                        selected = matchType == type,
                                        onClick = { onMatchTypeChange(type) },
                                        label = { Text(type, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón Guardar
                Button(
                    onClick = onSaveClick,
                    enabled = name.isNotBlank() && career.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Guardar Cambios",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
