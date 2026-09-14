package com.drson.launcher.telephony

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract

object ContactsRepository {
    fun loadContacts(context: Context): List<ContactEntry> {
        val list = mutableListOf<ContactEntry>()
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )
        cursor?.use {
            val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val seen = mutableSetOf<String>()
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val number = it.getString(numberIdx) ?: continue
                val key = "$name|$number"
                if (seen.add(key)) {
                    list.add(ContactEntry(id = it.getLong(idIdx), name = name, number = number))
                }
            }
        }
        return list
    }
}

object CallLogRepository {
    fun loadRecent(context: Context, limit: Int = 50): List<CallLogEntry> {
        val list = mutableListOf<CallLogEntry>()
        val resolver = context.contentResolver
        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
            ),
            null, null,
            "${CallLog.Calls.DATE} DESC",
        )
        cursor?.use {
            val idIdx = it.getColumnIndex(CallLog.Calls._ID)
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            var count = 0
            while (it.moveToNext() && count < limit) {
                val number = it.getString(numberIdx) ?: ""
                val name = it.getString(nameIdx)?.takeIf { n -> n.isNotBlank() } ?: number
                list.add(
                    CallLogEntry(
                        id = it.getLong(idIdx),
                        name = name,
                        number = number,
                        type = it.getInt(typeIdx),
                        timestampMs = it.getLong(dateIdx),
                    )
                )
                count++
            }
        }
        return list
    }
}
