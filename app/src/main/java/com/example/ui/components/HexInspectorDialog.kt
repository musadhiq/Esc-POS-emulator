package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.escpos.ParsedReceipt
import com.example.ui.theme.ConsoleAccent
import com.example.ui.theme.ConsoleBg
import com.example.ui.theme.ConsoleText
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensityText
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextSubtle
import com.example.ui.theme.PurpleBorder
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurpleOnContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StatusReady

@Composable
fun HexInspectorDialog(
    receipt: ParsedReceipt,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("hex_inspector_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PurpleLight)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ESC/POS Stream Inspector",
                            color = HighDensityText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${receipt.totalBytes} bytes • ${receipt.commandLogs.size} commands • ${receipt.cutCount} cuts",
                            color = HighDensityTextSubtle,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val hexStr = formatHexDump(receipt.rawBytes)
                                copyToClipboard(context, "Hex Dump", hexStr)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Hex",
                                tint = PurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = HighDensityTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Tabs: Command AST Breakdown vs Raw Hex Dump
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = HighDensityBg,
                    contentColor = PurplePrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PurplePrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Command AST (${receipt.commandLogs.size})",
                                fontSize = 12.5.sp,
                                color = if (selectedTab == 0) PurplePrimary else HighDensityTextMuted,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Raw Hex Dump",
                                fontSize = 12.5.sp,
                                color = if (selectedTab == 1) PurplePrimary else HighDensityTextMuted,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                // Tab Content
                if (selectedTab == 0) {
                    CommandAstList(receipt = receipt)
                } else {
                    RawHexDumpView(bytes = receipt.rawBytes)
                }
            }
        }
    }
}

@Composable
private fun CommandAstList(receipt: ParsedReceipt) {
    if (receipt.commandLogs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No ESC/POS commands detected in payload.",
                color = HighDensityTextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(receipt.commandLogs) { index, cmd ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HighDensityBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PurpleContainer)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cmd.mnemonic,
                                        color = PurpleOnContainer,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offset: 0x${"%04X".format(cmd.offset)}",
                                    color = HighDensityTextSubtle,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = "${cmd.length}B",
                                color = StatusReady,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = cmd.description,
                            color = HighDensityText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(ConsoleBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cmd.hex,
                                color = ConsoleAccent,
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

@Composable
private fun RawHexDumpView(bytes: ByteArray) {
    val scrollState = rememberScrollState()
    val formattedHex = remember(bytes) { formatHexDump(bytes) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ConsoleBg)
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = formattedHex,
                    color = ConsoleText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.horizontalScroll(scrollState)
                )
            }
        }
    }
}

private fun formatHexDump(bytes: ByteArray): String {
    val sb = StringBuilder()
    val chunkSize = 16

    for (i in bytes.indices step chunkSize) {
        val chunkEnd = (i + chunkSize).coerceAtMost(bytes.size)
        val slice = bytes.copyOfRange(i, chunkEnd)

        // Offset
        sb.append("%04X  ".format(i))

        // Hex bytes (split in two 8-byte columns)
        for (j in 0 until chunkSize) {
            if (j < slice.size) {
                sb.append("%02X ".format(slice[j]))
            } else {
                sb.append("   ")
            }
            if (j == 7) sb.append(" ")
        }

        sb.append(" |")

        // ASCII representation
        for (b in slice) {
            val charCode = b.toInt() and 0xFF
            if (charCode in 32..126) {
                sb.append(charCode.toChar())
            } else {
                sb.append('.')
            }
        }
        sb.append("|\n")
    }

    return sb.toString()
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
