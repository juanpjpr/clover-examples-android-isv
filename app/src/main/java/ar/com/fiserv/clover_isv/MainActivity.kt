package ar.com.fiserv.clover_isv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.api.RetrievePaymentRequestIntentBuilder
import ar.com.fiserv.clover_isv.ui.theme.CloverisvTheme
import com.clover.sdk.cfp.activity.helper.CloverCFPActivityHelper
import com.clover.sdk.cfp.activity.helper.CloverCFPCommsHelper
import com.clover.sdk.util.CustomerMode
import com.clover.sdk.v1.Intents.EXTRA_CUSTOMER_TENDER
import com.clover.sdk.v3.payments.Batch
import com.clover.sdk.v3.payments.RegionalExtras
import com.clover.sdk.v3.payments.api.CloseoutRequestIntentBuilder
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder
import com.clover.sdk.v3.payments.api.PaymentRequestIntentBuilder
import java.util.HashMap

// Colores de Clover
val CloverGreen = Color(0xFF43B02A)
val CloverDarkGreen = Color(0xFF388E1E)
val CloverLightGray = Color(0xFFF5F5F5)

private const val CLOSEOUT_REQUEST_CODE = 1001

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
                    errorMessage = data?.getStringExtra(Intents.EXTRA_FAILURE_MESSAGE)
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
                    errorMessage = data?.getStringExtra(Intents.EXTRA_FAILURE_MESSAGE)
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
                    paymentResult = null // Clear previous payment result
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
                    paymentResult = null // Clear previous payment result
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
                            onPayClick = { amount ->
                                val externalId = (1_000_000_000..9_999_999_999).random().toString()
                                currentExternalPaymentId = externalId
                                val p = KioskPayRequestIntentBuilder(amount, currentExternalPaymentId.toString())
                                //val p = PaymentRequestIntentBuilder(currentExternalPaymentId.toString(), amount)
                                val intent = p.build(this@MainActivity)
                                var map = HashMap<String, String>()
                                //map.put(RegionalExtras.INSTALLMENT_NUMBER_KEY, "2")
                                     //map.put(RegionalExtras.CASHBACK_AMOUNT_KEY, "12300")
                                map.put(RegionalExtras.FISCAL_INVOICE_NUMBER_KEY, "123456778892")
                                //map.put(EXTRA_CUSTOMER_TENDER, "ar.com.fiserv.fiservqr.dev")
                               // map.put("clover.intent.extra.CUSTOMER_TENDER_REQUIRED", "true")
                                //map.put(RegionalExtras.BUSINESS_ID_KEY, "123")
                               // map.put(RegionalExtras.SUB_MERCHANT_KEY, "123")
                             //   map.put(RegionalExtras.DYNAMIC_MERCHANT_NAME_KEY, "123")
                                intent.putExtra(Intents.EXTRA_REGIONAL_EXTRAS,map)


                                isLoading = true
                                payLauncher.launch(intent)
                            },
                            onPayQrClick = { amount ->
                                val externalId = (1_000_000_000..9_999_999_999).random().toString()
                                currentExternalPaymentId = externalId
                                val p = KioskPayRequestIntentBuilder(amount, currentExternalPaymentId.toString())
                                val intents = p.build(this@MainActivity)
                                isLoading = true
                                payLauncher.launch(intents)
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
                                errorMessage = null // Clear previous messages
                                paymentResult = null // Clear previous payment result
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
                                text = it, // Display success or failure message from closeout or payment
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


@Composable
fun PaymentScreen(
    onPayClick: (Long) -> Unit,
    onPayQrClick: (Long) -> Unit,
    onRetrieveClick: () -> Unit,
    onSendMessageClick: () -> Unit,
    onCloseoutBatchClick: () -> Unit, // New callback for closeout
    isLoading: Boolean
){
    //KeepScreenOn()
    var amountText by remember { mutableStateOf("") }

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
            onAcceptClick = {
                val amount = getAmountLong()
                if (amount > 0) {
                    onPayClick(amount)
                } else {
                    // If amount is 0 and NumberPad OK is clicked,
                    // consider it as "Send Message" if that's the desired flow,
                    // or handle as an invalid payment amount.
                    // For now, let's assume it could be a signal for another action if needed
                    // or simply does nothing if amount is not > 0 for payment.
                    // If "OK" on numpad should also trigger closeout if amount is 0,
                    // that logic would be added here.
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetrieveClick,
            colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen),
            modifier = Modifier.width(200.dp)
        ) {
            Text("Recuperar Pago", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

        Button(
            onClick = {
                val amount = getAmountLong()
                if (amount > 0) {
                    onPayQrClick(amount)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CloverGreen),
            modifier = Modifier.width(200.dp)
        ) {
            Text("Pagar con QR", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

        Button(
            onClick = onCloseoutBatchClick, // Invoke the closeout callback
            colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen), // Consistent color
            modifier = Modifier.width(200.dp)
        ) {
            Text("Cierre de Lote", color = Color.White)
        }


        // Consider if onSendMessageClick needs a dedicated button or is triggered differently
        // Spacer(modifier = Modifier.height(8.dp))
        // Button(
        // onClick = onSendMessageClick,
        // colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        // modifier = Modifier.width(200.dp)
        // ) {
        // Text("Send Message to POS", color = Color.White)
        // }


        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = CloverGreen)
        }
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
