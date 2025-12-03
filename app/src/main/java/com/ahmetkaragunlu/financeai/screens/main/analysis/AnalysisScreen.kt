package com.ahmetkaragunlu.financeai.screens.main.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.utils.formatAsCurrency
import com.ahmetkaragunlu.financeai.utils.toResId
import com.ahmetkaragunlu.financeai.viewmodel.BudgetUiState
import com.ahmetkaragunlu.financeai.viewmodel.BudgetViewModel
import com.ahmetkaragunlu.financeai.viewmodel.CategoryBudgetState
import com.ahmetkaragunlu.financeai.viewmodel.GeneralBudgetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // BottomSheet Gösterim Durumu ve Tipi
    var showBottomSheet by remember { mutableStateOf(false) }
    var initialBottomSheetType by remember { mutableStateOf(BudgetType.GENERAL_MONTHLY) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background))
    ) {
        when {
            // Yükleniyor durumu (Opsiyonel, veri hızlı gelirse kullanıcı fark etmez)
            uiState.isBudgetEmpty -> {
                // Veri Yok -> BOŞ TASARIM
                EmptyAnalysisContent(
                    onAddLimitClick = {
                        initialBottomSheetType = BudgetType.GENERAL_MONTHLY // İlk açılışta Genel ile başlat
                        showBottomSheet = true
                    }
                )
            }
            else -> {
                // Veri Var -> DOLU TASARIM
                FilledAnalysisContent(
                    uiState = uiState,
                    onAddLimitClick = {
                        initialBottomSheetType = BudgetType.CATEGORY_AMOUNT // Yeni eklerken Kategori ile başlat
                        showBottomSheet = true
                    },
                    onEditBudgetClick = {
                        initialBottomSheetType = BudgetType.GENERAL_MONTHLY // Düzenlerken Genel ile başlat
                        showBottomSheet = true
                    }
                )
            }
        }

        // --- BOTTOM SHEET (EKLEME & DÜZENLEME PENCERESİ) ---
        if (showBottomSheet) {
            AddBudgetBottomSheet(
                initialType = initialBottomSheetType,
                onDismiss = { showBottomSheet = false },
                onSave = { type, amount, category, percentage ->
                    viewModel.addBudgetRule(type, amount, category, percentage)
                    showBottomSheet = false
                }
            )
        }
    }
}

// ==========================================
// 1. DOLU DURUM TASARIMI (Filled Content)
// ==========================================

@Composable
private fun FilledAnalysisContent(
    uiState: BudgetUiState,
    onAddLimitClick: () -> Unit,
    onEditBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // General Budget Card
        uiState.generalBudgetState?.let { state ->
            GeneralBudgetCardFilled(state = state, onEditClick = onEditBudgetClick)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Warning Card
        uiState.aiWarningMessage?.let { message ->
            AIWarningCard(message = message)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section Title
        Text(
            text = stringResource(R.string.category_budgets),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category Budget List
        CategoryBudgetList(categories = uiState.categoryBudgetStates)

        Spacer(modifier = Modifier.height(20.dp))

        // Add New Limit Button
        AddLimitButton(onClick = onAddLimitClick)

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun GeneralBudgetCardFilled(
    state: GeneralBudgetState,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFb55ebf), Color(0xFF36a2cc))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tomorrow),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.edit), color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                Text(
                    text = state.remainingAmount.formatAsCurrency(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.remaining_budget),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Income and Expense Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.plus_income),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${state.incomeAmount.formatAsCurrency()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.minus_expense),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${state.expenseAmount.formatAsCurrency()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFEF5350),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (state.progress > 1f) Color(0xFFEF5350) else Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%${(state.progress * 100).toInt()} Harcandı",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetList(categories: List<CategoryBudgetState>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { categoryState ->
            val progressColor = when {
                categoryState.isOverBudget -> Color(0xFFEF5350) // Kırmızı
                categoryState.progress > 0.8f -> Color(0xFFFF9800) // Turuncu
                else -> Color(0xFF4CAF50) // Yeşil
            }

            CategoryBudgetCard(
                categoryName = stringResource(id = categoryState.category.toResId()),
                limit = categoryState.limitAmount.formatAsCurrency(),
                spent = categoryState.spentAmount.formatAsCurrency(),
                progress = categoryState.progress,
                progressColor = progressColor,
                percentage = categoryState.percentageUsed.toString(),
                isOverBudget = categoryState.isOverBudget,
                isCompleted = categoryState.progress >= 1f
            )
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    categoryName: String,
    limit: String,
    spent: String,
    progress: Float,
    progressColor: Color,
    percentage: String,
    isOverBudget: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3b4351), Color(0xFF2d3139))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kategori Harf İkonu
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = categoryName.take(1),
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$spent / $limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%$percentage",
                            style = MaterialTheme.typography.titleMedium,
                            color = progressColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (isOverBudget) {
                            Text(text = "⚠️", fontSize = 12.sp)
                        } else if (isCompleted) {
                            Text(text = "✓", fontSize = 12.sp, color = progressColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = progressColor,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

// ==========================================
// 2. BOŞ DURUM TASARIMI (Empty Content)
// ==========================================

@Composable
private fun EmptyAnalysisContent(
    onAddLimitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GeneralBudgetCardEmpty(onEditClick = onAddLimitClick)
        AIWelcomeCard()

        Text(
            text = stringResource(R.string.category_budgets),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        EmptyStateContent()

        // Büyük Ekleme Butonu
        Button(
            onClick = onAddLimitClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFb55ebf), Color(0xFF36a2cc))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_new_limit),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GeneralBudgetCardEmpty(
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(colors = listOf(Color(0xFFb55ebf), Color(0xFF36a2cc))),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.monthly_budget),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    // Küçük Edit Butonu (Boşken de çalışır)
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.create),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.not_set),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = stringResource(R.string.set_monthly_budget_to_start),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun AIWelcomeCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(colors = listOf(Color(0xFFF6D365), Color(0xFFFDA085))),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ai_suggestion),
                    contentDescription = null,
                    tint = Color(0xFF5D4037)
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = stringResource(R.string.ai_assistant),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.ai_welcome_message),
                        color = Color(0xFF5D4037).copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp)
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "💰", fontSize = 80.sp, modifier = Modifier.padding(bottom = 20.dp))
        Text(
            text = stringResource(R.string.no_budget_rules_yet),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.create_first_budget_rule_description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}

