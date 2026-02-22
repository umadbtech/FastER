package com.faster.festival.data.repository

sealed class AuthException(message: String? = null) : Exception(message)

class SmsSendFailedException(message: String? = null) : AuthException(message)
class RateLimitException(message: String? = null, val retryAfterSeconds: Int? = null) : AuthException(message)
class PhoneValidationException(message: String? = null) : AuthException(message)
class GenericAuthException(message: String? = null) : AuthException(message)
