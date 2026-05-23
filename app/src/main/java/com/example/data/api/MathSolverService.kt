package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MathSolverService {

    suspend fun solveMathProblem(
        problem: String,
        category: String = "General Math"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is missing. Please add your GEMINI_API_KEY to the AI Studio Secrets panel."))
        }

        val systemInstructionText = """
            You are "Math Solver", a highly sophisticated mathematical engine and professor. 
            Solve the user's mathematical question precisely, regardless of whether it represents General Math, Higher Math (calculus, linear algebra, differential equations, discrete math, complex analysis), Graphing analysis, or Statistics.
            
            Follow these guidance principles when formatting your response:
            1. Provide a crisp, clear, bold final answer at the very beginning or highlighted nicely at the end (e.g., **Final Answer: x = 5**).
            2. Break down the steps logically with clear explanations. Break your math down into structured steps (Step 1, Step 2, etc.).
            3. Use clear mathematical notation with symbols like ∫, ∑, ∂, ∂x, d/dx, √, π, θ, ^ for powers (e.g. x² as x^2), and matrices formatted cleanly.
            4. If the question is about statistics, outline the intermediate steps (e.g., write down standard deviation equations, means, formulas) so the user gets context of how it's calculated.
            5. If the user asks for graphing details, describe the function properties: intercepts, bounds, critical values, asymptotes, and general shape so they understand how to plot it.
            6. Keep the prose warm, professional, objective, and easy to read. Do not output raw HTML tags or complex LaTeX formulas that don't render on plain phone displays. Instead, use clean Unicode / standard math formatting (e.g., ∫ x² dx = x³/3 + C).
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = problem)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.15f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemInstructionText))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val solutionText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (solutionText != null) {
                Result.success(solutionText)
            } else {
                Result.failure(Exception("Could not extract a solution from the AI response structure. Please try rephrasing basic inputs."))
            }
        } catch (e: Exception) {
            Log.e("MathSolverService", "Error solving problem", e)
            val friendlyError = if (e is retrofit2.HttpException) {
                when (e.code()) {
                    400 -> Exception("HTTP 400 Bad Request: Invalid request or API key. Please make sure the API key is active, unrestricted, and formatted correctly.")
                    403 -> Exception("HTTP 403 Forbidden: Your Gemini API key is invalid, restricted, or your region is not supported by Google Gemini API. Please make sure to add a valid, unrestricted GEMINI_API_KEY in the AI Studio Secrets panel.")
                    404 -> Exception("HTTP 404 Not Found: The requested Gemini model endpoint is unavailable. Please verify the model configuration.")
                    429 -> Exception("HTTP 429 Too Many Requests: Gemini API rate limit exceeded. Please wait a moment and try again.")
                    else -> Exception("HTTP ${e.code()}: ${e.message() ?: "Request failed"}")
                }
            } else e
            Result.failure(friendlyError)
        }
    }
}
