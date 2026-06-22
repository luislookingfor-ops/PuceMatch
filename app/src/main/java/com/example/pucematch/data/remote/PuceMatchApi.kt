package com.example.pucematch.data.remote

import com.example.pucematch.data.local.StudentEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato Retrofit para la API de PuceMatch.
 * Define la comunicación cliente-servidor para sincronizar perfiles, registrar likes,
 * verificar matches y transferir mensajes de chat reales.
 */
interface PuceMatchApi {

    @GET("api/v1/students")
    suspend fun getGlobalCatalog(): Response<List<StudentEntity>>

    @POST("api/v1/students")
    suspend fun registerProfile(@Body student: StudentEntity): Response<Unit>

    @POST("api/v1/matches/like")
    suspend fun likeProfile(
        @Query("likerId") likerId: String,
        @Query("likedId") likedId: String
    ): Response<MatchResponse>

    @GET("api/v1/matches/chat/{matchId}")
    suspend fun getChatMessages(@Path("matchId") matchId: String): Response<List<MessageResponse>>

    @POST("api/v1/matches/chat")
    suspend fun sendMessage(@Body message: MessageRequest): Response<Unit>
}

/**
 * Modelo de respuesta al registrar un like, indicando si se concretó una coincidencia mutua.
 */
data class MatchResponse(
    val isMatch: Boolean,
    val matchId: String?
)

/**
 * Modelo de respuesta para la descarga de mensajes del servidor.
 */
data class MessageResponse(
    val id: String,
    val matchId: String,
    val senderId: String,
    val content: String,
    val timestamp: String,
    val createdAt: Long
)
