package com.elara.app.ui.errors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.ErrorEntity
import com.elara.app.databinding.ActivityReviewErrorsBinding
import com.elara.app.domain.usecase.GenerateExerciseUseCase
import kotlinx.coroutines.launch

class ReviewErrorsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewErrorsBinding
    private var isExerciseMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewErrorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        binding.exerciseButton.setOnClickListener {
            if (isExerciseMode) {
                binding.exerciseScroll.visibility = View.GONE
                binding.errorsRecycler.visibility = View.VISIBLE
                binding.exerciseButton.text = "Exercises"
                isExerciseMode = false
            } else {
                generateExercises()
            }
        }

        loadErrors()
    }

    private fun loadErrors() {
        lifecycleScope.launch {
            val db = AppDatabase.get(this@ReviewErrorsActivity)
            val errors = db.errorDao().getAll()
            val total = errors.size
            val reviewed = errors.count { it.reviewCount > 0 }
            binding.errorCount.text = "$total mistakes saved · $reviewed reviewed"

            binding.errorsRecycler.layoutManager = LinearLayoutManager(this@ReviewErrorsActivity)
            binding.errorsRecycler.adapter = ErrorAdapter(errors) { error ->
                lifecycleScope.launch {
                    db.errorDao().markReviewed(error.id)
                    Toast.makeText(this@ReviewErrorsActivity, "Reviewed!", Toast.LENGTH_SHORT).show()
                    loadErrors()
                }
            }
        }
    }

    private fun generateExercises() {
        binding.exerciseScroll.visibility = View.VISIBLE
        binding.errorsRecycler.visibility = View.GONE
        binding.exerciseButton.text = "Back"
        isExerciseMode = true
        binding.exerciseContent.text = "Generating exercises..."

        lifecycleScope.launch {
            val db = AppDatabase.get(this@ReviewErrorsActivity)
            val errors = db.errorDao().getAll()
            val result = GenerateExerciseUseCase().generate(errors)
            binding.exerciseContent.text = result
        }
    }
}

class ErrorAdapter(
    private val items: List<ErrorEntity>,
    private val onTap: (ErrorEntity) -> Unit
) : RecyclerView.Adapter<ErrorAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val original: TextView = view.findViewById(R.id.errorOriginal)
        val corrected: TextView = view.findViewById(R.id.errorCorrected)
        val reviewed: TextView = view.findViewById(R.id.errorReviewed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_error, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.original.text = "❌ ${e.original}"
        holder.corrected.text = "✅ ${e.corrected}"
        holder.reviewed.text = if (e.reviewCount > 0) "✓${e.reviewCount}" else ""
        holder.itemView.setOnClickListener { onTap(e) }
    }
}
