package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.example.data.VyraRepository
import com.example.model.MusicTrack
import com.example.ui.theme.*

/**
 * FEATURE 2: Local Music Picker & Authorized Catalogue Interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSelectionSheet(
    repository: VyraRepository,
    selectedTrack: MusicTrack?,
    onSelectTrack: (MusicTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Catalog, 1 = Device Audio
    var searchQuery by remember { mutableStateOf("") }
    val catalogue = remember(searchQuery) { repository.getMusicCatalogue(searchQuery) }

    var localAudioError by remember { mutableStateOf<String?>(null) }
    var pickedLocalTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var trimStartOffset by remember { mutableFloatStateOf(0f) }

    var playingTrackId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Audio Picker launcher for local audio files from device storage
    val localAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            localAudioError = null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Device Audio"
                val title = if (!rawTitle.isNullOrBlank()) rawTitle else fileName
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.ifBlank { "Local Audio" } ?: "Local Audio"
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 30000L
                val durationSec = (durationMs / 1000).toInt().coerceAtLeast(5)

                pickedLocalTrack = MusicTrack(
                    id = "local_${System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    album = "Device Audio",
                    coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                    audioUrl = uri.toString(),
                    durationSeconds = durationSec,
                    genre = "Local Track",
                    spotifySharingUrl = ""
                )
                trimStartOffset = 0f
            } catch (e: Exception) {
                localAudioError = "Could not read audio file: ${e.localizedMessage ?: "Unsupported format"}"
            } finally {
                try {
                    retriever.release()
                } catch (ignored: Exception) {}
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("music_selection_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Add Soundtrack",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Curated catalog & local device music",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab bar: Catalogue vs Device Audio
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Vyra Catalogue", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Local Device Audio", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // Search by title, artist, or genre
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by song title, artist, or genre...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = VyraViolet)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("music_search_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(catalogue, key = { it.id }) { track ->
                        val isCurrent = selectedTrack?.id == track.id
                        val isPlaying = playingTrackId == track.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTrack(track)
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    onDismiss()
                                }
                                .testTag("music_track_${track.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) VyraViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isCurrent) BorderStroke(1.5.dp, VyraViolet) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Album Art
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = track.coverUrl,
                                        contentDescription = track.album,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Play / Pause audio preview overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f))
                                            .clickable {
                                                if (isPlaying) {
                                                    mediaPlayer?.stop()
                                                    mediaPlayer?.release()
                                                    mediaPlayer = null
                                                    playingTrackId = null
                                                } else {
                                                    try {
                                                        mediaPlayer?.release()
                                                        mediaPlayer = MediaPlayer().apply {
                                                            setAudioAttributes(
                                                                AudioAttributes.Builder()
                                                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                                    .build()
                                                            )
                                                            setDataSource(track.audioUrl)
                                                            prepareAsync()
                                                            setOnPreparedListener { start() }
                                                            setOnCompletionListener {
                                                                playingTrackId = null
                                                            }
                                                        }
                                                        playingTrackId = track.id
                                                    } catch (e: Exception) {
                                                        playingTrackId = null
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Track Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${track.album}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = track.genre,
                                            fontSize = 10.sp,
                                            color = VyraMagenta,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${track.durationSeconds}s",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Select button
                                Button(
                                    onClick = {
                                        onSelectTrack(track)
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrent) VyraViolet else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isCurrent) "Selected" else "Add",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // FEATURE 2: Local Device Audio Tab
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(VyraMagenta.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AudioFile, contentDescription = null, tint = VyraMagenta, modifier = Modifier.size(24.dp))
                                }
                                Column {
                                    Text("Pick Song from Storage", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Select MP3, AAC, WAV, or local song", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Button(
                                onClick = {
                                    localAudioLauncher.launch("audio/*")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_local_audio_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta)
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Audio File", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (localAudioError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = localAudioError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Selected Local Track Preview & Segment Selection
                    val local = pickedLocalTrack
                    if (local != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.5.dp, VyraMagenta),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (playingTrackId == local.id) {
                                                        mediaPlayer?.stop()
                                                        mediaPlayer?.release()
                                                        mediaPlayer = null
                                                        playingTrackId = null
                                                    } else {
                                                        try {
                                                            mediaPlayer?.release()
                                                            mediaPlayer = MediaPlayer().apply {
                                                                setDataSource(context, Uri.parse(local.audioUrl))
                                                                prepare()
                                                                seekTo((trimStartOffset * 1000).toInt())
                                                                start()
                                                                setOnCompletionListener { playingTrackId = null }
                                                            }
                                                            playingTrackId = local.id
                                                        } catch (e: Exception) {
                                                            localAudioError = "Preview failed: ${e.localizedMessage}"
                                                            playingTrackId = null
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (playingTrackId == local.id) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "Preview",
                                                    tint = Color.White
                                                )
                                            }
                                        }

                                        Column {
                                            Text(local.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${local.artist} • ${local.durationSeconds}s", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    IconButton(onClick = { pickedLocalTrack = null }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                // Segment / Start trim slider
                                if (local.durationSeconds > 15) {
                                    Column {
                                        Text(
                                            text = "Start Offset: ${trimStartOffset.toInt()}s",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Slider(
                                            value = trimStartOffset,
                                            onValueChange = {
                                                trimStartOffset = it
                                                if (playingTrackId == local.id) {
                                                    mediaPlayer?.seekTo((it * 1000).toInt())
                                                }
                                            },
                                            valueRange = 0f..(local.durationSeconds - 5f).coerceAtLeast(0f),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onSelectTrack(local)
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_local_audio_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VyraMagenta)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply This Song to Content", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
