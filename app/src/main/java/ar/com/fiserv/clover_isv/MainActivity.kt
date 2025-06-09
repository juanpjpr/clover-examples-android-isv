package ar.com.fiserv.clover_isv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.clover.sdk.v3.payments.api.PaymentRequestIntentBuilder
import com.clover.sdk.v3.payments.api.RetrievePaymentRequestIntentBuilder
import ar.com.fiserv.clover_isv.ui.theme.CloverisvTheme
import com.clover.sdk.cashdrawer.CloverServiceCashDrawer
import com.clover.sdk.cfp.activity.helper.CloverCFPActivityHelper
import com.clover.sdk.cfp.activity.helper.CloverCFPCommsHelper
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder

// Colores de Clover
val CloverGreen = Color(0xFF43B02A)
val CloverDarkGreen = Color(0xFF388E1E)
val CloverLightGray = Color(0xFFF5F5F5)

class MainActivity : ComponentActivity(), CloverCFPCommsHelper.MessageListener {
    private lateinit var activityHelper: CloverCFPActivityHelper
    private lateinit var commsHelper: CloverCFPCommsHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            onPayClick = {
                                val externalId = (1_000_000_000_000_00..9_999_999_999_999_99).random().toString()
                                currentExternalPaymentId = externalId

                                val k = KioskPayRequestIntentBuilder("10".toLong(),"12321321313")
                                k.taxAmount(10L)
                                val intent = k.build(this@MainActivity)
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
                            onSendMessageClick = { doSendMessageToPOS() },
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
                                text = "❌ Error: $it",
                                color = MaterialTheme.colorScheme.error,
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
        // Llamado cuando remoteDeviceConnector.sendMessageToActivity(...) es invocado desde POS
        if ("FINISH" == payload) {
            finishWithPayloadToPOS("")
        }
    }

    private fun doSendMessageToPOS() {
        try {
            val payload = "some message"
            commsHelper.sendMessage("PROCESANDO_PAGO|${1232313213}")
           // commsHelper.sendMessage(payload) // enviar mensaje a la instancia RemoteDeviceConnector
        } catch (e: Exception) {
            // registrar la excepción, actualizar ui, etc.
            Log.e("MainActivity", "Error sending message to POS", e)
        }
    }

    private fun finishWithPayloadToPOS(resultPayload: String) {
        activityHelper.setResultAndFinish(Activity.RESULT_OK, resultPayload)
    }
}

@Composable
fun PaymentScreen(onPayClick: () -> Unit, onRetrieveClick: () -> Unit,  onSendMessageClick: () -> Unit,isLoading: Boolean) {
    KeepScreenOn()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPayClick,
            colors = ButtonDefaults.buttonColors(containerColor = CloverGreen),
            modifier = Modifier.width(200.dp)
        ) {
            Text("Iniciar Pago", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetrieveClick,
            colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen),
            modifier = Modifier.width(200.dp)
        ) {
            Text("Recuperar Pago", color = Color.White)
        }

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

                    Log.d("SecretExit", "Tap count: $tapCount")

                    if (tapCount >= 3) {
                        Log.d("SecretExit", "Saliendo de la app")
                        onExit()
                        tapCount = 0
                    }
                }
            }
    )
}