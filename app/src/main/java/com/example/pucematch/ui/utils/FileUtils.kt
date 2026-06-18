package com.example.pucematch.ui.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Copia un archivo desde una URI de contenido externo al almacenamiento interno privado de la app.
 * Esto asegura que la URI temporal del selector de fotos no expire y la imagen persista localmente.
 * Retorna la ruta absoluta del archivo copiado.
 */
fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Crear un archivo único en el directorio de archivos de la app
        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
