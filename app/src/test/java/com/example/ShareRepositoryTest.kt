package com.example

import com.example.data.ShareRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ShareRepositoryTest {

    @Test
    fun computeStatsForRange_filtersAndAggregatesCorrectly() {
        val day1 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val day2 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 11, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val day3 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sessions = listOf(
            Session(startTime = day1, duration = 20 * 60_000L),
            Session(startTime = day2, duration = 40 * 60_000L),
            Session(startTime = day3, duration = 60 * 60_000L)
        )

        val rangeStats = ShareRepository.computeStatsForRange(sessions, day1, day2)
        assertEquals(60 * 60_000L, rangeStats.totalFocusMs)
        assertEquals(2, rangeStats.sessionCount)
        assertEquals(30 * 60_000L, rangeStats.averageMs)
        assertEquals(40 * 60_000L, rangeStats.longestMs)

        val liveStats = ShareRepository.computeStatsForRange(sessions, day2, null)
        assertEquals(100 * 60_000L, liveStats.totalFocusMs)
        assertEquals(2, liveStats.sessionCount)
        assertEquals(50 * 60_000L, liveStats.averageMs)
        assertEquals(60 * 60_000L, liveStats.longestMs)
    }

    @Test
    fun utcDatePickerMillisToLocalStartOfDay_preservesCalendarDate() {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 13, 0, 0, 0)
        }
        val localMs = ShareRepository.utcDatePickerMillisToLocalStartOfDay(utcCal.timeInMillis)
        val localCal = Calendar.getInstance().apply { timeInMillis = localMs }
        assertEquals(2026, localCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, localCal.get(Calendar.MONTH))
        assertEquals(13, localCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, localCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun formatDurationReadable_formatsHoursMinutesSeconds() {
        assertEquals("0s", ShareRepository.formatDurationReadable(0L))
        assertEquals("45s", ShareRepository.formatDurationReadable(45_000L))
        assertEquals("12m 30s", ShareRepository.formatDurationReadable(12 * 60_000L + 30_000L))
        assertEquals("2h 5m", ShareRepository.formatDurationReadable(2 * 3600_000L + 5 * 60_000L))
    }

    @Test
    fun computeDailyStatsForRange_groupsByDayAccurately() {
        val nov10a = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nov10b = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nov11 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 11, 11, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sessions = listOf(
            Session(startTime = nov10a, duration = 20 * 60_000L),
            Session(startTime = nov10b, duration = 40 * 60_000L),
            Session(startTime = nov11, duration = 30 * 60_000L)
        )

        val daily = ShareRepository.computeDailyStatsForRange(sessions, nov10a, nov11)
        assertEquals(2, daily.size)

        val day10 = daily["2026-11-10"]!!
        assertEquals(60 * 60_000L, day10.totalFocusMs)
        assertEquals(2, day10.sessionCount)
        assertEquals(30 * 60_000L, day10.averageMs)
        assertEquals(40 * 60_000L, day10.longestMs)

        val day11 = daily["2026-11-11"]!!
        assertEquals(30 * 60_000L, day11.totalFocusMs)
        assertEquals(1, day11.sessionCount)
        assertEquals(30 * 60_000L, day11.averageMs)
        assertEquals(30 * 60_000L, day11.longestMs)
    }
}
