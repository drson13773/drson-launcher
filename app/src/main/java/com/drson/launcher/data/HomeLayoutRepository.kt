package com.drson.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.drson.launcher.model.HomeSlotContent
import kotlinx.coroutines.flow.first

private val Context.layoutDataStore by preferencesDataStore(name = "drson_home_layout")

const val HOME_GRID_COLUMNS = 8
const val HOME_GRID_ROWS = 4
const val HOME_SLOT_COUNT = HOME_GRID_COLUMNS * HOME_GRID_ROWS // luoi day toan man hinh nen (8 cot x 4 hang)
const val DOCK_SLOT_COUNT = 7 // tang tu 5 len 7 de co san cho khi them app

private const val EMPTY_MARKER = "__empty__"

class HomeLayoutRepository(private val context: Context) {

    private fun homeKey(index: Int) = stringPreferencesKey("home_slot_$index")
    private fun dockKey(index: Int) = stringPreferencesKey("dock_slot_$index")

    /** Mỗi ô Home Screen có thể là app hoặc widget - xem [HomeSlotContent]. */
    suspend fun loadHomeSlots(): List<HomeSlotContent?> {
        val prefs = context.layoutDataStore.data.first()
        return (0 until HOME_SLOT_COUNT).map { i ->
            prefs[homeKey(i)]?.takeIf { it != EMPTY_MARKER }?.let { HomeSlotContent.decode(it) }
        }
    }

    /** Dock chỉ chứa icon ứng dụng (không hỗ trợ widget). */
    suspend fun loadDockSlots(): List<String?> {
        val prefs = context.layoutDataStore.data.first()
        return (0 until DOCK_SLOT_COUNT).map { i ->
            prefs[dockKey(i)]?.takeIf { it != EMPTY_MARKER }
        }
    }

    suspend fun setHomeSlot(index: Int, content: HomeSlotContent?) {
        context.layoutDataStore.edit { prefs -> prefs[homeKey(index)] = content?.encode() ?: EMPTY_MARKER }
    }

    suspend fun setDockSlot(index: Int, packageName: String?) {
        context.layoutDataStore.edit { prefs -> prefs[dockKey(index)] = packageName ?: EMPTY_MARKER }
    }
}
