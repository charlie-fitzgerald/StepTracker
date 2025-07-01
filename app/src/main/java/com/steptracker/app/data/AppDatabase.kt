package com.steptracker.app.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.steptracker.app.data.dao.SavedRouteDao
import com.steptracker.app.data.dao.StepDataDao
import com.steptracker.app.data.dao.WalkSessionDao
import com.steptracker.app.data.model.SavedRoute
import com.steptracker.app.data.model.StepData
import com.steptracker.app.data.model.WalkSession
import java.time.LocalDate
import java.time.LocalDateTime

@Database(
    entities = [StepData::class, WalkSession::class, SavedRoute::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun stepDataDao(): StepDataDao
    abstract fun walkSessionDao(): WalkSessionDao
    abstract fun savedRouteDao(): SavedRouteDao
    
    companion object {
        const val DATABASE_NAME = "steptracker_db"
    }
}

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }
} 