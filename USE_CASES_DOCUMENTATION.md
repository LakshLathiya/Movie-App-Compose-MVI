# Use Cases Documentation - Clean Architecture

## Overview

Use cases represent the application's business logic layer in the Clean Architecture pattern. They sit between the UI layer (ViewModels) and the Data layer (Repositories), providing a clean abstraction for operations the app can perform.

## Architecture Layers

```
┌─────────────────────────────────────────┐
│           UI Layer                      │
│   (Composables, ViewModels)             │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Use Cases Layer (Domain)          │ ← You are here
│   (Business Logic, Validation)          │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Repository Layer (Data)           │
│   (Data Access, Network Calls)          │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Remote Data Source                │
│   (Retrofit API, TheMovieDB)            │
└─────────────────────────────────────────┘
```

## Use Cases in This Project

### 1. GetMoviesUseCase

**Purpose**: Fetch paginated list of movies

**Responsibilities**:
- Coordinate with repository to get paginated data
- Manage pagination state
- Handle data transformation if needed
- Provide clean interface for ViewModels

**Files**:
- `GetMoviesUseCase.kt` - Interface and implementation
- `UseCaseModule.kt` - Dependency injection configuration

**Usage**:
```kotlin
// In ViewModel
val moviesFlow: Flow<PagingData<Result>> = getMoviesUseCase()
    .cachedIn(viewModelScope)

// In Composable
val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
LazyColumn {
    items(lazyPagingItems.itemCount) { index ->
        lazyPagingItems[index]?.let { movie ->
            MovieCard(movie)
        }
    }
}
```

**API Endpoint**:
```
GET /discover/movie?page={page}&api_key={key}
```

**Response Flow**:
```
Page 1: Results [1-20]
   ↓
User scrolls
   ↓
Page 2: Results [21-40]
   ↓
User scrolls
   ↓
Page 3: Results [41-60]
...
```

**Error Handling**:
- Automatic retry on paging errors
- Graceful degradation on network failures
- Managed by Paging library

---

### 2. GetMovieDetailsUseCase

**Purpose**: Fetch detailed information for a single movie

**Responsibilities**:
- Validate movie ID input
- Emit loading state
- Handle various error scenarios
- Provide detailed error information
- Transform API response to UI-friendly format

**Files**:
- `GetMovieDetailsUseCase.kt` - Interface and implementation with Result sealed class
- `UseCaseModule.kt` - Dependency injection configuration

**Key Features**:

#### Result Sealed Class
```kotlin
sealed class MovieDetailsResult {
    data class Success(val movieDetails: MovieDetails) : MovieDetailsResult()
    data class NetworkError(val throwable: IOException) : MovieDetailsResult()
    data class HttpError(val code: Int, val message: String) : MovieDetailsResult()
    data class Error(val throwable: Throwable) : MovieDetailsResult()
    object Loading : MovieDetailsResult()
}
```

**Usage**:
```kotlin
// In ViewModel
getMovieDetailsUseCase(movieId = 550).collect { result ->
    when (result) {
        MovieDetailsResult.Loading -> {
            // Show loading indicator
            viewState.emit(viewState.value.copy(isLoading = true))
        }
        
        is MovieDetailsResult.Success -> {
            // Display movie details
            viewState.emit(viewState.value.copy(
                movieDetails = result.movieDetails,
                isLoading = false
            ))
        }
        
        is MovieDetailsResult.NetworkError -> {
            // Handle network error (no internet, timeout)
            val msg = "Network error: ${result.throwable.localizedMessage}"
            viewState.emit(viewState.value.copy(
                error = msg,
                isLoading = false
            ))
        }
        
        is MovieDetailsResult.HttpError -> {
            // Handle HTTP errors (404, 500, etc)
            val msg = "Error ${result.code}: ${result.message}"
            viewState.emit(viewState.value.copy(
                error = msg,
                isLoading = false
            ))
        }
        
        is MovieDetailsResult.Error -> {
            // Handle unexpected errors
            viewState.emit(viewState.value.copy(
                error = result.throwable.localizedMessage,
                isLoading = false
            ))
        }
    }
}

// In Composable
val viewState = viewModel.viewStateFlow.collectAsState().value

when {
    viewState.isLoading -> LoadingScreen()
    viewState.error != null -> ErrorScreen(viewState.error)
    viewState.movieDetails != null -> MovieDetailsContent(viewState.movieDetails)
}
```

