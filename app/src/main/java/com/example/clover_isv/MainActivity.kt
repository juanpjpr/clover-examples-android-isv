package com.example.clover_isv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.clover.sdk.v3.payments.api.PaymentRequestIntentBuilder
import com.example.clover_isv.ui.theme.CloverisvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CloverisvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                    buildPaymentRequestIntent()
                }
            }
        }
    }

    private fun buildPaymentRequestIntent() : Intent {
        val externalPaymentId = "45641248574" // should be unique for each request
        val amount = 1000L
        val context = this

        val builder = PaymentRequestIntentBuilder(externalPaymentId, amount)
        return builder.build(context)

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CloverisvTheme {
        Greeting("Android")
    }
}