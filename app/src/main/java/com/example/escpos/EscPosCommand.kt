package com.example.escpos

import android.graphics.Bitmap

/**
 * Alignment options for ESC/POS commands (ESC a n).
 */
enum class PrintAlignment {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Font type for ESC/POS commands (ESC M n).
 */
enum class PrintFont {
    FONT_A, // Standard 12x24
    FONT_B, // Condensed 9x17
    FONT_C  // Extra condensed
}

/**
 * Text styling attributes (Bold, Underline, Invert, Size).
 */
data class TextStyle(
    val font: PrintFont = PrintFont.FONT_A,
    val bold: Boolean = false,
    val underline: Int = 0, // 0 = none, 1 = 1-dot, 2 = 2-dot
    val doubleStrike: Boolean = false,
    val inverted: Boolean = false, // White on black
    val widthScale: Int = 1, // 1 to 8
    val heightScale: Int = 1 // 1 to 8
)

/**
 * Barcode symbology types (GS k).
 */
enum class BarcodeType {
    UPC_A,
    UPC_E,
    JAN13_EAN13,
    JAN8_EAN8,
    CODE39,
    ITF,
    CODABAR,
    CODE93,
    CODE128
}

/**
 * Human Readable Interpretation (HRI) position for barcodes (GS H n).
 */
enum class HriPosition {
    NONE,
    ABOVE,
    BELOW,
    BOTH
}

/**
 * Parsed ESC/POS document elements.
 */
sealed class EscPosElement {
    data class Text(
        val text: String,
        val style: TextStyle,
        val alignment: PrintAlignment,
        val lineSpacingDots: Int = 30
    ) : EscPosElement()

    data class FeedLines(val count: Int) : EscPosElement()

    data class FeedDots(val dots: Int) : EscPosElement()

    data class Cut(
        val isPartial: Boolean = false,
        val feedLinesBefore: Int = 0
    ) : EscPosElement()

    data class Barcode(
        val type: BarcodeType,
        val content: String,
        val heightDots: Int = 64,
        val widthRatio: Int = 2,
        val hri: HriPosition = HriPosition.BELOW,
        val alignment: PrintAlignment = PrintAlignment.CENTER
    ) : EscPosElement()

    data class QrCode(
        val content: String,
        val moduleSize: Int = 4,
        val errorCorrection: Char = 'M',
        val alignment: PrintAlignment = PrintAlignment.CENTER
    ) : EscPosElement()

    data class RasterImage(
        val bitmap: Bitmap,
        val width: Int,
        val height: Int,
        val alignment: PrintAlignment = PrintAlignment.CENTER
    ) : EscPosElement()

    data class HorizontalDivider(
        val character: Char = '-',
        val doubleLine: Boolean = false
    ) : EscPosElement()

    data class DrawerKick(val pin: Int, val onTimeMs: Int) : EscPosElement()

    data class SoundBuzzer(val count: Int, val durationMs: Int) : EscPosElement()

    data class CommandLog(
        val offset: Int,
        val length: Int,
        val hex: String,
        val mnemonic: String,
        val description: String
    ) : EscPosElement()
}

/**
 * Structured document representing a complete or continuous print job.
 */
data class ParsedReceipt(
    val elements: List<EscPosElement>,
    val commandLogs: List<EscPosElement.CommandLog>,
    val totalBytes: Int,
    val cutCount: Int,
    val rawBytes: ByteArray,
    val title: String,
    val clientAddress: String = "127.0.0.1",
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ParsedReceipt
        return rawBytes.contentEquals(other.rawBytes) && timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = rawBytes.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
