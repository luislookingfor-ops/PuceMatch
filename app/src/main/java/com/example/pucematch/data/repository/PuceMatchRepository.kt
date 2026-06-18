package com.example.pucematch.data.repository

import com.example.pucematch.data.local.MessageDao
import com.example.pucematch.data.local.MessageEntity
import com.example.pucematch.data.local.StudentEntity
import com.example.pucematch.data.local.UserDao
import com.example.pucematch.data.remote.MatchResponse
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
     * Inserta un perfil de estudiante (al registrarse) local y remotamente.
     */
    suspend fun insertProfile(student: StudentEntity) = withContext(Dispatchers.IO) {
        // 1. Guardar localmente
        userDao.insertProfile(student)
        // 2. Intentar subir al servidor remoto
        try {
            api.registerProfile(student)
        } catch (e: Exception) {
            e.printStackTrace()
            // Offline-first: se mantiene localmente aunque falle la red
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
     * Envía un like al servidor remoto. Si el servidor confirma un match mutuo,
     * se actualiza el estado local en la base de datos de Room.
     */
    suspend fun likeProfile(likerId: String, likedId: String): Result<MatchResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.likeProfile(likerId, likedId)
            if (response.isSuccessful && response.body() != null) {
                val matchResult = response.body()!!
                if (matchResult.isMatch) {
                    userDao.updateMatchStatus(likedId, true)
                }
                Result.success(matchResult)
            } else {
                Result.failure(Exception("Error al registrar Like en servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sincroniza el catálogo local con la red, preservando las modificaciones locales (como matches y avatares).
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
     * Sincroniza los mensajes del servidor para un chat específico y los guarda en Room.
     */
    suspend fun refreshMessages(matchId: String, currentUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getChatMessages(matchId)
            if (response.isSuccessful && response.body() != null) {
                val remoteMessages = response.body()!!
                val localEntities = remoteMessages.map { remote ->
                    MessageEntity(
                        id = remote.id,
                        matchId = remote.matchId,
                        text = remote.content,
                        isFromMe = remote.senderId == currentUserId,
                        timestamp = remote.timestamp
                    )
                }
                messageDao.insertMessages(localEntities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al descargar mensajes: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Guarda un mensaje localmente y luego intenta enviarlo al servidor remoto de manera resiliente.
     */
    suspend fun sendMessage(matchId: String, senderId: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val currentTime = sdf.format(java.util.Date())
        val messageId = java.util.UUID.randomUUID().toString()

        // 1. Guardado local inmediato en Room (actualiza la UI reactivamente)
        val localMessage = MessageEntity(
            id = messageId,
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
     * Guarda un mensaje de respuesta localmente.
     */
    suspend fun insertMessage(message: MessageEntity) {
        withContext(Dispatchers.IO) {
            messageDao.insertMessage(message)
        }
    }
}
