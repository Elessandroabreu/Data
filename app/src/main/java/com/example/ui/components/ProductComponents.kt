package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExpiryUrgency
import com.example.data.model.ProductItem
import com.example.ui.theme.LocalExpiryPalette
import com.example.util.DateUtils
import com.example.util.ImageUtils

@Composable
fun ProductThumbnail(
    photoBase64: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClickPhoto: (() -> Unit)? = null
) {
    val bitmap = remember(photoBase64) {
        ImageUtils.decodeBase64ToBitmap(photoBase64)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (onClickPhoto != null) Modifier.clickable(onClick = onClickPhoto)
                    else Modifier
                )
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = "Produto sem foto",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun PhotoPreviewDialog(
    photoBase64: String,
    title: String,
    onDismiss: () -> Unit
) {
    val bitmap = remember(photoBase64) { ImageUtils.decodeBase64ToBitmap(photoBase64) }
    if (bitmap == null) {
        onDismiss()
        return
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.ifBlank { "Foto do produto" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar visualização")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
        }
    }
}

@Composable
fun ActiveProductCard(
    product: ProductItem,
    todayIso: String,
    onEdit: () -> Unit,
    onResolveConfirmed: () -> Unit,
    onArchiveManualConfirmed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysUntil = DateUtils.daysUntilExpiry(product.expiryDate, todayIso)
    val urgency = DateUtils.classifyUrgency(product.expiryDate, todayIso)
    val isDueToday = urgency == ExpiryUrgency.DUE_TODAY

    var menuExpanded by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<ConfirmProductAction?>(null) }
    var showPhotoZoom by remember { mutableStateOf(false) }

    val expiryColors = LocalExpiryPalette.current
    val (tintBg, accentColor, textColor) = when (urgency) {
        ExpiryUrgency.EXPIRED_RECENT,
        ExpiryUrgency.SHOULD_AUTO_ARCHIVE -> Triple(
            expiryColors.expiredRecentBg,
            expiryColors.expiredRecentBadge,
            expiryColors.expiredRecentText
        )
        ExpiryUrgency.DUE_TODAY -> Triple(
            expiryColors.dueTodayPillBg,
            expiryColors.dueTodayAccent,
            expiryColors.dueTodayText
        )
        ExpiryUrgency.DUE_IN_3_DAYS -> Triple(
            expiryColors.dueSoonBg,
            expiryColors.dueSoonBadge,
            expiryColors.dueSoonText
        )
        ExpiryUrgency.DUE_IN_7_DAYS -> Triple(
            expiryColors.warningBg,
            expiryColors.warningBadge,
            expiryColors.warningText
        )
        else -> Triple(
            expiryColors.safeBg,
            expiryColors.safeBadge,
            expiryColors.safeText
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "due_today_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_rotation"
    )
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_blink"
    )

    val rotatingContourBrush = remember(rotationAngle, expiryColors) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val shader = SweepGradient(
                    cx,
                    cy,
                    intArrayOf(
                        expiryColors.dueTodayAccent.toArgb(),
                        expiryColors.dueTodayGlow.toArgb(),
                        Color(0xFFFECACA).toArgb(),
                        expiryColors.dueTodayAccent.toArgb()
                    ),
                    null
                )
                val matrix = Matrix()
                matrix.postRotate(rotationAngle, cx, cy)
                shader.setLocalMatrix(matrix)
                return shader
            }
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    val cardModifier = if (isDueToday) {
        modifier
            .fillMaxWidth()
            .clip(cardShape)
            .drawWithContent {
                drawContent()
                val strokePx = 3.dp.toPx()
                val half = strokePx / 2f
                val radiusPx = (20.dp.toPx() - half).coerceAtLeast(0f)
                // Base sutil para manter o perímetro sempre evidente
                drawRoundRect(
                    color = expiryColors.dueTodayAccent.copy(alpha = 0.22f),
                    topLeft = Offset(half, half),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokePx)
                )
                // Contorno vermelho tracejado + gradiente girando exatamente no contorno do card
                drawRoundRect(
                    brush = rotatingContourBrush,
                    topLeft = Offset(half, half),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 12f), dashPhase)
                    )
                )
            }
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        modifier = cardModifier.testTag("product_card_${product.id}"),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = if (isDueToday) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = if (isDueToday) 2.dp else 0.dp
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral semântica de urgência alinhada ao card
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    ProductThumbnail(
                        photoBase64 = product.photoBase64,
                        contentDescription = product.name.ifBlank { "Foto do produto" },
                        modifier = Modifier.size(76.dp),
                        onClickPhoto = if (!product.photoBase64.isNullOrBlank()) {
                            { showPhotoZoom = true }
                        } else null
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = product.name.ifBlank { "Produto registrado por foto" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 2.dp)
                            )

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("product_more_${product.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Mais ações do produto",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Editar dados") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onEdit()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Arquivar manualmente") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Archive, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            confirmAction = ConfirmProductAction.ARCHIVE_MANUAL
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Excluir definitivamente",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            confirmAction = ConfirmProductAction.DELETE
                                        }
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = product.marketName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${DateUtils.isoToBrDisplay(product.expiryDate)} • ${
                                    DateUtils.formatRemainingDaysLabel(daysUntil)
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Selo de status em texto + cor (sem depender só da cor)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isDueToday) expiryColors.dueTodayAccent else tintBg,
                            modifier = if (isDueToday) Modifier.alpha(pulseAlpha) else Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isDueToday) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Alerta de vencimento hoje",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Vence hoje • É deste dia!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = DateUtils.urgencyStatusTitle(urgency),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }

                        if (product.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = product.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("edit_product_${product.id}"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { confirmAction = ConfirmProductAction.RESOLVE },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1.25f)
                            .height(48.dp)
                            .testTag("resolve_product_${product.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolver", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showPhotoZoom && !product.photoBase64.isNullOrBlank()) {
        PhotoPreviewDialog(
            photoBase64 = product.photoBase64,
            title = product.name,
            onDismiss = { showPhotoZoom = false }
        )
    }

    confirmAction?.let { action ->
        val (title, body, confirmLabel) = when (action) {
            ConfirmProductAction.RESOLVE -> Triple(
                "Marcar como resolvido?",
                "O produto sairá da lista ativa e ficará salvo no histórico para consulta ou restauração.",
                "Sim, resolver"
            )
            ConfirmProductAction.ARCHIVE_MANUAL -> Triple(
                "Arquivar manualmente?",
                "O produto será movido para o histórico como arquivado manualmente e poderá ser restaurado depois.",
                "Sim, arquivar"
            )
            ConfirmProductAction.DELETE -> Triple(
                "Excluir definitivamente?",
                "Essa ação apaga o registro permanentemente e não poderá ser desfeita.",
                "Excluir"
            )
        }
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction = null
                        when (action) {
                            ConfirmProductAction.RESOLVE -> onResolveConfirmed()
                            ConfirmProductAction.ARCHIVE_MANUAL -> onArchiveManualConfirmed()
                            ConfirmProductAction.DELETE -> onDeleteConfirmed()
                        }
                    },
                    colors = if (action == ConfirmProductAction.DELETE) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private enum class ConfirmProductAction {
    RESOLVE,
    ARCHIVE_MANUAL,
    DELETE
}
