package net.simtx.mobile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

@Composable
fun RtlReceiverScreen(frequency: String, onFrequency: (String) -> Unit, squelch: Float, onSquelch: (Float) -> Unit) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Disconnected") }
    var socket by remember { mutableStateOf<Socket?>(null) }
    DisposableEffect(Unit) { onDispose { runCatching { socket?.close() } } }

    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Receiver", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("RTL-TCP / argon.simtx.net:1240", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().height(140.dp).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("RTL-TCP I/Q", fontSize = 10.sp)
                    Text(if (connected) "CONNECTED" else "NO SIGNAL", fontSize = 10.sp)
                }
                Canvas(Modifier.fillMaxSize().padding(top = 10.dp)) {
                    for (i in 0..32) { val x = size.width * i / 32f; drawLine(Color(0x338FA1B8), Offset(x, 0f), Offset(x, size.height), 1f) }
                    val mid = size.height * .7f
                    drawLine(Color(0xFF5DF2C1), Offset(0f, mid), Offset(size.width, mid), 1.5f, cap = StrokeCap.Round)
                }
            }
        }
        Text(status, fontSize = 12.sp)
        OutlinedTextField(value = frequency, onValueChange = onFrequency, label = { Text("Frequency (MHz)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Squelch"); Text("${squelch.toInt()}%") }
        Slider(value = squelch, onValueChange = onSquelch, valueRange = 0f..100f)
        Button(onClick = {
            if (connected) {
                runCatching { socket?.close() }
                socket = null
                connected = false
                status = "Disconnected"
            } else if (!busy) {
                busy = true
                status = "Connecting…"
                scope.launch {
                    val attempt = withContext(Dispatchers.IO) { runCatching { RtlTcpClient.connect("argon.simtx.net", 1240) } }
                    attempt.onSuccess { client ->
                        socket = client
                        connected = true
                        status = "Connected; RTL0 handshake received"
                        val hz = ((frequency.toDoubleOrNull() ?: 7.1) * 1_000_000).toLong()
                        scope.launch(Dispatchers.IO) { runCatching { RtlTcpClient.setFrequency(client, hz) } }
                    }.onFailure { error -> status = "Connection failed: ${error.message ?: "unknown error"}" }
                    busy = false
                }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Icon(if (connected) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (busy) "Connecting…" else if (connected) "Disconnect" else "Connect receiver")
        }
        Button(onClick = {
            val client = socket
            val hz = ((frequency.toDoubleOrNull() ?: 7.1) * 1_000_000).toLong()
            if (client != null) scope.launch(Dispatchers.IO) { runCatching { RtlTcpClient.setFrequency(client, hz) } }
        }, enabled = connected && (frequency.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.fillMaxWidth()) {
            Text("Apply frequency")
        }
        Text("The TCP connection and tuning commands are implemented. Audio demodulation and a live FFT waterfall remain to be added.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
