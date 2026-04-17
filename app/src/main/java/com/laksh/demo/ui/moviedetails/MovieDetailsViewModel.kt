package com.laksh.demo.ui.moviedetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laksh.demo.usecase.GetMovieDetailsUseCase
import com.laksh.demo.usecase.MovieDetailsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(private val getMovieDetailsUseCase: GetMovieDetailsUseCase) :
    ViewModel() {
    private val _viewStateFlow = MutableStateFlow(MovieDetailsViewState())
    val viewStateFlow = _viewStateFlow.asStateFlow()
    private val _sideEffectFlow = MutableSharedFlow<MovieDetailsSideEffect>()
    val sideEffectFlow = _sideEffectFlow.asSharedFlow()
    fun handleIntent(intent: MovieDetailsIntent) {
        when (intent) {
            is MovieDetailsIntent.LoadMovieDetails -> loadMovieDetails(intent.movieId)
            MovieDetailsIntent.Retry -> {
                val currentState = _viewStateFlow.value
                if (currentState.error != null) {
                    val currentMovie = currentState.movieDetails
                    if (currentMovie != null) {
                        loadMovieDetails(currentMovie.id ?: 0)
                    }
                }
            }
        }
    }

    private fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            getMovieDetailsUseCase(movieId).collect { result ->
                when (result) {
                    MovieDetailsResult.Loading -> {
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(isLoading = true, error = null)
                        )
                    }

                    is MovieDetailsResult.Success -> {
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(
                                movieDetails = result.movieDetails,
                                isLoading = false,
                                error = null
                            )
                        )
                    }

                    is MovieDetailsResult.NetworkError -> {
                        val errorMessage = "Network error: ${result.throwable.localizedMessage}"
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        )
                        _sideEffectFlow.emit(MovieDetailsSideEffect.ShowError(errorMessage))
                    }

                    is MovieDetailsResult.HttpError -> {
                        val errorMessage = "HTTP Error ${result.code}: ${result.message}"
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        )
                        _sideEffectFlow.emit(MovieDetailsSideEffect.ShowError(errorMessage))
                    }

                    is MovieDetailsResult.Error -> {
                        val errorMessage =
                            result.throwable.localizedMessage ?: "Unknown error occurred"
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        )
                        _sideEffectFlow.emit(MovieDetailsSideEffect.ShowError(errorMessage))
                    }
                }
            }
        }
    }
}


