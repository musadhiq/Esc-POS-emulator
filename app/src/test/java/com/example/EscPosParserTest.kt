package com.example

import com.example.escpos.BarcodeType
import com.example.escpos.EscPosElement
import com.example.escpos.EscPosParser
import com.example.escpos.PrintAlignment
import com.example.escpos.SampleReceipts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class EscPosParserTest {

    private val parser = EscPosParser(paperDotWidth = 576)

    @Test
    fun testParseSimpleTextWithAlignmentAndBold() {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40)) // Init
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
        out.write("COFFEE SHOP\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // Partial cut

        val parsed = parser.parse(out.toByteArray(), "192.168.1.50:9100")

        assertEquals("COFFEE SHOP", parsed.title)
        assertEquals(1, parsed.cutCount)
        assertTrue(parsed.elements.any { it is EscPosElement.Text && it.text == "COFFEE SHOP\n" && it.alignment == PrintAlignment.CENTER && it.style.bold })
        assertTrue(parsed.elements.any { it is EscPosElement.Cut && it.isPartial })
    }

    @Test
    fun testParseCode128Barcode() {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40))
        val barcodeData = "INV-99201"
        out.write(byteArrayOf(0x1D, 0x68, 0x40)) // Height 64
        out.write(byteArrayOf(0x1D, 0x77, 0x02)) // Width 2
        out.write(byteArrayOf(0x1D, 0x6B, 0x49, barcodeData.length.toByte()))
        out.write(barcodeData.toByteArray(Charsets.US_ASCII))

        val parsed = parser.parse(out.toByteArray(), "USB-POS")

        val barcodeElem = parsed.elements.filterIsInstance<EscPosElement.Barcode>().firstOrNull()
        assertNotNull(barcodeElem)
        assertEquals(BarcodeType.CODE128, barcodeElem?.type)
        assertEquals("INV-99201", barcodeElem?.content)
    }

    @Test
    fun testParseQrCode() {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40))
        val qrPayload = "https://pay.example.com"
        val pL = ((qrPayload.length + 3) and 0xFF).toByte()
        val pH = (((qrPayload.length + 3) shr 8) and 0xFF).toByte()

        // Set Size
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06))
        // Store Data
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        out.write(qrPayload.toByteArray(Charsets.UTF_8))
        // Print QR
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

        val parsed = parser.parse(out.toByteArray(), "HTTP-Client")

        val qrElem = parsed.elements.filterIsInstance<EscPosElement.QrCode>().firstOrNull()
        assertNotNull(qrElem)
        assertEquals(qrPayload, qrElem?.content)
    }

    @Test
    fun testRealtimeStatusResponses() {
        val statusPaper = EscPosParser.generateStatusResponse(4)
        assertEquals(1, statusPaper.size)
        assertEquals(0x00.toByte(), statusPaper[0]) // Paper adequate & present
    }

    @Test
    fun testPresetReceiptsParsing() {
        val retail = parser.parse(SampleReceipts.generateRetailReceipt(), "Retail")
        assertTrue(retail.elements.isNotEmpty())
        assertEquals("METRO MARKET", retail.title)

        val restaurant = parser.parse(SampleReceipts.generateRestaurantBill(), "Restaurant")
        assertTrue(restaurant.elements.isNotEmpty())
        assertEquals("THE BISTRO TABLE", restaurant.title)

        val kitchen = parser.parse(SampleReceipts.generateKitchenTicket(), "Kitchen")
        assertTrue(kitchen.elements.isNotEmpty())

        val stress = parser.parse(SampleReceipts.generateStressTest(), "Diagnostics")
        assertTrue(stress.elements.isNotEmpty())
    }
}
