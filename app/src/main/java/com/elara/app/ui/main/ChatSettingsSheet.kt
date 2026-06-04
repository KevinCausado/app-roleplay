package com.elara.app.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.elara.app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChatSettingsSheet(
    private val currentFontSize: Float,
    private val currentWorldName: String,
    private val currentPitch: Float,
    private val currentRate: Float,
    private val onFontSizeChanged: (Float) -> Unit,
    private val onBackgroundPicked: (Uri) -> Unit,
    private val onVoiceChanged: (pitch: Float, rate: Float) -> Unit
) : BottomSheetDialogFragment() {

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onBackgroundPicked(uri)
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_chat_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Font
        val seekFont = view.findViewById<SeekBar>(R.id.fontSizeSeekBar)
        val preview = view.findViewById<TextView>(R.id.fontPreview)
        seekFont.max = 10
        seekFont.progress = ((currentFontSize - 12f) * 2).toInt().coerceIn(0, 10)
        preview.textSize = currentFontSize
        seekFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 12f + progress * 0.5f
                preview.textSize = size
                onFontSizeChanged(size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Background
        val bgLabel = view.findViewById<TextView>(R.id.bgWorldLabel)
        bgLabel.text = "Background for: $currentWorldName"
        view.findViewById<Button>(R.id.changeBgButton).setOnClickListener {
            pickImage.launch("image/*")
        }

        // Voice gender presets
        view.findViewById<Button>(R.id.voiceFemaleButton).setOnClickListener {
            onVoiceChanged(1.3f, 0.95f)
        }
        view.findViewById<Button>(R.id.voiceMaleButton).setOnClickListener {
            onVoiceChanged(0.7f, 0.9f)
        }

        // Pitch slider
        val seekPitch = view.findViewById<SeekBar>(R.id.pitchSeekBar)
        seekPitch.max = 20
        seekPitch.progress = ((currentPitch - 0.5f) * 10).toInt().coerceIn(0, 20)
        seekPitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onVoiceChanged(0.5f + progress * 0.1f, currentRate)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Speed slider
        val seekRate = view.findViewById<SeekBar>(R.id.rateSeekBar)
        seekRate.max = 14
        seekRate.progress = ((currentRate - 0.5f) * 10).toInt().coerceIn(0, 14)
        seekRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onVoiceChanged(currentPitch, 0.5f + progress * 0.1f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
