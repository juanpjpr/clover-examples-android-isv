package com.example.clover_isv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.api.PaymentRequestIntentBuilder
import com.example.clover_isv.ui.theme.CloverisvTheme

class MainActivity : ComponentActivity() {
    private fun buildPaymentRequestIntent() : Intent {
        val externalPaymentId = (1_000_000_000_000_00..9_999_999_999_999_99).random().toString()
        val amount = 1000L
        val context = this

        val builder = PaymentRequestIntentBuilder(externalPaymentId, amount)
        return builder.build(context)

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var paymentResult by remember { mutableStateOf<Payment?>(null) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val data = result.data
                if (result.resultCode == Activity.RESULT_OK) {
                    val payment = data?.getParcelableExtra<Payment>(Intents.EXTRA_PAYMENT)
                    paymentResult = payment
                    // TODO: verify payment
                } else {
                    errorMessage = data?.getStringExtra(Intents.EXTRA_FAILURE_MESSAGE)
                }
            }

            PaymentScreen(onPayClick = {
                val intent = buildPaymentRequestIntent()
                launcher.launch(intent)
            })

            // Optionally show results
            paymentResult?.let {
                // Show success UI or handle the result
            }

            errorMessage?.let {
                // Show error message in UI
            }
        }
    }
}

@Composable
fun PaymentScreen(onPayClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onPayClick) {
            Text("Pay")
        }
    }
}


