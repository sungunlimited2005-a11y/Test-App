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
import androidx.compose.material.icons.automirrored.filled.Send
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
 * FEATURE 3: VYRA Moments
 * Short text updates, photos, moods, attached music with 24-hour expiration,
 * reactions, and threaded replies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateBack: () -> Unit,
    onNavigateToUser: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showCreateMomentSheet by remember { mutableStateOf(false) }
    var selectedMomentForReplies by remember { mutableStateOf<VyraMoment?>(null) }

    val moments by repository.observeMoments().collectAsStateWithLifecycle(initialValue = emptyList())
    val mutedUserIds by repository.observeMutedUsers().collectAsStateWithLifecycle(initialValue = emptyList())
    val closeFriendIds by repository.observeCloseFriends().collectAsStateWithLifecycle(initialValue = emptyList())

    // Filter moments: respect privacy (everyone, followers, close_friends) and muted users
    val visibleMoments = remember(moments, mutedUserIds, closeFriendIds, currentUserId) {
        moments.filter { moment ->
            if (moment.userId in mutedUserIds) return@filter false
            if (moment.userId == currentUserId) return@filter true
            when (moment.privacy) {
                "close_friends" -> moment.userId in closeFriendIds
                else -> true
            }
        }
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("VYRA Moments", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Surface(
                            color = VyraViolet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "24h",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VyraViolet,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateMomentSheet = true },
                        modifier = Modifier.testTag("button_create_moment")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add Moment", tint = VyraViolet, modifier = Modifier.size(28.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateMomentSheet = true },
                containerColor = VyraViolet,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                text = { Text("Share Moment", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_share_moment")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (visibleMoments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = VyraViolet,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active Moments right now",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Moments are spontaneous 24-hour updates with your mood, music, and thoughts. Be the first to share one!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showCreateMomentSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post a Moment")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(visibleMoments, key = { it.id }) { moment ->
                        MomentCard(
                            moment = moment,
                            currentUserId = currentUserId,
                            onLike = { isLiked ->
                                scope.launch { repository.toggleLikeMoment(moment, isLiked) }
                            },
                            onOpenReplies = {
                                selectedMomentForReplies = moment
                            },
                            onDelete = {
                                scope.launch { repository.deleteMoment(moment.id) }
                            },
                            onNavigateToUser = onNavigateToUser
                        )
                    }
                }
            }
        }
    }

    if (showCreateMomentSheet) {
        CreateMomentBottomSheet(
            currentUser = currentUser,
            onDismiss = { showCreateMomentSheet = false },
            onPostMoment = { text, mediaUrl, mood, musicTitle, musicArtist, privacy ->
                showCreateMomentSheet = false
                scope.launch {
                    repository.publishMoment(
                        text = text,
                        mediaUrl = mediaUrl,
                        mood = mood,
                        musicTitle = musicTitle,
                        musicArtist = musicArtist,
                        privacy = privacy
                    )
                }
            }
        )
    }

    selectedMomentForReplies?.let { activeMoment ->
        MomentRepliesSheet(
            moment = activeMoment,
            currentUserId = currentUserId,
            repository = repository,
            onDismiss = { selectedMomentForReplies = null }
        )
    }
}

/**
 * Individual Moment Card with 24h countdown, Mood badge, Music track, and Reactions
 */