**API Endpoint**:
```
GET /movie/{movie_id}?api_key={key}
```

**Input Validation**:
- Movie ID must be > 0
- Throws `IllegalArgumentException` if invalid

**Error Scenarios Handled**:

| Scenario | Result Type | Examples |
|----------|------------|----------|
| Network unavailable | `NetworkError` | No internet, timeout |
| Invalid movie ID | `HttpError` (404) | Movie doesn't exist |
| Server error | `HttpError` (5xx) | Server down |
| Malformed response | `Error` | Empty/invalid body |
| Unexpected exception | `Error` | Any other throwable |

---

## Benefits of Using Use Cases

### 1. **Separation of Concerns**
- Business logic isolated from UI and data layers
- Each layer has clear responsibilities
- Easier to understand code flow

### 2. **Reusability**
- Use cases can be called from multiple ViewModels
- Consistent business logic across app
- No duplication of logic

### 3. **Testability**
```kotlin
// Easy to mock for testing
class MockGetMoviesUseCase : GetMoviesUseCase {
    override fun invoke(): Flow<PagingData<Result>> {
        return flowOf(PagingData.from(mockMovieList))
    }
}

// Easy to test with different scenarios
@Test
fun testMovieDetailsSuccess() = runTest {
    val useCase = GetMovieDetailsUseCaseImpl(mockRepository)
    val results = mutableListOf<MovieDetailsResult>()
    
    useCase(movieId = 550).collect { result ->
        results.add(result)
    }
    
    assertTrue(results.any { it is MovieDetailsResult.Loading })
    assertTrue(results.any { it is MovieDetailsResult.Success })
}
```

### 3. **Single Responsibility**
- Each use case does one thing well
- Changes to business logic don't affect UI
- Independent scaling and optimization

### 4. **Dependency Injection**
- Use cases are provided by Hilt
- No manual instantiation needed
- Singleton scope for efficiency

```kotlin
@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase  // Injected automatically
) : ViewModel()
```

---

## Data Flow Example - Get Movies

```
User opens app
    ↓
MoviesScreen composable appears
    ↓
MoviesViewModel.moviesFlow is initialized
    ↓
getMoviesUseCase() is called
    ↓
GetMoviesUseCaseImpl.invoke() returns repository flow
    ↓
MovieRepository.getMoviesPaginated() creates Pager
    ↓
MoviesPagingSource loads first page
    ↓
Retrofit calls: GET /discover/movie?page=1
    ↓
TheMovieDB API returns 20 movies
    ↓
Results emitted to Flow<PagingData<Result>>
    ↓
LazyColumn renders movies
    ↓
User scrolls to bottom
    ↓
Paging detects end, requests page 2
    ↓
MoviesPagingSource loads next page
    ↓
Retrofit calls: GET /discover/movie?page=2
    ↓
20 more movies appended to list
```

---

## Data Flow Example - Get Movie Details

```
User clicks on movie card
    ↓
MovieDetailsScreen opens with movieId = 550
    ↓
MovieDetailsViewModel.handleIntent(LoadMovieDetails(550))
    ↓
getMovieDetailsUseCase(550) is called
    ↓
GetMovieDetailsUseCaseImpl.invoke(550) flow starts
    ↓
Emit: MovieDetailsResult.Loading
    ↓
ViewState updates: isLoading = true
    ↓
UI shows loading spinner
    ↓
Validate: movieId > 0 ✓
    ↓
Retrofit calls: GET /movie/550?api_key=key
    ↓
TheMovieDB API returns movie details
    ↓
Response.isSuccessful = true
    ↓
Emit: MovieDetailsResult.Success(movieDetails)
    ↓
ViewState updates: movieDetails = data, isLoading = false
    ↓
UI shows movie details (poster, title, genres, overview, etc.)
```

---

## File Structure

