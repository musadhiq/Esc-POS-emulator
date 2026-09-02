package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PrintJobEntity
import com.example.data.repository.PrintJobRepository
import com.example.escpos.EscPosElement
import com.example.escpos.EscPosParser
import com.example.escpos.ParsedReceipt
import com.example.escpos.SampleReceipts
import com.example.server.NetworkHelper
import com.example.server.ServerStatus
import com.example.service.PrinterServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrintJobRepository.getInstance(application)

    // Persistent Jobs History
    val historyJobs: StateFlow<List<PrintJobEntity>> = repository.allJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Receipt on Screen
    private val _activeReceipt = MutableStateFlow<ParsedReceipt>(
        EscPosParser(576).parse(SampleReceipts.generateRetailReceipt(), "Retail Supermarket (Sample)")
    )
    val activeReceipt: StateFlow<ParsedReceipt> = _activeReceipt.asStateFlow()

    // Paper Width Settings (80mm = 576 dots, 58mm = 384 dots)
    private val _paperWidthMm = MutableStateFlow(80)
    val paperWidthMm: StateFlow<Int> = _paperWidthMm.asStateFlow()

    // Navigation Tab (0: Live Preview, 1: Virtual Terminal, 2: Server & USB Dashboard, 3: History)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Hex / AST Inspector Dialog
    private val _showInspector = MutableStateFlow(false)
    val showInspector: StateFlow<Boolean> = _showInspector.asStateFlow()

    // Live Server Status
    private val _serverStatus = MutableStateFlow(ServerStatus(isRunning = true))
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    // Device IP
    private val _ipAddress = MutableStateFlow(NetworkHelper.getLocalIpAddress(application))
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    // Cash Drawer Simulated State
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    init {
        // Automatically start the foreground printer server
        PrinterServerService.startService(application)

        // Observe real-time server status from the active service
        viewModelScope.launch {
            while (true) {
                val s = PrinterServerService.activeTcpServer?.status?.value
                if (s != null) {
                    _serverStatus.value = s
                }
                _ipAddress.value = NetworkHelper.getLocalIpAddress(getApplication())
                delay(1000)
            }
        }

        // Listen for new print jobs arriving over TCP or HTTP
        viewModelScope.launch {
            repository.newPrintEvent.collect { newJob ->
                val parser = EscPosParser(if (_paperWidthMm.value == 58) 384 else 576)
                val parsed = parser.parse(newJob.rawBytes, newJob.clientInfo)
                _activeReceipt.value = parsed
                _selectedTab.value = 0 // Switch to Live Preview tab

                // Play tactile feedback
                triggerPrintFeedback(parsed)
            }
        }
    }

    fun setPaperWidth(widthMm: Int) {
        _paperWidthMm.value = widthMm
        // Re-parse current receipt with new dot width
        val currentBytes = _activeReceipt.value.rawBytes
        if (currentBytes.isNotEmpty()) {
            val parser = EscPosParser(if (widthMm == 58) 384 else 576)
            _activeReceipt.value = parser.parse(currentBytes, _activeReceipt.value.title)
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setShowInspector(show: Boolean) {
        _showInspector.value = show
    }

    fun loadReceipt(job: PrintJobEntity) {
        val parser = EscPosParser(if (_paperWidthMm.value == 58) 384 else 576)
        _activeReceipt.value = parser.parse(job.rawBytes, job.title)
        _selectedTab.value = 0
    }

    fun sendPayload(bytes: ByteArray, title: String, source: String = "Virtual Terminal") {
        viewModelScope.launch {
            val savedJob = repository.savePrintJob(
                rawBytes = bytes,
                clientInfo = "Local Injection",
                source = source,
                paperWidthMm = _paperWidthMm.value
            )
            loadReceipt(savedJob)
        }
    }

    fun toggleFavorite(job: PrintJobEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(job)
        }
    }

    fun deleteJob(id: Long) {
        viewModelScope.launch {
            repository.deleteJob(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun toggleServer(start: Boolean, tcpPort: Int, httpPort: Int) {
        if (start) {
            PrinterServerService.startService(getApplication(), tcpPort, httpPort)
        } else {
            PrinterServerService.stopService(getApplication())
        }
    }

    fun triggerDrawerPulse() {
        viewModelScope.launch {
            _isDrawerOpen.value = true
            playDrawerSound()
            delay(3000)
            _isDrawerOpen.value = false
        }
    }

    fun playBuzzer() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
        } catch (ignored: Exception) {}
    }

    private fun playDrawerSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        } catch (ignored: Exception) {}
    }

    private fun triggerPrintFeedback(receipt: ParsedReceipt) {
        // Check if receipt contains drawer kick or buzzer
        val hasDrawer = receipt.elements.any { it is EscPosElement.DrawerKick }
        val hasBuzzer = receipt.elements.any { it is EscPosElement.SoundBuzzer }

        if (hasDrawer) {
            triggerDrawerPulse()
        }
        if (hasBuzzer) {
            playBuzzer()
        }

        // Haptic feedback
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(50)
            }
        } catch (ignored: Exception) {}
    }
}
