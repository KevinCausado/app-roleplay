package com.elara.app.ui.stats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.elara.app.data.local.AppDatabase
import com.elara.app.databinding.ActivityStatsBinding
import com.elara.app.ui.main.StreakManager
import kotlinx.coroutines.launch

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            val db = AppDatabase.get(this@StatsActivity)
            val wordCount = db.wordDao().count()
            val errorCount = db.errorDao().count()
            val plan = db.studyPlanDao().get()
            val completed = plan?.completedScenarios?.split(",")?.filter { it.isNotBlank() }?.size ?: 0
            val streak = StreakManager.getStreak(this@StatsActivity)

            binding.statStreak.text = if (streak > 0) "🔥 $streak" else "🔥 0"
            binding.statWords.text = "$wordCount"
            binding.statErrors.text = "$errorCount"
            binding.statScenarios.text = "$completed"

            val level = when {
                completed >= 20 -> "B2"
                completed >= 14 -> "B1"
                completed >= 8 -> "A2"
                completed >= 4 -> "A1"
                else -> "A1"
            }
            binding.statLevel.text = level
        }
    }
}
