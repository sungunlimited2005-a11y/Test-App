package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.DraftItem
import com.example.model.MusicTrack
import com.example.model.UserProfile
import com.example.ui.components.MusicSelectionSheet
import com.example.ui.components.VyraMusicTag
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val CURATED_MEDIA_PRESETS = listOf(
    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
    "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
    "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=800",
    "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800",
    "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
    "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?w=800"
)

enum class CreateMode {
    POST, REEL, STORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    currentUserId: String,
    currentUser: UserProfile?,
    repository: VyraRepository,
    onPublishSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(CreateMode.POST) }

    var selectedMediaUrl by remember { mutableStateOf(CURATED_MEDIA_PRESETS[0]) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var customUrlInput by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var allowComments by remember { mutableStateOf(true) }
    var isCloseFriendsOnly by remember { mutableStateOf(false) }
    var collaboratorUsername by remember { mutableStateOf("") }

    // Reel trimming parameters (seconds 0 to 60)
    var trimStartSec by remember { mutableFloatStateOf(0f) }
    var trimEndSec by remember { mutableFloatStateOf(30f) }

    // Audio Volume Mix (0 - 100% and Mute)
    var originalAudioVolume by remember { mutableFloatStateOf(1.0f) }
    var musicVolume by remember { mutableFloatStateOf(1.0f) }
    var isOriginalAudioMuted by remember { mutableStateOf(false) }
    var isMusicMuted by remember { mutableStateOf(false) }

    // Selected Music Track
    var selectedMusic by remember { mutableStateOf<MusicTrack?>(null) }
    var showMusicSheet by remember { mutableStateOf(false) }

    // Drafts
    val drafts by repository.observeUserDrafts().collectAsStateWithLifecycle(initialValue = emptyList())
    var showDraftsSheet by remember { mutableStateOf(false) }

    // Upload & Publishing State
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var publishJob by remember { mutableStateOf<Job?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker for compliant media selection
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            selectedMediaUrl = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("create_content_screen")
    ) {
        // Screen Header with Drafts button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create Content",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Drafts badge button
            OutlinedButton(
                onClick = { showDraftsSheet = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("open_drafts_button")
            ) {
                Icon(Icons.Filled.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Drafts (${drafts.size})", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Selector: Post, Reel, Story
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CreateMode.values().forEach { m ->
                val isSelected = mode == m
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        mode = m
                        // Default trimming for reel
                        if (m == CreateMode.REEL) {
                            trimStartSec = 0f
                            trimEndSec = 30f
                        }
                    },
                    label = {
                        Text(
                            text = when (m) {
                                CreateMode.POST -> "Photo Post"
                                CreateMode.REEL -> "Vertical Reel"
                                CreateMode.STORY -> "24h Story"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (m) {
                            CreateMode.POST -> VyraViolet
                            CreateMode.REEL -> VyraMagenta
                            CreateMode.STORY -> VyraCyan
                        },
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (mode == CreateMode.REEL || mode == CreateMode.STORY) 9f / 13f else 1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = selectedMediaUrl,
                    contentDescription = "Selected Media Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Overlay tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = when (mode) {
                                CreateMode.POST -> Icons.Filled.PhotoCamera
                                CreateMode.REEL -> Icons.Filled.Movie
                                CreateMode.STORY -> Icons.Filled.History
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = mode.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Device Gallery Picker
                FilledTonalButton(
                    onClick = {
                        val request = if (mode == CreateMode.REEL) {
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        } else {
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        }
                        photoPickerLauncher.launch(request)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .testTag("pick_device_media_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (mode == CreateMode.REEL) Icons.Filled.VideoLibrary else Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (mode == CreateMode.REEL) "Pick Video" else "Gallery Media", fontSize = 12.sp)
                }
            }
        }

        // FEATURE 3: Reel Video Trimmer Controls
        if (mode == CreateMode.REEL) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.ContentCut, contentDescription = null, tint = VyraMagenta, modifier = Modifier.size(16.dp))
                            Text("Trim Video Clip", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Text(
                            text = "${trimStartSec.toInt()}s - ${trimEndSec.toInt()}s (${(trimEndSec - trimStartSec).toInt()}s duration)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VyraMagenta
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RangeSlider(
                        value = trimStartSec..trimEndSec,
                        onValueChange = { range ->
                            trimStartSec = range.start
                            trimEndSec = range.endInclusive
                        },
                        valueRange = 0f..60f,
                        steps = 59,
                        modifier = Modifier.fillMaxWidth().testTag("video_trim_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = VyraMagenta,
                            activeTrackColor = VyraMagenta
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Curated Presets
        Text("Choose from Aesthetic Presets", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CURATED_MEDIA_PRESETS) { preset ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (selectedMediaUrl == preset) 2.5.dp else 1.dp,
                            color = if (selectedMediaUrl == preset) VyraViolet else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedMediaUrl = preset
                            selectedMediaUri = null
                        }
                ) {
                    AsyncImage(
                        model = preset,
                        contentDescription = "Preset thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom URL Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customUrlInput,
                onValueChange = { customUrlInput = it },
                placeholder = { Text("Or paste image / video URL") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_custom_media_url"),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (customUrlInput.isNotBlank()) {
                        selectedMediaUrl = customUrlInput.trim()
                        selectedMediaUri = null
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FEATURE 4: Music Selection Interface
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = VyraMagenta)
                        Column {
                            Text("Music Soundtrack", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Add background music & Spotify attribution", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (selectedMusic == null) {
                        OutlinedButton(
                            onClick = { showMusicSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("button_select_music")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Track", fontSize = 12.sp)
                        }
                    }
                }

                if (selectedMusic != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VyraMagenta.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VyraMagenta, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(selectedMusic?.title ?: "", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text(selectedMusic?.artist ?: "", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { showMusicSheet = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Change Track", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { selectedMusic = null },
                                    modifier = Modifier.size(28.dp).testTag("button_remove_music")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove Track", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Audio Mixing & Volume Controls (Original Video Audio vs Added Music)
        if (mode == CreateMode.REEL || selectedMusic != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("audio_mixing_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = VyraCyan, modifier = Modifier.size(18.dp))
                            Text("Audio Mix & Volumes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Text("Dual-Track Sync", fontSize = 11.sp, color = VyraCyan, fontWeight = FontWeight.Bold)
                    }

                    // Original Video Audio Volume
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (isOriginalAudioMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isOriginalAudioMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Original Audio: ${if (isOriginalAudioMuted) "Muted" else "${(originalAudioVolume * 100).toInt()}%"}", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { isOriginalAudioMuted = !isOriginalAudioMuted },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(if (isOriginalAudioMuted) "Unmute" else "Mute", fontSize = 11.sp, color = if (isOriginalAudioMuted) VyraGreen else MaterialTheme.colorScheme.error)
                            }
                        }
                        Slider(
                            value = if (isOriginalAudioMuted) 0f else originalAudioVolume,
                            onValueChange = {
                                originalAudioVolume = it
                                if (isOriginalAudioMuted && it > 0f) isOriginalAudioMuted = false
                            },
                            valueRange = 0f..1f,
                            enabled = !isOriginalAudioMuted,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Added Music Soundtrack Volume
                    if (selectedMusic != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (isMusicMuted) Icons.Filled.VolumeOff else Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = if (isMusicMuted) MaterialTheme.colorScheme.error else VyraMagenta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Added Music: ${if (isMusicMuted) "Muted" else "${(musicVolume * 100).toInt()}%"}", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = { isMusicMuted = !isMusicMuted },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isMusicMuted) "Unmute" else "Mute", fontSize = 11.sp, color = if (isMusicMuted) VyraGreen else MaterialTheme.colorScheme.error)
                                }
                            }
                            Slider(
                                value = if (isMusicMuted) 0f else musicVolume,
                                onValueChange = {
                                    musicVolume = it
                                    if (isMusicMuted && it > 0f) isMusicMuted = false
                                },
                                valueRange = 0f..1f,
                                enabled = !isMusicMuted,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(thumbColor = VyraMagenta, activeTrackColor = VyraMagenta)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Caption Input
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text(if (mode == CreateMode.STORY) "Story Caption (Optional)" else "Write a caption...") },
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_caption"),
            shape = RoundedCornerShape(12.dp)
        )

        // Hashtag quick inserts
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val suggestions = listOf("#vyra", "#creators", "#photography", "#cinematic", "#reels", "#sunset", "#vibe")
            items(suggestions) { tag ->
                SuggestionChip(
                    onClick = {
                        if (!caption.contains(tag)) {
                            caption = if (caption.isBlank()) tag else "$caption $tag"
                        }
                    },
                    label = { Text(tag, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Feature Toggles
        if (mode == CreateMode.STORY) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = VyraGreen)
                    Column {
                        Text("Close Friends Only", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Only users on your close friends list can view", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = isCloseFriendsOnly,
                    onCheckedChange = { isCloseFriendsOnly = it },
                    modifier = Modifier.testTag("switch_close_friends")
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = VyraViolet)
                    Column {
                        Text("Private (Followers Only)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Only approved followers can see this in their feed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    modifier = Modifier.testTag("switch_post_privacy")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = VyraMagenta)
                    Column {
                        Text("Allow Comments", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Let viewers discuss and comment on this post", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = allowComments,
                    onCheckedChange = { allowComments = it },
                    modifier = Modifier.testTag("switch_allow_comments")
                )
            }

            // FEATURE 7: Collab Posts (Tag Collaborator)
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("collab_post_section")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = VyraCyan, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Invite Collaborator", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Post will appear on both profiles once accepted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = collaboratorUsername,
                        onValueChange = { collaboratorUsername = it },
                        placeholder = { Text("@username") },
                        singleLine = true,
                        leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = VyraCyan) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_collab_username"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Upload progress indicator (FEATURE 3: Progress & Cancel)
        if (isPublishing) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("upload_progress_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == CreateMode.REEL) "Uploading Reel..." else "Publishing...",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${(uploadProgress * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = VyraMagenta
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = VyraMagenta
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cancel upload button
                    OutlinedButton(
                        onClick = {
                            publishJob?.cancel()
                            isPublishing = false
                            uploadProgress = 0f
                            statusMessage = "Upload canceled."
                        },
                        modifier = Modifier.align(Alignment.End).testTag("cancel_upload_button"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel Upload", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusMessage ?: "", color = if (statusMessage?.contains("saved", true) == true) VyraGreen else MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: Save Draft & Publish
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // FEATURE 6: Save Draft Button
            OutlinedButton(
                onClick = {
                    val extractedHashtags = caption.split(" ", "\n")
                        .filter { it.startsWith("#") && it.length > 1 }
                        .map { it.removePrefix("#").lowercase() }

                    val draft = DraftItem(
                        id = "",
                        userId = currentUserId,
                        caption = caption,
                        mediaUri = selectedMediaUrl,
                        mediaType = if (mode == CreateMode.REEL) "video" else "image",
                        isReel = mode == CreateMode.REEL,
                        hashtags = extractedHashtags,
                        musicTitle = selectedMusic?.title ?: "",
                        musicArtist = selectedMusic?.artist ?: "",
                        musicTrackUrl = selectedMusic?.audioUrl ?: "",
                        spotifyUrl = selectedMusic?.spotifySharingUrl ?: "",
                        trimStartSeconds = trimStartSec,
                        trimEndSeconds = trimEndSec
                    )

                    scope.launch {
                        try {
                            repository.saveDraft(draft)
                            statusMessage = "Draft saved successfully!"
                        } catch (e: Exception) {
                            statusMessage = "Failed to save draft: ${e.localizedMessage}"
                        }
                    }
                },
                enabled = !isPublishing && (selectedMediaUrl.isNotBlank() || caption.isNotBlank()),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("button_save_draft"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Draft", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            // Publish Button
            Button(
                onClick = {
                    if (selectedMediaUrl.isBlank()) {
                        statusMessage = "Please select or enter media."
                        return@Button
                    }

                    isPublishing = true
                    uploadProgress = 0.1f
                    statusMessage = null

                    val extractedHashtags = caption.split(" ", "\n")
                        .filter { it.startsWith("#") && it.length > 1 }
                        .map { it.removePrefix("#").lowercase() }

                    publishJob = scope.launch {
                        try {
                            // Step 1: Upload media to Firebase Storage if a local URI is selected
                            var finalMediaUrl = selectedMediaUrl
                            if (selectedMediaUri != null) {
                                uploadProgress = 0.25f
                                finalMediaUrl = repository.uploadMedia(selectedMediaUri!!, if (mode == CreateMode.REEL) "reels" else "posts")
                                uploadProgress = 0.65f
                            } else {
                                uploadProgress = 0.5f
                            }

                            delay(300)
                            uploadProgress = 0.85f

                            if (mode == CreateMode.STORY) {
                                repository.publishStory(
                                    mediaUrl = finalMediaUrl,
                                    mediaType = "image",
                                    caption = caption,
                                    isCloseFriendsOnly = isCloseFriendsOnly
                                )
                            } else {
                                val newPostId = repository.createPost(
                                    mediaUrl = finalMediaUrl,
                                    mediaType = if (mode == CreateMode.REEL) "video" else "image",
                                    caption = caption,
                                    hashtags = extractedHashtags,
                                    isReel = mode == CreateMode.REEL,
                                    isPrivate = isPrivate,
                                    allowComments = allowComments,
                                    musicTitle = selectedMusic?.title ?: "",
                                    musicArtist = selectedMusic?.artist ?: "",
                                    musicUrl = selectedMusic?.audioUrl ?: "",
                                    spotifyUrl = selectedMusic?.spotifySharingUrl ?: "",
                                    contentType = if (mode == CreateMode.REEL) "REEL" else "POST",
                                    originalAudioVolume = originalAudioVolume,
                                    musicVolume = musicVolume,
                                    isOriginalAudioMuted = isOriginalAudioMuted,
                                    isMusicMuted = isMusicMuted
                                )
                                if (collaboratorUsername.isNotBlank()) {
                                    try {
                                        repository.inviteCollaborator(newPostId, collaboratorUsername)
                                    } catch (ce: Exception) {
                                        android.util.Log.e("VyraCollab", "Collab invite error: ${ce.message}")
                                    }
                                }
                            }
                            uploadProgress = 1.0f
                            delay(200)
                            isPublishing = false
                            onPublishSuccess()
                        } catch (e: Exception) {
                            isPublishing = false
                            uploadProgress = 0f
                            statusMessage = e.localizedMessage ?: "Failed to publish."
                        }
                    }
                },
                enabled = !isPublishing,
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp)
                    .testTag("button_publish_content"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (mode) {
                        CreateMode.POST -> VyraViolet
                        CreateMode.REEL -> VyraMagenta
                        CreateMode.STORY -> VyraCyan
                    }
                )
            ) {
                Text(
                    text = when (mode) {
                        CreateMode.POST -> "Share to Feed"
                        CreateMode.REEL -> "Publish Reel"
                        CreateMode.STORY -> "Add to Story"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Music Selection Modal Bottom Sheet
    if (showMusicSheet) {
        MusicSelectionSheet(
            repository = repository,
            selectedTrack = selectedMusic,
            onSelectTrack = { track ->
                selectedMusic = track
                showMusicSheet = false
            },
            onDismiss = { showMusicSheet = false }
        )
    }

    // FEATURE 6: Drafts Sheet
    if (showDraftsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDraftsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .testTag("drafts_bottom_sheet")
            ) {
                Text(
                    text = "Saved Drafts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Resume unfinished posts or delete them",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (drafts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No saved drafts yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
                    ) {
                        items(drafts, key = { it.id }) { draft ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().testTag("draft_item_${draft.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (draft.mediaUri.isNotBlank()) {
                                            AsyncImage(
                                                model = draft.mediaUri,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = if (draft.isReel) "Reel Draft" else "Post Draft",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (draft.isReel) VyraMagenta else VyraViolet
                                            )
                                            Text(
                                                text = draft.caption.ifBlank { "Untitled Draft" },
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (draft.musicTitle.isNotBlank()) {
                                                Text(
                                                    text = "🎵 ${draft.musicTitle}",
                                                    fontSize = 10.sp,
                                                    color = VyraMagenta
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Resume draft button
                                        IconButton(
                                            onClick = {
                                                selectedMediaUrl = draft.mediaUri
                                                selectedMediaUri = null
                                                caption = draft.caption
                                                mode = if (draft.isReel) CreateMode.REEL else CreateMode.POST
                                                trimStartSec = draft.trimStartSeconds
                                                trimEndSec = if (draft.trimEndSeconds > 0) draft.trimEndSeconds else 30f
                                                if (draft.musicTitle.isNotBlank()) {
                                                    selectedMusic = MusicTrack(
                                                        id = "draft_music",
                                                        title = draft.musicTitle,
                                                        artist = draft.musicArtist.ifBlank { "Unknown" },
                                                        audioUrl = draft.musicTrackUrl,
                                                        spotifySharingUrl = draft.spotifyUrl
                                                    )
                                                }
                                                showDraftsSheet = false
                                            },
                                            modifier = Modifier.testTag("resume_draft_${draft.id}")
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Resume Draft", tint = VyraViolet)
                                        }

                                        // Delete draft button
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.deleteDraft(draft.id)
                                                }
                                            },
                                            modifier = Modifier.testTag("delete_draft_${draft.id}")
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Draft", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
