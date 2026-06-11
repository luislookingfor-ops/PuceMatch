package com.example.pucematch.presentation.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pucematch.data.local.AppDatabase
import com.example.pucematch.data.local.StudentEntity
import com.example.pucematch.domain.Screen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreenStateful(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val profiles by database.userDao().getAllProfiles().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    // Semilla de perfiles por defecto si la base de datos está vacía
    LaunchedEffect(profiles) {
        if (profiles.isEmpty()) {
            val seedProfiles = listOf(
                StudentEntity(
                    id = "1",
                    name = "Yulieth Galarza",
                    career = "Ingeniería en Software",
                    interests = "Kotlin,Jetpack Compose,UI Design,Android,Material Design 3",
                    bio = "Desarrolladora de UI interactiva. Me encanta crear interfaces fluidas y componentes optimizados."
                ),
                StudentEntity(
                    id = "2",
                    name = "Jorge López",
                    career = "Ingeniería en Software",
                    interests = "Room,Clean Architecture,SQL,Kotlin,Coroutines",
                    bio = "Enfocado en base de datos locales e infraestructura robusta. Offline-first lover."
                ),
                StudentEntity(
                    id = "3",
                    name = "Kevin Cevallos",
                    career = "Ingeniería en Sistemas",
                    interests = "Retrofit,APIs,Git,Backend Integration,Testing",
                    bio = "Especialista en integración remota y consumo de servicios HTTP fiables."
                ),
                StudentEntity(
                    id = "4",
                    name = "María Belén",
                    career = "Diseño Multimedios",
                    interests = "Figma,UX Research,Branding,Illustrator,Visual Design",
                    bio = "Diseñadora de interfaces digitales buscando crear la mejor experiencia estudiantil."
                ),
                StudentEntity(
                    id = "5",
                    name = "Daniel Proaño",
                    career = "Negocios Internacionales",
                    interests = "Marketing,Finanzas,Liderazgo,Idiomas,Tech Startups",
                    bio = "Emprendedor tecnológico. Me interesa el ecosistema de apps móviles y negocios."
                )
            )
            coroutineScope.launch {
                database.userDao().insertProfiles(seedProfiles)
            }
        }
    }

    HomeScreenStateless(
        profiles = profiles,
        onNavigateToChat = { matchId ->
            navController.navigate(Screen.ChatDetail(matchId))
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenStateless(
    profiles: List<StudentEntity>,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSwipeViewMode by rememberSaveable { mutableStateOf(true) }
    
    // Lista local de perfiles para manejar el Swipe a nivel de interfaz de usuario
    val activeProfiles = remember(profiles) { mutableStateListOf<StudentEntity>().apply { addAll(profiles) } }
    val swipedHistory = remember { mutableStateListOf<StudentEntity>() }
    
    val currentTopProfile = activeProfiles.lastOrNull()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
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
                    IconButton(onClick = { isSwipeViewMode = !isSwipeViewMode }) {
                        Icon(
                            imageVector = if (isSwipeViewMode) Icons.AutoMirrored.Filled.List else Icons.Default.ViewCarousel,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isSwipeViewMode) {
                // Modo Swipe Cards (Tinder Style)
                if (activeProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "¡Has visto todos los perfiles!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Regresa más tarde para encontrar nuevos compañeros de estudio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    activeProfiles.addAll(profiles)
                                    swipedHistory.clear()
                                }
                            ) {
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
                        // El Stack de Tarjetas usando Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Mostrar la tarjeta debajo (background card) para dar profundidad
                            if (activeProfiles.size > 1) {
                                val secondProfile = activeProfiles[activeProfiles.lastIndex - 1]
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

                            // La tarjeta superior interactiva
                            if (currentTopProfile != null) {
                                key(currentTopProfile.id) {
                                    ProfileCard(
                                        profile = currentTopProfile,
                                        modifier = Modifier.fillMaxSize(),
                                        isSwipeable = true,
                                        onSwipeLeft = {
                                            swipedHistory.add(currentTopProfile)
                                            activeProfiles.removeAt(activeProfiles.lastIndex)
                                        },
                                        onSwipeRight = {
                                            val profileMatched = currentTopProfile
                                            swipedHistory.add(profileMatched)
                                            activeProfiles.removeAt(activeProfiles.lastIndex)
                                            onNavigateToChat(profileMatched.id)
                                        },
                                        onCardClick = {
                                            onNavigateToChat(currentTopProfile.id)
                                        }
                                    )
                                }
                            }
                        }

                        // Botones de acción inferior estilo M3
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón de Rechazo (Izquierda)
                            FilledIconButton(
                                onClick = {
                                    if (currentTopProfile != null) {
                                        swipedHistory.add(currentTopProfile)
                                        activeProfiles.removeAt(activeProfiles.lastIndex)
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

                            // Botón de Superlike (Centro)
                            FilledIconButton(
                                onClick = {
                                    if (currentTopProfile != null) {
                                        val profileMatched = currentTopProfile
                                        swipedHistory.add(profileMatched)
                                        activeProfiles.removeAt(activeProfiles.lastIndex)
                                        onNavigateToChat(profileMatched.id)
                                    }
                                },
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Superlike",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Botón de Match / Like (Derecha)
                            FilledIconButton(
                                onClick = {
                                    if (currentTopProfile != null) {
                                        val profileMatched = currentTopProfile
                                        swipedHistory.add(profileMatched)
                                        activeProfiles.removeAt(activeProfiles.lastIndex)
                                        onNavigateToChat(profileMatched.id)
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
                                    contentDescription = "Me gusta / Chatear",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Modo Lista (Fase 1: LazyColumn obligatoria para validar comportamiento perezoso y smart skipping)
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = profiles,
                        key = { it.id } // Llave obligatoria para optimización de Skipping
                    ) { profile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToChat(profile.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = profile.name, style = MaterialTheme.typography.titleLarge)
                                Text(text = profile.career, style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // LazyRow para renderizar eficientemente las etiquetas de interés
                                val interestList = profile.interests.split(",")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(interestList) { interest ->
                                        SuggestionChip(
                                            onClick = {}, 
                                            label = { Text(interest) }
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
    val coroutineScope = rememberCoroutineScope()

    // Animación suave de regreso si no se arrastra lo suficiente
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

    // Rotación de la tarjeta basada en el movimiento en el eje X
    val rotationZ = (offsetXAnimated / 1000f) * 45f

    // Generar gradientes elegantes usando un hash del nombre del estudiante para que el avatar luzca premium y dinámico
    val nameHash = profile.name.hashCode()
    val colors = listOf(
        Color(0xFF0056B3).copy(alpha = 0.8f),
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
                                if (offsetX > 350f) {
                                    onSwipeRight()
                                } else if (offsetX < -350f) {
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
            // Sección superior de Imagen / Iniciales Estilo Premium
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(avatarGradient),
                contentAlignment = Alignment.Center
            ) {
                // Iniciales gigantes del estudiante
                val initials = profile.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
                Text(
                    text = initials,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                // Indicador de "LIKE" o "NOPE" al arrastrar la tarjeta
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

            // Sección de detalles del perfil
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                    
                    // LazyRow para renderizar eficientemente las etiquetas de interés
                    val interestList = remember(profile.interests) { profile.interests.split(",") }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = interestList,
                            key = { it } // Llave obligatoria para optimización de Skipping
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
