package com.eaters.eaters

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.eaters.eaters.interfaces.ApiService
import com.eaters.eaters.models.SendMessageResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SendCVActivity : AppCompatActivity() {

    private lateinit var textViewFileName: TextView
    private lateinit var selectedFileUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_cv)

        textViewFileName = findViewById(R.id.textViewFileName)
        val buttonChooseFile: Button = findViewById(R.id.buttonChooseFile)
        val buttonUpload: Button = findViewById(R.id.buttonUpload)
        val buttonProceedWithoutCV: Button = findViewById(R.id.buttonProceedWithoutCV)
        val nameEditText: EditText = findViewById(R.id.editTextName)

        // Set up button listeners
        buttonChooseFile.setOnClickListener {
            openFilePicker()
        }

        buttonUpload.setOnClickListener {
            uploadFile()
        }

        buttonProceedWithoutCV.setOnClickListener {
            val name = nameEditText.text.toString().trim() // Trim to remove leading and trailing spaces
            if (name.isNotEmpty()) {
                val intent = Intent(this@SendCVActivity, MainActivity::class.java)
                // Optional: Pass the name to the MainActivity if needed
                intent.putExtra("userName", name)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@SendCVActivity, "Please enter your name!", Toast.LENGTH_SHORT).show()
            }
        }

        nameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val nameText = nameEditText.text.toString().trim()
                if (nameText.isNotEmpty()) {
                    val intent = Intent(this@SendCVActivity, MainActivity::class.java)
                    intent.putExtra("userName", nameText)
                    startActivity(intent)
                    finish()
                    return@setOnEditorActionListener true // Correctly consume the action here
                }
                // If nameText is empty, we don't consume the action, allowing other possible handling.
            }
            false // Return false for all other cases
        }


    }

    // Registering for the file picker result
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                val fileName = uri.lastPathSegment ?: "Unknown"
                textViewFileName.text = "Selected File: $fileName"
            }
        }
    }

    private fun openFilePicker() {
        // Intent to pick a file, allowing for various file types including PDF and Word documents
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }
        filePickerLauncher.launch(intent)
    }

    fun ContentResolver.getFileName(fileUri: Uri): String {
        var name: String? = null

        // Query the uri with the content resolver to access the file name
        val cursor = this.query(fileUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                // Get the column index, then use it to get the string if index is valid
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = it.getString(nameIndex)
                }
            }
        }

        // Return the file name or a default name if the name couldn't be determined
        return name ?: "unknown_file"
    }


    private fun uploadFile() {
        if (this::selectedFileUri.isInitialized) {
            val contentResolver = applicationContext.contentResolver
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r", null)
            val fileInputStream = FileInputStream(parcelFileDescriptor?.fileDescriptor)
            val file = File(cacheDir, contentResolver.getFileName(selectedFileUri)) // Use a function to get file name
            val fileOutputStream = FileOutputStream(file)

            fileInputStream.copyTo(fileOutputStream)
            fileInputStream.close()
            fileOutputStream.close()

            val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val messagePart = "Sample Message".toRequestBody("text/plain".toMediaTypeOrNull())

            RetrofitClient.service.sendMessageWithFile(messagePart, body).enqueue(object : Callback<SendMessageResponse> {
                override fun onResponse(
                    call: Call<SendMessageResponse>,
                    response: Response<SendMessageResponse>
                ) {
                    if (response.isSuccessful) {
                        // Handle successful upload response
                        val intent = Intent(this@SendCVActivity, MainActivity::class.java)
                        // Optional: Pass the name to the MainActivity if needed
                        intent.putExtra("cvreply", response.body()!!.reply)
                        startActivity(intent)
                        Toast.makeText(this@SendCVActivity, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@SendCVActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                        // Handle failure
                    }
                }

                override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                    Toast.makeText(this@SendCVActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    // Handle error
                }
            })
        } else {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show()
        }
    }

}
