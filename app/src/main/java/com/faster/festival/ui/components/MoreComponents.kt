package com.faster.festival.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.faster.festival.R
import com.faster.festival.data.models.Poi
import com.faster.festival.ui.theme.FastERTheme
import com.faster.festival.ui.theme.FasterRed
import com.faster.festival.ui.theme.FasterRedDark

// Map Marker
@Composable
fun MapMarker(
        poi: Poi,
        isSelected: Boolean = false,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(
            modifier = modifier.clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
                modifier =
                        Modifier.size(if (isSelected) 56.dp else 48.dp)
                                .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color =
                                                if (isSelected) MaterialTheme.colorScheme.secondary
                                                else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                ),
                shape = RoundedCornerShape(12.dp),
                color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = poi.name,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = poi.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Draggable Shortcuts Sheet
@Composable
fun DraggableShortcutsSheet(
        shortcuts: List<String>,
        onShortcutClick: (String) -> Unit,
        modifier: Modifier = Modifier
) {
    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(bottom = 16.dp)
    ) {
        // Handle
        Box(
                modifier =
                        Modifier.align(Alignment.CenterHorizontally)
                                .padding(top = 12.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .background(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(2.dp)
                                )
        )

        // Title
        Text(
                text = stringResource(id = R.string.shortcuts),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        // Shortcuts Chips
        LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shortcuts) { shortcut ->
                FilterChip(
                        selected = false,
                        onClick = { onShortcutClick(shortcut) },
                        label = {
                            Text(text = shortcut, style = MaterialTheme.typography.labelSmall)
                        },
                        shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

// Map Placeholder
@Composable
fun MapPlaceholder(modifier: Modifier = Modifier) {
    Box(
            modifier =
                    modifier.fillMaxSize()
                            .background(
                                    Brush.verticalGradient(
                                            colors =
                                                    listOf(
                                                            FasterRed.copy(alpha = 0.6f),
                                                            FasterRedDark.copy(alpha = 0.6f)
                                                    )
                                    )
                            )
    ) {
        Text(
                text = stringResource(id = R.string.map_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center)
        )
    }
}

// Profile Header
@Composable
fun ProfileHeader(
        name: String,
        email: String,
        onEditClick: () -> Unit,
        onSettingsClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    MaterialTheme.colorScheme
                                                                            .primaryContainer,
                                                                    MaterialTheme.colorScheme
                                                                            .secondaryContainer
                                                            )
                                            )
                            )
                            .padding(16.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = Color.White,
                            modifier = Modifier.padding(12.dp)
                    )
                }

                Column {
                    Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEditClick) {
                    Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Profile Card Section
@Composable
fun ProfileCardSection(
        title: String,
        items: List<Pair<String, String>>,
        onActionClick: () -> Unit,
        actionLabel: String = "Add",
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = onActionClick) {
                Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }

        ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                items.forEachIndexed { index, (label, value) ->
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }

                    if (index < items.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

// Auth Buttons
@Composable
fun AuthButtons(
        onPhoneClick: () -> Unit,
        onEmailClick: () -> Unit,
        modifier: Modifier = Modifier,
        onSignupClick: (() -> Unit)? = null // optional signup callback
) {
    Column(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
                onClick = onPhoneClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 8.dp)
            )
            Text(stringResource(id = R.string.continue_with_phone))
        }

        Button(
                onClick = onEmailClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                        )
        ) {
            Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 8.dp)
            )
            Text(stringResource(id = R.string.continue_with_email))
        }

        // Sign Up button (only shown if callback provided)
        if (onSignupClick != null) {
            OutlinedButton(
                onClick = onSignupClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 8.dp)
                )
                Text(stringResource(id = R.string.create_account))
            }
        }
    }
}

@Preview
@Composable
fun PreviewMapMarker() {
    FastERTheme {
        MapMarker(poi = Poi("1", "Main Stage", "stage"), isSelected = true, onClick = {})
    }
}

@Preview
@Composable
fun PreviewProfileCardSection() {
    FastERTheme {
        ProfileCardSection(
                title = stringResource(id = R.string.profile_title),
                items = listOf(stringResource(id = R.string.phone) to "+1 (555) 123-4567", stringResource(id = R.string.email) to "alex@example.com"),
                onActionClick = {},
                actionLabel = stringResource(id = R.string.edit)
        )
    }
}
