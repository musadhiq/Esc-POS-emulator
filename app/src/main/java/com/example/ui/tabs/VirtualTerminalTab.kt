package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escpos.SampleReceipts
import com.example.ui.theme.ConsoleAccent
import com.example.ui.theme.ConsoleBg
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensityText
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextSubtle
import com.example.ui.theme.PurpleBorder
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleDark
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurpleOnContainer
import com.example.ui.theme.PurplePrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VirtualTerminalTab(
    onSendPayload: (bytes: ByteArray, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var hexInput by remember {
        mutableStateOf("1B 40 1B 61 01 1D 21 11 44 45 4D 4F 20 54 45 53 54 0A 1D 21 00 1B 61 00 53 74 61 74 75 73 3A 20 53 75 63 63 65 73 73 0A 1B 64 03 1D 56 42 00")
    }
    var textInput by remember {
        mutableStateOf("Store #104 - City Center\nItem: Coffee Latte - $4.50\nPayment: Cash\nThank you!")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Preset Demo Receipts Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Presets",
                        tint = PurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "One-Click Test Presets",
                        color = HighDensityText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSendPayload(SampleReceipts.generateRetailReceipt(), "Retail Supermarket")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleLight,
                            contentColor = PurpleDark
                        ),
                        modifier = Modifier.testTag("preset_retail_button")
                    ) {
                        Text("\uD83D\uDED2 Retail Supermarket", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSendPayload(SampleReceipts.generateRestaurantBill(), "Restaurant Dine-In")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleLight,
                            contentColor = PurpleDark
                        ),
                        modifier = Modifier.testTag("preset_restaurant_button")
                    ) {
                        Text("\uD83C\uDF7D Restaurant Bill (QR)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSendPayload(SampleReceipts.generateKitchenTicket(), "Kitchen Ticket")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleLight,
                            contentColor = PurpleDark
                        ),
                        modifier = Modifier.testTag("preset_kitchen_button")
                    ) {
                        Text("\uD83C\uDF73 Kitchen Ticket", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSendPayload(SampleReceipts.generateStressTest(), "ESC/POS Diagnostics")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("preset_stress_button")
                    ) {
                        Text("⚡ ESC/POS Diagnostic", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Command Injector Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Command Builder",
                    color = HighDensityText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any snippet to append ESC/POS sequence into the injector:",
                    color = HighDensityTextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CommandSnippetChip("Init (1B 40)") { appendHex("1B 40 ") { hexInput = it } }
                    CommandSnippetChip("Center (1B 61 01)") { appendHex("1B 61 01 ") { hexInput = it } }
                    CommandSnippetChip("Bold ON (1B 45 01)") { appendHex("1B 45 01 ") { hexInput = it } }
                    CommandSnippetChip("Bold OFF (1B 45 00)") { appendHex("1B 45 00 ") { hexInput = it } }
                    CommandSnippetChip("2x Scale (1D 21 11)") { appendHex("1D 21 11 ") { hexInput = it } }
                    CommandSnippetChip("Invert ON (1D 42 01)") { appendHex("1D 42 01 ") { hexInput = it } }
                    CommandSnippetChip("Invert OFF (1D 42 00)") { appendHex("1D 42 00 ") { hexInput = it } }
                    CommandSnippetChip("Feed 3L (1B 64 03)") { appendHex("1B 64 03 ") { hexInput = it } }
                    CommandSnippetChip("Cut (1D 56 42 00)") { appendHex("1D 56 42 00 ") { hexInput = it } }
                    CommandSnippetChip("Drawer (1B 70 00 19 32)") { appendHex("1B 70 00 19 32 ") { hexInput = it } }
                    CommandSnippetChip("Buzzer (1B 42 02 04)") { appendHex("1B 42 02 04 ") { hexInput = it } }
                }
            }
        }

        // Raw Hex Injection Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Raw Hex Stream Injector",
                        color = HighDensityText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { hexInput = "" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityTextMuted)
                    ) {
                        Text("Clear", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("hex_payload_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ConsoleAccent,
                        unfocusedTextColor = ConsoleAccent,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = HighDensityBorder,
                        focusedContainerColor = ConsoleBg,
                        unfocusedContainerColor = ConsoleBg
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    placeholder = {
                        Text("1B 40 1B 61 01 ...", color = HighDensityTextSubtle, fontFamily = FontFamily.Monospace)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val parsedBytes = parseHexStringToBytes(hexInput)
                        if (parsedBytes.isNotEmpty()) {
                            onSendPayload(parsedBytes, "Hex Injection (${parsedBytes.size}B)")
                            Toast.makeText(context, "Executed ${parsedBytes.size} bytes", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid hex string", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_hex_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute Hex Stream", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Plain Text Quick Printer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick ASCII Text Printer",
                    color = HighDensityText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("text_payload_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighDensityText,
                        unfocusedTextColor = HighDensityText,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = HighDensityBorder,
                        focusedContainerColor = HighDensityBg,
                        unfocusedContainerColor = HighDensityBg
                    ),
                    placeholder = { Text("Enter receipt text...", color = HighDensityTextSubtle) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val header = byteArrayOf(0x1B, 0x40, 0x1B, 0x61, 0x01, 0x1D, 0x21, 0x11)
                            val title = "ASCII PRINT\n".toByteArray(Charsets.UTF_8)
                            val reset = byteArrayOf(0x1D, 0x21, 0x00, 0x1B, 0x61, 0x00)
                            val body = (textInput + "\n\n").toByteArray(Charsets.UTF_8)
                            val cut = byteArrayOf(0x1B, 0x64, 0x03, 0x1D, 0x56, 0x42, 0x00)

                            val combined = header + title + reset + body + cut
                            onSendPayload(combined, "ASCII Print")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleContainer,
                        contentColor = PurpleOnContainer
                    )
                ) {
                    Text("Print ASCII Text", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommandSnippetChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = PurpleContainer,
            labelColor = PurpleOnContainer
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = PurpleBorder
        )
    )
}

private fun appendHex(snippet: String, update: (String) -> Unit) {
    update(snippet)
}

private fun parseHexStringToBytes(hexString: String): ByteArray {
    val clean = hexString.replace("[^0-9A-Fa-f]".toRegex(), "")
    if (clean.isEmpty() || clean.length % 2 != 0) {
        val tokens = hexString.trim().split("\\s+".toRegex())
        val out = mutableListOf<Byte>()
        for (token in tokens) {
            val t = token.replace("0x", "").replace("0X", "")
            val parsed = t.toIntOrNull(16)
            if (parsed != null) {
                out.add(parsed.toByte())
            }
        }
        return out.toByteArray()
    }

    val result = ByteArray(clean.length / 2)
    for (i in result.indices) {
        val index = i * 2
        val byteVal = clean.substring(index, index + 2).toInt(16)
        result[i] = byteVal.toByte()
    }
    return result
}
