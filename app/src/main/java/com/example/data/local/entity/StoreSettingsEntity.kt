package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "TOKO MAKMUR",
    val storeAddress: String = "Jl. Kembang Kuning No.17, Surabaya",
    val storePhone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan.",
    val initialCapital: Long = 10000000L, // Modal Usaha / Modal Awal Toko Rp 10.000.000
    val quickNominals: String = "10,20,30,50,100"
) {
    fun getParsedQuickNominals(): List<String> {
        return quickNominals.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.toLongOrNull() != null }
            .ifEmpty { listOf("10", "20", "30", "50", "100") }
    }
}
