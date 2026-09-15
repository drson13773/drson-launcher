package com.drson.launcher.ui.adapter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.R
import com.drson.launcher.model.AppItem

class DockGridAdapter(
    private var appList: List<AppItem?>,
    private var isEditMode: Boolean = false,
    private val onItemClick: (Int, AppItem?) -> Unit,
    private val onItemLongClick: (Int, AppItem?) -> Unit
) : RecyclerView.Adapter<DockGridAdapter.DockViewHolder>() {

    class DockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgDockIcon)
        val tvPlus: TextView? = itemView.findViewById(R.id.tvDockPlus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dock_app, parent, false)
        return DockViewHolder(view)
    }

    override fun onBindViewHolder(holder: DockViewHolder, position: Int) {
        val app = appList.getOrNull(position)

        if (app != null) {
            holder.tvPlus?.visibility = View.GONE
            holder.imgIcon.visibility = View.VISIBLE

            when (val icon = app.icon) {
                is Drawable -> holder.imgIcon.setImageDrawable(icon)
                is ImageBitmap -> holder.imgIcon.setImageBitmap(icon.asAndroidBitmap())
                else -> holder.imgIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } else {
            if (isEditMode) {
                holder.imgIcon.visibility = View.GONE
                holder.tvPlus?.visibility = View.VISIBLE
                holder.tvPlus?.text = "+"
                holder.tvPlus?.setTextColor(Color.parseColor("#D4AF37"))
            } else {
                holder.imgIcon.visibility = View.INVISIBLE
                holder.tvPlus?.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(position, app)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(position, app)
            true
        }
    }

    override fun getItemCount(): Int = 4

    fun updateData(newApps: List<AppItem?>, editMode: Boolean = isEditMode) {
        this.appList = newApps
        this.isEditMode = editMode
        notifyDataSetChanged()
    }

    fun setEditMode(editMode: Boolean) {
        this.isEditMode = editMode
        notifyDataSetChanged()
    }
}
