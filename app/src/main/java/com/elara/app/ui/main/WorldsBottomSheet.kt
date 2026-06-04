package com.elara.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elara.app.R
import com.elara.app.domain.model.World
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WorldsBottomSheet(
    private val currentWorld: World,
    private val onWorldSelected: (World) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_worlds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.worldsTitle)
        title.text = "Choose World"
        val recycler = view.findViewById<RecyclerView>(R.id.worldsRecycler)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = WorldAdapter(currentWorld) { world ->
            onWorldSelected(world)
            dismiss()
        }
    }

    class WorldAdapter(
        private val current: World,
        private val onClick: (World) -> Unit
    ) : RecyclerView.Adapter<WorldAdapter.VH>() {

        private val worlds = World.entries

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_world, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val world = worlds[position]
            holder.emoji.text = world.emoji
            holder.name.text = world.displayName
            holder.desc.text = world.description
            holder.selected.visibility = if (world == current) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onClick(world) }
        }

        override fun getItemCount() = worlds.size

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.worldEmoji)
            val name: TextView = v.findViewById(R.id.worldName)
            val desc: TextView = v.findViewById(R.id.worldDesc)
            val selected: TextView = v.findViewById(R.id.worldSelected)
        }
    }
}
