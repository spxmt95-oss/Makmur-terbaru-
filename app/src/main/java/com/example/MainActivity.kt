package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.Screen
import com.example.ui.screens.CapitalExpenseScreen
import com.example.ui.screens.CashierScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReceiptDialog
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.RedDanger
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = false) {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Cashier) }
                val snackbarHostState = remember { SnackbarHostState() }
                val lastReceipt by viewModel.lastCompletedReceipt.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val cart by viewModel.cart.collectAsStateWithLifecycle()
                val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()

                val storeName = settings?.storeName ?: "TOKO MAKMUR"
                val storeAddress = settings?.storeAddress ?: "Jl. Kembang Kuning No.17, Surabaya"

                // Handle back press: if on other screen, go back to Cashier
                BackHandler(enabled = currentScreen != Screen.Cashier) {
                    currentScreen = Screen.Cashier
                }

                // Handle ViewModel UI messages
                LaunchedEffect(Unit) {
                    viewModel.uiMessage.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.statusBars,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = storeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = storeAddress,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = PrimaryGreen,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            Screen.bottomNavItems.forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        if (screen == Screen.Cashier && cart.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = PrimaryGreen) {
                                                        Text("${cart.sumOf { it.quantity }}")
                                                    }
                                                }
                                            ) {
                                                Icon(screen.icon, contentDescription = screen.title)
                                            }
                                        } else if (screen == Screen.Products && lowStockCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = RedDanger) {
                                                        Text("$lowStockCount")
                                                    }
                                                }
                                            ) {
                                                Icon(screen.icon, contentDescription = screen.title)
                                            }
                                        } else {
                                            Icon(screen.icon, contentDescription = screen.title)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryGreen,
                                        selectedTextColor = PrimaryGreen,
                                        indicatorColor = PrimaryGreen.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier.testTag("nav_${screen.route}")
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ScreenTransition"
                        ) { target ->
                            when (target) {
                                Screen.Cashier -> CashierScreen(viewModel = viewModel)
                                Screen.Products -> ProductsScreen(viewModel = viewModel)
                                Screen.Transactions -> TransactionsScreen(viewModel = viewModel)
                                Screen.Reports -> ReportsScreen(viewModel = viewModel)
                                Screen.CapitalExpense -> CapitalExpenseScreen(viewModel = viewModel)
                                Screen.Settings -> SettingsScreen(viewModel = viewModel)
                            }
                        }

                        // Struk Belanja Popup Dialog (when payment succeeds or viewed from history)
                        if (lastReceipt != null) {
                            ReceiptDialog(
                                receipt = lastReceipt!!,
                                onDismiss = { viewModel.dismissReceipt() }
                            )
                        }
                    }
                }
            }
        }
    }
}
