package com.example.walkie_buds

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


private const val LOG_TAG = "WalkieBuds"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_CHANNELS = 16
private const val RECORDER_AUDIO_ENCODING = ENCODING_PCM_16BIT


class MainActivity : AppCompatActivity() {
    private var fileName: String = ""

    private var recordButton: Button? = null
    private var recorder: AudioRecord? = null

    private var player: MediaPlayer? = null

    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    private lateinit var at: AudioTrack
    private var isRecording = false


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if(requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if(!permissionToRecordAccepted) finish()
    }

    private fun onRecord(start: Boolean) = if (start) {
        startRecording()
    } else {
        stopRecording()
    }


    private fun startRecording() {

        var bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING)
//        println(bufferSize)
//        bufferSize += 2048
        var bytesPerElement = 2

        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize)

        recorder!!.startRecording()

        isRecording = true

        var recordingThread = Thread(Runnable {
            try {
                val sData = ByteArray(bufferSize)

                while (isRecording) {
                    recorder!!.read(sData, 0, bufferSize) //isRecording = false; onStop button
                    at.play()
                    // Write the byte array to the track
                    println(at.write(sData, 0, sData.size))
                    at.stop()
                    //at.release()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }, "Transmitter Thread")
        recordingThread.start()

    }

    private fun stopRecording() {
        isRecording = false
//        at.stop()
        at.release()

        recorder?.apply {
            stop()
            release()
        }
        recorder = null

    }

    internal inner class RecordButton(ctx: Context) : androidx.appcompat.widget.AppCompatButton(ctx) {
        var mStartPlaying = true;
        var clicker: OnClickListener = OnClickListener {
            onRecord(mStartPlaying)
            text = when(mStartPlaying) {
                true -> "Stop transmitting"
                false -> "Start transmitting"
            }
            mStartPlaying = !mStartPlaying
        }

        init {
            text = "Start transmitting"
            setOnClickListener(clicker)
        }
    }



    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setContentView(R.layout.activity_main)

        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        val intSize = AudioTrack.getMinBufferSize(
            RECORDER_SAMPLERATE,
            AudioFormat.CHANNEL_OUT_MONO,
            RECORDER_AUDIO_ENCODING
        )

        at = AudioTrack(
            AudioManager.STREAM_MUSIC,
            RECORDER_SAMPLERATE,
            AudioFormat.CHANNEL_OUT_MONO,
            RECORDER_AUDIO_ENCODING,
            intSize,
            AudioTrack.MODE_STREAM
        )

//        recordButton = RecordButton(this)

        recordButton = record
        var mStartPlaying = true
        recordButton?.setOnClickListener {
            onRecord(mStartPlaying)

            recordButton!!.text = when(mStartPlaying) {
                true -> "Disable Microphone Sharing"
                false -> "Enable Microphone Sharing"
            }
            mStartPlaying = !mStartPlaying
        }

//        val ll = LinearLayout(this).apply {
//            addView(recordButton, LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    0f))
//        }
//        setContentView(ll)
    }

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }
}