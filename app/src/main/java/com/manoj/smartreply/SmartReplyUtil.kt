package com.manoj.smartreply

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult
import java.io.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SmartReplyUtil() {
    companion object {
        val TAG = "SmartReplyUtil"
        private var statusCount = 0

        public fun getStatusCount() : Int{
            return statusCount
        }
        @Synchronized public fun updateStatusCount() {
            statusCount = statusCount + 1
        }
    }

    private var inputStream: InputStream? = null

    private var smartReply: FirebaseSmartReply? = null
    private var bufferedWriter: BufferedWriter? = null
//    private var currentLine:String? = null
    private var taskCount = 0
    private var completedTaskCount = 0

    private var startTime = System.currentTimeMillis()
    private var endTime = System.currentTimeMillis()

    public var fileReader: BufferedReader? = null
    public var appContext:Context? = null
    public var fileUri: Uri? = null

    private var threadPoolExecutor:ThreadPoolExecutor? = null

    fun updateUI(message:String) {
        Log.d(SmartReplyUtil.TAG, "updateUI: " + message)
        var intent = Intent(appContext, FileConverstionActivity::class.java)
        intent.action = SmartReplyJobService.UPDATE_UI
        intent.putExtra("message", message)
        LocalBroadcastManager.getInstance(appContext!!).sendBroadcast(intent)
        return

    }


    fun start() {
        if (inputStream != null) {
            inputStream!!.close()
            inputStream = null
        }

        if (fileReader != null) {
            fileReader!!.close()
            fileReader = null
        }

        SmartReplyUtil.statusCount = 0;

        inputStream = appContext!!.contentResolver.openInputStream(fileUri!!)
        fileReader = BufferedReader(InputStreamReader(inputStream))

        if (smartReply == null) {
            smartReply = FirebaseNaturalLanguage.getInstance().smartReply
            val file = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "smart_reply_output.csv")
            bufferedWriter = BufferedWriter(FileWriter(file.path))
        }


        completedTaskCount = 0
        taskCount = 0
        startTime = System.currentTimeMillis()


        val threadPoolMaxSize = 64
        threadPoolExecutor = ThreadPoolExecutor(1, threadPoolMaxSize, 0,
            TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>())
        var line: String? = fileReader!!.readLine()

        while(line != null) {
            var currentPoolSize = taskCount - SmartReplyUtil.statusCount

            Log.d(SmartReplyUtil.TAG, "Pool size: " + currentPoolSize + " ==> " + taskCount)

            if (currentPoolSize > threadPoolMaxSize - 8) {
                Log.d(SmartReplyUtil.TAG, "Pool size: " + currentPoolSize + ", on sleep")
                Thread.sleep(100)
                continue
            }
            taskCount = taskCount + 1
            threadPoolExecutor!!.execute(
                SmartReplyRunnable(line!!, bufferedWriter, smartReply!!, appContext!!)
            )

            line = fileReader!!.readLine()
        }

        Executors.newSingleThreadExecutor().execute {
                checkStatus()
        }
    }

    fun checkStatus() {
        if (threadPoolExecutor == null) {
            return
        }

        val current = SmartReplyUtil.statusCount

        if (current == taskCount) {
            endTime = System.currentTimeMillis()
            val diff = (endTime - startTime)/(1000)

            threadPoolExecutor = null
            updateUI("Done, total $current/$taskCount records processed\nOutput: " +
                    "Downloads/smart_reply_output.csv\nTime: $diff(s)")
            Log.d(SmartReplyUtil.TAG, "Done, total $current/$taskCount " +
                    "records processed\nOutput: " +
                    "Downloads/smart_reply_output.csv\nTime: $diff(s)")
            bufferedWriter!!.close()
            bufferedWriter = null
            inputStream!!.close()
            inputStream = null
            fileReader!!.close()
            fileReader = null
            smartReply!!.close()
            smartReply = null
            val scheduler = appContext!!.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            scheduler.cancel(SmartReplyJobService.JOB_ID)
        } else {
            Thread.sleep(100)
            checkStatus()
        }
    }
}


// Implementing the Runnable interface to implement threads.
class SmartReplyRunnable(lineP: String,
                         bufferedWriterP: BufferedWriter?, smartReplyP: FirebaseSmartReply,
                         appContextP:Context): Runnable {

    private var bufferedWriter = bufferedWriterP
    private var line = lineP
    private var smartReply: FirebaseSmartReply = smartReplyP
    private var appContext = appContextP

    public override fun run() {
        Log.d(SmartReplyUtil.TAG, "Started thread")
        processLine(line)

    }

    fun processLine(line:String)  {
        var currentLine = line
        val conversation = ArrayList<FirebaseTextMessage>()

        conversation.add(
            FirebaseTextMessage.createForRemoteUser(
                line.toString(), System.currentTimeMillis() - 1000, "Manoj"))

            smartReply.suggestReplies(conversation)
            .addOnSuccessListener { result ->

                var value = currentLine
                value = value.replace("\"", "\\\"")
                value = "\"" + value + "\""
                var resultText = value + ","

                if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                    // The conversation's language isn't supported, so the
                    // the result doesn't contain any suggestions.
                    updateUIInParent("NOT_SUPPORTED_LANGUAGE")
                } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    // Task completed successfully
                    for (suggestion in result.suggestions) {
                        var value = suggestion.text
                        value = value.replace("\"", "\\\"")
                        value = "\"" + value + "\""
                        resultText = resultText + value + ",";
                        value = suggestion.confidence.toString()
                        value = value.replace("\"", "\\\"")
                        value = "\"" + value + "\""
                        resultText = resultText + value + ",";
                    }
                }

                resultText = resultText + "\n"

                bufferedWriter!!.write(resultText)
                bufferedWriter!!.flush()
                updateUIInParent("Completed")
            }
            .addOnFailureListener {
                updateUIInParent("Failure: $it")
            }
    }


    fun updateUIInParent(message:String) {
        SmartReplyUtil.updateStatusCount()
        Log.d(SmartReplyUtil.TAG, "updateUIInParent: " + message)
        var intent = Intent(appContext, FileConverstionActivity::class.java)
        intent.action = SmartReplyJobService.UPDATE_UI
        intent.putExtra("message", message)
        intent.putExtra("type", SmartReplyJobService.TYPE_PROCESSED)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent)
        return
    }
}