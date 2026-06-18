package com.example.pucematch.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.pucematch.data.repository.PuceMatchRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para el Login.
 * Gestiona el formulario y conserva su estado de forma resiliente a través de SavedStateHandle.
 */
class LoginViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PuceMatchRepository
) : ViewModel() {

    private val emailKey = "email"
    private val passwordKey = "password"
    private val emailErrorKey = "email_error"

    // Flujos de estado reactivos que persisten ante la muerte del proceso
    val email = savedStateHandle.getStateFlow(emailKey, "")
    val password = savedStateHandle.getStateFlow(passwordKey, "")
    val isEmailError = savedStateHandle.getStateFlow(emailErrorKey, false)

    fun onEmailChange(input: String) {
        savedStateHandle[emailKey] = input
        savedStateHandle[emailErrorKey] = !input.endsWith("@puce.edu.ec") && input.isNotEmpty()
    }

    fun onPasswordChange(input: String) {
        savedStateHandle[passwordKey] = input
    }

    fun validateAndLogin(onSuccess: (userId: String) -> Unit) {
        val isEmailValid = email.value.endsWith("@puce.edu.ec") && email.value.isNotEmpty()
        val isPasswordValid = password.value.isNotEmpty()

        if (isEmailValid && isPasswordValid) {
            viewModelScope.launch {
                // Generar un ID de usuario determinista a partir del correo electrónico
                val userId = java.util.UUID.nameUUIDFromBytes(email.value.trim().lowercase().toByteArray()).toString()
                onSuccess(userId)
            }
        } else {
            savedStateHandle[emailErrorKey] = !isEmailValid
        }
    }

    companion object {
        fun provideFactory(repository: PuceMatchRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = this.createSavedStateHandle()
                LoginViewModel(savedStateHandle, repository)
            }
        }
    }
}
