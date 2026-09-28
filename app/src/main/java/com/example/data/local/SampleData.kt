package com.example.data.local

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreSettingsEntity

object SampleData {
    val defaultSettings = StoreSettingsEntity(
        id = 1,
        storeName = "TOKO MAKMUR",
        storeAddress = "Jl. Kembang Kuning No.17, Surabaya",
        storePhone = "0812-3456-7890",
        receiptFooter = "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan.",
        initialCapital = 15000000L, // Rp 15.000.000
        quickNominals = "10,20,30,50,100"
    )

    val defaultCategories = listOf(
        CategoryEntity(id = 1, name = "Sembako", iconName = "shopping_basket"),
        CategoryEntity(id = 2, name = "Minuman", iconName = "local_cafe"),
        CategoryEntity(id = 3, name = "Makanan Ringan", iconName = "fastfood"),
        CategoryEntity(id = 4, name = "Bumbu Dapur", iconName = "kitchen"),
        CategoryEntity(id = 5, name = "Kebutuhan Rumah", iconName = "clean_hands"),
        CategoryEntity(id = 6, name = "Rokok & Tembakau", iconName = "smoking_rooms")
    )

    val defaultProducts = listOf(
        ProductEntity(
            id = 1,
            barcode = "8992753210111",
            name = "Beras Premium Ramos 5 Kg",
            categoryId = 1,
            categoryName = "Sembako",
            buyPrice = 64000L,
            sellPrice = 72000L,
            stock = 25,
            minStockAlert = 5,
            unit = "sak"
        ),
        ProductEntity(
            id = 2,
            barcode = "8998866103010",
            name = "Minyak Goreng Bimoli 2 Liter",
            categoryId = 1,
            categoryName = "Sembako",
            buyPrice = 33000L,
            sellPrice = 37500L,
            stock = 18,
            minStockAlert = 5,
            unit = "pouch"
        ),
        ProductEntity(
            id = 3,
            barcode = "8991002101234",
            name = "Gula Pasir Gulaku 1 Kg",
            categoryId = 1,
            categoryName = "Sembako",
            buyPrice = 15500L,
            sellPrice = 18000L,
            stock = 30,
            minStockAlert = 8,
            unit = "bungkus"
        ),
        ProductEntity(
            id = 4,
            barcode = "8992388111222",
            name = "Indomie Goreng Original 85g",
            categoryId = 3,
            categoryName = "Makanan Ringan",
            buyPrice = 2800L,
            sellPrice = 3500L,
            stock = 3, // Low stock sample!
            minStockAlert = 10,
            unit = "bungkus"
        ),
        ProductEntity(
            id = 5,
            barcode = "8999999001122",
            name = "Kopi Kapal Api Spesial Mix 10x24g",
            categoryId = 2,
            categoryName = "Minuman",
            buyPrice = 12500L,
            sellPrice = 15000L,
            stock = 4, // Low stock sample!
            minStockAlert = 6,
            unit = "renteng"
        ),
        ProductEntity(
            id = 6,
            barcode = "8992761001018",
            name = "Teh Botol Sosro Kotak 250ml",
            categoryId = 2,
            categoryName = "Minuman",
            buyPrice = 3200L,
            sellPrice = 4500L,
            stock = 24,
            minStockAlert = 6,
            unit = "kotak"
        ),
        ProductEntity(
            id = 7,
            barcode = "8993175537557",
            name = "Sabun Cuci Piring Sunlight 650ml",
            categoryId = 5,
            categoryName = "Kebutuhan Rumah",
            buyPrice = 13000L,
            sellPrice = 15500L,
            stock = 12,
            minStockAlert = 4,
            unit = "pouch"
        ),
        ProductEntity(
            id = 8,
            barcode = "8991389220015",
            name = "Kecap Manis Bango 520ml",
            categoryId = 4,
            categoryName = "Bumbu Dapur",
            buyPrice = 21000L,
            sellPrice = 24500L,
            stock = 15,
            minStockAlert = 5,
            unit = "pouch"
        ),
        ProductEntity(
            id = 9,
            barcode = "8996001304123",
            name = "Air Mineral Aqua 600ml",
            categoryId = 2,
            categoryName = "Minuman",
            buyPrice = 2800L,
            sellPrice = 4000L,
            stock = 2, // Low stock sample!
            minStockAlert = 10,
            unit = "botol"
        ),
        ProductEntity(
            id = 10,
            barcode = "8992753001235",
            name = "Telur Ayam Ras 1 Kg",
            categoryId = 1,
            categoryName = "Sembako",
            buyPrice = 26000L,
            sellPrice = 29500L,
            stock = 20,
            minStockAlert = 5,
            unit = "kg"
        )
    )

    val defaultExpenses = listOf(
        ExpenseEntity(
            id = 1,
            title = "Bayar Listrik Toko PLN",
            category = "Operasional",
            amount = 320000L,
            timestamp = System.currentTimeMillis() - 86400000L * 2,
            notes = "Listrik bulan ini"
        ),
        ExpenseEntity(
            id = 2,
            title = "Kantong Kresek & Lakban",
            category = "Perlengkapan",
            amount = 45000L,
            timestamp = System.currentTimeMillis() - 86400000L,
            notes = "Perlengkapan kasir"
        )
    )
}
