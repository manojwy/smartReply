package com.manoj.smartreply

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp
import android.os.*
import android.provider.ContactsContract
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.View
import android.widget.EditText
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import android.provider.OpenableColumns
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import java.util.concurrent.Executors


class FileConversionActivity : AppCompatActivity() {

    val READ_REQUEST_CODE: Int = 42
    var filePath:String? = null
    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Utils.UPDATE_UI) {
                val message = intent.extras.getString("message")
                val msg = findViewById<EditText>(R.id.result)
                msg.setText(message)
            } else if (intent.action == Utils.COMPLETED_JOB) {
                val message = intent.extras.getString("message")
                val msg = findViewById<EditText>(R.id.result)
                msg.setText(message)
                val chooseFileBtn = findViewById<Button>(R.id.chooseFile)
                chooseFileBtn.visibility = View.VISIBLE
                val suggestionBtn = findViewById<Button>(R.id.button)
                suggestionBtn.visibility = View.VISIBLE
                val stopJobBtn = findViewById<Button>(R.id.stopjob)
                stopJobBtn.visibility = View.GONE
            }
        }
    }


    fun isJobRunning(jobId: Int): Boolean {
//        val scheduler = applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
//
//        var hasBeenScheduled = false
//
//        for (jobInfo in scheduler.allPendingJobs) {
//            if (jobInfo.id == jobId) {
//                hasBeenScheduled = true
//                break
//            }
//        }
//
//        return hasBeenScheduled

        return SmartReplyUtil.IS_RUNNING
    }

    private fun requestPermission() {
        val requestPermissionArray = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, requestPermissionArray, 100)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.size == 0) {
            showToast("Require storage permission from app info")
            return
        }

        val grantResult = grantResults[0]

        if(grantResult == PackageManager.PERMISSION_GRANTED && requestCode == 100) {
            checkJobStatus();
        } else {
            showToast("Require storage permission from app info")
        }
    }

    fun checkJobStatus() {
        if (SmartReplyUtil.IS_RUNNING == true) {
            val chooseFileBtn = findViewById<Button>(R.id.chooseFile)
            chooseFileBtn.visibility = View.GONE
            val suggestionBtn = findViewById<Button>(R.id.button)
            suggestionBtn.visibility = View.GONE
            val stopJobBtn = findViewById<Button>(R.id.stopjob)
            stopJobBtn.visibility = View.VISIBLE
            var srcFilename = Utils.getSourceFilenameFromPref(applicationContext)
            val filePathNode = findViewById<TextView>(R.id.filePath)
            filePathNode.setText(srcFilename)
            filePath = srcFilename
        } else if (Utils.isCrashRecovery(applicationContext)) {
            var srcFilename = Utils.getSourceFilenameFromPref(applicationContext)
            val filePathNode = findViewById<TextView>(R.id.filePath)
            filePathNode.setText(srcFilename)
            filePath = srcFilename
            scheduleJob()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = R.layout.activity_file_conversion
        setContentView(view)

        registerReceiver();
        setTitle("SmartReply Util: Using file")

        // check permission
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
            showToast("Require storage permission")
        } else {
            checkJobStatus();
        }

        val suggestion = findViewById<Button>(R.id.button)
        suggestion.setOnClickListener {

            if (checkPermission() == false) {
                return@setOnClickListener
            }

            if (filePath != null) {
                if (isJobRunning(Utils.JOB_ID)) {
                    showToast("Wait!, I'm busy")
                    return@setOnClickListener
                }

                scheduleJob()
            } else {
                showToast("No file selected, choose a file")
            }

        }

        val chooseFileBtn = findViewById<Button>(R.id.chooseFile)
        chooseFileBtn.setOnClickListener {

            if (isJobRunning(Utils.JOB_ID)) {
                showToast("Wait!, I'm busy")
                return@setOnClickListener
            }

            openFilePicker()
        }

        var textConvert = findViewById<Button>(R.id.textConvert)
        textConvert.setOnClickListener {
            startActivity(Intent(this@FileConversionActivity, TextConversionActivity::class.java))
        }

        val stopJobBtn = findViewById<Button>(R.id.stopjob)
        stopJobBtn.setOnClickListener {

            if (SmartReplyUtil.IS_RUNNING == true) {

                val builder = AlertDialog.Builder(this)

                // Set the alert dialog title
                builder.setTitle("SmartReply Util")

                // Display a message on alert dialog
                builder.setMessage("Are you sure to stop the current JOB?")

                // Set a positive button and its click listener on alert dialog
                builder.setPositiveButton("YES"){dialog, which ->
                    val scheduler = applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    scheduler.cancel(Utils.JOB_ID)
                    SmartReplyUtil.IS_RUNNING = false
                    val chooseFileBtn = findViewById<Button>(R.id.chooseFile)
                    chooseFileBtn.visibility = View.VISIBLE
                    val suggestionBtn = findViewById<Button>(R.id.button)
                    suggestionBtn.visibility = View.VISIBLE
                    val stopJobBtn = findViewById<Button>(R.id.stopjob)
                    stopJobBtn.visibility = View.GONE
                }

                // Display a neutral button on alert dialog
                builder.setNeutralButton("NO"){_,_ ->

                }

                // Finally, make the alert dialog using builder
                val dialog: AlertDialog = builder.create()

                // Display the alert dialog on app interface
                dialog.show()
            }
        }
    }

    fun showToast(msg:String) {
        val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG)
        val view = toast.getView()
        val text = view.findViewById<TextView>(android.R.id.message)
        text.setTextColor(applicationContext.getColor(android.R.color.holo_red_dark))
        toast.show()

    }

    fun openFilePicker() {
        val filePathNode = findViewById<TextView>(R.id.filePath)
        filePathNode.setText("")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }



            var uri = Uri.parse(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS).path)

        intent.setDataAndType(uri, "text/*")

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    fun scheduleJob() {

        val chooseFileBtn = findViewById<Button>(R.id.chooseFile)
        chooseFileBtn.visibility = View.GONE
        val suggestionBtn = findViewById<Button>(R.id.button)
        suggestionBtn.visibility = View.GONE
        val stopJobBtn = findViewById<Button>(R.id.stopjob)
        stopJobBtn.visibility = View.VISIBLE

        val componentName = ComponentName(applicationContext, SmartReplyJobService::class.java!!)

        val builder = JobInfo.Builder(Utils.JOB_ID, componentName)
        val bundle = PersistableBundle()
        bundle.putString("file_path", filePath)
        builder.setOverrideDeadline(0);
        builder.setExtras(bundle)

        val scheduler = applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.schedule(builder.build())
    }

    override fun onStop() {
//        unRegisterReceiver()
        super.onStop()
    }

    fun checkPermission(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            showToast("Require storage permission from app info")
            return false;
        }
    }

    fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            FirebaseApp.initializeApp(this);

            resultData?.data?.also { uri ->
                var fname = getFileName(uri);

                val srcFile = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ), fname
                )

                if (srcFile == null) {
                    findViewById<TextView>(R.id.filePath).setText("Invalid file!, file need to be in Download folder")
                } else {
                    filePath = fname
                    findViewById<TextView>(R.id.filePath).setText(filePath)
                    Utils.updatePrefWithSourceFilename(applicationContext, filePath!!)
                    findViewById<EditText>(R.id.result).setText("")
                }
            }
        }
    }


    fun registerReceiver() {
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(broadCastReceiver, IntentFilter(Utils.UPDATE_UI))

        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(broadCastReceiver, IntentFilter(Utils.COMPLETED_JOB))

    }

    fun unRegisterReceiver() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadCastReceiver)
    }
}
