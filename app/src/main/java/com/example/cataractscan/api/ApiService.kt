package com.example.cataractscan.api

import com.example.cataractscan.api.models.User
import com.example.cataractscan.api.models.AnalysisResult
import com.example.cataractscan.api.models.ConfidenceScores
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

interface ApiService {
    // User Authentication
    @POST("auth/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body registerRequest: RegisterRequest
    ): Response<RegisterResponse>

    // Forgot Password
    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body forgotRequest: ForgotRequest
    ): Response<ForgotPasswordResponse>

    // Change Password (for logged-in users)
    @PATCH("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    // Reset Password (using token from email link)
    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Query("token") token: String,
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>

    // User Profile - Updated endpoint to match your API
    @GET("auth/user")
    suspend fun getUserProfile(
        @Header("Authorization") authorization: String // Pass "Bearer {token}"
    ): Response<UserProfileResponse>

    // Logout
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String // Pass "Bearer {token}"
    ): Response<LogoutResponse>

    // Primary Profile update endpoint
    @Multipart
    @PATCH("profile/edit")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Part image: MultipartBody.Part?
    ): Response<ProfileUpdateResponse>

    // Alternative Profile update endpoint (fallback)
    @Multipart
    @PATCH("auth/profile/edit")
    suspend fun updateProfileAlternative(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Part image: MultipartBody.Part?
    ): Response<ProfileUpdateResponse>

    // Profile update with text fields (if supported by backend)
    @Multipart
    @PATCH("profile/edit")
    suspend fun updateProfileWithDetails(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Part image: MultipartBody.Part?,
        @Part("username") username: okhttp3.RequestBody?,
        @Part("email") email: okhttp3.RequestBody?
    ): Response<ProfileUpdateResponse>

    // Image Analysis
    @Multipart
    @POST("user/dashboard/predict")
    suspend fun analyzeImage(
        @Header("Authorization") authorization: String, // Pass "Bearer {token}"
        @Part image: MultipartBody.Part
    ): Response<AnalysisResult>

    // Get Profile Edit - Try different possible endpoints
    @GET("profile/edit")
    suspend fun getProfileEdit(
        @Header("Authorization") token: String
    ): Response<ProfileEditResponse>

    // Alternative endpoint for profile edit
    @GET("auth/profile/edit")
    suspend fun getProfileEditAlt(
        @Header("Authorization") token: String
    ): Response<ProfileEditResponse>

    // History endpoint
    @GET("user/dashboard/history")
    suspend fun getHistory(
        @Header("Authorization") authorization: String // Pass "Bearer {token}"
    ): Response<HistoryResponse>
}

// Request data classes
data class LoginRequest(
    val login: String,
    val password: String
)

data class ForgotRequest(
    val email: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val retype_password: String
)

data class ChangePasswordRequest(
    val newPassword: String,
    val retypePassword: String
)

data class ResetPasswordRequest(
    val newPassword: String
)

// Response data classes
data class LoginResponse(
    val message: String,
    val greeting: String,
    val token: String
)

data class RegisterResponse(
    val message: String,
    val user: RegisterUser
)

data class RegisterUser(
    val id: Int,
    val username: String,
    val email: String
)

data class LogoutResponse(
    val message: String
)

// UPDATED: ProfileUpdateResponse data class
data class ProfileUpdateResponse(
    val message: String,
    val user: UserProfileUpdate?
)

data class UserProfileUpdate(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("image_link")
    val imageLink: String?,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("updatedAt")
    val updatedAt: String?
)

data class ForgotPasswordResponse(
    val message: String,
    val resetLink: String? = null
)

data class ChangePasswordResponse(
    val message: String
)

data class ResetPasswordResponse(
    val message: String
)

data class HistoryResponse(
    val message: String,
    val history: List<HistoryItem>
)

data class HistoryItem(
    val id: Int,
    val prediction: String,
    val explanation: String,
    val createdAt: String,
    val updatedAt: String,
    val photoUrl: String,
    // Add confidence scores to history
    @SerializedName("confidence_scores")
    val confidenceScores: ConfidenceScores? = null
)

// Profile Response classes - Updated to match API response
data class ProfileResponse(
    val message: String,
    val user: UserProfile
)

// ProfileEditResponse for getProfileEdit endpoint
data class ProfileEditResponse(
    val message: String,
    val user: ProfileEditUser
)

data class ProfileEditUser(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("image_link")
    val imageLink: String?,
    val createdAt: String,
    val updatedAt: String
)

// UserProfileResponse for getUserProfile endpoint
data class UserProfileResponse(
    val message: String,
    val user: UserProfileData
)

data class UserProfileData(
    val id: Int,
    val username: String,
    @SerializedName("image_link")
    val imageLink: String?
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("image_link")
    val imageLink: String?,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)

