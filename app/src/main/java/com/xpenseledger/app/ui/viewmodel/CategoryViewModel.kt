package com.xpenseledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpenseledger.app.domain.model.Category
import com.xpenseledger.app.domain.model.TransactionType
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
//  Category IDs — used to scope the category list per transaction type
// ─────────────────────────────────────────────────────────────────────────────

private const val ID_INCOME          = 300L   // "Income" MAIN category
private const val ID_FINANCE         = 7L     // "Finance" MAIN category
private const val ID_FAMILY_SUPPORT  = 78L    // "Family Support" SUB under Finance

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repo: CategoryRepository
) : ViewModel() {

    init {
        viewModelScope.launch { repo.syncDefaults() }
    }

    private val allCategories: StateFlow<List<Category>> = repo.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** ALL main categories (used as fallback). */
    val mainCategories: StateFlow<List<Category>> = allCategories
        .map { list -> list.filter { it.type == "MAIN" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<CategoryUiState> = allCategories
        .map { list ->
            if (list.isEmpty()) CategoryUiState.Loading
            else CategoryUiState.Ready(list.filter { it.type == "MAIN" })
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CategoryUiState.Loading)

    /**
     * Returns the MAIN categories relevant for the given [TransactionType]:
     *
     *  • EXPENSE  → every MAIN category except "Income"
     *  • INCOME   → only the "Income" category
     *  • TRANSFER → only the "Finance" category (which has "Family Support" as a sub)
     */
    fun mainCategoriesFor(type: TransactionType): List<Category> {
        val mains = allCategories.value.filter { it.type == "MAIN" }
        return when (type) {
            TransactionType.INCOME   -> mains.filter { it.id == ID_INCOME }
            TransactionType.TRANSFER -> mains.filter { it.id == ID_FINANCE }
            TransactionType.EXPENSE  -> mains.filter { it.id != ID_INCOME }
        }
    }

    /**
     * Returns subcategories for [mainId], filtered by [type]:
     *  • TRANSFER → only "Family Support" (id=78), regardless of mainId
     *  • all other types → all subcategories under [mainId]
     */
    fun subCategoriesFor(mainId: Long, type: TransactionType = TransactionType.EXPENSE): List<Category> {
        val subs = allCategories.value.filter { it.type == "SUB" && it.parentId == mainId }
        return if (type == TransactionType.TRANSFER)
            subs.filter { it.id == ID_FAMILY_SUPPORT }
        else
            subs
    }

    /** Synchronous — safe because allCategories is Eagerly shared. */
    fun categoryById(id: Long): Category? =
        allCategories.value.firstOrNull { it.id == id }
}
