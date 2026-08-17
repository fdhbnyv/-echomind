package com.echomind.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echomind.app.data.memory.Memory
import com.echomind.app.data.memory.MemoryCategory
import com.echomind.app.data.memory.MemoryRepository
import com.echomind.app.data.memory.MemoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoryUiState(
    val memories: List<Memory> = emptyList(),
    val loading: Boolean = false,
    val syncing: Boolean = false,
    val totalCount: Int = 0,
    val prefCount: Int = 0,
    val factCount: Int = 0,
    val activeFilter: String? = null,
    val errorMessage: String? = null,
)

class MemoryViewModel(
    private val repository: MemoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState

    init {
        loadMemories()
    }

    fun loadMemories() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            repository.allMemories.collect { memories ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        memories = memories,
                        totalCount = memories.size,
                        prefCount = memories.count { it.type == MemoryType.PREFERENCE.name },
                        factCount = memories.count { it.type == MemoryType.FACT.name },
                    )
                }
            }
        }
    }

    fun addMemory(
        content: String,
        category: String,
        type: String,
        importance: Int,
        tags: List<String>,
    ) {
        viewModelScope.launch {
            val id = repository.addMemory(Memory(
                id = 0,
                content = content,
                category = category,
                type = type,
                tags = tags,
                importance = importance,
                source = "manual",
                createdAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis(),
            ))
            if (id > 0) loadMemories()
        }
    }

    fun updateMemory(memory: Memory) {
        viewModelScope.launch {
            repository.updateMemory(memory)
            loadMemories()
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            loadMemories()
        }
    }

    fun syncFromNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(syncing = true) }
            try {
                val count = repository.syncMemoriesFromNotes(
                    com.echomind.app.data.repository.NoteRepository(
                        com.echomind.app.ui.screens.ApplicationProvider.applicationContext!!
                    )
                )
                _uiState.update { it.copy(syncing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(syncing = false, errorMessage = e.message) }
            }
        }
    }
}

// Helper to get application context in ViewModels
object ApplicationProvider {
    @Volatile
    var applicationContext: android.content.Context? = null
}
