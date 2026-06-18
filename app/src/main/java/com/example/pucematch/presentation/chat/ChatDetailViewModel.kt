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
 * Valida la existencia de match y descarga/sintoniza mensajes reales del servidor.
 */
class ChatDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PuceMatchRepository,
    val currentUserId: String
) : ViewModel() {

    // Extraer matchId inyectado automáticamente por la navegación
    val matchId: String = savedStateHandle.get<String>("matchId") ?: ""

    private val inputTextKey = "input_text"
    val inputText = savedStateHandle.getStateFlow(inputTextKey, "")

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    // Historial de mensajes reactivo desde Room local
    val messages: StateFlow<List<MessageEntity>> = repository.getMessages(matchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Perfil del otro estudiante
    val matchedStudent: StateFlow<StudentEntity?> = repository.getProfiles()
        .map { profiles -> profiles.find { it.id == matchId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Defensa: requiere match activo
    val hasMatchRight: StateFlow<Boolean> = matchedStudent.map { it?.isMatched == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Sondeo (polling) cada 3 segundos para sincronizar mensajes en tiempo real entre celulares
        viewModelScope.launch {
            while (true) {
                // Solo realizamos la petición si ya hay match mutuo activo
                if (hasMatchRight.value) {
                    repository.refreshMessages(matchId, currentUserId)
                }
                delay(3000)
            }
        }
    }

    fun onInputTextChange(text: String) {
        savedStateHandle[inputTextKey] = text
    }

    fun sendMessage() {
        val content = inputText.value
        if (content.isBlank() || !hasMatchRight.value) return

        // Limpiar el input de inmediato
        savedStateHandle[inputTextKey] = ""

        viewModelScope.launch {
            // Guardar localmente y subir a la red usando el ID de usuario real
            repository.sendMessage(matchId, currentUserId, content)
        }
    }

    companion object {
        fun provideFactory(repository: PuceMatchRepository, currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = this.createSavedStateHandle()
                ChatDetailViewModel(savedStateHandle, repository, currentUserId)
            }
        }
    }
}
