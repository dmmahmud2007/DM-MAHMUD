package com.example.util

import java.util.Locale
import kotlin.math.*

class MathExpressionEvaluator {

    companion object {
        /**
         * Preprocesses the equation string by resolving implicit multiplications
         * and standardizing function names so that the recursive descent parser can evaluate it.
         */
        fun preprocess(formula: String): String {
            var f = formula.lowercase(Locale.ROOT)
                .replace(" ", "")
                .replace("y=", "")
                .replace("f(x)=", "")

            // Match digit directly followed by 'x' -> 'digit*x'
            f = f.replace(Regex("(\\d)(x)"), "$1*$2")
            // Match 'x' directly followed by a digit (rare, but handle) -> 'x*digit'
            f = f.replace(Regex("(x)(\\d)"), "$1*$2")
            // Match digit directly followed by opening parenthesis -> 'digit*('
            f = f.replace(Regex("(\\d)(\\()"), "$1*$2")
            // Match 'x' directly followed by opening parenthesis -> 'x*('
            f = f.replace(Regex("(x)(\\()"), "$1*$2")
            // Match digit directly followed by function name
            val mathFuncs = listOf("sin", "cos", "tan", "sqrt", "log", "ln", "abs", "exp")
            for (func in mathFuncs) {
                f = f.replace(Regex("(\\d)($func)"), "$1*$2")
                f = f.replace(Regex("(x)($func)"), "$1*$2")
                f = f.replace(Regex("(\\))($func)"), "$1*$2")
            }
            // Match closing parenthesis followed by digit -> ')*digit'
            f = f.replace(Regex("(\\))(\\d)"), "$1*$2")
            // Match closing parenthesis followed by 'x' -> ')*x'
            f = f.replace(Regex("(\\))(x)"), "$1*$2")
            // Match closing parenthesis followed by opening parenthesis -> ')*('
            f = f.replace(Regex("(\\))(\\()"), "$1*$2")

            return f
        }
    }

    /**
     * Evaluates a mathematical expression at a specific x-value.
     */
    fun evaluate(expression: String, xVal: Double): Double {
        val sanitized = preprocess(expression)
        return Parser(sanitized, xVal).parse()
    }

    private class Parser(private val str: String, private val xVal: Double) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            pos++
            ch = if (pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) {
                // If parsing finished earlier than the string, something is wrong
                return Double.NaN
            }
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm() // addition
                else if (eat('-'.code)) x -= parseTerm() // subtraction
                else return x
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor() // multiplication
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    x = if (divisor == 0.0) Double.NaN else x / divisor // division
                } else return x
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor() // unary plus
            if (eat('-'.code)) return -parseFactor() // unary minus

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) { // parentheses
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, this.pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions and variables
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, this.pos)
                if (func == "x") {
                    x = xVal
                } else {
                    // It must be a math function, evaluate its factor argument
                    val arg = parseFactor()
                    x = when (func) {
                        "sin" -> sin(arg)
                        "cos" -> cos(arg)
                        "tan" -> tan(arg)
                        "sqrt" -> if (arg < 0.0) Double.NaN else sqrt(arg)
                        "log" -> if (arg <= 0.0) Double.NaN else log10(arg)
                        "ln" -> if (arg <= 0.0) Double.NaN else ln(arg)
                        "abs" -> abs(arg)
                        "exp" -> exp(arg)
                        else -> Double.NaN
                    }
                }
            } else {
                return Double.NaN
            }

            if (eat('^'.code)) {
                val power = parseFactor()
                x = x.pow(power)
            }

            return x
        }
    }
}
