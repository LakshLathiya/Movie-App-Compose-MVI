package com.laksh.demo.usecase

import androidx.paging.PagingData
import com.laksh.demo.data.repository.MovieRepository
import com.laksh.demo.remote.model.Result
import kotlinx.coroutines.flow.Flow

interface GetMoviesUseCase {
    operator fun invoke(): Flow<PagingData<Result>>
}


class GetMoviesUseCaseImpl(
    private val movieRepository: MovieRepository
) : GetMoviesUseCase {

    override fun invoke(): Flow<PagingData<Result>> {
        return movieRepository.getMoviesPaginated()
    }
}

