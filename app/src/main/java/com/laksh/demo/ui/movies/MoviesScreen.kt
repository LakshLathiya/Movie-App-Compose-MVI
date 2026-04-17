package com.laksh.demo.ui.movies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.laksh.demo.remote.model.Result
import kotlinx.coroutines.flow.collectLatest


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    modifier: Modifier = Modifier,
    viewModel: MoviesViewModel = hiltViewModel(),
    onMovieClick: (Result) -> Unit = {}
) {
    val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.sideEffectFlow.collectLatest { effect ->
            when (effect) {
                is MoviesSideEffect.ShowError -> {
                    // Show error toast or snackbar
                }

                is MoviesSideEffect.NavigateToDetails -> {
                    // Handle navigation
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Movies") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = { index ->
                    lazyPagingItems[index]?.id ?: index
                }
            ) { index ->
                val movie = lazyPagingItems[index]
                movie?.let {
                    MovieCard(
                        movie = it,
                        onMovieClick = onMovieClick
                    )
                }
            }

            when {
                lazyPagingItems.loadState.refresh is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                lazyPagingItems.loadState.append is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                lazyPagingItems.loadState.refresh is LoadState.Error -> {
                    val error = lazyPagingItems.loadState.refresh as LoadState.Error
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Error: ${error.error.localizedMessage}")
                        }
                    }
                }

                lazyPagingItems.loadState.append is LoadState.Error -> {
                    val error = lazyPagingItems.loadState.append as LoadState.Error
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Error loading more: ${error.error.localizedMessage}")
                        }
                    }
                }
            }
        }
    }
}



