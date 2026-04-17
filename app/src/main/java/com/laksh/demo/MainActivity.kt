package com.laksh.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.laksh.demo.ui.moviedetails.MovieDetailsScreen
import com.laksh.demo.ui.movies.MoviesScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieAppNavigation()
        }
    }
}


@Composable
fun MovieAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController, startDestination = Destinations.MOVIES
    ) {
        composable(route = Destinations.MOVIES) {
            MoviesScreen(
                onMovieClick = { movie ->
                    val movieId = movie.id ?: return@MoviesScreen
                    navController.navigate(Destinations.movieDetailsRoute(movieId))
                })
        }

        composable(
            route = "${Destinations.MOVIE_DETAILS}/{${Destinations.MOVIE_ID_ARG}}",
            arguments = listOf(
                navArgument(Destinations.MOVIE_ID_ARG) {
                    type = NavType.IntType
                })) { backStackEntry ->
            val movieId =
                backStackEntry.arguments?.getInt(Destinations.MOVIE_ID_ARG) ?: return@composable
            MovieDetailsScreen(
                movieId = movieId, onBackClick = { navController.popBackStack() })
        }
    }
}

private object Destinations {
    const val MOVIES = "movies"
    const val MOVIE_DETAILS = "movie_details"
    const val MOVIE_ID_ARG = "movieId"

    fun movieDetailsRoute(movieId: Int): String = "$MOVIE_DETAILS/$movieId"
}