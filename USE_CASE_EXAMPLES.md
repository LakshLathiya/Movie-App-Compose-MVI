# Use Case Usage Examples

## Example 1: Basic Movies List Usage

```kotlin
// In MoviesViewModel
@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase
) : ViewModel() {
    
    val moviesFlow: Flow<PagingData<Result>> = getMoviesUseCase()
        .cachedIn(viewModelScope)
}

// In MoviesScreen
@Composable
fun MoviesScreen(viewModel: MoviesViewModel = hiltViewModel()) {
    val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
    
    LazyColumn {
        items(lazyPagingItems.itemCount) { index ->
            lazyPagingItems[index]?.let { movie ->
                MovieCard(movie = movie)
            }
        }
    }
}
```

---

## Example 2: Movie Details with Result Handling

```kotlin
// In MovieDetailsViewModel
@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase
) : ViewModel() {

    private val _viewStateFlow = MutableStateFlow(MovieDetailsViewState())
    val viewStateFlow = _viewStateFlow.asStateFlow()

    fun loadMovieDetails(movieId: Int) {
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
                                isLoading = false
                            )
                        )
                    }

                    is MovieDetailsResult.NetworkError -> {
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(
                                error = "Network error: ${result.throwable.message}",
                                isLoading = false
                            )
                        )
                    }

                    is MovieDetailsResult.HttpError -> {
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(
                                error = "HTTP ${result.code}: ${result.message}",
                                isLoading = false
                            )
                        )
                    }

                    is MovieDetailsResult.Error -> {
                        _viewStateFlow.emit(
                            _viewStateFlow.value.copy(
                                error = result.throwable.message ?: "Unknown error",
                                isLoading = false
                            )
                        )
                    }
                }
            }
        }
    }
}

// In MovieDetailsScreen
@Composable
fun MovieDetailsScreen(
    movieId: Int,
    viewModel: MovieDetailsViewModel = hiltViewModel()
) {
    val viewState = viewModel.viewStateFlow.collectAsState().value

    LaunchedEffect(movieId) {
        viewModel.loadMovieDetails(movieId)
    }

    when {
        viewState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        viewState.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = viewState.error)
            }
        }

        viewState.movieDetails != null -> {
            MovieDetailsContent(movieDetails = viewState.movieDetails)
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available")
            }
        }
    }
}
```

---

## Example 3: Custom Use Case with Filtering

```kotlin
// Extension use case for filtered movies
interface GetTrendingMoviesUseCase {
    operator fun invoke(): Flow<PagingData<Result>>
}

class GetTrendingMoviesUseCaseImpl(
    private val movieRepository: MovieRepository
) : GetTrendingMoviesUseCase {
    
    override fun invoke(): Flow<PagingData<Result>> {
        return movieRepository.getTrendingMovies()
    }
}

// Add to UseCaseModule
@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {
    
    // ... existing provides ...

    @Singleton
    @Provides
    fun provideGetTrendingMoviesUseCase(
        movieRepository: MovieRepository
    ): GetTrendingMoviesUseCase {
        return GetTrendingMoviesUseCaseImpl(movieRepository)
    }
}

// Use in ViewModel
@HiltViewModel
class TrendingMoviesViewModel @Inject constructor(
    private val getTrendingMoviesUseCase: GetTrendingMoviesUseCase
) : ViewModel() {
    
    val trendingMoviesFlow: Flow<PagingData<Result>> = getTrendingMoviesUseCase()
        .cachedIn(viewModelScope)
}
```

---

## Example 4: Use Case with Search

```kotlin
// Search use case
interface SearchMoviesUseCase {
    operator fun invoke(query: String): Flow<PagingData<Result>>
}

class SearchMoviesUseCaseImpl(
    private val movieRepository: MovieRepository
) : SearchMoviesUseCase {
    
    override fun invoke(query: String): Flow<PagingData<Result>> {
        // Validate input
        require(query.isNotBlank()) { "Search query cannot be empty" }
        
        return movieRepository.searchMovies(query)
    }
}

// In ViewModel
@HiltViewModel
class SearchMoviesViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResultsFlow: Flow<PagingData<Result>> = _searchQuery
        .debounce(300) // Wait 300ms before searching
        .distinctUntilChanged()
        .filter { it.isNotBlank() }
        .flatMapLatest { query ->
            searchMoviesUseCase(query)
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

// In Screen
@Composable
fun SearchMoviesScreen(viewModel: SearchMoviesViewModel = hiltViewModel()) {
    var query by remember { mutableStateOf("") }
    val lazyPagingItems = viewModel.searchResultsFlow.collectAsLazyPagingItems()

    Column {
        TextField(
            value = query,
            onValueChange = { 
                query = it
                viewModel.updateSearchQuery(it)
            },
            placeholder = { Text("Search movies...") }
        )

        LazyColumn {
            items(lazyPagingItems.itemCount) { index ->
                lazyPagingItems[index]?.let { MovieCard(movie = it) }
            }
        }
    }
}
```

