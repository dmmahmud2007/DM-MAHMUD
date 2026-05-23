package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.MathSolverService
import com.example.data.db.AppDatabase
import com.example.data.db.HistoryItem
import com.example.data.db.HistoryRepository
import com.example.util.StatisticsCalculator
import com.example.util.StatisticsResult
import com.example.util.RegressionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SolverUiState {
    object Idle : SolverUiState
    object Loading : SolverUiState
    data class Success(val solution: String) : SolverUiState
    data class Error(val message: String) : SolverUiState
}

class MathSolverViewModel(
    application: Application,
    private val repository: HistoryRepository,
    private val solverService: MathSolverService
) : AndroidViewModel(application) {

    // Global selected tab
    private val _currentTab = MutableStateFlow(0) // 0: Solver, 1: Graphing, 2: Statistics, 3: History
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Solver Module State
    private val _solverInput = MutableStateFlow("")
    val solverInput: StateFlow<String> = _solverInput.asStateFlow()

    private val _solverState = MutableStateFlow<SolverUiState>(SolverUiState.Idle)
    val solverState: StateFlow<SolverUiState> = _solverState.asStateFlow()

    // Graphing Module State
    private val _graphInput = MutableStateFlow("y = x^2 - 4")
    val graphInput: StateFlow<String> = _graphInput.asStateFlow()

    private val _zoomLevel = MutableStateFlow(10f) // Represents maximum coordinate bounds on X and Y: -zoom to +zoom
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _selectedGraphPoint = MutableStateFlow<Pair<Float, Float>?>(null) // Interactive graph point tracker [x, y]
    val selectedGraphPoint: StateFlow<Pair<Float, Float>?> = _selectedGraphPoint.asStateFlow()

    private val _panOffsetX = MutableStateFlow(0f)
    val panOffsetX: StateFlow<Float> = _panOffsetX.asStateFlow()

    private val _panOffsetY = MutableStateFlow(0f)
    val panOffsetY: StateFlow<Float> = _panOffsetY.asStateFlow()

    private val _graphDragMode = MutableStateFlow(0) // 0: Trace cursor, 1: Pan canvas
    val graphDragMode: StateFlow<Int> = _graphDragMode.asStateFlow()

    // Statistics Module State
    private val _statsInput = MutableStateFlow("1, 2, 3, 4, 5, 6, 7")
    val statsInput: StateFlow<String> = _statsInput.asStateFlow()

    private val _statsYInput = MutableStateFlow("2.1, 3.9, 6.1, 8.0, 9.9, 12.1, 14.2")
    val statsYInput: StateFlow<String> = _statsYInput.asStateFlow()

    private val _statsResult = MutableStateFlow<StatisticsResult?>(null)
    val statsResult: StateFlow<StatisticsResult?> = _statsResult.asStateFlow()

    private val _statsYResult = MutableStateFlow<StatisticsResult?>(null)
    val statsYResult: StateFlow<StatisticsResult?> = _statsYResult.asStateFlow()

    private val _regressionResult = MutableStateFlow<RegressionResult?>(null)
    val regressionResult: StateFlow<RegressionResult?> = _regressionResult.asStateFlow()

    // History list observed from Room
    val historyList: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Trigger initial statistics computation for default numbers
        computeStatistics()
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun updateSolverInput(query: String) {
        _solverInput.value = query
    }

    fun appendToSolverInput(symbol: String) {
        _solverInput.value = _solverInput.value + symbol
    }

    fun updateGraphInput(formula: String) {
        _graphInput.value = formula
        _selectedGraphPoint.value = null // clear marker on function changes
    }

    fun setZoom(zoom: Float) {
        // constrain zoom range between 2 and 100
        _zoomLevel.value = zoom.coerceIn(2f, 100f)
    }

    fun setGraphPoint(point: Pair<Float, Float>?) {
        _selectedGraphPoint.value = point
    }

    fun pan(dx: Float, dy: Float) {
        _panOffsetX.value += dx
        _panOffsetY.value += dy
    }

    fun resetPan() {
        _panOffsetX.value = 0f
        _panOffsetY.value = 0f
    }

    fun setDragMode(mode: Int) {
        _graphDragMode.value = mode
    }

    fun updateStatsInput(numbers: String) {
        _statsInput.value = numbers
        computeStatistics()
    }

    fun updateStatsYInput(numbers: String) {
        _statsYInput.value = numbers
        computeStatistics()
    }

    private fun computeStatistics() {
        val parsedX = StatisticsCalculator.parseInput(_statsInput.value)
        val parsedY = StatisticsCalculator.parseInput(_statsYInput.value)

        _statsResult.value = StatisticsCalculator.calculate(parsedX)
        _statsYResult.value = StatisticsCalculator.calculate(parsedY)

        _regressionResult.value = if (parsedX.isNotEmpty() && parsedY.isNotEmpty()) {
            StatisticsCalculator.calculateRegression(parsedX, parsedY)
        } else {
            null
        }
    }

    fun solveCurrentProblem() {
        val query = _solverInput.value.trim()
        if (query.isEmpty()) {
            _solverState.value = SolverUiState.Error("Please enter a math question first.")
            return
        }

        _solverState.value = SolverUiState.Loading

        viewModelScope.launch {
            val result = solverService.solveMathProblem(query)
            result.onSuccess { solution ->
                _solverState.value = SolverUiState.Success(solution)
                // Save to historical Room database automatically!
                repository.insert(
                    HistoryItem(
                        question = query,
                        answer = solution,
                        category = "Solver"
                    )
                )
            }
            result.onFailure { error ->
                _solverState.value = SolverUiState.Error(error.message ?: "Failed to contact Math Engine service.")
            }
        }
    }

    fun addHistoryItem(question: String, answer: String, category: String) {
        viewModelScope.launch {
            repository.insert(HistoryItem(question = question, answer = answer, category = category))
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Factory Class for construction
    class Factory(
        private val application: Application,
        private val repository: HistoryRepository,
        private val solverService: MathSolverService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MathSolverViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MathSolverViewModel(application, repository, solverService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
