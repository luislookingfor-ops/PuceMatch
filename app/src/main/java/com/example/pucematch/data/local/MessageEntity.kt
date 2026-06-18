package com.example.pucematch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room que almacena los mensajes de los chats.
 * Permite mantener un historial de conversación ordenado cronológicamente.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val id: String,
    val matchId: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String
)
