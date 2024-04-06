package com.eaters.eaters

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eaters.eaters.adapters.MessageAdapter
import com.eaters.eaters.models.Message
import com.eaters.eaters.models.SendMessageRequest
import com.eaters.eaters.models.SendMessageResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var sendButton: Button
    private lateinit var messageInput: EditText
    private lateinit var microphoneButton: ImageButton
    private lateinit var messagingBar: LinearLayout
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var progressBar: ProgressBar
    private lateinit var nameStr: String
    private var messageCounter: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_activity)

        nameStr = intent.getStringExtra("userName")?.toTitleCase().toString()

        initializeUI()
        initializeSpeechRecognizer()
        // Initialize TextToSpeech.
        textToSpeech = TextToSpeech(this, this)
    }

    private fun initializeUI() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        sendButton = findViewById(R.id.buttonSend)
        messageInput = findViewById(R.id.editTextMessage)
        microphoneButton = findViewById(R.id.buttonMicrophone)

        messagingBar = findViewById(R.id.layoutMessage)

        progressBar = findViewById(R.id.progressBar)

        messageAdapter = MessageAdapter(mutableListOf())
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }

        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                hideMessagingBar()
                sendMessageToChatService(messageText, nameStr)
                messageInput.text.clear()
            }
        }

        microphoneButton.setOnClickListener {
            if (isMicrophonePermissionGranted()) {
                speechRecognizer?.startListening(recognizerIntent)
            } else {
                requestMicrophonePermission()
            }
        }

        messageInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                // Call your send message method here
                val messageText = messageInput.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    hideMessagingBar()
                    sendMessageToChatService(messageText, nameStr)
                    messageInput.text.clear()
                    true
                }
                false// Return true to consume the action
            } else {
                false // Return false to let the system handle the event further
            }
        }

        if(intent.hasExtra("cvreply")){
            val firstReply = intent.getStringExtra("cvreply")
            val receivedMessage = firstReply?.let { Message("bot", it) }
            receivedMessage?.let { messageAdapter.addMessage(it) }

        }

        if(intent.hasExtra("userName")){
            val firstPrompt = "Hi I'm " + (intent.getStringExtra("userName")?.toTitleCase() ?: "-") + " and I'm ready for the interview"
//            val firstReply = "Hi " + (intent.getStringExtra("userName")?.toTitleCase() ?: "-") + "!\nWelcome to this mock interview. Are you ready to start our interview?"
            val request = SendMessageRequest(message = firstPrompt, name = (intent.getStringExtra("userName")?.toTitleCase().toString()))
            RetrofitClient.service.sendMessage(request).enqueue(object : Callback<SendMessageResponse> {
                override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                    progressBar.visibility = View.GONE // Hide ProgressBar on response
                    if (response.isSuccessful) {
                        response.body()?.let { responseBody ->
                            val receivedMessage = Message("bot", responseBody.reply)
                            runOnUiThread {
                                messageAdapter.addMessage(receivedMessage)
                                messageCounter += 1
                                showMessagingBar()

                                // Use Text to Speech to say the received message
                                textToSpeech.speak(responseBody.reply, TextToSpeech.QUEUE_FLUSH, null, "")
                            }
                        }
                    } else {
                        // Handle request error
                        showMessagingBar()
                        Toast.makeText(applicationContext, "Something error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE // Hide ProgressBar on failure
                    // Handle network error
                    showMessagingBar()
                    Toast.makeText(applicationContext, "Check your internet connection", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    Toast.makeText(applicationContext, "Speech recognition error", Toast.LENGTH_SHORT).show()
                }
                override fun onResults(results: Bundle) {
                    // Extract the list of recognized speech results
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        // The most confident result is the first entry in the list
                        // (if you're using the default speech recognizer settings).
                        val text = matches[0]

                        // Use the recognized text as needed
                        messageInput.setText(text) // For example, set it to an EditText.

                        // If you want to automatically send the recognized text as a message:
                        sendMessageToChatService(text, nameStr)
                        messageInput.text.clear()
                    } else {
                        Toast.makeText(applicationContext, "No speech input recognized.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }

    }

    private fun sendMessageToChatService(text: String, name: String) {
        progressBar.visibility = View.VISIBLE // Show the ProgressBar when request starts
        val request = SendMessageRequest(message = text, name = name)
        val userMessage = Message("user", text)
        messageAdapter.addMessage(userMessage)

        RetrofitClient.service.sendMessage(request).enqueue(object : Callback<SendMessageResponse> {
            override fun onResponse(call: Call<SendMessageResponse>, response: Response<SendMessageResponse>) {
                progressBar.visibility = View.GONE // Hide ProgressBar on response
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val receivedMessage = Message("bot", responseBody.reply)
                        runOnUiThread {
                            messageAdapter.addMessage(receivedMessage)
                            messageCounter += 1
                            showMessagingBar()

                            // Use Text to Speech to say the received message
                            textToSpeech.speak(responseBody.reply, TextToSpeech.QUEUE_FLUSH, null, "")
                        }
                    }
                } else {
                    // Handle request error
                    showMessagingBar()
                    Toast.makeText(applicationContext, "Something error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                progressBar.visibility = View.GONE // Hide ProgressBar on failure
                // Handle network error
                showMessagingBar()
                Toast.makeText(applicationContext, "Check your internet connection", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun showMessagingBar() {
        messagingBar.visibility = View.VISIBLE
    }

    private fun hideMessagingBar() {
        messagingBar.visibility = View.GONE
    }

    private fun isMicrophonePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MICROPHONE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                microphoneButton.performClick()
            } else {
                Toast.makeText(this, "Microphone permission is required for speech recognition", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.UK) // or any other language
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Check if user can resolve missing data
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                if (installIntent.resolveActivity(packageManager) != null) {
                    // Prompt the user to install the data
                    Toast.makeText(this, "Installing TTS data...", Toast.LENGTH_SHORT).show()
                    startActivity(installIntent)
                } else {
                    // Data cannot be installed, notify the user
                    Toast.makeText(this, "TTS Language data is missing and cannot be installed.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_MICROPHONE_PERMISSION_CODE = 1
    }

    fun String.toTitleCase(): String = this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_feedback -> {
                // Implement your feedback action here
                if(messageCounter > 2){
                    val intent = Intent(this@MainActivity, FeedbackActivity::class.java)
                    intent.putExtra("userName", nameStr)
                    startActivity(intent)
                    finish()
                    true
                } else {
                    Toast.makeText(this, "You cannot open feedback now. Please answer more questions", Toast.LENGTH_SHORT).show()
                    true
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
