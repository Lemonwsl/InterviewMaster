package com.eaters.eaters.models

data class SendMessageRequest(
    val message: String, // Assuming the endpoint expects a JSON with a message field.
    val name: String
)