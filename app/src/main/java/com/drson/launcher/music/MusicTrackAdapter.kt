package com.drson.launcher.music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.R

class MusicTrackAdapter(
    private var tracks: List<PureTrack>,
    private val onTrackSelected: (PureTrack) -> Unit
) : RecyclerView.Adapter<MusicTrackAdapter.TrackViewHolder>() {

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTrackTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvTrackArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = tracks[position]
        holder.tvTitle.text = item.title
        holder.tvArtist.text = item.artist
        holder.itemView.setOnClickListener { onTrackSelected(item) }
    }

    override fun getItemCount(): Int = tracks.size

    fun updateData(newTracks: List<PureTrack>) {
        tracks = newTracks
        notifyDataSetChanged()
    }
}
