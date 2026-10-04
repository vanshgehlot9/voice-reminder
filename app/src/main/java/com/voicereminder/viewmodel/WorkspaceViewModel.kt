package com.voicereminder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicereminder.network.MemoryResponse
import com.voicereminder.network.ProjectResponse
import com.voicereminder.network.VoiceboxApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceViewModel : ViewModel() {
    private val api = VoiceboxApiClient.get()
    
    private val _projects = MutableStateFlow<List<ProjectResponse>>(emptyList())
    val projects: StateFlow<List<ProjectResponse>> = _projects.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryResponse>>(emptyList())
    val memories: StateFlow<List<MemoryResponse>> = _memories.asStateFlow()

    fun fetchData() {
        viewModelScope.launch {
            try {
                val projRes = api.getProjects()
                if (projRes.isSuccessful) {
                    _projects.value = projRes.body() ?: emptyList()
                }
                
                val memRes = api.getMemories()
                if (memRes.isSuccessful) {
                    _memories.value = memRes.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("WorkspaceViewModel", "Error fetching data", e)
            }
        }
    }
}
