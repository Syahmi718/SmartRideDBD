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
    }
}
