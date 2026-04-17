# Use Cases Implementation Summary

## 📋 Overview

This document summarizes the Use Case layer implementation for the Pagination Demo project, following Clean Architecture principles.

---

## ✅ What Has Been Created

### 1. **Core Use Cases**

#### GetMoviesUseCase
- **Location**: `domain/usecase/GetMoviesUseCase.kt`
- **Purpose**: Fetch paginated movies list
- **Interface + Implementation**: Separate interface and impl class
- **Returns**: `Flow<PagingData<Result>>`
- **Error Handling**: Handled by Paging library

#### GetMovieDetailsUseCase
- **Location**: `domain/usecase/GetMovieDetailsUseCase.kt`
- **Purpose**: Fetch individual movie details
- **Result Type**: `MovieDetailsResult` sealed class
- **Returns**: `Flow<MovieDetailsResult>`
- **Error Handling**: Comprehensive with NetworkError, HttpError, and Error variants
- **Validation**: Input validation (movieId > 0)
- **Loading State**: Emits Loading state before fetching

### 2. **Dependency Injection**

#### UseCaseModule
- **Location**: `di/UseCaseModule.kt`
- **Purpose**: Provides all use case instances via Hilt
- **Scope**: Singleton (shared across app lifetime)
- **Dependencies**: MovieRepository injected into use cases

### 3. **Updated ViewModels**

#### MoviesViewModel
- **Changes**: Now uses `GetMoviesUseCase` instead of repository directly
- **Dependency**: `GetMoviesUseCase` injected via Hilt
- **Flow**: `moviesFlow` now created from use case invocation

#### MovieDetailsViewModel
- **Changes**: Now uses `GetMovieDetailsUseCase` with result handling
- **Dependency**: `GetMovieDetailsUseCase` injected via Hilt
- **Features**: 
  - Handles Loading state
  - Distinguishes between NetworkError, HttpError, and generic Error
  - Emits side effects for errors
  - Updates view state based on result type

---

## 📁 File Structure

```
app/src/main/java/com/laksh/demo/
├── domain/                              ← NEW LAYER
│   └── usecase/                         ← NEW LAYER
│       ├── GetMoviesUseCase.kt          ✅ NEW
│       │   ├── GetMoviesUseCase (interface)
│       │   └── GetMoviesUseCaseImpl (implementation)
│       └── GetMovieDetailsUseCase.kt    ✅ NEW
│           ├── GetMovieDetailsUseCase (interface)
│           ├── GetMovieDetailsUseCaseImpl (implementation)
│           └── MovieDetailsResult (sealed class)
│
├── ui/
│   ├── movies/
│   │   ├── MoviesViewModel.kt           ✅ UPDATED (uses GetMoviesUseCase)
│   │   └── MoviesScreen.kt
│   └── moviedetails/
│       ├── MovieDetailsViewModel.kt     ✅ UPDATED (uses GetMovieDetailsUseCase)
│       └── MovieDetailsScreen.kt
│
├── data/
│   ├── repository/
│   │   └── MovieRepository.kt           (unchanged)
│   └── paging/
│       └── MoviesPagingSource.kt        (unchanged)
│
├── di/
│   ├── UseCaseModule.kt                 ✅ NEW
│   ├── RepositoryModule.kt              (unchanged)
│   └── NetworkModule.kt                 (unchanged)
│
└── remote/
    ├── MovieApi.kt                      (unchanged)
    └── model/                           (unchanged)
```

---

## 🏗️ Architecture Overview

### Before (Without Use Cases)
```
ViewModel → Repository → API
```

### After (With Use Cases)
```
ViewModel → UseCase → Repository → API
```

**Benefits**:
- Better separation of concerns
- Business logic isolated in domain layer
- Easier to test
- Reusable across multiple screens
- Follows SOLID principles

---

## 🔄 Data Flow

### GetMoviesUseCase Flow
```
UI Layer (MoviesScreen)
    ↓
ViewModel (MoviesViewModel)
    ↓
UseCase: invoke() → Flow<PagingData<Result>>
    ↓
Repository: getMoviesPaginated() → Flow<PagingData<Result>>
    ↓
PagingSource: load() → List<Result>
    ↓
API: GET /discover/movie?page={n}
    ↓
TheMovieDB Server
    ↓
Response: {results: [...], page: n, total_pages: X}
    ↓
PagingData emitted
    ↓
Displayed in LazyColumn
```

