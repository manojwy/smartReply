package com.manoj.smartreply

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult
import java.io.*

class SmartReplyUtil(handler: Handler) {

    private var inputStream: InputStream? = null

    private var smartReply: FirebaseSmartReply? = null
    private var bufferedWriter: BufferedWriter? = null
    private var currentLine:String? = null
    private var count = 1

    private var handler = handler

    public var fileReader: BufferedReader? = null
    public var appContext:Context? = null
    public var fileUri: Uri? = null

    fun updateUI(message:String) {
        val msg = handler.obtainMessage()
        msg.what = 1
        msg.obj = message
        handler.sendMessage(msg)
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
                    Environment.DIRECTORY_DOWNLOADS), "smart_reply_output.csv")
            bufferedWriter = BufferedWriter(FileWriter(file.path))
        }

        var line: String = fileReader!!.readLine()
        processLine(line)
    }

    fun processLine(line:String?)  {
        if (line == null) {
            updateUI("Done, total $count records processed\nOutput: Downloads/smart_reply_output.csv")
            bufferedWriter!!.close()
            bufferedWriter = null
            currentLine = null
            smartReply!!.close()
            smartReply = null
            inputStream!!.close()
            inputStream = null
            fileReader!!.close()
            fileReader = null
            count = 1
            return
        }

        currentLine = line
        val conversation = ArrayList<FirebaseTextMessage>()

        conversation.add(
            FirebaseTextMessage.createForRemoteUser(
                line.toString(), System.currentTimeMillis() - 1000, "Manoj"))

        smartReply!!.suggestReplies(conversation)
            .addOnSuccessListener { result ->

                var value = currentLine!!
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
                updateUI("Processed: $count")

                count = count + 1
                val line = fileReader!!.readLine()
                processLine(line)
            }
            .addOnFailureListener {
                updateUI("Exception: $it")
            }
    }

}
