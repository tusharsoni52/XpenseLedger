package com.xpenseledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpenseledger.app.domain.model.Category
import com.xpenseledger.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  Category UI state
// ─────────────────────────────────────────────────────────────────────────────

sealed class CategoryUiState {
    object Loading : CategoryUiState()
    data class Ready(val mainCategories: List<Category>) : CategoryUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repo: CategoryRepository
) : ViewModel() {

    init {
        // Sync all default categories on every start.
        // INSERT OR IGNORE means existing rows are untouched; only new entries are added.
        viewModelScope.launch { repo.syncDefaults() }
    }

    /**
     * All categories collected eagerly so [subCategoriesFor] and [categoryById]
     * can be called synchronously from composables without suspending.
     * Eagerly sharing ensures the value is populated before any subscriber reads .value.
     */
    private val allCategories: StateFlow<List<Category>> = repo.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Filtered to MAIN categories only. */
    val mainCategories: StateFlow<List<Category>> = allCategories
        .map { list -> list.filter { it.type == "MAIN" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Combined loading/ready state for use in composables that need to show a loader. */
    val uiState: StateFlow<CategoryUiState> = allCategories
        .map { list ->
            if (list.isEmpty()) CategoryUiState.Loading
            else CategoryUiState.Ready(list.filter { it.type == "MAIN" })
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CategoryUiState.Loading)

    /** Synchronous — safe because allCategories is Eagerly shared. */
    fun subCategoriesFor(mainId: Long): List<Category> =
        allCategories.value.filter { it.type == "SUB" && it.parentId == mainId }

    /** Synchronous — safe because allCategories is Eagerly shared. */
    fun categoryById(id: Long): Category? =
        allCategories.value.firstOrNull { it.id == id }
}
