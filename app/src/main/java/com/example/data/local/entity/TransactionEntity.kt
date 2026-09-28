package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Long,      // Total Belanja (yang harus dibayar pembeli)
    val totalCost: Long,        // Total Harga Beli / HPP untuk hitung laba bersih
    val paymentType: String,    // "TUNAI" or "NON_TUNAI"
    val cashPaid: Long,         // Nominal Pembayaran yang diterima
    val changeAmount: Long,     // Kembalian
    val totalItems: Int,        // Jumlah total barang terjual
    val notes: String = ""
)
