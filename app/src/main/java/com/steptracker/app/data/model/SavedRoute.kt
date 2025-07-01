package com.steptracker.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "saved_routes")
@Parcelize
data class SavedRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val distance: Double, // in meters
    val estimatedTime: Long, // in minutes
    val routePolyline: String, // encoded polyline
    val startLocation: String, // address or description
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable 