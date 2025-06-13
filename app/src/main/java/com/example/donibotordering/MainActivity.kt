package com.example.donibotordering

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.donibotordering.ui.theme.DoniBotOrderingTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔊 Initialize TextToSpeech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        setContent {
            DoniBotOrderingTheme {
                var screen by remember { mutableStateOf("main") }

                when (screen) {
                    "main" -> MainMenuScreen { screen = "order" }
                    "order" -> OrderingScreen(
                        onBack = { screen = "main" },
                        onConfirm = { screen = "thankyou" },
                        speak = { msg -> tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null) }
                    )
                    "thankyou" -> ThankYouScreen(
                        speak = { msg -> tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null) },
                        onReturn = { screen = "main" }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}

@Composable
fun MainMenuScreen(onOrderClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        OrderButton(onClick = onOrderClick)
    }
}

@Composable
fun OrderButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = "Order",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Order", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun OrderingScreen(onBack: () -> Unit, onConfirm: () -> Unit, speak: (String) -> Unit) {
    val menuItems = remember {
        mutableStateListOf(
            MenuItem("Salad", 0),
            MenuItem("Pizza", 0),
            MenuItem("Pasta", 0),
            MenuItem("Burger", 0)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ORDERING", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        menuItems.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, fontSize = 18.sp)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        if (item.quantity > 0) {
                            menuItems[index] = item.copy(quantity = item.quantity - 1)
                        }
                    }) {
                        Text("-")
                    }

                    Text(
                        item.quantity.toString(),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Button(onClick = {
                        menuItems[index] = item.copy(quantity = item.quantity + 1)
                    }) {
                        Text("+")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                val summary = menuItems.filter { it.quantity > 0 }
                    .joinToString { "${it.quantity} ${it.name}" }
                val message = if (summary.isEmpty()) {
                    "No items selected."
                } else {
                    "Order confirmed. Please wait while we serve you. You ordered: $summary."
                }
                speak(message)
                onConfirm()
            }) {
                Text("Confirm")
            }

            OutlinedButton(onClick = {
                for (i in menuItems.indices) {
                    menuItems[i] = menuItems[i].copy(quantity = 0)
                }
            }) {
                Text("Cancel")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Order summary
        Text("Order Summary", fontSize = 20.sp, modifier = Modifier.padding(top = 12.dp))
        menuItems.filter { it.quantity > 0 }.forEach {
            Text("- ${it.name}: ${it.quantity}", fontSize = 16.sp)
        }
    }
}

@Composable
fun ThankYouScreen(speak: (String) -> Unit, onReturn: () -> Unit) {
    LaunchedEffect(Unit) {
        speak("Thank you. Your order has been received.")
        Handler(Looper.getMainLooper()).postDelayed({
            onReturn()
        }, 5000)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✅", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Order received successfully!", fontSize = 20.sp)
        }
    }
}

data class MenuItem(val name: String, val quantity: Int)