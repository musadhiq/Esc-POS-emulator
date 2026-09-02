package com.example.server

/**
 * Developer recipes and commands for seamlessly connecting PC printer drivers and POS systems to this emulator via USB or Network.
 */
object UsbDebuggingHelper {

    const val ADB_FORWARD_COMMAND = "adb forward tcp:9100 tcp:9100"
    const val ADB_REVERSE_COMMAND = "adb reverse tcp:9100 tcp:9100"

    val WINDOWS_SETUP_GUIDE = """
        1. Connect Android phone to PC via USB and enable USB Debugging.
        2. In Windows Command Prompt / PowerShell, run:
           adb forward tcp:9100 tcp:9100
        3. Go to Windows Settings > Bluetooth & Devices > Printers & Scanners > Add a printer.
        4. Select 'The printer that I want isn't listed' > 'Add a printer using TCP/IP address or hostname'.
        5. Enter Hostname or IP: 127.0.0.1 (Port will automatically map to 9100).
        6. Choose Driver: 'Generic / Text Only' or 'Generic ESC/POS Printer Driver'.
        7. Print test page! The preview will render instantly on your Android phone screen.
    """.trimIndent()

    val LINUX_CUPS_SETUP_GUIDE = """
        1. Forward port via ADB:
           adb forward tcp:9100 tcp:9100
        2. Add Raw / ESC-POS printer queue to CUPS:
           lpadmin -p VirtualPOS -E -v socket://127.0.0.1:9100 -m raw
        3. Send any ESC/POS binary file directly:
           lp -d VirtualPOS receipt.bin
    """.trimIndent()

    val NODEJS_EXAMPLE = """
        const net = require('net');
        const client = new net.Socket();

        client.connect(9100, '127.0.0.1', () => {
            console.log('Connected to Android ESC/POS Emulator');
            // ESC @ (Init) + Bold + Cut
            const data = Buffer.from([
                0x1B, 0x40,
                0x1B, 0x61, 0x01, // Center
                0x1D, 0x21, 0x11, // 2x W/H
                ...Buffer.from('HELLO FROM NODEJS\n'),
                0x1D, 0x21, 0x00, // Normal
                ...Buffer.from('Debugging POS via USB\n\n\n'),
                0x1D, 0x56, 0x00  // Cut
            ]);
            client.write(data, () => client.end());
        });
    """.trimIndent()

    val PYTHON_EXAMPLE = """
        import socket

        # Send print job via USB forwarded socket
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(('127.0.0.1', 9100))

        # ESC/POS raw bytes: Init + Align Center + Text + Feed 3 + Cut
        payload = b'\x1b\x40\x1b\x61\x01\x1d\x21\x11TEST RECEIPT\n\x1d\x21\x00\x1b\x64\x03\x1d\x56\x00'
        s.sendall(payload)
        s.close()
    """.trimIndent()

    val CURL_HTTP_EXAMPLE = """
        # Send receipt over HTTP POST (Port 9101)
        curl -X POST http://127.0.0.1:9101/print \
          -H "Content-Type: text/plain" \
          --data-binary $'Welcome to My Store\nTotal: $19.99\nThank you!\n'
    """.trimIndent()
}
