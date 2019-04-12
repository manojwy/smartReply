package com.manoj.smartreply

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.TextView
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult
import java.io.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SmartReplyUtil(handler: Handler?) {

    private var inputStream: InputStream? = null

    private var smartReply: FirebaseSmartReply? = null
    private var bufferedWriter: BufferedWriter? = null
//    private var currentLine:String? = null
    private var taskCount = 0
    private var completedTaskCount = 0

    private var handler = handler

    private var startTime = System.currentTimeMillis()
    private var endTime = System.currentTimeMillis()

    public var fileReader: BufferedReader? = null
    public var appContext:Context? = null
    public var fileUri: Uri? = null

    private var threadPoolExecutor:ThreadPoolExecutor? = null

    val taskHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what === 2) {
                completedTaskCount = completedTaskCount + 1
            } else if(msg.what === 1) {
                updateUI(msg.obj as String)
            }
            super.handleMessage(msg)
        }
    }


    fun updateUI(message:String) {
        if (handler == null) {
            Log.d("MM - updateUI", message)
            var intent = Intent(appContext, FileConverstionActivity::class.java)
            intent.action = SmartReplyJobService.UPDATE_UI
            intent.putExtra("message", message)
//            appContext!!.sendBroadcast(intent)
            LocalBroadcastManager.getInstance(appContext!!).sendBroadcast(intent)
            return
        }

        val msg = handler!!.obtainMessage()
        msg.what = 1
        msg.obj = message
        handler!!.sendMessage(msg)
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

        inputStream = appContext!!.contentResolver.openInputStream(fileUri!!)
        fileReader = BufferedReader(InputStreamReader(inputStream))

        if (smartReply == null) {
            smartReply = FirebaseNaturalLanguage.getInstance().smartReply
            val file = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "2smart_reply_output.csv")
            bufferedWriter = BufferedWriter(FileWriter(file.path))
        }


        completedTaskCount = 0
        taskCount = 0
        startTime = System.currentTimeMillis()

//        threadPoolExecutor = ThreadPoolExecutor(16, 32, 10,
        threadPoolExecutor = ThreadPoolExecutor(1, 128, 0,
            TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>())
        var line: String? = fileReader!!.readLine()
        while(line != null) {
            taskCount = taskCount + 1
            threadPoolExecutor!!.execute(
                SmartReplyRunnable(line!!, taskCount, handler, taskHandler, bufferedWriter, smartReply!!)
            )
            line = fileReader!!.readLine()
        }

        Handler().postDelayed({
            checkStatus()
        }, 2)
    }

    fun checkStatus() {
        if (threadPoolExecutor == null) {
            return
        }

        if (completedTaskCount == taskCount) {

            endTime = System.currentTimeMillis()
            val diff = (endTime - startTime)/(1000)

            threadPoolExecutor = null
            updateUI("Done, total $completedTaskCount records processed\nOutput: Downloads/smart_reply_output.csv\nTime: $diff(s)")
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
            updateUI("Total $completedTaskCount/$taskCount records processed")

            Handler().postDelayed({
                checkStatus()
            }, 100)
        }
    }
}


// Implementing the Runnable interface to implement threads.
class SmartReplyRunnable(lineP: String, countP: Int, handlerP: Handler?, taskCompletionHandlerP: Handler,
                         bufferedWriterP: BufferedWriter?, smartReplyP: FirebaseSmartReply): Runnable {

    private var handler = handlerP
    private var taskCompletionHandler = taskCompletionHandlerP
    private var bufferedWriter = bufferedWriterP
    private var line = lineP
    private var count = countP
    private var smartReply: FirebaseSmartReply = smartReplyP

    public override fun run() {

        processLine(line)

    }

    fun processLine(line:String)  {
//        if (line == null) {
//            updateUI("Done, total $count records processed\nOutput: Downloads/smart_reply_output.csv")
//            bufferedWriter!!.close()
//            bufferedWriter = null
//            currentLine = null
//            smartReply!!.close()
//            smartReply = null
//            inputStream!!.close()
//            inputStream = null
//            fileReader!!.close()
//            fileReader = null
//            count = 1
//            return
//        }

        var currentLine = line
        val conversation = ArrayList<FirebaseTextMessage>()

        conversation.add(
            FirebaseTextMessage.createForRemoteUser(
                line.toString(), System.currentTimeMillis() - 1000, "Manoj"))

//        FirebaseNaturalLanguage.getInstance().smartReply!!.
            smartReply.suggestReplies(conversation)
            .addOnSuccessListener { result ->

                var value = currentLine
                value = value.replace("\"", "\\\"")
                value = "\"" + value + "\""
                var resultText = value + ","

                if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                    // The conversation's language isn't supported, so the
                    // the result doesn't contain any suggestions.
                    updateUI("Error: STATUS_NOT_SUPPORTED_LANGUAGE")
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

                updateTaskCompletion()
            }
            .addOnFailureListener {
                updateUI("Exception: $it")
                updateTaskCompletion()
            }
    }


    fun updateTaskCompletion() {
        val msg = taskCompletionHandler.obtainMessage()
        msg.what = 2
        taskCompletionHandler.sendMessage(msg)
    }
    fun updateUI(message:String) {
        if (handler == null) {
            Log.d("MM", message)
            val msg = taskCompletionHandler.obtainMessage()
            msg.what = 1
            msg.obj = message
            taskCompletionHandler.sendMessage(msg)
            return
        }
        val msg = handler!!.obtainMessage()
        msg.what = 1
        msg.obj = message
        handler!!.sendMessage(msg)
    }
}