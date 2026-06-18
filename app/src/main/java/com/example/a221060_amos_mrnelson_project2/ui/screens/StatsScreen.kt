package com.example.a221060_amos_mrnelson_project2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a221060_amos_mrnelson_project2.ui.components.TrendLineChart
import com.example.a221060_amos_mrnelson_project2.ui.components.WeeklyBarChart
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityHigh
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityLow
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityMedium
import com.example.a221060_amos_mrnelson_project2.util.generateAndSaveReport
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Real-time calculation based on screen usage
    val workHoursFromScreenTime = viewModel.totalScreenTimeMillis.value / (1000 * 60 * 60f)
    val restHoursFromInactivity = 24f - workHoursFromScreenTime
    val workRatio = (workHoursFromScreenTime / 24f).coerceIn(0f, 1f)

    var selectedBarValue by remember { mutableStateOf<Pair<String, Float>?>(null) }
    var selectedTrendValue by remember { mutableStateOf<Pair<String, Float>?>(null) }

    val calendar = Calendar.getInstance()
    val weekNo = calendar.get(Calendar.WEEK_OF_YEAR)
    val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    // 1. Burnout Trend (Last Week: Mon-Sun)
    val lastWeekMetrics = viewModel.getLastWeekMetrics()
    val stressLevels = lastWeekMetrics.map { it.burnoutScore }
    val lastWeekLabels = lastWeekMetrics.map { 
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.dateIsoString)
            SimpleDateFormat("EEE", Locale.getDefault()).format(date!!)
        } catch (e: Exception) { "???" }
    }

    // 2. Weekly Completion (This Week: Mon-Today)
    val thisWeekMetrics = viewModel.getThisWeekMetrics()
    val completionRates = thisWeekMetrics.map { it.completionRate }
    val thisWeekLabels = thisWeekMetrics.map { 
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.dateIsoString)
            SimpleDateFormat("EEE", Locale.getDefault()).format(date!!)
        } catch (e: Exception) { "???" }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Data & Insights", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Burnout Trend (Last Week)
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Burnout Trend (Last Week)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Monday to Sunday", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (selectedTrendValue != null) {
                    Text("${selectedTrendValue?.first}: Stress level ${(selectedTrendValue?.second!! * 100).toInt()}%", color = PriorityHigh, fontWeight = FontWeight.Bold)
                } else {
                    Text("Tap points to see daily stress level", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    TrendLineChart(modifier = Modifier.fillMaxSize(), data = stressLevels)
                    Row(modifier = Modifier.fillMaxSize()) {
                        lastWeekLabels.forEachIndexed { i, day ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { selectedTrendValue = day to stressLevels.getOrElse(i) { 0f } })
                        }
                    }
                }
            }
        }

        // 2. Weekly Bar Chart (Completion Progress - This Week)
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weekly Completion Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("This Week (Mon-Today)", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (selectedBarValue != null) {
                    Text("${selectedBarValue?.first}: ${selectedBarValue?.second?.toInt()}% complete", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                } else {
                    Text("Tap bars to see daily completion %", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    WeeklyBarChart(modifier = Modifier.fillMaxSize(), data = completionRates)
                    Row(modifier = Modifier.fillMaxSize()) {
                        thisWeekLabels.forEachIndexed { i, day ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { selectedBarValue = day to completionRates.getOrElse(i) { 0f } })
                        }
                    }
                }
            }
        }

        // 3. Focus Time Tracker (Moved UP)
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Focus Time Tracker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Work (Screen Time) vs. Rest (Inactivity)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Work: ${String.format("%.1f", workHoursFromScreenTime)}h", color = PriorityHigh, fontWeight = FontWeight.Bold)
                    Text("Rest: ${String.format("%.1f", restHoursFromInactivity)}h", color = PriorityLow, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)).background(PriorityLow)) {
                    Box(modifier = Modifier.fillMaxWidth(workRatio).fillMaxHeight().background(PriorityHigh))
                }
            }
        }

        // 4. Weighted Completion Rate Card (Moved DOWN)
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weekly Weighted Completion Rate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Difficulty-weighted performance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = (viewModel.weightedCompletionRate / 100).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("${String.format("%.1f", viewModel.weightedCompletionRate)}%", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
            }
        }

        // Decompression Insights
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Decompression Insights", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                val insight = when {
                    viewModel.burnoutRisk > 0.6f -> "High Risk: Take a 15-min screen-free break now."
                    viewModel.burnoutRisk > 0.3f -> "Moderate Load: Practice the 20-20-20 rule."
                    else -> "Great balance! Keep managing your load effectively."
                }
                Text(insight, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Button(
            onClick = {
                scope.launch {
                    viewModel.isGeneratingReport.value = true
                    delay(1500)
                    val isSuccess = generateAndSaveReport(context, viewModel.taskList, viewModel.dailyMetrics)
                    viewModel.isGeneratingReport.value = false
                    if (isSuccess) Toast.makeText(context, "Report saved to Downloads folder!", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !viewModel.isGeneratingReport.value
        ) {
            if (viewModel.isGeneratingReport.value) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("Generate Executive Report", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
