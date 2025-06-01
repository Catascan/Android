package com.example.cataractscan.api.models

data class AnalysisResult(
    val message: String,
    val prediction: String,
    val explanation: String,
    val confidence_scores: ConfidenceScores,
    val photoUrl: String? = null
)

data class ConfidenceScores(
    val immature: Float,
    val mature: Float,
    val normal: Float
)