package com.verdseat.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdseat.VerdConfig
import com.verdseat.model.*
import com.verdseat.ui.viewmodel.VerdViewModel

/// 翠座主界面 — 4 Tab 导航
enum class VerdTab { HOME, SPINE, DEVICE, HISTORY }

@Composable
fun VerdApp(viewModel: VerdViewModel? = null) {
    var selectedTab by remember { mutableStateOf(VerdTab.HOME) }
    val analysis by (viewModel?.latestAnalysis?.collectAsState() ?: remember { mutableStateOf(null) })
    val guidance by (viewModel?.guidance?.collectAsState() ?: remember { mutableStateOf(null) })

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(VerdConfig.APP_NAME_CN) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFF1F8E9)) {
                    NavigationBarItem(
                        selected = selectedTab == VerdTab.HOME,
                        onClick = { selectedTab = VerdTab.HOME },
                        icon = { Icon(Icons.Filled.Home, "首页") },
                        label = { Text("首页") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == VerdTab.SPINE,
                        onClick = { selectedTab = VerdTab.SPINE },
                        icon = { Icon(Icons.Outlined.AccessibilityNew, "脊椎") },
                        label = { Text("脊椎") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == VerdTab.DEVICE,
                        onClick = { selectedTab = VerdTab.DEVICE },
                        icon = { Icon(Icons.Outlined.Bluetooth, "设备") },
                        label = { Text("设备") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == VerdTab.HISTORY,
                        onClick = { selectedTab = VerdTab.HISTORY },
                        icon = { Icon(Icons.Outlined.Timeline, "数据") },
                        label = { Text("数据") }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTab) {
                    VerdTab.HOME -> HomeDashboard(analysis, guidance)
                    VerdTab.SPINE -> SpineMonitorScreen(analysis)
                    VerdTab.DEVICE -> DeviceScreen()
                    VerdTab.HISTORY -> PlaceholderScreen("历史数据", "坐姿与椎间盘压力趋势")
                }
            }
        }
    }
}

// ─── 首页仪表盘 ──────────────────────────────────────

@Composable
fun HomeDashboard(analysis: SpineAnalysis?, guidance: CorrectionGuidance?) {
    Column(
        Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 风险等级大圆环
        val risk = analysis?.overallRisk ?: SpinePressureLevel.LOW
        Card(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = riskColor(risk).copy(alpha = 0.08f))
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("椎间盘状态", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text(risk.emoji, fontSize = 56.sp)
                Spacer(Modifier.height(4.dp))
                Text(risk.label, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = riskColor(risk))
                Spacer(Modifier.height(4.dp))
                Text(risk.suggestion, fontSize = 13.sp, color = Color.Gray)
            }
        }

        // 纠正指导
        if (guidance != null && guidance.currentState != PostureState.NEUTRAL) {
            Card(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("💡 ${guidance.verbalGuidance}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (guidance.pelvicTiltAdjustDeg != 0.0) {
                        Text("骨盆调整: ${"%.1f".format(guidance.pelvicTiltAdjustDeg)}°", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        // 关键指标行
        if (analysis != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("峰值压力", "${"%.0f".format(analysis.peakPressureKPa)} kPa", analysis.peakPressureSegment.label, Modifier.weight(1f))
                MetricCard("左右偏差", "${"%.1f".format(analysis.leftRightImbalancePercent)}%",
                    if (analysis.leftRightImbalancePercent < 15) "正常" else "注意", Modifier.weight(1f))
                MetricCard("累积负荷", "${"%.0f".format(analysis.cumulativeLoadIndex)}",
                    if (analysis.cumulativeLoadIndex < 50) "良好" else "偏高", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MetricCard(value: String, unit: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(unit, fontSize = 11.sp, color = Color.Gray)
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

// ─── 脊椎监测屏 ──────────────────────────────────────

@Composable
fun SpineMonitorScreen(analysis: SpineAnalysis?) {
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("椎间盘压力分布", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (analysis != null) {
            // 6 节段脊柱图
            SpinePressureVisualizer(analysis)
            Spacer(Modifier.height(16.dp))

            // 坐姿状态
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(16.dp)) {
                    Text("坐姿: ${analysis.postureState.label}", fontWeight = FontWeight.Bold)
                    Text(analysis.postureState.description, fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("连接 VERD 坐垫以查看数据", color = Color.Gray)
            }
        }
    }
}

@Composable
fun SpinePressureVisualizer(analysis: SpineAnalysis) {
    val segments = listOf(DiscSegment.L1_L2, DiscSegment.L2_L3, DiscSegment.L3_L4,
        DiscSegment.L4_L5, DiscSegment.L5_S1, DiscSegment.SACRUM)

    Column(Modifier.fillMaxWidth()) {
        segments.forEach { segment ->
            val data = analysis.segments[segment]
            if (data != null) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(segment.label, Modifier.width(56.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    LinearProgressIndicator(
                        progress = { (data.estimatedPressureKPa / 600.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(10.dp).padding(horizontal = 8.dp),
                        color = riskColor(data.riskLevel),
                        trackColor = Color.LightGray.copy(alpha = 0.3f),
                    )
                    Text(
                        "${"%.0f".format(data.estimatedPressureKPa)} kPa",
                        Modifier.width(64.dp),
                        fontSize = 11.sp,
                        color = riskColor(data.riskLevel),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── 设备屏 ──────────────────────────────────────────

@Composable
fun DeviceScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("VERD 坐垫", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("设备状态: 未连接", color = Color.Gray)
                Text("固件版本: v1.0.0", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { /* 扫描连接 */ }, Modifier.fillMaxWidth()) {
                    Text("扫描 VERD 设备")
                }
            }
        }
    }
}

// ─── 占位屏 ──────────────────────────────────────────

@Composable
fun PlaceholderScreen(title: String, message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// ─── 颜色工具 ────────────────────────────────────────

fun riskColor(level: SpinePressureLevel): Color = when (level) {
    SpinePressureLevel.LOW -> Color(0xFF4CAF50)
    SpinePressureLevel.MODERATE -> Color(0xFFFF9800)
    SpinePressureLevel.HIGH -> Color(0xFFFF5722)
    SpinePressureLevel.CRITICAL -> Color(0xFFE53935)
}
