package com.laksh.demo.ui.movies

import com.laksh.demo.remote.model.Result

sealed class MoviesIntent {
    data object LoadMovies : MoviesIntent()
    data object LoadMore : MoviesIntent()
    data object Retry : MoviesIntent()
}

data class MoviesViewState(
    val movies: List<Result> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMorePages: Boolean = true,
    val currentPage: Int = 1
)

sealed class MoviesSideEffect {
    data class ShowError(val message: String) : MoviesSideEffect()
    data object NavigateToDetails : MoviesSideEffect()
}

