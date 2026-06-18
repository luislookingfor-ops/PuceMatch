package com.example.pucematch.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Interceptor de OkHttp que intercepta las llamadas HTTP y devuelve respuestas simuladas.
 * Esto permite probar Retrofit de manera realista (incluyendo simulación de latencia)
 * sin requerir un servidor activo en internet.
 */
class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url().encodedPath()
        val method = request.method()

        // Simular latencia de red de 600ms para probar el feedback visual de carga
        try {
            Thread.sleep(600)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val responseString = when {
            path.endsWith("/api/v1/students") && method.equals("GET", ignoreCase = true) -> {
                // Catálogo de estudiantes con tipos de match específicos (Educativo, Recreacional, Sentimental)
                """
                [
                  {
                    "id": "1",
                    "name": "Yulieth Galarza",
                    "career": "Ingeniería en Software",
                    "interests": "Kotlin,Jetpack Compose,UI Design,Android,Material Design 3",
                    "bio": "Desarrolladora de UI interactiva. Me encanta crear interfaces fluidas y componentes optimizados.",
                    "avatarUri": null,
                    "matchType": "Educativo",
                    "isMatched": false
                  },
                  {
                    "id": "2",
                    "name": "Jorge López",
                    "career": "Ingeniería en Software",
                    "interests": "Room,Clean Architecture,SQL,Kotlin,Coroutines",
                    "bio": "Enfocado en base de datos locales e infraestructura robusta. Offline-first lover.",
                    "avatarUri": null,
                    "matchType": "Educativo",
                    "isMatched": false
                  },
                  {
                    "id": "3",
                    "name": "Kevin Cevallos",
                    "career": "Ingeniería en Sistemas",
                    "interests": "Retrofit,APIs,Git,Backend Integration,Testing",
                    "bio": "Especialista en integración remota y consumo de servicios HTTP fiables.",
                    "avatarUri": null,
                    "matchType": "Recreacional",
                    "isMatched": false
                  },
                  {
                    "id": "4",
                    "name": "María Belén",
                    "career": "Diseño Multimedios",
                    "interests": "Figma,UX Research,Branding,Illustrator,Visual Design",
                    "bio": "Diseñadora de interfaces digitales buscando crear la mejor experiencia estudiantil.",
                    "avatarUri": null,
                    "matchType": "Sentimental",
                    "isMatched": false
                  },
                  {
                    "id": "5",
                    "name": "Daniel Proaño",
                    "career": "Negocios Internacionales",
                    "interests": "Marketing,Finanzas,Liderazgo,Idiomas,Tech Startups",
                    "bio": "Emprendedor tecnológico. Me interesa el ecosistema de apps móviles y negocios.",
                    "avatarUri": null,
                    "matchType": "Recreacional",
                    "isMatched": false
                  }
                ]
                """.trimIndent()
            }
            path.endsWith("/api/v1/matches/chat") && method.equals("POST", ignoreCase = true) -> {
                // Simula el envío de un mensaje
                "{}"
            }
            else -> "{}"
        }

        val mediaType = MediaType.parse("application/json")
        val responseBody = ResponseBody.create(mediaType, responseString)

        return Response.Builder()
            .code(200)
            .message("OK")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(responseBody)
            .addHeader("content-type", "application/json")
            .build()
    }
}
