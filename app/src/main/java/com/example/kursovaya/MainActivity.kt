package com.example.kursovaya

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.Kursovaya.R
import com.example.kursovaya.RealPathUtil.getRealPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.util.UUID
import com.google.gson.Gson

data class MessageList(val messages: List<Message>)

class MainActivity : AppCompatActivity() {
    private lateinit var backendManager: BackendManager
    private lateinit var responseTextView: TextView
    private val Messages = mutableListOf<Message>()
    private var audioFilePath: String? = null
    private var isRecording = false
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var buttonHistory: Button
    private lateinit var sendFileButton: Button
    private val context: Context = this

    companion object {
        private const val RECORD_AUDIO_PERMISSION = 123
        private const val RS_PERMISSION = 456
        private const val MSG_STORY_FILE_NAME = "msg_story.json"
        private const val FILE_REQUEST_CODE = 456
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("AAAAAAAAAAAAAAAAAAAAA", "Requesting READ_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                RS_PERMISSION
            )
        } else {
            Log.d("AAAAAAAAAAAAAAAAAAAAA", "READ_EXTERNAL_STORAGE permission already granted")
        }
        backendManager = BackendManager()
        responseTextView = findViewById(R.id.responseTextView)
        sendFileButton = findViewById(R.id.sendFileButton)
        sendFileButton.setOnClickListener {
            openFileChooser()
        }
        val startRecordingButton: Button = findViewById(R.id.startRecordingButton)
        startRecordingButton.setOnClickListener {
            if (isRecording) {
                // Если запись активна, остановите запись
                stopRecording()
                startRecordingButton.text = "Record"
            } else {
                // Если запись не активна, начните новую запись
                checkPermissionsAndStartRecording()
                startRecordingButton.text = "Stop"
            }
        }

        buttonHistory = findViewById(R.id.buttonHistory)
        buttonHistory.setOnClickListener {
            val intent = Intent(this, MsgList::class.java)
            intent.putExtra("messagesList", ArrayList(Messages))
            startActivity(intent)
        }
        audioFilePath = generateRandomAudioFileName()
        loadMessagesFromFile()
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        startActivityForResult(intent, FILE_REQUEST_CODE)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            Log.d("asd",getRealPath(context,uri))
            sendFileToBackend(getRealPath(context,uri))
        }
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendFileToBackend(filePath: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("back", filePath)
                val response = backendManager.sendAudioToBackend(File(filePath ?: ""))
                responseTextView.text = response.replace('"',' ')

                val currentTime: LocalDateTime = LocalDateTime.now()
                val msg = Message(
                    text = response.replace('"',' '),
                    date = currentTime.toString(),
                    audioPath = filePath
                )
                Messages.add(msg)
                saveMessagesToFile()
            } catch (e: IOException) {
                responseTextView.text = "Error: ${e.message}"
            }
        }

    }

    private fun generateRandomAudioFileName(): String {
        val uniqueID: String = UUID.randomUUID().toString()
        return "${externalCacheDir?.absolutePath}/audio_$uniqueID.3gp"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadMessagesFromFile() {
        try {
            val file = File(externalCacheDir?.absolutePath, MSG_STORY_FILE_NAME)
            if (file.exists()) {
                val json = file.readText()
                val gson = Gson()

                val messageList = gson.fromJson(json, MessageList::class.java)
                Messages.clear()

                for (message in messageList.messages) {
                    Messages.add(message)
                }

                Log.d("TG", "Messages loaded from file: ${externalCacheDir?.absolutePath}/$MSG_STORY_FILE_NAME")
            } else {
                Log.d("TG", "File not found: ${externalCacheDir?.absolutePath}/$MSG_STORY_FILE_NAME")
            }
        } catch (e: Exception) {
            Log.e("TG", "Error loading messages from file: ${e.message}")
        }
    }

    private fun saveMessagesToFile() {
        try {
            val messageList = MessageList(Messages)
            val gson = Gson()
            val json = gson.toJson(messageList)

            try {
                val file = File(externalCacheDir?.absolutePath, MSG_STORY_FILE_NAME)
                file.writeText(json)
                Log.d("TG", "Messages saved to file: ${externalCacheDir?.absolutePath}/$MSG_STORY_FILE_NAME")
            } catch (e: Exception) {
                Log.e("TG", "Error saving messages to file: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("TG", "Error converting messages to JSON: ${e.message}")
        }
    }

    private fun checkPermissionsAndStartRecording() {
        val recordAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )

        if (recordAudioPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION
            )
        } else {
            startRecordingAndSendToBackend()
        }
    }

    private fun startRecordingAndSendToBackend() {
        if (!isRecording) {
            // Начните новую запись только если запись не активна
            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder.setOutputFile(audioFilePath)

            try {
                mediaRecorder.prepare()
                mediaRecorder.start()
                isRecording = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun stopRecording() {
        // Остановка записи и отправка аудио на бэкэнд
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (isRecording) {
                    mediaRecorder.stop()
                    mediaRecorder.release()

                    // Отправка аудио на бэкэнд
                    val response = backendManager.sendAudioToBackend(File(audioFilePath ?: ""))
                    responseTextView.text = response.replace('"',' ')

                    val currentTime: LocalDateTime = LocalDateTime.now()
                    val msg = Message(
                        text = response.replace('"',' '),
                        date = currentTime.toString(),
                        audioPath = audioFilePath.toString()
                    )
                    Messages.add(msg)
                    saveMessagesToFile()
                }
            } catch (e: IOException) {
                responseTextView.text = "Error: ${e.message}"
            } finally {
                isRecording = false
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecordingAndSendToBackend()
            }
        }
    }
}
