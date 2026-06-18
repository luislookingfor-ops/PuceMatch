package com.example.pucematch.data.repository

import com.example.pucematch.data.local.MessageDao
import com.example.pucematch.data.local.MessageEntity
import com.example.pucematch.data.local.StudentEntity
import com.example.pucematch.data.local.UserDao
import com.example.pucematch.data.remote.MessageRequest
import com.example.pucematch.data.remote.PuceMatchApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio central de PuceMatch.
 * Actúa como la única fuente de verdad para la aplicación, coordinando
 * la persistencia local en Room con las llamadas de red mediante Retrofit.
 */
class PuceMatchRepository(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val api: PuceMatchApi
) {
    /**
     * Obtiene el flujo reactivo de todos los perfiles de estudiantes.
     */
    fun getProfiles(): Flow<List<StudentEntity>> = userDao.getAllProfiles()

    /**
     * Inserta un perfil de estudiante (por ejemplo, al registrarse o crear perfiles manualmente).
     */
    suspend fun insertProfile(student: StudentEntity) {
        withContext(Dispatchers.IO) {
            userDao.insertProfile(student)
        }
    }

    /**
     * Actualiza el estado de match con un estudiante.
     */
    suspend fun updateMatchStatus(studentId: String, isMatched: Boolean) {
        withContext(Dispatchers.IO) {
            userDao.updateMatchStatus(studentId, isMatched)
        }
    }

    /**
     * Sincroniza el catálogo local con la red, preservando las modificaciones locales (como matches y avatares subidos).
     */
    suspend fun refreshProfiles(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getGlobalCatalog()
            if (response.isSuccessful) {
                val remoteProfiles = response.body() ?: emptyList()
                val localProfiles = userDao.getProfilesList()
                val localMap = localProfiles.associateBy { it.id }

                val mergedProfiles = remoteProfiles.map { remote ->
                    val local = localMap[remote.id]
                    if (local != null) {
                        // Conservar el estado de match y el avatar local
                        remote.copy(
                            isMatched = local.isMatched,
                            avatarUri = local.avatarUri ?: remote.avatarUri
                        )
                    } else {
                        remote
                    }
                }
                userDao.insertProfiles(mergedProfiles)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error en la llamada de red: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el flujo reactivo de los mensajes de chat para un match específico.
     */
    fun getMessages(matchId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForMatch(matchId)

    /**
     * Guarda un mensaje localmente y luego intenta enviarlo al servidor remoto de manera resiliente.
     */
    suspend fun sendMessage(matchId: String, senderId: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val currentTime = sdf.format(java.util.Date())

        // 1. Guardado local inmediato en Room (actualiza la UI reactivamente)
        val localMessage = MessageEntity(
            id = java.util.UUID.randomUUID().toString(),
            matchId = matchId,
            text = content,
            isFromMe = true,
            timestamp = currentTime
        )
        messageDao.insertMessage(localMessage)

        // 2. Intento de sincronización con Retrofit
        try {
            val request = MessageRequest(matchId = matchId, senderId = senderId, content = content)
            val response = api.sendMessage(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error de red al sincronizar mensaje: ${response.code()}"))
            }
        } catch (e: Exception) {
            // El mensaje se retiene en Room de forma resiliente, permitiendo trabajo Offline
            Result.failure(e)
        }
    }

    /**
     * Guarda un mensaje de respuesta del bot/compañero localmente.
     */
    suspend fun insertMessage(message: MessageEntity) {
        withContext(Dispatchers.IO) {
            messageDao.insertMessage(message)
        }
    }
}
