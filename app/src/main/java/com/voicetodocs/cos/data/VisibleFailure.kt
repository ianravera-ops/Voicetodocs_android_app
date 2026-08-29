package com.voicetodocs.cos.data

/**
 * User-visible error for Record and inbox load. Never empty; always retryable.
 */
data class VisibleFailure(val message: String) {
    companion object {
        fun of(error: Throwable): VisibleFailure {
            val fromCos = (error as? CosException)?.userMessage?.trim().orEmpty()
            val fromMessage = error.message?.trim().orEmpty()
            val text = when {
                fromCos.isNotBlank() -> fromCos
                fromMessage.isNotBlank() -> fromMessage
                else -> error.toString()
            }
            return VisibleFailure(text)
        }
    }
}
