package com.elara.app.ui.main

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.databinding.ItemMessageBinding
import com.elara.app.domain.model.ConversationTurn

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageVH>() {

    private val items = mutableListOf<ConversationTurn>()
    private var fontSize = 15f

    fun submitList(list: List<ConversationTurn>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun setFontSize(size: Float) {
        fontSize = size
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageVH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageVH(binding)
    }

    override fun onBindViewHolder(holder: MessageVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class MessageVH(private val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(turn: ConversationTurn) {
            b.messageText.text = turn.content
            b.messageText.textSize = fontSize

            if (turn.role == "user") {
                b.messageText.background = ContextCompat.getDrawable(b.root.context, R.drawable.bubble_user)
                b.messageText.setTextColor(0xFFEEEEFF.toInt())
                (b.messageText.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = 60
                    marginEnd = 0
                }
                (b.root as LinearLayout).gravity = Gravity.END
            } else {
                b.messageText.background = ContextCompat.getDrawable(b.root.context, R.drawable.bubble_elara)
                b.messageText.setTextColor(0xFFDDDDFF.toInt())
                (b.messageText.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = 0
                    marginEnd = 60
                }
                (b.root as LinearLayout).gravity = Gravity.START
            }
        }
    }
}
