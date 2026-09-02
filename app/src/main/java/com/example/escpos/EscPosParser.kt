package com.example.escpos

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.charset.Charset

/**
 * High-fidelity ESC/POS Parser & Interpreter.
 * Decodes raw binary ESC/POS streams into structured layout elements and command logs.
 */
class EscPosParser(
    private val paperWidthDots: Int = 576 // 80mm default (576 dots), 58mm is 384 dots
) {

    private var currentAlign = PrintAlignment.LEFT
    private var currentStyle = TextStyle()
    private var currentLineSpacingDots = 30
    private var barcodeHeight = 64
    private var barcodeWidth = 2
    private var barcodeHri = HriPosition.BELOW
    private var qrModel = 2
    private var qrModuleSize = 4
    private var qrErrorCorrection = 'M'
    private var qrDataBuffer = ""
    private var currentCharset = Charsets.UTF_8

    fun parse(bytes: ByteArray, clientInfo: String = "127.0.0.1"): ParsedReceipt {
        val elements = mutableListOf<EscPosElement>()
        val commandLogs = mutableListOf<EscPosElement.CommandLog>()
        val lineBuffer = StringBuilder()
        var cuts = 0
        var detectedTitle = ""

        fun flushTextLine() {
            if (lineBuffer.isNotEmpty()) {
                val lineText = lineBuffer.toString()
                if (detectedTitle.isEmpty() && lineText.trim().isNotEmpty()) {
                    detectedTitle = lineText.trim().take(36)
                }

                // Check if the line is purely divider dashes/equals
                val trimmed = lineText.trim()
                if (trimmed.length >= 10 && trimmed.all { it == '-' || it == '=' || it == '*' || it == '_' }) {
                    elements.add(
                        EscPosElement.HorizontalDivider(
                            character = trimmed.first(),
                            doubleLine = trimmed.first() == '='
                        )
                    )
                } else {
                    elements.add(
                        EscPosElement.Text(
                            text = lineText,
                            style = currentStyle,
                            alignment = currentAlign,
                            lineSpacingDots = currentLineSpacingDots
                        )
                    )
                }
                lineBuffer.clear()
            }
        }

        var i = 0
        val len = bytes.size

        while (i < len) {
            val b = bytes[i].toInt() and 0xFF
            val startOffset = i

            when (b) {
                // LF (Line Feed - 0x0A)
                0x0A -> {
                    flushTextLine()
                    commandLogs.add(
                        EscPosElement.CommandLog(
                            startOffset, 1, "0A", "LF", "Print line and feed paper"
                        )
                    )
                    i++
                }

                // CR (Carriage Return - 0x0D)
                0x0D -> {
                    // In ESC/POS, CR followed by LF is treated as one line break
                    if (i + 1 < len && bytes[i + 1].toInt() and 0xFF == 0x0A) {
                        flushTextLine()
                        commandLogs.add(
                            EscPosElement.CommandLog(
                                startOffset, 2, "0D 0A", "CR LF", "Carriage Return and Line Feed"
                            )
                        )
                        i += 2
                    } else {
                        flushTextLine()
                        commandLogs.add(
                            EscPosElement.CommandLog(
                                startOffset, 1, "0D", "CR", "Carriage Return"
                            )
                        )
                        i++
                    }
                }

                // FF (Form Feed - 0x0C)
                0x0C -> {
                    flushTextLine()
                    elements.add(EscPosElement.FeedLines(2))
                    commandLogs.add(
                        EscPosElement.CommandLog(startOffset, 1, "0C", "FF", "Form feed / Print & return")
                    )
                    i++
                }

                // HT (Horizontal Tab - 0x09)
                0x09 -> {
                    lineBuffer.append("    ") // 4 spaces tab
                    commandLogs.add(
                        EscPosElement.CommandLog(startOffset, 1, "09", "HT", "Horizontal Tab")
                    )
                    i++
                }

                // BEL / Beeper (0x07)
                0x07 -> {
                    elements.add(EscPosElement.SoundBuzzer(1, 100))
                    commandLogs.add(
                        EscPosElement.CommandLog(startOffset, 1, "07", "BEL", "Buzzer alarm")
                    )
                    i++
                }

                // ESC (0x1B) Commands
                0x1B -> {
                    if (i + 1 >= len) {
                        i++
                        continue
                    }
                    val escCmd = bytes[i + 1].toInt() and 0xFF

                    when (escCmd) {
                        // ESC @ (Initialize - 0x1B 0x40)
                        0x40 -> {
                            flushTextLine()
                            currentAlign = PrintAlignment.LEFT
                            currentStyle = TextStyle()
                            currentLineSpacingDots = 30
                            commandLogs.add(
                                EscPosElement.CommandLog(startOffset, 2, "1B 40", "ESC @", "Initialize printer defaults")
                            )
                            i += 2
                        }

                        // ESC a n (Select justification - 0x1B 0x61 n)
                        0x61 -> {
                            if (i + 2 < len) {
                                flushTextLine()
                                val n = bytes[i + 2].toInt() and 0xFF
                                currentAlign = when (n) {
                                    1, 49, '1'.code -> PrintAlignment.CENTER
                                    2, 50, '2'.code -> PrintAlignment.RIGHT
                                    else -> PrintAlignment.LEFT
                                }
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 61 ${toHex(n)}",
                                        "ESC a $n",
                                        "Set text alignment to $currentAlign"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC E n (Bold on/off - 0x1B 0x45 n)
                        0x45 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                val bold = (n and 1) == 1 || n == 49
                                currentStyle = currentStyle.copy(bold = bold)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 45 ${toHex(n)}",
                                        "ESC E $n",
                                        "Turn bold ${if (bold) "ON" else "OFF"}"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC G n (Double-strike - 0x1B 0x47 n)
                        0x47 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                val ds = (n and 1) == 1
                                currentStyle = currentStyle.copy(doubleStrike = ds)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 47 ${toHex(n)}",
                                        "ESC G $n",
                                        "Double strike ${if (ds) "ON" else "OFF"}"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC - n (Underline - 0x1B 0x2D n)
                        0x2D -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                val underline = when (n) {
                                    1, 49 -> 1
                                    2, 50 -> 2
                                    else -> 0
                                }
                                currentStyle = currentStyle.copy(underline = underline)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 2D ${toHex(n)}",
                                        "ESC - $n",
                                        "Underline mode: $underline"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC M n (Select font - 0x1B 0x4D n)
                        0x4D -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                val font = when (n) {
                                    1, 49 -> PrintFont.FONT_B
                                    2, 50 -> PrintFont.FONT_C
                                    else -> PrintFont.FONT_A
                                }
                                currentStyle = currentStyle.copy(font = font)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 4D ${toHex(n)}",
                                        "ESC M $n",
                                        "Select $font"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC ! n (Master print mode - 0x1B 0x21 n)
                        0x21 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                val font = if ((n and 0x01) != 0) PrintFont.FONT_B else PrintFont.FONT_A
                                val bold = (n and 0x08) != 0
                                val doubleHeight = (n and 0x10) != 0
                                val doubleWidth = (n and 0x20) != 0
                                val underline = if ((n and 0x80) != 0) 1 else 0

                                currentStyle = currentStyle.copy(
                                    font = font,
                                    bold = bold,
                                    widthScale = if (doubleWidth) 2 else 1,
                                    heightScale = if (doubleHeight) 2 else 1,
                                    underline = underline
                                )
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 21 ${toHex(n)}",
                                        "ESC ! $n",
                                        "Master print mode (Font: $font, Bold: $bold, 2xW: $doubleWidth, 2xH: $doubleHeight)"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC 2 (Default line spacing 1/6 inch ~ 30 dots)
                        0x32 -> {
                            currentLineSpacingDots = 30
                            commandLogs.add(
                                EscPosElement.CommandLog(startOffset, 2, "1B 32", "ESC 2", "Set default line spacing (30 dots)")
                            )
                            i += 2
                        }

                        // ESC 3 n (Set line spacing to n dots - 0x1B 0x33 n)
                        0x33 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                currentLineSpacingDots = n.coerceIn(0, 255)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 33 ${toHex(n)}",
                                        "ESC 3 $n",
                                        "Set custom line spacing to $n dots"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC d n (Print and feed n lines - 0x1B 0x64 n)
                        0x64 -> {
                            if (i + 2 < len) {
                                flushTextLine()
                                val n = (bytes[i + 2].toInt() and 0xFF).coerceIn(1, 50)
                                elements.add(EscPosElement.FeedLines(n))
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 64 ${toHex(n)}",
                                        "ESC d $n",
                                        "Feed $n lines"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC J n (Print and feed paper n dots - 0x1B 0x4A n)
                        0x4A -> {
                            if (i + 2 < len) {
                                flushTextLine()
                                val n = bytes[i + 2].toInt() and 0xFF
                                elements.add(EscPosElement.FeedDots(n))
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 4A ${toHex(n)}",
                                        "ESC J $n",
                                        "Feed $n dots"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC p m t1 t2 (Generate pulse to cash drawer - 0x1B 0x70 m t1 t2)
                        0x70 -> {
                            if (i + 4 < len) {
                                val m = bytes[i + 2].toInt() and 0xFF
                                val t1 = bytes[i + 3].toInt() and 0xFF
                                val t2 = bytes[i + 4].toInt() and 0xFF
                                val onMs = t1 * 2
                                elements.add(EscPosElement.DrawerKick(m, onMs))
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 5,
                                        "1B 70 ${toHex(m)} ${toHex(t1)} ${toHex(t2)}",
                                        "ESC p $m $t1 $t2",
                                        "Pulse cash drawer pin $m for ${onMs}ms"
                                    )
                                )
                                i += 5
                            } else i += 2
                        }

                        // ESC B n t (Sound buzzer / bell - 0x1B 0x42 n t)
                        0x42 -> {
                            if (i + 3 < len) {
                                val count = bytes[i + 2].toInt() and 0xFF
                                val time = bytes[i + 3].toInt() and 0xFF
                                elements.add(EscPosElement.SoundBuzzer(count, time * 50))
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 4,
                                        "1B 42 ${toHex(count)} ${toHex(time)}",
                                        "ESC B $count $time",
                                        "Sound buzzer $count times"
                                    )
                                )
                                i += 4
                            } else i += 2
                        }

                        // ESC t n (Select code page - 0x1B 0x74 n)
                        0x74 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                currentCharset = when (n) {
                                    0 -> Charset.forName("CP437")
                                    2 -> Charset.forName("CP850")
                                    3 -> Charset.forName("CP860")
                                    4 -> Charset.forName("CP863")
                                    5 -> Charset.forName("CP865")
                                    16 -> Charset.forName("windows-1252")
                                    17 -> Charset.forName("CP866")
                                    18 -> Charset.forName("CP852")
                                    19 -> Charset.forName("CP858")
                                    255 -> Charsets.UTF_8
                                    else -> Charsets.UTF_8
                                }
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1B 74 ${toHex(n)}",
                                        "ESC t $n",
                                        "Select character code table $n (${currentCharset.name()})"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // ESC * m nL nH d1..dk (Column bit image)
                        0x2A -> {
                            if (i + 4 < len) {
                                flushTextLine()
                                val m = bytes[i + 2].toInt() and 0xFF
                                val nL = bytes[i + 3].toInt() and 0xFF
                                val nH = bytes[i + 4].toInt() and 0xFF
                                val width = nL + (nH shl 8)
                                val dotsPerColumn = if (m == 0 || m == 1) 8 else 24
                                val bytesPerColumn = dotsPerColumn / 8
                                val dataLength = width * bytesPerColumn

                                if (i + 5 + dataLength <= len) {
                                    val bitmap = decodeColumnBitImage(bytes, i + 5, width, dotsPerColumn)
                                    if (bitmap != null) {
                                        elements.add(
                                            EscPosElement.RasterImage(
                                                bitmap = bitmap,
                                                width = bitmap.width,
                                                height = bitmap.height,
                                                alignment = currentAlign
                                            )
                                        )
                                    }
                                    commandLogs.add(
                                        EscPosElement.CommandLog(
                                            startOffset, 5 + dataLength,
                                            "1B 2A ${toHex(m)} ${toHex(nL)} ${toHex(nH)}...",
                                            "ESC * $m",
                                            "Column bit image: ${width}x${dotsPerColumn} dots"
                                        )
                                    )
                                    i += 5 + dataLength
                                } else {
                                    i = len
                                }
                            } else i += 2
                        }

                        // ESC i / ESC m (Partial / Full Cut on some POS printers)
                        0x69, 0x6D -> {
                            flushTextLine()
                            cuts++
                            elements.add(EscPosElement.Cut(isPartial = (escCmd == 0x69)))
                            commandLogs.add(
                                EscPosElement.CommandLog(
                                    startOffset, 2,
                                    "1B ${toHex(escCmd)}",
                                    "ESC ${if (escCmd == 0x69) "i" else "m"}",
                                    "Paper cut (${if (escCmd == 0x69) "Partial" else "Full"})"
                                )
                            )
                            i += 2
                        }

                        else -> {
                            // Unhandled ESC command
                            commandLogs.add(
                                EscPosElement.CommandLog(
                                    startOffset, 2,
                                    "1B ${toHex(escCmd)}",
                                    "ESC ${toHex(escCmd)}",
                                    "Generic ESC command"
                                )
                            )
                            i += 2
                        }
                    }
                }

                // GS (0x1D) Commands
                0x1D -> {
                    if (i + 1 >= len) {
                        i++
                        continue
                    }
                    val gsCmd = bytes[i + 1].toInt() and 0xFF

                    when (gsCmd) {
                        // GS ! n (Select character size - 0x1D 0x21 n)
                        0x21 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                val heightMultiplier = (n and 0x0F) + 1
                                val widthMultiplier = ((n shr 4) and 0x0F) + 1
                                currentStyle = currentStyle.copy(
                                    widthScale = widthMultiplier.coerceIn(1, 8),
                                    heightScale = heightMultiplier.coerceIn(1, 8)
                                )
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1D 21 ${toHex(n)}",
                                        "GS ! $n",
                                        "Set character scale: ${widthMultiplier}x Width, ${heightMultiplier}x Height"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // GS B n (Reverse / Negative white on black print - 0x1D 0x42 n)
                        0x42 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                val inverted = (n and 1) == 1 || n == 49
                                currentStyle = currentStyle.copy(inverted = inverted)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1D 42 ${toHex(n)}",
                                        "GS B $n",
                                        "White/Black inverted mode ${if (inverted) "ON" else "OFF"}"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // GS V m [n] (Cut paper - 0x1D 0x56 m [n])
                        0x56 -> {
                            flushTextLine()
                            cuts++
                            if (i + 2 < len) {
                                val m = bytes[i + 2].toInt() and 0xFF
                                var feedLines = 0
                                var consumed = 3

                                val isPartial = (m == 1 || m == 49 || m == 66 || m == 'B'.code)
                                if (m >= 65 && i + 3 < len) {
                                    feedLines = bytes[i + 3].toInt() and 0xFF
                                    consumed = 4
                                }
                                elements.add(EscPosElement.Cut(isPartial = isPartial, feedLinesBefore = feedLines))
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, consumed,
                                        "1D 56 ${toHex(m)}",
                                        "GS V $m",
                                        "Cut paper (${if (isPartial) "Partial" else "Full"}, Feed: $feedLines)"
                                    )
                                )
                                i += consumed
                            } else {
                                elements.add(EscPosElement.Cut(isPartial = false))
                                i += 2
                            }
                        }

                        // GS h n (Barcode height - 0x1D 0x68 n)
                        0x68 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                barcodeHeight = n.coerceIn(1, 255)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1D 68 ${toHex(n)}",
                                        "GS h $n",
                                        "Set barcode height to $n dots"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // GS w n (Barcode width module - 0x1D 0x77 n)
                        0x77 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                barcodeWidth = n.coerceIn(1, 6)
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1D 77 ${toHex(n)}",
                                        "GS w $n",
                                        "Set barcode module width ratio to $n"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // GS H n (Select HRI character position - 0x1D 0x48 n)
                        0x48 -> {
                            if (i + 2 < len) {
                                val n = bytes[i + 2].toInt() and 0xFF
                                barcodeHri = when (n) {
                                    1, 49 -> HriPosition.ABOVE
                                    2, 50 -> HriPosition.BELOW
                                    3, 51 -> HriPosition.BOTH
                                    else -> HriPosition.NONE
                                }
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, 3,
                                        "1D 48 ${toHex(n)}",
                                        "GS H $n",
                                        "Set barcode HRI position to $barcodeHri"
                                    )
                                )
                                i += 3
                            } else i += 2
                        }

                        // GS k m [n] d1..dk (Print Barcode - 0x1D 0x6B)
                        0x6B -> {
                            flushTextLine()
                            if (i + 2 < len) {
                                val m = bytes[i + 2].toInt() and 0xFF
                                val barcodeType = when (m) {
                                    0, 65 -> BarcodeType.UPC_A
                                    1, 66 -> BarcodeType.UPC_E
                                    2, 67 -> BarcodeType.JAN13_EAN13
                                    3, 68 -> BarcodeType.JAN8_EAN8
                                    4, 69 -> BarcodeType.CODE39
                                    5, 70 -> BarcodeType.ITF
                                    6, 71 -> BarcodeType.CODABAR
                                    72 -> BarcodeType.CODE93
                                    73 -> BarcodeType.CODE128
                                    else -> BarcodeType.CODE128
                                }

                                var barcodeData = ""
                                var consumed = 3

                                if (m >= 65) {
                                    // Format B: GS k m n d1..dn
                                    if (i + 3 < len) {
                                        val dataLen = bytes[i + 3].toInt() and 0xFF
                                        val dataEnd = (i + 4 + dataLen).coerceAtMost(len)
                                        val rawSlice = bytes.copyOfRange(i + 4, dataEnd)
                                        barcodeData = String(rawSlice, currentCharset)
                                        consumed = 4 + (dataEnd - (i + 4))
                                    }
                                } else {
                                    // Format A: GS k m d1..dk \0 (NUL terminated)
                                    val sbData = StringBuilder()
                                    var scanIdx = i + 3
                                    while (scanIdx < len && bytes[scanIdx].toInt() != 0) {
                                        sbData.append(bytes[scanIdx].toInt().toChar())
                                        scanIdx++
                                    }
                                    barcodeData = sbData.toString()
                                    consumed = (scanIdx - i) + 1
                                }

                                elements.add(
                                    EscPosElement.Barcode(
                                        type = barcodeType,
                                        content = barcodeData,
                                        heightDots = barcodeHeight,
                                        widthRatio = barcodeWidth,
                                        hri = barcodeHri,
                                        alignment = currentAlign
                                    )
                                )
                                commandLogs.add(
                                    EscPosElement.CommandLog(
                                        startOffset, consumed,
                                        "1D 6B ${toHex(m)} \"$barcodeData\"",
                                        "GS k $m",
                                        "Print $barcodeType barcode: \"$barcodeData\""
                                    )
                                )
                                i += consumed
                            } else i += 2
                        }

                        // GS ( k (2D Code commands - QR Code / PDF417)
                        0x28 -> {
                            if (i + 2 < len && bytes[i + 2].toInt() and 0xFF == 0x6B) { // 'k'
                                if (i + 5 < len) {
                                    val pL = bytes[i + 3].toInt() and 0xFF
                                    val pH = bytes[i + 4].toInt() and 0xFF
                                    val paramLen = pL + (pH shl 8)
                                    val cn = bytes[i + 5].toInt() and 0xFF // 49 for QR
                                    val fn = if (i + 6 < len) bytes[i + 6].toInt() and 0xFF else 0

                                    when (fn) {
                                        // fn 165 (0xA5): Select QR model (model 1 or 2)
                                        165, 0x41 -> {
                                            if (i + 7 < len) {
                                                qrModel = bytes[i + 7].toInt() and 0xFF
                                            }
                                        }
                                        // fn 167 (0xA7): Set QR module size (1 to 16 dots)
                                        167, 0x43 -> {
                                            if (i + 7 < len) {
                                                qrModuleSize = (bytes[i + 7].toInt() and 0xFF).coerceIn(2, 10)
                                            }
                                        }
                                        // fn 169 (0xA9): Set QR error correction level
                                        169, 0x45 -> {
                                            if (i + 7 < len) {
                                                val ec = bytes[i + 7].toInt() and 0xFF
                                                qrErrorCorrection = when (ec) {
                                                    48 -> 'L'
                                                    49 -> 'M'
                                                    50 -> 'Q'
                                                    51 -> 'H'
                                                    else -> 'M'
                                                }
                                            }
                                        }
                                        // fn 180 (0xB4): Store QR data
                                        180, 0x50 -> {
                                            val dataLen = paramLen - 3
                                            if (dataLen > 0 && i + 7 + dataLen <= len) {
                                                val rawSlice = bytes.copyOfRange(i + 7, i + 7 + dataLen)
                                                qrDataBuffer = String(rawSlice, currentCharset)
                                            }
                                        }
                                        // fn 181 (0xB5): Print QR code
                                        181, 0x51 -> {
                                            flushTextLine()
                                            elements.add(
                                                EscPosElement.QrCode(
                                                    content = qrDataBuffer.ifEmpty { "ESC/POS QR" },
                                                    moduleSize = qrModuleSize,
                                                    errorCorrection = qrErrorCorrection,
                                                    alignment = currentAlign
                                                )
                                            )
                                        }
                                    }

                                    val totalCmdLen = 5 + paramLen
                                    commandLogs.add(
                                        EscPosElement.CommandLog(
                                            startOffset, totalCmdLen.coerceAtMost(len - startOffset),
                                            "1D 28 6B ...",
                                            "GS ( k fn $fn",
                                            "QR Code function $fn (Param len: $paramLen)"
                                        )
                                    )
                                    i += totalCmdLen.coerceAtMost(len - startOffset)
                                } else i += 3
                            } else i += 2
                        }

                        // GS v 0 (Print raster bit image - 0x1D 0x76 0x30 m xL xH yL yH d1..dk)
                        0x76 -> {
                            if (i + 7 < len && (bytes[i + 2].toInt() and 0xFF == 0x30 || bytes[i + 2].toInt() and 0xFF == 0)) {
                                flushTextLine()
                                val m = bytes[i + 3].toInt() and 0xFF
                                val xL = bytes[i + 4].toInt() and 0xFF
                                val xH = bytes[i + 5].toInt() and 0xFF
                                val yL = bytes[i + 6].toInt() and 0xFF
                                val yH = bytes[i + 7].toInt() and 0xFF

                                val bytesWidth = xL + (xH shl 8)
                                val dotsHeight = yL + (yH shl 8)
                                val totalDataBytes = bytesWidth * dotsHeight

                                if (i + 8 + totalDataBytes <= len) {
                                    val bitmap = decodeRasterBitImage(bytes, i + 8, bytesWidth, dotsHeight)
                                    if (bitmap != null) {
                                        elements.add(
                                            EscPosElement.RasterImage(
                                                bitmap = bitmap,
                                                width = bitmap.width,
                                                height = bitmap.height,
                                                alignment = currentAlign
                                            )
                                        )
                                    }
                                    commandLogs.add(
                                        EscPosElement.CommandLog(
                                            startOffset, 8 + totalDataBytes,
                                            "1D 76 30 ${toHex(m)} ${toHex(xL)} ${toHex(xH)} ${toHex(yL)} ${toHex(yH)}...",
                                            "GS v 0",
                                            "Raster bit image (${bytesWidth * 8}x$dotsHeight dots)"
                                        )
                                    )
                                    i += 8 + totalDataBytes
                                } else {
                                    i = len
                                }
                            } else i += 2
                        }

                        // GS r n (Transmit status - 0x1D 0x72 n)
                        0x72 -> {
                            val n = if (i + 2 < len) bytes[i + 2].toInt() and 0xFF else 1
                            commandLogs.add(
                                EscPosElement.CommandLog(
                                    startOffset, 3,
                                    "1D 72 ${toHex(n)}",
                                    "GS r $n",
                                    "Transmit printer status request ($n)"
                                )
                            )
                            i += 3
                        }

                        else -> {
                            commandLogs.add(
                                EscPosElement.CommandLog(
                                    startOffset, 2,
                                    "1D ${toHex(gsCmd)}",
                                    "GS ${toHex(gsCmd)}",
                                    "Generic GS command"
                                )
                            )
                            i += 2
                        }
                    }
                }

                // DLE (0x10) - Real-time command: DLE EOT n (0x10 0x04 n)
                0x10 -> {
                    if (i + 2 < len && bytes[i + 1].toInt() and 0xFF == 0x04) {
                        val n = bytes[i + 2].toInt() and 0xFF
                        commandLogs.add(
                            EscPosElement.CommandLog(
                                startOffset, 3,
                                "10 04 ${toHex(n)}",
                                "DLE EOT $n",
                                "Real-time status request (Type $n)"
                            )
                        )
                        i += 3
                    } else i++
                }

                // Standard printable character
                else -> {
                    if (b in 32..255) {
                        // Decode character using current charset
                        val singleByte = byteArrayOf(bytes[i])
                        lineBuffer.append(String(singleByte, currentCharset))
                    }
                    i++
                }
            }
        }

        flushTextLine()

        return ParsedReceipt(
            elements = elements,
            commandLogs = commandLogs,
            totalBytes = len,
            cutCount = cuts,
            rawBytes = bytes,
            title = detectedTitle.ifEmpty { "Print Job (#${System.currentTimeMillis() % 10000})" },
            clientAddress = clientInfo
        )
    }

    /**
     * Decodes GS v 0 standard ESC/POS raster bit image format.
     */
    private fun decodeRasterBitImage(
        bytes: ByteArray,
        offset: Int,
        bytesWidth: Int,
        dotsHeight: Int
    ): Bitmap? {
        val width = bytesWidth * 8
        val height = dotsHeight
        if (width <= 0 || height <= 0 || width > 2048 || height > 4096) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        var src = offset
        for (y in 0 until height) {
            val rowStart = y * width
            for (bx in 0 until bytesWidth) {
                if (src >= bytes.size) break
                val byteVal = bytes[src++].toInt() and 0xFF
                for (bit in 0 until 8) {
                    val isBlack = ((byteVal shr (7 - bit)) and 1) == 1
                    val x = bx * 8 + bit
                    pixels[rowStart + x] = if (isBlack) Color.BLACK else Color.TRANSPARENT
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Decodes ESC * column bit image format.
     */
    private fun decodeColumnBitImage(
        bytes: ByteArray,
        offset: Int,
        width: Int,
        dotsHeight: Int
    ): Bitmap? {
        if (width <= 0 || dotsHeight <= 0) return null
        val bitmap = Bitmap.createBitmap(width, dotsHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * dotsHeight)
        val bytesPerColumn = dotsHeight / 8

        var src = offset
        for (x in 0 until width) {
            for (by in 0 until bytesPerColumn) {
                if (src >= bytes.size) break
                val byteVal = bytes[src++].toInt() and 0xFF
                for (bit in 0 until 8) {
                    val y = by * 8 + bit
                    val isBlack = ((byteVal shr (7 - bit)) and 1) == 1
                    if (y < dotsHeight) {
                        pixels[y * width + x] = if (isBlack) Color.BLACK else Color.TRANSPARENT
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, dotsHeight)
        return bitmap
    }

    private fun toHex(value: Int): String {
        return "%02X".format(value and 0xFF)
    }

    companion object {
        /**
         * Generates standard ESC/POS status byte for bidirectional query replies (e.g. DLE EOT, GS r).
         * Bits: Bit 3 = 0 (Online), Bit 5 = 0 (Paper present), Bit 6 = 0 (No error).
         */
        fun generateStatusResponse(queryType: Int, drawerOpen: Boolean = false): ByteArray {
            return when (queryType) {
                1 -> {
                    // Printer status: Bit 2 = drawer state (1=open, 0=closed), Bit 3 = 0 (online)
                    val status = (if (drawerOpen) 0x04 else 0x00) or 0x12
                    byteArrayOf(status.toByte())
                }
                2 -> {
                    // Offline cause: Cover closed, feed button not pressed, paper present
                    byteArrayOf(0x12)
                }
                3 -> {
                    // Error cause: No auto-cutter error, no unrecoverable error
                    byteArrayOf(0x12)
                }
                4 -> {
                    // Roll paper sensor: Paper present (bits 5,6 = 0)
                    byteArrayOf(0x12)
                }
                else -> byteArrayOf(0x12)
            }
        }
    }
}
