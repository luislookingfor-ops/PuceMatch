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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Home.
 * Gestiona el catálogo de perfiles segmentado por categorías y procesa los likes reales en el servidor.
 */
class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PuceMatchRepository,
    val currentUserId: String
) : ViewModel() {

    private val isSwipeViewModeKey = "is_swipe_view_mode"
    private val activeTabKey = "active_tab"
    private val swipedProfileIdsKey = "swiped_profile_ids"

    val isSwipeViewMode = savedStateHandle.getStateFlow(isSwipeViewModeKey, true)
    val activeTab = savedStateHandle.getStateFlow(activeTabKey, "Educativo")
    val swipedProfileIds = savedStateHandle.getStateFlow<List<String>>(swipedProfileIdsKey, emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // Todos los perfiles en la base de datos local (excluyendo el perfil del usuario actual)
    val allProfiles: StateFlow<List<StudentEntity>> = repository.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Perfiles filtrados por tipo de match (excluyendo a uno mismo)
    val filteredProfiles: StateFlow<List<StudentEntity>> = combine(allProfiles, activeTab) { profiles, tab ->
        profiles.filter { 
            it.matchType.equals(tab, ignoreCase = true) && it.id != currentUserId 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Perfiles listos para deslizar en la pestaña activa
    val swipeCardProfiles: StateFlow<List<StudentEntity>> = combine(filteredProfiles, swipedProfileIds) { profiles, swipedIds ->
        profiles.filter { it.id !in swipedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Matches activos (donde ya hay coincidencia mutua)
    val activeMatches: StateFlow<List<StudentEntity>> = allProfiles.combine(allProfiles) { profiles, _ ->
        profiles.filter { it.isMatched && it.id != currentUserId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Carga inicial con indicador visual
        refreshCatalog()

        // Sondeo periódico silencioso en segundo plano (cada 2 segundos) para sincronizar perfiles de otros celulares
        viewModelScope.launch {
            while (true) {
                delay(2000)
                repository.refreshProfiles()
            }
        }
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

    /**
     * Envía un like al servidor remoto. Invoca un callback con el resultado
     * para disparar el diálogo flotante en la UI si se concreta una coincidencia mutua.
     */
    fun swipeRight(student: StudentEntity, onMatchResult: (Boolean, String?) -> Unit) {
        val currentSwiped = swipedProfileIds.value.toMutableList()
        if (student.id !in currentSwiped) {
            currentSwiped.add(student.id)
            savedStateHandle[swipedProfileIdsKey] = currentSwiped
        }

        viewModelScope.launch {
            val result = repository.likeProfile(likerId = currentUserId, likedId = student.id)
            result.onSuccess { matchResponse ->
                onMatchResult(matchResponse.isMatch, matchResponse.matchId)
            }.onFailure {
                // Si la red falla (Offline), reportamos que no hay match inmediato.
                onMatchResult(false, null)
            }
        }
    }

    fun swipeLeft(studentId: String) {
        val currentSwiped = swipedProfileIds.value.toMutableList()
        if (studentId !in currentSwiped) {
            currentSwiped.add(studentId)
            savedStateHandle[swipedProfileIdsKey] = currentSwiped
        }
    }

    fun resetSwipes() {
        savedStateHandle[swipedProfileIdsKey] = emptyList<String>()
        refreshCatalog()
    }

    companion object {
        fun provideFactory(repository: PuceMatchRepository, currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = this.createSavedStateHandle()
                HomeViewModel(savedStateHandle, repository, currentUserId)
            }
        }
    }
}
