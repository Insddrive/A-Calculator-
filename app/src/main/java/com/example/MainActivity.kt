package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalculatorTab
import com.example.ui.CalculatorViewModel
import com.example.ui.CustomIcons
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorApp(viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorApp(viewModel: CalculatorViewModel) {
    val context = LocalContext.current
    val tabs by viewModel.allTabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeTokenIdx by viewModel.activeTokenIndex.collectAsStateWithLifecycle()
    
    val activeTab = tabs.find { it.id == activeTabId } ?: CalculatorTab(id = activeTabId)
    val liveResult = viewModel.getLiveResult(activeTab.expression)
    val tokens = viewModel.tokenizeExpression(activeTab.expression)

    var keypadVisible by remember { mutableStateOf(true) }
    var dragAmountY by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Base colors matching the original PWA CSS style
    val bgColor = Color(0xFF000000)
    val orangeColor = Color(0xFFF07041)
    val grayColor = Color(0xFF888888)
    val tabBorderColor = Color(0xFF333333)
    val highlightColor = Color(0x5962AEEF) // rgba(98, 174, 239, 0.35)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(innerPadding)
        ) {
            // Header: horizontal scroll tabs + '+' add button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable tab items list
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tabs_list")
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabs) { tab ->
                        val isActive = tab.id == activeTabId
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isActive) orangeColor else bgColor)
                                .clickable { viewModel.selectTab(tab.id) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.id.toString(),
                                color = if (isActive) Color.Black else Color(0xFF999999),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Standard '+' Add tab button in circle border
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { viewModel.addTab() }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Calculator Tab",
                        tint = orangeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Display Area (Flexible height)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.deselectToken()
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAmountY = 0f },
                            onDragEnd = {
                                if (dragAmountY > 60f) {
                                    keypadVisible = false // swipe down -> hide
                                } else if (dragAmountY < -60f) {
                                    keypadVisible = true // swipe up -> show
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAmountY += dragAmount
                            }
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // History area (scrolls freely)
                val histScrollState = rememberScrollState()
                LaunchedEffect(activeTab.history) {
                    histScrollState.scrollTo(histScrollState.maxValue)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(histScrollState)
                            .clickable { viewModel.restoreActiveTabHistory() }
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = activeTab.history,
                            color = Color(0xFF666666),
                            fontSize = 20.sp,
                            textAlign = TextAlign.End,
                            maxLines = 10,
                            overflow = TextOverflow.Clip
                        )
                    }
                }

                // Dynamic input sizing
                val fullTextStr = tokens.sumOf { viewModel.formatNumber(it).length }
                val fontSize = when {
                    fullTextStr <= 6 -> 64.sp
                    fullTextStr <= 10 -> 52.sp
                    fullTextStr <= 16 -> 42.sp
                    fullTextStr <= 24 -> 34.sp
                    fullTextStr <= 34 -> 30.sp
                    else -> 26.sp
                }

                // Input container (scrolling horizontally for extreme equations)
                val inputRowScrollState = rememberScrollState()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        tokens.forEachIndexed { idx, token ->
                            val isSelected = idx == activeTokenIdx
                            val isOp = token in listOf("+", "-", "*", "/")
                            val displayText = when (token) {
                                "*" -> "×"
                                "/" -> "÷"
                                else -> viewModel.formatNumber(token)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) highlightColor else Color.Transparent)
                                    .clickable { viewModel.selectToken(idx) }
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    color = if (isOp) orangeColor else Color.White,
                                    fontSize = fontSize,
                                    fontWeight = if (isOp) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Live calculated result row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { viewModel.pressButton("=") },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (liveResult.isNotEmpty()) {
                        Text(
                            text = liveResult,
                            color = grayColor,
                            fontSize = 32.sp,
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Interactive Keyboard Area
            AnimatedVisibility(
                visible = keypadVisible,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                // Keypad height takes roughly 55% of full vertical space
                val ops = listOf("+", "-", "*", "/")
                val currentToken = activeTokenIdx?.let { tokens.getOrNull(it) }
                val isEditingOp = currentToken != null && currentToken in ops
                val isEditingNumber = currentToken != null && currentToken !in ops

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Clear, Backspace, Percent, Division
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Clear button: AC (triggers alert if user holds, plain click resets current)
                        KeypadButton(
                            text = "C",
                            modifier = Modifier.weight(1f),
                            textColor = orangeColor,
                            onClick = {
                                showConfirmation(context, "ਕੀ ਤੁਸੀਂ ਸਭ ਕੁਝ ਡਿਲੀਟ ਕਰਨਾ ਚਾਹੁੰਦੇ ਹੋ?") {
                                    viewModel.pressButton("AC")
                                }
                            }
                        )

                        // Backspace vector button
                        KeypadIconButton(
                            icon = CustomIcons.Backspace,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.pressButton("BACK") }
                        )

                        KeypadButton(
                            text = "%",
                            modifier = Modifier.weight(1f),
                            textColor = orangeColor,
                            enabled = !isEditingOp,
                            onClick = { viewModel.pressButton("%") }
                        )

                        KeypadButton(
                            text = "÷",
                            modifier = Modifier.weight(1f),
                            textColor = orangeColor,
                            enabled = !isEditingNumber,
                            onClick = { viewModel.pressButton("/") }
                        )
                    }

                    // Row 2: 7, 8, 9, Multiplication
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeypadButton(text = "7", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("7") })
                        KeypadButton(text = "8", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("8") })
                        KeypadButton(text = "9", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("9") })
                        KeypadButton(text = "×", modifier = Modifier.weight(1f), textColor = orangeColor, enabled = !isEditingNumber, onClick = { viewModel.pressButton("*") })
                    }

                    // Row 3: 4, 5, 6, Subtraction
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeypadButton(text = "4", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("4") })
                        KeypadButton(text = "5", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("5") })
                        KeypadButton(text = "6", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("6") })
                        KeypadButton(text = "-", modifier = Modifier.weight(1f), textColor = orangeColor, enabled = !isEditingNumber, onClick = { viewModel.pressButton("-") })
                    }

                    // Row 4: 1, 2, 3, Addition
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeypadButton(text = "1", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("1") })
                        KeypadButton(text = "2", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("2") })
                        KeypadButton(text = "3", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("3") })
                        KeypadButton(text = "+", modifier = Modifier.weight(1f), textColor = orangeColor, enabled = !isEditingNumber, onClick = { viewModel.pressButton("+") })
                    }

                    // Row 5: Reset All, Delete Tab, 0, Dot, Equal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Master Reset Trash button
                        KeypadIconButton(
                            icon = CustomIcons.Trash,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                showConfirmation(context, "ਕੀ ਤੁਸੀਂ ਸਭ ਕੁਝ ਡਿਲੀਟ ਕਰਨਾ ਚਾਹੁੰਦੇ ਹੋ?") {
                                    viewModel.masterReset()
                                }
                            }
                        )

                        // Delete active tab button in orange minus outline circle
                        KeypadIconButton(
                            icon = CustomIcons.DeleteTab,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (tabs.size > 1) {
                                    showConfirmation(context, "ਕੀ ਤੁਸੀਂ ਇਹ ਕੈਲਕੁਲੇਟਰ ਡਿਲੀਟ ਕਰਨਾ ਚਾਹੁੰਦੇ ਹੋ?") {
                                        viewModel.deleteActiveTab()
                                    }
                                }
                            }
                        )

                        KeypadButton(text = "0", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton("0") })
                        KeypadButton(text = ".", modifier = Modifier.weight(1f), enabled = !isEditingOp, onClick = { viewModel.pressButton(".") })
                        
                        // Equal action button changes color/checkmark style depending on editing mode
                        val isEditing = activeTokenIdx != null
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isEditing) Color(0xFF62AEEF) else orangeColor)
                                .clickable { viewModel.pressButton("=") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEditing) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save edit change",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Text(
                                    text = "=",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Confirmation helper alert matching original PWA dialog messages
fun showConfirmation(context: Context, message: String, onConfirm: () -> Unit) {
    android.app.AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setMessage(message)
        .setPositiveButton("ਹਾਂ") { _, _ -> onConfirm() }
        .setNegativeButton("ਨਾ", null)
        .create()
        .show()
}

@Composable
fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .alpha(if (enabled) 1f else 0.25f)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (text == "C") 24.sp else 32.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .alpha(if (enabled) 1f else 0.25f)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Unspecified, // use custom vectors' intrinsic colors
            modifier = Modifier.size(28.dp)
        )
    }
}