@Composable
fun MomentCard(
    moment: VyraMoment,
    currentUserId: String,
    onLike: (Boolean) -> Unit,
    onOpenReplies: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToUser: (String) -> Unit
) {
    val isMine = moment.userId == currentUserId
    val isLiked = currentUserId in moment.likedUserIds

    // Time remaining until 24h expiration
    val remainingHours = remember(moment.expiresAt) {
        if (moment.expiresAt != null) {
            val diffMs = moment.expiresAt.toDate().time - System.currentTimeMillis()
            (diffMs / (1000 * 60 * 60)).coerceAtLeast(0)
        } else 24L
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_${moment.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Author info, Badges, 24h countdown, Privacy indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clickable { onNavigateToUser(moment.userId) }
                        .weight(1f)
                ) {
                    if (moment.userAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = moment.userAvatarUrl,
                            contentDescription = moment.username,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VyraViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(moment.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(moment.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            VyraUserBadgeRow(badges = moment.authorBadges, size = 13.dp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Expires in ${remainingHours}h",
                                fontSize = 11.sp,
                                color = VyraViolet,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (moment.privacy == "close_friends") {
                                Surface(
                                    color = VyraGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Close Friends",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VyraGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isMine) {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Moment", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }

            // Mood pill if set
            if (moment.mood.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = moment.mood,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Text update
            if (moment.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = moment.text,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Attached photo
            if (moment.mediaUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = moment.mediaUrl,
                    contentDescription = "Moment image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Music track pill if attached
            if (moment.musicTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VyraMagenta, modifier = Modifier.size(18.dp))
                        Column {
                            Text(moment.musicTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (moment.musicArtist.isNotBlank()) {
                                Text(moment.musicArtist, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action footer: React, Reply count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onLike(isLiked) }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = moment.likesCount.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onOpenReplies)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Replies",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${moment.repliesCount} replies",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Quick emoji reaction bar
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val emojis = listOf("❤️", "🔥", "✨", "👏", "😂", "⚡")
                    items(emojis) { em ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onLike(false) }
                                .padding(2.dp)
                        ) {
                            Text(em, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet to Compose and Publish a New Moment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMomentBottomSheet(
    currentUser: UserProfile?,
    onDismiss: () -> Unit,
    onPostMoment: (text: String, mediaUrl: String, mood: String, musicTitle: String, musicArtist: String, privacy: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("✨ Inspired") }
    var mediaUrl by remember { mutableStateOf("") }
    var musicTitle by remember { mutableStateOf("") }
    var musicArtist by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("everyone") }

    val moodOptions = listOf(
        "⚡ Focused",
        "☕ Chilling",
        "✨ Inspired",
        "🔥 Creating",
        "🎧 Vibing",
        "🌙 Late Night",
        "🚀 Grinding"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Create a Moment", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Surface(
                    color = VyraViolet.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Auto-expires in 24h",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VyraViolet,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mood chips
            Text("Select Current Mood:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(moodOptions) { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood },
                        label = { Text(mood, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("What's on your mind right now? (Short update)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("input_moment_text"),
                shape = RoundedCornerShape(14.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optional image URL
            OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                placeholder = { Text("Attach image URL (optional)") },
                leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Optional music title
            OutlinedTextField(
                value = musicTitle,
                onValueChange = { musicTitle = it },
                placeholder = { Text("Attach song / music title (optional)") },
                leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy selection
            Text("Audience & Privacy:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = privacy == "everyone",
                    onClick = { privacy = "everyone" },
                    label = { Text("Everyone") }
                )
                FilterChip(
                    selected = privacy == "followers",
                    onClick = { privacy = "followers" },
                    label = { Text("Followers Only") }
                )
                FilterChip(
                    selected = privacy == "close_friends",
                    onClick = { privacy = "close_friends" },
                    label = { Text("Close Friends ⭐") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onPostMoment(
                        text.trim(),
                        mediaUrl.trim(),
                        selectedMood,
                        musicTitle.trim(),
                        musicArtist.trim(),
                        privacy
                    )
                },
                enabled = text.isNotBlank() || mediaUrl.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_submit_moment"),
                colors = ButtonDefaults.buttonColors(containerColor = VyraViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Publish Moment", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * Threaded Replies Sheet for a Moment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentRepliesSheet(
    moment: VyraMoment,
    currentUserId: String,
    repository: VyraRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var replyText by remember { mutableStateOf("") }
    val replies by repository.observeMomentReplies(moment.id).collectAsStateWithLifecycle(initialValue = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text("Replies to @${moment.username}'s Moment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            if (replies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No replies yet. Be the first to reply!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(replies, key = { it.id }) { reply ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (reply.userAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = reply.userAvatarUrl,
                                    contentDescription = reply.username,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(VyraViolet),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(reply.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(reply.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    VyraUserBadgeRow(badges = reply.userBadges, size = 12.dp)
                                }
                                Text(reply.text, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply to this moment...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            val text = replyText.trim()
                            replyText = ""
                            scope.launch {
                                repository.replyToMoment(moment.id, text)
                            }
                        }
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = VyraViolet)
                }
            }
        }
    }
}
