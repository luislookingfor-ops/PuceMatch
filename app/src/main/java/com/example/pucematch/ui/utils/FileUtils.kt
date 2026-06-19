package com.example.pucematch.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
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

/**
 * Convierte una URI de imagen en un String Base64 comprimido para sincronización remota.
 * Redimensiona a un tamaño máximo (ej. 300x300) y comprime a JPEG para mantener el tamaño muy pequeño.
 */
fun convertUriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        
        // Redimensionar para optimizar peso (máximo 300px en el lado más largo)
        val maxDimension = 300
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        // Comprimir a JPEG con calidad 75%
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        
        // Retornar en formato data URI Base64
        "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Decodifica un String Base64 en un Bitmap.
 */
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val cleanStr = if (base64Str.startsWith("data:image")) {
            base64Str.substringAfter(",")
        } else {
            base64Str
        }
        val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Carga un Bitmap desde una cadena que puede ser un Base64 o una ruta local.
 */
fun loadProfileBitmap(avatarUri: String?): Bitmap? {
    if (avatarUri.isNullOrEmpty()) return null
    return if (avatarUri.startsWith("data:image") || avatarUri.length > 500) {
        decodeBase64ToBitmap(avatarUri)
    } else {
        try {
            BitmapFactory.decodeFile(avatarUri)
        } catch (e: Exception) {
            null
        }
    }
}
