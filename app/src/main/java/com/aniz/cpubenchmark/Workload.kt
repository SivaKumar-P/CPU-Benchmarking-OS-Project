package com.aniz.cpubenchmark

enum class WorkloadType { PRIME_COUNTING, MATRIX_MULTIPLY }

object Workload {

    // ---- Prime counting ----
    fun countPrimesInRange(start: Int, end: Int): Long {
        var count = 0L
        for (n in start until end) {
            if (isPrime(n)) count++
        }
        return count
    }

    fun isPrime(n: Int): Boolean {
        if (n < 2) return false
        if (n == 2) return true
        if (n % 2 == 0) return false
        var i = 3
        while (i.toLong() * i <= n) {
            if (n % i == 0) return false
            i += 2
        }
        return true
    }

    // ---- Matrix multiplication ----
    const val MATRIX_SIZE = 300

    private val matrixA: Array<DoubleArray> by lazy { generateMatrix(MATRIX_SIZE) }
    private val matrixB: Array<DoubleArray> by lazy { generateMatrix(MATRIX_SIZE) }

    private fun generateMatrix(n: Int): Array<DoubleArray> =
        Array(n) { i -> DoubleArray(n) { j -> ((i + j) % 7 + 1).toDouble() } }

    // Multiplies rows [start, end) of matrixA by matrixB. Returns cells computed.
    fun multiplyRows(start: Int, end: Int): Long {
        val n = MATRIX_SIZE
        var cellsComputed = 0L
        for (row in start until end) {
            for (col in 0 until n) {
                var sum = 0.0
                for (k in 0 until n) {
                    sum += matrixA[row][k] * matrixB[k][col]
                }
                cellsComputed++
            }
        }
        return cellsComputed
    }

    // Picks the correct range to split across threads depending on workload type.
    fun rangeEndFor(type: WorkloadType, requestedRange: Int): Int =
        if (type == WorkloadType.MATRIX_MULTIPLY) MATRIX_SIZE else requestedRange
}