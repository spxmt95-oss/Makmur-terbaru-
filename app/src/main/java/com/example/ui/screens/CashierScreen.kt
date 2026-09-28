package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.CartItem
import com.example.ui.theme.CashTunaiGreen
import com.example.ui.theme.NonTunaiBlue
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.RedDanger
import com.example.ui.viewmodel.MainViewModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val qrInput by viewModel.qrInputText.collectAsStateWithLifecycle()
    val paymentType by viewModel.paymentType.collectAsStateWithLifecycle()
    val nominalPayment by viewModel.nominalPaymentText.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val quickNominals by viewModel.quickNominals.collectAsStateWithLifecycle()

    var showProductPickerSheet by switchableState()
    var pickerSearchQuery by remember { mutableStateOf("") }

    // Scroll state for outer container to support keyboard IME smoothly
    val scrollState = rememberScrollState()

    val parsedNominal = if (paymentType == "NON_TUNAI") cartTotal else CurrencyFormatter.parseAmount(nominalPayment)
    val changeAmount = if (paymentType == "NON_TUNAI") 0L else (parsedNominal - cartTotal).coerceAtLeast(0L)
    val isUnderpaid = paymentType == "TUNAI" && parsedNominal < cartTotal && cartTotal > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Store & QR Scan Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryGreen.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Scan Kode QR / Barcode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Teks otomatis terhapus setelah scan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Button to open visual catalog picker
                    OutlinedButton(
                        onClick = { showProductPickerSheet = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("catalog_picker_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Katalog", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // QR Code Text Input with Done/Enter Action & Auto-Clear
                OutlinedTextField(
                    value = qrInput,
                    onValueChange = { viewModel.updateQrInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_code_input"),
                    placeholder = { Text("Ketik atau scan kode QR produk di sini...") },
                    leadingIcon = {
                        Icon(Icons.Default.QrCode, contentDescription = "Kode QR", tint = PrimaryGreen)
                    },
                    trailingIcon = {
                        if (qrInput.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateQrInput("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.onQrSubmit()
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tekan Tambah atau Enter di keyboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.onQrSubmit() },
                        enabled = qrInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("add_by_qr_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah")
                    }
                }
            }
        }

        // Shopping Cart (Rincian Pesanan Kasir)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keranjang Belanja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${cart.sumOf { it.quantity }}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (cart.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier.testTag("clear_cart_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Kosongkan Keranjang",
                                tint = RedDanger
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))

                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Keranjang masih kosong",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Scan barcode atau klik tombol Katalog untuk memilih produk",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Cart items list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (item in cart) {
                            CartItemRow(
                                item = item,
                                onIncrement = { viewModel.incrementCartItem(item.product.id) },
                                onDecrement = { viewModel.decrementCartItem(item.product.id) },
                                onDelete = { viewModel.removeFromCart(item.product.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Subtotal & Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Belanja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(cartTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }

        // Payment Section (Tunai & Non-Tunai, Nominal Cepat, Uang Pas)
        if (cart.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab selector for Tunai vs Non-Tunai
                    TabRow(
                        selectedTabIndex = if (paymentType == "TUNAI") 0 else 1,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = paymentType == "TUNAI",
                            onClick = { viewModel.setPaymentType("TUNAI") },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalAtm, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tunai (Cash)", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = paymentType == "NON_TUNAI",
                            onClick = { viewModel.setPaymentType("NON_TUNAI") },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Non Tunai (QRIS / Transfer)", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (paymentType == "NON_TUNAI") {
                        // Non Tunai description: nominal equals total exactly
                        Surface(
                            color = NonTunaiBlue.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Pembayaran Non Tunai (QRIS / EDC / Transfer)",
                                    fontWeight = FontWeight.Bold,
                                    color = NonTunaiBlue,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Nominal yang dibayar otomatis sesuai dengan total belanja: ${CurrencyFormatter.formatRupiah(cartTotal)}. Tidak ada kembalian.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Tunai: Dynamic quick nominal buttons ("Uang Pas", "10", "20", etc.)
                        Text(
                            text = "Pilihan Nominal Cepat (Tunai):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val allButtons = remember(quickNominals) {
                            listOf("pas" to "Uang Pas") + quickNominals.map { it to it }
                        }
                        val chunkedRows = remember(allButtons) {
                            allButtons.chunked(3)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chunkedRows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { (value, label) ->
                                        val isPas = value == "pas"
                                        QuickNominalButton(
                                            label = label,
                                            value = value,
                                            isPas = isPas,
                                            modifier = Modifier.weight(if (isPas && rowItems.size < 3) 1.2f else 1f)
                                        ) {
                                            viewModel.onQuickNominalClick(value)
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Editable Nominal Field
                        Text(
                            text = "Nominal Pembayaran Diterima (Bisa diedit):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = nominalPayment,
                            onValueChange = { viewModel.updateNominalPaymentText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cash_nominal_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Text(
                                    "Rp",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (nominalPayment.isNotBlank()) {
                                    IconButton(onClick = { viewModel.updateNominalPaymentText("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                    }
                                }
                            },
                            singleLine = true,
                            isError = isUnderpaid,
                            supportingText = {
                                if (isUnderpaid) {
                                    Text(
                                        text = "Kurang ${CurrencyFormatter.formatRupiah(cartTotal - parsedNominal)}",
                                        color = RedDanger
                                    )
                                } else if (parsedNominal >= cartTotal && cartTotal > 0) {
                                    Text(
                                        text = "Terbilang: ${CurrencyFormatter.formatRupiah(parsedNominal)}",
                                        color = PrimaryGreen
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Kembalian calculation display
                        Surface(
                            color = if (changeAmount > 0) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kembalian:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(changeAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (changeAmount > 0) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Final Checkout Button
                    Button(
                        onClick = { viewModel.processPayment() },
                        enabled = !isUnderpaid && cart.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("process_payment_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentType == "NON_TUNAI") NonTunaiBlue else PrimaryGreen
                        )
                    ) {
                        Text(
                            text = if (paymentType == "NON_TUNAI") "Selesaikan Pembayaran Non Tunai" else "Bayar & Cetak Struk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for browsing product catalog / quick tap to add
    if (showProductPickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showProductPickerSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Produk dari Stok",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showProductPickerSheet = false }) {
                        Icon(Icons.Default.Clear, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pickerSearchQuery,
                    onValueChange = { pickerSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("picker_search_input"),
                    placeholder = { Text("Cari nama produk atau barcode...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val displayedProducts = allProducts.filter { p ->
                    pickerSearchQuery.isBlank() ||
                            p.name.contains(pickerSearchQuery, ignoreCase = true) ||
                            p.barcode.contains(pickerSearchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedProducts) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addToCart(product)
                                    showProductPickerSheet = false
                                }
                                .testTag("picker_product_${product.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Kode: ${product.barcode} • Stok: ${product.stock} ${product.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (product.stock <= product.minStockAlert) RedDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.formatRupiah(product.sellPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.product.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${CurrencyFormatter.formatRupiah(item.product.sellPrice)} x ${item.quantity} = ${CurrencyFormatter.formatRupiah(item.product.sellPrice * item.quantity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quantity controls
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(32.dp).testTag("decrement_${item.product.id}")
                ) {
                    Icon(
                        if (item.quantity > 1) Icons.Default.Remove else Icons.Default.Delete,
                        contentDescription = "Kurang",
                        modifier = Modifier.size(16.dp),
                        tint = if (item.quantity > 1) MaterialTheme.colorScheme.onSurface else RedDanger
                    )
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp).testTag("increment_${item.product.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Direct delete button if cancelled
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_cart_item_${item.product.id}")
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(16.dp), tint = RedDanger)
                }
            }
        }
    }
}

@Composable
fun QuickNominalButton(
    label: String,
    value: String,
    isPas: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(42.dp)
            .testTag("quick_nominal_$value"),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isPas) PrimaryGreen.copy(alpha = 0.12f) else Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPas) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

// Simple state helper
@Composable
private fun switchableState(initial: Boolean = false) = remember { mutableStateOf(initial) }
