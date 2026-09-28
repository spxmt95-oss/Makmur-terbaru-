package com.example.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val settings: StoreSettingsEntity,
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val transactions: List<TransactionEntity>,
    val transactionItems: List<TransactionItemEntity>,
    val expenses: List<ExpenseEntity>
)

data class BackupFileInfo(
    val name: String,
    val uri: Uri,
    val sizeBytes: Long,
    val lastModified: Long,
    val formattedDate: String,
    val isExternal: Boolean = true
)

object BackupRestoreHelper {

    fun generateBackupFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "toko_makmur_backup_${sdf.format(Date())}.json"
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun exportToJson(data: BackupData): String {
        val root = JSONObject()
        root.put("format", "JSON")
        root.put("version", 1)
        root.put("appName", "TOKO MAKMUR")
        root.put("exportedAt", System.currentTimeMillis())

        val summary = JSONObject().apply {
            put("productsCount", data.products.size)
            put("transactionsCount", data.transactions.size)
            put("expensesCount", data.expenses.size)
            put("categoriesCount", data.categories.size)
        }
        root.put("summary", summary)

        // Settings
        val s = JSONObject().apply {
            put("id", data.settings.id)
            put("storeName", data.settings.storeName)
            put("storeAddress", data.settings.storeAddress)
            put("storePhone", data.settings.storePhone)
            put("receiptFooter", data.settings.receiptFooter)
            put("initialCapital", data.settings.initialCapital)
            put("quickNominals", data.settings.quickNominals)
        }
        root.put("settings", s)

        // Categories
        val catsArray = JSONArray()
        data.categories.forEach { c ->
            catsArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("iconName", c.iconName)
            })
        }
        root.put("categories", catsArray)

        // Products
        val prodsArray = JSONArray()
        data.products.forEach { p ->
            prodsArray.put(JSONObject().apply {
                put("id", p.id)
                put("barcode", p.barcode)
                put("name", p.name)
                put("categoryId", p.categoryId)
                put("categoryName", p.categoryName)
                put("buyPrice", p.buyPrice)
                put("sellPrice", p.sellPrice)
                put("stock", p.stock)
                put("minStockAlert", p.minStockAlert)
                put("unit", p.unit)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("products", prodsArray)

        // Transactions
        val txArray = JSONArray()
        data.transactions.forEach { t ->
            txArray.put(JSONObject().apply {
                put("id", t.id)
                put("invoiceNumber", t.invoiceNumber)
                put("timestamp", t.timestamp)
                put("totalAmount", t.totalAmount)
                put("totalCost", t.totalCost)
                put("paymentType", t.paymentType)
                put("cashPaid", t.cashPaid)
                put("changeAmount", t.changeAmount)
                put("totalItems", t.totalItems)
                put("notes", t.notes)
            })
        }
        root.put("transactions", txArray)

        // Transaction Items
        val itemsArray = JSONArray()
        data.transactionItems.forEach { ti ->
            itemsArray.put(JSONObject().apply {
                put("id", ti.id)
                put("transactionId", ti.transactionId)
                put("productId", ti.productId)
                put("productName", ti.productName)
                put("barcode", ti.barcode)
                put("quantity", ti.quantity)
                put("buyPrice", ti.buyPrice)
                put("sellPrice", ti.sellPrice)
                put("subtotal", ti.subtotal)
            })
        }
        root.put("transactionItems", itemsArray)

        // Expenses
        val expArray = JSONArray()
        data.expenses.forEach { e ->
            expArray.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("category", e.category)
                put("amount", e.amount)
                put("timestamp", e.timestamp)
                put("notes", e.notes)
            })
        }
        root.put("expenses", expArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        val sObj = root.optJSONObject("settings")
        val settings = if (sObj != null) {
            StoreSettingsEntity(
                id = sObj.optInt("id", 1),
                storeName = sObj.optString("storeName", "TOKO MAKMUR"),
                storeAddress = sObj.optString("storeAddress", "Jl. Kembang Kuning No.17, Surabaya"),
                storePhone = sObj.optString("storePhone", "0812-3456-7890"),
                receiptFooter = sObj.optString("receiptFooter", "Terima kasih atas kunjungan Anda!"),
                initialCapital = sObj.optLong("initialCapital", 15000000L),
                quickNominals = sObj.optString("quickNominals", "10,20,30,50,100")
            )
        } else {
            StoreSettingsEntity()
        }

        val categories = mutableListOf<CategoryEntity>()
        val catsArray = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catsArray.length()) {
            val obj = catsArray.getJSONObject(i)
            categories.add(
                CategoryEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.getString("name"),
                    iconName = obj.optString("iconName", "category")
                )
            )
        }

        val products = mutableListOf<ProductEntity>()
        val prodsArray = root.optJSONArray("products") ?: JSONArray()
        for (i in 0 until prodsArray.length()) {
            val obj = prodsArray.getJSONObject(i)
            products.add(
                ProductEntity(
                    id = obj.optLong("id", 0L),
                    barcode = obj.getString("barcode"),
                    name = obj.getString("name"),
                    categoryId = obj.optLong("categoryId", 1L),
                    categoryName = obj.optString("categoryName", "Umum"),
                    buyPrice = obj.optLong("buyPrice", 0L),
                    sellPrice = obj.optLong("sellPrice", 0L),
                    stock = obj.optInt("stock", 0),
                    minStockAlert = obj.optInt("minStockAlert", 5),
                    unit = obj.optString("unit", "pcs"),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val transactions = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            transactions.add(
                TransactionEntity(
                    id = obj.optLong("id", 0L),
                    invoiceNumber = obj.getString("invoiceNumber"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    totalAmount = obj.optLong("totalAmount", 0L),
                    totalCost = obj.optLong("totalCost", 0L),
                    paymentType = obj.optString("paymentType", "TUNAI"),
                    cashPaid = obj.optLong("cashPaid", 0L),
                    changeAmount = obj.optLong("changeAmount", 0L),
                    totalItems = obj.optInt("totalItems", 0),
                    notes = obj.optString("notes", "")
                )
            )
        }

        val items = mutableListOf<TransactionItemEntity>()
        val itemsArray = root.optJSONArray("transactionItems") ?: JSONArray()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            items.add(
                TransactionItemEntity(
                    id = obj.optLong("id", 0L),
                    transactionId = obj.optLong("transactionId", 0L),
                    productId = obj.optLong("productId", 0L),
                    productName = obj.getString("productName"),
                    barcode = obj.optString("barcode", ""),
                    quantity = obj.optInt("quantity", 1),
                    buyPrice = obj.optLong("buyPrice", 0L),
                    sellPrice = obj.optLong("sellPrice", 0L),
                    subtotal = obj.optLong("subtotal", 0L)
                )
            )
        }

        val expenses = mutableListOf<ExpenseEntity>()
        val expArray = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until expArray.length()) {
            val obj = expArray.getJSONObject(i)
            expenses.add(
                ExpenseEntity(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    category = obj.optString("category", "Operasional"),
                    amount = obj.optLong("amount", 0L),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    notes = obj.optString("notes", "")
                )
            )
        }

        return BackupData(
            settings = settings,
            categories = categories,
            products = products,
            transactions = transactions,
            transactionItems = items,
            expenses = expenses
        )
    }

    // Storage Access Framework (SAF) - Local folder backup
    fun saveBackupToFolder(context: Context, folderUri: Uri, json: String): BackupFileInfo? {
        val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        if (!dir.canWrite()) return null

        val fileName = generateBackupFileName()
        val file = dir.createFile("application/json", fileName) ?: return null

        context.contentResolver.openOutputStream(file.uri)?.use { os ->
            os.write(json.toByteArray(Charsets.UTF_8))
            os.flush()
        } ?: return null

        return BackupFileInfo(
            name = fileName,
            uri = file.uri,
            sizeBytes = file.length(),
            lastModified = file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis(),
            formattedDate = DateFormatter.formatDateTime(file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()),
            isExternal = true
        )
    }

    // Storage Access Framework - Write directly to a user-chosen destination Uri (e.g. from CreateDocument)
    fun writeJsonToUri(context: Context, destinationUri: Uri, json: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Storage Access Framework - Read JSON from any chosen Uri (e.g. from OpenDocument)
    fun readJsonFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
        } ?: throw IOException("Tidak dapat membaca data dari file yang dipilih.")
    }

    // List all .json backup files located in the selected SAF folder
    fun getBackupFilesFromFolder(context: Context, folderUri: Uri): List<BackupFileInfo> {
        val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        if (!dir.canRead()) return emptyList()

        return try {
            dir.listFiles()
                .filter { it.isFile && (it.name?.endsWith(".json", ignoreCase = true) == true) }
                .map { f ->
                    val lastMod = f.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
                    BackupFileInfo(
                        name = f.name ?: "backup.json",
                        uri = f.uri,
                        sizeBytes = f.length(),
                        lastModified = lastMod,
                        formattedDate = DateFormatter.formatDateTime(lastMod),
                        isExternal = true
                    )
                }
                .sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Delete a document file
    fun deleteDocumentFile(context: Context, uri: Uri): Boolean {
        return try {
            val doc = DocumentFile.fromSingleUri(context, uri)
            doc?.delete() ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Get a friendly display name for the picked folder Uri
    fun getDisplayFolderName(context: Context, folderUri: Uri): String {
        try {
            val dir = DocumentFile.fromTreeUri(context, folderUri)
            val name = dir?.name
            if (!name.isNullOrBlank()) {
                return name
            }
            val path = folderUri.path ?: ""
            val treePart = path.substringAfterLast(":")
            if (treePart.isNotBlank()) {
                return treePart.replace("/", " > ")
            }
        } catch (e: Exception) {
            // fallback
        }
        return "Penyimpanan Internal"
    }

    // Internal app directory backup (fallback & cache)
    fun saveBackupToFile(context: Context, json: String): File {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val fileName = generateBackupFileName()
        val file = File(backupDir, fileName)
        file.writeText(json)
        return file
    }

    fun getBackupFiles(context: Context): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
