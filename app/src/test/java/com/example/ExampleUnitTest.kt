package com.example

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.navigation.Screen
import com.example.util.BackupData
import com.example.util.BackupRestoreHelper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun bottomNavItems_areAllNonNullAndHaveRoutes() {
    val items = Screen.bottomNavItems
    assertEquals(6, items.size)
    items.forEach { screen ->
      assertNotNull("Screen in bottomNavItems must not be null", screen)
      assertTrue("Route must not be empty", screen.route.isNotBlank())
      assertTrue("Title must not be empty", screen.title.isNotBlank())
    }
  }

  @Test
  fun quickNominalMultipliedByThousand() {
    fun convertNominal(value: String): String {
      val numeric = value.toLongOrNull() ?: 0L
      return (numeric * 1000L).toString()
    }

    assertEquals("10000", convertNominal("10"))
    assertEquals("20000", convertNominal("20"))
    assertEquals("30000", convertNominal("30"))
    assertEquals("50000", convertNominal("50"))
    assertEquals("100000", convertNominal("100"))
    assertEquals("75000", convertNominal("75"))
    assertEquals("150000", convertNominal("150"))
    assertEquals("200000", convertNominal("200"))
  }

  @Test
  fun testStoreSettings_parsedQuickNominals() {
    val defaultSettings = StoreSettingsEntity()
    assertEquals(listOf("10", "20", "30", "50", "100"), defaultSettings.getParsedQuickNominals())

    val customSettings = StoreSettingsEntity(
        quickNominals = "10, 20, 50, 75, 100, 200"
    )
    assertEquals(listOf("10", "20", "50", "75", "100", "200"), customSettings.getParsedQuickNominals())

    val emptyOrInvalid = StoreSettingsEntity(
        quickNominals = "   , invalid,   "
    )
    assertEquals(listOf("10", "20", "30", "50", "100"), emptyOrInvalid.getParsedQuickNominals())
  }

  @Test
  fun testBackupFileName_hasJsonExtension() {
    val fileName = BackupRestoreHelper.generateBackupFileName()
    assertTrue("Backup file must end with .json", fileName.endsWith(".json"))
    assertTrue("Backup file must start with prefix", fileName.startsWith("toko_makmur_backup_"))
  }

  @Test
  fun testBackupExportAndParse_JsonRoundtrip() {
    val sampleSettings = StoreSettingsEntity(
        storeName = "TOKO MAKMUR UTAMA",
        storeAddress = "Surabaya",
        storePhone = "0812345678",
        receiptFooter = "Terima kasih",
        initialCapital = 10000000L,
        quickNominals = "10, 20, 30, 50, 100"
    )
    val sampleCategories = listOf(
        CategoryEntity(id = 1L, name = "Makanan", iconName = "fastfood")
    )
    val sampleProducts = listOf(
        ProductEntity(
            id = 1L,
            barcode = "1234567890",
            name = "Beras Rojolele 5kg",
            categoryId = 1L,
            categoryName = "Makanan",
            buyPrice = 65000L,
            sellPrice = 75000L,
            stock = 25,
            minStockAlert = 5,
            unit = "karung",
            updatedAt = System.currentTimeMillis()
        )
    )
    val sampleTransactions = listOf(
        TransactionEntity(
            id = 1L,
            invoiceNumber = "INV-20260928-001",
            timestamp = System.currentTimeMillis(),
            totalAmount = 75000L,
            totalCost = 65000L,
            paymentType = "TUNAI",
            cashPaid = 100000L,
            changeAmount = 25000L,
            totalItems = 1,
            notes = "Lunas"
        )
    )
    val sampleItems = listOf(
        TransactionItemEntity(
            id = 1L,
            transactionId = 1L,
            productId = 1L,
            productName = "Beras Rojolele 5kg",
            barcode = "1234567890",
            quantity = 1,
            buyPrice = 65000L,
            sellPrice = 75000L,
            subtotal = 75000L
        )
    )
    val sampleExpenses = listOf(
        ExpenseEntity(
            id = 1L,
            title = "Beli Plastik Kresek",
            category = "Operasional",
            amount = 15000L,
            timestamp = System.currentTimeMillis(),
            notes = "Kebutuhan kasir"
        )
    )

    val backupData = BackupData(
        settings = sampleSettings,
        categories = sampleCategories,
        products = sampleProducts,
        transactions = sampleTransactions,
        transactionItems = sampleItems,
        expenses = sampleExpenses
    )

    val jsonString = BackupRestoreHelper.exportToJson(backupData)
    assertNotNull(jsonString)
    assertTrue(jsonString.isNotBlank())

    // Validate JSON structure
    val jsonObject = JSONObject(jsonString)
    assertEquals("JSON", jsonObject.getString("format"))
    assertEquals("TOKO MAKMUR", jsonObject.getString("appName"))
    assertEquals(1, jsonObject.getInt("version"))

    // Parse back
    val parsedData = BackupRestoreHelper.parseFromJson(jsonString)
    assertEquals("TOKO MAKMUR UTAMA", parsedData.settings.storeName)
    assertEquals(1, parsedData.categories.size)
    assertEquals("Makanan", parsedData.categories[0].name)
    assertEquals(1, parsedData.products.size)
    assertEquals("Beras Rojolele 5kg", parsedData.products[0].name)
    assertEquals(75000L, parsedData.products[0].sellPrice)
    assertEquals(1, parsedData.transactions.size)
    assertEquals("INV-20260928-001", parsedData.transactions[0].invoiceNumber)
    assertEquals(1, parsedData.transactionItems.size)
    assertEquals(1, parsedData.expenses.size)
    assertEquals("Beli Plastik Kresek", parsedData.expenses[0].title)
  }

  @Test
  fun testFileSizeFormatting() {
    assertEquals("500 B", BackupRestoreHelper.formatFileSize(500L))
    assertTrue(BackupRestoreHelper.formatFileSize(1500L).contains("KB"))
    assertTrue(BackupRestoreHelper.formatFileSize(2000000L).contains("MB"))
  }
}
