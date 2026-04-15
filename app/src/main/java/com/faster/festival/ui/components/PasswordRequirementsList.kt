package com.faster.festival.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.faster.festival.utils.PasswordValidator

/**
 * Live password-rules checklist. Green check = rule met, gray X = rule unmet.
 * Animates in/out so the list only appears after the user starts typing.
 */
@Composable
fun PasswordRequirementsList(
    password: String,
    modifier: Modifier = Modifier,
    visibleWhen: (String) -> Boolean = { it.isNotEmpty() }
) {
    val result = PasswordValidator.validate(password)

    AnimatedVisibility(
        visible = visibleWhen(password),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(
            modifier = modifier.padding(top = 4.dp, start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Rule(
                label = "${PasswordValidator.MIN_LENGTH}–${PasswordValidator.MAX_LENGTH} characters",
                met = result.hasValidLength
            )
            Rule(label = "Uppercase letter (A–Z)", met = result.hasUppercase)
            Rule(label = "Lowercase letter (a–z)", met = result.hasLowercase)
            Rule(label = "Number (0–9)", met = result.hasDigit)
            Rule(label = "Special character (!@#\$ …)", met = result.hasSpecialChar)
        }
    }
}

@Composable
private fun Rule(label: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (met) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (met) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
