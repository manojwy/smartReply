package com.manoj.smartreply

import android.app.Activity
import android.app.AlertDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.EditText


class FileConverstionActivity : AppCompatActivity() {

    val READ_REQUEST_CODE: Int = 42
    var filePath:String? = null
    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SmartReplyJobService.UPDATE_UI) {
                val message = intent.extras.getString("message")
                val msg = findViewById<EditText>(R.id.result)
                msg.setText(message)
            }
        }
    }


    fun isJobRunning(jobId: Int): Boolean {
        val scheduler = applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        var hasBeenScheduled = false

        for (jobInfo in scheduler.allPendingJobs) {
            if (jobInfo.id == jobId) {
                hasBeenScheduled = true
                break
            }
        }

        return hasBeenScheduled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = R.layout.activity_file_converstion
        setContentView(view)


        registerReceiver();

        var filename = Utils.getSourceFilenameFromPref(applicationContext)


        if (filename != null && filename.isEmpty() == false) {
            val filePathNode = findViewById<TextView>(R.id.filePath)
            filePathNode.setText(filename)
            filePath = filename
        } else {
            Utils.updatePrefWithFilename(applicationContext, "")
            Utils.updatePrefWithLineNo(applicationContext, 0)
        }


        val suggestion = findViewById<Button>(R.id.button)
        suggestion.setOnClickListener {

            if (checkPermission() == false) {
                return@setOnClickListener
            }

            if (filePath != null) {
                if (isJobRunning(SmartReplyJobService.JOB_ID)) {
                    showToast("Wait!, I'm busy")
                    return@setOnClickListener
                }


                var filename = Utils.getFilenameFromPref(applicationContext)

                if (filename != null && filename.isEmpty() == false) {

                    val alertDialogBuilder = AlertDialog.Builder(this)

                    alertDialogBuilder.setTitle("Smart Reply Util")
                    alertDialogBuilder.setMessage("Looks like last job was not completed successfully, do you want to continue?")
                    alertDialogBuilder.setPositiveButton("Continue") {dialog, which ->
                        scheduleJob()
                    }
                    alertDialogBuilder.setNegativeButton("Restart") {dialog, which ->
                        Utils.updatePrefWithFilename(applicationContext, "")
                        Utils.updatePrefWithLineNo(applicationContext, 0)
                        scheduleJob()
                    }
                    alertDialogBuilder.setNeutralButton("Cancel") {dialog, which ->
                    }
                    val dialog: AlertDialog = alertDialogBuilder.create()

                    // Display the alert dialog on app interface
                    dialog.show()
                } else {
                    scheduleJob()
                }

            } else {
                showToast("No file selected, choose a file")
            }

        }

        val button = findViewById<Button>(R.id.chooseFile)
        button.setOnClickListener {

            if (isJobRunning(SmartReplyJobService.JOB_ID)) {
                showToast("Wait!, I'm busy")
                return@setOnClickListener
            }

            var filename = Utils.getFilenameFromPref(applicationContext)

            if (filename != null && filename.isEmpty() == false) {

                val alertDialogBuilder = AlertDialog.Builder(this)

                alertDialogBuilder.setTitle("Smart Reply Util")
                alertDialogBuilder.setMessage("Looks like last job was not completed successfully, do you want to continue?")
                alertDialogBuilder.setPositiveButton("Continue") {dialog, which ->
                    scheduleJob()
                }
                alertDialogBuilder.setNegativeButton("Restart") {dialog, which ->
                    Utils.updatePrefWithFilename(applicationContext, "")
                    Utils.updatePrefWithLineNo(applicationContext, 0)
                    scheduleJob()
                }
                alertDialogBuilder.setNeutralButton("Cancel") {dialog, which ->
                    openFilePicker()
                }
                val dialog: AlertDialog = alertDialogBuilder.create()

                // Display the alert dialog on app interface
                dialog.show()
            } else {
                openFilePicker()
            }
        }

//        val cancelBtn = findViewById<Button>(R.id.cancel_current_job)
//        cancelBtn.setOnClickListener {
//            val scheduler = applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
//            scheduler.cancel(SmartReplyJobService.JOB_ID)
//        }
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

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    fun scheduleJob() {
        val componentName = ComponentName(applicationContext, SmartReplyJobService::class.java!!)

        val builder = JobInfo.Builder(SmartReplyJobService.JOB_ID, componentName)
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
                val filePathNode = findViewById<TextView>(R.id.filePath)
                filePathNode.setText(uri.getPath())
                filePath = uri.toString()
                Utils.updatePrefWithSourceFilename(applicationContext, filePath!!)
//                Log.i("MM", "Uri: $uri")
//                Log.i("MM", "filePath: $filePath")
            }
        }
    }

    fun registerReceiver() {


        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(broadCastReceiver, IntentFilter(SmartReplyJobService.UPDATE_UI))
    }

    fun unRegisterReceiver() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadCastReceiver)
    }
}
