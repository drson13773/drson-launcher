package com.drson.launcher.data

import android.content.Context
import android.content.SharedPreferences

class HomeLayoutRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDockSlots(): List<String?> {
        val raw = prefs.getString(KEY_DOCK_SLOTS, null) ?: return listOf(null, null, null, null)
        val items = raw.split(",")
        return (0 until 4).map { idx: Int ->
            val pkg: String = items.getOrElse(idx) { "" }.trim()
            if (pkg.isNotEmpty() && pkg != "null") pkg else null
        }
    }

    fun saveDockSlots(slots: List<String?>) {
        val serialized = slots.joinToString(",") { slot: String? -> slot ?: "null" }
        prefs.edit().putString(KEY_DOCK_SLOTS, serialized).apply()
    }

    fun setDockSlot(index: Int, packageName: String?) {
        val current = getDockSlots().toMutableList()
        if (index in 0 until 4) {
            current[index] = packageName
            saveDockSlots(current)
        }
    }

    companion object {
        private const val PREFS_NAME = "drson_launcher_layout_prefs"
        private const val KEY_DOCK_SLOTS = "dock_slots_v1"
    }
}
