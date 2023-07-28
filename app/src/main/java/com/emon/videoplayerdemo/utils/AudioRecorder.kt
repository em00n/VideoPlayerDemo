package com.emon.videoplayerdemo.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.IOException

class AudioRecorder(private val outputFile: String) {
    var mediaRecorder: MediaRecorder? = null

    fun startRecording(context: Context) {
        mediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
    }
}
