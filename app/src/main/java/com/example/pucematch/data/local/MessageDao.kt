package com.example.pucematch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para gestionar los mensajes locales.
 * Expone un Flow observable para actualizar reactivamente la pantalla de chat.
 */
@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE matchId = :matchId ORDER BY localId ASC")
    fun getMessagesForMatch(matchId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
}
