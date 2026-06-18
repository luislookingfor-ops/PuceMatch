package com.example.pucematch.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.pucematch.data.local.MessageEntity
import com.example.pucematch.data.local.StudentEntity
import com.example.pucematch.data.repository.PuceMatchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de detalle de chat.
 * Realiza la defensa y verificación de match activo antes de chatear,
 * y controla la mensajería y las respuestas simuladas del bot.
 */
class ChatDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PuceMatchRepository
) : ViewModel() {

    // Extraer matchId inyectado automáticamente por Navigation Compose
    val matchId: String = savedStateHandle.get<String>("matchId") ?: ""

    private val inputTextKey = "input_text"
    val inputText = savedStateHandle.getStateFlow(inputTextKey, "")

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    // Historial de mensajes reactivo desde base de datos local (Room)
    val messages: StateFlow<List<MessageEntity>> = repository.getMessages(matchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estudiante asociado al chat
    val matchedStudent: StateFlow<StudentEntity?> = repository.getProfiles()
        .map { profiles -> profiles.find { it.id == matchId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Defensa: verifica si el chat está desbloqueado por un match mutuo
    val hasMatchRight: StateFlow<Boolean> = matchedStudent.map { it?.isMatched == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Respuestas automáticas simuladas para dinamizar el chat
    private val botReplies = listOf(
        "¡Excelente! Deberíamos reunirnos en la biblioteca de la PUCE esta semana.",
        "Genial. Precisamente estoy libre los martes y jueves por la tarde para avanzar.",
        "Totalmente de acuerdo. PuceMatch me está pareciendo súper útil para esto.",
        "¡Qué bien! Te paso mi número de WhatsApp por interno si gustas.",
        "Buenísimo, nos organizamos entonces."
    )
    private var replyIndex = 0

    fun onInputTextChange(text: String) {
        savedStateHandle[inputTextKey] = text
    }

    fun sendMessage() {
        val content = inputText.value
        if (content.isBlank() || !hasMatchRight.value) return

        // Limpiar el input
        savedStateHandle[inputTextKey] = ""

        viewModelScope.launch {
            // 1. Guardar mensaje localmente y subir a Retrofit
            repository.sendMessage(matchId, "me", content)

            // 2. Simular escritura y respuesta del bot
            delay(800)
            _isTyping.value = true
            delay(1500)
            _isTyping.value = false

            val replyText = botReplies[replyIndex % botReplies.size]
            replyIndex++

            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            val replyMessage = MessageEntity(
                id = java.util.UUID.randomUUID().toString(),
                matchId = matchId,
                text = replyText,
                isFromMe = false,
                timestamp = sdf.format(java.util.Date())
            )
            repository.insertMessage(replyMessage)
        }
    }

    companion object {
        fun provideFactory(repository: PuceMatchRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = this.createSavedStateHandle()
                ChatDetailViewModel(savedStateHandle, repository)
            }
        }
    }
}
