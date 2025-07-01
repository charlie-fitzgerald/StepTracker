package com.steptracker.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Entity(tableName = "walk_sessions")
@Parcelize
data class WalkSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val duration: Long, // in seconds
    val distance: Double, // in meters
    val steps: Int,
    val averagePace: Double, // minutes per kilometer
    val maxElevation: Double, // in meters
    val elevationGain: Double, // in meters
    val routePolyline: String? = null, // encoded polyline
    val walkMode: WalkMode,
    val isSaved: Boolean = false,
    val routeName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

enum class WalkMode {
    AUTO_ROUTE,
    DRAW_ROUTE,
    JUST_WALK
} 