package com.knittrac.app.presentation.feature_stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R
import com.knittrac.app.presentation.common.EmptyStateView
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.*

/**
 * Экран статистики проекта.
 *
 * Четыре состояния (в правильном порядке):
 * 1. isLoading → CircularProgressIndicator.
 * 2. error != null → сообщение об ошибке.
 * 3. dailyStats.isEmpty() → EmptyStateView (нет данных).
 * 4. else → LineChart с графиком.
 */
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
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val errorRes = state.errorRes
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorRes != null -> {
                EmptyStateView(
                    icon = Icons.Default.Warning,
                    title = stringResource(errorRes),
                    description = stringResource(R.string.error_load_stats_description),
                    actionText = stringResource(R.string.error_retry),
                    onActionClick = { viewModel.onAction(StatsContract.Action.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            state.dailyStats.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Default.BarChart,
                    title = stringResource(R.string.no_stats_title),
                    description = stringResource(R.string.no_stats_description),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
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
                            text = stringResource(R.string.chart_time_by_day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
