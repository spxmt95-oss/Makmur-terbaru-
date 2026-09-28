package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Cashier : Screen("cashier", "Kasir", Icons.Default.PointOfSale)
    data object Products : Screen("products", "Katalog & Stok", Icons.Default.Inventory2)
    data object Transactions : Screen("transactions", "Riwayat", Icons.Default.History)
    data object Reports : Screen("reports", "Laporan Laba", Icons.Default.Assessment)
    data object CapitalExpense : Screen("capital_expense", "Modal & Beban", Icons.Default.AccountBalanceWallet)
    data object Settings : Screen("settings", "Pengaturan", Icons.Default.Settings)

    companion object {
        // Use getter to avoid static initialization cycle in Kotlin JVM where nested objects are null during companion <clinit>
        val bottomNavItems: List<Screen>
            get() = listOf(
                Cashier,
                Products,
                Transactions,
                Reports,
                CapitalExpense,
                Settings
            )
    }
}
