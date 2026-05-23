package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "math_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val answer: String,
    val category: String, // "algebra", "calculus", "statistics", "graphing", "general", etc.
    val timestamp: Long = System.currentTimeMillis()
)
