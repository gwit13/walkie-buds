package com.example.walkie_buds

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.Exception

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val RECORDER_SAMPLERATE = 44100
private const val RECORDER_CHANNELS = 16
private const val RECORDER_AUDIO_ENCODING = ENCODING_PCM_16BIT


class MainActivity : AppCompatActivity() {

    private var recordButton: Button? = null //Streaming button
    private var recorder: AudioRecord? = null //Input from mic
    private var intSize: Int = 0 //used for buffer size
    private lateinit var at: AudioTrack //reads from recorder, outputs to audio device

    //for proper permission handling
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    private var isRecording = false //for switching button text/functionality
    private var isDisabled = false //in order to safely exit while loop within thread

    //asks once, remembers
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if(requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if(!permissionToRecordAccepted) finish()
    }

    //handler for the button toggle: depending on start, turn on or off.
    private fun onRecord(start: Boolean) = if (start) {
        startRecording()
    } else {
        stopRecording()
    }


    private fun startRecording() {
        var a = 0 //used for refreshing the AudioTrack object every 32 calls

        var bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) //finding the minimum allowable buffer with current values (at the top)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_PERFORMANCE,
                    RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING,
                    bufferSize
                ) //mic input
            }
            else{
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING,
                    bufferSize
                ) //mic input
            }
        recorder!!.startRecording() //start listening

        //initializes AudioTrack using intSize initialized in onCreate
        at = AudioTrack(
            AudioManager.STREAM_MUSIC,
            RECORDER_SAMPLERATE,
            AudioFormat.CHANNEL_OUT_MONO,
            RECORDER_AUDIO_ENCODING,
            intSize,
            AudioTrack.MODE_STREAM
        )

        //used for safe interrupt of while loop within thread
        isRecording = true
        isDisabled = false

        var rightThread = Thread(Runnable {
            try {
                val sData = ByteArray(bufferSize) //buffer data format

                while (isRecording) {
                    println(recorder!!.read(sData, 0, bufferSize)) //isRecording = false; onStop button
                    at.play()
                    // Write the byte array to the track
                    at.write(sData, 0, sData.size)
                    //stops the playing for this cycle
                    at.stop()
                    //refreshes AudioTrack object every 32 cycles due to limitations with the class
                    if(a > 32) {
                        at.release()
                        at = AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            RECORDER_SAMPLERATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            RECORDER_AUDIO_ENCODING,
                            intSize,
                            AudioTrack.MODE_STREAM
                        )
                        a = 0
                    }
                    a++ //increment

                    //safe interrupt for the thread
                    if(isDisabled) {
                        isRecording = false
                        at.release()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }, "Right Mic Thread")
        rightThread.start()

    }

    private fun stopRecording() {
        isDisabled = true

        recorder?.apply {
            stop()
            release()
        }
        recorder = null

    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        supportActionBar?.hide()


        intSize = AudioTrack.getMinBufferSize(
            RECORDER_SAMPLERATE,
            AudioFormat.CHANNEL_OUT_MONO,
            RECORDER_AUDIO_ENCODING
        )

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
    }

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
    }
}