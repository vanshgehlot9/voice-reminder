package com.voicereminder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicereminder.network.VoiceboxApiClient
import com.voicereminder.network.WorkflowResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkflowViewModel : ViewModel() {
    private val api = VoiceboxApiClient.get()
    
    private val _workflows = MutableStateFlow<List<WorkflowResponse>>(emptyList())
    val workflows: StateFlow<List<WorkflowResponse>> = _workflows.asStateFlow()

    private var isPolling = false

    fun fetchData() {
        if (isPolling) return
        isPolling = true
        viewModelScope.launch {
            while (true) {
                try {
                    val res = api.getWorkflows()
                    if (res.isSuccessful) {
                        _workflows.value = res.body() ?: emptyList()
                    }
                } catch (e: Exception) {
                    Log.e("WorkflowViewModel", "Error fetching workflows", e)
                }
                delay(2000) // Poll every 2 seconds
            }
        }
    }

    fun approveWorkflow(workflowId: String) {
        viewModelScope.launch {
            try {
                api.approveWorkflow(workflowId)
                // The polling will pick up the status change
            } catch (e: Exception) {
                Log.e("WorkflowViewModel", "Error approving workflow", e)
            }
        }
    }
}
