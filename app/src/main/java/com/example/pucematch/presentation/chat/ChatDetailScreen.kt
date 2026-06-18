package com.example.pucematch.presentation.chat

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pucematch.PuceMatchApplication
import com.example.pucematch.data.local.MessageEntity
import com.example.pucematch.data.local.StudentEntity
import java.util.*

@Composable
fun ChatDetailScreenStateful(
    navController: NavController,
    matchId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PuceMatchApplication
    val viewModel: ChatDetailViewModel = viewModel(
        factory = ChatDetailViewModel.provideFactory(app.repository, app.getCurrentUserId())
    )

    val student by viewModel.matchedStudent.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val hasMatchRight by viewModel.hasMatchRight.collectAsState()

    ChatDetailScreenStateless(
        student = student ?: StudentEntity(
            id = matchId,
            name = "Cargando...",
            career = "...",
            interests = "",
            bio = ""
        ),
        messages = messages,
        isTyping = isTyping,
        inputText = inputText,
        hasMatchRight = hasMatchRight,
        onInputTextChange = { viewModel.onInputTextChange(it) },
        onSendMessage = { viewModel.sendMessage() },
        onBackClick = { navController.popBackStack() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreenStateless(
    student: StudentEntity,
    messages: List<MessageEntity>,
    isTyping: Boolean,
    inputText: String,
    hasMatchRight: Boolean,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    // DEFENSA FINAL: Verificar si hay match mutuo activo
    if (!hasMatchRight && student.name != "Cargando...") {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Acceso Denegado") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            },
            modifier = modifier.fillMaxSize()
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Conversación Bloqueada",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Únicamente se permite chatear con estudiantes si ambos coinciden en un Match mutuo. Vuelve a la pantalla principal y desliza hacia la derecha para iniciar la conversación.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBackClick) {
                        Text("Regresar al catálogo")
                    }
                }
            }
        }
        return
    }

    val nameHash = student.name.hashCode()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp),
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

                    // Avatar con Gradiente e Iniciales o Foto local
                    val avatarColors = listOf(
                        Color(0xFF003554).copy(alpha = 0.85f),
                        Color(nameHash or 0xFF000000.toInt()).copy(alpha = 0.9f)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(avatarColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (student.avatarUri != null) {
                            val bitmap = remember(student.avatarUri) {
                                try {
                                    BitmapFactory.decodeFile(student.avatarUri)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = student.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initials = student.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
                                Text(
                                    text = initials,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            val initials = student.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
                            Text(
                                text = initials,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
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
                        onValueChange = onInputTextChange,
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
                                onSendMessage()
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
