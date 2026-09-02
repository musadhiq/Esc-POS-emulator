package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.HexInspectorDialog
import com.example.ui.tabs.HistoryTab
import com.example.ui.tabs.ReceiptPreviewTab
import com.example.ui.tabs.ServerDashboardTab
import com.example.ui.tabs.VirtualTerminalTab
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensityText
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextSubtle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PurpleBorder
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleOnContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.Rose500
import com.example.ui.theme.StatusReady

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = false) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeReceipt by viewModel.activeReceipt.collectAsState()
    val paperWidthMm by viewModel.paperWidthMm.collectAsState()
    val showInspector by viewModel.showInspector.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()
    val historyJobs by viewModel.historyJobs.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HighDensityBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensityBg)
            ) {
                // High Density Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurplePrimary)
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Printer Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ESC/POS Link",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = HighDensityText,
                                lineHeight = 20.sp
                            )
                            Text(
                                text = if (serverStatus.isRunning) "EMULATOR ACTIVE" else "EMULATOR STANDBY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (serverStatus.isRunning) PurplePrimary else HighDensityTextSubtle,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Settings / Inspector Action Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, HighDensityBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { viewModel.setShowInspector(true) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Inspector",
                                tint = HighDensityTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // High Density Interface Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PurpleContainer)
                        .border(1.dp, PurpleBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "Interface",
                                tint = PurpleOnContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "USB & NETWORK INTERFACE",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleOnContainer,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (serverStatus.isRunning) "TCP:${serverStatus.port} • HTTP:${serverStatus.httpPort} • Attached" else "Server Offline • Connect via ADB / LAN",
                                    fontSize = 10.5.sp,
                                    color = HighDensityTextMuted
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (serverStatus.isRunning) StatusReady else Rose500)
                            )
                            Text(
                                text = if (serverStatus.isRunning) "READY" else "OFFLINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleOnContainer,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HighDensityBorder)
                )
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = HighDensityText
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Preview"
                            )
                        },
                        label = {
                            Text(
                                "Preview",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleOnContainer,
                            selectedTextColor = PurplePrimary,
                            indicatorColor = PurpleContainer,
                            unselectedIconColor = HighDensityTextSubtle,
                            unselectedTextColor = HighDensityTextSubtle
                        ),
                        modifier = Modifier.testTag("nav_tab_preview")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.DataObject,
                                contentDescription = "Console"
                            )
                        },
                        label = {
                            Text(
                                "Console",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleOnContainer,
                            selectedTextColor = PurplePrimary,
                            indicatorColor = PurpleContainer,
                            unselectedIconColor = HighDensityTextSubtle,
                            unselectedTextColor = HighDensityTextSubtle
                        ),
                        modifier = Modifier.testTag("nav_tab_terminal")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.setSelectedTab(2) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (serverStatus.activeConnections > 0) {
                                        Badge(containerColor = StatusReady) {
                                            Text("${serverStatus.activeConnections}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Endpoints"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Endpoints",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleOnContainer,
                            selectedTextColor = PurplePrimary,
                            indicatorColor = PurpleContainer,
                            unselectedIconColor = HighDensityTextSubtle,
                            unselectedTextColor = HighDensityTextSubtle
                        ),
                        modifier = Modifier.testTag("nav_tab_server")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.setSelectedTab(3) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (historyJobs.isNotEmpty()) {
                                        Badge(containerColor = PurpleContainer) {
                                            Text(
                                                "${historyJobs.size}",
                                                color = PurpleOnContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Logs"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Logs",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleOnContainer,
                            selectedTextColor = PurplePrimary,
                            indicatorColor = PurpleContainer,
                            unselectedIconColor = HighDensityTextSubtle,
                            unselectedTextColor = HighDensityTextSubtle
                        ),
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ReceiptPreviewTab(
                    receipt = activeReceipt,
                    paperWidthMm = paperWidthMm,
                    onPaperWidthChange = { viewModel.setPaperWidth(it) },
                    onOpenInspector = { viewModel.setShowInspector(true) }
                )

                1 -> VirtualTerminalTab(
                    onSendPayload = { bytes, title ->
                        viewModel.sendPayload(bytes, title)
                    }
                )

                2 -> ServerDashboardTab(
                    serverStatus = serverStatus,
                    ipAddress = ipAddress,
                    onToggleServer = { start, tcp, http ->
                        viewModel.toggleServer(start, tcp, http)
                    },
                    onTriggerDrawerPulse = { viewModel.triggerDrawerPulse() },
                    onPlayBuzzer = { viewModel.playBuzzer() },
                    isDrawerOpen = isDrawerOpen
                )

                3 -> HistoryTab(
                    jobs = historyJobs,
                    onSelectJob = { viewModel.loadReceipt(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDeleteJob = { viewModel.deleteJob(it) },
                    onClearAll = { viewModel.clearAllHistory() }
                )
            }
        }
    }

    if (showInspector) {
        HexInspectorDialog(
            receipt = activeReceipt,
            onDismiss = { viewModel.setShowInspector(false) }
        )
    }
}
