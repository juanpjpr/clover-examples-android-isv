package ar.com.fiserv.clover_isv

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ar.com.fiserv.clover_isv.ui.theme.CloverisvTheme
import com.clover.sdk.cfp.activity.helper.CloverCFPActivityHelper
import com.clover.sdk.cfp.activity.helper.CloverCFPCommsHelper
import com.clover.sdk.util.CustomerMode
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.payments.Batch
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.RegionalExtras
import com.clover.sdk.v3.payments.api.CloseoutRequestIntentBuilder
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder
import com.clover.sdk.v3.payments.api.RetrievePaymentRequestIntentBuilder
import java.util.HashMap

// Colores de Clover
val CloverGreen = Color(0xFF43B02A)
val CloverDarkGreen = Color(0xFF388E1E)
val CloverLightGray = Color(0xFFF5F5F5)

class MainActivity : ComponentActivity(), CloverCFPCommsHelper.MessageListener {
    private lateinit var activityHelper: CloverCFPActivityHelper
    private lateinit var commsHelper: CloverCFPCommsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CustomerMode.enable(this)
        // Inicializar helpers para CFP
        activityHelper = CloverCFPActivityHelper(this)
        commsHelper = CloverCFPCommsHelper(this, intent, this)

        setContent {
            var paymentResult by remember { mutableStateOf<Payment?>(null) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var currentExternalPaymentId by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(false) }

            val payLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                isLoading = false
                val data = result.data
                if (result.resultCode == Activity.RESULT_OK) {
                    val payment = data?.getParcelableExtra<Payment>(Intents.EXTRA_PAYMENT)
                    paymentResult = payment
                    errorMessage = null
                } else {
                    errorMessage = "❌ " + data?.getStringExtra(Intents.EXTRA_FAILURE_MESSAGE)
                }
            }

