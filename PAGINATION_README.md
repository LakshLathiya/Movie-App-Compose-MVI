# Pagination Implementation with MVI Architecture

This implementation demonstrates a paginated list using Jetpack Compose with MVI (Model-View-Intent) architecture pattern.

## Architecture Overview

### MVI Pattern Components

1. **Intent** - User actions and events
   - `LoadMovies` - Initial load of movies
   - `LoadMore` - Load next page
   - `Retry` - Retry failed requests

2. **View State** - Current UI state
   - `movies` - List of displayed movies
   - `isLoading` - Initial loading state
   - `isLoadingMore` - Pagination loading state
   - `error` - Error messages
   - `hasMorePages` - Whether more pages are available
   - `currentPage` - Current page number

3. **Side Effects** - One-time events
   - `ShowError` - Show error messages
   - `NavigateToDetails` - Navigate to movie details

## Project Structure

```
app/src/main/java/com/laksh/demo/
├── ui/
│   ├── movies/
│   │   ├── MoviesContract.kt      # MVI contract (Intent, State, SideEffect)
│   │   ├── MoviesViewModel.kt     # ViewModel with MVI pattern
│   │   ├── MoviesScreen.kt        # Main Compose UI screen
│   │   └── MovieCard.kt           # Movie list item card component
│   └── theme/
├── data/
│   ├── repository/
│   │   └── MovieRepository.kt     # Data access layer
│   └── paging/
│       └── MoviesPagingSource.kt  # Pagination implementation
├── remote/
│   ├── MovieApi.kt                # Retrofit API interface
│   └── model/
│       ├── Movies.kt              # API response models
│       └── MovieDetails.kt
├── di/
│   ├── NetworkModule.kt           # Network DI configuration
│   └── RepositoryModule.kt        # Repository DI configuration
├── utils/
│   └── Constants.kt               # API keys and base URLs
└── MainActivity.kt                # Application entry point
```

## Key Features

### 1. Pagination with Jetpack Paging
- Uses `PagingSource` for handling pagination logic
- Automatic loading of next page when user scrolls to bottom
- Built-in error handling and retry mechanism
- Efficient caching with `cachedIn(viewModelScope)`

### 2. MVI Architecture
- Clear separation of concerns
- Intent-based user actions
- Reactive state management
- Side effects for one-time events

### 3. Compose UI
- LazyColumn for efficient list rendering
- Loading and error states
- Movie card with image, title, rating, and release date
- Responsive layout

## Usage

### 1. Navigate to Movies Screen
The `MoviesScreen` composable is already integrated in `MainActivity`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaginationDemoTheme {
                MoviesScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
```

### 2. Using MoviesViewModel
The ViewModel is automatically provided by Hilt through the `@HiltViewModel` annotation:

```kotlin
@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {
    val moviesFlow: Flow<PagingData<Result>> = movieRepository.getMoviesPaginated()
        .cachedIn(viewModelScope)
}
```

### 3. Handling Pagination
The pagination is handled automatically by Paging library:

```kotlin
val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()

LazyColumn {
    items(
        count = lazyPagingItems.itemCount,
        key = { index -> lazyPagingItems[index]?.id ?: index }
    ) { index ->
        val movie = lazyPagingItems[index]
        movie?.let { MovieCard(movie = it, onMovieClick = onMovieClick) }
    }
    
    // Handle loading and error states
    when(lazyPagingItems.loadState.refresh) {
        is LoadState.Loading -> { /* Show loading */ }
        is LoadState.Error -> { /* Show error */ }
        else -> {}
    }
}
```

## Data Flow

```
User Interaction
    ↓
MoviesScreen (UI)
    ↓
MoviesViewModel (MVI - ViewModel)
    ↓
MovieRepository (Data Layer)
    ↓
MoviesPagingSource (Paging Source)
    ↓
MovieApi (Retrofit)
    ↓
TheMovieDB API
```

## Customization

### Changing Page Size
In `MovieRepository.kt`:
```kotlin
PagingConfig(
    pageSize = 20,  // Change this value
    enablePlaceholders = false,
    initialLoadSize = 20
)
```

### Adding Movie Details Screen
1. Create `MovieDetailsScreen` composable
2. Pass `onMovieClick` callback to navigate
3. Fetch movie details using `getMovieDetails()` from MovieApi

### Modifying Movie Card
Edit `MovieCard.kt` to customize:
- Card layout and dimensions
- Image dimensions
- Text formatting and styling
- Additional movie information

## Dependencies

Key dependencies used:
- `androidx.paging:paging-runtime` - Paging library
- `androidx.paging:paging-compose` - Paging Compose integration
- `com.squareup.retrofit2:retrofit` - API calls
- `com.google.dagger:hilt-android` - Dependency injection
- `io.coil-kt.coil3:coil-compose` - Image loading
- `androidx.compose` - Compose UI framework

## Build and Run

```bash
./gradlew build
./gradlew installDebug
```

Or use Android Studio's Run button.

## Notes

- The API key is pre-configured in `Constants.kt`
- Images are fetched from TheMovieDB CDN
- Pagination automatically handles network errors
- The UI is fully responsive and handles rotation

