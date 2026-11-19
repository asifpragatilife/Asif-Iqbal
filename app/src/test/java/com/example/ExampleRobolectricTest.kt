package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.pdf.DocumentExportEngine
import com.example.pdf.NormalizedRect
import com.example.security.VaultCryptoManager
import com.example.vision.OnDeviceOcrEngine
import com.example.vision.QrEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `app name matches AsifAraf`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("AsifAraf", context.getString(R.string.app_name))
    }

    @Test
    fun `vault PBKDF2 pin and AES-256-GCM roundtrip works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = VaultCryptoManager(context)
        val salt = crypto.generateSaltBase64()
        val hash = crypto.hashPinBase64("2468", salt)

        assertTrue(crypto.verifyPin("2468", hash, salt))
        assertFalse(crypto.verifyPin("0000", hash, salt))

        val secret = "Top secret passport bytes".toByteArray()
        val encrypted = crypto.encryptBytes(secret)
        val decrypted = crypto.decryptBytes(encrypted)
        assertArrayEquals(secret, decrypted)
    }

    @Test
    fun `smart classifier detects receipt and id`() {
        val receipt = OnDeviceOcrEngine.classify("Coffee House\nSubtotal $12.00\nTax $1.00\nTotal $13.00")
        assertEquals("Receipt", receipt.category)

        val idDoc = OnDeviceOcrEngine.classify("PASSPORT\nNationality: BGD\nDate of Birth: 1998-04-10\nExpires: 2031")
        assertEquals("ID", idDoc.category)
    }

    @Test
    fun `qr engine classifies wifi and generates bitmap`() {
        val wifi = QrEngine.classifyPayload("WIFI:T:WPA;S:AsifNet;P:SecretPass;;")
        assertEquals("WIFI", wifi.qrType)
        assertTrue(wifi.title.contains("AsifNet"))

        val bmp = QrEngine.generateQrBitmap("https://asifaraf.local", 256)
        assertNotNull(bmp)
        assertEquals(256, bmp!!.width)
    }

    @Test
    fun `permanent redaction burns black pixels and export files are created`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val burned = DocumentExportEngine.burnRedactionsOntoBitmap(
            bmp,
            listOf(NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f)),
        )
        assertEquals(Color.BLACK, burned.getPixel(50, 50))
        assertEquals(Color.WHITE, burned.getPixel(5, 5))

        val docx = DocumentExportEngine.exportDocx(context, "Brief", "Line 1\nLine 2", "test_brief")
        val xlsx = DocumentExportEngine.exportXlsx(context, "Ledger", "Item | Price\nPen | 2.00", "test_ledger")
        assertTrue(docx.exists() && docx.length() > 100)
        assertTrue(xlsx.exists() && xlsx.length() > 100)
    }
}
