package com.laksh.demo.ui.moviedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.laksh.demo.utils.Constants
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    movieId: Int,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val viewState = viewModel.viewStateFlow.collectAsState().value

    LaunchedEffect(movieId) {
        viewModel.handleIntent(MovieDetailsIntent.LoadMovieDetails(movieId))
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffectFlow.collectLatest { effect ->
            when (effect) {
                is MovieDetailsSideEffect.ShowError -> {
                    // Show error toast or snackbar
                }

                MovieDetailsSideEffect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Movie Details") },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable { onBackClick() })
                }
            )
        }
    ) { innerPadding ->
        when {
            viewState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            viewState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = viewState.error)
                    }
                }
            }

            viewState.movieDetails != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Poster Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.LightGray)
                    ) {
                        AsyncImage(
                            model = if (!viewState.movieDetails.poster_path.isNullOrEmpty()) {
                                Constants.IMG_BASE_URL + viewState.movieDetails.poster_path
                            } else {
                                null
                            },
                            contentDescription = viewState.movieDetails.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title
                        Text(
                            text = viewState.movieDetails.title ?: "Unknown",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Release Date
                        Text(
                            text = "Release Date: ${viewState.movieDetails.release_date ?: "N/A"}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        // Rating
                        Text(
                            text = "Rating: ${viewState.movieDetails.vote_average ?: 0.0}/10",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Genres
                        if (!viewState.movieDetails.genres.isNullOrEmpty()) {
                            Text(
                                text = "Genres: ${viewState.movieDetails.genres.joinToString(", ") { it.name ?: "" }}",
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Overview
                        Text(
                            text = "Overview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = viewState.movieDetails.overview ?: "No overview available",
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available")
                }
            }
        }
    }
}


