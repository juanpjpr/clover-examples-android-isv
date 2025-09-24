package ar.com.fiserv.clover_isv

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ar.com.fiserv.clover_isv.ui.theme.CloverisvTheme
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.payments.RegionalExtras

// Colores de Clover
val CloverGreen = Color(0xFF43B02A)
val CloverDarkGreen = Color(0xFF388E1E)
val CloverLightGray = Color(0xFFF5F5F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CloverisvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CloverLightGray
                ) {
                    PaymentScreen(
                        onPayClick = { amount, extras -> /* lógica de pago */ },
                        onRetrieveClick = { /* recuperar pago */ },
                        onSendMessageClick = { /* enviar mensaje */ },
                        onCloseoutBatchClick = { /* cierre de lote */ },
                        isLoading = false
                    )
                }
            }
        }
    }
}

// =================================================================================
// DATA CLASS PARA KEY-VALUE DINÁMICO
// =================================================================================
data class KeyValuePair(
    val id: Int,
    val key: String = "",
    val value: String = "",
    val enabled: Boolean = true
)

// =================================================================================
// COMPOSABLE PRINCIPAL
// =================================================================================
@Composable
fun PaymentScreen(
    onPayClick: (Long, Map<String, String>) -> Unit,
    onRetrieveClick: () -> Unit,
    onSendMessageClick: () -> Unit,
    onCloseoutBatchClick: () -> Unit,
    isLoading: Boolean
) {
    KeepScreenOn()
    var amountText by remember { mutableStateOf("") }

    // --- ESTADOS PARA EXTRAS REGIONALES ---
    var fiscalInvoiceEnabled by remember { mutableStateOf(false) }
    var fiscalInvoiceNumber by remember { mutableStateOf("") }
    var installmentEnabled by remember { mutableStateOf(false) }
    var installmentNumber by remember { mutableStateOf("") }
    var cashbackEnabled by remember { mutableStateOf(false) }
    var cashbackAmount by remember { mutableStateOf("") }
    var isQrPayment by remember { mutableStateOf(false) }
    var businessIdEnabled by remember { mutableStateOf(false) }
    var businessId by remember { mutableStateOf("") }
    var subMerchantEnabled by remember { mutableStateOf(false) }
    var subMerchant by remember { mutableStateOf("") }
    var dynamicMerchantNameEnabled by remember { mutableStateOf(false) }
    var dynamicMerchantName by remember { mutableStateOf("") }

    // --- LISTA OBSERVABLE DE KEY-VALUE DINÁMICO ---
    val keyValueList = remember { mutableStateListOf(KeyValuePair(0)) }
    var nextId by remember { mutableStateOf(1) }

    fun getAmountLong(): Long = amountText.toLongOrNull() ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- MONTO ---
        Text(
            text = "$${amountText.ifEmpty { "0" }}",
            style = MaterialTheme.typography.headlineMedium,
            color = CloverDarkGreen,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp)
        )

        // --- NUMPAD ---
        NumberPad(
            onNumberClick = { digit ->
                if (amountText.length < 9) amountText += digit
            },
            onDeleteClick = { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) },
            onAcceptClick = {
                val amount = getAmountLong()
                if (amount > 0) {
                    val extras = buildMap {
                        if (fiscalInvoiceEnabled && fiscalInvoiceNumber.isNotBlank())
                            put(RegionalExtras.FISCAL_INVOICE_NUMBER_KEY, fiscalInvoiceNumber)
                        if (installmentEnabled && installmentNumber.isNotBlank())
                            put(RegionalExtras.INSTALLMENT_NUMBER_KEY, installmentNumber)
                        if (cashbackEnabled && cashbackAmount.isNotBlank())
                            put(RegionalExtras.CASHBACK_AMOUNT_KEY, cashbackAmount)
                        if (isQrPayment)
                            put(Intents.EXTRA_CUSTOMER_TENDER, "ar.com.fiserv.fiservqr.dev")
                        if (businessIdEnabled && businessId.isNotBlank())
                            put(RegionalExtras.BUSINESS_ID_KEY, businessId)
                        if (subMerchantEnabled && subMerchant.isNotBlank())
                            put(RegionalExtras.SUB_MERCHANT_KEY, subMerchant)
                        if (dynamicMerchantNameEnabled && dynamicMerchantName.isNotBlank())
                            put(RegionalExtras.DYNAMIC_MERCHANT_NAME_KEY, dynamicMerchantName)
                        // --- KEY-VALUE DINÁMICO ---
                        keyValueList.forEach { kv ->
                            if (kv.enabled && kv.key.isNotBlank() && kv.value.isNotBlank()) {
                                put(kv.key, kv.value)
                            }
                        }
                    }
                    onPayClick(amount, extras)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SCROLLABLE EXTRAS REGIONALES ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            RegionalExtraInput("Nº Factura Fiscal", fiscalInvoiceEnabled, { fiscalInvoiceEnabled = it }, fiscalInvoiceNumber) { fiscalInvoiceNumber = it }
            RegionalExtraInput("Nº de Cuotas", installmentEnabled, { installmentEnabled = it }, installmentNumber) { installmentNumber = it }
            RegionalExtraInput("Monto Cashback", cashbackEnabled, { cashbackEnabled = it }, cashbackAmount) { cashbackAmount = it }
            RegionalExtraInput("Business ID", businessIdEnabled, { businessIdEnabled = it }, businessId) { businessId = it }
            RegionalExtraInput("Sub Merchant", subMerchantEnabled, { subMerchantEnabled = it }, subMerchant) { subMerchant = it }
            RegionalExtraInput("Dynamic Merchant Name", dynamicMerchantNameEnabled, { dynamicMerchantNameEnabled = it }, dynamicMerchantName) { dynamicMerchantName = it }

            // --- KEY-VALUE DINÁMICO ---
            Text("Extras Dinámicos (Key-Value)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            keyValueList.forEachIndexed { index, kv ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = kv.enabled,
                        onCheckedChange = { checked ->
                            keyValueList[index] = kv.copy(enabled = checked)
                        },
                        colors = CheckboxDefaults.colors(checkedColor = CloverGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = kv.key,
                        onValueChange = { newKey ->
                            keyValueList[index] = kv.copy(key = newKey)
                        },
                        label = { Text("Key") },
                        enabled = kv.enabled,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = CloverGreen,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = kv.value,
                        onValueChange = { newValue ->
                            keyValueList[index] = kv.copy(value = newValue)
                        },
                        label = { Text("Value") },
                        enabled = kv.enabled,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = CloverGreen,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { keyValueList.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }

            Button(
                onClick = {
                    keyValueList.add(KeyValuePair(nextId))
                    nextId++
                },
                colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Agregar Extra", color = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = isQrPayment,
                    onCheckedChange = { isQrPayment = it },
                    colors = CheckboxDefaults.colors(checkedColor = CloverGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pago con QR", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTONES DE ACCIÓN ---
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRetrieveClick,
                modifier = Modifier.size(width = 140.dp, height = 80.dp)
            ) { Text("Recuperar Pago") }

            Button(
                onClick = onCloseoutBatchClick,
                modifier = Modifier.size(width = 140.dp, height = 80.dp)
            ) { Text("Cierre de Lote") }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = CloverGreen, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

// =================================================================================
// COMPOSABLES AUXILIARES
// =================================================================================
@Composable
fun RegionalExtraInput(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CloverGreen)
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            enabled = checked,
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = CloverGreen,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            )
        )
    }
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun NumberPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onAcceptClick: () -> Unit
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("Del", "0", "OK")
    )

    Column {
        buttons.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { label ->
                    Button(
                        onClick = {
                            when (label) {
                                "Del" -> onDeleteClick()
                                "OK" -> onAcceptClick()
                                else -> onNumberClick(label)
                            }
                        },
                        modifier = Modifier.size(80.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (label) {
                                "OK" -> CloverGreen
                                "Del" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = Color.White
                        )
                    ) {
                        Text(label, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
