package com.example.pucematch

import android.app.Application
import com.example.pucematch.data.local.AppDatabase
import com.example.pucematch.data.remote.MockInterceptor
import com.example.pucematch.data.remote.PuceMatchApi
import com.example.pucematch.data.repository.PuceMatchRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Clase Application personalizada para configurar la inyección manual
 * y el ciclo de vida de los Singletons de datos (Room, Retrofit y Repositorio).
 */
class PuceMatchApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: PuceMatchRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Inicializar la base de datos local
        database = AppDatabase.getDatabase(this)

        // 2. Configurar el cliente OkHttp con nuestro MockInterceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MockInterceptor())
            .build()

        // 3. Inicializar el cliente Retrofit apuntando a una URL base de pruebas
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.pucematch.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(PuceMatchApi::class.java)

        // 4. Instanciar el Repositorio global
        repository = PuceMatchRepository(
            userDao = database.userDao(),
            messageDao = database.messageDao(),
            api = api
        )
    }
}
