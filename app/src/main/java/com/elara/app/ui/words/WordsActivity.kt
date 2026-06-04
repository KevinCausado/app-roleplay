package com.elara.app.ui.words

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
import com.elara.app.data.local.entity.WordEntity
import com.elara.app.databinding.ActivityWordsBinding
import kotlinx.coroutines.launch

class WordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.wordsRecycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val db = AppDatabase.get(this@WordsActivity)
            val words = db.wordDao().getAll()
            val due = db.wordDao().getDueForReview()

            binding.wordCount.text = "${words.size} words"
            binding.statTotal.text = words.size.toString()
            binding.statDue.text = due.size.toString()
            binding.statLevel.text = levelFromCount(words.size)

            if (due.isNotEmpty()) {
                binding.reviewButton.visibility = View.VISIBLE
                binding.reviewButton.setOnClickListener {
                    startActivity(Intent(this@WordsActivity, ReviewActivity::class.java))
                }
            }

            binding.wordsRecycler.adapter = WordsAdapter(words)
        }
    }

    override fun onResume() {
        super.onResume()
        recreate()
    }

    private fun levelFromCount(count: Int) = when {
        count < 100 -> "A1"
        count < 300 -> "A2"
        count < 600 -> "B1"
        count < 1000 -> "B2"
        count < 2000 -> "C1"
        else -> "C2"
    }
}

class WordsAdapter(private val words: List<WordEntity>) : RecyclerView.Adapter<WordsAdapter.VH>() {

    inner class VH(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
        return VH(v)
    }

    override fun getItemCount() = words.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val w = words[position]
        holder.view.findViewById<TextView>(R.id.wordText).text = w.word
        holder.view.findViewById<TextView>(R.id.translationText).text = w.translation
        holder.view.findViewById<TextView>(R.id.contextText).text = w.context
    }
}
