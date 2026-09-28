package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SampleData
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.util.BackupData
import com.example.util.BackupRestoreHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class StoreRepository(private val database: AppDatabase) {
    private val categoryDao = database.categoryDao()
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val expenseDao = database.expenseDao()
    private val storeSettingsDao = database.storeSettingsDao()

    // Settings
    val settingsFlow: Flow<StoreSettingsEntity?> = storeSettingsDao.getSettings()
    suspend fun getSettingsSync(): StoreSettingsEntity {
        return storeSettingsDao.getSettingsSync() ?: SampleData.defaultSettings
    }
    suspend fun updateSettings(settings: StoreSettingsEntity) {
        storeSettingsDao.insertOrUpdate(settings)
    }

    // Categories
    val allCategoriesFlow: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    suspend fun addCategory(name: String): Long {
        return categoryDao.insertCategory(CategoryEntity(name = name.trim()))
    }
    suspend fun deleteCategory(category: CategoryEntity): Boolean {
        val count = categoryDao.countProductsByCategoryId(category.id)
        if (count > 0) {
            return false // Cannot delete category that has products!
        }
        categoryDao.deleteCategory(category)
        return true
    }
    suspend fun countProductsInCategory(categoryId: Long): Int {
        return categoryDao.countProductsByCategoryId(categoryId)
    }

    // Products
    val allProductsFlow: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProductsFlow: Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    val lowStockCountFlow: Flow<Int> = productDao.countLowStockProducts()
    val totalInventoryValueFlow: Flow<Long?> = productDao.getTotalInventoryValue()

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode.trim())
    }

    suspend fun getProductById(id: Long): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun saveProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteProductById(id: Long) {
        productDao.deleteProductById(id)
    }

    // Transactions
    val allTransactionsFlow: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startTime, endTime)
    }

    fun getTransactionItems(transactionId: Long): Flow<List<TransactionItemEntity>> {
        return transactionDao.getItemsForTransaction(transactionId)
    }

    suspend fun getTransactionItemsSync(transactionId: Long): List<TransactionItemEntity> {
        return transactionDao.getItemsForTransactionSync(transactionId)
    }

    suspend fun processCheckout(
        items: List<CartItem>,
        paymentType: String, // "TUNAI" or "NON_TUNAI"
        cashPaid: Long,
        notes: String = ""
    ): Pair<Long, String> {
        val totalAmount = items.sumOf { it.product.sellPrice * it.quantity }
        val totalCost = items.sumOf { it.product.buyPrice * it.quantity }
        val totalQuantity = items.sumOf { it.quantity }
        val finalCashPaid = if (paymentType == "NON_TUNAI") totalAmount else cashPaid
        val changeAmount = if (paymentType == "NON_TUNAI") 0L else (finalCashPaid - totalAmount).coerceAtLeast(0L)

        val invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
        val tx = TransactionEntity(
            invoiceNumber = invoiceNumber,
            timestamp = System.currentTimeMillis(),
            totalAmount = totalAmount,
            totalCost = totalCost,
            paymentType = paymentType,
            cashPaid = finalCashPaid,
            changeAmount = changeAmount,
            totalItems = totalQuantity,
            notes = notes
        )

        val txId = transactionDao.insertTransaction(tx)

        val txItems = items.map {
            TransactionItemEntity(
                transactionId = txId,
                productId = it.product.id,
                productName = it.product.name,
                barcode = it.product.barcode,
                quantity = it.quantity,
                buyPrice = it.product.buyPrice,
                sellPrice = it.product.sellPrice,
                subtotal = it.product.sellPrice * it.quantity
            )
        }
        transactionDao.insertTransactionItems(txItems)

        // Decrease stock for all products sold
        for (item in items) {
            productDao.decreaseStock(item.product.id, item.quantity)
        }

        return Pair(txId, invoiceNumber)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransactionWithItems(id)
    }

    // Expenses
    val allExpensesFlow: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesBetween(startTime, endTime)
    }

    suspend fun saveExpense(expense: ExpenseEntity): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    // Full Backup & Restore
    suspend fun createBackupData(): BackupData {
        val settings = getSettingsSync()
        val categories = categoryDao.getAllCategoriesSync()
        val products = productDao.getAllProductsSync()
        val transactions = transactionDao.getAllTransactionsSync()
        val transactionItems = transactionDao.getAllTransactionItemsSync()
        val expenses = expenseDao.getAllExpensesSync()

        return BackupData(
            settings = settings,
            categories = categories,
            products = products,
            transactions = transactions,
            transactionItems = transactionItems,
            expenses = expenses
        )
    }

    suspend fun restoreBackupData(data: BackupData) {
        // Save settings
        storeSettingsDao.insertOrUpdate(data.settings)

        // Save categories
        categoryDao.insertCategories(data.categories)

        // Save products
        productDao.insertProducts(data.products)

        // Save transactions & items
        for (tx in data.transactions) {
            transactionDao.insertTransaction(tx)
        }
        transactionDao.insertTransactionItems(data.transactionItems)

        // Save expenses
        expenseDao.insertExpenses(data.expenses)
    }
}

// Data model for active cart item in POS
data class CartItem(
    val product: ProductEntity,
    var quantity: Int
)
