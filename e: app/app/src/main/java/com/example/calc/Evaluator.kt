package com.example.calc

import kotlin.math.*

/** Recursive-descent scientific expression evaluator. */
class Evaluator(private val degrees: Boolean) {
    private var s = ""
    private var i = 0

    fun eval(expr: String): Double {
        s = expr.replace(" ", "").replace("×", "*").replace("÷", "/").replace("π", "pi").replace("√", "sqrt")
        i = 0
        val v = parseExpr()
        if (i < s.length) error("Unexpected '${s[i]}'")
        if (v.isNaN() || v.isInfinite()) error("Math error")
        return v
    }

    private fun peek() = if (i < s.length) s[i] else '\u0000'

    private fun parseExpr(): Double {
        var v = parseTerm()
        while (true) when (peek()) {
            '+' -> { i++; v += parseTerm() }
            '-' -> { i++; v -= parseTerm() }
            else -> return v
        }
    }

    private fun parseTerm(): Double {
        var v = parseUnary()
        while (true) {
            val c = peek()
            when {
                c == '*' -> { i++; v *= parseUnary() }
                c == '/' -> { i++; v /= parseUnary() }
                c == '(' || c.isLetter() -> v *= parseUnary() // implicit multiply: 2pi, 3(4+1)
                else -> return v
            }
        }
    }

    private fun parseUnary(): Double = when (peek()) {
        '-' -> { i++; -parseUnary() }
        '+' -> { i++; parseUnary() }
        else -> parsePow()
    }

    private fun parsePow(): Double {
        val base = parsePostfix()
        if (peek() == '^') { i++; return base.pow(parseUnary()) }
        return base
    }

    private fun parsePostfix(): Double {
        var v = parsePrimary()
        while (true) when (peek()) {
            '!' -> { i++; v = fact(v) }
            '%' -> { i++; v /= 100 }
            else -> return v
        }
    }

    private fun parsePrimary(): Double {
        val c = peek()
        if (c.isDigit() || c == '.') {
            val st = i
            while (peek().isDigit() || peek() == '.') i++
            return s.substring(st, i).toDouble()
        }
        if (c == '(') {
            i++; val v = parseExpr(); expect(')'); return v
        }
        if (c.isLetter()) {
            val st = i
            while (peek().isLetter()) i++
            val name = s.substring(st, i)
            if (peek() == '(') {
                i++
                val args = mutableListOf(parseExpr())
                while (peek() == ',') { i++; args += parseExpr() }
                expect(')')
                return call(name, args)
            }
            return when (name) {
                "pi" -> PI
                "e" -> E
                else -> error("Unknown '$name'")
            }
        }
        error("Unexpected input")
    }

    private fun expect(ch: Char) { if (peek() != ch) error("Expected '$ch'"); i++ }

    private fun rad(x: Double) = if (degrees) Math.toRadians(x) else x
    private fun out(x: Double) = if (degrees) Math.toDegrees(x) else x

    private fun call(n: String, a: List<Double>): Double {
        val x = a[0]
        return when (n) {
            "sin" -> sin(rad(x))
            "cos" -> cos(rad(x))
            "tan" -> tan(rad(x))
            "asin" -> out(asin(x))
            "acos" -> out(acos(x))
            "atan" -> out(atan(x))
            "sinh" -> sinh(x)
            "cosh" -> cosh(x)
            "tanh" -> tanh(x)
            "ln" -> ln(x)
            "log" -> if (a.size == 2) ln(a[1]) / ln(x) else log10(x)
            "sqrt" -> sqrt(x)
            "cbrt" -> cbrt(x)
            "abs" -> abs(x)
            "exp" -> exp(x)
            "floor" -> floor(x)
            "ceil" -> ceil(x)
            "round" -> round(x)
            "nPr" -> fact(x) / fact(x - a[1])
            "nCr" -> fact(x) / (fact(a[1]) * fact(x - a[1]))
            "mod" -> x % a[1]
            "gcd" -> gcd(x.toLong(), a[1].toLong()).toDouble()
            else -> error("Unknown function '$n'")
        }
    }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) abs(a) else gcd(b, a % b)

    private fun fact(x: Double): Double {
        if (x < 0 || x != floor(x) || x > 170) error("Math error")
        var r = 1.0
        for (k in 2..x.toInt()) r *= k
        return r
    }
}
