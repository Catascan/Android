package com.example.cataractscan.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
// Import model User dari package API Anda (jika masih digunakan)
import com.example.cataractscan.api.models.User
// Import model UserProfile dari package API Anda
import com.example.cataractscan.api.UserProfile
// Import UserProfileData dari getUserProfile endpoint (jika masih digunakan)
import com.example.cataractscan.api.UserProfileData
// Import ProfileEditUser (asumsi ini adalah model yang Anda maksud)
import com.example.cataractscan.api.ProfileEditUser // Asumsi ini adalah model yang Anda gunakan untuk saveUserProfile(ProfileEditUser)

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("CataractScanPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PreferenceManager"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_USER_ID = "key_user_id" // Jika ID dari model User berupa String
        private const val KEY_USER_ID_INT = "user_id_int" // Untuk ID Integer dari UserProfile
        private const val KEY_NAME = "key_name" // Dari model User
        private const val KEY_USERNAME = "username" // Dari model UserProfile
        private const val KEY_EMAIL = "key_email"
        private const val KEY_PROFILE_IMAGE = "key_profile_image" // Kunci untuk menyimpan imageLink
        private const val KEY_LOGIN_EMAIL = "key_login_email"
        private const val KEY_LOGIN_PASSWORD = "key_login_password"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"

        // KONSTANTA BARU UNTUK STATISTIK
        private const val KEY_TOTAL_SCANS = "total_scans"
        private const val KEY_DETECTED_CASES = "detected_cases"
        private const val KEY_ACCURACY = "accuracy"
    }

    // Auth related methods
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
        Log.d(TAG, "Token saved")
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "User logged out, preferences cleared")
    }

    // User profile methods (Metode-metode yang sudah ada)
    // Metode ini untuk menyimpan profil dari model User (misal dari /user endpoint)
    fun saveUserProfile(user: com.example.cataractscan.api.models.User) {
        Log.d(TAG, "Saving user profile (User model): ${user.name}")
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_NAME, user.name)
            putString(KEY_EMAIL, user.email)
            putString(KEY_PROFILE_IMAGE, user.profileImage) // Asumsi user.profileImage sudah ada
            user.token?.let { putString(KEY_TOKEN, it) }
            apply()
        }
        Log.d(TAG, "User profile (User model) saved successfully")
    }

    // Metode ini untuk menyimpan profil dari model UserProfile (misal dari /auth/profile/edit endpoint)
    fun saveUserProfile(userProfile: UserProfile) { // Overload method
        Log.d(TAG, "Saving user profile (UserProfile model): ${userProfile.username}")
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_USER_ID_INT, userProfile.id)
        editor.putString(KEY_USERNAME, userProfile.username)
        editor.putString(KEY_EMAIL, userProfile.email)
        editor.putString(KEY_PROFILE_IMAGE, userProfile.imageLink) // <<< imageLink disimpan di sini
        editor.putString(KEY_CREATED_AT, userProfile.createdAt)
        editor.putString(KEY_UPDATED_AT, userProfile.updatedAt)
        editor.apply()
        Log.d(TAG, "User profile (UserProfile model) saved successfully - imageLink: ${userProfile.imageLink}")
    }

    // New method to save user profile data from getUserProfile endpoint response
    fun saveUserProfileData(userProfileData: UserProfileData) {
        Log.d(TAG, "Saving user profile data: ${userProfileData.username}")
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_USER_ID_INT, userProfileData.id)
        editor.putString(KEY_USERNAME, userProfileData.username)
        editor.putString(KEY_PROFILE_IMAGE, userProfileData.imageLink)
        editor.apply()
        Log.d(TAG, "User profile data saved successfully - imageLink: ${userProfileData.imageLink}")
    }

    // Helper method to save specific profile data fields
    fun saveUserProfileData(userId: Int, username: String, imageLink: String?) {
        Log.d(TAG, "Saving user profile data manually: $username")
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_USER_ID_INT, userId)
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_PROFILE_IMAGE, imageLink)
        editor.apply()
        Log.d(TAG, "User profile data saved manually - imageLink: $imageLink")
    }

    fun saveLoginCredentials(email: String, password: String) {
        sharedPreferences.edit().apply {
            putString(KEY_LOGIN_EMAIL, email)
            putString(KEY_LOGIN_PASSWORD, password)
            apply()
        }
        Log.d(TAG, "Login credentials saved")
    }

    // Method to save email after login
    fun saveEmail(email: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
        Log.d(TAG, "Email saved: $email")
    }

    // Method to save username after login
    fun saveUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
        Log.d(TAG, "Username saved: $username")
    }

    // NEW METHOD: Save profile image individually
    fun saveProfileImage(imageUrl: String?) {
        sharedPreferences.edit().putString(KEY_PROFILE_IMAGE, imageUrl).apply()
        Log.d(TAG, "Profile image saved: $imageUrl")
    }

    fun getLoginEmail(): String? {
        return sharedPreferences.getString(KEY_LOGIN_EMAIL, null)
    }

    fun getLoginPassword(): String? {
        return sharedPreferences.getString(KEY_LOGIN_PASSWORD, null)
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getUserIdInt(): Int {
        return sharedPreferences.getInt(KEY_USER_ID_INT, 0)
    }

    fun getName(): String? { // Ini mungkin perlu disinkronkan dengan getUsername()
        return sharedPreferences.getString(KEY_NAME, null)
    }

    fun getUsername(): String? { // Untuk UserProfile model
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    fun getProfileImage(): String? {
        val imageUrl = sharedPreferences.getString(KEY_PROFILE_IMAGE, null)
        Log.d(TAG, "Retrieved profile image: $imageUrl")
        return imageUrl
    }

    fun getCreatedAt(): String? {
        return sharedPreferences.getString(KEY_CREATED_AT, null)
    }

    fun getUpdatedAt(): String? {
        return sharedPreferences.getString(KEY_UPDATED_AT, null)
    }

    // FUNGSI BARU YANG ANDA MINTA DITAMBAHKAN DI SINI
    fun saveUserProfile(userProfile: ProfileEditUser) {
        Log.d(TAG, "Saving user profile (ProfileEditUser model): ${userProfile.username}, ID: ${userProfile.id}")
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_USER_ID_INT, userProfile.id)
        editor.putString(KEY_USERNAME, userProfile.username)
        editor.putString(KEY_EMAIL, userProfile.email)
        editor.putString(KEY_PROFILE_IMAGE, userProfile.imageLink) // Asumsi ProfileEditUser memiliki imageLink
        // Asumsi ProfileEditUser tidak memiliki createdAt/updatedAt jika tidak ada di modelnya
        editor.apply()
        Log.d(TAG, "User profile (ProfileEditUser model) saved successfully - imageLink: ${userProfile.imageLink}")
    }

    // ============= STATISTIK METHODS =============

    // Method untuk menyimpan total scan
    fun saveTotalScans(total: Int) {
        sharedPreferences.edit().putInt(KEY_TOTAL_SCANS, total).apply()
        Log.d(TAG, "Total scans saved: $total")
    }

    // Method untuk mendapatkan total scan
    fun getTotalScans(): Int? {
        val total = sharedPreferences.getInt(KEY_TOTAL_SCANS, 0)
        Log.d(TAG, "Retrieved total scans: $total")
        return if (total == 0 && !sharedPreferences.contains(KEY_TOTAL_SCANS)) null else total
    }

    // Method untuk menyimpan jumlah kasus yang terdeteksi
    fun saveDetectedCases(detected: Int) {
        sharedPreferences.edit().putInt(KEY_DETECTED_CASES, detected).apply()
        Log.d(TAG, "Detected cases saved: $detected")
    }

    // Method untuk mendapatkan jumlah kasus yang terdeteksi
    fun getDetectedCases(): Int? {
        val detected = sharedPreferences.getInt(KEY_DETECTED_CASES, 0)
        Log.d(TAG, "Retrieved detected cases: $detected")
        return if (detected == 0 && !sharedPreferences.contains(KEY_DETECTED_CASES)) null else detected
    }

    // Method untuk menyimpan akurasi
    fun saveAccuracy(accuracy: Int) {
        sharedPreferences.edit().putInt(KEY_ACCURACY, accuracy).apply()
        Log.d(TAG, "Accuracy saved: $accuracy%")
    }

    // Method untuk mendapatkan akurasi
    fun getAccuracy(): Int? {
        val accuracy = sharedPreferences.getInt(KEY_ACCURACY, 95) // Default 95%
        Log.d(TAG, "Retrieved accuracy: $accuracy%")
        return accuracy
    }

    // Method untuk increment total scans (ketika user melakukan scan baru)
    fun incrementTotalScans() {
        val currentTotal = getTotalScans() ?: 0
        saveTotalScans(currentTotal + 1)
        Log.d(TAG, "Total scans incremented to: ${currentTotal + 1}")
    }

    // Method untuk increment detected cases (ketika terdeteksi katarak)
    fun incrementDetectedCases() {
        val currentDetected = getDetectedCases() ?: 0
        saveDetectedCases(currentDetected + 1)
        Log.d(TAG, "Detected cases incremented to: ${currentDetected + 1}")
    }

    // Method untuk reset semua statistik
    fun resetStatistics() {
        sharedPreferences.edit().apply {
            remove(KEY_TOTAL_SCANS)
            remove(KEY_DETECTED_CASES)
            remove(KEY_ACCURACY)
            apply()
        }
        Log.d(TAG, "All statistics reset")
    }

    // Method untuk menyimpan semua statistik sekaligus
    fun saveAllStatistics(totalScans: Int, detectedCases: Int, accuracy: Int) {
        sharedPreferences.edit().apply {
            putInt(KEY_TOTAL_SCANS, totalScans)
            putInt(KEY_DETECTED_CASES, detectedCases)
            putInt(KEY_ACCURACY, accuracy)
            apply()
        }
        Log.d(TAG, "All statistics saved - Scans: $totalScans, Detected: $detectedCases, Accuracy: $accuracy%")
    }
}