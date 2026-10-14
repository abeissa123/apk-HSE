package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.RealtimeWeatherData
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainTab

@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // CIMFIG Badge
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp).padding(end = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color(0xFF059669), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "CIMFIG",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // CIMENCAM center branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE11D48), Color(0xFF10B981))
                        ),
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "CIMENCAM",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp
            )
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(2.dp)
                    .background(HSEBlueLight)
            )
        }

        // HOLCIM SPIRIT
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "HOLCIM",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "SPIRIT",
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun HseTopBar(
    userName: String,
    avatarInitials: String,
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bonjour",
                color = HSETextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = userName,
                color = HSETextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Notifications button with badge
            Box {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(HSECardElevated, CircleShape)
                        .testTag("notifications_bell_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = HSETextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(HSERedUrgent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadNotifications.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // User Avatar ("DG")
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(HSEBluePrimary)
                    .clickable(onClick = onProfileClick)
                    .testTag("profile_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun SiteWeatherPill(
    assignedSite: String,
    weatherData: RealtimeWeatherData = RealtimeWeatherData(),
    onRefreshWeather: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val weatherIcon = when {
        weatherData.weatherCode in listOf(95, 96, 99) || weatherData.weatherCondition.contains("Orage", ignoreCase = true) -> Icons.Default.Thunderstorm
        weatherData.weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 80, 81, 82) || weatherData.weatherCondition.contains("Pluie", ignoreCase = true) || weatherData.weatherCondition.contains("Averse", ignoreCase = true) || weatherData.weatherCondition.contains("Bruine", ignoreCase = true) -> Icons.Default.WaterDrop
        weatherData.weatherCode in listOf(3, 45, 48) || weatherData.weatherCondition.contains("Nuageux", ignoreCase = true) || weatherData.weatherCondition.contains("Couvert", ignoreCase = true) || weatherData.weatherCondition.contains("Brume", ignoreCase = true) -> Icons.Default.Cloud
        else -> Icons.Default.WbSunny
    }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Surface(
            color = HSEBlueDark.copy(alpha = 0.90f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onRefreshWeather != null) { onRefreshWeather?.invoke() }
                .testTag("site_weather_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Live Weather Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = "Météo en direct",
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${weatherData.temperatureCelsius}°C",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• ${weatherData.weatherCondition}",
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (weatherData.isLive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(HSEGreenSafe, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Météo en direct • ${weatherData.lastUpdated}",
                                    color = HSEGreenSafe,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 4.dp)
                )

                // Unchangeable Assigned Site Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFE8F5E9),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = assignedSite.ifBlank { "Site de Figuil" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Site affecté verrouillé",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Site affecté",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HseBottomNavBar(
    currentTab: MainTab,
    isAdmin: Boolean = false,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = HSECardBg,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = HSECardBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Dashboard,
                label = "Accueil",
                isSelected = currentTab == MainTab.DASHBOARD,
                onClick = { onTabSelected(MainTab.DASHBOARD) },
                testTag = "nav_dashboard"
            )

            if (isAdmin) {
                BottomNavItem(
                    icon = Icons.Default.PermMedia,
                    label = "Contenus",
                    isSelected = currentTab == MainTab.CONTENUS,
                    onClick = { onTabSelected(MainTab.CONTENUS) },
                    testTag = "nav_contenus"
                )

                BottomNavItem(
                    icon = Icons.Default.Group,
                    label = "Utilisateurs",
                    isSelected = currentTab == MainTab.UTILISATEURS,
                    onClick = { onTabSelected(MainTab.UTILISATEURS) },
                    testTag = "nav_utilisateurs"
                )
            } else {
                BottomNavItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "Programme",
                    isSelected = currentTab == MainTab.PROGRAMME,
                    onClick = { onTabSelected(MainTab.PROGRAMME) },
                    testTag = "nav_programme"
                )
            }

            BottomNavItem(
                icon = Icons.Default.Shield,
                label = "Références",
                isSelected = currentTab == MainTab.POLITIQUES,
                onClick = { onTabSelected(MainTab.POLITIQUES) },
                testTag = "nav_politiques"
            )

            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profil",
                isSelected = currentTab == MainTab.PROFIL,
                onClick = { onTabSelected(MainTab.PROFIL) },
                testTag = "nav_profil"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) HSECardElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) HSEBluePrimary else HSETextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) HSEBluePrimary else HSETextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(18.dp)
                    .height(2.5.dp)
                    .background(HSEBluePrimary, RoundedCornerShape(1.dp))
            )
        }
    }
}
