package com.elara.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.elara.app.R
import com.elara.app.databinding.ActivityOnboardingBinding
import com.elara.app.databinding.ItemOnboardingPageBinding
import com.elara.app.ui.main.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pages = listOf(
            Triple("🗣️", "Welcome to Elara", "Your AI companion to learn English through real conversations. No textbooks, no drills — just talk."),
            Triple("📚", "Learn Naturally", "Chat with characters, practice scenarios, take photos and describe them. Every conversation teaches you something new."),
            Triple("🚀", "Track Your Progress", "Words saved, errors corrected, scenarios completed. Watch yourself improve day by day. Ready?")
        )

        binding.onboardingPager.adapter = OnboardingAdapter(pages)
        binding.onboardingNext.setOnClickListener {
            val current = binding.onboardingPager.currentItem
            if (current < pages.size - 1) {
                binding.onboardingPager.currentItem = current + 1
            } else {
                getSharedPreferences("elara_prefs", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.onboardingNext.text = if (position == pages.size - 1) "Start" else "Next"
            }
        })
    }
}

class OnboardingAdapter(private val pages: List<Triple<String, String, String>>) :
    RecyclerView.Adapter<OnboardingAdapter.VH>() {

    inner class VH(val b: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (emoji, title, desc) = pages[position]
        holder.b.pageEmoji.text = emoji
        holder.b.pageTitle.text = title
        holder.b.pageDesc.text = desc
    }
}
