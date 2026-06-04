package com.elara.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.domain.model.Character
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CharacterBottomSheet(
    private val currentCharacter: Character,
    private val onCharacterSelected: (Character) -> Unit
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
