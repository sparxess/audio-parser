package com.example.kursovaya

import android.media.MediaRecorder
import java.io.IOException

class AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    fun startRecording(outputFile: String) {
        if (!isRecording) {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder?.setOutputFile(outputFile)

            try {
                mediaRecorder?.prepare()
                mediaRecorder?.start()
                isRecording = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        if (isRecording) {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        }
    }
}
