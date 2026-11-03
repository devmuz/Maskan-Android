package com.maskan.mobileapp.data.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Maps Firebase/system errors from any login, sign-up, or password flow to a
 * short, human-readable message — raw Firebase/exception text (error codes,
 * internal class names) must never reach the screen.
 */
fun friendlyAuthErrorMessage(t: Throwable): String {
    if (t is FirebaseAuthException) {
        return when (t.errorCode) {
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email."
            "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "An account already exists with this email."
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
            "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
            "ERROR_USER_DISABLED" -> "This account has been disabled. Contact support."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
            "ERROR_NETWORK_REQUEST_FAILED" -> "No internet connection. Please check your network and try again."
            "ERROR_USER_TOKEN_EXPIRED", "ERROR_REQUIRES_RECENT_LOGIN" -> "Your session has expired. Please log in again."
            else -> "Something went wrong. Please try again."
        }
    }

    // tenantLogin's errors (e.g. "Invalid Property ID or password.") are
    // already written for the user by the Cloud Function — pass through.
    if (t is FirebaseFunctionsException) {
        return t.message ?: "Something went wrong. Please try again."
    }

    if (t is FirebaseNetworkException) {
        return "No internet connection. Please check your network and try again."
    }

    return "Something went wrong. Please try again."
}
