package com.verdseat.ui.viewmodel

import com.verdseat.data.*
import com.verdseat.engine.*
import com.verdseat.model.*
import com.verdseat.platform.BleAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/// 主 ViewModel — 编排 BLE + 椎间盘分析 + 坐姿纠正
class VerdViewModel(
    private val bleAdapter: BleAdapter,
    private val discEngine: IDiscLoadEngine = DiscLoadEngine(),
    private val correctionEngine: IPostureCorrectionEngine = PostureCorrectionEngine(),
    val appState: AppState = AppState()
) {
    private val repository = VerdRepository(bleAdapter)
    private var scope: CoroutineScope? = null

    val latestSnapshot: StateFlow<PressureSnapshot?> = appState.latestSnapshot
    val latestAnalysis: StateFlow<SpineAnalysis?> = appState.latestAnalysis
    val deviceState: StateFlow<DeviceState> = appState.deviceState
    val guidance: StateFlow<CorrectionGuidance?> = appState.guidance
    val userProfile: StateFlow<UserProfile> = appState.userProfile
    val connectedDevice: StateFlow<String?> = bleAdapter.connectedDevice

    fun startMonitoring(address: String) {
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope!!.launch {
            try {
                bleAdapter.connect(address)
                appState.deviceState.value = appState.deviceState.value.copy(connected = true)
                repository.pressureStream().collect { snapshot ->
                    val analysis = discEngine.estimate(snapshot, appState.userProfile.value)
                    val guide = correctionEngine.computeGuidance(analysis)
                    appState.record(snapshot, analysis)
                    appState.guidance.value = guide
                }
            } catch (e: Exception) {
                appState.deviceState.value = appState.deviceState.value.copy(connected = false)
            }
        }
    }

    fun stopMonitoring() {
        scope?.cancel(); scope = null
        bleAdapter.disconnect()
        appState.deviceState.value = appState.deviceState.value.copy(connected = false)
    }

    suspend fun setMassageMode(mode: MassageMode) {
        repository.setMassageMode(mode)
        appState.deviceState.value = appState.deviceState.value.copy(massageMode = mode)
    }

    suspend fun setHeater(enabled: Boolean, temp: Int) {
        repository.setHeater(enabled, temp)
        appState.deviceState.value = appState.deviceState.value.copy(heaterEnabled = enabled, heaterTempC = temp)
    }

    fun updateProfile(profile: UserProfile) {
        appState.userProfile.value = profile
    }

    fun dispose() { stopMonitoring() }
}
