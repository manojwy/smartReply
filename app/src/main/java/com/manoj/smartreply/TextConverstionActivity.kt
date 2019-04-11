package com.manoj.smartreply

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult
import android.content.Context.INPUT_METHOD_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.view.inputmethod.InputMethodManager


class TextConverstionActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = R.layout.activity_text_converstion
        setContentView(view)

        val heTextNode = findViewById<EditText>(R.id.he)
        val resultTextNode = findViewById<EditText>(R.id.result)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {

            val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(heTextNode.getWindowToken(), 0)

            FirebaseApp.initializeApp(this);
            val conversation = ArrayList<FirebaseTextMessage>()

            conversation.add(FirebaseTextMessage.createForRemoteUser(
                heTextNode.text.toString(), System.currentTimeMillis() - 1000, "Manoj"))

            resultTextNode.setText("")

            val smartReply = FirebaseNaturalLanguage.getInstance().smartReply
            smartReply.suggestReplies(conversation)
                .addOnSuccessListener { result ->
                    if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                        // The conversation's language isn't supported, so the
                        // the result doesn't contain any suggestions.
                        resultTextNode.setText("Error: STATUS_NOT_SUPPORTED_LANGUAGE")
                    } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                        // Task completed successfully
                        var resultText = ""
                        var count = 1
                        for (suggestion in result.suggestions) {
                            resultText = resultText + "$count. " + suggestion.text + " (" + suggestion.confidence.toString() + ") \n"
                            count = count + 1
                        }
                        resultTextNode.setText(resultText)
                    } else {
                        resultTextNode.setText("No result")
                    }
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    // ...
                    resultTextNode.setText("Exception: $it")
                }
        }

    }

}