@Composable
private fun AIWarningCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFC92A2A))),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⚠️", fontSize = 24.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.attention_budget_exceeded),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AddLimitButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp)
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "+", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.add_new_limit),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==========================================
// BOTTOM SHEET (VERİ GİRİŞ PENCERESİ)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetBottomSheet(
    initialType: BudgetType,
    onDismiss: () -> Unit,
    onSave: (BudgetType, Double, CategoryType?, Double?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // STATE
    var selectedType by remember { mutableStateOf(initialType) }
    var inputAmount by remember { mutableStateOf("") }
    var inputPercentage by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryType?>(null) }

    val expenseCategories = remember {
        CategoryType.entries.filter { it.type == TransactionType.EXPENSE }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorResource(R.color.background),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_new_limit),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 1. TİP SEÇİCİ ---
            TypeSelector(
                selectedType = selectedType,
                onTypeSelected = {
                    selectedType = it
                    inputAmount = ""
                    inputPercentage = ""
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. KATEGORİ SEÇİMİ ---
            if (selectedType != BudgetType.GENERAL_MONTHLY) {
                Text(
                    text = "Kategori Seçin", // strings.xml'de yoksa direkt metin
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                CategoryDropdown(
                    categories = expenseCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- 3. DEĞER GİRİŞİ ---
            if (selectedType == BudgetType.CATEGORY_PERCENTAGE) {
                OutlinedTextField(
                    value = inputPercentage,
                    onValueChange = { if (it.length <= 3) inputPercentage = it.filter { char -> char.isDigit() } },
                    label = { Text("Yüzde (%)") },
                    suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF36a2cc),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            } else {
                OutlinedTextField(
                    value = inputAmount,
                    onValueChange = { inputAmount = it },
                    label = { Text("Tutar (₺)") },
                    suffix = { Text("₺") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF36a2cc),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 4. KAYDET ---
            Button(
                onClick = {
                    val amount = inputAmount.toDoubleOrNull() ?: 0.0
                    val percentage = inputPercentage.toDoubleOrNull()

                    val isValid = when (selectedType) {
                        BudgetType.GENERAL_MONTHLY -> amount > 0
                        BudgetType.CATEGORY_AMOUNT -> amount > 0 && selectedCategory != null
                        BudgetType.CATEGORY_PERCENTAGE -> (percentage ?: 0.0) > 0 && selectedCategory != null
                    }

                    if (isValid) {
                        onSave(selectedType, amount, selectedCategory, percentage)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFb55ebf), Color(0xFF36a2cc))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TypeSelector(
    selectedType: BudgetType,
    onTypeSelected: (BudgetType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(Color(0xFF2A2D35), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val types = listOf(
            Triple(BudgetType.GENERAL_MONTHLY, "Genel", "🏠"),
            Triple(BudgetType.CATEGORY_AMOUNT, "Kategori", "🏷️"),
            Triple(BudgetType.CATEGORY_PERCENTAGE, "Yüzde %", "📊")
        )

        types.forEach { (type, label, _) ->
            val isSelected = selectedType == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFF404349) else Color.Transparent)
                    .clickable { onTypeSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<CategoryType>,
    selectedCategory: CategoryType?,
    onCategorySelected: (CategoryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory?.let { stringResource(it.toResId()) } ?: "Seçiniz...",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = if (selectedCategory == null) Color.Gray else Color.White,
                focusedBorderColor = Color(0xFF36a2cc),
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2A2D35))
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(category.toResId()),
                            color = Color.White
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}