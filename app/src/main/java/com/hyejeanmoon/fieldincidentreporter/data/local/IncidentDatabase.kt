package com.hyejeanmoon.fieldincidentreporter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [IncidentEntity::class], version = 1, exportSchema = false)
internal abstract class IncidentDatabase : RoomDatabase() {
    abstract fun incidents(): IncidentDao
}