---

## Example 5: Use Case with Caching

```kotlin
// Cached use case that fetches details and caches them
interface GetMovieDetailsWithCacheUseCase {
    operator fun invoke(movieId: Int): Flow<MovieDetailsResult>
    fun invalidateCache()
}

class GetMovieDetailsWithCacheUseCaseImpl(
    private val movieRepository: MovieRepository
) : GetMovieDetailsWithCacheUseCase {

    private val cache = mutableMapOf<Int, MovieDetails>()

    override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
        require(movieId > 0) { "Movie ID must be greater than 0" }

        // Check cache first
        cache[movieId]?.let { cachedDetails ->
            emit(MovieDetailsResult.Success(cachedDetails))
            return@flow
        }

        emit(MovieDetailsResult.Loading)

        try {
            val response = movieRepository.getMovieDetails(movieId)
            if (response.isSuccessful) {
                response.body()?.let { movieDetails ->
                    cache[movieId] = movieDetails
                    emit(MovieDetailsResult.Success(movieDetails))
                } ?: emit(
                    MovieDetailsResult.Error(Exception("Empty response body"))
                )
            } else {
                emit(
                    MovieDetailsResult.HttpError(
                        code = response.code(),
                        message = response.message()
                    )
                )
            }
        } catch (ioException: IOException) {
            emit(MovieDetailsResult.NetworkError(ioException))
        } catch (exception: Exception) {
            emit(MovieDetailsResult.Error(exception))
        }
    }

    override fun invalidateCache() {
        cache.clear()
    }
}

// In ViewModel
@HiltViewModel
class CachedMovieDetailsViewModel @Inject constructor(
    private val getMovieDetailsUseCase: GetMovieDetailsWithCacheUseCase
) : ViewModel() {

    private val _viewStateFlow = MutableStateFlow(MovieDetailsViewState())
    val viewStateFlow = _viewStateFlow.asStateFlow()

    fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            getMovieDetailsUseCase(movieId).collect { result ->
                // Handle result
            }
        }
    }

    fun refreshMovieDetails(movieId: Int) {
        getMovieDetailsUseCase.invalidateCache()
        loadMovieDetails(movieId)
    }
}
```

---

## Example 6: Use Case with Retry Logic

```kotlin
// Use case with built-in retry
interface GetMovieDetailsWithRetryUseCase {
    operator fun invoke(movieId: Int, maxRetries: Int = 3): Flow<MovieDetailsResult>
}

class GetMovieDetailsWithRetryUseCaseImpl(
    private val movieRepository: MovieRepository
) : GetMovieDetailsWithRetryUseCase {

    override fun invoke(
        movieId: Int,
        maxRetries: Int
    ): Flow<MovieDetailsResult> = flow {
        require(movieId > 0) { "Movie ID must be greater than 0" }
        require(maxRetries > 0) { "Max retries must be greater than 0" }

        emit(MovieDetailsResult.Loading)

        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = movieRepository.getMovieDetails(movieId)
                if (response.isSuccessful) {
                    response.body()?.let { movieDetails ->
                        emit(MovieDetailsResult.Success(movieDetails))
                        return@flow
                    }
                } else {
                    emit(
                        MovieDetailsResult.HttpError(
                            response.code(),
                            response.message()
                        )
                    )
                    return@flow
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(1000 * (attempt + 1)) // Exponential backoff
                }
            } catch (e: Exception) {
                lastException = e
                return@flow
            }
        }

        emit(MovieDetailsResult.NetworkError(lastException as IOException))
    }
}
```

---

## Example 7: Use Case Testing

