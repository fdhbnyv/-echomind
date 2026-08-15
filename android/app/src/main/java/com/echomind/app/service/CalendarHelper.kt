package com.echomind.app.service

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar

/**
 * Helper to write recognized schedule items to the system calendar.
 *
 * PRD JS-06: 语音中提到"明天下午3点跟老王开会"，自动识别并创建日历项
 *
 * Uses Android Calendar Provider (ContentResolver) — no Google Calendar API needed.
 * Requires WRITE_CALENDAR permission.
 */
object CalendarHelper {

    /**
     * Insert a calendar event from a parsed schedule entry.
     *
     * @param context Application context
     * @param title Event title
     * @param description Event description
     * @param startTimeMs Start time in epoch millis (null = use current time + 1h)
     * @param endTimeMs End time in epoch millis (null = start + 1h)
     * @return The event ID if successful, null on failure
     */
    fun insertEvent(
        context: Context,
        title: String,
        description: String = "",
        startTimeMs: Long? = null,
        endTimeMs: Long? = null,
    ): Long? {
        return try {
            val cal = Calendar.getInstance()
            val start = startTimeMs ?: (cal.timeInMillis + 3600000L)
            val end = endTimeMs ?: (start + 3600000L)

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, getPrimaryCalendarId(context) ?: return null)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, end)
                put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().timeZone.id)
                put(CalendarContract.Events.ALL_DAY, 0)
            }

            val uri = context.contentResolver.insert(
                CalendarContract.Events.CONTENT_URI,
                values,
            )
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: SecurityException) {
            // WRITE_CALENDAR permission not granted
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse an ISO 8601 date string (e.g. "2026-07-04T15:00:00") to epoch millis.
     * Returns null if parsing fails.
     */
    fun parseDeadlineToMillis(deadline: String): Long? {
        return try {
            // Handle formats like "2026-07-04T15:00:00" or "2026-07-04"
            val cleaned = deadline.trim()
            if (cleaned.contains("T")) {
                val parts = cleaned.split("T")
                val dateParts = parts[0].split("-")
                val timeParts = parts[1].split(":")
                if (dateParts.size >= 3 && timeParts.size >= 2) {
                    val cal = Calendar.getInstance()
                    cal.set(
                        dateParts[0].toInt(),
                        dateParts[1].toInt() - 1,
                        dateParts[2].toInt(),
                        timeParts[0].toInt(),
                        timeParts[1].toInt(),
                        timeParts.getOrNull(2)?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0,
                    )
                    cal.timeInMillis
                } else null
            } else {
                val dateParts = cleaned.split("-")
                if (dateParts.size >= 3) {
                    val cal = Calendar.getInstance()
                    cal.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt(), 9, 0, 0)
                    cal.timeInMillis
                } else null
            }
        } catch (_: Exception) { null }
    }

    /**
     * Get the ID of the primary local calendar.
     * Returns null if no calendar is available.
     */
    private fun getPrimaryCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND ${CalendarContract.Calendars.OWNER_ACCOUNT} = ?"
        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                null,
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getLong(0)
                }
            }
        } catch (_: SecurityException) { }

        // Fallback: try any calendar
        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null,
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getLong(0)
                }
            }
        } catch (_: Exception) { }

        return null
    }

    /**
     * Check if the calendar provider has any calendars available.
     */
    fun hasCalendar(context: Context): Boolean {
        return getPrimaryCalendarId(context) != null
    }
}
