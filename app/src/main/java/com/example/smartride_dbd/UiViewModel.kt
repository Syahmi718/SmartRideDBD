package com.example.smartride_dbd

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UiViewModel : ViewModel() {

    private val _sensorData = MutableLiveData<FloatArray>()
    val sensorData: LiveData<FloatArray> get() = _sensorData

    private val _httpConnectionStatus = MutableLiveData<Boolean>()
    val httpConnectionStatus: LiveData<Boolean> get() = _httpConnectionStatus

    private val _predictionResult = MutableLiveData<String>()
    val predictionResult: LiveData<String> get() = _predictionResult

    private val _adviceForAlert = MutableLiveData<String>()
    val adviceForAlert: LiveData<String> get() = _adviceForAlert
    
    // Flag to track if the alert has been shown
    private var _alertShown = false
    val alertShown: Boolean get() = _alertShown
    
    // Speed data
    private val _currentSpeed = MutableLiveData<Float>().apply { value = 0f }
    val currentSpeed: LiveData<Float> get() = _currentSpeed
    
    // Track max speed in current driving session
    private val _maxSpeed = MutableLiveData<Float>().apply { value = 0f }
    val maxSpeed: LiveData<Float> get() = _maxSpeed
    
    // Speed limit settings (can be updated based on road type/GPS)
    private val _speedLimit = MutableLiveData<Float>().apply { value = 60f } // Default 60 km/h
    val speedLimit: LiveData<Float> get() = _speedLimit
    
    // Flag indicating if speed is above limit
    private val _isSpeedingState = MutableLiveData<Boolean>().apply { value = false }
    val isSpeedingState: LiveData<Boolean> get() = _isSpeedingState

    fun updateSensorData(data: FloatArray) {
        _sensorData.postValue(data)
    }

    fun updateHttpConnectionStatus(status: Boolean) {
        _httpConnectionStatus.postValue(status)
    }

    fun updatePredictionResult(result: String) {
        _predictionResult.postValue(result)
    }

    fun updateAdviceForAlert(advice: String) {
        _adviceForAlert.postValue(advice) // Update LiveData with new advice
        _alertShown = false // Reset the flag when new advice is generated
    }
    
    fun markAlertAsShown() {
        _alertShown = true
    }
    
    fun shouldShowAlert(): Boolean {
        return !_alertShown && !_adviceForAlert.value.isNullOrEmpty()
    }
    
    fun updateCurrentSpeed(speedKmh: Float) {
        _currentSpeed.postValue(speedKmh)
        
        // Update max speed if needed
        val currentMaxSpeed = _maxSpeed.value ?: 0f
        if (speedKmh > currentMaxSpeed) {
            _maxSpeed.postValue(speedKmh)
        }
        
        // Check if exceeding speed limit
        val limit = _speedLimit.value ?: Float.MAX_VALUE
        val isSpeeding = speedKmh > limit
        if (_isSpeedingState.value != isSpeeding) {
            _isSpeedingState.postValue(isSpeeding)
        }
    }
    
    fun updateSpeedLimit(newLimitKmh: Float) {
        _speedLimit.postValue(newLimitKmh)
        
        // Re-check speeding state with new limit
        val speed = _currentSpeed.value ?: 0f
        _isSpeedingState.postValue(speed > newLimitKmh)
    }
    
    fun resetSpeedData() {
        _currentSpeed.postValue(0f)
        _maxSpeed.postValue(0f)
        _isSpeedingState.postValue(false)
    }
}
