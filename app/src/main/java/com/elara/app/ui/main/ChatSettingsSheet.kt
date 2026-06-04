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
    private val onFontSizeChanged: (Float) -> Unit,
    private val onBackgroundPicked: (Uri) -> Unit
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
        val seekBar = view.findViewById<SeekBar>(R.id.fontSizeSeekBar)
        val preview = view.findViewById<TextView>(R.id.fontPreview)
        val bgButton = view.findViewById<Button>(R.id.changeBgButton)
        val bgLabel = view.findViewById<TextView>(R.id.bgWorldLabel)

        bgLabel.text = "Background for: $currentWorldName"

        seekBar.max = 10
        seekBar.progress = ((currentFontSize - 12f) * 2).toInt().coerceIn(0, 10)
        preview.textSize = currentFontSize

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 12f + progress * 0.5f
                preview.textSize = size
                onFontSizeChanged(size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        bgButton.setOnClickListener {
            pickImage.launch("image/*")
        }
    }
}
