package com.laksh.demo.usecase

import com.laksh.demo.data.repository.MovieRepository
import com.laksh.demo.remote.model.MovieDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
sealed class MovieDetailsResult {
    data class Success(val movieDetails: MovieDetails) : MovieDetailsResult()
    data class NetworkError(val throwable: IOException) : MovieDetailsResult()
    data class HttpError(val code: Int, val message: String) : MovieDetailsResult()
    data class Error(val throwable: Throwable) : MovieDetailsResult()
    object Loading : MovieDetailsResult()
}
interface GetMovieDetailsUseCase {
    operator fun invoke(movieId: Int): Flow<MovieDetailsResult>
}
class GetMovieDetailsUseCaseImpl(
    private val movieRepository: MovieRepository
) : GetMovieDetailsUseCase {
    override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
        require(movieId > 0) { "Movie ID must be greater than 0" }
        emit(MovieDetailsResult.Loading)
        try {
            val response = movieRepository.getMovieDetails(movieId)
            if (response.isSuccessful) {
                val movieDetails = response.body()
                if (movieDetails != null) {
                    emit(MovieDetailsResult.Success(movieDetails))
                } else {
                    emit(MovieDetailsResult.Error(Exception("Empty response body from server")))
                }
            } else {
                // Emit HTTP error with status code
                emit(MovieDetailsResult.HttpError(code = response.code(), message = response.message()))
            }
        } catch (ioException: IOException) {
            // Network error (no internet, timeout, etc.)
            emit(MovieDetailsResult.NetworkError(ioException))
        } catch (httpException: HttpException) {
            // HTTP error not caught by response.isSuccessful
            emit(
                MovieDetailsResult.HttpError(
                    code = httpException.code(),
                    message = httpException.message()
                )
            )
        } catch (exception: Exception) {
            // Other unexpected errors
            emit(MovieDetailsResult.Error(exception))
        }
    }
}

