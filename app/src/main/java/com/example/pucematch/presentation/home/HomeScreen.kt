package com.example.pucematch.presentation.home

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pucematch.PuceMatchApplication
import com.example.pucematch.data.local.StudentEntity
import com.example.pucematch.domain.Screen
import com.example.pucematch.ui.utils.copyUriToInternalStorage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreenStateful(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as PuceMatchApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(app.repository, app.getCurrentUserId())
    )

    val isSwipeViewMode by viewModel.isSwipeViewMode.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val filteredProfiles by viewModel.filteredProfiles.collectAsState()
    val swipeCardProfiles by viewModel.swipeCardProfiles.collectAsState()
    val activeMatches by viewModel.activeMatches.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    HomeScreenStateless(
        isSwipeViewMode = isSwipeViewMode,
        activeTab = activeTab,
        filteredProfiles = filteredProfiles,
        swipeCardProfiles = swipeCardProfiles,
        activeMatches = activeMatches,
        isRefreshing = isRefreshing,
        onSwipeViewModeToggle = { viewModel.setSwipeViewMode(!isSwipeViewMode) },
        onTabSelect = { viewModel.setActiveTab(it) },
        onSwipeLeft = { student -> viewModel.swipeLeft(student.id) },
        onSwipeRight = { student, onMatchResult -> viewModel.swipeRight(student, onMatchResult) },
        onReloadClick = { viewModel.resetSwipes() },
        onNavigateToChat = { matchId ->
            navController.navigate(Screen.ChatDetail(matchId))
        },
        onNavigateToEditProfile = {
            navController.navigate(Screen.EditProfile)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenStateless(
    isSwipeViewMode: Boolean,
    activeTab: String,
    filteredProfiles: List<StudentEntity>,
    swipeCardProfiles: List<StudentEntity>,
    activeMatches: List<StudentEntity>,
    isRefreshing: Boolean,
    onSwipeViewModeToggle: () -> Unit,
    onTabSelect: (String) -> Unit,
    onSwipeLeft: (StudentEntity) -> Unit,
    onSwipeRight: (StudentEntity, (Boolean, String?) -> Unit) -> Unit,
    onReloadClick: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var matchDialogProfile by remember { mutableStateOf<StudentEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "PuceMatch",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Editar Perfil"
                        )
                    }
                    IconButton(onClick = onSwipeViewModeToggle) {
                        Icon(
                            imageVector = if (isSwipeViewMode) Icons.AutoMirrored.Filled.List else Icons.Default.Favorite,
                            contentDescription = if (isSwipeViewMode) "Modo Lista" else "Modo Carrusel/Swipe"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Selector de Pestañas de Match (Material You Tabs)
            TabRow(
                selectedTabIndex = listOf("Educativo", "Recreacional", "Sentimental").indexOf(activeTab),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                listOf("Educativo", "Recreacional", "Sentimental").forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { onTabSelect(tab) },
                        text = { Text(tab, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // 2. Carrusel Horizontal de Matches Activos (Tinder Gold Style)
            if (activeMatches.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "Matches Activos (${activeMatches.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeMatches, key = { it.id }) { match ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onNavigateToChat(match.id) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (match.avatarUri != null) {
                                        val bitmap = remember(match.avatarUri) {
                                            try {
                                                BitmapFactory.decodeFile(match.avatarUri)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = match.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = match.name.take(2).uppercase(),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = match.name.take(2).uppercase(),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = match.name.split(" ").firstOrNull() ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // 3. Contenedor Principal (Swipe vs List)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (isSwipeViewMode) {
                    // MODO SWIPE CARDS
                    if (swipeCardProfiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "¡Fin del catálogo en $activeTab!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Has visto todos los perfiles de este grupo de interés.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onReloadClick) {
                                    Text("Recargar catálogo")
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currentTopProfile = swipeCardProfiles.lastOrNull()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (swipeCardProfiles.size > 1) {
                                    val secondProfile = swipeCardProfiles[swipeCardProfiles.lastIndex - 1]
                                    ProfileCard(
                                        profile = secondProfile,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 16.dp)
                                            .graphicsLayer {
                                                scaleX = 0.93f
                                                scaleY = 0.93f
                                                alpha = 0.6f
                                            },
                                        isSwipeable = false,
                                        onSwipeLeft = {},
                                        onSwipeRight = {},
                                        onCardClick = {}
                                    )
                                }

                                if (currentTopProfile != null) {
                                    key(currentTopProfile.id) {
                                        ProfileCard(
                                            profile = currentTopProfile,
                                            modifier = Modifier.fillMaxSize(),
                                            isSwipeable = true,
                                            onSwipeLeft = {
                                                onSwipeLeft(currentTopProfile)
                                            },
                                            onSwipeRight = {
                                                onSwipeRight(currentTopProfile) { isMatch, _ ->
                                                    if (isMatch) {
                                                        matchDialogProfile = currentTopProfile
                                                    }
                                                }
                                            },
                                            onCardClick = {
                                                // Check for match before opening chat
                                                if (currentTopProfile.isMatched) {
                                                    onNavigateToChat(currentTopProfile.id)
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            "Primero debes deslizar a la derecha y hacer match para chatear."
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Botones inferiores de acción
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        if (currentTopProfile != null) {
                                            onSwipeLeft(currentTopProfile)
                                        }
                                    },
                                    modifier = Modifier.size(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Descartar",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        if (currentTopProfile != null) {
                                            onSwipeRight(currentTopProfile) { isMatch, _ ->
                                                if (isMatch) {
                                                    matchDialogProfile = currentTopProfile
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Me gusta",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // MODO LISTA
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredProfiles,
                            key = { it.id }
                        ) { profile ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (profile.isMatched) {
                                            onNavigateToChat(profile.id)
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Acceso Denegado: Debes hacer match con ${profile.name} en el carrusel."
                                                )
                                            }
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar del estudiante
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (profile.avatarUri != null) {
                                            val bitmap = remember(profile.avatarUri) {
                                                try {
                                                    BitmapFactory.decodeFile(profile.avatarUri)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = profile.name.take(2).uppercase(),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = profile.name.take(2).uppercase(),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(text = profile.career, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Chips de intereses
                                        val interestList = profile.interests.split(",")
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            items(interestList) { interest ->
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(interest, fontSize = 10.sp) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Indicador de bloqueo/chat (Defensa de Match)
                                    Box(modifier = Modifier.padding(start = 8.dp)) {
                                        if (profile.isMatched) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Match Activo",
                                                tint = Color(0xFF4CAF50)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Bloqueado",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

        // 4. Modal Diálogo "¡Es un Match!"
        matchDialogProfile?.let { matchedProfile ->
            AlertDialog(
                onDismissRequest = { matchDialogProfile = null },
                confirmButton = {
                    Button(
                        onClick = {
                            matchDialogProfile = null
                            onNavigateToChat(matchedProfile.id)
                        }
                    ) {
                        Text("Chatear ahora")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { matchDialogProfile = null }) {
                        Text("Seguir deslizando")
                    }
                },
                title = {
                    Text(
                        "¡Es un Match! 🎉",
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 22.sp
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (matchedProfile.avatarUri != null) {
                                val bitmap = remember(matchedProfile.avatarUri) {
                                    try {
                                        BitmapFactory.decodeFile(matchedProfile.avatarUri)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = matchedProfile.name.take(2).uppercase(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = matchedProfile.name.take(2).uppercase(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Has coincidido con ${matchedProfile.name} para compartir fines de tipo $activeTab.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }

    }
}

@Composable
fun ProfileCard(
    profile: StudentEntity,
    isSwipeable: Boolean,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val offsetXAnimated by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "offsetX"
    )
    val offsetYAnimated by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "offsetY"
    )

    val rotationZ = (offsetXAnimated / 1000f) * 45f

    val nameHash = profile.name.hashCode()
    val colors = listOf(
        Color(0xFF003554).copy(alpha = 0.85f),
        Color(nameHash or 0xFF000000.toInt()).copy(alpha = 0.9f),
        MaterialTheme.colorScheme.primary
    )
    val avatarGradient = Brush.verticalGradient(colors)

    Card(
        modifier = modifier
            .offset { IntOffset(offsetXAnimated.roundToInt(), offsetYAnimated.roundToInt()) }
            .graphicsLayer {
                this.rotationZ = rotationZ
            }
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onCardClick)
            .then(
                if (isSwipeable) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            },
                            onDragEnd = {
                                if (offsetX > 300f) {
                                    onSwipeRight()
                                } else if (offsetX < -300f) {
                                    onSwipeLeft()
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        )
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(avatarGradient),
                contentAlignment = Alignment.Center
            ) {
                // Dibujar foto de perfil cargada localmente si existe
                if (profile.avatarUri != null) {
                    val bitmap = remember(profile.avatarUri) {
                        try {
                            BitmapFactory.decodeFile(profile.avatarUri)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback a iniciales gigantes
                        val initials = profile.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
                        Text(
                            text = initials,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                } else {
                    val initials = profile.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
                    Text(
                        text = initials,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }

                // Indicador de "LIKE" o "NOPE" al arrastrar
                if (offsetXAnimated > 50f) {
                    val alpha = (offsetXAnimated / 300f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(24.dp)
                            .graphicsLayer { this.alpha = alpha }
                            .shadow(4.dp, shape = RoundedCornerShape(8.dp))
                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "MATCH",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                } else if (offsetXAnimated < -50f) {
                    val alpha = (-offsetXAnimated / 300f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .graphicsLayer { this.alpha = alpha }
                            .shadow(4.dp, shape = RoundedCornerShape(8.dp))
                            .background(Color(0xFFF44336), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SKIP",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (profile.isMatched) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Match",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = profile.career,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Intereses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val interestList = remember(profile.interests) { profile.interests.split(",") }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = interestList,
                            key = { it }
                        ) { interest ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(interest) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
