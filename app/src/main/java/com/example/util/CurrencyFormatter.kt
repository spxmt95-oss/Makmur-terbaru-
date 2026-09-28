package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val localeID = Locale("in", "ID")
    private val formatRp = NumberFormat.getCurrencyInstance(localeID).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return formatRp.format(amount).replace("Rp", "Rp ")
    }

    fun formatNumber(amount: Long): String {
        return NumberFormat.getNumberInstance(localeID).format(amount)
    }

    fun parseAmount(text: String): Long {
        val clean = text.filter { it.isDigit() }
        return clean.toLongOrNull() ?: 0L
    }
}
