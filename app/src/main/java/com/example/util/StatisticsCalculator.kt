package com.example.util

import kotlin.math.sqrt
import kotlin.math.abs

data class StatisticsResult(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val median: Double,
    val mode: List<Double>,
    val min: Double,
    val max: Double,
    val range: Double,
    val sampleVariance: Double,
    val sampleStandardDeviation: Double,
    val populationVariance: Double,
    val populationStandardDeviation: Double,
    val sortedData: List<Double>
)

data class RegressionResult(
    val slope: Double,
    val intercept: Double,
    val r: Double, // Pearson correlation coefficient
    val rSquared: Double, // Coefficient of determination
    val formula: String, // e.g. "y = 1.25x + 3.0"
    val xSum: Double,
    val ySum: Double,
    val xMean: Double,
    val yMean: Double,
    val count: Int,
    val pairs: List<Pair<Double, Double>>
)

object StatisticsCalculator {

    fun calculate(values: List<Double>): StatisticsResult? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val count = sorted.size
        val sum = sorted.sum()
        val mean = sum / count

        // Median
        val median = if (count % 2 == 0) {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        } else {
            sorted[count / 2]
        }

        // Mode
        val frequencyMap = sorted.groupingBy { it }.eachCount()
        val maxFrequency = frequencyMap.values.maxOrNull() ?: 0
        val mode = frequencyMap.filter { it.value == maxFrequency && maxFrequency > 1 }.keys.toList()

        val min = sorted.first()
        val max = sorted.last()
        val range = max - min

        // Variance & SD
        var accumSample = 0.0
        var accumPop = 0.0
        for (num in sorted) {
            val diff = num - mean
            accumSample += diff * diff
            accumPop += diff * diff
        }

        val sampleVariance = if (count > 1) accumSample / (count - 1) else 0.0
        val sampleStandardDeviation = sqrt(sampleVariance)

        val populationVariance = accumPop / count
        val populationStandardDeviation = sqrt(populationVariance)

        return StatisticsResult(
            count = count,
            sum = sum,
            mean = mean,
            median = median,
            mode = mode,
            min = min,
            max = max,
            range = range,
            sampleVariance = sampleVariance,
            sampleStandardDeviation = sampleStandardDeviation,
            populationVariance = populationVariance,
            populationStandardDeviation = populationStandardDeviation,
            sortedData = sorted
        )
    }

    /**
     * Calculates simple linear regression for two paired lists of values (X and Y datasets).
     */
    fun calculateRegression(xValues: List<Double>, yValues: List<Double>): RegressionResult? {
        val n = minOf(xValues.size, yValues.size)
        if (n < 2) return null

        val x = xValues.take(n)
        val y = yValues.take(n)

        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).map { it.first * it.second }.sum()
        val sumXX = x.map { it * it }.sum()
        val sumYY = y.map { it * it }.sum()

        val meanX = sumX / n
        val meanY = sumY / n

        val numeratorSlope = n * sumXY - sumX * sumY
        val denominatorSlope = n * sumXX - sumX * sumX

        if (denominatorSlope == 0.0) return null

        val slope = numeratorSlope / denominatorSlope
        val intercept = meanY - slope * meanX

        // Pearson correlation r
        val rNumerator = n * sumXY - sumX * sumY
        val rDenominator = sqrt((n * sumXX - sumX * sumX) * (n * sumYY - sumY * sumY))

        val r = if (rDenominator != 0.0) rNumerator / rDenominator else 0.0
        val rSquared = r * r

        val formattedSlope = String.format("%.4f", slope)
        val formattedIntercept = String.format("%.4f", intercept)
        val sign = if (intercept >= 0) "+" else "-"
        val formula = "y = ${formattedSlope}x $sign ${abs(intercept)}"

        return RegressionResult(
            slope = slope,
            intercept = intercept,
            r = r,
            rSquared = rSquared,
            formula = formula,
            xSum = sumX,
            ySum = sumY,
            xMean = meanX,
            yMean = meanY,
            count = n,
            pairs = x.zip(y)
        )
    }

    /**
     * Parse comma/space separated input into double list safely.
     */
    fun parseInput(input: String): List<Double> {
        val matches = Regex("[-+]?\\d*\\.?\\d+").findAll(input)
        return matches.mapNotNull { it.value.toDoubleOrNull() }.toList()
    }
}
