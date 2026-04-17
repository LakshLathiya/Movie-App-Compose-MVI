package com.laksh.demo.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.laksh.demo.remote.model.Result
import com.laksh.demo.usecase.GetMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase
) : ViewModel() {

    private val _sideEffectFlow = MutableSharedFlow<MoviesSideEffect>()
    val sideEffectFlow = _sideEffectFlow.asSharedFlow()

    val moviesFlow: Flow<PagingData<Result>> = getMoviesUseCase()
        .cachedIn(viewModelScope)
}

