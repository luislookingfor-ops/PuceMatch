package com.example.pucematch.presentation.home

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Home.
 * Gestiona de forma reactiva (UDF) el catálogo de estudiantes filtrado por grupos de match,
 * el historial de swipes del usuario para evitar pérdida de estado y la creación manual de perfiles.
 */
class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PuceMatchRepository
) : ViewModel() {

    private val isSwipeViewModeKey = "is_swipe_view_mode"
    private val activeTabKey = "active_tab"
    private val swipedProfileIdsKey = "swiped_profile_ids"

    // Estados de configuración de vista y filtrado guardados en SavedStateHandle
    val isSwipeViewMode = savedStateHandle.getStateFlow(isSwipeViewModeKey, true)
    val activeTab = savedStateHandle.getStateFlow(activeTabKey, "Educativo") // "Educativo", "Recreacional", "Sentimental"
    val swipedProfileIds = savedStateHandle.getStateFlow<List<String>>(swipedProfileIdsKey, emptyList())

    // Estado de carga de red
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // Flujo global de todos los perfiles de la base de datos local (Room)
    val allProfiles: StateFlow<List<StudentEntity>> = repository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Perfiles filtrados por la pestaña (grupo de match) activa
    val filteredProfiles: StateFlow<List<StudentEntity>> = combine(allProfiles, activeTab) { profiles, tab ->
        profiles.filter { it.matchType.equals(tab, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Perfiles filtrados de la pestaña activa que NO han sido deslizados en esta sesión (para el modo Swipe)
    val swipeCardProfiles: StateFlow<List<StudentEntity>> = combine(filteredProfiles, swipedProfileIds) { profiles, swipedIds ->
        profiles.filter { it.id !in swipedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Matches activos (estudiantes con los que ya se tiene coincidencia mutua)
    val activeMatches: StateFlow<List<StudentEntity>> = allProfiles.combine(allProfiles) { profiles, _ ->
        profiles.filter { it.isMatched }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Al iniciar, refrescar automáticamente del servidor Retrofit de forma asíncrona
        refreshCatalog()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshProfiles()
            _isRefreshing.value = false
        }
    }

    fun setSwipeViewMode(enabled: Boolean) {
        savedStateHandle[isSwipeViewModeKey] = enabled
    }

    fun setActiveTab(tab: String) {
        savedStateHandle[activeTabKey] = tab
    }

    fun swipeRight(student: StudentEntity) {
        // Agregar al historial de deslizados
        val currentSwiped = swipedProfileIds.value.toMutableList()
        if (student.id !in currentSwiped) {
            currentSwiped.add(student.id)
            savedStateHandle[swipedProfileIdsKey] = currentSwiped
        }
        
        // Simular match mutuo actualizando la base de datos local
        viewModelScope.launch {
            repository.updateMatchStatus(student.id, true)
        }
    }

    fun swipeLeft(studentId: String) {
        // Agregar al historial de deslizados
        val currentSwiped = swipedProfileIds.value.toMutableList()
        if (studentId !in currentSwiped) {
            currentSwiped.add(studentId)
            savedStateHandle[swipedProfileIdsKey] = currentSwiped
        }
    }

    fun resetSwipes() {
        savedStateHandle[swipedProfileIdsKey] = emptyList<String>()
    }

    /**
     * Permite la creación manual de perfiles de estudiantes locales en caliente.
     */
    fun addManualProfile(
        name: String,
        career: String,
        interests: String,
        bio: String,
        matchType: String,
        avatarUri: String?
    ) {
        viewModelScope.launch {
            val newProfile = StudentEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                career = career,
                interests = interests,
                bio = bio,
                avatarUri = avatarUri,
                matchType = matchType,
                isMatched = false
            )
            repository.insertProfile(newProfile)
        }
    }

    companion object {
        fun provideFactory(repository: PuceMatchRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = this.createSavedStateHandle()
                HomeViewModel(savedStateHandle, repository)
            }
        }
    }
}
