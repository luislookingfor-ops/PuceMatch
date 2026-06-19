package com.example.pucematch

import android.app.Application
import android.content.Context
import com.example.pucematch.data.local.AppDatabase
import com.example.pucematch.data.remote.MockInterceptor
import com.example.pucematch.data.remote.PuceMatchApi
import com.example.pucematch.data.repository.PuceMatchRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Indicador de configuración global para cambiar fácilmente entre:
 * - true: Servidor virtual en memoria local (MockInterceptor).
 * - false: Conexión real a un backend de producción/desarrollo (Retrofit real).
 */
const val USE_MOCK_API = false

/**
 * URL base del servidor remoto.
 * - Si pruebas en emulador con servidor local: use "http://10.0.2.2:8080/"
 * - Si pruebas con servidor en red local o celular real: use la IP de tu PC "http://192.168.1.XX:8080/"
 */
const val BASE_URL = "https://pucematch-backend.onrender.com/"

class PuceMatchApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: PuceMatchRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Inicializar base de datos
        database = AppDatabase.getDatabase(this)

        // 2. Construir cliente OkHttp conditionally injecting MockInterceptor
        val clientBuilder = OkHttpClient.Builder()
        if (USE_MOCK_API) {
            clientBuilder.addInterceptor(MockInterceptor())
        }

        // 3. Crear cliente Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(PuceMatchApi::class.java)

        // 4. Instanciar repositorio
        repository = PuceMatchRepository(
            userDao = database.userDao(),
            messageDao = database.messageDao(),
            api = api
        )
    }

    /**
     * Métodos utilitarios para gestionar las credenciales de la sesión activa en el dispositivo.
     */
    fun getCurrentUserId(): String {
        val sharedPrefs = getSharedPreferences("pucematch_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("current_user_id", "") ?: ""
    }

    fun saveCurrentUserId(userId: String) {
        val sharedPrefs = getSharedPreferences("pucematch_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("current_user_id", userId).apply()
    }
}
