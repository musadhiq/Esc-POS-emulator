package com.example.ui.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escpos.ParsedReceipt
import com.example.ui.components.ThermalReceiptCanvas
import com.example.ui.theme.ConsoleAccent
import com.example.ui.theme.ConsoleBg
import com.example.ui.theme.ConsoleDim
import com.example.ui.theme.ConsoleHeaderBg
import com.example.ui.theme.ConsoleText
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensityText
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextSubtle
import com.example.ui.theme.PurpleBorder
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleOnContainer
import com.example.ui.theme.PurplePrimary

@Composable
fun ReceiptPreviewTab(
    receipt: ParsedReceipt,
    paperWidthMm: Int,
    onPaperWidthChange: (Int) -> Unit,
    onOpenInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Paper Preview Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Paper Preview Card Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PAPER PREVIEW",
                            color = HighDensityTextSubtle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HighDensityBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${paperWidthMm}mm Roll",
                                fontSize = 10.5.sp,
                                color = HighDensityTextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Width selection chips
                        FilterChip(
                            selected = paperWidthMm == 80,
                            onClick = { onPaperWidthChange(80) },
                            label = { Text("80mm", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurpleOnContainer,
                                containerColor = HighDensityBg,
                                labelColor = HighDensityTextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = paperWidthMm == 80,
                                borderColor = HighDensityBorder,
                                selectedBorderColor = PurpleBorder
                            ),
                            modifier = Modifier.testTag("paper_width_80mm")
                        )

                        FilterChip(
                            selected = paperWidthMm == 58,
                            onClick = { onPaperWidthChange(58) },
                            label = { Text("58mm", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainer,
                                selectedLabelColor = PurpleOnContainer,
                                containerColor = HighDensityBg,
                                labelColor = HighDensityTextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = paperWidthMm == 58,
                                borderColor = HighDensityBorder,
                                selectedBorderColor = PurpleBorder
                            ),
                            modifier = Modifier.testTag("paper_width_58mm")
                        )

                        IconButton(
                            onClick = {
                                val textContent = extractPlainSummary(receipt)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Receipt Content", textContent)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied receipt text", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = HighDensityTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HighDensityBorder)
                )

                // Scrollable Thermal Receipt Canvas Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA))
                        .verticalScroll(scrollState)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ThermalReceiptCanvas(
                        receipt = receipt,
                        paperWidthMm = paperWidthMm
                    )
                }
            }
        }

        // High Density Raw Data Stream (Hex) Console
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ConsoleBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Console Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ConsoleHeaderBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RAW DATA STREAM (HEX)",
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    TextButton(
                        onClick = onOpenInspector,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = "INSPECT",
                            color = ConsoleAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Console Monospace Body
                val hexLines = remember(receipt.rawBytes) {
                    formatRawStreamLines(receipt.rawBytes)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (hexLines.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(12.dp)
                                    .background(ConsoleAccent)
                            )
                            Text(
                                text = "Awaiting data stream from TCP/HTTP...",
                                color = ConsoleDim,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(hexLines) { line ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "[RX]",
                                        color = ConsoleAccent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = line,
                                        color = ConsoleText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatRawStreamLines(bytes: ByteArray): List<String> {
    if (bytes.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    val chunkSize = 8
    val maxBytes = bytes.size.coerceAtMost(128)
    for (i in 0 until maxBytes step chunkSize) {
        val end = (i + chunkSize).coerceAtMost(maxBytes)
        val chunk = bytes.copyOfRange(i, end)
        val hex = chunk.joinToString(" ") { "%02X".format(it) }
        lines.add(hex)
    }
    return lines
}

private fun extractPlainSummary(receipt: ParsedReceipt): String {
    val sb = StringBuilder()
    for (elem in receipt.elements) {
        if (elem is com.example.escpos.EscPosElement.Text) {
            sb.append(elem.text)
        }
    }
    return sb.toString()
}

