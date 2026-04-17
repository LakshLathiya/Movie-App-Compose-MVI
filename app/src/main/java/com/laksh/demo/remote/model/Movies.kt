package com.laksh.demo.remote.model


data class Movies(
    val page: Int? = null,
    val results: List<Result>? = null,
    val total_pages: Int? = null,
    val total_results: Int? = null
)

data class Result(
    val backdrop_path: String? = null,
    val id: Int? = null,
    val release_date: String? = null,
    val title: String? = null,
    val vote_average: Double? = null,
)
