package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculator_tabs")
data class CalculatorTab(
    @PrimaryKey val id: Int,
    val expression: String = "0",
    val history: String = "",
    val isResultState: Boolean = false
)
