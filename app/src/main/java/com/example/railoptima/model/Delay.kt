package com.example.railoptima.model

data class Delay(
    val rakeId: String,
    val minutes: Int,
    val station: String? = null
)