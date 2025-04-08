package com.example.geupjo_bus

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RouteSearchViewModel : ViewModel() {

   
    private val _recentSearches = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val recentSearches: StateFlow<List<Pair<String, String>>> = _recentSearches

    fun addRecentSearch(from: String, to: String) {
        val newItem = Pair(from, to)
        val current = _recentSearches.value.toMutableList()

   
        current.remove(newItem)
        current.add(0, newItem)

    
        if (current.size > 4) {
            current.removeLast()
        }

        _recentSearches.value = current
    }
}
