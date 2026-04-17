package com.laksh.demo.ui.moviedetails

import com.laksh.demo.remote.model.MovieDetails

sealed class MovieDetailsIntent {
    data class LoadMovieDetails(val movieId: Int) : MovieDetailsIntent()
    data object Retry : MovieDetailsIntent()
}

data class MovieDetailsViewState(
    val movieDetails: MovieDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class MovieDetailsSideEffect {
    data class ShowError(val message: String) : MovieDetailsSideEffect()
    data object NavigateBack : MovieDetailsSideEffect()
}

