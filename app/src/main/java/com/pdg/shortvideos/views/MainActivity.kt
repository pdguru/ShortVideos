package com.pdg.shortvideos.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.pdg.shortvideos.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val TAG = "CAMERA_APP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkRequiredPermissions()) {
            showAlert()
        }

        question1CL.setOnClickListener {
            val intent = Intent(this, AnswerActivity::class.java).apply {
                putExtra("question", "Briefly introduce yourself.")
                putExtra("id", "Question1")
            }
            startActivity(intent)
        }

        question2CL.setOnClickListener {
            val intent = Intent(this, AnswerActivity::class.java).apply {
                putExtra("question", "Where was your last holiday?")
                putExtra("id", "Question2")
            }
            startActivity(intent)
        }

        question3CL.setOnClickListener {
            val intent = Intent(this, AnswerActivity::class.java).apply {
                putExtra("question", "What are your hobbies?")
                putExtra("id", "Question3")
            }
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "✅ App started.")

        if (SharedPref.getAnswer("Question1", this@MainActivity) != "") {
            question1CL.setBackgroundColor(getResources().getColor(R.color.lightGreen))
        } //else question1CL.setBackgroundColor(getResources().getColor(R.color.lightRed))

        if (SharedPref.getAnswer("Question2", this@MainActivity) != "") {
            question2CL.setBackgroundColor(getResources().getColor(R.color.midGreen))
        } //else question2CL.setBackgroundColor(getResources().getColor(R.color.midRed))

        if (SharedPref.getAnswer("Question3", this@MainActivity) != "") {
            question3CL.setBackgroundColor(getResources().getColor(R.color.darkGreen))
        } //else question3CL.setBackgroundColor(getResources().getColor(R.color.darkRed))
    }

    //PermissionsManager
    val REQUEST_CODE = 103
    val permissionsRequired = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    fun checkRequiredPermissions(): Boolean {
        Log.i(TAG, "⚠️ Checking for required permission.")
        for (permission in permissionsRequired) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "👍 $permission permission available.")
            } else {
                Log.d(TAG, "❌ $permission permission unavailable.")
                return false
            }
        }
        return true
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            permissionsRequired,
            REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Log.i(TAG, "❌ permissions have been denied by user")
                    showAlert()
                } else {
                    Log.i(TAG, "👍 permissions have been granted by user")
                }
            }
        }
    }

    fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Short Videos")
        builder.setMessage("This app will not work without the required permissions. ")
        builder.setPositiveButton("Grant") { dialog, which ->
            dialog.dismiss()
            requestPermissions()
        }
        builder.setNegativeButton("Close") { dialog, which -> finish() }

        val dialog = builder.create()
        dialog.show()
    }
}
