package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.Market
import com.example.data.model.ProductItem
import com.example.ui.components.ProductThumbnail
import com.example.util.DateUtils
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    existingProduct: ProductItem?,
    allMarkets: List<Market>,
    defaultMarketId: String?,
    isSaving: Boolean,
    onSave: (
        name: String,
        market: Market,
        expiryIso: String,
        notes: String,
        photoBase64: String?
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val availableMarkets = remember(allMarkets, existingProduct) {
        val active = allMarkets.filter { it.active }
        if (existingProduct != null && active.none { it.id == existingProduct.marketId }) {
            val currentMarket = allMarkets.firstOrNull { it.id == existingProduct.marketId }
            if (currentMarket != null) listOf(currentMarket) + active else active
        } else {
            active
        }
    }

    var selectedMarket by remember(existingProduct, availableMarkets, defaultMarketId) {
        mutableStateOf(
            when {
                existingProduct != null ->
                    availableMarkets.firstOrNull { it.id == existingProduct.marketId }
                        ?: availableMarkets.firstOrNull()
                defaultMarketId != null ->
                    availableMarkets.firstOrNull { it.id == defaultMarketId }
                        ?: availableMarkets.firstOrNull()
                else -> availableMarkets.firstOrNull()
            }
        )
    }

    var name by remember(existingProduct) { mutableStateOf(existingProduct?.name ?: "") }
    var expiryIso by remember(existingProduct) {
        mutableStateOf(existingProduct?.expiryDate ?: "")
    }
    var typedBrDate by remember(existingProduct) {
        mutableStateOf(
            existingProduct?.expiryDate?.let { DateUtils.isoToBrDisplay(it) } ?: ""
        )
    }
    var showManualDateInput by remember { mutableStateOf(false) }
    var notes by remember(existingProduct) { mutableStateOf(existingProduct?.notes ?: "") }
    var photoBase64 by remember(existingProduct) { mutableStateOf(existingProduct?.photoBase64) }

    var isProcessingPhoto by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMarketSheet by remember { mutableStateOf(false) }
    var showCameraRationale by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun processImageUri(uri: Uri) {
        isProcessingPhoto = true
        formError = null
        scope.launch {
            val result = ImageUtils.compressUriToBase64(context, uri)
            isProcessingPhoto = false
            result.onSuccess { encoded ->
                photoBase64 = encoded
            }.onFailure { err ->
                formError = err.localizedMessage ?: "Não foi possível carregar a foto."
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            processImageUri(uri)
        }
    }

    fun launchCameraSafely() {
        try {
            val uri = ImageUtils.createCameraTempUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            pendingCameraUri = null
            formError = "Não foi possível abrir a câmera neste aparelho. Use a galeria."
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraSafely()
        } else {
            formError = "Permissão de câmera negada. Você pode escolher uma foto da galeria ou cadastrar pelo nome."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processImageUri(uri)
        }
    }

    fun addDaysToTodayIso(days: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            val today = DateUtils.todayIso()
            timeInMillis = DateUtils.isoToUtcMidnightMillis(today) ?: System.currentTimeMillis()
            add(Calendar.DAY_OF_MONTH, days)
        }
        return DateUtils.utcMillisToIso(cal.timeInMillis)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingProduct == null) "Cadastrar produto" else "Editar produto",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (formError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // 1. Foto do produto (opcional)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Foto do produto (opcional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Com foto, o nome fica opcional. Depois da foto, preencha a validade abaixo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ProductThumbnail(
                                    photoBase64 = photoBase64,
                                    contentDescription = "Pré-visualização da foto do produto",
                                    modifier = Modifier.size(84.dp)
                                )
                                if (isProcessingPhoto) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (granted) {
                                                launchCameraSafely()
                                            } else {
                                                showCameraRationale = true
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("take_photo_button"),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Câmera")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            } catch (_: Exception) {
                                                formError = "Não foi possível abrir a galeria neste aparelho."
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("gallery_photo_button"),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Galeria")
                                    }
                                }

                                if (!photoBase64.isNullOrBlank()) {
                                    TextButton(
                                        onClick = { photoBase64 = null },
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Remover foto",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Data de vencimento (obrigatória)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Data de vencimento *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showManualDateInput = !showManualDateInput }
                            ) {
                                Text(if (showManualDateInput) "Usar calendário" else "Digitar data")
                            }
                        }

                        if (showManualDateInput) {
                            OutlinedTextField(
                                value = typedBrDate,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(8)
                                    typedBrDate = when {
                                        digits.length <= 2 -> digits
                                        digits.length <= 4 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
                                        else -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
                                    }
                                    val parsed = DateUtils.brDisplayToIso(typedBrDate)
                                    if (parsed != null) {
                                        expiryIso = parsed
                                        formError = null
                                    }
                                },
                                label = { Text("Validade (DD/MM/AAAA)") },
                                placeholder = { Text("Ex: 18/11/2026") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expiry_manual_input")
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clickable { showDatePicker = true }
                                    .testTag("expiry_picker_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (expiryIso.isNotBlank()) {
                                                DateUtils.isoToBrDisplay(expiryIso)
                                            } else {
                                                "Selecionar data (DD/MM/AAAA)"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "Alterar",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val todayIso = addDaysToTodayIso(0)
                            val plus3Iso = addDaysToTodayIso(3)
                            val plus7Iso = addDaysToTodayIso(7)
                            FilterChip(
                                selected = expiryIso == todayIso,
                                onClick = {
                                    expiryIso = todayIso
                                    typedBrDate = DateUtils.isoToBrDisplay(todayIso)
                                    formError = null
                                },
                                label = { Text("Hoje") }
                            )
                            FilterChip(
                                selected = expiryIso == plus3Iso,
                                onClick = {
                                    expiryIso = plus3Iso
                                    typedBrDate = DateUtils.isoToBrDisplay(plus3Iso)
                                    formError = null
                                },
                                label = { Text("+3 dias") }
                            )
                            FilterChip(
                                selected = expiryIso == plus7Iso,
                                onClick = {
                                    expiryIso = plus7Iso
                                    typedBrDate = DateUtils.isoToBrDisplay(plus7Iso)
                                    formError = null
                                },
                                label = { Text("+7 dias") }
                            )
                        }
                    }
                }

                // 3. Mercado (obrigatório via ModalBottomSheet confortável)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Mercado *",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (availableMarkets.isEmpty()) {
                            Text(
                                text = "Nenhum mercado ativo disponível. Ative ou cadastre um mercado na aba Mercados.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable { showMarketSheet = true }
                                    .testTag("form_market_selector")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = selectedMarket?.name ?: "Escolher mercado",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        contentDescription = "Abrir lista de mercados",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Nome e Observações
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        formError = null
                    },
                    label = {
                        Text(
                            if (photoBase64.isNullOrBlank()) "Nome do produto *"
                            else "Nome do produto (opcional com foto)"
                        )
                    },
                    placeholder = { Text("Ex: Iogurte Natural Integral 170g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    placeholder = { Text("Ex: Corredor de frios, lote 24, 5 unidades") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_notes_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val finalIso = if (showManualDateInput) {
                            DateUtils.brDisplayToIso(typedBrDate) ?: ""
                        } else {
                            expiryIso
                        }
                        val chosenMarket = selectedMarket
                        val cleanName = name.trim()
                        val hasPhoto = !photoBase64.isNullOrBlank()

                        when {
                            chosenMarket == null ->
                                formError = "Selecione um mercado para o produto."
                            finalIso.isBlank() ->
                                formError = "Informe uma data de vencimento válida no formato DD/MM/AAAA."
                            cleanName.isEmpty() && !hasPhoto ->
                                formError = "Informe o nome do produto ou adicione uma foto."
                            else -> {
                                formError = null
                                onSave(
                                    cleanName,
                                    chosenMarket,
                                    finalIso,
                                    notes.trim(),
                                    photoBase64
                                )
                            }
                        }
                    },
                    enabled = !isSaving && !isProcessingPhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_product_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Salvando...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (existingProduct == null) "Salvar produto" else "Salvar alterações",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showMarketSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMarketSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Escolha o mercado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableMarkets, key = { it.id }) { market ->
                        val isSelected = selectedMarket?.id == market.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMarket = market
                                    showMarketSheet = false
                                    formError = null
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = market.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = expiryIso.takeIf { it.isNotBlank() }?.let {
            DateUtils.isoToUtcMidnightMillis(it)
        } ?: DateUtils.isoToUtcMidnightMillis(DateUtils.todayIso())

        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val iso = DateUtils.utcMillisToIso(millis)
                            expiryIso = iso
                            typedBrDate = DateUtils.isoToBrDisplay(iso)
                            formError = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCameraRationale) {
        AlertDialog(
            onDismissRequest = { showCameraRationale = false },
            title = { Text("Permitir uso da câmera") },
            text = {
                Text("A câmera é usada apenas para fotografar os produtos alimentícios que você está cadastrando na prateleira.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraRationale = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Permitir câmera")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraRationale = false }) {
                    Text("Agora não")
                }
            }
        )
    }
}
