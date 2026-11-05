package com.maskan.mobileapp.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.maskan.mobileapp.data.util.CloudFunctionEnv
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Landlords use standard Firebase Auth email/password; their UID *is*
 * `landlordId` throughout the schema. Tenants authenticate via the
 * `tenantLogin` Cloud Function, never client-side (02-data-models.md).
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInLandlord(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUpLandlord(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    /** Exchanges a Google ID token (from Credential Manager) for a Firebase session — same account for both login and sign-up, Firebase creates it on first use. */
    suspend fun signInLandlordWithGoogle(googleIdToken: String) {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(credential).await()
    }

    /**
     * Permanently deletes the signed-in landlord and everything they own —
     * properties, tenants, bills, documents, Storage files and the Auth
     * user — via the deleteAccount{Env} Cloud Function.
     */
    suspend fun deleteAccount() {
        functions.getHttpsCallable("deleteAccount${CloudFunctionEnv.suffix}").call().await()
        auth.signOut()
    }

    /**
     * Firebase requires a recent sign-in to change the password, so this
     * re-authenticates with the current password first (which also verifies it).
     */
    suspend fun changeLandlordPassword(current: String, new: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        try {
            user.reauthenticate(EmailAuthProvider.getCredential(email, current)).await()
        } catch (t: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            throw IncorrectCurrentPasswordException()
        }
        user.updatePassword(new).await()
    }

    /** Landlords who sign in with email/password can change it in Settings. */
    val hasPasswordLogin: Boolean
        get() = auth.currentUser?.providerData?.any { it.providerId == "password" } == true

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /** Returns the uppercased, trimmed Property ID code used for the login, for session caching. */
    suspend fun signInTenant(propertyIdCode: String, password: String): String {
        val code = propertyIdCode.trim().uppercase()
        val payload = hashMapOf("propertyId" to code, "password" to password)
        val result = functions.getHttpsCallable("tenantLogin${CloudFunctionEnv.suffix}").call(payload).await()

        @Suppress("UNCHECKED_CAST")
        val resultMap = result.getData() as? Map<String, Any?>
        val token = resultMap?.get("token") as? String
            ?: error("tenantLogin did not return a token")
        auth.signInWithCustomToken(token).await()
        return code
    }

    fun signOut() {
        auth.signOut()
    }
}

class IncorrectCurrentPasswordException : Exception("Current password is incorrect.")
