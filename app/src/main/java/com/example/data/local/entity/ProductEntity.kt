package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = false),
        Index(value = ["categoryId"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val buyPrice: Long,      // Harga Beli
    val sellPrice: Long,     // Harga Jual
    val stock: Int,          // Jumlah Stok Barang
    val minStockAlert: Int = 5, // Batas Peringatan Stok Sedikit
    val unit: String = "pcs",
    val updatedAt: Long = System.currentTimeMillis()
)
