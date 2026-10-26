package com.example.calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.util.Currency

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(tab == 0, { tab = 0 }, { Text("÷") }, label = { Text("Calculator") })
            NavigationBarItem(tab == 1, { tab = 1 }, { Text("$") }, label = { Text("Currency") })
        }
    }) { p -> Box(Modifier.padding(p)) { if (tab == 0) CalcScreen() else CurrencyScreen() } }
}

private fun fmt(v: Double) = BigDecimal(v).round(MathContext(12)).stripTrailingZeros().toPlainString()

@Composable
fun CalcScreen() {
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var deg by remember { mutableStateOf(true) }
    val rows = listOf(
        listOf("sin(", "cos(", "tan(", "ln(", "log("),
        listOf("asin(", "acos(", "atan(", "sqrt(", "cbrt("),
        listOf("(", ")", "^", "!", "%"),
        listOf("pi", "e", "nPr(", "nCr(", "abs("),
        listOf("7", "8", "9", "÷", "⌫"),
        listOf("4", "5", "6", "×", "C"),
        listOf("1", "2", "3", "-", "+"),
        listOf("0", ".", ",", "exp(", "=")
    )
    fun live() { result = runCatching { fmt(Evaluator(deg).eval(expr)) }.getOrDefault("") }
    fun press(k: String) {
        when (k) {
            "C" -> { expr = ""; result = "" }
            "⌫" -> { expr = expr.dropLast(1); live() }
            "=" -> runCatching { expr = fmt(Evaluator(deg).eval(expr)); result = "" }.onFailure { result = "Error" }
            else -> { expr += k; live() }
        }
    }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilterChip(deg, { deg = !deg; live() }, { Text(if (deg) "DEG" else "RAD") })
        }
        Column(Modifier.fillMaxWidth().weight(1.2f), verticalArrangement = Arrangement.Bottom, horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(expr, fontSize = 30.sp, textAlign = TextAlign.End)
            Text(result, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(4f)) {
            rows.forEach { r ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    r.forEach { k ->
                        Button(
                            { press(k) }, Modifier.weight(1f).fillMaxHeight().padding(2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text(k, fontSize = if (k.length > 2) 14.sp else 20.sp) }
                    }
                }
            }
        }
    }
}

private fun currencyName(c: String) = runCatching { Currency.getInstance(c).displayName }.getOrDefault(c)

@Composable
fun CodePicker(value: String, codes: List<String>, onPick: (String) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton({ open = true }, Modifier.fillMaxWidth()) { Text(value) }
        DropdownMenu(open, { open = false }) {
            codes.forEach { c -> DropdownMenuItem({ Text("$c – ${currencyName(c)}") }, { onPick(c); open = false }) }
        }
    }
}

@Composable
fun CurrencyScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf(CurrencyRepository.cached(ctx)) }
    var status by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("1") }
    var from by remember { mutableStateOf("USD") }
    var to by remember { mutableStateOf("EUR") }

    fun refresh() = scope.launch {
        status = "Updating…"
        CurrencyRepository.fetch(ctx)
            .onSuccess { data = it; status = "Updated: ${it.updated}" }
            .onFailure { status = if (data == null) "Offline – no saved rates" else "Offline – showing saved rates" }
    }
    LaunchedEffect(Unit) { refresh() }

    val codes = remember(data) { data?.rates?.keys?.sorted() ?: listOf("USD", "EUR") }
    val result = data?.let { d ->
        val a = amount.toDoubleOrNull(); val f = d.rates[from]; val t = d.rates[to]
        if (a != null && f != null && t != null) a / f * t else null
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Currency Converter", fontSize = 22.sp)
        OutlinedTextField(
            amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CodePicker(from, codes, { from = it }, Modifier.weight(1f))
            TextButton({ val t = from; from = to; to = t }) { Text("⇄") }
            CodePicker(to, codes, { to = it }, Modifier.weight(1f))
        }
        Text(result?.let { "${fmt(it)} $to" } ?: "—", fontSize = 32.sp)
        Text("${currencyName(from)} → ${currencyName(to)}", fontSize = 14.sp)
        Text(status, fontSize = 12.sp)
        Button({ refresh() }) { Text("Refresh rates") }
        Text("Rates by open.er-api.com", fontSize = 11.sp)
    }
}
