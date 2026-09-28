package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.RedDanger
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BackupFileInfo
import com.example.util.BackupRestoreHelper
import com.example.util.CurrencyFormatter

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val quickNominals by viewModel.quickNominals.collectAsStateWithLifecycle()
    val selectedFolderUri by viewModel.selectedFolderUri.collectAsStateWithLifecycle()
    val selectedFolderName by viewModel.selectedFolderName.collectAsStateWithLifecycle()
    val folderBackups by viewModel.folderBackups.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var storeName by remember { mutableStateOf("") }
    var storeAddress by remember { mutableStateOf("") }
    var storePhone by remember { mutableStateOf("") }
    var receiptFooter by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        settings?.let {
            storeName = it.storeName
            storeAddress = it.storeAddress
            storePhone = it.storePhone
            receiptFooter = it.receiptFooter
        }
    }

    var newNominalInput by remember { mutableStateOf("") }
    var batchNominalText by remember { mutableStateOf("") }
    var editingNominal by remember { mutableStateOf<String?>(null) }
    var editNominalValue by remember { mutableStateOf("") }

    LaunchedEffect(quickNominals) {
        batchNominalText = quickNominals.joinToString(", ")
    }

    var showBackupResultDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }

    // SAF ActivityResult Launchers
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.setBackupFolder(uri)
        }
    }

    val createJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.backupToCustomUri(uri) { json ->
                backupJsonText = json
                showBackupResultDialog = true
            }
        }
    }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmRestoreUriDialog by remember { mutableStateOf(false) }

    val openJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showConfirmRestoreUriDialog = true
        }
    }

    var selectedBackupToRestore by remember { mutableStateOf<com.example.util.BackupFileInfo?>(null) }
    var selectedBackupToDelete by remember { mutableStateOf<com.example.util.BackupFileInfo?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storefront, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Pengaturan Toko & Struk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
                Text(
                    text = "Sesuaikan informasi toko pada struk belanja dan kelola cadangan data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Store & Receipt Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Format Struk Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = Color(0xFFF1F5F9))

                // Nama Toko (Bagian Atas Struk)
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Nama Toko (Bagian Atas Struk)") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_store_name"),
                    singleLine = true
                )

                // Alamat Toko (Bagian Atas Struk)
                OutlinedTextField(
                    value = storeAddress,
                    onValueChange = { storeAddress = it },
                    label = { Text("Alamat Toko (Bagian Atas Struk)") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_store_address"),
                    maxLines = 2
                )

                // Telepon Toko
                OutlinedTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    label = { Text("No. Telepon / Kontak Toko") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_store_phone"),
                    singleLine = true
                )

                // Kata Penutup Struk (Bagian Paling Bawah Struk)
                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Kata Penutup Struk (Bagian Paling Bawah)") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_receipt_footer"),
                    maxLines = 3,
                    supportingText = {
                        Text("Teks ini akan dicetak pada bagian paling bawah setiap struk belanja")
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        viewModel.updateStoreSettings(
                            storeName = storeName,
                            storeAddress = storeAddress,
                            storePhone = storePhone,
                            receiptFooter = receiptFooter
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Simpan Pengaturan Struk", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Pilihan Nominal Cepat (Tunai) Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pilihan Nominal Cepat (Tunai)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Atur tombol pilihan nominal cepat yang tampil pada layar kasir saat pembayaran Tunai. Angka nominal otomatis dikalikan 1.000 (contoh: 10 menjadi Rp 10.000, 100 menjadi Rp 100.000).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = Color(0xFFF1F5F9))

                Text(
                    text = "Daftar Tombol Nominal Aktif:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // List of existing nominal chips/items
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickNominals.forEach { nominal ->
                        val numeric = nominal.toLongOrNull() ?: 0L
                        val fullRupiah = CurrencyFormatter.formatRupiah(numeric * 1000L)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_nominal_item_$nominal"),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = PrimaryGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = nominal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fullRupiah,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Tombol $nominal di kasir ➔ $fullRupiah",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Edit button
                                IconButton(
                                    onClick = {
                                        editingNominal = nominal
                                        editNominalValue = nominal
                                    },
                                    modifier = Modifier.size(36.dp).testTag("edit_nominal_$nominal")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit nominal $nominal",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = {
                                        viewModel.removeQuickNominal(nominal)
                                    },
                                    enabled = quickNominals.size > 1,
                                    modifier = Modifier.size(36.dp).testTag("delete_nominal_$nominal")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus nominal $nominal",
                                        tint = if (quickNominals.size > 1) RedDanger else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tambah Nominal Baru
                Text(
                    text = "Tambah Pilihan Nominal Baru:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newNominalInput,
                        onValueChange = { input ->
                            newNominalInput = input.filter { it.isDigit() }
                        },
                        label = { Text("Angka Baru (misal: 75)") },
                        placeholder = { Text("Contoh: 75") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_nominal_input"),
                        singleLine = true,
                        supportingText = {
                            val num = newNominalInput.toLongOrNull()
                            if (num != null && num > 0) {
                                Text("Akan menjadi: ${CurrencyFormatter.formatRupiah(num * 1000L)}")
                            } else {
                                Text("Ketik angka tanpa '000' (misal: 75 = Rp 75.000)")
                            }
                        }
                    )

                    Button(
                        onClick = {
                            if (newNominalInput.isNotBlank()) {
                                viewModel.addQuickNominal(newNominalInput)
                                newNominalInput = ""
                            }
                        },
                        enabled = newNominalInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("add_nominal_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Edit Semua Nominal Sekaligus
                Text(
                    text = "Atur Urutan / Edit Semua Sekaligus:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = batchNominalText,
                    onValueChange = { batchNominalText = it },
                    label = { Text("Daftar Nominal (Dipisah Koma)") },
                    modifier = Modifier.fillMaxWidth().testTag("batch_nominal_input"),
                    singleLine = true,
                    supportingText = {
                        Text("Contoh: 10, 20, 30, 50, 100, 150")
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val list = batchNominalText.split(",")
                                .map { it.trim().filter { c -> c.isDigit() } }
                                .filter { it.isNotBlank() }
                            viewModel.updateQuickNominals(list)
                        },
                        modifier = Modifier.weight(1f).testTag("save_batch_nominal_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetQuickNominalsToDefault()
                        },
                        modifier = Modifier.testTag("reset_nominal_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Standar")
                    }
                }
            }
        }

        // Backup & Restore Card (.JSON with Local Folder Access)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cadangkan & Pulihkan Data (.JSON)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Format .JSON • Akses Penyimpanan Internal Ponsel",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Seluruh data toko (produk, riwayat transaksi, pengeluaran kas, dan pengaturan) disimpan dalam file format standar .JSON. Tentukan pilihan folder lokal di penyimpanan ponsel agar file cadangan tersimpan rapi dan mudah dipulihkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = Color(0xFFF1F5F9))

                // Pilihan Folder Lokal di Penyimpanan Internal
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pilihan Folder Penyimpanan Lokal:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (selectedFolderUri != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryGreen.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFolderName.ifBlank { "Folder Lokal Aktif" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Akses folder lokal aktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryGreen
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.clearBackupFolder() },
                                    modifier = Modifier.size(32.dp).testTag("clear_folder_button")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Lepas Akses Folder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.backupToSelectedFolder() },
                                    modifier = Modifier.weight(1f).testTag("backup_to_folder_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cadangkan Sekarang (.JSON)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }

                                OutlinedButton(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    modifier = Modifier.testTag("change_folder_button")
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ganti Folder", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Daftar File Cadangan di Folder Terpilih
                    if (folderBackups.isNotEmpty()) {
                        Text(
                            text = "File Cadangan .JSON Ditemukan (${folderBackups.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            folderBackups.forEach { fileInfo ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fileInfo.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${BackupRestoreHelper.formatFileSize(fileInfo.sizeBytes)} • ${fileInfo.formattedDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = { selectedBackupToRestore = fileInfo },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp).testTag("restore_file_${fileInfo.name}")
                                        ) {
                                            Text("Pulihkan", style = MaterialTheme.typography.labelMedium)
                                        }
                                        IconButton(
                                            onClick = { selectedBackupToDelete = fileInfo },
                                            modifier = Modifier.size(34.dp).testTag("delete_file_${fileInfo.name}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = RedDanger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Belum ada file cadangan .JSON di folder ini. Klik tombol cadangkan di atas untuk membuat cadangan baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Belum Ada Folder Lokal Terpilih",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Beri akses ke salah satu folder di memori internal ponsel (seperti folder Download atau Documents) agar file cadangan .JSON tersimpan di perangkat Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { folderPickerLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier.fillMaxWidth().testTag("choose_folder_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pilih Folder Penyimpanan Lokal", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                // Opsi File .JSON Bebas & Eksternal
                Text(
                    text = "Akses File .JSON Langsung:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            createJsonFileLauncher.launch(BackupRestoreHelper.generateBackupFileName())
                        },
                        modifier = Modifier.weight(1f).testTag("save_as_json_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan .JSON", maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = {
                            openJsonFileLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*", "*/*"))
                        },
                        modifier = Modifier.weight(1f).testTag("restore_from_file_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pilih File .JSON", maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Opsi Teks JSON Manual / Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.createBackupJson { json ->
                                backupJsonText = json
                                showBackupResultDialog = true
                            }
                        },
                        modifier = Modifier.testTag("share_json_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin / Bagikan Teks JSON", style = MaterialTheme.typography.bodySmall)
                    }

                    TextButton(
                        onClick = {
                            restoreInputJson = ""
                            showRestoreDialog = true
                        },
                        modifier = Modifier.testTag("paste_json_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tempel Teks JSON", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // App Information Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOKO MAKMUR - POS & Inventaris v1.0",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Jl. Kembang Kuning No.17, Surabaya",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Backup Result Dialog
    if (showBackupResultDialog) {
        AlertDialog(
            onDismissRequest = { showBackupResultDialog = false },
            title = { Text("Cadangan Data Berhasil") },
            text = {
                Column {
                    Text(
                        text = "Data telah disimpan ke penyimpanan lokal. Anda juga dapat menyalin atau membagikan teks cadangan ini:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupJsonText.take(200) + if (backupJsonText.length > 200) "\n... [selengkapnya]" else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Backup Toko Makmur")
                            putExtra(Intent.EXTRA_TEXT, backupJsonText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Cadangan"))
                        showBackupResultDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Backup Toko Makmur", backupJsonText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Teks backup disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        showBackupResultDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin")
                }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Pulihkan Data (Restore)") },
            text = {
                Column {
                    Text(
                        text = "Tempelkan kode JSON cadangan di bawah ini untuk memulihkan seluruh data:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreInputJson,
                        onValueChange = { restoreInputJson = it },
                        modifier = Modifier.fillMaxWidth().testTag("restore_json_input"),
                        placeholder = { Text("Tempel JSON di sini...") },
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInputJson.isNotBlank()) {
                            viewModel.restoreFromJson(restoreInputJson)
                            showRestoreDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirm Restore from Selected URI Dialog
    if (showConfirmRestoreUriDialog && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmRestoreUriDialog = false
                pendingRestoreUri = null
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = PrimaryGreen) },
            title = { Text("Konfirmasi Pulihkan Data") },
            text = {
                Text(
                    text = "Aplikasi akan memulihkan data produk, riwayat transaksi, pengeluaran, dan pengaturan toko dari file .JSON yang Anda pilih. Data saat ini akan diperbarui dengan data cadangan ini. Lanjutkan?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri
                        if (uri != null) {
                            viewModel.restoreFromUri(uri)
                        }
                        showConfirmRestoreUriDialog = false
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.testTag("confirm_restore_uri_button")
                ) {
                    Text("Ya, Pulihkan Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmRestoreUriDialog = false
                        pendingRestoreUri = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirm Restore from List Item Dialog
    if (selectedBackupToRestore != null) {
        val target = selectedBackupToRestore!!
        AlertDialog(
            onDismissRequest = { selectedBackupToRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = PrimaryGreen) },
            title = { Text("Pulihkan File Cadangan") },
            text = {
                Text(
                    text = "Pulihkan seluruh data toko dari file '${target.name}' (${BackupRestoreHelper.formatFileSize(target.sizeBytes)}) yang tersimpan di folder lokal?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromUri(target.uri)
                        selectedBackupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.testTag("confirm_restore_selected_button")
                ) {
                    Text("Ya, Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackupToRestore = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirm Delete from List Item Dialog
    if (selectedBackupToDelete != null) {
        val target = selectedBackupToDelete!!
        AlertDialog(
            onDismissRequest = { selectedBackupToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RedDanger) },
            title = { Text("Hapus File Cadangan") },
            text = {
                Text(
                    text = "Hapus file cadangan '${target.name}' secara permanen dari folder lokal Anda?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolderBackup(target)
                        selectedBackupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                    modifier = Modifier.testTag("confirm_delete_backup_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackupToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Single Nominal Dialog
    if (editingNominal != null) {
        AlertDialog(
            onDismissRequest = { editingNominal = null },
            title = { Text("Edit Pilihan Nominal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ubah nominal '${editingNominal}' menjadi angka baru (tanpa '000'):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = editNominalValue,
                        onValueChange = { editNominalValue = it.filter { c -> c.isDigit() } },
                        label = { Text("Nominal Baru") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_single_nominal_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    val previewNum = editNominalValue.toLongOrNull()
                    if (previewNum != null && previewNum > 0) {
                        Text(
                            text = "Akan menjadi: ${CurrencyFormatter.formatRupiah(previewNum * 1000L)} saat kasir mengklik tombol ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val old = editingNominal
                        if (old != null && editNominalValue.isNotBlank()) {
                            viewModel.editQuickNominal(old, editNominalValue)
                            editingNominal = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.testTag("confirm_edit_nominal_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNominal = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
