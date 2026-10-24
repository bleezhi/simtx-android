package net.simtx.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF090E17)
private val Panel = Color(0xFF121B29)
private val Panel2 = Color(0xFF1A2638)
private val Mint = Color(0xFF5DF2C1)
private val Muted = Color(0xFF8FA1B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Mint, onPrimary = Ink, background = Ink, surface = Panel, onSurface = Color(0xFFF1F5FA), secondary = Color(0xFF8BA8FF))) { SimTxApp() } }
    }
}

private enum class Screen(val label: String) { HOME("Home"), STATIONS("Stations"), RECEIVER("Receiver"), TRANSMIT("Transmit"), ACCOUNT("Account") }

@Composable
private fun SimTxApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var url by remember { mutableStateOf("https://") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var frequency by remember { mutableStateOf("145.500") }
    var squelch by remember { mutableFloatStateOf(25f) }

    Scaffold(containerColor = Ink, bottomBar = {
        NavigationBar(containerColor = Panel) {
            val entries = listOf(Screen.HOME to Icons.Default.Dashboard, Screen.STATIONS to Icons.Default.CellTower, Screen.RECEIVER to Icons.Default.GraphicEq, Screen.TRANSMIT to Icons.Default.SettingsInputAntenna, Screen.ACCOUNT to Icons.Default.AccountCircle)
            entries.forEach { (item, icon) ->
                NavigationBarItem(selected = screen == item, onClick = { screen = item; notice = null }, icon = { Icon(icon, contentDescription = item.label) }, label = { Text(item.label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Mint, selectedTextColor = Mint, indicatorColor = Panel2, unselectedIconColor = Muted, unselectedTextColor = Muted))
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Mint, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, null, tint = Ink, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("SIMTX", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp); Text("RADIO CONTROL", fontSize = 10.sp, color = Muted, letterSpacing = 2.sp) }
                Text("● OFFLINE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (notice != null) Text(notice!!, color = Mint, modifier = Modifier.padding(bottom = 10.dp))
            when (screen) {
                Screen.HOME -> HomeScreen(onNavigate = { screen = it })
                Screen.STATIONS -> EmptyScreen("Stations", "Your account's SimTX stations will appear here after sign-in.", Icons.Default.CellTower)
                Screen.RECEIVER -> ReceiverScreen(frequency, { frequency = it }, squelch, { squelch = it })
                Screen.TRANSMIT -> TransmitScreen()
                Screen.ACCOUNT -> AccountScreen(url, { url = it }, username, { username = it }, password, { password = it }, { notice = SimTxConnection(url, username, password).validate() ?: "Fields look valid. No network request was made; the verified SimTX sign-in adapter is not implemented yet." })
            }
        }
    }
}

@Composable
private fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Command center", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Your SimTX radio operations at a glance.", color = Muted)
        Surface(color = Panel, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("NETWORK STATUS", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                Text("Not connected", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = Panel2)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Metric("STATIONS", "—"); Metric("RECEIVERS", "0"); Metric("TX SESSIONS", "0") }
                Text("Connect your account to load live SimTX data.", color = Muted, fontSize = 12.sp)
            }
        }
        Text("Quick access", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        QuickCard(Icons.Default.CellTower, "Station workspace", "Stations and status", { onNavigate(Screen.STATIONS) })
        QuickCard(Icons.Default.GraphicEq, "Receiver console", "Frequency and squelch controls", { onNavigate(Screen.RECEIVER) })
        QuickCard(Icons.Default.SettingsInputAntenna, "Transmitter console", "Safe transmit session setup", { onNavigate(Screen.TRANSMIT) })
        Text("No fake stations or live signal data are shown.", color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun Metric(label: String, value: String) { Column { Text(label, fontSize = 9.sp, color = Muted, letterSpacing = 1.sp); Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Mint) } }

@Composable
private fun QuickCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, click: () -> Unit) {
    Surface(onClick = click, color = Panel, shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Panel2, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Mint) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 12.sp) }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun EmptyScreen(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted)
        Surface(color = Panel, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = Mint, modifier = Modifier.size(42.dp))
                Text("No station data", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Not connected to SimTX", color = Muted)
                Text("Live listing needs the verified API adapter.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ReceiverScreen(frequency: String, onFrequency: (String) -> Unit, squelch: Float, onSquelch: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Receiver", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("RX CONSOLE  /  SESSION IDLE", fontSize = 11.sp, color = Muted, letterSpacing = 1.sp)
        Surface(color = Panel, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().height(160.dp).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("SPECTRUM", color = Muted, fontSize = 10.sp); Text("NO SIGNAL", color = Mint, fontSize = 10.sp) }
                Canvas(Modifier.fillMaxSize().padding(top = 10.dp)) {
                    for (i in 0..32) { val x = size.width * i / 32f; drawLine(Panel2, Offset(x, 0f), Offset(x, size.height), 1f) }
                    for (i in 0..4) { val y = size.height * i / 4f; drawLine(Panel2, Offset(0f, y), Offset(size.width, y), 1f) }
                    val mid = size.height * .7f
                    drawLine(Mint.copy(alpha = .6f), Offset(0f, mid), Offset(size.width, mid), 1.5f, cap = StrokeCap.Round)
                }
            }
        }
        OutlinedTextField(value = frequency, onValueChange = onFrequency, label = { Text("Frequency (MHz)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Squelch"); Text(squelch.toInt().toString() + "%", color = Mint) }
        Slider(value = squelch, onValueChange = onSquelch, valueRange = 0f..100f)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Connect receiver — API pending") }
        Text("Controls are local UI only until the SimTX v25 protocol adapter is implemented.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun TransmitScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Transmitter", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("TX CONSOLE  /  SAFE MODE", fontSize = 11.sp, color = Muted, letterSpacing = 1.sp)
        Surface(color = Panel, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.SettingsInputAntenna, null, tint = Mint, modifier = Modifier.size(48.dp))
                Text("Transmitter offline", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("No transmission is possible in this build.", color = Muted)
            }
        }
        InfoRow("Session", "Not started")
        InfoRow("Authorization", "Not verified")
        InfoRow("Protocol", "v25 adapter pending")
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text("Transmit disabled") }
        Text("Transmit stays disabled until authentication, permissions and protocol messages are verified.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun AccountScreen(url: String, onUrl: (String) -> Unit, username: String, onUsername: (String) -> Unit, password: String, onPassword: (String) -> Unit, onValidate: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("Account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Connect to your SimTX account", color = Muted)
        OutlinedTextField(value = url, onValueChange = onUrl, label = { Text("API base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username, onValueChange = onUsername, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = onPassword, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(onClick = onValidate, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.VerifiedUser, null); Spacer(Modifier.width(8.dp)); Text("Validate details") }
        Text("Local validation only. This build does not send or persist credentials; sign-in awaits verified API endpoint mapping.", color = Muted, fontSize = 12.sp)
        Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Security", fontWeight = FontWeight.Bold)
                InfoRow("Transport", "HTTPS only")
                InfoRow("Credential storage", "Not persisted")
                InfoRow("Session token", "Not configured")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
