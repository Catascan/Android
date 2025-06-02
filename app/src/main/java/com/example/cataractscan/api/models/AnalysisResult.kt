package com.example.cataractscan.api.models

import com.google.gson.annotations.SerializedName

// Updated AnalysisResult to match API response structure
data class AnalysisResult(
    val message: String,
    val prediction: String,
    val explanation: String,
    @SerializedName("confidence_scores")
    val confidenceScores: ConfidenceScores,
    @SerializedName("photo_url")
    val photoUrl: String? = null,
    val id: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

// Updated ConfidenceScores with proper serialization
data class ConfidenceScores(
    @SerializedName("immature")
    val immature: Float,
    @SerializedName("mature")
    val mature: Float,
    @SerializedName("normal")
    val normal: Float
)

// Alternative version if your API returns different field names
data class AnalysisResultAlt(
    val message: String,
    val prediction: String,
    val explanation: String,
    @SerializedName("confidence_score")
    val confidenceScore: ConfidenceScoresAlt? = null,
    @SerializedName("confidence_scores")
    val confidenceScores: ConfidenceScores? = null,
    @SerializedName("photo_url")
    val photoUrl: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    val id: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

// Alternative confidence scores structure (in case API uses different format)
data class ConfidenceScoresAlt(
    val immature: Float,
    val mature: Float,
    val normal: Float
)