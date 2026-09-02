package com.example.escpos

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pre-configured ESC/POS sample payloads for one-click testing and development benchmarking.
 */
object SampleReceipts {

    fun generateRetailReceipt(): ByteArray {
        val out = ByteArrayOutputStream()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        // ESC @ (Init)
        out.write(byteArrayOf(0x1B, 0x40))

        // Center Align
        out.write(byteArrayOf(0x1B, 0x61, 0x01))

        // Double Height & Width Title
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        out.write("METRO MARKET\n".toByteArray(Charsets.UTF_8))

        // Reset Size & Font B
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x4D, 0x01))
        out.write("1042 Market Street, Suite 400\n".toByteArray(Charsets.UTF_8))
        out.write("San Francisco, CA 94103\n".toByteArray(Charsets.UTF_8))
        out.write("Tel: (415) 555-0199\n\n".toByteArray(Charsets.UTF_8))

        // Left Align, Font A
        out.write(byteArrayOf(0x1B, 0x4D, 0x00))
        out.write(byteArrayOf(0x1B, 0x61, 0x00))
        out.write("Date: $now\n".toByteArray(Charsets.UTF_8))
        out.write("Cashier: Alex M.       Register: #04\n".toByteArray(Charsets.UTF_8))
        out.write("Receipt: #MM-884920    Trans: Sale\n".toByteArray(Charsets.UTF_8))
        out.write("------------------------------------------------\n".toByteArray(Charsets.UTF_8))

        // Items Header
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
        out.write("ITEM                     QTY    PRICE     TOTAL\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off
        out.write("------------------------------------------------\n".toByteArray(Charsets.UTF_8))

        // Items
        out.write("Organic Almond Milk 1L    2     $3.49     $6.98\n".toByteArray(Charsets.UTF_8))
        out.write("Artisan Sourdough Loaf    1     $5.50     $5.50\n".toByteArray(Charsets.UTF_8))
        out.write("Avocado Hass (Bag of 4)   1     $4.99     $4.99\n".toByteArray(Charsets.UTF_8))
        out.write("Dark Roast Coffee 250g    1     $8.25     $8.25\n".toByteArray(Charsets.UTF_8))
        out.write("Greek Yogurt Plain 500g   2     $2.80     $5.60\n".toByteArray(Charsets.UTF_8))
        out.write("Fair Trade Bananas (kg)   1.4   $1.20     $1.68\n".toByteArray(Charsets.UTF_8))
        out.write("------------------------------------------------\n".toByteArray(Charsets.UTF_8))

        // Totals (Right Align)
        out.write(byteArrayOf(0x1B, 0x61, 0x02))
        out.write("Subtotal:  $33.00\n".toByteArray(Charsets.UTF_8))
        out.write("Tax (8.5%):   $2.81\n".toByteArray(Charsets.UTF_8))
        out.write("Savings / Member Discount:  -$3.00\n".toByteArray(Charsets.UTF_8))

        // Grand Total (Bold, Double Width & Height)
        out.write(byteArrayOf(0x1B, 0x45, 0x01))
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        out.write("TOTAL: $32.81\n".toByteArray(Charsets.UTF_8))

        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write("Payment: VISA Ending *4821\n".toByteArray(Charsets.UTF_8))
        out.write("Auth Code: 089124  Approved\n\n".toByteArray(Charsets.UTF_8))

        // Center Align for Barcode & Footer
        out.write(byteArrayOf(0x1B, 0x61, 0x01))
        out.write("Thank you for shopping with us!\n".toByteArray(Charsets.UTF_8))
        out.write("Return policy: 30 days with receipt.\n\n".toByteArray(Charsets.UTF_8))

        // Barcode (GS k CODE128)
        out.write(byteArrayOf(0x1D, 0x68, 0x48)) // Height 72 dots
        out.write(byteArrayOf(0x1D, 0x77, 0x02)) // Width 2
        out.write(byteArrayOf(0x1D, 0x48, 0x02)) // HRI below
        val barcodeStr = "MM8849207712"
        out.write(byteArrayOf(0x1D, 0x6B, 0x49, barcodeStr.length.toByte()))
        out.write(barcodeStr.toByteArray(Charsets.US_ASCII))

        // Feed & Cut
        out.write(byteArrayOf(0x1B, 0x64, 0x03)) // Feed 3 lines
        out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // Partial cut

        return out.toByteArray()
    }

    fun generateRestaurantBill(): ByteArray {
        val out = ByteArrayOutputStream()
        val now = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US).format(Date())

        out.write(byteArrayOf(0x1B, 0x40)) // Init

        // Center Header
        out.write(byteArrayOf(0x1B, 0x61, 0x01))
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        out.write("THE BISTRO TABLE\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write("Fine Dining & Artisan Cocktails\n".toByteArray(Charsets.UTF_8))
        out.write("www.thebistrotable.example\n\n".toByteArray(Charsets.UTF_8))

        // Left Alignment Table Info
        out.write(byteArrayOf(0x1B, 0x61, 0x00))
        out.write("TABLE: 14            GUESTS: 3\n".toByteArray(Charsets.UTF_8))
        out.write("SERVER: Chloe D.     TIME: $now\n".toByteArray(Charsets.UTF_8))
        out.write("================================================\n".toByteArray(Charsets.UTF_8))

        out.write("1x Truffle Fries (Parmesan, Aioli)        $12.00\n".toByteArray(Charsets.UTF_8))
        out.write("1x Burrata Salad (Heirloom Tomatoes)      $16.50\n".toByteArray(Charsets.UTF_8))
        out.write("1x Pan-Seared Salmon (Asparagus)          $29.00\n".toByteArray(Charsets.UTF_8))
        out.write("1x Ribeye Steak 12oz (Medium Rare)        $38.00\n".toByteArray(Charsets.UTF_8))
        out.write("1x Wild Mushroom Risotto                  $24.00\n".toByteArray(Charsets.UTF_8))
        out.write("2x Classic Old Fashioned                  $28.00\n".toByteArray(Charsets.UTF_8))
        out.write("1x Sparkling Mineral Water 750ml           $7.00\n".toByteArray(Charsets.UTF_8))
        out.write("------------------------------------------------\n".toByteArray(Charsets.UTF_8))

        // Subtotal & Tax
        out.write(byteArrayOf(0x1B, 0x61, 0x02))
        out.write("Food & Beverage:  $154.50\n".toByteArray(Charsets.UTF_8))
        out.write("City Tax (9.25%):   $14.29\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x45, 0x01))
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        out.write("BALANCE DUE: $168.79\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))

        out.write("\nSuggested Gratuity:\n".toByteArray(Charsets.UTF_8))
        out.write("18% = $27.81 | 20% = $30.90 | 22% = $33.99\n\n".toByteArray(Charsets.UTF_8))

        // QR Code for Fast Pay
        out.write(byteArrayOf(0x1B, 0x61, 0x01))
        out.write("Scan to Pay Table Bill:\n\n".toByteArray(Charsets.UTF_8))

        val qrPayload = "https://pay.bistro.example/bill/T14-9982"
        val pL = ((qrPayload.length + 3) and 0xFF).toByte()
        val pH = (((qrPayload.length + 3) shr 8) and 0xFF).toByte()

        // GS ( k Model 2
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        // Set Size 5
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x05))
        // Store Data
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        out.write(qrPayload.toByteArray(Charsets.UTF_8))
        // Print QR
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

        out.write("\n\nThank you for joining us!\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x64, 0x03))
        out.write(byteArrayOf(0x1D, 0x56, 0x00)) // Full Cut
        out.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0x32)) // Drawer Kick pulse
        return out.toByteArray()
    }

    fun generateKitchenTicket(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40)) // Init

        // Buzzer alert for kitchen
        out.write(byteArrayOf(0x1B, 0x42, 0x02, 0x04))

        // Center Order Header
        out.write(byteArrayOf(0x1B, 0x61, 0x01))
        out.write(byteArrayOf(0x1D, 0x42, 0x01)) // Reverse / Inverted White on Black
        out.write(" *** KITCHEN ORDER *** \n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x42, 0x00)) // Invert off

        out.write(byteArrayOf(0x1D, 0x21, 0x33)) // 4x Width, 4x Height
        out.write("\n#084\n\n".toByteArray(Charsets.UTF_8))

        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x61, 0x00))
        out.write("Table: 07          Server: Sam R.\n".toByteArray(Charsets.UTF_8))
        out.write("Type: DINE-IN      Time: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}\n".toByteArray(Charsets.UTF_8))
        out.write("================================================\n".toByteArray(Charsets.UTF_8))

        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
        out.write(byteArrayOf(0x1D, 0x21, 0x11)) // 2x
        out.write("[2] BACON CHEESEBURGER\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write("    >> NO ONIONS, EXTRA PICKLES\n".toByteArray(Charsets.UTF_8))
        out.write("    >> FRIES WELL DONE\n\n".toByteArray(Charsets.UTF_8))

        out.write(byteArrayOf(0x1B, 0x45, 0x01))
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        out.write("[1] SPICY CHICKEN WRAP\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write("    >> SAUCE ON SIDE\n".toByteArray(Charsets.UTF_8))
        out.write("    >> SWEET POTATO FRIES\n\n".toByteArray(Charsets.UTF_8))

        out.write(byteArrayOf(0x1B, 0x45, 0x01))
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        out.write("[1] CAESAR SALAD (LARGE)\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write("    >> ADD GRILLED CHICKEN\n".toByteArray(Charsets.UTF_8))
        out.write("================================================\n".toByteArray(Charsets.UTF_8))

        out.write(byteArrayOf(0x1B, 0x64, 0x04))
        out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00)) // Full Cut
        return out.toByteArray()
    }

    fun generateStressTest(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40)) // Init

        // Title
        out.write(byteArrayOf(0x1B, 0x61, 0x01))
        out.write(byteArrayOf(0x1B, 0x45, 0x01))
        out.write("ESC/POS COMPREHENSIVE TEST\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write("Printer Emulator Diagnostics\n".toByteArray(Charsets.UTF_8))
        out.write("================================================\n\n".toByteArray(Charsets.UTF_8))

        // Alignment Test
        out.write(byteArrayOf(0x1B, 0x61, 0x00))
        out.write("Left Aligned Text (ESC a 0)\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x61, 0x01))
        out.write("Center Aligned Text (ESC a 1)\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x61, 0x02))
        out.write("Right Aligned Text (ESC a 2)\n\n".toByteArray(Charsets.UTF_8))

        // Styles Test
        out.write(byteArrayOf(0x1B, 0x61, 0x00))
        out.write("Normal Font A (12x24 dots)\n".toByteArray(Charsets.UTF_8))

        out.write(byteArrayOf(0x1B, 0x4D, 0x01))
        out.write("Font B Condensed (9x17 dots)\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x4D, 0x00))

        out.write(byteArrayOf(0x1B, 0x45, 0x01))
        out.write("Emphasized / Bold Text (ESC E 1)\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))

        out.write(byteArrayOf(0x1B, 0x2D, 0x01))
        out.write("1-Dot Underline Text (ESC - 1)\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x2D, 0x02))
        out.write("2-Dot Heavy Underline (ESC - 2)\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x2D, 0x00))

        out.write(byteArrayOf(0x1D, 0x42, 0x01))
        out.write(" INVERTED WHITE-ON-BLACK (GS B 1) \n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x42, 0x00))

        // Scaling Test
        out.write("\nCharacter Scaling Multipliers:\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x10))
        out.write("2x Width\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x01))
        out.write("2x Height\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x11))
        out.write("2x Width & Height\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x22))
        out.write("3x W & H\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x21, 0x00))

        // Barcode Showcase
        out.write("\n--- Barcodes Benchmark ---\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x61, 0x01))

        // Code 128
        out.write(byteArrayOf(0x1D, 0x68, 0x38))
        out.write(byteArrayOf(0x1D, 0x77, 0x02))
        out.write(byteArrayOf(0x1D, 0x48, 0x02))
        val c128 = "ESC-POS-TEST-128"
        out.write(byteArrayOf(0x1D, 0x6B, 0x49, c128.length.toByte()))
        out.write(c128.toByteArray(Charsets.US_ASCII))
        out.write("\n\n".toByteArray(Charsets.UTF_8))

        // EAN 13
        val ean = "5901234123457"
        out.write(byteArrayOf(0x1D, 0x6B, 0x43, ean.length.toByte()))
        out.write(ean.toByteArray(Charsets.US_ASCII))
        out.write("\n\n".toByteArray(Charsets.UTF_8))

        // QR Code
        val qrTest = "https://github.com/pos-integration/escpos-emulator"
        val pL = ((qrTest.length + 3) and 0xFF).toByte()
        val pH = (((qrTest.length + 3) shr 8) and 0xFF).toByte()
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x04))
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        out.write(qrTest.toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

        out.write("\n\n=== ALL TESTS PASSED ===\n".toByteArray(Charsets.UTF_8))
        out.write(byteArrayOf(0x1B, 0x64, 0x03))
        out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // Partial Cut
        return out.toByteArray()
    }
}
