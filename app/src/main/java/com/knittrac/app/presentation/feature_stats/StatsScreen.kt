package com.knittrac.app.presentation.feature_stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                state.dailyStats.isEmpty() -> {
                    Text(
                        text = "Нет данных для отображения. Начните вязать!",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else -> {
                    val points = state.dailyStats.reversed().mapIndexed { index, stat ->
                        Point(index.toFloat(), (stat.totalSeconds / 60f))
                    }

                    if (points.isNotEmpty()) {
                        val lineChartData = LineChartData(
                            linePlotData = LinePlotData(
                                lines = listOf(
                                    Line(
                                        dataPoints = points,
                                        lineStyle = LineStyle(color = MaterialTheme.colorScheme.primary),
                                        intersectionPoint = IntersectionPoint(),
                                        selectionHighlightPoint = SelectionHighlightPoint(),
                                        shadowUnderLine = ShadowUnderLine()
                                    )
                                )
                            ),
                            xAxisData = AxisData.Builder()
                                .axisStepSize(40.dp)
                                .bottomPadding(40.dp)
                                .axisLabelColor(Color.Gray)
                                .axisLineColor(Color.LightGray)
                                .build(),
                            yAxisData = AxisData.Builder()
                                .axisStepSize(40.dp)
                                .axisLabelColor(Color.Gray)
                                .axisLineColor(Color.LightGray)
                                .build(),
                            gridLines = GridLines()
                        )

                        LineChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            lineChartData = lineChartData
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Время вязания (в минутах) по дням",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
