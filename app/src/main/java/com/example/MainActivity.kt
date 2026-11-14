package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.DataRepository
import com.example.notifications.ExpiryNotificationHelper
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketsScreen
import com.example.ui.screens.ProductFormScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DataTheme
import com.example.ui.viewmodel.DataViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ExpiryNotificationHelper.ensureChannel(this)
        setContent {
            DataTheme {
                DataAppRoot()
            }
        }
    }
}

private enum class TopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("home", "Início", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    HISTORY("history", "Histórico", Icons.Filled.Archive, Icons.Outlined.Archive, "nav_history"),
    MARKETS("markets", "Mercados", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_markets"),
    SETTINGS("settings", "Config", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

private const val ROUTE_FORM = "product_form"

@Composable
fun DataAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedApp(user = user)
        }
    }
}

@Composable
fun AuthenticatedApp(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DataViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DataViewModel(
                    repository = DataRepository(db),
                    currentUserId = user.uid,
                    appContext = app.applicationContext
                )
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAndRunAutoArchive()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val homeModel by viewModel.homeUiModel.collectAsStateWithLifecycle()
    val historyItems by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val historySearch by viewModel.historySearch.collectAsStateWithLifecycle()
    val historyMarketId by viewModel.historyMarketId.collectAsStateWithLifecycle()
    val historyReasonFilter by viewModel.historyReasonFilter.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopTab = TopTab.entries.any { it.route == currentRoute }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentRoute == TopTab.HOME.route) {
                    FloatingActionButton(
                        onClick = { navController.navigate(ROUTE_FORM) },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_product")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Cadastrar produto"
                        )
                    }
                }
            },
            bottomBar = {
                if (!useRail && isTopTab) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && isTopTab) {
                    NavigationRail {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = TopTab.HOME.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(TopTab.HOME.route) {
                        HomeScreen(
                            model = homeModel,
                            userName = user.displayName,
                            onSelectMarket = viewModel::selectMarket,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onSelectStatusFilter = viewModel::setStatusFilter,
                            onSelectDateFilter = viewModel::setDateFilter,
                            onClearFilters = viewModel::clearAllHomeFilters,
                            onRefresh = viewModel::refreshAndRunAutoArchive,
                            onAddProductClick = { navController.navigate(ROUTE_FORM) },
                            onEditProduct = { productId ->
                                navController.navigate("$ROUTE_FORM?productId=$productId")
                            },
                            onResolveProduct = viewModel::markProductResolved,
                            onArchiveManualProduct = viewModel::archiveProductManually,
                            onDeleteProduct = viewModel::deleteProductPermanently
                        )
                    }

                    composable(TopTab.MARKETS.route) {
                        MarketsScreen(
                            markets = homeModel.markets,
                            activeProducts = homeModel.allActiveProducts,
                            onAddMarket = { name -> viewModel.addMarket(name) },
                            onEditMarket = { id, newName, active ->
                                viewModel.updateMarket(id, newName, active)
                            },
                            onToggleActive = viewModel::toggleMarketActive
                        )
                    }

                    composable(TopTab.HISTORY.route) {
                        HistoryScreen(
                            items = historyItems,
                            markets = homeModel.markets,
                            searchQuery = historySearch,
                            selectedMarketId = historyMarketId,
                            reasonFilter = historyReasonFilter,
                            onSearchChange = viewModel::setHistorySearch,
                            onSelectMarket = viewModel::setHistoryMarketId,
                            onSelectReasonFilter = viewModel::setHistoryReasonFilter,
                            onRestoreProduct = viewModel::restoreProduct,
                            onDeletePermanently = viewModel::deleteProductPermanently
                        )
                    }

                    composable(TopTab.SETTINGS.route) {
                        SettingsScreen(
                            userEmail = user.email,
                            userName = user.displayName,
                            notificationsEnabled = homeModel.notificationsEnabled,
                            onToggleNotifications = viewModel::setNotificationsEnabled,
                            onTestNotification = viewModel::triggerTestNotification,
                            onRunAutoArchiveNow = viewModel::refreshAndRunAutoArchive,
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }

                    composable(
                        route = "$ROUTE_FORM?productId={productId}",
                        arguments = listOf(
                            navArgument("productId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { entry ->
                        val productId = entry.arguments?.getString("productId")
                        val existing = viewModel.findProductById(productId)
                        ProductFormScreen(
                            existingProduct = existing,
                            allMarkets = homeModel.markets,
                            defaultMarketId = homeModel.selectedMarketId,
                            isSaving = isSaving,
                            onSave = { name, market, expiryIso, notes, photo ->
                                viewModel.saveProduct(
                                    editingProductId = existing?.id,
                                    name = name,
                                    market = market,
                                    expiryDateIso = expiryIso,
                                    notes = notes,
                                    photoBase64 = photo,
                                    onSuccess = { navController.popBackStack() }
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
