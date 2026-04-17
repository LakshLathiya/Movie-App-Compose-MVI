# Code Examples - Pagination with MVI

This file contains practical code examples for using the paginated movie list implementation.

## Example 1: Basic Movie List

```kotlin
@Composable
fun BasicMovieListExample() {
    MoviesScreen()
}
```

## Example 2: Movie List with Navigation

```kotlin
@Composable
fun MovieListWithNavigation() {
    var selectedMovieId by remember { mutableStateOf<Int?>(null) }
    
    if (selectedMovieId != null) {
        MovieDetailsScreen(
            movieId = selectedMovieId!!,
            onBackClick = { selectedMovieId = null }
        )
    } else {
        MoviesScreen(
            onMovieClick = { movie ->
                selectedMovieId = movie.id
            }
        )
    }
}
```

## Example 3: Custom Movies Screen with Additional Features

```kotlin
@Composable
fun CustomMoviesScreen(viewModel: MoviesViewModel = hiltViewModel()) {
    val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Custom header
        Text(
            text = "Discover Movies",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        // Search bar
        SearchBar(
            query = "", 
            onQueryChange = { /* Handle search */ }
        )
        
        // Movies list
        LazyColumn {
            items(
                count = lazyPagingItems.itemCount,
                key = { index -> lazyPagingItems[index]?.id ?: index }
            ) { index ->
                lazyPagingItems[index]?.let { movie ->
                    MovieCard(
                        movie = movie,
                        onMovieClick = { /* Handle click */ }
                    )
                }
            }
            
            // Loading state
            lazyPagingItems.apply {
                when {
                    loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    
                    loadState.append is LoadState.Loading -> {
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
                    
                    loadState.refresh is LoadState.Error -> {
                        item {
                            ErrorScreen(
                                error = (loadState.refresh as LoadState.Error).error.localizedMessage,
                                onRetry = { refresh() }
                            )
                        }
                    }
                    
                    loadState.append is LoadState.Error -> {
                        item {
                            ErrorItem(
                                message = "Error loading more movies",
                                onRetry = { retry() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search movies...") },
        singleLine = true
    )
}

@Composable
fun ErrorScreen(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops! Something went wrong",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error ?: "Unknown error")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun ErrorItem(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = message)
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
```

## Example 4: Movie List with Pull-to-Refresh

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListWithRefresh(viewModel: MoviesViewModel = hiltViewModel()) {
    val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
    val refreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { lazyPagingItems.refresh() }
    )
    
    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                count = lazyPagingItems.itemCount,
                key = { index -> lazyPagingItems[index]?.id ?: index }
            ) { index ->
                lazyPagingItems[index]?.let { movie ->
                    MovieCard(movie = movie)
                }
            }
        }
        
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
```

## Example 5: Grid Layout for Movies

```kotlin
@Composable
fun MovieGridScreen(viewModel: MoviesViewModel = hiltViewModel()) {
    val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = { index -> lazyPagingItems[index]?.id ?: index }
        ) { index ->
            lazyPagingItems[index]?.let { movie ->
                MovieGridItem(movie = movie)
            }
        }
    }
}

@Composable
fun MovieGridItem(movie: Result, onMovieClick: (Result) -> Unit = {}) {
    Card(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clickable { onMovieClick(movie) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = if (!movie.backdrop_path.isNullOrEmpty()) {
                    Constants.IMG_BASE_URL + movie.backdrop_path
                } else {
                    null
                },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            
            Text(
                text = movie.title ?: "Unknown",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

## Example 6: Advanced ViewModel with Search

```kotlin
@HiltViewModel
class AdvancedMoviesViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {
    
    private val searchQuery = MutableStateFlow("")
    private val _viewState = MutableStateFlow(MoviesViewState())
    val viewState = _viewState.asStateFlow()
    
    val moviesFlow: Flow<PagingData<Result>> = searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                movieRepository.getMoviesPaginated()
            } else {
                // Implement search logic here
                movieRepository.getMoviesPaginated()
            }
        }
        .cachedIn(viewModelScope)
    
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}
```

## Example 7: Using Side Effects

```kotlin
@Composable
fun MovieScreenWithSideEffects(viewModel: MoviesViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffectFlow.collectLatest { effect ->
            when (effect) {
                is MoviesSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                MoviesSideEffect.NavigateToDetails -> {
                    // Handle navigation
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        MoviesScreen(modifier = Modifier.padding(paddingValues))
    }
}
```

## Example 8: Custom Movie Card with Animation

```kotlin
@Composable
fun AnimatedMovieCard(
    movie: Result,
    onMovieClick: (Result) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { 
                isExpanded = !isExpanded
                if (!isExpanded) {
                    onMovieClick(movie)
                }
            }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpanded) 300.dp else 200.dp)
                    .background(Color.LightGray)
                    .animateContentSize()
            ) {
                AsyncImage(
                    model = Constants.IMG_BASE_URL + movie.backdrop_path,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = movie.title ?: "Unknown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Release: ${movie.release_date}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Rating: ${movie.vote_average}/10",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
```

## Example 9: Infinite Scroll with Pagination Indicator

```kotlin
@Composable
fun MovieListWithPaginationIndicator(viewModel: MoviesViewModel = hiltViewModel()) {
    val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
    var currentPage by remember { mutableStateOf(1) }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = lazyPagingItems.itemCount,
            key = { index -> lazyPagingItems[index]?.id ?: index }
        ) { index ->
            lazyPagingItems[index]?.let { movie ->
                MovieCard(movie = movie)
                
                // Update page when reaching end
                if (index == lazyPagingItems.itemCount - 1) {
                    currentPage++
                }
            }
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Page: $currentPage")
            }
        }
    }
}
```

## Example 10: Repository Pattern for Testing

```kotlin
// Mock repository for testing
class MockMovieRepository : MovieRepository {
    override fun getMoviesPaginated(): Flow<PagingData<Result>> {
        return flowOf(
            PagingData.from(
                listOf(
                    Result(
                        id = 1,
                        title = "Test Movie 1",
                        vote_average = 8.5,
                        release_date = "2024-01-01"
                    ),
                    Result(
                        id = 2,
                        title = "Test Movie 2",
                        vote_average = 7.5,
                        release_date = "2024-01-02"
                    )
                )
            )
        )
    }
}

// Usage in tests
@Test
fun testMovieListWithMockData() = runTest {
    val viewModel = MoviesViewModel(MockMovieRepository())
    // Test assertions here
}
```

These examples cover common use cases and patterns for working with the pagination implementation.