```
app/src/main/java/com/laksh/demo/
├── domain/
│   └── usecase/
│       ├── GetMoviesUseCase.kt           ← List pagination use case
│       └── GetMovieDetailsUseCase.kt     ← Detail fetching use case
├── ui/
│   ├── movies/
│   │   ├── MoviesViewModel.kt            ← Uses GetMoviesUseCase
│   │   └── MoviesScreen.kt
│   └── moviedetails/
│       ├── MovieDetailsViewModel.kt      ← Uses GetMovieDetailsUseCase
│       └── MovieDetailsScreen.kt
├── data/
│   ├── repository/
│   │   └── MovieRepository.kt
│   └── paging/
│       └── MoviesPagingSource.kt
├── di/
│   ├── UseCaseModule.kt                  ← Provides use cases
│   ├── RepositoryModule.kt
│   └── NetworkModule.kt
└── remote/
    ├── MovieApi.kt
    └── model/
```

---

## Dependency Injection Setup

The `UseCaseModule` provides all use cases:

```kotlin
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
```

**Scope**: `Singleton` - Single instance shared across app lifetime

---

## Best Practices

### 1. **One Responsibility**
```kotlin
// ✅ GOOD - Focused use case
class GetMoviesUseCase {
    operator fun invoke(): Flow<PagingData<Result>>
}

// ❌ BAD - Too many responsibilities
class MovieUseCase {
    fun getMovies(): Flow<PagingData<Result>>
    fun getMovieDetails(id: Int): Flow<MovieDetailsResult>
    fun searchMovies(query: String): Flow<PagingData<Result>>
    fun addToFavorites(movieId: Int): Flow<Unit>
}
```

### 2. **Use Result Types for Complex Scenarios**
```kotlin
// ✅ GOOD - Detailed error information
sealed class MovieDetailsResult {
    data class Success(val data: MovieDetails)
    data class NetworkError(val throwable: IOException)
    data class HttpError(val code: Int, val message: String)
    data class Error(val throwable: Throwable)
    object Loading
}

// ❌ BAD - Loss of error details
data class MovieDetailsResult(
    val data: MovieDetails? = null,
    val error: String? = null
)
```

### 3. **Validate Inputs**
```kotlin
// ✅ GOOD - Input validation
override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
    require(movieId > 0) { "Movie ID must be greater than 0" }
    // ... rest of logic
}

// ❌ BAD - No validation, crashes if invalid
override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
    val result = repository.getMovieDetails(movieId)
    // ... rest of logic
}
```

### 4. **Emit Loading State**
```kotlin
// ✅ GOOD - User sees loading indicator
override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
    emit(MovieDetailsResult.Loading)
    // ... fetch data
}

// ❌ BAD - UI doesn't know when to show loading
override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
    // ... fetch data immediately
}
```

---

## Testing Use Cases

### Unit Test Example

```kotlin
class GetMovieDetailsUseCaseTest {

    private lateinit var mockRepository: MovieRepository
    private lateinit var useCase: GetMovieDetailsUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetMovieDetailsUseCaseImpl(mockRepository)
    }

    @Test
    fun testGetMovieDetailsSuccess() = runTest {
        val mockMovie = MovieDetails(
            id = 550,
            title = "Fight Club",
            overview = "An insomniac office worker..."
        )
        
        every { mockRepository.getMovieDetails(550) } returns 
            Response.success(mockMovie)

        val results = mutableListOf<MovieDetailsResult>()
        useCase(550).collect { results.add(it) }

        assertEquals(3, results.size)
        assertTrue(results[0] is MovieDetailsResult.Loading)
        assertTrue(results[1] is MovieDetailsResult.Success)
        assertEquals(mockMovie, (results[1] as MovieDetailsResult.Success).movieDetails)
    }

    @Test
    fun testGetMovieDetailsNetworkError() = runTest {
        every { mockRepository.getMovieDetails(550) } throws
            IOException("No internet")

        val results = mutableListOf<MovieDetailsResult>()
        useCase(550).collect { results.add(it) }

        assertEquals(2, results.size)
        assertTrue(results[0] is MovieDetailsResult.Loading)
        assertTrue(results[1] is MovieDetailsResult.NetworkError)
    }

    @Test
    fun testGetMovieDetailsInvalidId() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            useCase(-1)
        }
        assertEquals("Movie ID must be greater than 0", exception.message)
    }
}
```

---

## Summary

Use cases are the heart of the Clean Architecture pattern. They:
- ✅ Encapsulate business logic
- ✅ Provide clean interfaces for UI
- ✅ Handle validation and error scenarios
- ✅ Enable easy testing
- ✅ Support code reusability
- ✅ Make the codebase maintainable and scalable

By using well-designed use cases, your app becomes more testable, maintainable, and easier to extend with new features.

