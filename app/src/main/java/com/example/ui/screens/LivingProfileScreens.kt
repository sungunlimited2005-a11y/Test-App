package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.*
import com.example.ui.components.VyraUserBadgeRow
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * FEATURE 4: Living Profiles Customizer
 * Themes, banner styles, card borders, bio links, pinned content, and live preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivingProfileCustomizerScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val myPosts by repository.observeUserPosts(currentUserId).collectAsStateWithLifecycle(initialValue = emptyList())

    // Editable state
    var selectedTheme by remember { mutableStateOf(currentUser?.profileTheme?.ifBlank { "default" } ?: "default") }
    var selectedBannerStyle by remember { mutableStateOf(currentUser?.bannerStyle?.ifBlank { "gradient" } ?: "gradient") }
    var selectedCardBorder by remember { mutableStateOf(currentUser?.cardBorderStyle?.ifBlank { "neon" } ?: "neon") }
    val pinnedPostIds = remember { mutableStateListOf<String>().apply { addAll(currentUser?.pinnedPostIds ?: emptyList()) } }
    val bioLinks = remember { mutableStateListOf<BioLink>().apply { addAll(currentUser?.bioLinks ?: emptyList()) } }

    var isSaving by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val themes = listOf(
        "default" to "Obsidian Dark",
        "cyberpunk" to "Cyberpunk Neon",
        "sunset" to "Sunset Ember",
        "emerald" to "Emerald Gold",
        "frost" to "Frost Minimal"
    )

    val bannerStyles = listOf("gradient", "mesh", "geometric", "minimal")
    val cardBorders = listOf("neon", "rounded", "glass", "sleek")

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Customize Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    TextButton(onClick = { showPreviewDialog = true }) {
                        Text("Preview", color = VyraCyan, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                repository.updateProfileCustomization(
                                    profileTheme = selectedTheme,
                                    bannerStyle = selectedBannerStyle,
                                    cardBorderStyle = selectedCardBorder,
                                    pinnedPostIds = pinnedPostIds.toList(),
                                    sectionOrder = listOf("featured", "links", "posts"),
                                    bioLinks = bioLinks.toList()
                                )
                                isSaving = false
                                onNavigateBack()
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = VyraViolet),
                        modifier = Modifier.testTag("button_save_profile_customization")
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        else Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Themes
            Text("Profile Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(themes) { (id, label) ->
                    val isSelected = selectedTheme == id
                    Card(
                        modifier = Modifier
                            .clickable { selectedTheme = id }
                            .width(130.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) VyraViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(2.dp, VyraViolet) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(getThemeBrush(id))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 2: Banner Appearance
            Text("Banner Art Style", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bannerStyles.forEach { style ->
                    FilterChip(
                        selected = selectedBannerStyle == style,
                        onClick = { selectedBannerStyle = style },
                        label = { Text(style.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Section 3: Card Border Styling
            Text("Card Styling", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cardBorders.forEach { border ->
                    FilterChip(
                        selected = selectedCardBorder == border,
                        onClick = { selectedCardBorder = border },
                        label = { Text(border.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Section 4: Pin Featured Content (Up to 3 posts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pin Featured Posts (${pinnedPostIds.size}/3)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)
                if (pinnedPostIds.isNotEmpty()) {
                    TextButton(onClick = { pinnedPostIds.clear() }) { Text("Clear All", color = MaterialTheme.colorScheme.error) }
                }
            }

            if (myPosts.isEmpty()) {
                Text("No posts available to pin yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(myPosts, key = { it.id }) { post ->
                        val isPinned = post.id in pinnedPostIds
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isPinned) pinnedPostIds.remove(post.id)
                                    else if (pinnedPostIds.size < 3) pinnedPostIds.add(post.id)
                                }
                                .border(
                                    width = if (isPinned) 3.dp else 1.dp,
                                    color = if (isPinned) VyraViolet else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isPinned) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .background(VyraViolet, RoundedCornerShape(bottomStart = 8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PushPin, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: External Links
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bio & External Links", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)
                TextButton(onClick = { showAddLinkDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Link")
                }
            }

            if (bioLinks.isEmpty()) {
                Text("Add links to your portfolio, GitHub, website, or Spotify.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                bioLinks.forEachIndexed { index, link ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = VyraViolet)
                                Column {
                                    Text(link.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(link.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { bioLinks.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Link Dialog
    if (showAddLinkDialog) {
        var linkTitle by remember { mutableStateOf("") }
        var linkUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false },
            title = { Text("Add External Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = linkTitle,
                        onValueChange = { linkTitle = it },
                        label = { Text("Link Title (e.g. Portfolio, GitHub)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("URL (e.g. https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = if (linkUrl.startsWith("http://") || linkUrl.startsWith("https://")) linkUrl.trim() else "https://${linkUrl.trim()}"
                        bioLinks.add(BioLink(title = linkTitle.trim().ifBlank { "Link" }, url = url))
                        showAddLinkDialog = false
                    },
                    enabled = linkTitle.isNotBlank() && linkUrl.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLinkDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Live Preview Dialog
    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Profile Live Preview", fontWeight = FontWeight.Bold) },
            text = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(getThemeBrush(selectedTheme))
                        )
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(currentUser?.displayName ?: "User", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("@${currentUser?.username ?: "user"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Theme: $selectedTheme • Banner: $selectedBannerStyle", fontSize = 11.sp, color = VyraViolet)
                            if (bioLinks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("🔗 ${bioLinks.first().title} (${bioLinks.first().url})", fontSize = 11.sp, color = VyraCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPreviewDialog = false }) { Text("Looks Great!") }
            }
        )
    }
}

/**
 * FEATURE 8: Profile Insights Dashboard
 * Real metrics: Profile visits, post reach, reel views, followers gained, engagement rate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInsightsScreen(
    userId: String,
    repository: VyraRepository,
    onNavigateBack: () -> Unit
) {
    val insights by repository.observeProfileInsights(userId).collectAsStateWithLifecycle(initialValue = ProfileInsightsData())

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Professional Insights", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header summary card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Overview (Last 30 Days)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(color = VyraGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text("Realtime", color = VyraGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(insights.profileVisits.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = VyraViolet)
                            Text("Profile Visits", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text(insights.postReach.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = VyraCyan)
                            Text("Accounts Reached", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("${insights.engagementRate}%", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = VyraMagenta)
                            Text("Engagement", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Detailed Metric Cards
            Text("Content Performance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricMiniCard(
                    title = "Reel Views",
                    value = insights.reelViews.toString(),
                    icon = Icons.Filled.Movie,
                    color = VyraMagenta,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "Followers Growth",
                    value = "+${insights.followersGained}",
                    icon = Icons.Filled.TrendingUp,
                    color = VyraGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Top Content List
            Text("Top Performing Content", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)

            if (insights.topPosts.isEmpty()) {
                Text("No posts published yet to analyze.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                insights.topPosts.forEachIndexed { index, post ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("#${index + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = VyraViolet)

                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = post.caption.ifBlank { "Untitled Post" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("❤️ ${post.likesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("💬 ${post.commentsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (post.isReel) Text("👁️ ${post.viewsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun getThemeBrush(theme: String): Brush {
    return when (theme) {
        "cyberpunk" -> Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6)))
        "sunset" -> Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFF59E0B)))
        "emerald" -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFFF59E0B)))
        "frost" -> Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFE2E8F0)))
        else -> Brush.horizontalGradient(listOf(VyraViolet, VyraMagenta, VyraCyan))
    }
}
