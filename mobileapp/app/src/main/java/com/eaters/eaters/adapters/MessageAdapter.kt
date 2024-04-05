package com.eaters.eaters.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eaters.eaters.R
import com.eaters.eaters.models.Message

class MessageAdapter(private val messages: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.role == "user") VIEW_TYPE_MESSAGE_SENT else VIEW_TYPE_MESSAGE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> R.layout.item_message_sent // Layout for sent messages (on the right)
            VIEW_TYPE_MESSAGE_RECEIVED -> R.layout.item_message_received // Layout for received messages (on the left)
            else -> throw IllegalArgumentException("Invalid view type")
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount() = messages.size

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageBody: TextView = view.findViewById(R.id.text_message_body)

        fun bind(message: Message) {
            messageBody.text = message.content
        }
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}
