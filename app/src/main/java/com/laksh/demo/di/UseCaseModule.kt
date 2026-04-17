package com.laksh.demo.di

import com.laksh.demo.data.repository.MovieRepository
import com.laksh.demo.usecase.GetMovieDetailsUseCase
import com.laksh.demo.usecase.GetMovieDetailsUseCaseImpl
import com.laksh.demo.usecase.GetMoviesUseCase
import com.laksh.demo.usecase.GetMoviesUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {
    @Singleton
    @Provides
    fun provideGetMoviesUseCase(
        movieRepository: MovieRepository
    ): GetMoviesUseCase {
        return GetMoviesUseCaseImpl(movieRepository)
    }

    @Singleton
    @Provides
    fun provideGetMovieDetailsUseCase(
        movieRepository: MovieRepository
    ): GetMovieDetailsUseCase {
        return GetMovieDetailsUseCaseImpl(movieRepository)
    }
}

