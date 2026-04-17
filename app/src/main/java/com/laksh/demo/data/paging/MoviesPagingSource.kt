package com.laksh.demo.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.laksh.demo.remote.MovieApi
import com.laksh.demo.remote.model.Result
import retrofit2.HttpException
import java.io.IOException

class MoviesPagingSource(private val movieApi: MovieApi) : PagingSource<Int, Result>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Result> {
        return try {
            val page = params.key ?: 1
            val response = movieApi.getMovies(page = page)

            val movies = response.results ?: emptyList()

            LoadResult.Page(
                data = movies,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (movies.isEmpty()) null else page + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Result>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

