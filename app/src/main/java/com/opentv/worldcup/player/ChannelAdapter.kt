package com.opentv.worldcup.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opentv.worldcup.R

/**
 * Renders the channel list as large, D-pad-focusable rows.
 */
class ChannelAdapter(
    private val onClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val items = mutableListOf<Channel>()

    fun submit(channels: List<Channel>) {
        items.clear()
        items.addAll(channels)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
    }

    override fun getItemCount(): Int = items.size

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.channelName)
        private val group: TextView = itemView.findViewById(R.id.channelGroup)

        fun bind(channel: Channel) {
            name.text = channel.name
            val cat = channel.group
            if (cat.isNullOrBlank()) {
                group.visibility = View.GONE
            } else {
                group.visibility = View.VISIBLE
                group.text = cat
            }
        }
    }
}
