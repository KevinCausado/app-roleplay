package com.elara.app.ui.words

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.WordEntity
import com.elara.app.databinding.ActivityReviewBinding
import kotlinx.coroutines.launch

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private var queue = mutableListOf<WordEntity>()
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            queue = AppDatabase.get(this@ReviewActivity).wordDao().getDueForReview().toMutableList()
            if (queue.isEmpty()) { finish(); return@launch }
            showCard()
        }

        binding.showButton.setOnClickListener {
            binding.reviewTranslation.visibility = View.VISIBLE
            binding.answerButtons.visibility = View.VISIBLE
            binding.showButton.visibility = View.GONE
        }

        binding.hardButton.setOnClickListener { grade(1) }
        binding.goodButton.setOnClickListener { grade(3) }
        binding.easyButton.setOnClickListener { grade(5) }
    }

    private fun showCard() {
        if (index >= queue.size) { finish(); return }
        val w = queue[index]
        binding.reviewProgress.text = "${index + 1} / ${queue.size}"
        binding.reviewWord.text = w.word
        binding.reviewContext.text = w.context
        binding.reviewTranslation.text = w.translation
        binding.reviewTranslation.visibility = View.INVISIBLE
        binding.answerButtons.visibility = View.GONE
        binding.showButton.visibility = View.VISIBLE
    }

    private fun grade(quality: Int) {
        val w = queue[index]
        lifecycleScope.launch {
            val newInterval = when {
                quality < 2 -> 1
                quality < 4 -> (w.reviewInterval * 1.5).toInt().coerceAtLeast(2)
                else -> (w.reviewInterval * 2.5).toInt().coerceAtLeast(4)
            }
            val nextReview = System.currentTimeMillis() + newInterval * 86400000L
            AppDatabase.get(this@ReviewActivity).wordDao().update(
                w.copy(reviewInterval = newInterval, nextReview = nextReview, reviewCount = w.reviewCount + 1)
            )
        }
        index++
        showCard()
    }
}
