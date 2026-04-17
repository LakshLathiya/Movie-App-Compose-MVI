# Integration Guide - Paginated Movies with MVI Architecture

## Overview

This guide shows how to integrate and use the paginated movie list with the complete MVI (Model-View-Intent) architecture pattern using Jetpack Compose.

## Architecture Components Created

### 1. **MVI Pattern Files**

#### MoviesContract.kt
Defines the MVI contract for the movies list:
- **MoviesIntent**: User actions (LoadMovies, LoadMore, Retry)
- **MoviesViewState**: Complete UI state
- **MoviesSideEffect**: One-time events

#### MoviesViewModel.kt
Manages the MVI logic and pagination:
- Exposes `moviesFlow` for paginated data
- Handles side effects
- Uses Hilt for dependency injection

#### MovieDetailsContract.kt
Defines the MVI contract for movie details:
- **MovieDetailsIntent**: User actions (LoadMovieDetails, Retry)
- **MovieDetailsViewState**: Movie details UI state
- **MovieDetailsSideEffect**: One-time events

#### MovieDetailsViewModel.kt
Manages movie details loading:
- Handles intent processing
- Manages view state
- Emits side effects

### 2. **Data Layer Files**

#### MovieRepository.kt
Data access layer:
- `getMoviesPaginated()` - Returns paginated movie flow
- `getMovieDetails(movieId)` - Fetches individual movie details

#### MoviesPagingSource.kt
Implements Paging library's PagingSource:
- Handles pagination logic
- Manages page loading
- Handles errors

### 3. **UI Layer Files**

#### MoviesScreen.kt
Main composable for the movie list:
- Displays paginated list of movies
- Handles loading states
- Shows error messages
- Integrates with MoviesViewModel

#### MovieCard.kt
Reusable composable for individual movie items:
- Displays movie poster image
- Shows title, release date, and rating
- Handles click events

#### MovieDetailsScreen.kt
Bonus: Screen for displaying movie details:
- Shows full movie information
- Displays genres and overview
- Includes back navigation

### 4. **DI Files**

#### RepositoryModule.kt
Dependency injection configuration for repositories

#### NetworkModule.kt (Updated)
Already configured in the project

## Step-by-Step Integration

### Step 1: Verify Dependencies

Ensure your `build.gradle.kts` has:
```kotlin
// Paging
implementation(libs.androidx.paging.runtime)
implementation(libs.androidx.paging.compose)

// Hilt
implementation(libs.hilt.android)
kapt(libs.hilt.compiler)
implementation(libs.androidx.hilt.navigation.compose)

// Retrofit and networking
implementation(libs.retrofit)
implementation(libs.converter.gson)

// Compose
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.compose.material3)

// Image loading
implementation(libs.coil.compose)

// Coroutines
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.android)
```

### Step 2: Add @AndroidEntryPoint to MainActivity

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

### Step 3: Run the Application

```bash
./gradlew build
```

Then run the app on an Android device or emulator.

## Using the Implementation

### Basic Usage - Display Paginated List

```kotlin
@Composable
fun MyScreen() {
    MoviesScreen(
        modifier = Modifier.fillMaxSize(),
        onMovieClick = { movie ->
            // Handle movie click
            println("Clicked: ${movie.title}")
        }
    )
}
```

### Advanced Usage - Manual Pagination Control

If you need more control over pagination in your own composable:

```kotlin
@Composable
fun CustomMoviesScreen(viewModel: MoviesViewModel = hiltViewModel()) {
    val lazyPagingItems = viewModel.moviesFlow.collectAsLazyPagingItems()
    
    LazyColumn {
        items(
            count = lazyPagingItems.itemCount,
            key = { index -> lazyPagingItems[index]?.id ?: index }
        ) { index ->
            lazyPagingItems[index]?.let { movie ->
                MovieCard(movie = movie)
            }
        }
        
        // Handle pagination states
        when (lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                item { CircularProgressIndicator() }
            }
            is LoadState.Error -> {
                item { ErrorMessage() }
            }
            else -> {}
        }
    }
}
```

### Using Movie Details Screen

