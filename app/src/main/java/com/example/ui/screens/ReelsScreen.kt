package com.example.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.components.VyraMusicTag
import com.example.ui.components.VyraUserBadgeRow
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReelsScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onSharePost: (Post) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    // FIX BUG 4: Reels Navigation Bug - ensure BackHandler always returns smoothly
    BackHandler { onNavigateBack() }

    val reels by repository.observeReels().collectAsState(initial = emptyList())
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())

    val visibleReels = remember(reels, blockedUsers, currentUser?.mutedUserIds) {
        val muted = currentUser?.mutedUserIds ?: emptyList()
        reels.filter { it.userId !in blockedUsers && it.userId !in muted }
    }

    if (visibleReels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Header with back button even in empty state
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("reels_empty_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Reels", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(VyraButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Reels yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Be the first to publish a high-energy short video or reel on VYRA!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNavigateToCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta),
                    modifier = Modifier.testTag("reels_create_first_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Reel")
                }
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { visibleReels.size })

        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("reels_vertical_pager")
        ) { page ->
            val reel = visibleReels[page]
            val isPageActive = pagerState.currentPage == page

            SingleReelPage(
                reel = reel,
                isPageActive = isPageActive,
                currentUserId = currentUserId,
                currentUser = currentUser,
                repository = repository,
                onNavigateToProfile = { onNavigateToProfile(reel.userId) },
                onSharePost = { onSharePost(reel) },
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
fun SingleReelPage(
    reel: Post,
    isPageActive: Boolean,
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onNavigateToProfile: () -> Unit,
    onSharePost: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(reel.likesCount) }
    var isSaved by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    // Dual-track synchronized audio volume state
    var isMuted by remember(reel.id) { mutableStateOf(reel.isMusicMuted) }
    var volumeLevel by remember(reel.id) { mutableFloatStateOf(if (reel.musicVolume > 0f) reel.musicVolume else 1.0f) }
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(reel.id) {
        repository.isPostLiked(reel.id, currentUserId).collect { liked ->
            isLiked = liked
        }
    }

    LaunchedEffect(reel.id) {
        repository.isPostSaved(reel.id).collect { saved ->
            isSaved = saved
        }
    }

    // Increment view count
    LaunchedEffect(reel.id) {
        repository.incrementViews(reel.id)
    }

    // FEATURE 3 FIX: Dual-track Audio Synchronization for Reel Music Playback
    DisposableEffect(reel.id, reel.musicUrl, isPageActive) {
        var player: MediaPlayer? = null
        if (isPageActive && reel.musicUrl.isNotBlank()) {
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    if (reel.musicUrl.startsWith("content://") || reel.musicUrl.startsWith("file://")) {
                        setDataSource(context, Uri.parse(reel.musicUrl))
                    } else {
                        setDataSource(reel.musicUrl)
                    }
                    isLooping = true
                    val vol = if (isMuted) 0f else volumeLevel
                    setVolume(vol, vol)
                    setOnPreparedListener {
                        if (isPlaying && isPageActive) start()
                    }
                    prepareAsync()
                }
                mediaPlayerInstance = player
            } catch (ignored: Exception) {
                player = null
                mediaPlayerInstance = null
            }
        }

        onDispose {
            try {
                player?.stop()
                player?.release()
            } catch (ignored: Exception) {}
            mediaPlayerInstance = null
        }
    }

    // Pause/Play synchronization when user taps screen or switches pages
    LaunchedEffect(isPlaying, isPageActive) {
        mediaPlayerInstance?.let { player ->
            try {
                if (isPlaying && isPageActive) {
                    if (!player.isPlaying) player.start()
                } else {
                    if (player.isPlaying) player.pause()
                }
            } catch (ignored: Exception) {}
        }
    }

    // Volume updates
    LaunchedEffect(isMuted, volumeLevel) {
        mediaPlayerInstance?.let { player ->
            try {
                val vol = if (isMuted) 0f else volumeLevel
                player.setVolume(vol, vol)
            } catch (ignored: Exception) {}
        }
    }

    // Rotating vinyl animation for music disc
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPlaying = !isPlaying
                showPlayPauseIndicator = true
            }
    ) {
        // Reel Media (Full-bleed)
        AsyncImage(
            model = reel.mediaUrl,
            contentDescription = reel.caption,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay at top and bottom for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Play/Pause center indicator
        AnimatedVisibility(
            visible = showPlayPauseIndicator,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            LaunchedEffect(showPlayPauseIndicator) {
                delay(600)
                showPlayPauseIndicator = false
            }
        }

        // Top Header with Explicit Back Navigation & Volume Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("reels_back_nav_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Reels",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Mute / Unmute Quick Toggle Button in Header
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .testTag("reel_volume_toggle_${reel.id}")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                    tint = if (isMuted) MaterialTheme.colorScheme.error else Color.White
                )
            }
        }

        // Right Action Column (Like, Comment, Share, Save, Music Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val newLiked = !isLiked
                        isLiked = newLiked
                        likesCount += if (newLiked) 1 else -1
                        scope.launch { repository.toggleLikePost(reel, !newLiked) }
                    },
                    modifier = Modifier.testTag("reel_like_button_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like Reel",
                        tint = if (isLiked) VyraMagenta else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "$likesCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment
            if (reel.allowComments) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showCommentsSheet = true },
                        modifier = Modifier.testTag("reel_comment_button_${reel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment Reel",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = "${reel.commentsCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Share
            IconButton(
                onClick = onSharePost,
                modifier = Modifier.testTag("reel_share_button_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Share Reel",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Save
            IconButton(
                onClick = {
                    val newSaved = !isSaved
                    isSaved = newSaved
                    scope.launch {
                        if (newSaved) {
                            repository.savePost(reel)
                        } else {
                            repository.unsavePost(reel.id)
                        }
                    }
                },
                modifier = Modifier.testTag("reel_save_button_${reel.id}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save Reel",
                    tint = if (isSaved) VyraViolet else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Rotating Vinyl Record Disc with Music indicator
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .rotate(if (isPlaying && isPageActive) rotation else 0f)
                    .clickable { isMuted = !isMuted },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.Gray else VyraMagenta)
                )
            }
        }

        // Bottom Creator Info & Caption
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 16.dp, bottom = 90.dp)
        ) {
            // Creator row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable(onClick = onNavigateToProfile)
            ) {
                AsyncImage(
                    model = reel.userAvatarUrl,
                    contentDescription = reel.username,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "@${reel.username}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    VyraUserBadgeRow(badges = reel.authorBadges, size = 15.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            if (reel.caption.isNotBlank()) {
                Text(
                    text = reel.caption,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sound indicator & Music attribution
            Spacer(modifier = Modifier.height(8.dp))
            if (!reel.musicTitle.isNullOrBlank()) {
                VyraMusicTag(
                    musicTitle = reel.musicTitle ?: "",
                    musicArtist = reel.musicArtist ?: "",
                    spotifyUrl = reel.spotifyUrl
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Original Audio • ${reel.username}",
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showCommentsSheet) {
        CommentsBottomSheet(
            post = reel,
            currentUserId = currentUserId,
            currentUser = currentUser,
            repository = repository,
            onDismiss = { showCommentsSheet = false }
        )
    }
}
