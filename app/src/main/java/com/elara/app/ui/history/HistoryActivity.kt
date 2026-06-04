package com.elara.app.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.SessionEntity
import com.elara.app.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.historyRecycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val db = AppDatabase.get(this@HistoryActivity)
            val sessions = db.sessionDao().getAllSessions()
            val sessionsWithMessages = sessions.map { session ->
                val messages = db.sessionDao().getMessages(session.id)
                val preview = messages.lastOrNull { it.role == "assistant" }?.content?.take(100) ?: ""
                Triple(session, messages.size, preview)
            }.filter { it.second > 0 }

            binding.historyRecycler.adapter = HistoryAdapter(sessionsWithMessages)
        }
    }
}

class HistoryAdapter(
    private val items: List<Triple<SessionEntity, Int, String>>
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)

    inner class VH(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history_session, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (session, msgCount, preview) = items[position]
        holder.view.findViewById<TextView>(R.id.sessionCharacter).text =
            "${session.character} · $msgCount messages"
        holder.view.findViewById<TextView>(R.id.sessionDate).text =
            dateFormat.format(Date(session.startTime))
        holder.view.findViewById<TextView>(R.id.sessionWorld).text =
            "${session.world} · ${session.personality}"
        holder.view.findViewById<TextView>(R.id.sessionPreview).text = preview
    }
}
