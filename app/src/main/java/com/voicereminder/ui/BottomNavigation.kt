package com.voicereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Using the clean redesign palette
private val NavBgColor = Color(0xFFFFFFFF)
private val NavDividerColor = Color(0xFFE5E5EA)
private val NavActiveColor = Color(0xFF1D1D1F) // OR Color(0xFF5E5CE6) for purple
private val NavInactiveColor = Color(0xFF86868B)
private val IndicatorColor = Color(0xFF5E5CE6) // Purple accent for the active indicator

@Composable
fun AppBottomNavigation(
    currentRoute: String?,
    onNavigateToHome: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(NavBgColor)) {
        HorizontalDivider(color = NavDividerColor, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                isSelected = currentRoute == "home",
                onClick = onNavigateToHome
            )
            NavItem(
                icon = Icons.Default.List,
                label = "Reminders",
                isSelected = currentRoute == "reminders",
                onClick = onNavigateToReminders
            )
            NavItem(
                icon = if (currentRoute == "tasks") Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                label = "Tasks",
                isSelected = currentRoute == "tasks",
                onClick = onNavigateToTasks
            )
            NavItem(
                icon = if (currentRoute == "notes") Icons.Filled.Edit else Icons.Outlined.Edit,
                label = "Notes",
                isSelected = currentRoute == "notes",
                onClick = onNavigateToNotes
            )
            NavItem(
                icon = Icons.Default.Menu,
                label = "More",
                isSelected = currentRoute == "settings",
                onClick = onNavigateToMore
            )
        }
        // Bottom padding for edge-to-edge / gesture bar area
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) IndicatorColor else NavInactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) NavActiveColor else NavInactiveColor
        )
    }
}
