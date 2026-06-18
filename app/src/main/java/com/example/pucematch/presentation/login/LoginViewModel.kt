package com.example.pucematch.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pucematch.data.repository.PuceMatchRepository

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
        // Validar en tiempo real que pertenezca al dominio institucional de la PUCE
        savedStateHandle[emailErrorKey] = !input.endsWith("@puce.edu.ec") && input.isNotEmpty()
    }

    fun onPasswordChange(input: String) {
        savedStateHandle[passwordKey] = input
    }

    fun validateAndLogin(onSuccess: () -> Unit) {
        val isEmailValid = email.value.endsWith("@puce.edu.ec") && email.value.isNotEmpty()
        val isPasswordValid = password.value.isNotEmpty()

        if (isEmailValid && isPasswordValid) {
            onSuccess()
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
