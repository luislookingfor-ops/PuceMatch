package com.example.pucematch.presentation.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.pucematch.data.local.StudentEntity
import com.example.pucematch.data.repository.PuceMatchRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para el registro de nuevos perfiles de estudiantes.
 * Garantiza resiliencia de datos en formularios extensos usando SavedStateHandle.
 */
class RegisterViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PuceMatchRepository
) : ViewModel() {

    private val nameKey = "name"
    private val emailKey = "email"
    private val semesterKey = "semester"
    private val careerKey = "career"
    private val passwordKey = "password"
    private val confirmPasswordKey = "confirm_password"
    private val avatarUriKey = "avatar_uri"
    private val matchTypeKey = "match_type"

    private val emailErrorKey = "email_error"
    private val semesterErrorKey = "semester_error"
    private val passwordErrorKey = "password_error"
    private val confirmPasswordErrorKey = "confirm_password_error"

    // Estados expuestos como StateFlows observables y salvaguardados
    val name = savedStateHandle.getStateFlow(nameKey, "")
    val email = savedStateHandle.getStateFlow(emailKey, "")
    val semester = savedStateHandle.getStateFlow(semesterKey, "")
    val career = savedStateHandle.getStateFlow(careerKey, "")
    val password = savedStateHandle.getStateFlow(passwordKey, "")
    val confirmPassword = savedStateHandle.getStateFlow(confirmPasswordKey, "")
    val avatarUri = savedStateHandle.getStateFlow<String?>(avatarUriKey, null)
    val matchType = savedStateHandle.getStateFlow(matchTypeKey, "Educativo")

    val isEmailError = savedStateHandle.getStateFlow(emailErrorKey, false)
    val isSemesterError = savedStateHandle.getStateFlow(semesterErrorKey, false)
    val isPasswordError = savedStateHandle.getStateFlow(passwordErrorKey, false)
    val isConfirmPasswordError = savedStateHandle.getStateFlow(confirmPasswordErrorKey, false)

    fun onNameChange(input: String) {
        savedStateHandle[nameKey] = input
    }

    fun onEmailChange(input: String) {
        savedStateHandle[emailKey] = input
        savedStateHandle[emailErrorKey] = !input.endsWith("@puce.edu.ec") && input.isNotEmpty()
    }

    fun onSemesterChange(input: String) {
        savedStateHandle[semesterKey] = input
        if (input.isEmpty()) {
            savedStateHandle[semesterErrorKey] = false
        } else {
            val semNum = input.toIntOrNull()
            savedStateHandle[semesterErrorKey] = semNum == null || semNum !in 1..12
        }
    }

    fun onCareerChange(input: String) {
        savedStateHandle[careerKey] = input
    }

    fun onPasswordChange(input: String) {
        savedStateHandle[passwordKey] = input
        savedStateHandle[passwordErrorKey] = input.length < 6
        if (confirmPassword.value.isNotEmpty()) {
            savedStateHandle[confirmPasswordErrorKey] = input != confirmPassword.value
        }
    }

    fun onConfirmPasswordChange(input: String) {
        savedStateHandle[confirmPasswordKey] = input
        savedStateHandle[confirmPasswordErrorKey] = password.value != input
    }

    fun onAvatarChange(uri: String?) {
        savedStateHandle[avatarUriKey] = uri
    }

    fun onMatchTypeChange(type: String) {
        savedStateHandle[matchTypeKey] = type
    }

    fun registerUser(onSuccess: () -> Unit) {
        val isEmailValid = email.value.endsWith("@puce.edu.ec") && email.value.isNotEmpty()
        val semNum = semester.value.toIntOrNull()
        val isSemesterValid = semester.value.isNotEmpty() && semNum != null && semNum in 1..12
        val isPasswordValid = password.value.isNotEmpty() && password.value.length >= 6
        val isConfirmValid = password.value == confirmPassword.value
        val isNameValid = name.value.isNotEmpty()
        val isCareerValid = career.value.isNotEmpty()

        if (isEmailValid && isSemesterValid && isPasswordValid && isConfirmValid && isNameValid && isCareerValid) {
            viewModelScope.launch {
                // Registrar el perfil del estudiante en la base de datos local
                val newProfile = StudentEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name.value,
                    career = career.value,
                    interests = "Kotlin,Android,UI/UX,Diseño,Base de Datos",
                    bio = "Estudiante de la PUCE cursando ${semester.value}° semestre. Buscando conectar en el ámbito ${matchType.value}.",
                    avatarUri = avatarUri.value,
                    matchType = matchType.value,
                    isMatched = false
                )
                repository.insertProfile(newProfile)
                onSuccess()
            }
        } else {
            savedStateHandle[emailErrorKey] = !isEmailValid
            savedStateHandle[semesterErrorKey] = !isSemesterValid
            savedStateHandle[passwordErrorKey] = !isPasswordValid
            savedStateHandle[confirmPasswordErrorKey] = !isConfirmValid
        }
    }

    companion object {
        fun provideFactory(repository: PuceMatchRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = this.createSavedStateHandle()
                RegisterViewModel(savedStateHandle, repository)
            }
        }
    }
}
