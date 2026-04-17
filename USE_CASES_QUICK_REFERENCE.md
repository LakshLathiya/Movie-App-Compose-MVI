# Use Cases Quick Reference Guide

## 📱 Quick Navigation

### Files Location
```
GetMoviesUseCase.kt
├── Location: app/src/main/java/com/laksh/demo/domain/usecase/
├── Purpose: Fetch paginated movies list
└── Used by: MoviesViewModel

GetMovieDetailsUseCase.kt
├── Location: app/src/main/java/com/laksh/demo/domain/usecase/
├── Purpose: Fetch individual movie details
├── Result Type: MovieDetailsResult (sealed class)
└── Used by: MovieDetailsViewModel

UseCaseModule.kt
├── Location: app/src/main/java/com/laksh/demo/di/
├── Purpose: Dependency injection for use cases
└── Scope: Singleton
```

---

## 🎯 Use Case Definitions

### GetMoviesUseCase

```kotlin
// Interface
interface GetMoviesUseCase {
    operator fun invoke(): Flow<PagingData<Result>>
}

// Usage
val moviesFlow: Flow<PagingData<Result>> = getMoviesUseCase()
    .cachedIn(viewModelScope)

// In Composable
val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
```

**Key Features**:
- Returns paginated flow
- Automatic pagination handling
- Error handling by Paging library
- Caches data in viewmodel scope

---

### GetMovieDetailsUseCase

```kotlin
// Interface
interface GetMovieDetailsUseCase {
    operator fun invoke(movieId: Int): Flow<MovieDetailsResult>
}

// Usage
getMovieDetailsUseCase(movieId = 550).collect { result ->
    when (result) {
        is MovieDetailsResult.Success -> { /* Handle success */ }
        is MovieDetailsResult.Loading -> { /* Show loading */ }
        is MovieDetailsResult.NetworkError -> { /* Handle network error */ }
        is MovieDetailsResult.HttpError -> { /* Handle HTTP error */ }
        is MovieDetailsResult.Error -> { /* Handle generic error */ }
    }
}
```

**Key Features**:
- Input validation (movieId > 0)
- Explicit loading state
- Typed error handling
- Detailed error information

---

## 🔀 Result Type

```kotlin
sealed class MovieDetailsResult {
    // Represents loading state
    object Loading : MovieDetailsResult()
    
    // Successful fetch
    data class Success(val movieDetails: MovieDetails) : MovieDetailsResult()
    
    // Network errors (no internet, timeout, etc)
    data class NetworkError(val throwable: IOException) : MovieDetailsResult()
    
    // HTTP errors (404, 500, etc)
    data class HttpError(val code: Int, val message: String) : MovieDetailsResult()
    
    // Other exceptions
    data class Error(val throwable: Throwable) : MovieDetailsResult()
}
```

---

## 🎬 Common Patterns

### Pattern 1: Get List and Display
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase
) : ViewModel() {
    val movies: Flow<PagingData<Result>> = getMoviesUseCase()
        .cachedIn(viewModelScope)
}

@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val lazyItems = viewModel.movies.collectAsLazyPagingItems()
    
    LazyColumn {
        items(lazyItems.itemCount) { index ->
            lazyItems[index]?.let { MovieCard(it) }
        }
    }
}
```

### Pattern 2: Get Details with State
```kotlin
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getDetailsUseCase: GetMovieDetailsUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(MovieDetailsViewState())
    val state = _state.asStateFlow()
    
    fun loadDetails(movieId: Int) {
        viewModelScope.launch {
            getDetailsUseCase(movieId).collect { result ->
                _state.value = when (result) {
                    MovieDetailsResult.Loading -> 
                        state.value.copy(isLoading = true, error = null)
                    is MovieDetailsResult.Success -> 
                        state.value.copy(movieDetails = result.movieDetails, isLoading = false)
                    is MovieDetailsResult.Error -> 
                        state.value.copy(error = result.throwable.message, isLoading = false)
                    else -> state.value
                }
            }
        }
    }
}
```

---

## ⚡ Quick Tips

### Tip 1: Always Cache Movies
```kotlin
// ✅ GOOD
val moviesFlow: Flow<PagingData<Result>> = getMoviesUseCase()
    .cachedIn(viewModelScope)

// ❌ BAD - Data is not cached
val moviesFlow: Flow<PagingData<Result>> = getMoviesUseCase()
```

### Tip 2: Handle All Result Types
```kotlin
// ✅ GOOD - Exhaustive when
when (result) {
    MovieDetailsResult.Loading -> {}
    is MovieDetailsResult.Success -> {}
    is MovieDetailsResult.NetworkError -> {}
    is MovieDetailsResult.HttpError -> {}
    is MovieDetailsResult.Error -> {}
}

// ❌ BAD - Partial handling
if (result is MovieDetailsResult.Success) {
    // ...
}
```

### Tip 3: Validate Before Calling
```kotlin
// ✅ GOOD - Validation in use case
// Use case validates: require(movieId > 0)

