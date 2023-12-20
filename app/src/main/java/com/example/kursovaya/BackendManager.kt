package com.example.kursovaya

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

// Определите интерфейс для взаимодействия с бэкэндом
interface BackendService {
    @Multipart
    @POST("process_voice_message")
    suspend fun uploadAudio(@Part audioFile: MultipartBody.Part): Response<String>
}

// Класс для работы с бэкэндом
class BackendManager {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS) //  таймаут на подключение
        .writeTimeout(120, TimeUnit.SECONDS)   //  таймаут на запись
        .readTimeout(120, TimeUnit.SECONDS)    //  таймаут на чтение
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://95.84.193.71:60/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .client(okHttpClient)
        .build()

    private val backendService = retrofit.create(BackendService::class.java)

    suspend fun sendAudioToBackend(audioFile: File): String {
        val requestFile = RequestBody.create("audio/*".toMediaTypeOrNull(), audioFile)
        Log.d("BM", audioFile.path)
        val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, requestFile)
        val response = backendService.uploadAudio(audioPart)
        if (response.isSuccessful) {
            return response.body() ?: "Empty response"
        } else {
            throw IOException("Error uploading audio to backend: ${response.message()}")
        }
    }
}
