package com.elara.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.data.local.AppDatabase
import com.elara.app.domain.model.Character
import com.elara.app.ui.character.CreateCharacterActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CharacterBottomSheet(
    private val currentCharacter: Character,
    private val onCharacterSelected: (Character) -> Unit,
    private val onCustomSelected: ((String) -> Unit)? = null
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_worlds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.worldsTitle)
        title.text = "Choose Character"

        val recycler = view.findViewById<RecyclerView>(R.id.worldsRecycler)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = CharacterAdapter(currentCharacter) { character ->
            onCharacterSelected(character)
            dismiss()
        }

        lifecycleScope.launch {
            val customs = AppDatabase.get(requireContext()).customCharacterDao().getAll()
            if (customs.isNotEmpty()) {
                title.text = "Choose Character (${Character.entries.size + customs.size})"
            }
        }

        val createBtn = Button(requireContext()).apply {
            text = "+ Create custom character"
            setTextColor(0xFF9B9BBB.toInt())
            setBackgroundColor(0x00000000)
            setOnClickListener {
                startActivity(Intent(requireContext(), CreateCharacterActivity::class.java))
                dismiss()
            }
        }
        (view as ViewGroup).addView(createBtn)
    }
}

class CharacterAdapter(
    private val current: Character,
    private val onClick: (Character) -> Unit
) : RecyclerView.Adapter<CharacterAdapter.VH>() {

    inner class VH(val root: View) : RecyclerView.ViewHolder(root) {
        val name: TextView = root.findViewById(R.id.worldName)
        val desc: TextView = root.findViewById(R.id.worldDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_world, parent, false)
        return VH(v)
    }

    override fun getItemCount() = Character.entries.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val char = Character.entries[position]
        holder.name.text = "${char.emoji} ${char.displayName}"
        holder.desc.text = char.description
        holder.root.alpha = if (char == current) 1f else 0.6f
        holder.root.setOnClickListener { onClick(char) }
    }
}
