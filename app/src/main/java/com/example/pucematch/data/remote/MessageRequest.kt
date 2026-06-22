package com.example.pucematch.data.remote

/**
 * Modelo de request para enviar un mensaje en el chat de un match.
 */
data class MessageRequest(
    val id: String,
    val matchId: String,
    val senderId: String,
    val content: String,
    val timestamp: String,
    val createdAt: Long
)
