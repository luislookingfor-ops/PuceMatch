package com.example.pucematch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa el perfil de un estudiante PUCE.
 * Actúa como la fuente de verdad local en la arquitectura Offline-First.
 */
@Entity(tableName = "profiles")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val career: String,
    val interests: String, // Guardado como String delimitado por comas para la Fase 1
    val bio: String,
    val avatarUri: String? = null,
    val matchType: String = "Educativo", // "Educativo", "Recreacional", "Sentimental"
    val isMatched: Boolean = false
)