```kotlin
@Composable
fun MyNavigation() {
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

## Data Flow

```
User Interaction
        ↓
   Composable
        ↓
   ViewModel (MVI)
    Intent → State
        ↓
   Repository
        ↓
   PagingSource
        ↓
   Retrofit API
        ↓
   TheMovieDB Server
        ↓
   Response Flow
        ↓
   Composable Updates
```

## Customization Guide

### Change Page Size

In `MovieRepository.kt`:
```kotlin
PagingConfig(
    pageSize = 50,  // Change from 20 to 50
    enablePlaceholders = false,
    initialLoadSize = 50
)
```

### Customize Loading State

In `MoviesScreen.kt`:
```kotlin
when (lazyPagingItems.loadState.refresh) {
    is LoadState.Loading -> {
        item {
            // Your custom loading UI
            CustomLoadingScreen()
        }
    }
    // ...
}
```

### Add Pull-to-Refresh

```kotlin
val refreshState = rememberPullRefreshState(
    refreshing = lazyPagingItems.loadState.refresh is LoadState.Loading,
    onRefresh = { lazyPagingItems.refresh() }
)

Box(modifier = Modifier.pullRefresh(refreshState)) {
    LazyColumn {
        // ... items
    }
    PullRefreshIndicator(
        refreshing = refreshState.isRefreshing,
        state = refreshState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
```

### Add Search/Filter

Create a new intent:
```kotlin
sealed class MoviesIntent {
    data object LoadMovies : MoviesIntent()
    data class SearchMovies(val query: String) : MoviesIntent()
    data object LoadMore : MoviesIntent()
}
```

Update ViewModel to handle search queries.

## Testing

### Unit Tests for ViewModel

```kotlin
@Test
fun testMoviesFlowEmitsData() = runTest {
    val viewModel = MoviesViewModel(mockRepository)
    val items = mutableListOf<PagingData<Result>>()
    
    val job = launch {
        viewModel.moviesFlow.collect { items.add(it) }
    }
    
    advanceUntilIdle()
    assertTrue(items.isNotEmpty())
    
    job.cancel()
}
```

### UI Tests

Use Compose testing framework to test the UI components.

## Performance Optimization

1. **Pagination**: Configured with page size of 20 items
2. **Image Caching**: Coil handles image caching automatically
3. **Memory**: `cachedIn(viewModelScope)` prevents data loss on rotation
4. **Lazy Loading**: LazyColumn only renders visible items

## Troubleshooting

### Movies not loading?
- Check API key in `Constants.kt`
- Verify internet connectivity
- Check logcat for error messages

### Images not showing?
- Ensure IMG_BASE_URL is correct
- Check internet permissions in AndroidManifest.xml

### ViewModel not injected?
- Ensure MainActivity has `@AndroidEntryPoint`
- Check Hilt configuration in `di` package

### Pagination not working?
- Verify PagingConfig settings
- Check if API supports pagination
- Ensure page parameter is correct

## File Structure Summary

```
app/src/main/java/com/laksh/demo/
├── ui/
│   ├── movies/
│   │   ├── MoviesContract.kt
│   │   ├── MoviesViewModel.kt
│   │   ├── MoviesScreen.kt
│   │   └── MovieCard.kt
│   ├── moviedetails/
│   │   ├── MovieDetailsContract.kt
│   │   ├── MovieDetailsViewModel.kt
│   │   └── MovieDetailsScreen.kt
│   └── theme/
├── data/
│   ├── repository/
│   │   └── MovieRepository.kt
│   └── paging/
│       └── MoviesPagingSource.kt
├── remote/
│   ├── MovieApi.kt
│   └── model/
├── di/
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── utils/
│   └── Constants.kt
└── MainActivity.kt
```

## Next Steps

1. **Add Navigation**: Integrate Compose Navigation for better screen navigation
2. **Add Database**: Use Room for caching movies locally
3. **Add Search**: Implement movie search functionality
4. **Add Filters**: Add genre, year, and rating filters
5. **Improve UI**: Add animations and transitions
6. **Add Tests**: Write unit and UI tests

## Resources

- [Jetpack Paging Library](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- [Compose Documentation](https://developer.android.com/compose)
- [MVI Architecture Pattern](https://blog.mindorks.com/mvi-architecture-android-tutorial-for-beginners)
- [TheMovieDB API](https://www.themoviedb.org/settings/api)