### GetMovieDetailsUseCase Flow
```
UI Layer (MovieDetailsScreen)
    ↓
ViewModel: handleIntent(LoadMovieDetails(movieId))
    ↓
UseCase: invoke(movieId) → Flow<MovieDetailsResult>
    ↓
Emit: Loading
    ↓
Validate: movieId > 0
    ↓
Repository: getMovieDetails(movieId)
    ↓
API: GET /movie/{movieId}?api_key={key}
    ↓
TheMovieDB Server
    ↓
Response Check
    ├─ Success → Emit: Success(movieDetails)
    ├─ Network Error → Emit: NetworkError(IOException)
    ├─ HTTP Error → Emit: HttpError(code, message)
    └─ Other Error → Emit: Error(throwable)
    ↓
ViewModel collects result
    ↓
Updates ViewState
    ↓
UI reflects state (Loading/Success/Error)
```

---

## 🎯 Key Features

### 1. Single Responsibility
Each use case does one thing well:
- GetMoviesUseCase: Fetch paginated list
- GetMovieDetailsUseCase: Fetch single movie details

### 2. Result Sealed Class
```kotlin
sealed class MovieDetailsResult {
    object Loading : MovieDetailsResult()
    data class Success(val movieDetails: MovieDetails) : MovieDetailsResult()
    data class NetworkError(val throwable: IOException) : MovieDetailsResult()
    data class HttpError(val code: Int, val message: String) : MovieDetailsResult()
    data class Error(val throwable: Throwable) : MovieDetailsResult()
}
```

Provides:
- Type-safe error handling
- Detailed error information
- Exhaustive when expressions
- No null values

### 3. Input Validation
```kotlin
require(movieId > 0) { "Movie ID must be greater than 0" }
```

Prevents invalid API calls at the use case level.

### 4. Loading State
Always emits Loading before fetching data, allowing UI to show loading indicator.

### 5. Dependency Injection
All use cases are provided by Hilt, ensuring:
- Single instance (Singleton scope)
- Automatic injection into ViewModels
- Easy testing with mocks

---

## 📊 Comparison: Before vs After

| Aspect | Before | After |
|--------|--------|-------|
| **ViewModel dependency** | Repository | UseCase |
| **Error handling** | Basic try/catch | Sealed class result type |
| **Validation** | None | Input validation in use case |
| **Loading state** | Not explicit | Explicitly emitted |
| **Reusability** | Low | High |
| **Testability** | Medium | High |
| **Lines of code** | ~50 per ViewModel | ~80 per ViewModel (more features) |
| **Architecture** | 2 layers | 3 layers |

---

## 🧪 Testing

### Unit Test Example
```kotlin
@Test
fun testGetMovieDetailsSuccess() = runTest {
    val mockRepository = mockk<MovieRepository>()
    val useCase = GetMovieDetailsUseCaseImpl(mockRepository)
    
    every { mockRepository.getMovieDetails(550) } returns 
        Response.success(mockMovie)

    val results = mutableListOf<MovieDetailsResult>()
    useCase(550).collect { results.add(it) }

    assertEquals(2, results.size)
    assertTrue(results[0] is MovieDetailsResult.Loading)
    assertTrue(results[1] is MovieDetailsResult.Success)
}
```

### ViewModel Test Example
```kotlin
@Test
fun testMovieDetailsViewState() = runTest {
    val mockUseCase = mockk<GetMovieDetailsUseCase>()
    val viewModel = MovieDetailsViewModel(mockUseCase)
    
    every { mockUseCase(550) } returns flowOf(
        MovieDetailsResult.Loading,
        MovieDetailsResult.Success(mockMovie)
    )

    viewModel.handleIntent(MovieDetailsIntent.LoadMovieDetails(550))
    
    val viewState = viewModel.viewStateFlow.value
    assertTrue(viewState.isLoading)
}
```

---

## 📚 Documentation Files Created

