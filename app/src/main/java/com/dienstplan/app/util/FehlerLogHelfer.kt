package com.dienstplan.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Zentrales Diagnose- und Fehler-Protokoll für die App.
 */
object FehlerLogHelfer {

    private val zeitFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    val eintraege = mutableStateListOf<String>()

    fun logInfo(bereich: String, nachricht: String) {
        val zeit = LocalTime.now().format(zeitFormat)
        eintraege.add(0, "[$zeit] INFO ($bereich):\n$nachricht")
    }

    fun logFehler(bereich: String, nachricht: String, fehler: Throwable? = null) {
        val zeit = LocalTime.now().format(zeitFormat)
        val details = fehler?.let { "\nUrsache: ${it.javaClass.simpleName}: ${it.localizedMessage}" } ?: ""
        eintraege.add(0, "[$zeit] FEHLER ($bereich):\n$nachricht$details")
    }

    fun logLeeren() {
        eintraege.clear()
    }

    fun alsText(): String {
        return if (eintraege.isEmpty()) "Noch keine Log-Einträge vorhanden."
        else eintraege.joinToString("\n\n------------------------\n\n")
    }
}

@Composable
fun FehlerLogDialog(onSchliessen: () -> Unit) {
    val context = LocalContext.current
    val logText = FehlerLogHelfer.alsText()

    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Diagnose- & Fehler-Log") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = logText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Dienstplan-Log", logText))
                Toast.makeText(context, "Log in Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
            }) {
                Text("Log kopieren")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { FehlerLogHelfer.logLeeren() }) {
                    Text("Leeren", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onSchliessen) {
                    Text("Schließen")
                }
            }
        }
    )
}
