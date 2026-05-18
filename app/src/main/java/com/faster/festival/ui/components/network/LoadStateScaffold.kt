package com.faster.festival.ui.components.network

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.faster.festival.ui.util.LoadState

/**
 * One-liner scaffold that fans a [LoadState] out to the right surface:
 *  Loading → CircularProgressIndicator
 *  Success → caller-supplied [content]
 *  Empty   → [EmptyState]
 *  Offline → [NoInternetScreen]
 *  Error   → [ErrorRetryScreen]
 */
@Composable
fun <T> LoadStateScaffold(
    state: LoadState<T>,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is LoadState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        is LoadState.Success -> content(state.data)
        is LoadState.Empty -> EmptyState()
        is LoadState.Offline -> NoInternetScreen(onRetry = onRetry)
        is LoadState.Error -> ErrorRetryScreen(message = state.message, onRetry = onRetry)
    }
}
