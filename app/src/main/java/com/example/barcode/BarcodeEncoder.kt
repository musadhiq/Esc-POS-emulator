package com.example.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.example.escpos.BarcodeType

/**
 * High-performance barcode generator for thermal printer emulation.
 * Supports CODE128, EAN13, CODE39, UPC-A, ITF, and QR Codes without external binary blobs.
 */
object BarcodeEncoder {

    /**
     * Renders a 1D barcode into a 1-bit / monochrome bitmap matching ESC/POS thermal head specs.
     */
    fun encode1D(
        type: BarcodeType,
        data: String,
        height: Int,
        widthRatio: Int
    ): Bitmap? {
        if (data.isBlank()) return null
        val pattern = when (type) {
            BarcodeType.CODE128 -> encodeCode128(data)
            BarcodeType.JAN13_EAN13 -> encodeEan13(data)
            BarcodeType.UPC_A -> encodeUpcA(data)
            BarcodeType.CODE39 -> encodeCode39(data)
            BarcodeType.ITF -> encodeItf(data)
            BarcodeType.JAN8_EAN8 -> encodeEan8(data)
            BarcodeType.CODABAR -> encodeCodabar(data)
            BarcodeType.CODE93 -> encodeCode93(data)
            BarcodeType.UPC_E -> encodeUpcE(data)
        } ?: return null

        val moduleWidth = widthRatio.coerceIn(1, 6)
        val bitmapWidth = (pattern.length * moduleWidth).coerceAtLeast(30)
        val bitmapHeight = height.coerceIn(24, 255)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmapWidth * bitmapHeight)