1. **USE_CASES_DOCUMENTATION.md**
   - Comprehensive guide to use cases
   - Architecture explanation
   - Detailed flow diagrams
   - Best practices
   - Testing examples

2. **USE_CASE_EXAMPLES.md**
   - 8 practical code examples
   - Different use case patterns
   - Testing patterns
   - Advanced scenarios

---

## 🚀 How to Use

### 1. Inject Use Case in ViewModel
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val getMoviesUseCase: GetMoviesUseCase
) : ViewModel()
```

### 2. Call Use Case
```kotlin
val moviesFlow = getMoviesUseCase()
    .cachedIn(viewModelScope)
```

### 3. Collect Results
```kotlin
getMovieDetailsUseCase(movieId).collect { result ->
    when (result) {
        is MovieDetailsResult.Success -> { /* Handle */ }
        is MovieDetailsResult.Error -> { /* Handle */ }
        MovieDetailsResult.Loading -> { /* Handle */ }
    }
}
```

---

## ✨ Benefits Realized

### 1. **Better Code Organization**
- Clear separation of concerns
- Domain layer holds business logic
- Easy to navigate and understand

### 2. **Improved Testability**
- Use cases can be tested independently
- ViewModels can be tested with mock use cases
- No need to mock repositories

### 3. **Enhanced Reusability**
- Use cases can be shared across ViewModels
- Same business logic, different UIs
- Reduces code duplication

### 4. **Easier Maintenance**
- Changes to business logic in one place
- API changes handled in repository layer
- UI can remain unchanged

### 5. **Better Error Handling**
- Typed errors with sealed classes
- Specific handling for different error types
- Clear error information for users

### 6. **Scalability**
- Easy to add new use cases
- New features don't affect existing code
- Foundation for future growth

---

## 🔗 Integration Points

### Hilt Dependency Graph
```
UseCaseModule
    ↓
GetMoviesUseCase ← MovieRepository
GetMovieDetailsUseCase ← MovieRepository
    ↓
MovieRepository ← MovieApi (from NetworkModule)
    ↓
MovieApi ← Retrofit (from NetworkModule)
```

### Flow Through Layers
```
MainActivity
    ↓
MoviesScreen (Composable)
    ↓
MoviesViewModel (@HiltViewModel)
    ↓
GetMoviesUseCase (@Inject)
    ↓
MovieRepository (@Inject)
    ↓
MovieApi (@Inject)
    ↓
Retrofit (Singleton)
```

---

## 📝 Checklist

- ✅ GetMoviesUseCase interface and implementation created
- ✅ GetMovieDetailsUseCase with result sealed class created
- ✅ MovieDetailsResult sealed class defined
- ✅ UseCaseModule created and configured
- ✅ MoviesViewModel updated to use GetMoviesUseCase
- ✅ MovieDetailsViewModel updated to use GetMovieDetailsUseCase
- ✅ All files compile without errors
- ✅ Comprehensive documentation created
- ✅ Code examples provided
- ✅ Architecture diagrams explained

---

## 🎓 Next Steps

1. **Review Documentation**
   - Read USE_CASES_DOCUMENTATION.md
   - Study USE_CASE_EXAMPLES.md

2. **Add More Use Cases**
   - GetSearchMoviesUseCase
   - GetFavoriteMoviesUseCase
   - AddToFavoritesUseCase
   - RemoveFromFavoritesUseCase

3. **Implement Advanced Features**
   - Caching layer
   - Retry logic
   - Offline support
   - Pagination customization

4. **Add Comprehensive Tests**
   - Unit tests for all use cases
   - ViewModel tests
   - Integration tests

5. **Monitor Performance**
   - Add analytics
   - Track error rates
   - Monitor API response times

---

## 📞 Summary

The use case layer has been successfully implemented following Clean Architecture principles. The project now has:

- **3-layer architecture**: UI → UseCase → Data
- **Type-safe error handling**: Sealed result classes
- **Input validation**: Prevents invalid API calls
- **Loading states**: Explicit state emissions
- **Dependency injection**: All managed by Hilt
- **Comprehensive documentation**: Ready for team use
- **Code examples**: Practical patterns for common scenarios

The foundation is now in place for building a scalable, maintainable, and testable Android application!

---

**Status**: ✅ Complete and Ready for Use

