package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_items",
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["productId"])
    ]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val buyPrice: Long,      // Harga beli saat transaksi untuk hitung HPP akurat
    val sellPrice: Long,     // Harga jual per satuan
    val subtotal: Long       // quantity * sellPrice
)
