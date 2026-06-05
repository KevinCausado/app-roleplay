package com.elara.app.ui.learning

import android.content.Intent
import android.graphics.Typeface
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
import com.elara.app.data.local.entity.StudyPlanEntity
import com.elara.app.domain.model.CEFRLevel
import com.elara.app.domain.model.Scenario
import com.elara.app.databinding.ActivityLearningPathBinding
import kotlinx.coroutines.launch

class LearningPathActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearningPathBinding
    private val completedScenarios = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearningPathBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            val db = AppDatabase.get(this@LearningPathActivity)
            val plan = db.studyPlanDao().get() ?: StudyPlanEntity()
            completedScenarios.addAll(plan.completedScenarios.split(",").filter { it.isNotBlank() })

            val items = buildItemList()
            binding.progressSummary.text = "${completedScenarios.size}/${Scenario.entries.size} done"

            binding.pathRecycler.layoutManager = LinearLayoutManager(this@LearningPathActivity)
            binding.pathRecycler.adapter = PathAdapter(items) { scenario ->
                val result = Intent().apply {
                    putExtra("scenario_id", scenario.id)
                }
                setResult(RESULT_OK, result)
                finish()
            }
        }
    }

    private fun buildItemList(): List<PathItem> {
        val items = mutableListOf<PathItem>()
        val levels = CEFRLevel.entries
        var allPreviousDone = true

        for (level in levels) {
            val scenarios = Scenario.byLevel(level)
            val done = scenarios.count { completedScenarios.contains(it.id) }
            items.add(PathItem.Header(level, done, scenarios.size))

            for (scenario in scenarios) {
                val isDone = completedScenarios.contains(scenario.id)
                val isUnlocked = allPreviousDone || isDone
                items.add(PathItem.ScenarioItem(scenario, isDone, isUnlocked))
            }

            if (done < scenarios.size) allPreviousDone = false
        }
        return items
    }

    sealed class PathItem {
        data class Header(val level: CEFRLevel, val done: Int, val total: Int) : PathItem()
        data class ScenarioItem(val scenario: Scenario, val done: Boolean, val unlocked: Boolean) : PathItem()
    }
}

class PathAdapter(
    private val items: List<LearningPathActivity.PathItem>,
    private val onScenarioClick: (Scenario) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SCENARIO = 1
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is LearningPathActivity.PathItem.Header -> TYPE_HEADER
        is LearningPathActivity.PathItem.ScenarioItem -> TYPE_SCENARIO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TYPE_HEADER -> {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cefr_level, parent, false)
            HeaderVH(v)
        }
        else -> {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_scenario_card, parent, false)
            ScenarioVH(v)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LearningPathActivity.PathItem.Header -> {
                val h = holder as HeaderVH
                h.levelLabel.text = item.level.label
                h.levelLabel.setTextColor(item.level.color)
                h.levelProgress.text = "${item.done}/${item.total} completed"
            }
            is LearningPathActivity.PathItem.ScenarioItem -> {
                val h = holder as ScenarioVH
                val s = item.scenario
                h.emoji.text = s.emoji
                h.title.text = s.title
                h.category.text = s.category
                h.description.text = s.description

                if (item.done) {
                    h.status.text = "✓"
                    h.status.setTextColor(0xFF4ADE80.toInt())
                } else if (item.unlocked) {
                    h.status.text = "○"
                    h.status.setTextColor(0xFF8B88A8.toInt())
                } else {
                    h.status.text = "🔒"
                }

                h.itemView.alpha = if (item.unlocked) 1.0f else 0.4f
                h.itemView.isClickable = item.unlocked
                h.itemView.setOnClickListener {
                    if (item.unlocked) onScenarioClick(s)
                }
            }
        }
    }

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val levelLabel: TextView = view.findViewById(R.id.levelLabel)
        val levelProgress: TextView = view.findViewById(R.id.levelProgress)
    }

    inner class ScenarioVH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.scenarioEmoji)
        val title: TextView = view.findViewById(R.id.scenarioTitle)
        val category: TextView = view.findViewById(R.id.scenarioCategory)
        val description: TextView = view.findViewById(R.id.scenarioDescription)
        val status: TextView = view.findViewById(R.id.scenarioStatus)
    }
}
