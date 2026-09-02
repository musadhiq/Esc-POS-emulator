package com.example.server

import android.util.Base64
import android.util.Log
import com.example.data.repository.PrintJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HttpPrintServer(
    private val repository: PrintJobRepository,
    private val scope: CoroutineScope
) {
    private val tag = "HttpPrintServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    fun start(port: Int = 9101) {
        if (_isRunning.value) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }

                _isRunning.value = true
                Log.d(tag, "HTTP Print Server running on port $port")

                while (isActive && serverSocket?.isClosed == false) {
                    try {
                        val client = serverSocket!!.accept()
                        handleHttpClient(client, port)
                    } catch (e: Exception) {
                        if (isActive && serverSocket?.isClosed == false) {
                            Log.e(tag, "HTTP accept error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to start HTTP server", e)
                _isRunning.value = false
            }
        }
    }

    private fun handleHttpClient(socket: Socket, port: Int) {
        scope.launch(Dispatchers.IO) {
            val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"
            try {
                socket.soTimeout = 5000
                val input = socket.getInputStream()
                val reader = BufferedReader(InputStreamReader(input))
                val output = socket.getOutputStream()

                val requestLine = reader.readLine() ?: return@launch
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@launch
                val method = parts[0].uppercase()
                val path = parts[1]

                // Read headers
                var contentLength = 0
                var isBase64 = false
                var isJson = false
                var line: String? = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    val lower = line.lowercase()
                    if (lower.startsWith("content-length:")) {
                        contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                    } else if (lower.startsWith("x-encoding:") && lower.contains("base64")) {
                        isBase64 = true
                    } else if (lower.startsWith("content-type:") && lower.contains("application/json")) {
                        isJson = true
                    }
                    line = reader.readLine()
                }

                // Handle CORS preflight (OPTIONS)
                if (method == "OPTIONS") {
                    sendCorsResponse(output)
                    return@launch
                }

                when {
                    path.startsWith("/print") && method == "POST" -> {
                        val bodyBytes = if (contentLength > 0) {
                            val buffer = ByteArray(contentLength)
                            var readTotal = 0
                            while (readTotal < contentLength) {
                                val count = input.read(buffer, readTotal, contentLength - readTotal)
                                if (count == -1) break
                                readTotal += count
                            }
                            buffer
                        } else {
                            ByteArray(0)
                        }

                        val rawPayload = if (isBase64) {
                            try {
                                Base64.decode(bodyBytes, Base64.DEFAULT)
                            } catch (e: Exception) {
                                bodyBytes
                            }
                        } else {
                            bodyBytes
                        }

                        if (rawPayload.isNotEmpty()) {
                            withContext(Dispatchers.Default) {
                                repository.savePrintJob(
                                    rawBytes = rawPayload,
                                    clientInfo = "$clientIp:${socket.port}",
                                    source = "HTTP ($port)"
                                )
                            }
                            sendJsonResponse(
                                output,
                                200,
                                """{"success":true,"message":"Receipt received and rendered","bytes":${rawPayload.size}}"""
                            )
                        } else {
                            sendJsonResponse(output, 400, """{"success":false,"error":"Empty body"}""")
                        }
                    }

                    path.startsWith("/status") -> {
                        sendJsonResponse(
                            output,
                            200,
                            """{"status":"ready","printer":"ESC/POS Emulator","paper":"present","cover":"closed","drawer":"closed"}"""
                        )
                    }

                    else -> {
                        sendJsonResponse(
                            output,
                            200,
                            """{"app":"ESC/POS Thermal Printer Emulator","endpoints":["POST /print (Send raw bytes or text)","GET /status (Check printer status)"]}"""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "HTTP error", e)
            } finally {
                try {
                    socket.close()
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun sendJsonResponse(out: OutputStream, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, X-Encoding\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    private fun sendCorsResponse(out: OutputStream) {
        val header = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, X-Encoding\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.flush()
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverJob?.cancel()
        serverSocket = null
        _isRunning.value = false
    }
}
