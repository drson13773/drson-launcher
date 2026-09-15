package com.drson.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.model.AppItem

class DockGridAdapter(
    private val appList: List<AppItem?>,
    private val onItemClick: (AppItem) -> Unit,
    private val onItemLongClick: (Int) -> Unit
) : RecyclerView.Adapter<DockGridAdapter.DockViewHolder>() {

    class DockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAppIcon: ImageView = itemView.findViewById(R.id.imgAppIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dock_app, parent, false)
        return DockViewHolder(view)
    }

    override fun onBindViewHolder(holder: DockViewHolder, position: Int) {
        val item = appList.getOrNull(position)
        if (item != null) {
            holder.imgAppIcon.setImageBitmap(item.icon.asAndroidBitmap())
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.itemView.setOnLongClickListener {
                onItemLongClick(position)
                true
            }
        } else {
            holder.imgAppIcon.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int = appList.size
}
