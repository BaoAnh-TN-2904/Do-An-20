package com.example.doan20.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.data.model.User
import com.example.doan20.data.repository.AuthRepository
import com.example.doan20.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ItemViewModel : ViewModel() {
    private val repository = ItemRepository
    private val authRepository = AuthRepository

    val currentUser: StateFlow<User?> = authRepository.currentUser

    fun login(email: String, pass: String): Result<User> {
        return authRepository.login(email, pass)
    }

    fun logout() {
        authRepository.logout()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _statusFilter = MutableStateFlow<ItemStatus?>(null)
    val statusFilter: StateFlow<ItemStatus?> = _statusFilter

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter

    val allItems: StateFlow<List<Item>> = repository.itemsFlow

    val filteredItems: StateFlow<List<Item>> = combine(
        repository.itemsFlow,
        _searchQuery,
        _statusFilter,
        _categoryFilter
    ) { items, query, status, category ->
        items.filter { item ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.name.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true) ||
                        (item.notes?.contains(query, ignoreCase = true) == true)
            }
            val matchesStatus = status == null || item.status == status
            val matchesCategory = category == null || item.category.equals(category, ignoreCase = true)
            matchesQuery && matchesStatus && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: ItemStatus?) {
        _statusFilter.value = filter
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _statusFilter.value = null
        _categoryFilter.value = null
    }

    fun toggleSwitch(id: String) {
        repository.toggleSwitch(id)
    }

    fun setSwitchState(id: String, isOn: Boolean) {
        repository.setSwitchState(id, isOn)
    }

    fun toggleStatus(id: String) {
        repository.toggleStatus(id)
    }

    fun deleteItem(id: String) {
        repository.deleteItem(id)
    }

    fun addItem(item: Item) {
        repository.addItem(item)
    }

    fun updateItem(item: Item) {
        repository.updateItem(item)
    }

    fun getItem(id: String): Item? {
        return repository.getItemById(id)
    }
}
