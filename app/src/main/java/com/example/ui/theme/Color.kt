package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// VYRA Signature Brand Colors
val VyraViolet = Color(0xFF8B5CF6)
val VyraIndigo = Color(0xFF6366F1)
val VyraMagenta = Color(0xFFEC4899)
val VyraCyan = Color(0xFF06B6D4)
val VyraGreen = Color(0xFF10B981) // Close friends & verified status

// Dark Obsidian Theme (Primary Aesthetic)
val DarkBackground = Color(0xFF090712)
val DarkSurface = Color(0xFF120E21)
val DarkSurfaceElevated = Color(0xFF1B162E)
val DarkSurfaceCard = Color(0xFF221C3A)
val DarkBorder = Color(0xFF2C2449)
val DarkBorderSubtle = Color(0xFF1D1833)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

// Light Theme
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF94A3B8)

// Gradients
val VyraStoryGradient = Brush.linearGradient(
    colors = listOf(VyraMagenta, VyraViolet, VyraIndigo, VyraCyan)
)

val VyraCloseFriendsGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF059669))
)

val VyraButtonGradient = Brush.horizontalGradient(
    colors = listOf(VyraViolet, VyraMagenta)
)
