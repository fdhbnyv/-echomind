package com.echomind.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echomind.app.data.model.StructuredNote
import com.echomind.app.data.model.TemplateType
import com.echomind.app.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val notes: List<StructuredNote> = emptyList(),
    val searchQuery: String = "",
    val filterTemplate: TemplateType? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepo = NoteRepository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _filterTemplate = MutableStateFlow<TemplateType?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        _searchQuery,
        _filterTemplate,
    ) { query, template ->
        Pair(query, template)
    }.flatMapLatest { (query, template) ->
        when {
            query.isBlank() && template == null -> noteRepo.allNotes
            query.isNotBlank() && template != null -> noteRepo.searchNotesByTemplate(template.id, query)
            query.isNotBlank() -> noteRepo.searchNotes(query)
            template != null -> noteRepo.getNotesByTemplate(template.id)
            else -> noteRepo.allNotes
        }
    }.combine(
        combine(_searchQuery, _filterTemplate) { q, t -> Pair(q, t) }
    ) { notes, _ ->
        HistoryUiState(notes = notes, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true),
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }

    fun updateFilterTemplate(template: TemplateType?) {
        _filterTemplate.update { template }
    }

    /** Delete a single note */
    fun deleteNoteById(id: Long) {
        viewModelScope.launch {
            noteRepo.deleteNoteById(id)
        }
    }
}
