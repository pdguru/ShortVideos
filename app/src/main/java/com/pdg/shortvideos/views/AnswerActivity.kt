package com.pdg.shortvideos.views

import android.hardware.Camera
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import com.pdg.shortvideos.R
import com.pdg.shortvideos.R.id.*
import kotlinx.android.synthetic.main.activity_answers.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AnswerActivity : AppCompatActivity(), SurfaceHolder.Callback {

    val TAG = "CAMERA_APP"
    var answerState = "preview"
    var answerAttempt = 1
    lateinit var questionID: String
    var videoPath: String? = null
    var submitClick = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answers)

        val question = intent.getStringExtra("question")
        questionTextView.text = question
        questionID = intent.getStringExtra("id")

        surfaceView = findViewById(R.id.surfaceView)

    }

    override fun onStart() {
        super.onStart()

        videoPath = SharedPref.getAnswer(questionID, this@AnswerActivity)

        if (videoPath != "") {

            beginVideo.visibility = View.GONE
            submitVideo.visibility = View.GONE
            progressBar.visibility = View.GONE

            showVideoCaptured()

            cancelVideo.setOnClickListener { btn ->
                finish()
            }
        } else {

            videoView.visibility = View.GONE
            surfaceView?.visibility = View.VISIBLE

            submitVideo.visibility = View.GONE

            showCameraPreview()
            answerState = "preview"
            startTimer(answerState)

            beginVideo.setOnClickListener { btn ->
                updateUI("record")
            }

            cancelVideo.setOnClickListener { btn ->
                finish()
            }

            submitVideo.setOnClickListener { btn ->
                if (submitClick == 2) {
                    finish()
                } else {
                    showVideoCaptured()
                    updateUI("done")
                    submitClick++
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (timerState == TimerState.Running) {
            timerState = TimerState.Paused
            Log.d(TAG, "ℹ️ Timer paused")
        }
        releaseMediaRecorder() // if you are using MediaRecorder, release it first
        releaseCamera() // release the camera immediately on pause event
    }

    override fun onStop() {
        super.onStop()
        if (timerState == TimerState.Running || timerState == TimerState.Paused) {
            timerState = TimerState.Stopped
            timer.cancel()
            Log.d(TAG, "ℹ️ Timer stopped")
        }
    }


    private fun updateUI(state: String) {
        Log.d(TAG, "🔄 Number of attempts: $answerAttempt")
        Log.d(TAG, "📷 State: $state")
        if (answerAttempt <= 2) {
            when (state) {
                "record" -> {
                    surfaceView?.visibility = View.VISIBLE
                    videoView.visibility = View.GONE
                    releaseCamera()
                    label.visibility = View.GONE
                    submitVideo.visibility = View.VISIBLE
                    progressBar.max = 45; progressBar.progress = 0
                    timerLengthSeconds = 45; secondsRemaining = 45
                    prepareVideoRecorder()
                    startTimer("record")
                }
                "preview" -> {
                    label.visibility = View.VISIBLE
                    submitVideo.visibility = View.GONE
                    progressBar.max = 45; progressBar.progress = 0
                    timerLengthSeconds = 45; secondsRemaining = 45
                    startTimer("preview")
                }
                "done" -> {
                    releaseMediaRecorder()
                    label.visibility = View.VISIBLE
                    submitVideo.visibility = View.VISIBLE
                    progressBar.max = 60; progressBar.progress = 0
                    timerLengthSeconds = 60; secondsRemaining = 60
                    showVideoCaptured()
                    startTimer("preview")
                }
            }
        } else {
            Log.d(TAG, "💾 Two attempts done.")
            //save video to question number
            Toast.makeText(this@AnswerActivity, "Your response has been recorded.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    fun showVideoCaptured() {
        surfaceView?.visibility = View.GONE
        videoView.visibility = View.VISIBLE
        Log.d(TAG, "👍 $videoPath")
        videoView.setVideoPath(videoPath)
        videoView.start()
    }

    //TimerManager
    // Timer values and variables
    enum class TimerState { Running, Paused, Stopped }

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 45
    private var secondsRemaining: Long = 45
    private var timerState = TimerState.Stopped

    fun startTimer(state: String) {
        timerState = TimerState.Running

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000

                //Update button text
                when (state) {
                    "preview" -> {
                        beginVideo.text = "Recording begins in $secondsRemaining seconds. \nClick here to record."
                        answerState = "record"
                    }
                    "record" -> {
                        beginVideo.text = "Recording... \n$secondsRemaining seconds remaining."
                        answerState = "done"
                    }
                }

                //update progressbar
                progressBar.progress = (timerLengthSeconds - secondsRemaining).toInt()
            }
        }.start()
    }

    fun onTimerFinished() {
        Log.i(TAG, "⏰ Timer timed out.")
        updateUI(answerState)
    }

    //camera
    //Camera variables
    var videoURI: Uri? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var camera: Camera? = null
    private var surfaceView: SurfaceView? = null
    private var mMediaRecorder: MediaRecorder? = null

    fun showCameraPreview() {
        Log.d(TAG, "ℹ️ Starting camera preview")
        surfaceHolder = surfaceView?.holder
        surfaceHolder?.addCallback(this)
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
        camera!!.setDisplayOrientation(90)
        try {
            camera!!.setPreviewDisplay(surfaceHolder)
            camera!!.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        //resetCamera
        if (surfaceHolder!!.surface == null) {
            Log.d(TAG, "❌ Surface holder does not exists")
            return
        }

        // Stop if preview surface is already running.
        camera!!.stopPreview()
        try {
            // Set preview display
            camera!!.setPreviewDisplay(surfaceHolder)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Start the camera preview...
        camera!!.startPreview()

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
//        if (camera != null) {
        camera?.stopPreview()
        camera?.release()
        camera = null
//        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        showCameraPreview()
    }

    //record video
    private fun prepareVideoRecorder() {
        Log.d(TAG, "🎥 preparing video recorder.")
        answerAttempt++
        mMediaRecorder = MediaRecorder()
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
        camera!!.setDisplayOrientation(90)
        camera?.let { camera ->
            // Step 1: Unlock and set camera to MediaRecorder
            camera?.unlock()

            mMediaRecorder?.run {
                setCamera(camera)

                // Step 2: Set sources
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)

                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
                // This step conflicts with step 5
                // setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))

                // Step 4: Set output file
                setOutputFile(getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO).toString())

                // Step 5: Set the preview output
                setPreviewDisplay(surfaceView?.holder?.surface)

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)


                // Step 6: Prepare configured MediaRecorder
                try {
                    prepare()
                    Log.d(TAG, "🎥 video recorder ready.")
                    start()
                    Log.d(TAG, "🎥 recording video.")
                } catch (e: IllegalStateException) {
                    Log.d(TAG, "IllegalStateException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                } catch (e: IOException) {
                    Log.d(TAG, "IOException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                }
            }

        }

    }

    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "ShortVideos"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d("ShortVideos", "failed to create directory")
                    return null
                }
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
            }
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                videoPath = "${mediaStorageDir.path}${File.separator}VID_$questionID.mp4"
                Log.d(TAG, "💾 Saving video at $videoPath")
                SharedPref.setAnswer(
                    questionID,
                    "${mediaStorageDir.path}${File.separator}VID_$questionID.mp4",
                    this@AnswerActivity
                )
                File("${mediaStorageDir.path}${File.separator}VID_$questionID.mp4")

            }
            else -> null
        }
    }

    private fun releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            Log.d(TAG, "⚠️ stopping media recorder")
            mMediaRecorder?.stop()
            mMediaRecorder?.reset() // clear recorder configuration
            mMediaRecorder?.release() // release the recorder object
            mMediaRecorder = null
            camera?.lock() // lock camera for later use
            releaseCamera()
        }
    }

    private fun releaseCamera() {
        camera?.release() // release the camera for other applications
        camera = null
    }
}