            val retrieveLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                isLoading = false
                val data = result.data
                if (result.resultCode == Activity.RESULT_OK) {
                    val payment = data?.getParcelableExtra<Payment>(Intents.EXTRA_PAYMENT)
                    paymentResult = payment
                    errorMessage = null
                } else {
                    errorMessage = "❌ " + data?.getStringExtra(Intents.EXTRA_FAILURE_MESSAGE)
                }
            }

            val closeoutLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                isLoading = false
                val data = result.data
                if (result.resultCode == Activity.RESULT_OK) {
                    val batch = data?.getParcelableExtra<Batch>(CloseoutRequestIntentBuilder.Response.BATCH)
                    errorMessage = "✅ Cierre de lote exitoso: ID ${batch?.id}"
                    paymentResult = null
                } else {
                    val failureMessage = data?.getStringExtra(CloseoutRequestIntentBuilder.Response.FAILURE_MESSAGE)
                    val paymentIds = data?.getStringArrayListExtra(CloseoutRequestIntentBuilder.Response.PAYMENT_IDS)
                    var errorText = "❌ Falló el cierre de lote"
                    if (failureMessage != null) {
                        errorText += ": $failureMessage"
                    }
                    if (paymentIds != null && paymentIds.isNotEmpty()) {
                        errorText += "\nIDs de pago abiertos: ${paymentIds.joinToString()}"
                    }
                    errorMessage = errorText
                    paymentResult = null
                }
            }

            CloverisvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CloverLightGray
                ) {
                    val context = LocalContext.current
                    val activity = context as? Activity
                    Column {
                        SecretExitArea(
                            modifier = Modifier.align(Alignment.Start),
                            onExit = {
                                activity?.finish()
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PaymentScreen(
                            onPayClick = { amount, extras ->
                                val externalId = (1_000_000_000..9_999_999_999).random().toString()
                                currentExternalPaymentId = externalId
                                val p = KioskPayRequestIntentBuilder(amount, externalId)
                                val intent = p.build(this@MainActivity)

                                if (extras.isNotEmpty()) {
                                    val extrasHashMap = HashMap<String, String>(extras)
                                    intent.putExtra(Intents.EXTRA_REGIONAL_EXTRAS, extrasHashMap)
                                }

                                isLoading = true
                                payLauncher.launch(intent)
                            },
                            onRetrieveClick = {
                                val externalId = currentExternalPaymentId
                                if (externalId != null) {
                                    val intent = RetrievePaymentRequestIntentBuilder()
                                        .externalPaymentId(externalId)
                                        .build(this@MainActivity)
                                    isLoading = true
                                    retrieveLauncher.launch(intent)
                                } else {
                                    errorMessage = "No hay pago para recuperar"
                                }
                            },
                            onSendMessageClick = {
                                doSendMessageToPOS()
                            },
                            onCloseoutBatchClick = {
                                isLoading = true
                                errorMessage = null
                                paymentResult = null
                                val builder  = CloseoutRequestIntentBuilder()
                                val tipOptions = CloseoutRequestIntentBuilder.TipOptions.ZeroOutOpenTips()
                                val intent = builder.tipOptions(tipOptions).build(this@MainActivity)
                                closeoutLauncher.launch(intent)
                            },
                            isLoading = isLoading
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        paymentResult?.let {
                            Text(
                                text = "✅ Pago exitoso: ${it.id}",
                                color = CloverDarkGreen,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        errorMessage?.let {
                            Text(
                                text = it,
                                color = if (it.startsWith("✅")) CloverDarkGreen else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        activityHelper.dispose()
        commsHelper.dispose()
        super.onDestroy()
    }

    override fun onMessage(payload: String) {
        if ("FINISH" == payload) {
            finishWithPayloadToPOS("")
        }
    }

    private fun doSendMessageToPOS() {
        try {
            commsHelper.sendMessage("PROCESANDO_PAGO|${1232313213}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error sending message to POS", e)
        }
    }

    private fun finishWithPayloadToPOS(resultPayload: String) {
        activityHelper.setResultAndFinish(Activity.RESULT_OK, resultPayload)
    }
}

// =================================================================================
// Composable Functions
// =================================================================================

@Composable
fun PaymentScreen(
    onPayClick: (Long, Map<String, String>) -> Unit,
    onRetrieveClick: () -> Unit,
    onSendMessageClick: () -> Unit,
    onCloseoutBatchClick: () -> Unit,
    isLoading: Boolean
){
    KeepScreenOn()
    var amountText by remember { mutableStateOf("") }

    // --- ESTADOS PARA LOS REGIONAL EXTRAS ---
    var fiscalInvoiceEnabled by remember { mutableStateOf(false) }
    var fiscalInvoiceNumber by remember { mutableStateOf("") }
    var installmentEnabled by remember { mutableStateOf(false) }
    var installmentNumber by remember { mutableStateOf("") }
    var cashbackEnabled by remember { mutableStateOf(false) }
    var cashbackAmount by remember { mutableStateOf("") }
    var isQrPayment by remember { mutableStateOf(false) }
    // ▼▼▼ NUEVOS ESTADOS AÑADIDOS ▼▼▼
    var businessIdEnabled by remember { mutableStateOf(false) }
    var businessId by remember { mutableStateOf("") }
    var subMerchantEnabled by remember { mutableStateOf(false) }
    var subMerchant by remember { mutableStateOf("") }
    var dynamicMerchantNameEnabled by remember { mutableStateOf(false) }
    var dynamicMerchantName by remember { mutableStateOf("") }

    fun getAmountLong(): Long {
        return amountText.toLongOrNull() ?: 0L
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$${amountText.ifEmpty { "0" }}",
            style = MaterialTheme.typography.headlineLarge,
            color = CloverDarkGreen,
            modifier = Modifier.padding(16.dp)
        )

        NumberPad(
            onNumberClick = { digit ->
                if (amountText.length < 9) amountText += digit
            },
            onDeleteClick = {
                if (amountText.isNotEmpty()) {
                    amountText = amountText.dropLast(1)
                }
            },
            // ▼▼▼ LÓGICA DE PAGO ACTUALIZADA CON TODOS LOS CAMPOS ▼▼▼
            onAcceptClick = {
                val amount = getAmountLong()
                if (amount > 0) {
                    val extras = buildMap {
                        if (fiscalInvoiceEnabled && fiscalInvoiceNumber.isNotBlank()) {
                            put(RegionalExtras.FISCAL_INVOICE_NUMBER_KEY, fiscalInvoiceNumber)
                        }
                        if (installmentEnabled && installmentNumber.isNotBlank()) {
                            put(RegionalExtras.INSTALLMENT_NUMBER_KEY, installmentNumber)
                        }
                        if (cashbackEnabled && cashbackAmount.isNotBlank()) {
                            put(RegionalExtras.CASHBACK_AMOUNT_KEY, cashbackAmount)
                        }
                        if (isQrPayment) {
                            put(Intents.EXTRA_CUSTOMER_TENDER, "ar.com.fiserv.fiservqr.dev")
                        }
                        if (businessIdEnabled && businessId.isNotBlank()) {
                            put(RegionalExtras.BUSINESS_ID_KEY, businessId)
                        }
                        if (subMerchantEnabled && subMerchant.isNotBlank()) {
                            put(RegionalExtras.SUB_MERCHANT_KEY, subMerchant)
                        }
                        if (dynamicMerchantNameEnabled && dynamicMerchantName.isNotBlank()) {
                            put(RegionalExtras.DYNAMIC_MERCHANT_NAME_KEY, dynamicMerchantName)
                        }
                    }
                    onPayClick(amount, extras)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN DE EXTRAS REGIONALES (CON SCROLL) ---
        Column(
            modifier = Modifier
                .width(350.dp)
                .heightIn(max = 200.dp) // Limita la altura para que no ocupe toda la pantalla
                .verticalScroll(rememberScrollState()) // Habilita el scroll
        ) {
            RegionalExtraInput(
                label = "Nº Factura Fiscal",
                checked = fiscalInvoiceEnabled,
                onCheckedChange = { fiscalInvoiceEnabled = it },
                value = fiscalInvoiceNumber,
                onValueChange = { fiscalInvoiceNumber = it }
            )
            RegionalExtraInput(
                label = "Nº de Cuotas",
                checked = installmentEnabled,
                onCheckedChange = { installmentEnabled = it },
                value = installmentNumber,
                onValueChange = { installmentNumber = it }
            )
            RegionalExtraInput(
                label = "Monto Cashback",
                checked = cashbackEnabled,
                onCheckedChange = { cashbackEnabled = it },
                value = cashbackAmount,
                onValueChange = { cashbackAmount = it }
            )
            // ▼▼▼ NUEVOS CAMPOS AÑADIDOS A LA UI ▼▼▼
            RegionalExtraInput(
                label = "Business ID",
                checked = businessIdEnabled,
                onCheckedChange = { businessIdEnabled = it },
                value = businessId,
                onValueChange = { businessId = it }
            )
            RegionalExtraInput(
                label = "Sub Merchant",
                checked = subMerchantEnabled,
                onCheckedChange = { subMerchantEnabled = it },
                value = subMerchant,
                onValueChange = { subMerchant = it }
            )
            RegionalExtraInput(
                label = "Dynamic Merchant Name",
                checked = dynamicMerchantNameEnabled,
                onCheckedChange = { dynamicMerchantNameEnabled = it },
                value = dynamicMerchantName,
                onValueChange = { dynamicMerchantName = it }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isQrPayment,
                    onCheckedChange = { isQrPayment = it },
                    colors = CheckboxDefaults.colors(checkedColor = CloverGreen)
                )
                Text("Pago con QR", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTONES DE ACCIÓN ---
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRetrieveClick,
                colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen),
                modifier = Modifier.width(160.dp)
            ) {
                Text("Recuperar Pago", color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onCloseoutBatchClick,
                colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen),
                modifier = Modifier.width(160.dp)
            ) {
                Text("Cierre de Lote", color = Color.White)
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = CloverGreen)
        }
    }
}

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
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CloverGreen)
        )
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
fun SecretExitArea(
    modifier: Modifier = Modifier,
    onExit: () -> Unit
) {
    var tapCount by remember { mutableStateOf(0) }
    val timeoutMillis = 1000L
    var lastTapTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier
            .size(100.dp)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < timeoutMillis) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = currentTime
                    if (tapCount >= 3) {
                        onExit()
                        tapCount = 0
                    }
                }
            }
    )
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
                        modifier = Modifier
                            .size(80.dp),
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