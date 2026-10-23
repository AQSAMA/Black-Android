package com.racas.black.controlcenter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Interface screen's Haptics switch. It returns a tick the controls call on selection, save and a slider release;
 * with haptics off the call does nothing.
 */
@Composable
fun rememberHapticTick(): () -> Unit {
    val enabled = LocalHaptics.current
    val feedback = LocalHapticFeedback.current
    return remember(enabled, feedback) {
        {
            if (enabled) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(bottom = 8.dp)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.size(6.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.size(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SettingsGroup(
    title: String?,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.6.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        ) {
            Column { content() }
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        tick()
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(Modifier.size(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
fun DividerInset() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}

@Composable
fun SwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val tick = rememberHapticTick()
    SettingRow(
        title = title,
        subtitle = subtitle,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    tick()
                    onCheckedChange(it)
                },
                enabled = enabled,
                modifier = Modifier.scale(1.08f),
            )
        },
    )
}

@Composable
fun SliderSetting(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    subtitle: String? = null,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val tick = rememberHapticTick()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .heightIn(min = 84.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    valueText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = {
                tick()
                onValueChangeFinished?.invoke()
            },
            valueRange = range,
            steps = steps,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SegmentedSetting(
    title: String,
    subtitle: String? = null,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
) {
    val tick = rememberHapticTick()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(10.dp))
        // FlowRow rather than a fixed Row: four or five choices wrap on a phone instead of shrinking below a
        // comfortable touch target.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                if (selected) {
                    Button(
                        onClick = {
                            tick()
                            onSelect(index)
                        },
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .heightIn(min = 52.dp),
                        enabled = enabled,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            tick()
                            onSelect(index)
                        },
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .heightIn(min = 52.dp),
                        enabled = enabled,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceSetting(
    title: String,
    subtitle: String? = null,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    SettingRow(
        title = title,
        subtitle = subtitle,
        onClick = if (enabled) ({ expanded = true }) else null,
        trailing = {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 52.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                ) {
                    Text(
                        options.getOrElse(selectedIndex) { "" },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                expanded = false
                                tick()
                                onSelect(index)
                            },
                            leadingIcon = {
                                if (index == selectedIndex) Icon(Icons.Rounded.Check, null)
                                else Spacer(Modifier.size(24.dp))
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun StepperSetting(
    title: String,
    valueText: String,
    subtitle: String? = null,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    enabled: Boolean = true,
) {
    val tick = rememberHapticTick()
    SettingRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        tick()
                        onDecrease()
                    },
                    enabled = enabled,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(26.dp))
                }
                Text(
                    valueText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                IconButton(
                    onClick = {
                        tick()
                        onIncrease()
                    },
                    enabled = enabled,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase", modifier = Modifier.size(26.dp))
                }
            }
        },
    )
}

@Composable
fun ActionSetting(
    title: String,
    subtitle: String? = null,
    button: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tick = rememberHapticTick()
    SettingRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Button(
                onClick = {
                    tick()
                    onClick()
                },
                enabled = enabled,
                modifier = Modifier.heightIn(min = 52.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(button)
            }
        },
    )
}

@Composable
fun NavigationSetting(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        },
    )
}

@Composable
fun InfoBanner(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (tonal) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (tonal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tonal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tonal) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AccentDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val tick = rememberHapticTick()
    Surface(
        modifier = Modifier
            .size(if (selected) 56.dp else 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton) {
                tick()
                onClick()
            },
        shape = CircleShape,
        color = color,
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
    ) {
        if (selected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (color.luminance() > 0.45f) Color.Black else Color.White,
                )
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val tick = rememberHapticTick()
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        tick()
                        onClick()
                    }
                } else {
                    Modifier
                },
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
