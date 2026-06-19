package com.example.pucematch.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.pucematch.data.local.StudentEntity
import com.example.pucematch.data.repository.PuceMatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar la edición del perfil de usuario autenticado.
 * Carga el perfil actual de Room y expone estados seguros usando SavedStateHandle.
 */
class EditProfileViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PuceMatchRepository,
    val currentUserId: String
) : ViewModel() {

    private val nameKey = "name"
    private val careerKey = "career"
    private val bioKey = "bio"
    private val interestsKey = "interests"
    private val matchTypeKey = "match_type"
    private val avatarUriKey = "avatar_uri"

    val name = savedStateHandle.getStateFlow(nameKey, "")
    val career = savedStateHandle.getStateFlow(careerKey, "")
    val bio = savedStateHandle.getStateFlow(bioKey, "")
    val interests = savedStateHandle.getStateFlow(interestsKey, "")
    val matchType = savedStateHandle.getStateFlow(matchTypeKey, "Educativo")
    val avatarUri = savedStateHandle.getStateFlow<String?>(avatarUriKey, null)

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    init {
        // Cargar reactivamente los datos actuales del perfil del usuario
        viewModelScope.launch {
            repository.getProfiles().collect { profiles ->
                val profile = profiles.find { it.id == currentUserId }
                profile?.let {
                    // Inicializar el formulario si el estado actual está vacío
                    if (name.value.isEmpty()) savedStateHandle[nameKey] = it.name
                    if (career.value.isEmpty()) savedStateHandle[careerKey] = it.career
                    if (bio.value.isEmpty()) savedStateHandle[bioKey] = it.bio
                    if (interests.value.isEmpty()) savedStateHandle[interestsKey] = it.interests
                    if (avatarUri.value == null) savedStateHandle[avatarUriKey] = it.avatarUri
                    
                    // Solo inicializar matchType de Room si el formulario no tiene cambios
                    if (savedStateHandle.get<String>(matchTypeKey) == "Educativo" && it.matchType != "Educativo") {
                        savedStateHandle[matchTypeKey] = it.matchType
                    }
                }
            }
        }
    }

    fun onNameChange(input: String) {
        savedStateHandle[nameKey] = input
    }

    fun onCareerChange(input: String) {
        savedStateHandle[careerKey] = input
    }

    fun onBioChange(input: String) {
        savedStateHandle[bioKey] = input
    }

    fun onInterestsChange(input: String) {
        savedStateHandle[interestsKey] = input
    }

    fun onMatchTypeChange(type: String) {
        savedStateHandle[matchTypeKey] = type
    }

    fun onAvatarChange(uri: String?) {
        savedStateHandle[avatarUriKey] = uri
    }

    fun saveProfile() {
        if (name.value.isBlank() || career.value.isBlank()) return

        viewModelScope.launch {
            val updatedProfile = StudentEntity(
                id = currentUserId,
                name = name.value,
                career = career.value,
                interests = interests.value,
                bio = bio.value,
                avatarUri = avatarUri.value,
                matchType = matchType.value,
                isMatched = false
            )
            repository.insertProfile(updatedProfile)
            _isSaved.value = true
        }
    }

    companion object {
        fun provideFactory(repository: PuceMatchRepository, currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = this.createSavedStateHandle()
                EditProfileViewModel(savedStateHandle, repository, currentUserId)
            }
        }
    }
}
