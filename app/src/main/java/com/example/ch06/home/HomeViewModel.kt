package com.example.ch06.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ch06.data.Article
import com.example.ch06.data.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Menyimpan daftar lengkap hasil load.
    // HomeUiState hanya menyimpan hasil yang sudah difilter.
    private var allArticles: List<Article> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching { repository.getArticles() }
                .onSuccess { articles ->
                    // Simpan daftar lengkap agar pencarian dapat diterapkan
                    // kembali menggunakan query yang sedang aktif.
                    allArticles = articles

                    val currentQuery = _uiState.value.query
                    val filteredArticles = filter(allArticles, currentQuery)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            articles = filteredArticles
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Gagal memuat artikel"
                        )
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        val filteredArticles = filter(allArticles, newQuery)

        _uiState.update {
            it.copy(
                query = newQuery,
                articles = filteredArticles
            )
        }
    }

    private fun filter(
        source: List<Article>,
        query: String
    ): List<Article> {
        if (query.isBlank()) {
            return source
        }

        return source.filter { article ->
            article.title.contains(query, ignoreCase = true) ||
                    article.category.contains(query, ignoreCase = true)
        }
    }
}
