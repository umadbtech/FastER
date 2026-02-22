package android.util

// Minimal Patterns stub for JVM unit tests - supports EMAIL_ADDRESS.matcher(email).matches()
class _Matcher(private val regex: Regex, private val input: String) {
    fun matches(): Boolean = regex.matches(input)
}

class _Pattern(private val regex: Regex) {
    fun matcher(input: String): _Matcher = _Matcher(regex, input)
}

object Patterns {
    val EMAIL_ADDRESS = _Pattern(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
}
