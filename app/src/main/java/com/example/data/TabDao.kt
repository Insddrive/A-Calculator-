package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM calculator_tabs ORDER BY id ASC")
    fun getAllTabs(): Flow<List<CalculatorTab>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: CalculatorTab)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<CalculatorTab>)

    @Query("DELETE FROM calculator_tabs WHERE id = :id")
    suspend fun deleteTabById(id: Int)

    @Query("DELETE FROM calculator_tabs")
    suspend fun clearAll()
}
