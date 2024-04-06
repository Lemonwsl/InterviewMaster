package com.eaters.eaters

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eaters.eaters.models.FeedbackRequest
import com.eaters.eaters.models.FeedbackResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class FeedbackActivity : AppCompatActivity() {
    private lateinit var nameStr: String
    private lateinit var feedbackTv: TextView
    private lateinit var detailedFeedbackTv: TextView
    private lateinit var backToStartBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        // Additional setup if needed
        nameStr = intent.getStringExtra("userName")?.toTitleCase().toString()
        feedbackTv = findViewById(R.id.tvFeedback)
        detailedFeedbackTv = findViewById(R.id.tvFeedbackDetail)
        backToStartBtn = findViewById(R.id.btnBackToStart)

        val request = FeedbackRequest(nameStr)

        RetrofitClient.service.getFeedback(request).enqueue(object : Callback<FeedbackResponse> {
            override fun onResponse(call: Call<FeedbackResponse>, response: Response<FeedbackResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val receivedMessage = FeedbackResponse(responseBody.message)
                        runOnUiThread {
                            feedbackTv.setText(responseBody.message)
                        }
                    }
                } else {
                    // Handle request error
                    Toast.makeText(applicationContext, "Something error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Check your internet connection", Toast.LENGTH_SHORT).show()
            }
        })

        RetrofitClient.service.getDetailedFeedback(request).enqueue(object : Callback<FeedbackResponse> {
            override fun onResponse(call: Call<FeedbackResponse>, response: Response<FeedbackResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val receivedMessage = FeedbackResponse(responseBody.message)
                        runOnUiThread {
                            detailedFeedbackTv.setText(responseBody.message)
                        }
                    }
                } else {
                    // Handle request error
                    Toast.makeText(applicationContext, "Something error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Check your internet connection", Toast.LENGTH_SHORT).show()
            }
        })

        backToStartBtn.setOnClickListener {
            val intent = Intent(this@FeedbackActivity, SendCVActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
    fun String.toTitleCase(): String = this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
