package com.example.pucematch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room que almacena los mensajes de los chats.
 * Utiliza el id de UUID único del servidor como llave primaria para evitar duplicaciones
 * al sincronizar y realizar sondeos de red.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String, // UUID único del servidor
    val matchId: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String,
    val createdAt: Long = System.currentTimeMillis() // Usado para ordenamiento cronológico preciso
)
