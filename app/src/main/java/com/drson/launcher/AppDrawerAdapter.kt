package com.drson.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.R
import com.drson.launcher.model.AppItem

class AppDrawerAdapter(
    private val appList: List<AppItem>,
    private val onItemClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.AppViewHolder>() {

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgAppDrawerIcon)
        val tvLabel: TextView = itemView.findViewById(R.id.tvAppDrawerLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_drawer, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.tvLabel.text = app.label
        holder.imgIcon.setImageBitmap(app.icon.asAndroidBitmap())

        holder.itemView.setOnClickListener {
            onItemClick(app)
        }
    }

    override fun getItemCount(): Int = appList.size
}
