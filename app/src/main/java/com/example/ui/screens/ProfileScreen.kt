package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.Post
import com.example.model.StoryHighlight
import com.example.model.UserProfile
import com.example.ui.components.QrProfileDialog
import com.example.ui.components.VyraUserBadgeRow
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class ProfileTab {
    POSTS, REELS, SAVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileUserId: String,
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCreatorStudio: () -> Unit = {},
    onNavigateToCustomizer: () -> Unit = {},
    onNavigateToInsights: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onSelectPost: (Post) -> Unit = {},
    onNavigateToUser: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isOwnProfile = profileUserId == currentUserId

    var userProfile by remember { mutableStateOf<UserProfile?>(if (isOwnProfile) currentUser else null) }
    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAvatarPickerModal by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Highlights state
    val highlights by repository.observeUserHighlights(profileUserId).collectAsStateWithLifecycle(initialValue = emptyList())
    var showCreateHighlightDialog by remember { mutableStateOf(false) }
    var activeViewingHighlight by remember { mutableStateOf<StoryHighlight?>(null) }

    val userPosts by repository.observeUserPosts(profileUserId).collectAsStateWithLifecycle(initialValue = emptyList())
    val userReels by repository.observeUserReels(profileUserId).collectAsStateWithLifecycle(initialValue = emptyList())
    val savedPosts by repository.observeSavedPosts().collectAsStateWithLifecycle(initialValue = emptyList())
    val isFollowing by repository.isFollowing(profileUserId).collectAsStateWithLifecycle(initialValue = false)
    val blockedUsers by repository.observeBlockedUsers().collectAsStateWithLifecycle(initialValue = emptyList())
    val isBlocked = profileUserId in blockedUsers

    LaunchedEffect(profileUserId) {
        if (!isOwnProfile) {
            repository.recordProfileVisit(profileUserId)
            repository.observeUserProfile(profileUserId).collect { p ->
                userProfile = p
            }
        }
    }

    if (onNavigateBack != null) {
        BackHandler { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = userProfile?.username?.let { "@$it" } ?: "Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        // FEATURE 1: Badges in top bar
                        userProfile?.badges?.let { badges ->
                            VyraUserBadgeRow(badges = badges, size = 15.dp)
                        }
                    }
                },
                actions = {
                    // FEATURE 7: QR Code button
                    IconButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.testTag("open_qr_profile_button")
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "QR Code", tint = VyraViolet)
                    }

                    if (isOwnProfile) {
                        IconButton(onClick = onNavigateToCreatorStudio) {
                            Icon(Icons.Filled.Insights, contentDescription = "Creator Studio", tint = VyraMagenta)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    } else {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Big Avatar (FEATURE 2: Profile photo picker integration)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, VyraViolet, CircleShape)
                            .clickable(enabled = isOwnProfile) {
                                showAvatarPickerModal = true
                            }
                            .testTag("profile_avatar_image")
                    ) {
                        if (!userProfile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile?.avatarUrl,
                                contentDescription = userProfile?.username,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(VyraViolet),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile?.username?.take(1)?.uppercase() ?: "V",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Edit avatar overlay badge for own profile
                        if (isOwnProfile) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(VyraMagenta)
                                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Edit photo", tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                    }

                    // Stats: Posts, Followers, Following
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(count = userPosts.size.toLong(), label = "Posts")
                        StatItem(count = userProfile?.followersCount ?: 0L, label = "Followers")
                        StatItem(count = userProfile?.followingCount ?: 0L, label = "Following")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display Name & Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = userProfile?.displayName ?: "Creator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    userProfile?.badges?.let { badges ->
                        VyraUserBadgeRow(badges = badges, size = 16.dp)
                    }
                }

                if (userProfile?.isPrivate == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Private", tint = VyraMagenta, modifier = Modifier.size(14.dp))
                        Text("Private Account", fontSize = 12.sp, color = VyraMagenta, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Bio
                if (!userProfile?.bio.isNullOrBlank()) {
                    Text(
                        text = userProfile?.bio ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                // FEATURE 4: Living Profiles Banner
                val bannerStyle = userProfile?.bannerStyle ?: "none"
                if (bannerStyle != "none" && bannerStyle.isNotBlank()) {
                    val bannerBrush = when (bannerStyle) {
                        "neon" -> androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(VyraCyan, VyraMagenta))
                        "sunset" -> androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68)))
                        "cyberpunk" -> androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF00F0FF), Color(0xFF7000FF)))
                        "dark_slate" -> androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        else -> androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(VyraViolet, VyraMagenta))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bannerBrush)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = "LIVING PROFILE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Website Link
                if (!userProfile?.websiteUrl.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(userProfile?.websiteUrl))
                                    context.startActivity(intent)
                                } catch (ignored: Exception) {}
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = VyraCyan, modifier = Modifier.size(13.dp))
                        Text(
                            text = userProfile?.websiteUrl ?: "",
                            fontSize = 13.sp,
                            color = VyraCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // FEATURE 4: Bio Links Chips
                val bioLinksList = userProfile?.bioLinks ?: emptyList()
                if (bioLinksList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bioLinksList) { link ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                        context.startActivity(intent)
                                    } catch (ignored: Exception) {}
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Link, contentDescription = null, tint = VyraViolet, modifier = Modifier.size(12.dp))
                                    Text(link.title.ifBlank { link.url }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons: Edit Profile or Follow / Message
                if (isOwnProfile) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_edit_profile"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Edit Profile", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = onNavigateToCustomizer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_customize_living_profile"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet.copy(alpha = 0.25f))
                            ) {
                                Icon(Icons.Filled.Palette, contentDescription = null, tint = VyraViolet, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Living Theme", color = VyraViolet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToInsights(profileUserId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_profile_insights"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Filled.BarChart, contentDescription = null, tint = VyraCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insights", color = VyraCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = onNavigateToCreatorStudio,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_creator_studio"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Creator Studio", color = VyraMagenta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (isFollowing) {
                                        repository.unfollowUser(profileUserId)
                                    } else {
                                        if (userProfile != null) {
                                            repository.followUser(userProfile!!)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_follow_user"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else VyraViolet
                            )
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (userProfile != null) {
                                    scope.launch {
                                        val convId = repository.getOrCreateDirectConversation(userProfile!!)
                                        onNavigateToChat(convId)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_message_user"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Message", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // FEATURE 5: Story Highlights Tray
                Spacer(modifier = Modifier.height(14.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().testTag("story_highlights_tray")
                ) {
                    // If own profile, show "+" New Highlight button
                    if (isOwnProfile) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { showCreateHighlightDialog = true }
                                    .testTag("add_highlight_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "New Highlight", tint = VyraViolet, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("New", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Existing Highlights
                    items(highlights, key = { it.id }) { highlight ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { activeViewingHighlight = highlight }
                                .testTag("highlight_item_${highlight.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, VyraViolet, CircleShape)
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                            ) {
                                AsyncImage(
                                    model = highlight.coverUrl,
                                    contentDescription = highlight.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = highlight.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(max = 66.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Profile Tabs: Posts, Reels, Saved
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = VyraViolet
            ) {
                Tab(
                    selected = selectedTab == ProfileTab.POSTS,
                    onClick = { selectedTab = ProfileTab.POSTS },
                    icon = { Icon(Icons.Filled.GridOn, contentDescription = "Posts") }
                )
                Tab(
                    selected = selectedTab == ProfileTab.REELS,
                    onClick = { selectedTab = ProfileTab.REELS },
                    icon = { Icon(Icons.Filled.Movie, contentDescription = "Reels") }
                )
                if (isOwnProfile) {
                    Tab(
                        selected = selectedTab == ProfileTab.SAVED,
                        onClick = { selectedTab = ProfileTab.SAVED },
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Saved") }
                    )
                }
            }

            // Grid Content
            val pinnedIds = userProfile?.pinnedPostIds ?: emptyList()
            when (selectedTab) {
                ProfileTab.POSTS -> {
                    val postsList = userPosts
                    if (postsList.isEmpty()) {
                        EmptyTabBox("No photo posts yet.")
                    } else {
                        PostGrid(posts = postsList, pinnedIds = pinnedIds, onSelectPost = onSelectPost)
                    }
                }
                ProfileTab.REELS -> {
                    val reelsList = userReels
                    if (reelsList.isEmpty()) {
                        EmptyTabBox("No reels published yet.")
                    } else {
                        PostGrid(posts = reelsList, pinnedIds = pinnedIds, onSelectPost = onSelectPost)
                    }
                }
                ProfileTab.SAVED -> {
                    if (savedPosts.isEmpty()) {
                        EmptyTabBox("No saved posts. Bookmark posts from feed to see them here.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("saved_posts_grid"),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(savedPosts, key = { it.id }) { saved ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = saved.mediaUrl,
                                        contentDescription = saved.caption,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // FEATURE 7: QR Profile Dialog
    if (showQrDialog && userProfile != null) {
        QrProfileDialog(
            user = userProfile!!,
            onDismiss = { showQrDialog = false },
            onNavigateToUser = onNavigateToUser
        )
    }

    // FEATURE 2: Profile Photo Picker Modal
    if (showAvatarPickerModal && isOwnProfile) {
        ProfilePhotoPickerSheet(
            currentAvatarUrl = userProfile?.avatarUrl ?: "",
            repository = repository,
            onDismiss = { showAvatarPickerModal = false },
            onAvatarUpdated = { newUrl ->
                userProfile = userProfile?.copy(avatarUrl = newUrl)
                showAvatarPickerModal = false
            }
        )
    }

    // FEATURE 5: Create Highlight Dialog
    if (showCreateHighlightDialog && isOwnProfile) {
        CreateHighlightDialog(
            userPosts = userPosts,
            onDismiss = { showCreateHighlightDialog = false },
            onSave = { title, coverUrl, selectedIds ->
                scope.launch {
                    repository.createHighlight(title = title, coverUrl = coverUrl, storyIds = selectedIds)
                    showCreateHighlightDialog = false
                }
            }
        )
    }

    // FEATURE 5: Highlight Viewer
    if (activeViewingHighlight != null) {
        HighlightViewerDialog(
            highlight = activeViewingHighlight!!,
            isOwner = isOwnProfile,
            onDismiss = { activeViewingHighlight = null },
            onDelete = {
                scope.launch {
                    repository.deleteHighlight(activeViewingHighlight!!.id)
                    activeViewingHighlight = null
                }
            }
        )
    }

    if (showEditProfileDialog && userProfile != null) {
        EditProfileModal(
            currentProfile = userProfile!!,
            repository = repository,
            onDismiss = { showEditProfileDialog = false },
            onProfileUpdated = { updated ->
                userProfile = updated
                showEditProfileDialog = false
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Profile Options") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showReportDialog = false
                            scope.launch { repository.toggleBlockUser(profileUserId, isBlocked) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isBlocked) "Unblock User" else "Block User", color = MaterialTheme.colorScheme.error)
                    }

                    TextButton(
                        onClick = {
                            showReportDialog = false
                            scope.launch { repository.muteUser(profileUserId, mute = true) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mute User", color = MaterialTheme.colorScheme.error)
                    }

                    TextButton(
                        onClick = {
                            showReportDialog = false
                            scope.launch { repository.restrictUser(profileUserId, restrict = true) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restrict User", color = MaterialTheme.colorScheme.error)
                    }

                    TextButton(
                        onClick = {
                            showReportDialog = false
                            scope.launch { repository.reportContent("user", profileUserId, "Inappropriate profile", "") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Report User", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * FEATURE 2: Profile Photo Picker Sheet with PhotoPicker, Crop/Preview, Firebase Storage upload, Remove
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoPickerSheet(
    currentAvatarUrl: String,
    repository: VyraRepository,
    onDismiss: () -> Unit,
    onAvatarUpdated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var previewUrl by remember { mutableStateOf(currentAvatarUrl) }
    var isUploading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            previewUrl = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("avatar_picker_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Change Profile Picture",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar Preview Circle with zoom/crop framing
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, VyraViolet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (previewUrl.isNotBlank()) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = "Preview Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("choose_gallery_photo_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose from Gallery")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preset Avatars Row
            Text("Or select aesthetic avatar preset", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PRESET_AVATARS) { preset ->
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (previewUrl == preset) 2.5.dp else 1.dp,
                                color = if (previewUrl == preset) VyraMagenta else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedUri = null
                                previewUrl = preset
                            }
                    ) {
                        AsyncImage(model = preset, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save and Apply
            Button(
                onClick = {
                    isUploading = true
                    statusText = null
                    scope.launch {
                        try {
                            val finalUrl = if (selectedUri != null) {
                                repository.uploadMedia(selectedUri!!, "avatars")
                            } else {
                                previewUrl
                            }
                            repository.updateProfilePhoto(finalUrl)
                            isUploading = false
                            onAvatarUpdated(finalUrl)
                        } catch (e: Exception) {
                            isUploading = false
                            statusText = e.localizedMessage ?: "Failed to update profile picture"
                        }
                    }
                },
                enabled = !isUploading && previewUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_avatar_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Apply Profile Picture", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remove Current Picture
            if (currentAvatarUrl.isNotBlank()) {
                TextButton(
                    onClick = {
                        isUploading = true
                        scope.launch {
                            try {
                                repository.removeProfilePhoto()
                                isUploading = false
                                onAvatarUpdated("")
                            } catch (e: Exception) {
                                isUploading = false
                                statusText = e.localizedMessage
                            }
                        }
                    },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth().testTag("remove_avatar_button")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove Current Picture", color = MaterialTheme.colorScheme.error)
                }
            }

            if (statusText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusText ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * FEATURE 5: Create Story Highlight Dialog
 */
@Composable
fun CreateHighlightDialog(
    userPosts: List<Post>,
    onDismiss: () -> Unit,
    onSave: (title: String, coverUrl: String, selectedIds: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCoverUrl by remember { mutableStateOf(userPosts.firstOrNull()?.mediaUrl ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("create_highlight_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("New Highlight", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Highlight Name") },
                    placeholder = { Text("e.g. Vibes, Coding, Trip") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("highlight_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Select Cover Image", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val coverOptions = userPosts.map { it.mediaUrl }.distinct().ifEmpty {
                        listOf(
                            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
                            "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
                            "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=800"
                        )
                    }
                    items(coverOptions) { url ->
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selectedCoverUrl == url) 2.5.dp else 1.dp,
                                    color = if (selectedCoverUrl == url) VyraViolet else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCoverUrl = url }
                        ) {
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title.trim(), selectedCoverUrl, emptyList())
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_highlight_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * FEATURE 5: Story Highlight Viewer Dialog
 */
@Composable
fun HighlightViewerDialog(
    highlight: StoryHighlight,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black,
            modifier = Modifier.fillMaxWidth().aspectRatio(9f / 14f).testTag("highlight_viewer_dialog")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = highlight.coverUrl,
                    contentDescription = highlight.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top bar overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        ) {
                            AsyncImage(model = highlight.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Text(highlight.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwner) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_highlight_button")) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Highlight", tint = Color.White)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PostGrid(
    posts: List<Post>,
    pinnedIds: List<String> = emptyList(),
    onSelectPost: (Post) -> Unit
) {
    val sortedPosts = remember(posts, pinnedIds) {
        posts.sortedWith(
            compareByDescending<Post> { it.isPinned || it.id in pinnedIds }
                .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_posts_grid"),
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(sortedPosts, key = { it.id }) { post ->
            val isItemPinned = post.isPinned || post.id in pinnedIds
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onSelectPost(post) }
            ) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isItemPinned) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = VyraCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTabBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileModal(
    currentProfile: UserProfile,
    repository: VyraRepository,
    onDismiss: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(currentProfile.displayName) }
    var bio by remember { mutableStateOf(currentProfile.bio) }
    var websiteUrl by remember { mutableStateOf(currentProfile.websiteUrl) }
    var selectedAvatar by remember { mutableStateOf(currentProfile.avatarUrl) }
    var isPrivate by remember { mutableStateOf(currentProfile.isPrivate) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Change Avatar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PRESET_AVATARS.take(4)) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (selectedAvatar == url) 2.dp else 0.dp,
                                    color = if (selectedAvatar == url) VyraViolet else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = url },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("Website") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Private Account")
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        val updated = currentProfile.copy(
                            displayName = displayName.trim(),
                            bio = bio.trim(),
                            websiteUrl = websiteUrl.trim(),
                            avatarUrl = selectedAvatar,
                            isPrivate = isPrivate
                        )
                        repository.saveUserProfile(updated)
                        isSaving = false
                        onProfileUpdated(updated)
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
