package com.example.pucematch.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pucematch.data.local.AppDatabase
import com.example.pucematch.data.local.StudentEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String
)

@Composable
fun ChatDetailScreenStateful(
    navController: NavController,
    matchId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val profiles by database.userDao().getAllProfiles().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    // Encontrar el estudiante del match
    val matchedStudent = remember(profiles, matchId) {
        profiles.find { it.id == matchId } ?: StudentEntity(
            id = matchId,
            name = "Estudiante PUCE",
            career = "Carrera PUCE",
            interests = "Kotlin,Estudios",
            bio = "Buscando compañeros de estudio en la universidad."
        )
    }

    // Historial inicial de mensajes
    val messages = remember {
        mutableStateListOf(
            Message(
                text = "¡Hola! Vi que coincidimos en intereses.",
                isFromMe = false,
                timestamp = "10:15 AM"
            ),
            Message(
                text = "¡Hola! Qué bien. Sí, vi tu perfil y me gustó mucho tu bio.",
                isFromMe = true,
                timestamp = "10:16 AM"
            ),
            Message(
                text = "¡Gracias! Justamente estoy buscando a alguien para armar grupo de estudio para los proyectos de este semestre. ¿Te interesaría?",
                isFromMe = false,
                timestamp = "10:18 AM"
            )
        )
    }

    var isTyping by remember { mutableStateOf(false) }

    // Respuestas automáticas simuladas para hacer la experiencia dinámica
    val botReplies = listOf(
        "¡Excelente! Deberíamos reunirnos en la biblioteca de la PUCE esta semana.",
        "Genial. Precisamente estoy libre los martes y jueves por la tarde para avanzar.",
        "Totalmente de acuerdo. PuceMatch me está pareciendo súper útil para esto.",
        "¡Qué bien! Te paso mi número de WhatsApp por interno si gustas.",
        "Buenísimo, nos organizamos entonces."
    )
    var replyIndex by remember { mutableStateOf(0) }

    val onSendMessage: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val currentTime = sdf.format(Date())
            
            // Añadir mensaje del usuario
            messages.add(Message(text = text, isFromMe = true, timestamp = currentTime))
            
            // Simular respuesta asíncrona
            coroutineScope.launch {
                delay(800)
                isTyping = true
                delay(1500)
                isTyping = false
                val replyText = botReplies[replyIndex % botReplies.size]
                replyIndex++
                messages.add(Message(text = replyText, isFromMe = false, timestamp = sdf.format(Date())))
            }
        }
    }

    ChatDetailScreenStateless(
        student = matchedStudent,
        messages = messages,
        isTyping = isTyping,
        onBackClick = { navController.popBackStack() },
        onSendMessage = onSendMessage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreenStateless(
    student: StudentEntity,
    messages: List<Message>,
    isTyping: Boolean,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Auto-scroll al último mensaje al enviar/recibir
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    // Gradiente personalizado para la barra superior
    val nameHash = student.name.hashCode()
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.background
    )
    val appbarBackground = Brush.verticalGradient(colors)

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(4.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Avatar con Gradiente e Iniciales
                    val avatarColors = listOf(
                        Color(0xFF0056B3).copy(alpha = 0.8f),
                        Color(nameHash or 0xFF000000.toInt()).copy(alpha = 0.9f)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(avatarColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = student.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
                        Text(
                            text = initials,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isTyping) "Escribiendo..." else "En línea",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTyping) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                            fontWeight = if (isTyping) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .shadow(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Escribe un mensaje...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        singleLine = false,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
                    val bubbleBgColor = if (message.isFromMe) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                    val bubbleContentColor = if (message.isFromMe) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    // Globos de texto asimétricos estilo WhatsApp / Telegram
                    val bubbleShape = if (message.isFromMe) {
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 2.dp
                        )
                    } else {
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 2.dp,
                            bottomEnd = 16.dp
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        Surface(
                            shape = bubbleShape,
                            color = bubbleBgColor,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = message.text,
                                    color = bubbleContentColor,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = message.timestamp,
                                    color = bubbleContentColor.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }

                // Animación de Escribiendo...
                if (isTyping) {
                    item(key = "typing_indicator") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 2.dp,
                                    bottomEnd = 16.dp
                                ),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.widthIn(max = 100.dp)
                            ) {
                                TypingIndicatorDots()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        Box(modifier = Modifier.size(6.dp * dot1Scale).background(dotColor, CircleShape))
        Box(modifier = Modifier.size(6.dp * dot2Scale).background(dotColor, CircleShape))
        Box(modifier = Modifier.size(6.dp * dot3Scale).background(dotColor, CircleShape))
    }
}
