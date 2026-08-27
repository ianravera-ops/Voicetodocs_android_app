package com.voicetodocs.cos.data

class CosException(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

class NeedsUserConsent(val pendingIntent: android.app.PendingIntent) : Exception("User consent required")

class TokenExpiredException : Exception("Google access token expired")
