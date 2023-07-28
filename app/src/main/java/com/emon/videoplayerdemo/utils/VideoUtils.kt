package com.emon.videoplayerdemo.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore

object VideoUtils {

    fun getVideoDuration(context: Context, videoUri: Uri): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        val durationInMillis = durationString?.toLong() ?: 0
        val durationInSeconds = durationInMillis / 1000

        val minutes = durationInSeconds / 60
        val remainingSeconds = durationInSeconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    fun getVideoSize(context: Context, videoUri: Uri): String {
        val cursor = context.contentResolver.query(videoUri, null, null, null, null)
        cursor?.use {
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            it.moveToFirst()

            val sizeInKB = it.getLong(sizeIndex) / 1024
            val sizeInMB = sizeInKB / 1024
            return String.format("%.2f", sizeInMB.toDouble())
        }
        return ""
    }
}
