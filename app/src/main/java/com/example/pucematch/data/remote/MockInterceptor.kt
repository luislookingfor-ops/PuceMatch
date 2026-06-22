package com.example.pucematch.data.remote

import com.example.pucematch.data.local.StudentEntity
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interceptor de OkHttp que actúa como un servidor backend virtual en memoria.
 * Permite probar flujos dinámicos reales (Registro de usuarios, dar Likes cruzados,
 * matches reales y envío de chats bidireccionales) en simulaciones offline.
 */
class MockInterceptor : Interceptor {

    companion object {
        // Base de datos virtual en memoria (compartida entre emuladores/hilos durante la ejecución)
        private val registeredStudents = mutableListOf<StudentEntity>()
        private val likes = mutableSetOf<Pair<String, String>>() // Formato: Pair(LikerId, LikedId)
        private val messages = mutableListOf<MessageResponse>()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url().encodedPath()
        val method = request.method()

        // Simular latencia de red de 500ms
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val gson = Gson()
        var responseString = "{}"
        var responseCode = 200

        when {
            // GET /api/v1/students: Obtiene el catálogo global de estudiantes (comienza vacío)
            path.endsWith("/api/v1/students") && method.equals("GET", ignoreCase = true) -> {
                responseString = gson.toJson(registeredStudents)
            }

            // POST /api/v1/students: Registra o actualiza el perfil de un estudiante
            path.endsWith("/api/v1/students") && method.equals("POST", ignoreCase = true) -> {
                val buffer = Buffer()
                request.body()?.writeTo(buffer)
                val requestBody = buffer.readUtf8()
                try {
                    val student = gson.fromJson(requestBody, StudentEntity::class.java)
                    // Reemplazar si el ID ya existe para mantener datos frescos
                    registeredStudents.removeAll { it.id == student.id }
                    registeredStudents.add(student)
                    responseString = "{}"
                } catch (e: Exception) {
                    e.printStackTrace()
                    responseCode = 400
                    responseString = """{"error":"JSON inválido en el cuerpo de la petición"}"""
                }
            }

            // POST /api/v1/matches/like: Registra un like y evalúa si es match mutuo
            path.endsWith("/api/v1/matches/like") && method.equals("POST", ignoreCase = true) -> {
                val likerId = request.url().queryParameter("likerId") ?: ""
                val likedId = request.url().queryParameter("likedId") ?: ""

                if (likerId.isNotEmpty() && likedId.isNotEmpty()) {
                    likes.add(Pair(likerId, likedId))

                    // Comprobar si el otro usuario ya le había dado like al liker actual (Match mutuo)
                    val isMatch = likes.contains(Pair(likedId, likerId))
                    val matchId = if (isMatch) {
                        // Generar ID de match único ordenando los IDs alfabéticamente
                        if (likerId < likedId) "${likerId}_${likedId}" else "${likedId}_${likerId}"
                    } else null

                    // Simular que si hacemos match, actualizamos el flag isMatched local en memoria para ambos estudiantes
                    if (isMatch) {
                        registeredStudents.replaceAll { student ->
                            if (student.id == likedId || student.id == likerId) {
                                student.copy(isMatched = true)
                            } else student
                        }
                    }

                    val matchResponse = MatchResponse(isMatch = isMatch, matchId = matchId)
                    responseString = gson.toJson(matchResponse)
                } else {
                    responseCode = 400
                    responseString = """{"error":"Parámetros de consulta likerId o likedId faltantes"}"""
                }
            }

            // GET /api/v1/matches/chat/{matchId}: Obtiene el historial de mensajes reales de un chat
            path.contains("/api/v1/matches/chat/") && method.equals("GET", ignoreCase = true) -> {
                val matchId = path.substringAfter("/api/v1/matches/chat/")
                val filtered = messages.filter { it.matchId == matchId }
                responseString = gson.toJson(filtered)
            }

            // POST /api/v1/matches/chat: Envía y guarda un mensaje de chat real
            path.endsWith("/api/v1/matches/chat") && method.equals("POST", ignoreCase = true) -> {
                val buffer = Buffer()
                request.body()?.writeTo(buffer)
                val requestBody = buffer.readUtf8()
                try {
                    val messageReq = gson.fromJson(requestBody, MessageRequest::class.java)
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val newMessage = MessageResponse(
                        id = UUID.randomUUID().toString(),
                        matchId = messageReq.matchId,
                        senderId = messageReq.senderId,
                        content = messageReq.content,
                        timestamp = sdf.format(Date()),
                        createdAt = messageReq.createdAt
                    )
                    messages.add(newMessage)
                    responseString = "{}"
                } catch (e: Exception) {
                    e.printStackTrace()
                    responseCode = 400
                    responseString = """{"error":"JSON de mensaje inválido"}"""
                }
            }
        }

        val mediaType = MediaType.parse("application/json")
        val responseBody = ResponseBody.create(mediaType, responseString)

        return Response.Builder()
            .code(responseCode)
            .message(if (responseCode == 200) "OK" else "Bad Request")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(responseBody)
            .addHeader("content-type", "application/json")
            .build()
    }
}
