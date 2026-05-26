package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalculatorTab
import com.example.data.TabRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TabRepository
    val allTabs: StateFlow<List<CalculatorTab>>

    private val _activeTabId = MutableStateFlow(1)
    val activeTabId: StateFlow<Int> = _activeTabId.asStateFlow()

    private val _activeTokenIndex = MutableStateFlow<Int?>(null)
    val activeTokenIndex: StateFlow<Int?> = _activeTokenIndex.asStateFlow()

    private var isOverwriting = false

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TabRepository(database.tabDao())
        
        // Expose tabs flow
        allTabs = repository.allTabs.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = emptyList()
        )

        // Prepopulate if empty
        viewModelScope.launch {
            val tabs = repository.allTabs.first()
            if (tabs.isEmpty()) {
                val defaultTab = CalculatorTab(id = 1, expression = "0", history = "", isResultState = false)
                repository.insertTab(defaultTab)
                _activeTabId.value = 1
            } else {
                // Restore last active id from list
                val ids = tabs.map { it.id }
                if (activeTabId.value !in ids) {
                    _activeTabId.value = ids.firstOrNull() ?: 1
                }
            }
        }
    }

    fun selectTab(id: Int) {
        _activeTabId.value = id
        _activeTokenIndex.value = null
        isOverwriting = false
    }

    fun addTab() {
        viewModelScope.launch {
            val tabs = allTabs.value
            val maxId = tabs.maxOfOrNull { it.id } ?: 0
            val nextId = maxId + 1
            val newTab = CalculatorTab(id = nextId, expression = "0", history = "", isResultState = false)
            repository.insertTab(newTab)
            _activeTabId.value = nextId
            _activeTokenIndex.value = null
            isOverwriting = false
        }
    }

    fun deleteActiveTab() {
        val currentActive = _activeTabId.value
        val tabs = allTabs.value
        if (tabs.size <= 1) return // Keep at least one tab
        
        viewModelScope.launch {
            repository.deleteTab(currentActive)
            val remainingTabs = tabs.filter { it.id != currentActive }
            _activeTabId.value = remainingTabs.lastOrNull()?.id ?: 1
            _activeTokenIndex.value = null
            isOverwriting = false
        }
    }

    fun masterReset() {
        viewModelScope.launch {
            repository.clearAll()
            val defaultTab = CalculatorTab(id = 1, expression = "0", history = "", isResultState = false)
            repository.insertTab(defaultTab)
            _activeTabId.value = 1
            _activeTokenIndex.value = null
            isOverwriting = false
        }
    }

    fun selectToken(index: Int) {
        _activeTokenIndex.value = index
        isOverwriting = true
    }

    fun deselectToken() {
        _activeTokenIndex.value = null
        isOverwriting = false
    }

    fun restoreActiveTabHistory() {
        val tabs = allTabs.value
        val activeId = _activeTabId.value
        val activeTab = tabs.find { it.id == activeId } ?: return
        if (activeTab.history.isEmpty()) return

        var histStr = activeTab.history
        if (histStr.endsWith(" =")) {
            histStr = histStr.substring(0, histStr.length - 2)
        }
        val cleanExpr = histStr
            .replace("×", "*")
            .replace("÷", "/")
            .replace(",", "")
            .trim()

        viewModelScope.launch {
            val updated = activeTab.copy(
                expression = cleanExpr,
                history = "",
                isResultState = false
            )
            repository.insertTab(updated)
            _activeTokenIndex.value = null
            isOverwriting = false
        }
    }

    fun pressButton(value: String) {
        val tabs = allTabs.value
        val activeId = _activeTabId.value
        val activeTab = tabs.find { it.id == activeId } ?: return

        var ex = activeTab.expression
        if (ex == "Error") ex = "0"

        val tokens = tokenizeExpression(ex).toMutableList()
        val ops = listOf("+", "-", "*", "/")
        val selectedIdx = _activeTokenIndex.value

        when (value) {
            "AC" -> {
                viewModelScope.launch {
                    val updated = activeTab.copy(expression = "0", history = "", isResultState = false)
                    repository.insertTab(updated)
                    _activeTokenIndex.value = null
                }
            }
            "BACK" -> {
                var newEx = ex
                if (selectedIdx != null && selectedIdx in tokens.indices) {
                    val edited = tokens[selectedIdx]
                    val isOpToken = edited in ops
                    if (!isOpOp(edited)) {
                        // For numbers, backspace replaces with "0", still selected, ready to overwrite
                        tokens[selectedIdx] = "0"
                        isOverwriting = true
                    } else {
                        // For operators, remove token, clear selection
                        tokens.removeAt(selectedIdx)
                        if (tokens.isEmpty()) tokens.add("0")
                        _activeTokenIndex.value = null
                    }
                    newEx = tokens.joinToString("")
                } else {
                    newEx = if (ex.length > 1) ex.substring(0, ex.length - 1) else "0"
                }

                viewModelScope.launch {
                    val updated = activeTab.copy(expression = newEx, isResultState = false)
                    repository.insertTab(updated)
                }
            }
            "=" -> {
                if (selectedIdx != null) {
                    // Commit edit state
                    _activeTokenIndex.value = null
                    isOverwriting = false
                    return
                }

                val hasOp = tokens.any { it in ops }
                if (!hasOp) return

                _activeTokenIndex.value = null
                try {
                    var exprForEval = ex
                    while (exprForEval.isNotEmpty() && exprForEval.last().toString() in listOf("+", "-", "*", "/", ".")) {
                        exprForEval = exprForEval.substring(0, exprForEval.length - 1)
                    }

                    val rawTokens = tokenizeExpression(exprForEval)
                    val processed = preprocessTokens(rawTokens)
                    val res = evaluateTokenList(processed)

                    if (res != null && res.isFinite()) {
                        val roundedRes = Math.round(res * 100000000.0) / 100000000.0
                        
                        var formattedEx = ""
                        val histTokens = tokenizeExpression(exprForEval)
                        for (token in histTokens) {
                            if (token in ops) {
                                val disp = token.replace("*", "×").replace("/", "÷")
                                formattedEx += disp
                            } else {
                                formattedEx += formatNumber(token)
                            }
                        }

                        val resultStr = if (roundedRes % 1.0 == 0.0) {
                            roundedRes.toLong().toString()
                        } else {
                            roundedRes.toString()
                        }

                        viewModelScope.launch {
                            val updated = activeTab.copy(
                                expression = resultStr,
                                history = "$formattedEx =",
                                isResultState = true
                            )
                            repository.insertTab(updated)
                        }
                    } else {
                        showError(activeTab, ex)
                    }
                } catch (e: Exception) {
                    showError(activeTab, ex)
                }
            }
            "%" -> {
                val targetIdx = selectedIdx ?: (tokens.size - 1)
                if (targetIdx in tokens.indices && tokens[targetIdx].toDoubleOrNull() != null) {
                    val num = tokens[targetIdx].toDouble()
                    if (targetIdx >= 2 && tokens[targetIdx - 1] in ops) {
                        val op = tokens[targetIdx - 1]
                        val baseExprSub = tokens.subList(0, targetIdx - 1)
                        val preprocessed = preprocessTokens(baseExprSub)
                        val baseVal = evaluateTokenList(preprocessed)
                        if (baseVal != null) {
                            if (op == "+" || op == "-") {
                                val percentVal = baseVal * (num / 100.0)
                                val rounded = Math.round(percentVal * 100000000.0) / 100000000.0
                                tokens[targetIdx] = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
                            } else {
                                val percentVal = num / 100.0
                                val rounded = Math.round(percentVal * 100000000.0) / 100000000.0
                                tokens[targetIdx] = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
                            }
                        } else {
                            val percentVal = num / 100.0
                            val rounded = Math.round(percentVal * 100000000.0) / 100000000.0
                            tokens[targetIdx] = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
                        }
                    } else {
                        val percentVal = num / 100.0
                        val rounded = Math.round(percentVal * 100000000.0) / 100000000.0
                        tokens[targetIdx] = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
                    }
                    val newEx = tokens.joinToString("")
                    viewModelScope.launch {
                        val updated = activeTab.copy(expression = newEx, isResultState = false)
                        repository.insertTab(updated)
                        _activeTokenIndex.value = null
                    }
                }
            }
            in ops -> {
                var newEx = ex
                if (selectedIdx != null && selectedIdx in tokens.indices && tokens[selectedIdx] in ops) {
                    tokens[selectedIdx] = value
                    newEx = tokens.joinToString("")
                    _activeTokenIndex.value = null
                } else {
                    _activeTokenIndex.value = null
                    val lastChar = ex.takeLast(1)
                    if (lastChar in ops) {
                        newEx = ex.substring(0, ex.length - 1) + value
                    } else {
                        newEx += value
                    }
                }
                viewModelScope.launch {
                    val updated = activeTab.copy(expression = newEx, isResultState = false)
                    repository.insertTab(updated)
                }
            }
            else -> { // Numeric input "0" - "9" or "."
                var newEx = ex
                if (selectedIdx != null) {
                    if (selectedIdx in tokens.indices && tokens[selectedIdx] in ops) {
                        _activeTokenIndex.value = null
                        if (ex == "0" && value != ".") {
                            newEx = value
                        } else if (value == ".") {
                            newEx += "0."
                        } else {
                            newEx += value
                        }
                    } else if (selectedIdx in tokens.indices) {
                        if (isOverwriting) {
                            if (value == ".") {
                                tokens[selectedIdx] = "0."
                            } else {
                                tokens[selectedIdx] = value
                            }
                            isOverwriting = false
                        } else {
                            if (value == "." && tokens[selectedIdx].contains(".")) {
                                // Ignore duplicate dots in editing token
                            } else {
                                if (tokens[selectedIdx] == "0" && value != ".") {
                                    tokens[selectedIdx] = value
                                } else {
                                    tokens[selectedIdx] += value
                                }
                            }
                        }
                        newEx = tokens.joinToString("")
                    }
                } else {
                    if (activeTab.isResultState) {
                        newEx = if (value == ".") "0." else value
                        viewModelScope.launch {
                            val updated = activeTab.copy(expression = newEx, history = "", isResultState = false)
                            repository.insertTab(updated)
                        }
                        return
                    } else {
                        if (value == ".") {
                            val lastToken = tokens.lastOrNull() ?: "0"
                            if (lastToken in ops) {
                                newEx += "0."
                            } else if (!lastToken.contains(".")) {
                                newEx += "."
                            }
                        } else {
                            if (ex == "0" && value != ".") {
                                newEx = value
                            } else {
                                newEx += value
                            }
                        }
                    }
                }
                viewModelScope.launch {
                    val updated = activeTab.copy(expression = newEx, isResultState = false)
                    repository.insertTab(updated)
                }
            }
        }
    }

    private fun showError(activeTab: CalculatorTab, origEx: String) {
        viewModelScope.launch {
            val errTab = activeTab.copy(expression = "Error", isResultState = false)
            repository.insertTab(errTab)
            // Restore back after 1 sec
            kotlinx.coroutines.delay(1000)
            val restored = activeTab.copy(expression = origEx, isResultState = false)
            repository.insertTab(restored)
        }
    }

    private fun isOpOp(token: String): Boolean = token in listOf("+", "-", "*", "/")

    // Indian Grouping form: e.g. 12,34,567.89
    fun formatNumber(numStr: String): String {
        if (numStr.isEmpty() || numStr == "-") return numStr
        val parts = numStr.split(".")
        var intPart = parts[0]
        val isNeg = intPart.startsWith("-")
        if (isNeg) intPart = intPart.substring(1)

        if (intPart.isNotEmpty()) {
            val lastThree = if (intPart.length >= 3) intPart.substring(intPart.length - 3) else intPart
            val otherNumbers = if (intPart.length > 3) intPart.substring(0, intPart.length - 3) else ""
            
            val groupedOther = StringBuilder()
            var i = otherNumbers.length
            while (i > 0) {
                val start = (i - 2).coerceAtLeast(0)
                val chunk = otherNumbers.substring(start, i)
                if (groupedOther.isNotEmpty()) {
                    groupedOther.insert(0, ",")
                }
                groupedOther.insert(0, chunk)
                i -= 2
            }
            
            var res = lastThree
            if (groupedOther.isNotEmpty()) {
                res = groupedOther.toString() + "," + lastThree
            }
            val formattedInt = if (isNeg) "-$res" else res
            
            return if (parts.size > 1) {
                "$formattedInt.${parts[1]}"
            } else {
                formattedInt
            }
        }
        return numStr
    }

    fun tokenizeExpression(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                tokens.add(c.toString())
                i++
            } else if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    sb.append(expr[i])
                    i++
                }
                tokens.add(sb.toString())
            } else {
                i++
            }
        }
        return tokens
    }

    fun preprocessTokens(rawTokens: List<String>): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < rawTokens.size) {
            val token = rawTokens[i]
            if (i == 0 && (token == "-" || token == "+") && i + 1 < rawTokens.size && rawTokens[i + 1].toDoubleOrNull() != null) {
                val sign = if (token == "-") "-" else ""
                result.add(sign + rawTokens[i + 1])
                i += 2
            } else if ((token == "+" || token == "-" || token == "*" || token == "/") &&
                i + 1 < rawTokens.size && rawTokens[i + 1] == "-" &&
                i + 2 < rawTokens.size && rawTokens[i + 2].toDoubleOrNull() != null) {
                result.add(token)
                result.add("-" + rawTokens[i + 2])
                i += 3
            } else {
                result.add(token)
                i++
            }
        }
        return result
    }

    fun evaluateTokenList(tokens: List<String>): Double? {
        if (tokens.isEmpty()) return null
        val list = tokens.toMutableList()

        // Unary signs at start
        if (list.size >= 2 && list[0] == "-" && list[1].toDoubleOrNull() != null) {
            list[1] = "-" + list[1]
            list.removeAt(0)
        }
        if (list.size >= 2 && list[0] == "+" && list[1].toDoubleOrNull() != null) {
            list.removeAt(0)
        }

        // Multiplications and Divisions
        var j = 0
        while (j < list.size) {
            val token = list[j]
            if (token == "*" || token == "/") {
                if (j - 1 >= 0 && j + 1 < list.size) {
                    val left = list[j - 1].toDoubleOrNull()
                    val right = list[j + 1].toDoubleOrNull()
                    if (left != null && right != null) {
                        val res = if (token == "*") left * right else left / right
                        list[j - 1] = res.toString()
                        list.removeAt(j + 1)
                        list.removeAt(j)
                        j--
                    } else {
                        return null
                    }
                } else {
                    return null
                }
            } else {
                j++
            }
        }

        // Additions and Subtractions
        var k = 0
        while (k < list.size) {
            val token = list[k]
            if (token == "+" || token == "-") {
                if (k - 1 >= 0 && k + 1 < list.size) {
                    val left = list[k - 1].toDoubleOrNull()
                    val right = list[k + 1].toDoubleOrNull()
                    if (left != null && right != null) {
                        val res = if (token == "+") left + right else left - right
                        list[k - 1] = res.toString()
                        list.removeAt(k + 1)
                        list.removeAt(k)
                        k--
                    } else {
                        return null
                    }
                } else {
                    return null
                }
            } else {
                k++
            }
        }

        return if (list.size == 1) list[0].toDoubleOrNull() else null
    }

    fun getLiveResult(expr: String): String {
        var exprForLive = expr
        while (exprForLive.isNotEmpty() && exprForLive.last().toString() in listOf("+", "-", "*", "/", ".")) {
            exprForLive = exprForLive.substring(0, exprForLive.length - 1)
        }
        val ops = listOf("+", "-", "*", "/")
        val tokens = tokenizeExpression(exprForLive)
        val hasOp = tokens.any { it in ops }

        if (exprForLive.isNotEmpty() && hasOp && exprForLive != "Error") {
            try {
                val processed = preprocessTokens(tokens)
                val res = evaluateTokenList(processed)
                if (res != null && res.isFinite()) {
                    val roundedRes = Math.round(res * 100000000.0) / 100000000.0
                    val formatted = formatNumber(
                        if (roundedRes % 1.0 == 0.0) roundedRes.toLong().toString() else roundedRes.toString()
                    )
                    return "= $formatted"
                }
            } catch (e: Exception) {
                return ""
            }
        }
        return ""
    }
}
