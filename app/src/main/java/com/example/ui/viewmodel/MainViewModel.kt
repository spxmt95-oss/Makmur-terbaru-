package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.repository.CartItem
import com.example.data.repository.StoreRepository
import com.example.util.BackupFileInfo
import com.example.util.BackupRestoreHelper
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReportPeriod {
    HARI_INI,
    MINGGU_INI,
    BULAN_INI,
    SEMUA
}

data class CompletedReceipt(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>,
    val settings: StoreSettingsEntity
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = StoreRepository(database)

    // Store Settings
    val settings: StateFlow<StoreSettingsEntity?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quickNominals: StateFlow<List<String>> = settings
        .combine(MutableStateFlow(Unit)) { s, _ ->
            s?.getParsedQuickNominals() ?: listOf("10", "20", "30", "50", "100")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("10", "20", "30", "50", "100"))

    // Categories
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockCount: StateFlow<Int> = repository.lowStockCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalInventoryValue: StateFlow<Long> = repository.totalInventoryValueFlow
        .combine(MutableStateFlow(0L)) { v, _ -> v ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Product Filter state
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _filterOnlyLowStock = MutableStateFlow(false)
    val filterOnlyLowStock: StateFlow<Boolean> = _filterOnlyLowStock.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _productSearchQuery,
        _selectedCategoryId,
        _filterOnlyLowStock
    ) { products, query, catId, onlyLowStock ->
        products.filter { p ->
            val matchQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true)
            val matchCategory = catId == null || p.categoryId == catId
            val matchLowStock = !onlyLowStock || p.stock <= p.minStockAlert
            matchQuery && matchCategory && matchLowStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS / Cashier State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Long> = combine(_cart) { items ->
        items.first().sumOf { it.product.sellPrice * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _qrInputText = MutableStateFlow("")
    val qrInputText: StateFlow<String> = _qrInputText.asStateFlow()

    private val _paymentType = MutableStateFlow("TUNAI") // "TUNAI" or "NON_TUNAI"
    val paymentType: StateFlow<String> = _paymentType.asStateFlow()

    private val _nominalPaymentText = MutableStateFlow("")
    val nominalPaymentText: StateFlow<String> = _nominalPaymentText.asStateFlow()

    private val _lastCompletedReceipt = MutableStateFlow<CompletedReceipt?>(null)
    val lastCompletedReceipt: StateFlow<CompletedReceipt?> = _lastCompletedReceipt.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // Transactions & Reports
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reportPeriod = MutableStateFlow(ReportPeriod.HARI_INI)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    // Filtered Transactions & Expenses based on ReportPeriod
    val reportData: StateFlow<ReportSummary> = combine(
        allTransactions,
        allExpenses,
        _reportPeriod
    ) { txList, expList, period ->
        val now = System.currentTimeMillis()
        val startTime = when (period) {
            ReportPeriod.HARI_INI -> DateFormatter.getStartOfDay()
            ReportPeriod.MINGGU_INI -> DateFormatter.getStartOfWeek()
            ReportPeriod.BULAN_INI -> DateFormatter.getStartOfMonth()
            ReportPeriod.SEMUA -> 0L
        }

        val filteredTx = txList.filter { it.timestamp >= startTime }
        val filteredExp = expList.filter { it.timestamp >= startTime }

        val grossSales = filteredTx.sumOf { it.totalAmount }
        val totalCostOfGoods = filteredTx.sumOf { it.totalCost }
        val totalExpenses = filteredExp.sumOf { it.amount }

        val grossProfit = grossSales - totalCostOfGoods
        val netProfit = grossProfit - totalExpenses

        val cashTransactions = filteredTx.filter { it.paymentType == "TUNAI" }
        val nonCashTransactions = filteredTx.filter { it.paymentType == "NON_TUNAI" }

        val cashSalesTotal = cashTransactions.sumOf { it.totalAmount }
        val nonCashSalesTotal = nonCashTransactions.sumOf { it.totalAmount }

        ReportSummary(
            period = period,
            transactionCount = filteredTx.size,
            grossSales = grossSales,
            totalCostOfGoods = totalCostOfGoods,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            cashSalesTotal = cashSalesTotal,
            cashTransactionCount = cashTransactions.size,
            nonCashSalesTotal = nonCashSalesTotal,
            nonCashTransactionCount = nonCashTransactions.size,
            transactions = filteredTx,
            expenses = filteredExp
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportSummary())

    // POS Methods
    fun updateQrInput(text: String) {
        _qrInputText.value = text
    }

    /**
     * Requirement:
     * "Pada input kode QR teks akan terhapus otomatis setelah produk di tambahkan ke keranjang,
     * hal ini bertujuan untuk tidak perlu menghapus teks input kode QR untuk men-scan kode QR berikutnya"
     */
    fun onQrSubmit(customBarcode: String? = null) {
        val codeToScan = (customBarcode ?: _qrInputText.value).trim()
        if (codeToScan.isBlank()) return

        viewModelScope.launch {
            val product = repository.getProductByBarcode(codeToScan)
            if (product != null) {
                if (product.stock <= 0) {
                    _uiMessage.emit("Perhatian: Stok ${product.name} telah habis (0)!")
                }
                addToCart(product)
                // Automatically clear QR input text so user does not have to delete it before next scan!
                _qrInputText.value = ""
                _uiMessage.emit("Ditambahkan: ${product.name}")
            } else {
                _qrInputText.value = ""
                _uiMessage.emit("Produk dengan kode '$codeToScan' tidak ditemukan!")
            }
        }
    }

    fun addToCart(product: ProductEntity) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cart.value = current

        // If in Non Tunai mode, keep nominal equal to total
        if (_paymentType.value == "NON_TUNAI") {
            _nominalPaymentText.value = current.sumOf { it.product.sellPrice * it.quantity }.toString()
        }
    }

    fun incrementCartItem(productId: Long) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1)
            _cart.value = current
            if (_paymentType.value == "NON_TUNAI") {
                _nominalPaymentText.value = current.sumOf { it.product.sellPrice * it.quantity }.toString()
            }
        }
    }

    fun decrementCartItem(productId: Long) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = current[index]
            if (existing.quantity > 1) {
                current[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cart.value = current
            if (_paymentType.value == "NON_TUNAI") {
                _nominalPaymentText.value = current.sumOf { it.product.sellPrice * it.quantity }.toString()
            }
        }
    }

    fun removeFromCart(productId: Long) {
        val current = _cart.value.toMutableList()
        current.removeAll { it.product.id == productId }
        _cart.value = current
        if (_paymentType.value == "NON_TUNAI") {
            _nominalPaymentText.value = current.sumOf { it.product.sellPrice * it.quantity }.toString()
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _nominalPaymentText.value = ""
    }

    fun setPaymentType(type: String) {
        _paymentType.value = type
        if (type == "NON_TUNAI") {
            // Non Tunai nominal always matches total exactly
            _nominalPaymentText.value = cartTotal.value.toString()
        }
    }

    fun updateNominalPaymentText(text: String) {
        _nominalPaymentText.value = text
    }

    /**
     * Requirement:
     * "untuk pembayaran terdapat pilihan nominal cepat
     * Uang Pas 10 20 30 50 100 dimana nominal tersebut ketika di klik akan di tambahkan 000 pada saat pembayaran
     * Contoh klik 10 maka akan menjadi 10000 pada nominal pembayaran
     * Nominal yang ditampilkan bisa diedit angkanya sebelum pembayaran"
     */
    fun onQuickNominalClick(value: String) {
        if (value.equals("pas", ignoreCase = true)) {
            _nominalPaymentText.value = cartTotal.value.toString()
        } else {
            // Append "000" to the nominal number
            val numeric = value.toLongOrNull()
            if (numeric != null) {
                val fullAmount = numeric * 1000L
                _nominalPaymentText.value = fullAmount.toString()
            }
        }
    }

    fun processPayment() {
        val items = _cart.value
        if (items.isEmpty()) {
            viewModelScope.launch { _uiMessage.emit("Keranjang belanja masih kosong!") }
            return
        }

        val total = cartTotal.value
        val isNonTunai = _paymentType.value == "NON_TUNAI"
        val cashPaid = if (isNonTunai) total else CurrencyFormatter.parseAmount(_nominalPaymentText.value)

        if (!isNonTunai && cashPaid < total) {
            viewModelScope.launch {
                val shortage = total - cashPaid
                _uiMessage.emit("Uang pembayaran kurang ${CurrencyFormatter.formatRupiah(shortage)}!")
            }
            return
        }

        viewModelScope.launch {
            val (txId, invoiceNo) = repository.processCheckout(
                items = items,
                paymentType = _paymentType.value,
                cashPaid = cashPaid
            )

            val tx = repository.getTransactionById(txId)
            val txItems = repository.getTransactionItemsSync(txId)
            val settings = repository.getSettingsSync()

            if (tx != null) {
                _lastCompletedReceipt.value = CompletedReceipt(
                    transaction = tx,
                    items = txItems,
                    settings = settings
                )
            }

            clearCart()
            _uiMessage.emit("Transaksi $invoiceNo Berhasil Disimpan!")
        }
    }

    fun dismissReceipt() {
        _lastCompletedReceipt.value = null
    }

    fun showReceiptForTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val items = repository.getTransactionItemsSync(transaction.id)
            val currentSettings = repository.getSettingsSync()
            _lastCompletedReceipt.value = CompletedReceipt(
                transaction = transaction,
                items = items,
                settings = currentSettings
            )
        }
    }

    // Product & Category Management
    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setFilterOnlyLowStock(enable: Boolean) {
        _filterOnlyLowStock.value = enable
    }

    fun saveProduct(
        id: Long = 0,
        barcode: String,
        name: String,
        categoryId: Long,
        categoryName: String,
        buyPrice: Long,
        sellPrice: Long,
        stock: Int,
        minStockAlert: Int,
        unit: String
    ) {
        viewModelScope.launch {
            val p = ProductEntity(
                id = id,
                barcode = barcode.trim(),
                name = name.trim(),
                categoryId = categoryId,
                categoryName = categoryName,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                minStockAlert = minStockAlert,
                unit = unit.trim().ifBlank { "pcs" }
            )
            repository.saveProduct(p)
            _uiMessage.emit("Produk '${name}' berhasil disimpan!")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _uiMessage.emit("Produk '${product.name}' dihapus.")
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name.trim())
            _uiMessage.emit("Katalog '${name.trim()}' berhasil ditambahkan.")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val success = repository.deleteCategory(category)
            if (success) {
                _uiMessage.emit("Katalog '${category.name}' berhasil dihapus.")
            } else {
                _uiMessage.emit("Katalog '${category.name}' tidak dapat dihapus karena masih ada produk di dalamnya!")
            }
        }
    }

    // Expenses & Capital
    fun saveExpense(
        id: Long = 0,
        title: String,
        category: String,
        amount: Long,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val exp = ExpenseEntity(
                id = id,
                title = title.trim(),
                category = category.trim().ifBlank { "Operasional" },
                amount = amount,
                notes = notes.trim()
            )
            repository.saveExpense(exp)
            _uiMessage.emit("Pengeluaran '${title}' berhasil dicatat!")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _uiMessage.emit("Pengeluaran '${expense.title}' dihapus.")
        }
    }

    fun updateInitialCapital(newCapital: Long) {
        viewModelScope.launch {
            val cur = repository.getSettingsSync()
            repository.updateSettings(cur.copy(initialCapital = newCapital))
            _uiMessage.emit("Modal usaha berhasil diperbarui.")
        }
    }

    // Reports
    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx.id)
            _uiMessage.emit("Transaksi ${tx.invoiceNumber} berhasil dihapus.")
        }
    }

    // Settings
    fun updateStoreSettings(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        receiptFooter: String
    ) {
        viewModelScope.launch {
            val cur = repository.getSettingsSync()
            repository.updateSettings(
                cur.copy(
                    storeName = storeName.trim(),
                    storeAddress = storeAddress.trim(),
                    storePhone = storePhone.trim(),
                    receiptFooter = receiptFooter.trim()
                )
            )
            _uiMessage.emit("Pengaturan toko & struk berhasil disimpan!")
        }
    }

    fun updateQuickNominals(nominals: List<String>) {
        val cleanList = nominals
            .map { it.trim().filter { c -> c.isDigit() } }
            .filter { it.isNotBlank() && it.toLongOrNull() != null && (it.toLongOrNull() ?: 0L) > 0 }
            .distinct()

        val stringVal = if (cleanList.isEmpty()) "10,20,30,50,100" else cleanList.joinToString(",")
        viewModelScope.launch {
            val cur = repository.getSettingsSync()
            repository.updateSettings(cur.copy(quickNominals = stringVal))
            _uiMessage.emit("Pilihan nominal cepat berhasil diperbarui!")
        }
    }

    fun addQuickNominal(nominal: String) {
        val clean = nominal.trim().filter { it.isDigit() }
        if (clean.isBlank() || (clean.toLongOrNull() ?: 0L) <= 0L) {
            viewModelScope.launch { _uiMessage.emit("Masukkan angka nominal yang valid!") }
            return
        }
        val current = quickNominals.value.toMutableList()
        if (current.contains(clean)) {
            viewModelScope.launch { _uiMessage.emit("Nominal $clean sudah ada di daftar!") }
            return
        }
        current.add(clean)
        current.sortBy { it.toLongOrNull() ?: 0L }
        updateQuickNominals(current)
    }

    fun editQuickNominal(oldNominal: String, newNominal: String) {
        val clean = newNominal.trim().filter { it.isDigit() }
        if (clean.isBlank() || (clean.toLongOrNull() ?: 0L) <= 0L) {
            viewModelScope.launch { _uiMessage.emit("Masukkan angka nominal yang valid!") }
            return
        }
        val current = quickNominals.value.toMutableList()
        val index = current.indexOf(oldNominal)
        if (index >= 0) {
            current[index] = clean
            updateQuickNominals(current)
        }
    }

    fun removeQuickNominal(nominal: String) {
        val current = quickNominals.value.toMutableList()
        current.remove(nominal)
        if (current.isEmpty()) {
            viewModelScope.launch { _uiMessage.emit("Minimal harus ada satu pilihan nominal!") }
            return
        }
        updateQuickNominals(current)
    }

    fun resetQuickNominalsToDefault() {
        updateQuickNominals(listOf("10", "20", "30", "50", "100"))
    }

    // Backup & Restore with Local Folder & JSON format
    private val backupPrefs = application.getSharedPreferences("toko_makmur_backup_prefs", Context.MODE_PRIVATE)

    private val _selectedFolderUri = MutableStateFlow<Uri?>(
        backupPrefs.getString("folder_uri", null)?.let {
            try { Uri.parse(it) } catch (e: Exception) { null }
        }
    )
    val selectedFolderUri: StateFlow<Uri?> = _selectedFolderUri.asStateFlow()

    private val _selectedFolderName = MutableStateFlow<String>(
        backupPrefs.getString("folder_name", "") ?: ""
    )
    val selectedFolderName: StateFlow<String> = _selectedFolderName.asStateFlow()

    private val _folderBackups = MutableStateFlow<List<BackupFileInfo>>(emptyList())
    val folderBackups: StateFlow<List<BackupFileInfo>> = _folderBackups.asStateFlow()

    fun setBackupFolder(uri: Uri) {
        val app = getApplication<Application>()
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            app.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // Some SAF providers don't support persistable flags
        }
        val folderName = BackupRestoreHelper.getDisplayFolderName(app, uri)
        backupPrefs.edit()
            .putString("folder_uri", uri.toString())
            .putString("folder_name", folderName)
            .apply()
        _selectedFolderUri.value = uri
        _selectedFolderName.value = folderName
        refreshFolderBackups()
        viewModelScope.launch {
            _uiMessage.emit("Folder cadangan lokal dipilih: $folderName")
        }
    }

    fun clearBackupFolder() {
        backupPrefs.edit()
            .remove("folder_uri")
            .remove("folder_name")
            .apply()
        _selectedFolderUri.value = null
        _selectedFolderName.value = ""
        _folderBackups.value = emptyList()
        viewModelScope.launch {
            _uiMessage.emit("Folder cadangan lokal direset.")
        }
    }

    fun refreshFolderBackups() {
        val uri = _selectedFolderUri.value ?: return
        viewModelScope.launch {
            try {
                val files = BackupRestoreHelper.getBackupFilesFromFolder(getApplication(), uri)
                _folderBackups.value = files
            } catch (e: Exception) {
                _folderBackups.value = emptyList()
            }
        }
    }

    fun backupToSelectedFolder(onSuccess: (BackupFileInfo) -> Unit = {}) {
        val uri = _selectedFolderUri.value
        if (uri == null) {
            viewModelScope.launch {
                _uiMessage.emit("Silakan tentukan pilihan folder penyimpanan lokal terlebih dahulu!")
            }
            return
        }
        viewModelScope.launch {
            try {
                val backupData = repository.createBackupData()
                val json = BackupRestoreHelper.exportToJson(backupData)
                val fileInfo = BackupRestoreHelper.saveBackupToFolder(getApplication(), uri, json)
                if (fileInfo != null) {
                    refreshFolderBackups()
                    _uiMessage.emit("Berhasil dicadangkan ke folder lokal: ${fileInfo.name}")
                    onSuccess(fileInfo)
                } else {
                    _uiMessage.emit("Gagal menyimpan ke folder lokal. Pastikan izin akses folder diberikan.")
                }
            } catch (e: Exception) {
                _uiMessage.emit("Gagal membuat cadangan: ${e.localizedMessage}")
            }
        }
    }

    fun backupToCustomUri(destinationUri: Uri, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val backupData = repository.createBackupData()
                val json = BackupRestoreHelper.exportToJson(backupData)
                val ok = BackupRestoreHelper.writeJsonToUri(getApplication(), destinationUri, json)
                if (ok) {
                    _uiMessage.emit("File cadangan .JSON berhasil disimpan ke penyimpanan ponsel!")
                    refreshFolderBackups()
                    onSuccess(json)
                } else {
                    _uiMessage.emit("Gagal menyimpan file .JSON ke lokasi yang dipilih.")
                }
            } catch (e: Exception) {
                _uiMessage.emit("Gagal backup: ${e.localizedMessage}")
            }
        }
    }

    fun restoreFromUri(uri: Uri, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val jsonString = BackupRestoreHelper.readJsonFromUri(getApplication(), uri)
                val backupData = BackupRestoreHelper.parseFromJson(jsonString)
                repository.restoreBackupData(backupData)
                _uiMessage.emit("Data toko berhasil dipulihkan dari file .JSON!")
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.emit("Gagal memulihkan: Format file .JSON tidak valid atau tidak kompatibel.")
            }
        }
    }

    fun deleteFolderBackup(fileInfo: BackupFileInfo) {
        viewModelScope.launch {
            val deleted = BackupRestoreHelper.deleteDocumentFile(getApplication(), fileInfo.uri)
            if (deleted) {
                refreshFolderBackups()
                _uiMessage.emit("File cadangan '${fileInfo.name}' berhasil dihapus.")
            } else {
                _uiMessage.emit("Gagal menghapus file cadangan.")
            }
        }
    }

    fun createBackupJson(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = repository.createBackupData()
                val json = BackupRestoreHelper.exportToJson(backupData)
                val file = BackupRestoreHelper.saveBackupToFile(getApplication(), json)
                _uiMessage.emit("Backup tersimpan di: ${file.name}")
                onSuccess(json)
            } catch (e: Exception) {
                _uiMessage.emit("Gagal backup: ${e.localizedMessage}")
            }
        }
    }

    fun restoreFromJson(jsonString: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val backupData = BackupRestoreHelper.parseFromJson(jsonString)
                repository.restoreBackupData(backupData)
                _uiMessage.emit("Data berhasil dipulihkan dari teks backup JSON!")
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.emit("Gagal memulihkan data: Format JSON tidak valid.")
            }
        }
    }
}

data class ReportSummary(
    val period: ReportPeriod = ReportPeriod.HARI_INI,
    val transactionCount: Int = 0,
    val grossSales: Long = 0L,
    val totalCostOfGoods: Long = 0L,
    val grossProfit: Long = 0L,
    val totalExpenses: Long = 0L,
    val netProfit: Long = 0L,
    val cashSalesTotal: Long = 0L,
    val cashTransactionCount: Int = 0,
    val nonCashSalesTotal: Long = 0L,
    val nonCashTransactionCount: Int = 0,
    val transactions: List<TransactionEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList()
)
