package com.emon.videoplayerdemo

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.emon.videoplayerdemo.databinding.ActivityMainBinding
import com.emon.videoplayerdemo.utils.AudioRecorder
import com.emon.videoplayerdemo.utils.AudioReplace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val permissionRequestCode = 123
    private lateinit var videoView: VideoView


    private lateinit var audioRecorder: AudioRecorder
    private var selectedVideoUri: Uri? = null
    private var finalVideoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        videoView = findViewById<View>(R.id.videoView) as VideoView
        audioRecorder = AudioRecorder("${externalCacheDir?.absolutePath}/voiceover.mp3")

        binding.selectVideoBtn.setOnClickListener {
            if (checkPermissions()) {
                selectVideoFromStorage()
            } else {
                requestPermissions()
            }
        }

        binding.recordBtn.setOnClickListener {
            if (selectedVideoUri != null) {
                val startTime = binding.startTimeET.text.toString().toLong()
                val endTime = binding.endTimeET.text.toString().toLong()
                if (startTime.toString().isNotEmpty() && endTime.toString().isNotEmpty()) {
                    recordVoiceOver(startTime, endTime)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.please_enter_start_and_end_time),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.playBtn.setOnClickListener {
            finalVideoPath?.let { it1 -> playVideoWithReplacedAudio(it1) }
        }
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedVideoUri = result.data?.data
            binding.startTimeET.isVisible = true
            binding.endTimeET.isVisible = true
            binding.recordBtn.isVisible = true
        }
    }

    private fun checkPermissions(): Boolean {

        val readStoragePermission =
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        val writeStoragePermission =
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

        val recordAudioPermission =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)

        return readStoragePermission == PackageManager.PERMISSION_GRANTED &&
                writeStoragePermission == PackageManager.PERMISSION_GRANTED &&
                recordAudioPermission == PackageManager.PERMISSION_GRANTED

    }

    private fun requestPermissions() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.RECORD_AUDIO
            ),
            permissionRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectVideoFromStorage()
            }
        }
    }

    private fun selectVideoFromStorage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(intent)
    }

    private fun recordVoiceOver(startTime: Long, endTime: Long) {
        audioRecorder.startRecording(this)

        val duration = endTime - startTime
        binding.recordBtn.isEnabled = false
        binding.selectVideoBtn.isEnabled = false
        binding.playBtn.isEnabled = false
        binding.recordingLottieView.isVisible = true

        lifecycleScope.launch {

            delay(duration * 60_000.toLong())
            audioRecorder.stopRecording()

            withContext(Dispatchers.Main) {
                binding.recordBtn.isEnabled = true
                binding.selectVideoBtn.isEnabled = true
                binding.playBtn.isEnabled = true
                binding.recordingLottieView.isVisible = false
                binding.progressBar.isVisible = true
                binding.playBtn.isVisible = true
                binding.videoView.isVisible = true
            }

            val margeAudioJob = async(Dispatchers.Main) {
                val externalAudioPath = "${externalCacheDir?.absolutePath}/voiceover.mp3"
                val mainVideoPath = selectedVideoUri?.let { getPathFromUri(applicationContext, it) }
                finalVideoPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path + "/abcdFinal.mp4"

                AudioReplace.replaceAudio(
                    mainVideoPath ?: "",
                    externalAudioPath,
                    finalVideoPath ?: "",
                    startTime,
                    endTime,
                    isAudioReplaced = {
                        binding.progressBar.isVisible = false
                    })
            }
            margeAudioJob.await()
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        val scheme = uri.scheme

        if (scheme == "file") {
            return uri.path
        }

        if (scheme == "content") {
            val contentResolver: ContentResolver = context.contentResolver
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    return cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
                Log.e("Error", "Error getting path from URI: ${e.message}")
            } finally {
                cursor?.close()
            }
        }

        return null
    }

    private fun playVideoWithReplacedAudio(videoPath: String) {
        val videoUri = Uri.fromFile(File(videoPath))
        videoView.setVideoURI(videoUri)
        videoView.start()
    }
}
