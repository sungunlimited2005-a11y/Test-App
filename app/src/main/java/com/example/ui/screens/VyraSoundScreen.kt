package com.example.ui.screens

import android.media.MediaPlayer
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
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * FEATURE 5: VYRA Sound
 * Music collection & playlist sharing, custom playlist creation, track management,
 * and audio preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VyraSoundScreen(
    currentUserId: String,
    currentUser: UserProfile? = null,
    repository: VyraRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: My Playlists, 1: Community Collections, 2: Discover Tracks
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForDetail by remember { mutableStateOf<MusicPlaylist?>(null) }

    val myPlaylists by repository.observeUserPlaylists(currentUserId).collectAsStateWithLifecycle(initialValue = emptyList())
    val publicPlaylists by repository.observePublicPlaylists().collectAsStateWithLifecycle(initialValue = emptyList())

    // Audio preview player state
    var currentPlayingTrackId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    fun playTrack(track: PlaylistTrack) {
        if (currentPlayingTrackId == track.id) {
            mediaPlayer?.pause()
            currentPlayingTrackId = null
        } else {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                val player = MediaPlayer().apply {
                    setDataSource(track.audioUrl)
                    prepare()
                    start()
                    setOnCompletionListener {
                        currentPlayingTrackId = null
                    }
                }
                mediaPlayer = player
                currentPlayingTrackId = track.id
            } catch (e: Exception) {
                android.util.Log.e("VyraSound", "Play error: ${e.message}")
                currentPlayingTrackId = null
            }
        }
    }

    BackHandler {
        if (selectedPlaylistForDetail != null) {
            selectedPlaylistForDetail = null
        } else {
            onNavigateBack()
        }
    }

    if (selectedPlaylistForDetail != null) {
        PlaylistDetailView(
            playlist = selectedPlaylistForDetail!!,
            currentUserId = currentUserId,
            repository = repository,
            isPlaying = { id -> currentPlayingTrackId == id },
            onPlayTrack = { track -> playTrack(track) },
            onBack = { selectedPlaylistForDetail = null }
        )
    } else {
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
                            Text("VYRA Sound", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Icon(Icons.Filled.Headphones, contentDescription = null, tint = VyraMagenta, modifier = Modifier.size(20.dp))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showCreatePlaylistDialog = true },
                            modifier = Modifier.testTag("button_create_playlist")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "New Playlist", tint = VyraViolet)
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
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Collections (${myPlaylists.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Community Hub") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Authorized Library") }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // My Playlists
                        if (myPlaylists.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = VyraMagenta, modifier = Modifier.size(54.dp))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("No playlists yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Create and curate your personal music collections to attach to posts and share with friends.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { showCreatePlaylistDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = VyraViolet)) {
                                        Text("Create Playlist")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(myPlaylists, key = { it.id }) { playlist ->
                                    PlaylistCard(
                                        playlist = playlist,
                                        onClick = { selectedPlaylistForDetail = playlist }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Community Playlists
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(publicPlaylists, key = { it.id }) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { selectedPlaylistForDetail = playlist }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Curated Authorized Library Tracks
                        val curatedTracks = remember { repository.getMusicCatalogue("") }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(curatedTracks, key = { it.id }) { track ->
                                val pTrack = PlaylistTrack(
                                    id = track.id,
                                    title = track.title,
                                    artist = track.artist,
                                    audioUrl = track.audioUrl,
                                    durationMs = track.durationSeconds * 1000L,
                                    spotifyUrl = track.spotifySharingUrl
                                )
                                TrackRow(
                                    track = pTrack,
                                    isPlaying = currentPlayingTrackId == track.id,
                                    onPlayToggle = { playTrack(pTrack) },
                                    trailingAction = {
                                        // Quick add to first playlist if exists
                                        if (myPlaylists.isNotEmpty()) {
                                            IconButton(onClick = {
                                                scope.launch {
                                                    repository.addTrackToPlaylist(myPlaylists.first().id, pTrack)
                                                }
                                            }) {
                                                Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to playlist", tint = VyraViolet)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isPublic by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Playlist Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Public Collection", fontSize = 14.sp)
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.createPlaylist(title, description, isPublic = isPublic)
                            showCreatePlaylistDialog = false
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: MusicPlaylist,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("playlist_card_${playlist.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VyraMagenta),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Album, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.tracks.size} tracks • By @${playlist.creatorUsername}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (playlist.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playlist.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Playlist detail view with track listing, track deletion, and playback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailView(
    playlist: MusicPlaylist,
    currentUserId: String,
    repository: VyraRepository,
    isPlaying: (String) -> Boolean,
    onPlayTrack: (PlaylistTrack) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isOwner = playlist.userId == currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(playlist.title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = {
                            scope.launch {
                                repository.deletePlaylist(playlist.id)
                                onBack()
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(playlist.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(playlist.description.ifBlank { "Curated music playlist on VYRA" }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎵 ${playlist.tracks.size} tracks", fontSize = 12.sp, color = VyraViolet, fontWeight = FontWeight.Bold)
                        Text("👤 @${playlist.creatorUsername}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Tracks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VyraViolet)
            Spacer(modifier = Modifier.height(10.dp))

            if (playlist.tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("No tracks in this playlist yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(playlist.tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            isPlaying = isPlaying(track.id),
                            onPlayToggle = { onPlayTrack(track) },
                            trailingAction = {
                                if (isOwner) {
                                    IconButton(onClick = {
                                        scope.launch { repository.removeTrackFromPlaylist(playlist.id, track.id) }
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackRow(
    track: PlaylistTrack,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    trailingAction: @Composable () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) VyraMagenta else VyraViolet)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(track.artist, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            trailingAction()
        }
    }
}
