package com.example.server

import android.util.Log
import com.example.data.repository.PrintJobRepository
import com.example.escpos.EscPosParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ServerStatus(
    val isRunning: Boolean = false,
    val port: Int = 9100,
    val httpPort: Int = 9101,
    val activeConnections: Int = 0,
    val totalJobsReceived: Int = 0,
    val totalBytesReceived: Long = 0,
    val lastClientIp: String = "None",
    val lastActivityTime: Long = 0,
    val logs: List<String> = emptyList()
)

class TcpPrintServer(
    private val repository: PrintJobRepository,
    private val scope: CoroutineScope
) {
    private val tag = "TcpPrintServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _status = MutableStateFlow(ServerStatus())
    val status = _status.asStateFlow()

    fun start(port: Int = 9100) {
        if (_status.value.isRunning) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }

                addLog("TCP Raw Printer Server started on port $port")
                _status.value = _status.value.copy(
                    isRunning = true,
                    port = port
                )

                while (isActive && serverSocket?.isClosed == false) {
                    try {
                        val client = serverSocket!!.accept()
                        handleClient(client)
                    } catch (e: Exception) {
                        if (isActive && serverSocket?.isClosed == false) {
                            addLog("Connection accept error: ${e.localizedMessage}")
                        }
                    }
                }
            } catch (e: Exception) {
                addLog("Failed to bind port $port: ${e.localizedMessage}")
                Log.e(tag, "Server error", e)
                _status.value = _status.value.copy(isRunning = false)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"
            addLog("Client connected: $clientIp")
            _status.value = _status.value.copy(
                activeConnections = _status.value.activeConnections + 1,
                lastClientIp = clientIp,
                lastActivityTime = System.currentTimeMillis()
            )

            val buffer = ByteArrayOutputStream()
            val tempRead = ByteArray(4096)

            try {
                socket.soTimeout = 3000 // 3 sec idle timeout per read batch
                val inputStream: InputStream = socket.getInputStream()
                val outputStream: OutputStream = socket.getOutputStream()

                var lastByteTime = System.currentTimeMillis()

                while (isActive && !socket.isClosed) {
                    try {
                        val readBytes = inputStream.read(tempRead)
                        if (readBytes == -1) break

                        if (readBytes > 0) {
                            lastByteTime = System.currentTimeMillis()
                            buffer.write(tempRead, 0, readBytes)

                            // Check for real-time bidirectional status requests (DLE EOT, GS r)
                            checkAndReplyStatus(tempRead, readBytes, outputStream)
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Inactivity timeout - if data has been buffered, process it as a complete job
                        if (buffer.size() > 0 && (System.currentTimeMillis() - lastByteTime) >= 1500) {
                            break
                        }
                    }
                }

                val payload = buffer.toByteArray()
                if (payload.isNotEmpty()) {
                    addLog("Received print job (${payload.size} bytes) from $clientIp")
                    withContext(Dispatchers.Default) {
                        repository.savePrintJob(
                            rawBytes = payload,
                            clientInfo = "$clientIp:${socket.port}",
                            source = "TCP (${_status.value.port})"
                        )
                    }

                    _status.value = _status.value.copy(
                        totalJobsReceived = _status.value.totalJobsReceived + 1,
                        totalBytesReceived = _status.value.totalBytesReceived + payload.size,
                        lastActivityTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                addLog("Client error ($clientIp): ${e.localizedMessage}")
            } finally {
                try {
                    socket.close()
                } catch (ignored: Exception) {}

                _status.value = _status.value.copy(
                    activeConnections = (_status.value.activeConnections - 1).coerceAtLeast(0)
                )
                addLog("Client disconnected: $clientIp")
            }
        }
    }

    private fun checkAndReplyStatus(bytes: ByteArray, len: Int, out: OutputStream) {
        var idx = 0
        while (idx < len) {
            // DLE EOT n (0x10 0x04 n)
            if (bytes[idx].toInt() and 0xFF == 0x10 && idx + 2 < len && bytes[idx + 1].toInt() and 0xFF == 0x04) {
                val queryType = bytes[idx + 2].toInt() and 0xFF
                val response = EscPosParser.generateStatusResponse(queryType)
                try {
                    out.write(response)
                    out.flush()
                } catch (ignored: Exception) {}
                idx += 3
            } else if (bytes[idx].toInt() and 0xFF == 0x1D && idx + 2 < len && bytes[idx + 1].toInt() and 0xFF == 0x72) {
                // GS r n (0x1D 0x72 n)
                val response = byteArrayOf(0x00) // Paper roll OK status
                try {
                    out.write(response)
                    out.flush()
                } catch (ignored: Exception) {}
                idx += 3
            } else {
                idx++
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverJob?.cancel()
        serverSocket = null
        _status.value = _status.value.copy(isRunning = false, activeConnections = 0)
        addLog("TCP Raw Printer Server stopped")
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val formatted = "[$time] $message"
        val currentLogs = _status.value.logs.takeLast(49).toMutableList()
        currentLogs.add(formatted)
        _status.value = _status.value.copy(logs = currentLogs)
    }
}
