package com.verdseat.data

import com.verdseat.VerdConfig.Session.MAX_RECORDS
import com.verdseat.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock

/// 会话存储接口
interface ISessionStore {
    fun add(snapshot: PressureSnapshot)
    fun add(analysis: SpineAnalysis)
    fun recentSnapshots(count: Int = 100): List<PressureSnapshot>
    fun recentAnalyses(count: Int = 100): List<SpineAnalysis>
    fun todaySnapshots(): List<PressureSnapshot>
    fun count(): Int
    fun clear()
}

/// 内存环形缓冲区 — 压力 + 分析双轨存储
class InMemorySessionStore(private val maxSize: Int = MAX_RECORDS) : ISessionStore {
    private val snapshots = ArrayDeque<PressureSnapshot>(maxSize)
    private val analyses = ArrayDeque<SpineAnalysis>(maxSize)

    override fun add(snapshot: PressureSnapshot) {
        if (snapshots.size >= maxSize) snapshots.removeFirst()
        snapshots.addLast(snapshot)
    }

    override fun add(analysis: SpineAnalysis) {
        if (analyses.size >= maxSize) analyses.removeFirst()
        analyses.addLast(analysis)
    }

    override fun recentSnapshots(count: Int): List<PressureSnapshot> =
        snapshots.takeLast(count)

    override fun recentAnalyses(count: Int): List<SpineAnalysis> =
        analyses.takeLast(count)

    override fun todaySnapshots(): List<PressureSnapshot> {
        val dayStart = todayStartMs()
        return snapshots.filter { it.timestamp >= dayStart }
    }

    override fun count(): Int = snapshots.size
    override fun clear() { snapshots.clear(); analyses.clear() }

    private fun todayStartMs(): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return now - (now % 86_400_000L)
    }
}

/// 应用状态容器
class AppState(private val sessionStore: ISessionStore = InMemorySessionStore()) {
    val latestSnapshot: MutableStateFlow<PressureSnapshot?> = MutableStateFlow(null)
    val latestAnalysis: MutableStateFlow<SpineAnalysis?> = MutableStateFlow(null)
    val deviceState: MutableStateFlow<DeviceState> = MutableStateFlow(DeviceState())
    val userProfile: MutableStateFlow<UserProfile> = MutableStateFlow(UserProfile())
    val guidance: MutableStateFlow<CorrectionGuidance?> = MutableStateFlow(null)

    fun record(snapshot: PressureSnapshot, analysis: SpineAnalysis) {
        latestSnapshot.value = snapshot
        latestAnalysis.value = analysis
        sessionStore.add(snapshot)
        sessionStore.add(analysis)
    }

    fun todaySnapshots(): List<PressureSnapshot> = sessionStore.todaySnapshots()
    fun recentAnalyses(count: Int = 100): List<SpineAnalysis> = sessionStore.recentAnalyses(count)
    fun clearHistory() = sessionStore.clear()
}