```kotlin
class GetMoviesUseCaseTest {

    private lateinit var mockRepository: MovieRepository
    private lateinit var useCase: GetMoviesUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetMoviesUseCaseImpl(mockRepository)
    }

    @Test
    fun testGetMoviesReturnsFlow() = runTest {
        val mockPagingData = PagingData.from(
            listOf(
                Result(id = 1, title = "Movie 1"),
                Result(id = 2, title = "Movie 2")
            )
        )

        every { mockRepository.getMoviesPaginated() } returns 
            flowOf(mockPagingData)

        val flow = useCase()
        val results = mutableListOf<PagingData<Result>>()

        flow.collect { results.add(it) }

        assertEquals(1, results.size)
        assertNotNull(results[0])
    }
}

class GetMovieDetailsUseCaseTest {

    private lateinit var mockRepository: MovieRepository
    private lateinit var useCase: GetMovieDetailsUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetMovieDetailsUseCaseImpl(mockRepository)
    }

    @Test
    fun testSuccess() = runTest {
        val mockDetails = MovieDetails(
            id = 550,
            title = "Fight Club",
            overview = "An insomniac office worker..."
        )

        every { mockRepository.getMovieDetails(550) } returns 
            Response.success(mockDetails)

        val results = mutableListOf<MovieDetailsResult>()
        useCase(550).collect { results.add(it) }

        assertEquals(2, results.size)
        assertTrue(results[0] is MovieDetailsResult.Loading)
        assertTrue(results[1] is MovieDetailsResult.Success)
        assertEquals(mockDetails, (results[1] as MovieDetailsResult.Success).movieDetails)
    }

    @Test
    fun testNetworkError() = runTest {
        every { mockRepository.getMovieDetails(550) } throws
            IOException("No internet")

        val results = mutableListOf<MovieDetailsResult>()
        useCase(550).collect { results.add(it) }

        assertEquals(2, results.size)
        assertTrue(results[0] is MovieDetailsResult.Loading)
        assertTrue(results[1] is MovieDetailsResult.NetworkError)
    }

    @Test
    fun testHttpError() = runTest {
        every { mockRepository.getMovieDetails(550) } returns
            Response.error(404, ResponseBody.create(null, ""))

        val results = mutableListOf<MovieDetailsResult>()
        useCase(550).collect { results.add(it) }

        assertEquals(2, results.size)
        assertTrue(results[1] is MovieDetailsResult.HttpError)
        assertEquals(404, (results[1] as MovieDetailsResult.HttpError).code)
    }

    @Test
    fun testInvalidMovieId() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            useCase(-1)
        }
        assertEquals("Movie ID must be greater than 0", exception.message)
    }
}
```

---

## Example 8: Combining Multiple Use Cases

```kotlin
// Complex screen using multiple use cases
@HiltViewModel
class MovieExplorerViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase,
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    private val getTrendingMoviesUseCase: GetTrendingMoviesUseCase
) : ViewModel() {

    // Popular movies list
    val popularMoviesFlow: Flow<PagingData<Result>> = getMoviesUseCase()
        .cachedIn(viewModelScope)

    // Trending movies list
    val trendingMoviesFlow: Flow<PagingData<Result>> = getTrendingMoviesUseCase()
        .cachedIn(viewModelScope)

    // Movie details state
    private val _selectedMovieState = MutableStateFlow<MovieDetailsResult?>(null)
    val selectedMovieState = _selectedMovieState.asStateFlow()

    fun selectMovie(movieId: Int) {
        viewModelScope.launch {
            getMovieDetailsUseCase(movieId).collect { result ->
                _selectedMovieState.emit(result)
            }
        }
    }
}

@Composable
fun MovieExplorerScreen(viewModel: MovieExplorerViewModel = hiltViewModel()) {
    var selectedTab by remember { mutableStateOf(0) }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Popular")
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Trending")
            }
        }

        when (selectedTab) {
            0 -> {
                val lazyItems = viewModel.popularMoviesFlow.collectAsLazyPagingItems()
                MoviesList(lazyItems, viewModel::selectMovie)
            }

            1 -> {
                val lazyItems = viewModel.trendingMoviesFlow.collectAsLazyPagingItems()
                MoviesList(lazyItems, viewModel::selectMovie)
            }
        }

        // Show selected movie details
        val selectedMovie = viewModel.selectedMovieState.collectAsState().value
        selectedMovie?.let { result ->
            when (result) {
                is MovieDetailsResult.Success -> {
                    SelectedMovieDetails(result.movieDetails)
                }
                is MovieDetailsResult.Error -> {
                    Text("Error loading movie details")
                }
                MovieDetailsResult.Loading -> {
                    CircularProgressIndicator()
                }
                else -> {}
            }
        }
    }
}
```

---

## Key Patterns

### Pattern 1: Loading State Pattern
```kotlin
override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
    emit(MovieDetailsResult.Loading)  // Always emit loading first
    try {
        val result = repository.getMovieDetails(movieId)
        emit(MovieDetailsResult.Success(result))
    } catch (e: Exception) {
        emit(MovieDetailsResult.Error(e))
    }
}
```

### Pattern 2: Result Pattern
```kotlin
// Provides detailed information about different outcomes
sealed class Result {
    data class Success(val data: T) : Result()
    data class NetworkError(val throwable: IOException) : Result()
    data class HttpError(val code: Int) : Result()
    data class Error(val throwable: Throwable) : Result()
    object Loading : Result()
}
```

### Pattern 3: Flow Chaining
```kotlin
val filteredFlow = inputFlow
    .debounce(300)
    .distinctUntilChanged()
    .filter { it.isNotEmpty() }
    .flatMapLatest { searchUseCase(it) }
    .cachedIn(viewModelScope)
```

### Pattern 4: Error Handling
```kotlin
try {
    // Try operation
    emit(MovieDetailsResult.Success(data))
} catch (ioException: IOException) {
    // Handle specific exception
    emit(MovieDetailsResult.NetworkError(ioException))
} catch (httpException: HttpException) {
    emit(MovieDetailsResult.HttpError(httpException.code(), httpException.message()))
} catch (exception: Exception) {
    // Catch all
    emit(MovieDetailsResult.Error(exception))
}
```

