package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.CompletedReceipt
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter

@Composable
fun ReceiptDialog(
    receipt: CompletedReceipt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tx = receipt.transaction
    val items = receipt.items
    val settings = receipt.settings

    fun buildReceiptText(): String {
        val sb = StringBuilder()
        sb.appendLine("=================================")
        sb.appendLine(settings.storeName.uppercase())
        sb.appendLine(settings.storeAddress)
        if (settings.storePhone.isNotBlank()) {
            sb.appendLine("Telp: ${settings.storePhone}")
        }
        sb.appendLine("=================================")
        sb.appendLine("No. Struk : ${tx.invoiceNumber}")
        sb.appendLine("Waktu     : ${DateFormatter.formatDateTime(tx.timestamp)}")
        sb.appendLine("Kasir     : Utama")
        sb.appendLine("---------------------------------")
        for (item in items) {
            sb.appendLine(item.productName)
            val line = "  ${item.quantity}x @${CurrencyFormatter.formatRupiah(item.sellPrice)}"
            val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
            sb.appendLine(line.padEnd(20) + subtotal.padStart(13))
        }
        sb.appendLine("---------------------------------")
        sb.appendLine("TOTAL         : ${CurrencyFormatter.formatRupiah(tx.totalAmount)}")
        sb.appendLine("PEMBAYARAN    : ${tx.paymentType}")
        sb.appendLine("DIBAYAR       : ${CurrencyFormatter.formatRupiah(tx.cashPaid)}")
        sb.appendLine("KEMBALIAN     : ${CurrencyFormatter.formatRupiah(tx.changeAmount)}")
        sb.appendLine("=================================")
        sb.appendLine(settings.receiptFooter)
        sb.appendLine("=================================")
        return sb.toString()
    }

    fun shareReceipt() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Struk Belanja - ${settings.storeName}")
            putExtra(Intent.EXTRA_TEXT, buildReceiptText())
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Struk Belanja"))
    }

    fun printReceiptHtml() {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val itemsHtml = items.joinToString("") { item ->
            """
            <tr>
                <td colspan="2" style="font-weight:bold; padding-top:4px;">${item.productName}</td>
            </tr>
            <tr>
                <td style="color:#444;">${item.quantity} x ${CurrencyFormatter.formatRupiah(item.sellPrice)}</td>
                <td style="text-align:right;">${CurrencyFormatter.formatRupiah(item.subtotal)}</td>
            </tr>
            """.trimIndent()
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: monospace; font-size: 12px; margin: 0; padding: 12px; color: #000; width: 280px; }
                    .center { text-align: center; }
                    .divider { border-top: 1px dashed #000; margin: 8px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    .bold { font-weight: bold; }
                    .footer { text-align: center; font-size: 11px; margin-top: 12px; white-space: pre-line; }
                </style>
            </head>
            <body>
                <div class="center">
                    <h2 style="margin:0; font-size:16px;">${settings.storeName}</h2>
                    <p style="margin:2px 0;">${settings.storeAddress}</p>
                    <p style="margin:2px 0;">Telp: ${settings.storePhone}</p>
                </div>
                <div class="divider"></div>
                <div>No: ${tx.invoiceNumber}</div>
                <div>Waktu: ${DateFormatter.formatDateTime(tx.timestamp)}</div>
                <div>Kasir: Utama</div>
                <div class="divider"></div>
                <table>
                    $itemsHtml
                </table>
                <div class="divider"></div>
                <table>
                    <tr class="bold">
                        <td>TOTAL</td>
                        <td style="text-align:right;">${CurrencyFormatter.formatRupiah(tx.totalAmount)}</td>
                    </tr>
                    <tr>
                        <td>METODE</td>
                        <td style="text-align:right;">${tx.paymentType}</td>
                    </tr>
                    <tr>
                        <td>DIBAYAR</td>
                        <td style="text-align:right;">${CurrencyFormatter.formatRupiah(tx.cashPaid)}</td>
                    </tr>
                    <tr class="bold">
                        <td>KEMBALIAN</td>
                        <td style="text-align:right;">${CurrencyFormatter.formatRupiah(tx.changeAmount)}</td>
                    </tr>
                </table>
                <div class="divider"></div>
                <div class="footer">${settings.receiptFooter}</div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter("Struk_${tx.invoiceNumber}")
                printManager.print("Struk_${tx.invoiceNumber}", printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struk Pembayaran",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Thermal receipt paper card look
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Store details
                        Text(
                            text = settings.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = settings.storeAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (settings.storePhone.isNotBlank()) {
                            Text(
                                text = "Telp: ${settings.storePhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "No: ${tx.invoiceNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = DateFormatter.formatDateTime(tx.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items list
                        for (item in items) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity} x ${CurrencyFormatter.formatRupiah(item.sellPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.subtotal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Totals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Belanja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(tx.totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Metode Pembayaran",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (tx.paymentType == "NON_TUNAI") "NON TUNAI" else "TUNAI",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.paymentType == "NON_TUNAI") Color(0xFF1D4ED8) else Color(0xFF15803D)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Nominal Dibayar",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(tx.cashPaid),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Kembalian",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(tx.changeAmount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.changeAmount > 0) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Footer closing notes
                        Text(
                            text = settings.receiptFooter,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Cetak, Bagikan, Selesai
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { printReceiptHtml() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak")
                    }

                    OutlinedButton(
                        onClick = { shareReceipt() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_receipt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Transaksi Selesai", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
