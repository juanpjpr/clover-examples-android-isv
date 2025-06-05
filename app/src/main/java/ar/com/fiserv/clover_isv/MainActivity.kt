package ar.com.fiserv.clover_isv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.api.PaymentRequestIntentBuilder
import com.clover.sdk.v3.payments.api.RetrievePaymentRequestIntentBuilder
import ar.com.fiserv.clover_isv.ui.theme.CloverisvTheme

// Colores de Clover
val CloverGreen = Color(0xFF43B02A)
val CloverDarkGreen = Color(0xFF388E1E)
val CloverLightGray = Color(0xFFF5F5F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                                // Crear el PaymentRequestIntentBuilder
                                val builder = PaymentRequestIntentBuilder(externalId, 1000L)

                                builder.tenderOptions(
                                    PaymentRequestIntentBuilder.TenderOptions.Disable(
                                        true, // Deshabilitar efectivo
                                        true  // Deshabilitar Custom Tender
                                    )
                                )

                                val intent = builder.build(this@MainActivity)
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
}

@Composable
fun PaymentScreen(onPayClick: () -> Unit, onRetrieveClick: () -> Unit, isLoading: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onPayClick,
            colors = ButtonDefaults.buttonColors(containerColor = CloverGreen)
        ) {
            Text("Iniciar Pago", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetrieveClick,
            colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen)
        ) {
            Text("Recuperar Pago", color = Color.White)
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = CloverGreen)
        }
    }
}
