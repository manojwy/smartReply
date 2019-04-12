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
import com.manoj.smartreply.SmartReplyUtil
import java.util.jar.Manifest
import android.graphics.Bitmap
import android.os.Message


class FileConverstionActivity : AppCompatActivity() {

    val READ_REQUEST_CODE: Int = 42


    val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what === 1) {
                val resultTextNode = findViewById<TextView>(R.id.result)
                resultTextNode.setText(msg.obj as String)
            }
            super.handleMessage(msg)
        }
    }

    var smartReplyUtil = SmartReplyUtil(handler)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = R.layout.activity_file_converstion
        setContentView(view)

        val suggestion = findViewById<Button>(R.id.button)
        suggestion.setOnClickListener {

            if (checkPermission() == false) {
                return@setOnClickListener
            }

            if (smartReplyUtil.fileUri != null) {
                if (smartReplyUtil.fileReader != null) {
                    Toast.makeText(applicationContext, "Wait!, I'm busy", Toast.LENGTH_LONG).show()
                } else {
                    smartReplyUtil.start()
                }
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

        smartReplyUtil.appContext = applicationContext

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
                smartReplyUtil.fileUri = uri
            }
        }
    }


}
