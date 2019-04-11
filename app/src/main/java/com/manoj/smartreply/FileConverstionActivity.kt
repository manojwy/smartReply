package com.manoj.smartreply

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult
import java.io.*
import java.util.jar.Manifest


class FileConverstionActivity : AppCompatActivity() {

    val READ_REQUEST_CODE: Int = 42

    private var inputStream: InputStream? = null
    private var fileReader: BufferedReader? = null
    private var smartReply: FirebaseSmartReply? = null
    private var bufferedWriter: BufferedWriter? = null
    private var currentLine:String? = null
    private var count = 1
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = R.layout.activity_file_converstion
        setContentView(view)

        val suggestion = findViewById<Button>(R.id.button)
        suggestion.setOnClickListener {

            if (checkPermission() == false) {
                return@setOnClickListener
            }

            if (fileUri != null) {

                if (inputStream != null) {
                    inputStream!!.close()
                    inputStream = null
                }

                if (fileReader != null) {
                    fileReader!!.close()
                    fileReader = null
                }

                inputStream = contentResolver.openInputStream(fileUri!!)
                fileReader = BufferedReader(InputStreamReader(inputStream))

                if (smartReply == null) {
                    smartReply = FirebaseNaturalLanguage.getInstance().smartReply
                    val file = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "smart_reply_output.csv")
                    bufferedWriter = BufferedWriter(FileWriter(file.path))
                }

                var line: String = fileReader!!.readLine()
                processLine(line)
            } else {
                Toast.makeText(applicationContext, "No file selected", Toast.LENGTH_LONG).show()
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
                Log.i("MM", "Uri: $uri")
                val filePathNode = findViewById<TextView>(R.id.filePath)
                filePathNode.setText(uri.getPath())
                fileUri = uri
            }
        }
    }

    fun processLine(line:String?)  {
        if (line == null) {
            val resultTextNode = findViewById<TextView>(R.id.result)
            resultTextNode.setText("Done, total $count records processed\nOutput: Downloads/smart_reply_output.csv")
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

        conversation.add(FirebaseTextMessage.createForRemoteUser(
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
                    val resultTextNode = findViewById<TextView>(R.id.result)
                    resultTextNode.setText("Error: STATUS_NOT_SUPPORTED_LANGUAGE")
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
                val resultTextNode = findViewById<TextView>(R.id.result)
                resultTextNode.setText("Processed: $count")

                count = count + 1
                val line = fileReader!!.readLine()
                processLine(line)
            }
            .addOnFailureListener {
                // Task failed with an exception
                val resultTextNode = findViewById<TextView>(R.id.result)
                println("MM $it")
                resultTextNode.setText("Exception: $it")
            }
    }
}
