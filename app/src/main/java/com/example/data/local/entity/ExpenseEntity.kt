package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,          // Nama Pengeluaran (e.g. Listrik, Kantong Plastik, Air)
    val category: String,       // Operasional, Gaji, Perlengkapan, Sewa, Lainnya
    val amount: Long,           // Jumlah Nominal Pengeluaran
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
