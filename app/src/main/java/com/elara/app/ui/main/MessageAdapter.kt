package com.elara.app.ui.main

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.LinearLayout
import com.elara.app.databinding.ItemMessageBinding as MsgBinding
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.WordEntity
import com.elara.app.data.remote.TranslatorClient
import com.elara.app.databinding.ItemMessageBinding
import com.elara.app.domain.model.ConversationTurn
import kotlinx.coroutines.launch

class MessageAdapter(
    private val scope: LifecycleCoroutineScope,
    private val onShadow: ((String) -> Unit)? = null
) : RecyclerView.Adapter<MessageAdapter.MessageVH>() {

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
            b.messageText.textSize = fontSize

            if (turn.role == "user") {
                b.messageText.text = turn.content
                b.messageText.background = ContextCompat.getDrawable(b.root.context, R.drawable.bubble_user)
                b.messageText.setTextColor(0xFFEEEEFF.toInt())
                b.messageText.movementMethod = null
                (b.messageText.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = 60; marginEnd = 0
                }
                (b.root as LinearLayout).gravity = Gravity.END
            } else {
                b.messageText.background = ContextCompat.getDrawable(b.root.context, R.drawable.bubble_elara)
                b.messageText.setTextColor(0xFFDDDDFF.toInt())
                (b.messageText.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = 0; marginEnd = 60
                }
                (b.root as LinearLayout).gravity = Gravity.START
                makeWordsTappable(b.messageText, turn.content)
                b.repeatButton.visibility = View.VISIBLE
                b.repeatButton.setOnClickListener { onShadow?.invoke(turn.content) }
            }
        }

        private fun makeWordsTappable(tv: TextView, text: String) {
            val spannable = SpannableString(text)
            val wordRegex = Regex("[a-zA-Z']+")
            wordRegex.findAll(text).forEach { match ->
                val word = match.value
                val span = object : ClickableSpan() {
                    override fun onClick(view: View) {
                        showTranslatePopup(view, word, text)
                    }
                    override fun updateDrawState(ds: android.text.TextPaint) {
                        ds.color = 0xFFDDDDFF.toInt()
                        ds.isUnderlineText = false
                    }
                }
                spannable.setSpan(span, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            tv.text = spannable
            tv.movementMethod = LinkMovementMethod.getInstance()
            tv.highlightColor = 0x337B5EA7
        }

        private fun showTranslatePopup(anchor: View, word: String, context: String) {
            val ctx = anchor.context
            val popupView = LayoutInflater.from(ctx).inflate(R.layout.popup_translate, null)
            val popup = PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true)
            popup.elevation = 16f

            val wordTv = popupView.findViewById<TextView>(R.id.popupWord)
            val translationTv = popupView.findViewById<TextView>(R.id.popupTranslation)
            val saveBtn = popupView.findViewById<Button>(R.id.popupSaveButton)

            wordTv.text = word
            translationTv.text = "..."

            scope.launch {
                val translation = TranslatorClient.translate(word)
                translationTv.text = translation

                saveBtn.setOnClickListener {
                    scope.launch {
                        val db = AppDatabase.get(ctx)
                        val existing = db.wordDao().getByWord(word.lowercase())
                        if (existing == null) {
                            db.wordDao().insert(WordEntity(
                                word = word.lowercase(),
                                translation = translation,
                                context = context.take(120)
                            ))
                            saveBtn.text = "Saved!"
                            saveBtn.isEnabled = false
                        } else {
                            saveBtn.text = "Already saved"
                            saveBtn.isEnabled = false
                        }
                    }
                }
            }

            popup.showAtLocation(anchor, Gravity.CENTER, 0, -200)
        }
    }
}
