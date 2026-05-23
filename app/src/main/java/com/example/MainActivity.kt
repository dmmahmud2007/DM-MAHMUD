package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.api.MathSolverService
import com.example.data.db.AppDatabase
import com.example.data.db.HistoryRepository
import com.example.ui.MathSolverApp
import com.example.ui.MathSolverViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Local Database and Repositories
    val database = AppDatabase.getDatabase(applicationContext)
    val historyRepository = HistoryRepository(database.historyDao())
    val solverService = MathSolverService()

    // Initialize Math Solver ViewModel
    val viewModel = MathSolverViewModel.Factory(
        application = application,
        repository = historyRepository,
        solverService = solverService
    ).create(MathSolverViewModel::class.java)

    setContent {
      MyApplicationTheme {
        MathSolverApp(viewModel = viewModel)
      }
    }
  }
}
