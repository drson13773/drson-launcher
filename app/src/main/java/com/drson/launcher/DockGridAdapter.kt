package com.drson.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.model.AppItem

class DockGridAdapter(
    private var appList: List<AppItem?>,
    private val onItemClick: (Int, AppItem?) -> Unit,
    private val onItemLongClick: (Int, AppItem?) -> Unit
) : RecyclerView.Adapter<DockGridAdapter.DockViewHolder>() {

    class DockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAppIcon: ImageView = itemView.findViewById(R.id.imgAppIcon)
        val imgAddIcon: ImageView = itemView.findViewById(R.id.imgAddIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dock_app, parent, false)
        return DockViewHolder(view)
    }

    override fun onBindViewHolder(holder: DockViewHolder, position: Int) {
        val item = appList.getOrNull(position)

        if (item != null) {
            // Ô đã có app
            holder.imgAppIcon.visibility = View.VISIBLE
            holder.imgAddIcon.visibility = View.GONE
            holder.imgAppIcon.setImageBitmap(item.icon.asAndroidBitmap())
        } else {
            // Ô còn trống -> Hiện dấu (+) để Add app
            holder.imgAppIcon.visibility = View.GONE
            holder.imgAddIcon.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onItemClick(position, item)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(position, item)
            true
        }
    }

    override fun getItemCount(): Int = 4 // Cố định 4 slot trên Dock xe hơi

    fun updateData(newList: List<AppItem?>) {
        appList = newList
        notifyDataSetChanged()
    }
}
