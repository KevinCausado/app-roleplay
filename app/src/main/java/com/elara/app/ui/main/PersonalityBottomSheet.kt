package com.elara.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.domain.model.Personality
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PersonalityBottomSheet(
    private val current: Personality,
    private val onSelected: (Personality) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_worlds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(android.R.id.title) ?: view.findViewById<TextView>(R.id.worldsTitle)
        // Set title manually since the layout uses "bottom_sheet_worlds" title
        val textView = view.findViewById<TextView>(R.id.worldsTitle)
        textView.text = "Choose Personality"

        val recycler = view.findViewById<RecyclerView>(R.id.worldsRecycler)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = PersonalityAdapter(current) { personality ->
            onSelected(personality)
            dismiss()
        }
    }

    class PersonalityAdapter(
        private val current: Personality,
        private val onClick: (Personality) -> Unit
    ) : RecyclerView.Adapter<PersonalityAdapter.VH>() {

        private val items = Personality.entries

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_world, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = items[position]
            holder.emoji.text = p.emoji
            holder.name.text = p.displayName
            holder.desc.text = p.description
            holder.selected.visibility = if (p == current) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onClick(p) }
        }

        override fun getItemCount() = items.size

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.worldEmoji)
            val name: TextView = v.findViewById(R.id.worldName)
            val desc: TextView = v.findViewById(R.id.worldDesc)
            val selected: TextView = v.findViewById(R.id.worldSelected)
        }
    }
}
