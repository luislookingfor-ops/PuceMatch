package com.example.pucematch.domain

import kotlinx.serialization.Serializable

/**
 * Grafo de navegación fuertemente tipado para PuceMatch.
 * Cada ruta es un objeto/data class @Serializable que garantiza
 * seguridad de tipos en tiempo de compilación.
 */
sealed interface Screen {

    /** Pantalla de inicio de sesión con correo institucional PUCE. */
    @Serializable
    object Login : Screen

    /** Pantalla de registro de nuevo perfil estudiantil. */
    @Serializable
    object Register : Screen

    /** Pantalla principal con catálogo de perfiles y matches. */
    @Serializable
    object Home : Screen

    /** Detalle de chat con un match específico. */
    @Serializable
    data class ChatDetail(val matchId: String) : Screen

    /** Pantalla para editar el propio perfil del usuario autenticado. */
    @Serializable
    object EditProfile : Screen
}
