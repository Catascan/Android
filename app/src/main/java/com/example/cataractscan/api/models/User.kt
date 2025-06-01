package com.example.cataractscan.api.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImage: String?,
    val token: String?
)

