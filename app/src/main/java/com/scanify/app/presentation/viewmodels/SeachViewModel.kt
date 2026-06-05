package com.scanify.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.usecases.DocumentUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    object NoResults : SearchUiState
    data class Success(val documents: List<Document>) : SearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val documentUseCases: DocumentUseCases
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val uiState: StateFlow<SearchUiState> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                // Explicitly provide the generic type here to guide the compiler
                flowOf<SearchUiState>(SearchUiState.Idle)
            } else {
                flow {
                    emit(SearchUiState.Loading)
                    documentUseCases.searchDocumentsUseCase(query)
                        .map { documents ->
                            if (documents.isEmpty()) SearchUiState.NoResults
                            else SearchUiState.Success(documents)
                        }
                        .collect { emit(it) }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState.Idle
        )

    fun     onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }
}