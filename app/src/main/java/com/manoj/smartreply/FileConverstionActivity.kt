package com.manoj.smartreply

import android.app.Activity
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

                var message = intent.extras.getString("message")
                var type = intent.extras.getString("type")
                Log.d(SmartReplyUtil.TAG, "BroadcastReceiver: " + message)

                if (type.equals(SmartReplyJobService.TYPE_PROCESSED)) {
                    message = message + " -> " + SmartReplyUtil.getStatusCount()
                }
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

        val suggestion = findViewById<Button>(R.id.button)
        suggestion.setOnClickListener {

            if (checkPermission() == false) {
                return@setOnClickListener
            }

            if (filePath != null) {
                if (isJobRunning(SmartReplyJobService.JOB_ID)) {
                    Toast.makeText(applicationContext, "Wait!, I'm busy", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val componentName = ComponentName(applicationContext, SmartReplyJobService::class.java!!)

                val builder = JobInfo.Builder(SmartReplyJobService.JOB_ID, componentName)
                val bundle = PersistableBundle()
                bundle.putString("file_path", filePath)
                builder.setOverrideDeadline(0);
                builder.setExtras(bundle)

                val scheduler = applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                scheduler.schedule(builder.build())

            } else {
                Toast.makeText(applicationContext, "No file selected, choose a file", Toast.LENGTH_LONG).show()
            }

        }

        val button = findViewById<Button>(R.id.chooseFile)
        button.setOnClickListener {
            val filePathNode = findViewById<TextView>(R.id.filePath)
            filePathNode.setText("")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }

            startActivityForResult(intent, READ_REQUEST_CODE)
        }

//        val cancelBtn = findViewById<Button>(R.id.cancel_current_job)
//        cancelBtn.setOnClickListener {
//            val scheduler = applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
//            scheduler.cancel(SmartReplyJobService.JOB_ID)
//        }
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
            Toast.makeText(applicationContext, "Require storage permission from app info", Toast.LENGTH_LONG).show()
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