// ❌ BAD - No validation
try {
    getMovieDetailsUseCase(-1)  // Will fail
}
```

### Tip 4: Use Sealed Classes for Type Safety
```kotlin
// ✅ GOOD - Compiler ensures all cases handled
when (result) {
    is MovieDetailsResult.Success -> { /* Compiler knows type */ }
    is MovieDetailsResult.Error -> { /* Compiler knows type */ }
}

// ❌ BAD - String based errors lose type information
data class Result(val data: Any?, val error: String?)
```

---

## 🔌 Dependency Injection

### How It Works
```
UseCaseModule (provides use cases)
    ↓
@Provides fun provideGetMoviesUseCase()
    ↓
@HiltViewModel constructor(val useCase: GetMoviesUseCase)
    ↓
@Inject automatically fills the parameter
```

### How to Use
```kotlin
// No manual instantiation needed
@HiltViewModel
class MyViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase  // Hilt provides this
) : ViewModel()
```

---

## 🧪 Testing

### Mock Use Case
```kotlin
val mockUseCase = mockk<GetMoviesUseCase>()
every { mockUseCase() } returns flowOf(PagingData.empty())

val viewModel = MyViewModel(mockUseCase)
```

### Test Use Case
```kotlin
@Test
fun testSuccess() = runTest {
    val useCase = GetMovieDetailsUseCaseImpl(mockRepository)
    val results = mutableListOf<MovieDetailsResult>()
    
    useCase(550).collect { results.add(it) }
    
    assertTrue(results[0] is MovieDetailsResult.Loading)
    assertTrue(results[1] is MovieDetailsResult.Success)
}
```

---

## 📊 Error Handling Decision Tree

```
Error Occurs
    ↓
Is it IOException?
├─ YES → NetworkError(throwable)
├─ Handle: Show "Check internet connection"
└─ Retry: Show retry button
    ↓
Is it HttpException?
├─ YES → HttpError(code, message)
├─ Handle by code
│   ├─ 404 → Show "Movie not found"
│   ├─ 5xx → Show "Server error"
│   └─ Other → Show generic error
└─ Rarely retry
    ↓
Other Exception?
├─ YES → Error(throwable)
├─ Handle: Show exception message
└─ Maybe retry
```

---

## 🚀 Performance Notes

### Memory
- Use case instances: **Singleton** (1 per app lifetime)
- Flows are lazy (only compute when collected)
- PagingData caching prevents memory leaks

### Network
- Automatic pagination (only loads visible + buffer)
- Page size: 20 items (configurable)
- Loading states visible to user

### CPU
- Flow collection is efficient
- No blocking operations
- Coroutines handle background work

---

## 🔍 Debugging

### Enable Logging
```kotlin
// Add to UseCase to see all emissions
override fun invoke(movieId: Int): Flow<MovieDetailsResult> = flow {
    Log.d("UseCase", "Started loading movie $movieId")
    emit(MovieDetailsResult.Loading)
    try {
        val response = repository.getMovieDetails(movieId)
        Log.d("UseCase", "Got response: ${response.isSuccessful}")
        if (response.isSuccessful) {
            emit(MovieDetailsResult.Success(response.body()!!))
        }
    } catch (e: Exception) {
        Log.e("UseCase", "Error loading movie", e)
        emit(MovieDetailsResult.Error(e))
    }
}
```

### Common Issues

| Issue | Solution |
|-------|----------|
| ViewModel not getting data | Check if use case is injected |
| Data not updating | Verify collector is collecting |
| Memory leak | Call `cachedIn(viewModelScope)` |
| Loading never ends | Check if Loading state is emitted |
| Error not shown | Check all result types handled |

---

## 📚 Documentation Links

- **Detailed Guide**: USE_CASES_DOCUMENTATION.md
- **Code Examples**: USE_CASE_EXAMPLES.md
- **Implementation Summary**: USE_CASES_IMPLEMENTATION_SUMMARY.md

---

## ✅ Checklist for New Use Cases

When adding a new use case:

- [ ] Create interface in `domain/usecase/`
- [ ] Create implementation class
- [ ] Add `@Provides` method in UseCaseModule
- [ ] Use `@Singleton` scope
- [ ] Add input validation if needed
- [ ] Emit loading state if appropriate
- [ ] Handle all error types
- [ ] Add documentation comments
- [ ] Create unit tests
- [ ] Update ViewModels to use it

---

## 🎯 Key Takeaways

1. **Use cases are your business logic** - All domain logic lives here
2. **Results are typed** - Use sealed classes for different outcomes
3. **Inputs are validated** - Catch errors early
4. **Loading is explicit** - Always emit Loading state
5. **Dependencies are injected** - Use Hilt for all DI
6. **Testing is easy** - Mock use cases in tests
7. **Reuse across ViewModels** - Same use case, multiple screens
8. **Error handling is comprehensive** - Every error type handled

---

**Last Updated**: April 17, 2026
**Status**: ✅ Complete and Production Ready

