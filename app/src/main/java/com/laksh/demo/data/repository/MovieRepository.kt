package com.laksh.demo.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.laksh.demo.data.paging.MoviesPagingSource
import com.laksh.demo.remote.MovieApi
import com.laksh.demo.remote.model.MovieDetails
import com.laksh.demo.remote.model.Result
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import javax.inject.Inject

interface MovieRepository {
    fun getMoviesPaginated(): Flow<PagingData<Result>>

    suspend fun getMovieDetails(movieId: Int): Response<MovieDetails>
}

class MovieRepositoryImpl @Inject constructor(
    private val movieApi: MovieApi
) : MovieRepository {
    override fun getMoviesPaginated(): Flow<PagingData<Result>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { MoviesPagingSource(movieApi) }
        ).flow
    }

    override suspend fun getMovieDetails(movieId: Int): Response<MovieDetails> {
        return movieApi.getMovieDetails(movieId)
    }
}