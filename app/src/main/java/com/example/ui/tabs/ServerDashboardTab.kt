package com.example.ui.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.server.ServerStatus
import com.example.server.UsbDebuggingHelper
import com.example.ui.theme.Amber500
import com.example.ui.theme.ConsoleAccent
import com.example.ui.theme.ConsoleBg
import com.example.ui.theme.ConsoleDim
import com.example.ui.theme.ConsoleText
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
import com.example.ui.theme.Rose500
import com.example.ui.theme.StatusReady

@Composable
fun ServerDashboardTab(
    serverStatus: ServerStatus,
    ipAddress: String,
    onToggleServer: (start: Boolean, tcpPort: Int, httpPort: Int) -> Unit,
    onTriggerDrawerPulse: () -> Unit,
    onPlayBuzzer: () -> Unit,
    isDrawerOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var tcpPortText by remember { mutableStateOf(serverStatus.port.toString()) }
    var httpPortText by remember { mutableStateOf(serverStatus.httpPort.toString()) }
    var guideTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Server Live Status Hero Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (serverStatus.isRunning) StatusReady else Rose500)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (serverStatus.isRunning) "SERVER ACTIVE" else "SERVER STOPPED",
                            color = if (serverStatus.isRunning) StatusReady else HighDensityTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Switch(
                        checked = serverStatus.isRunning,
                        onCheckedChange = { start ->
                            val tcp = tcpPortText.toIntOrNull() ?: 9100
                            val http = httpPortText.toIntOrNull() ?: 9101
                            onToggleServer(start, tcp, http)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PurplePrimary,
                            uncheckedTrackColor = HighDensityBorder
                        ),
                        modifier = Modifier.testTag("server_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = HighDensityBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Network Endpoints info
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EndpointInfoRow(
                        icon = Icons.Default.Wifi,
                        label = "LAN WiFi / IP Address",
                        value = ipAddress,
                        onCopy = { copyToClipboard(context, "IP Address", ipAddress) }
                    )

                    EndpointInfoRow(
                        icon = Icons.Default.Cable,
                        label = "Raw ESC/POS Port (TCP)",
                        value = "Port ${serverStatus.port}",
                        onCopy = { copyToClipboard(context, "TCP Port", serverStatus.port.toString()) }
                    )

                    EndpointInfoRow(
                        icon = Icons.Default.Sensors,
                        label = "HTTP REST Endpoint",
                        value = "http://$ipAddress:${serverStatus.httpPort}/print",
                        onCopy = {
                            copyToClipboard(
                                context,
                                "HTTP URL",
                                "http://$ipAddress:${serverStatus.httpPort}/print"
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(modifier = Modifier.weight(1f), label = "Active Sockets", value = "${serverStatus.activeConnections}")
                    MetricBox(modifier = Modifier.weight(1f), label = "Total Jobs", value = "${serverStatus.totalJobsReceived}")
                    MetricBox(modifier = Modifier.weight(1f), label = "Total Bytes", value = "${serverStatus.totalBytesReceived} B")
                }
            }
        }

        // USB & PC Driver Integration Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Usb, contentDescription = "USB", tint = PurplePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PC USB Driver & Client Setup",
                        color = HighDensityText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Forward PC port to Android emulator over standard USB cable:",
                    color = HighDensityTextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Copyable ADB forward command box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ConsoleBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = UsbDebuggingHelper.ADB_FORWARD_COMMAND,
                        color = ConsoleAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            copyToClipboard(context, "ADB Command", UsbDebuggingHelper.ADB_FORWARD_COMMAND)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ADB Command",
                            tint = ConsoleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Guide Tabs
                TabRow(
                    selectedTabIndex = guideTab,
                    containerColor = HighDensityBg,
                    contentColor = PurplePrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[guideTab]),
                            color = PurplePrimary
                        )
                    }
                ) {
                    Tab(
                        selected = guideTab == 0,
                        onClick = { guideTab = 0 },
                        text = {
                            Text(
                                "Windows",
                                fontSize = 12.sp,
                                color = if (guideTab == 0) PurplePrimary else HighDensityTextMuted,
                                fontWeight = if (guideTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = guideTab == 1,
                        onClick = { guideTab = 1 },
                        text = {
                            Text(
                                "Linux/CUPS",
                                fontSize = 12.sp,
                                color = if (guideTab == 1) PurplePrimary else HighDensityTextMuted,
                                fontWeight = if (guideTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = guideTab == 2,
                        onClick = { guideTab = 2 },
                        text = {
                            Text(
                                "Node.js",
                                fontSize = 12.sp,
                                color = if (guideTab == 2) PurplePrimary else HighDensityTextMuted,
                                fontWeight = if (guideTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = guideTab == 3,
                        onClick = { guideTab = 3 },
                        text = {
                            Text(
                                "Python",
                                fontSize = 12.sp,
                                color = if (guideTab == 3) PurplePrimary else HighDensityTextMuted,
                                fontWeight = if (guideTab == 3) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val guideContent = when (guideTab) {
                    0 -> UsbDebuggingHelper.WINDOWS_SETUP_GUIDE
                    1 -> UsbDebuggingHelper.LINUX_CUPS_SETUP_GUIDE
                    2 -> UsbDebuggingHelper.NODEJS_EXAMPLE
                    3 -> UsbDebuggingHelper.PYTHON_EXAMPLE
                    else -> UsbDebuggingHelper.CURL_HTTP_EXAMPLE
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ConsoleBg)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "COPY CODE",
                                color = ConsoleAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .testTag("copy_guide_snippet")
                            )
                        }
                        Text(
                            text = guideContent,
                            color = ConsoleText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Hardware Simulation: Cash Drawer & Buzzer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Simulated Hardware Peripherals",
                    color = HighDensityText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cash drawer button
                    Button(
                        onClick = onTriggerDrawerPulse,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDrawerOpen) Amber500 else PurpleContainer,
                            contentColor = if (isDrawerOpen) Color.Black else PurpleOnContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isDrawerOpen) "⚡ Drawer OPEN" else "\uD83D\uDCB5 Kick Drawer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    // Buzzer button
                    Button(
                        onClick = onPlayBuzzer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Buzzer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sound Buzzer", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        // Live Socket Activity Log
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Socket Activity Log (${serverStatus.logs.size})",
                    color = HighDensityText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ConsoleBg)
                        .padding(10.dp)
                ) {
                    if (serverStatus.logs.isEmpty()) {
                        Text(
                            text = "Waiting for incoming socket connections...",
                            color = ConsoleDim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(serverStatus.logs.reversed()) { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("Received")) StatusReady else if (log.contains("error")) Rose500 else ConsoleAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EndpointInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = PurplePrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = label, color = HighDensityTextSubtle, fontSize = 11.sp)
                Text(text = value, color = HighDensityText, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            }
        }

        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = HighDensityTextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PurpleLight)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = HighDensityTextSubtle, fontSize = 10.sp)
            Text(text = value, color = PurpleDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
}
