package com.example.data

import kotlinx.coroutines.flow.Flow

class TabRepository(private val tabDao: TabDao) {
    val allTabs: Flow<List<CalculatorTab>> = tabDao.getAllTabs()

    suspend fun insertTab(tab: CalculatorTab) {
        tabDao.insertTab(tab)
    }

    suspend fun insertTabs(tabs: List<CalculatorTab>) {
        tabDao.insertTabs(tabs)
    }

    suspend fun deleteTab(id: Int) {
        tabDao.deleteTabById(id)
    }

    suspend fun clearAll() {
        tabDao.clearAll()
    }
}