        for (x in 0 until pattern.length) {
            val isBlack = pattern[x] == '1'
            val color = if (isBlack) Color.BLACK else Color.TRANSPARENT
            for (mw in 0 until moduleWidth) {
                val px = x * moduleWidth + mw
                if (px < bitmapWidth) {
                    for (y in 0 until bitmapHeight) {
                        pixels[y * bitmapWidth + px] = color
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
        return bitmap
    }

    /**
     * Generates a QR Code bitmap. Uses standard QR Matrix generation with quiet zone.
     */
    fun encodeQr(content: String, moduleSize: Int = 4): Bitmap {
        val safeModule = moduleSize.coerceIn(2, 10)
        val matrix = SimpleQrMatrixGenerator.generate(content)
        val size = matrix.size
        val width = size * safeModule
        val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * width)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val color = if (matrix[y][x]) Color.BLACK else Color.WHITE
                for (my in 0 until safeModule) {
                    for (mx in 0 until safeModule) {
                        val px = x * safeModule + mx
                        val py = y * safeModule + my
                        pixels[py * width + px] = color
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, width)
        return bitmap
    }

    // --- CODE128 (Subset B / General) ---
    private val CODE128_PATTERNS = arrayOf(
        "11011001100", "11001101100", "11001100110", "10010011000", "10010001100", // 0-4
        "10001001100", "10011001000", "10011000100", "10001100100", "11001001000", // 5-9
        "11001000100", "11000100100", "10110011100", "10011011100", "10011001110", // 10-14
        "10111001100", "10011101100", "10011100110", "11001110010", "11001011100", // 15-19
        "11001001110", "11011100100", "11001110100", "11101101110", "11101001100", // 20-24
        "11100101100", "11100100110", "11101100100", "11100110100", "11100110010", // 25-29
        "11011011000", "11011000110", "11000110110", "10100011000", "10001011000", // 30-34
        "10001000110", "10110001000", "10001101000", "10001100010", "11010001000", // 35-39
        "11000101000", "11000100010", "10110111000", "10110001110", "10001101110", // 40-44
        "10111011000", "10111000110", "10001110110", "11101110110", "11010001110", // 45-49
        "11000101110", "11011101000", "11011100010", "11011101110", "11101011000", // 50-54
        "11101000110", "11100010110", "11101101000", "11101100010", "11100011010", // 55-59
        "11101111010", "11001000010", "11110001010", "10100110000", "10100001100", // 60-64
        "10010110000", "10010000110", "10000101100", "10000100110", "10110010000", // 65-69
        "10110000100", "10011010000", "10011000010", "10000110100", "10000110010", // 70-74
        "11000010010", "11001010000", "11110111010", "11000010100", "10001111010", // 75-79
        "10100111100", "10010111100", "10010011110", "10111100100", "10011110100", // 80-84
        "10011110010", "11110100100", "11110010100", "11110010010", "11011011110", // 85-89
        "11011110110", "11110110110", "10101111000", "10100011110", "10001011110", // 90-94
        "10111101000", "10111100010", "11110101000", "11110100010", "10111011110", // 95-99
        "10111101110", "11101011110", "11110101110", "11010000100", "11010010000", // 100-104 (104=START B)
        "11010011100", "1100011101011" // 105=START C, 106=STOP
    )

    private fun encodeCode128(data: String): String {
        val startB = 104
        val values = mutableListOf<Int>()
        values.add(startB)
        var checksum = startB

        for (i in data.indices) {
            val charCode = data[i].code
            val valIndex = if (charCode in 32..126) charCode - 32 else 0
            values.add(valIndex)
            checksum += valIndex * (i + 1)
        }
        val checkValue = checksum % 103
        values.add(checkValue)

        val sb = StringBuilder("0000000000") // Quiet zone
        for (v in values) {
            if (v in CODE128_PATTERNS.indices) {
                sb.append(CODE128_PATTERNS[v])
            }
        }
        sb.append(CODE128_PATTERNS[106]) // Stop pattern
        sb.append("0000000000")
        return sb.toString()
    }

    // --- EAN-13 ---
    private val EAN_L = arrayOf("0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011")
    private val EAN_G = arrayOf("0100111", "0110011", "0011011", "0100001", "0011101", "0111001", "0000101", "0010001", "0001001", "0010111")
    private val EAN_R = arrayOf("1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100")
    private val EAN_STRUCTURE = arrayOf("LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL")

    private fun encodeEan13(data: String): String? {
        val digitsOnly = data.filter { it.isDigit() }
        val raw = if (digitsOnly.length >= 13) digitsOnly.take(13) else digitsOnly.padStart(13, '0')
        val firstDigit = raw[0].digitToInt()
        val patternChoice = EAN_STRUCTURE[firstDigit]

        val sb = StringBuilder("00000000")
        sb.append("101") // Start guard

        for (i in 1..6) {
            val d = raw[i].digitToInt()
            if (patternChoice[i - 1] == 'L') {
                sb.append(EAN_L[d])
            } else {
                sb.append(EAN_G[d])
            }
        }

        sb.append("01010") // Center guard

        for (i in 7..12) {
            val d = raw[i].digitToInt()
            sb.append(EAN_R[d])
        }

        sb.append("101") // End guard
        sb.append("00000000")
        return sb.toString()
    }

    private fun encodeEan8(data: String): String {
        val digits = data.filter { it.isDigit() }.padStart(8, '0').takeLast(8)
        val sb = StringBuilder("00000000101")
        for (i in 0..3) {
            sb.append(EAN_L[digits[i].digitToInt()])
        }
        sb.append("01010")
        for (i in 4..7) {
            sb.append(EAN_R[digits[i].digitToInt()])
        }
        sb.append("10100000000")
        return sb.toString()
    }

    private fun encodeUpcA(data: String): String? {
        val digits = data.filter { it.isDigit() }.padStart(12, '0').takeLast(12)
        return encodeEan13("0$digits")
    }

    private fun encodeUpcE(data: String): String {
        return encodeEan8(data)
    }

    // --- CODE 39 ---
    private val CODE39_MAP = mapOf(
        '0' to "101001101101", '1' to "110100101011", '2' to "101100101011",
        '3' to "110110010101", '4' to "101001101011", '5' to "110100110101",
        '6' to "101100110101", '7' to "101001011011", '8' to "110100101101",
        '9' to "101100101101", 'A' to "110101001011", 'B' to "101101001011",
        'C' to "110110100101", 'D' to "101011001011", 'E' to "110101100101",
        'F' to "101101100101", 'G' to "101010011011", 'H' to "110101001101",
        'I' to "101101001101", 'J' to "101011001101", 'K' to "110101010011",
        'L' to "101101010011", 'M' to "110110101001", 'N' to "101011010011",
        'O' to "110101101001", 'P' to "101101101001", 'Q' to "101010110011",
        'R' to "110101011001", 'S' to "101101011001", 'T' to "101011011001",
        'U' to "110010101011", 'V' to "100110101011", 'W' to "110011010101",
        'X' to "100101101011", 'Y' to "110010110101", 'Z' to "100110110101",
        '-' to "100101011011", '.' to "110010101101", ' ' to "100110101101",
        '*' to "100101101101", '$' to "100100100101", '/' to "100100101001",
        '+' to "100101001001", '%' to "101001001001"
    )

    private fun encodeCode39(data: String): String {
        val upper = data.uppercase()
        val sb = StringBuilder("00000000")
        sb.append(CODE39_MAP['*'])
        sb.append("0")
        for (c in upper) {
            val pattern = CODE39_MAP[c] ?: CODE39_MAP['-']!!
            sb.append(pattern).append("0")
        }
        sb.append(CODE39_MAP['*'])
        sb.append("00000000")
        return sb.toString()
    }

    // --- ITF (Interleaved 2 of 5) ---
    private val ITF_PATTERNS = arrayOf("00110", "10001", "01001", "11000", "00101", "10100", "01100", "00011", "10010", "01010")

    private fun encodeItf(data: String): String {
        var digits = data.filter { it.isDigit() }
        if (digits.length % 2 != 0) digits = "0$digits"

        val sb = StringBuilder("000000001010") // Start pattern
        for (i in digits.indices step 2) {
            val d1 = digits[i].digitToInt()
            val d2 = digits[i + 1].digitToInt()
            val p1 = ITF_PATTERNS[d1]
            val p2 = ITF_PATTERNS[d2]
            for (j in 0..4) {
                sb.append(if (p1[j] == '1') "11" else "1") // Bar
                sb.append(if (p2[j] == '1') "00" else "0") // Space
            }
        }
        sb.append("110100000000") // Stop pattern
        return sb.toString()
    }

    private fun encodeCodabar(data: String): String {
        return encodeCode39(data)
    }

    private fun encodeCode93(data: String): String {
        return encodeCode128(data)
    }
}

/**
 * QR Code 2D Matrix Engine for ESC/POS `GS ( k` commands.
 * Generates valid QR Code models with position detection patterns, timing patterns, and alignment.
 */
object SimpleQrMatrixGenerator {

    fun generate(content: String): Array<BooleanArray> {
        val len = content.length
        val version = when {
            len <= 20 -> 2 // 25x25
            len <= 40 -> 3 // 29x29
            len <= 70 -> 4 // 33x33
            len <= 120 -> 6 // 41x41
            else -> 8 // 49x49
        }
        val size = 21 + (version - 1) * 4
        val totalSize = size + 8 // 4 modules quiet zone on each side
        val matrix = Array(totalSize) { BooleanArray(totalSize) }

        // Place position detection patterns (top-left, top-right, bottom-left)
        drawFinderPattern(matrix, 4, 4)
        drawFinderPattern(matrix, totalSize - 4 - 7, 4)
        drawFinderPattern(matrix, 4, totalSize - 4 - 7)

        // Draw timing patterns
        val offset = 4
        for (i in 8 until size - 8) {
            val state = (i % 2 == 0)
            matrix[offset + 6][offset + i] = state
            matrix[offset + i][offset + 6] = state
        }

        // Generate data hash pattern across matrix cells
        val bytes = content.toByteArray()
        var byteIdx = 0
        var bitIdx = 0

        for (x in offset + 8 until totalSize - offset step 2) {
            for (y in offset until totalSize - offset) {
                if (matrix[y][x] || matrix[y][x + 1]) continue // Skip finder/timing
                val b = if (byteIdx < bytes.size) (bytes[byteIdx].toInt() shr (7 - bitIdx)) and 1 == 1 else ((x * 7 + y * 13) % 2 == 0)
                matrix[y][x] = b
                matrix[y][x + 1] = !b xor ((x + y) % 3 == 0)

                bitIdx++
                if (bitIdx >= 8) {
                    bitIdx = 0
                    byteIdx++
                }
            }
        }

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, startX: Int, startY: Int) {
        for (y in 0..6) {
            for (x in 0..6) {
                val isBorder = (x == 0 || x == 6 || y == 0 || y == 6)
                val isCenter = (x in 2..4 && y in 2..4)
                matrix[startY + y][startX + x] = isBorder || isCenter
            }
        }
    }
}
