package com.emon.videoplayerdemo.utils

import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File

object AudioReplace {
    fun replaceAudio(
        mainVideoPath: String,
        externalAudioPath: String,
        finalVideoPath: String,
        startTime: Long,
        endTime: Long,
        isAudioReplaced: (Boolean) -> Unit
    ) {

        val startTimeInSeconds = startTime * 60
        val externalAudioDuration = (endTime - startTime) * 60

        try {

            val outputFile = File(finalVideoPath)
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // Command to replace audio using FFmpeg
            val ffmpegCommand = arrayOf(
                "-i", mainVideoPath,
                "-i", externalAudioPath,
                "-filter_complex",
                "[0:a]atrim=0:${startTimeInSeconds}[main_audio_start];" +
                        "[0:a]atrim=${startTimeInSeconds + externalAudioDuration},asetpts=PTS-STARTPTS[main_audio_end];" +
                        "[1:a]atrim=0:$externalAudioDuration,asetpts=PTS-STARTPTS[external_audio];" +
                        "[main_audio_start][external_audio][main_audio_end]concat=n=3:v=0:a=1[out]",
                "-map", "0:v", "-map", "[out]",
                "-c:v", "copy",
                "-y", finalVideoPath
            )

            val result = FFmpeg.execute(ffmpegCommand)
            isAudioReplaced.invoke(result != Config.RETURN_CODE_SUCCESS)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}